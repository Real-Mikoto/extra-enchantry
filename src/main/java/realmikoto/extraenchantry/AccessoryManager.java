package realmikoto.extraenchantry;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.tags.DamageTypeTags;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 配饰结算中心（1.4.0「环佩与獠牙」）：8 配饰附魔 + 8 宝石被动的统一结算。
 *
 * <p>挂点分布（见设计稿 §10）：属性类走本类 tick（值变化才写瞬态修改器，活力同模式）；
 * 受伤 / 造成伤害由 {@code LivingEntityMixin} hurtServer 侧调用本类静态入口；
 * 击杀类挂 {@code ServerLivingEntityEvents.AFTER_DEATH}；自然恢复加速用自有计时器
 * （零 Mixin 的退路方案，稳定优先）。
 *
 * <p>宝石被动按配饰材质传导（铜 100% / 铁 125% / 金 150% / 钻 200%），
 * 每槽位至多 1 条、全身至多 4 条，天然不叠加。
 */
public final class AccessoryManager {

	// ============ 附魔数值（per level） ============
	private static final double SHIELD_PENDANT_REDUCTION = 0.03D;     // 盾坠：全伤害 -3%/级
	private static final double THUNDER_CLASP_BONUS = 0.04D;         // 雷鸣扣：雷雨造成伤害 +4%/级
	private static final double VERDANT_DROP_REGEN = 0.10D;          // 翠滴：自然恢复 +10%/级
	private static final double BLADE_RING_SPEED = 0.05D;            // 刃戒：攻速 +5%/级
	private static final double PLUME_RING_REDUCTION = 0.06D;        // 羽环：弹射物伤害 -6%/级
	private static final double EMBER_BRACELET_REDUCTION = 0.10D;    // 烬镯：火伤害 -10%/级
	private static final double TIDE_BRACELET_SWIM = 0.08D;          // 潮镯：游泳效率 +8%/级
	private static final int SOUL_CHIME_FOOD = 2;                     // 魂铃：饥饿 +2/级
	// （魂铃饱和 = level 点）

	// ============ 宝石被动基数（× 材质传导率） ============
	private static final double GEM_STORM_SPEED = 0.03D;              // 雷光石：雷雨移速 +3%
	private static final double GEM_BLADE_SPEED = 0.02D;              // 刃晶：攻速 +2%
	private static final double GEM_GUARD_REDUCTION = 0.02D;          // 盾纹玉：全伤害 -2%
	private static final double GEM_WIND_REDUCTION = 0.03D;          // 风羽晶：弹射物伤害 -3%
	private static final double GEM_EMBER_REDUCTION = 0.04D;          // 烬心石：火伤害 -4%
	private static final double GEM_TIDE_SWIM = 0.05D;               // 潮汐珠：游泳效率 +5%
	private static final double GEM_NATURE_REGEN = 0.05D;             // 萌芽晶：自然恢复 +5%
	private static final int GEM_SOUL_XP = 5;                         // 魂珀：击杀 +5 经验

	/** 原版自然回血基线间隔（food≥18 每 80 tick 回 1 HP） */
	private static final int VANILLA_REGEN_INTERVAL = 80;
	/** 恢复加速下限（防极端堆叠把间隔压得过低） */
	private static final int MIN_REGEN_INTERVAL = 40;

	/** 属性修改器当前值缓存（玩家 → 修改器 id → 值；值不变跳过写） */
	private static final Map<UUID, Map<String, Double>> ATTRIBUTE_CACHE = new HashMap<>();

	/** 自然恢复计时器（玩家 → 剩余 tick） */
	private static final Map<UUID, Integer> REGEN_COUNTDOWN = new HashMap<>();

	private AccessoryManager() {
	}

	// ============ 穿戴 ============

	/**
	 * 穿戴到指定槽位：空槽放入返回空；占用则交换返回旧配饰。
	 * 附魔等级与宝石被动即时生效（结算每 tick 重读，无需事件通知）。
	 */
	public static ItemStack equip(ServerPlayer player, Accessories.SlotType slot, ItemStack toEquip) {
		ItemStack current = AccessoryAttachments.get(player, slot);
		AccessoryAttachments.set(player, slot, toEquip);
		return current == null ? ItemStack.EMPTY : current;
	}

	/** 穿戴音效按材质对位（铜/铁 = 铁装备声，金 = 金，钻 = 钻石） */
	public static void playEquipSound(ServerLevel level, LivingEntity wearer, Accessories.Material material) {
		var event = switch (material) {
			case GOLD -> SoundEvents.ARMOR_EQUIP_GOLD;
			case DIAMOND -> SoundEvents.ARMOR_EQUIP_DIAMOND;
			default -> SoundEvents.ARMOR_EQUIP_IRON;   // 铜 / 铁共用
		};
		level.playSound(null, wearer.getX(), wearer.getY(), wearer.getZ(),
				event, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	// ============ tick 结算（属性修改器 + 自然恢复） ============

	/** 每服务端 tick 调用（END_SERVER_TICK → 玩家遍历在本类外完成） */
	public static void tick(ServerPlayer player) {
		ItemStack[] slots = AccessoryAttachments.slots(player);

		// ---- 属性类（合并同属性来源，值变化才写） ----
		double attackSpeed = 0.0D;
		double swimEfficiency = 0.0D;
		double stormSpeed = 0.0D;
		for (ItemStack stack : slots) {
			if (stack.isEmpty()) {
				continue;
			}
			attackSpeed += enchantmentLevel(stack, ExtraEnchantry.BLADE_RING) * BLADE_RING_SPEED;
			swimEfficiency += enchantmentLevel(stack, ExtraEnchantry.TIDE_BRACELET) * TIDE_BRACELET_SWIM;
			String gem = Accessories.socketedGem(stack);
			double conductivity = Accessories.conductivityOf(stack);
			if (gem != null && conductivity > 0.0D) {
				switch (gem) {
					case "blade_shard" -> attackSpeed += GEM_BLADE_SPEED * conductivity;
					case "tide_pearl" -> swimEfficiency += GEM_TIDE_SWIM * conductivity;
					case "storm_stone" -> {                                   // 雷光石仅雷雨天生效
						if (player.level().isThundering()) {
							stormSpeed += GEM_STORM_SPEED * conductivity;
						}
					}
					default -> {
					}
				}
			}
		}
		writeModifier(player, Attributes.ATTACK_SPEED, "accessory_attack_speed",
				attackSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		writeModifier(player, Attributes.WATER_MOVEMENT_EFFICIENCY, "accessory_swim",
				swimEfficiency, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		writeModifier(player, Attributes.MOVEMENT_SPEED, "accessory_storm_speed",
				stormSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

		// ---- 自然恢复加速（翠滴 + 萌芽晶）：自有计时器，替代注入原版回血分支 ----
		tickRegenBoost(player, slots);

		// ---- 家族铭印环境粒子（1.4.0，L1 常驻 ≤1 粒/2 秒/宝石，逐宝石节流） ----
		if (player.level() instanceof ServerLevel serverLevel) {
			for (ItemStack stack : slots) {
				String gem = Accessories.socketedGem(stack);
				if (gem != null) {
					GemFx.ambient(serverLevel, player, gem);
				}
			}
		}
	}

	/** 自然恢复：food≥18 且未满血时按加成缩短间隔（下限 40 tick） */
	private static void tickRegenBoost(ServerPlayer player, ItemStack[] slots) {
		boolean canRegen = player.isAlive() && !player.level().isClientSide()
				&& player.getFoodData().getFoodLevel() >= 18
				&& player.getHealth() < player.getMaxHealth();
		if (!canRegen) {
			REGEN_COUNTDOWN.remove(player.getUUID());
			return;
		}
		double bonus = 0.0D;
		for (ItemStack stack : slots) {
			if (stack.isEmpty()) {
				continue;
			}
			bonus += enchantmentLevel(stack, ExtraEnchantry.VERDANT_DROP) * VERDANT_DROP_REGEN;
			String gem = Accessories.socketedGem(stack);
			if ("sprout_crystal".equals(gem)) {
				bonus += GEM_NATURE_REGEN * Accessories.conductivityOf(stack);
			}
		}
		if (bonus <= 0.0D) {
			return;   // 无加成：交给原版回血（不重复 heal）
		}
		int interval = Math.max(MIN_REGEN_INTERVAL,
				(int) Math.round(VANILLA_REGEN_INTERVAL / (1.0D + bonus)));
		// 修复：旧实现 merge 后立即 getOrDefault 覆盖递减结果，countdown 恒为累加值、heal 永不可达
		Integer remaining = REGEN_COUNTDOWN.compute(player.getUUID(),
				(uuid, current) -> current == null ? interval : current - 1);
		if (remaining != null && remaining <= 0) {
			REGEN_COUNTDOWN.put(player.getUUID(), interval);
			player.heal(1.0F);
		}
	}

	/** 属性修改器写入（值缓存，值未变跳过；值 ≤ 0 移除） */
	private static void writeModifier(ServerPlayer player,
			Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
			String name, double value, AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		Map<String, Double> cache = ATTRIBUTE_CACHE.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
		Double previous = cache.get(name);
		if (previous != null && previous == value) {
			return;
		}
		cache.put(name, value);
		Identifier id = ExtraEnchantry.id(name);
		if (value <= 0.0D) {
			instance.removeModifier(id);
		} else {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, value, operation));
		}
	}

	// ============ hurtServer 侧入口（LivingEntityMixin 调用） ============

	/**
	 * 受到伤害减免（victim 侧，hurtServer HEAD ModifyVariable）：
	 * 盾坠 / 盾纹玉 全伤害；羽环 / 风羽晶 弹射物；烬镯 / 烬心石 火。
	 * 仅对 ServerPlayer 生效（非玩家无配饰）。
	 */
	public static float incomingDamage(LivingEntity victim, float amount, DamageSource source) {
		if (!(victim instanceof ServerPlayer player)) {
			return amount;
		}
		double reduction = 0.0D;
		boolean isProjectile = source.is(DamageTypeTags.IS_PROJECTILE);
		boolean isFire = source.is(DamageTypeTags.IS_FIRE);
		for (ItemStack stack : AccessoryAttachments.slots(player)) {
			if (stack.isEmpty()) {
				continue;
			}
			reduction += enchantmentLevel(stack, ExtraEnchantry.SHIELD_PENDANT) * SHIELD_PENDANT_REDUCTION;
			if (isProjectile) {
				reduction += enchantmentLevel(stack, ExtraEnchantry.PLUME_RING) * PLUME_RING_REDUCTION;
			}
			if (isFire) {
				reduction += enchantmentLevel(stack, ExtraEnchantry.EMBER_BRACELET) * EMBER_BRACELET_REDUCTION;
			}
			String gem = Accessories.socketedGem(stack);
			double conductivity = Accessories.conductivityOf(stack);
			if (gem != null && conductivity > 0.0D) {
				switch (gem) {
					case "guard_jade" -> reduction += GEM_GUARD_REDUCTION * conductivity;
					case "wind_feather" -> {
						if (isProjectile) {
							reduction += GEM_WIND_REDUCTION * conductivity;
						}
					}
					case "ember_heart" -> {
						if (isFire) {
							reduction += GEM_EMBER_REDUCTION * conductivity;
						}
					}
					default -> {
					}
				}
			}
		}
		return reduction <= 0.0D ? amount : amount * (float) Math.max(0.05D, 1.0D - reduction);
	}

	/**
	 * 造成伤害加成（攻击者侧）：
	 * 雷鸣扣——雷雨天气（isThundering）时全伤害 +4%/级。
	 * attacker 由 LivingEntityMixin 从 source.getEntity() 解析后传入（hurtServer 的 this 是受害者）。
	 */
	public static float outgoingDamage(ServerPlayer attacker, float amount, DamageSource source) {
		if (!attacker.level().isThundering()) {
			return amount;
		}
		double bonus = 0.0D;
		for (ItemStack stack : AccessoryAttachments.slots(attacker)) {
			bonus += enchantmentLevel(stack, ExtraEnchantry.THUNDER_CLASP) * THUNDER_CLASP_BONUS;
		}
		return bonus <= 0.0D ? amount : amount * (float) (1.0D + bonus);
	}

	// ============ 击杀类（AFTER_DEATH 事件调用） ============

	/** 击杀敌对生物：魂铃回复饥饿与饱和；魂珀额外经验（× 传导） */
	public static void onKillMob(ServerPlayer killer, LivingEntity victim) {
		if (!(victim instanceof net.minecraft.world.entity.monster.Enemy)) {
			return;   // 仅敌对生物
		}
		int soulChime = 0;
		int soulXp = 0;
		for (ItemStack stack : AccessoryAttachments.slots(killer)) {
			if (stack.isEmpty()) {
				continue;
			}
			soulChime = Math.max(soulChime, enchantmentLevel(stack, ExtraEnchantry.SOUL_CHIME));
			if ("soul_amber".equals(Accessories.socketedGem(stack))) {
				soulXp += (int) Math.round(GEM_SOUL_XP * Accessories.conductivityOf(stack));
			}
		}
		if (soulChime > 0) {
			killer.getFoodData().eat(SOUL_CHIME_FOOD * soulChime, soulChime);
		}
		if (soulXp > 0) {
			killer.giveExperiencePoints(soulXp);
		}
	}

	// ============ 死亡链路（PlayerMixin / ServerPlayerMixin 调用） ============

	/** 死亡暂存的誓约配饰（玩家 UUID → 4 槽快照），restoreFrom 时回插 */
	private static final Map<UUID, ItemStack[]> DEATH_KEPT = new HashMap<>();

	/**
	 * dropEquipment HEAD（keepInventory 关闭时）：
	 * 非誓约配饰掉落在尸体处、誓约配饰暂存、Attachment 清空——与背包掉落同点位同时机。
	 */
	public static void onDropEquipment(net.minecraft.world.entity.player.Player player) {
		ItemStack[] slots = AccessoryAttachments.slots(player);
		boolean any = false;
		for (ItemStack stack : slots) {
			if (!stack.isEmpty()) {
				any = true;
				break;
			}
		}
		if (!any) {
			return;
		}
		ItemStack[] kept = new ItemStack[slots.length];
		java.util.Arrays.fill(kept, ItemStack.EMPTY);
		boolean keptAny = false;
		for (int i = 0; i < slots.length; i++) {
			ItemStack stack = slots[i];
			if (stack.isEmpty()) {
				continue;
			}
			if (ExtraEnchantry.getOathboundLevel(stack) > 0) {
				kept[i] = stack;
				keptAny = true;
			} else {
				player.drop(stack, false, false);
			}
		}
		ItemStack[] cleared = new ItemStack[slots.length];
		java.util.Arrays.fill(cleared, ItemStack.EMPTY);
		AccessoryAttachments.set(player, cleared);
		if (keptAny) {
			DEATH_KEPT.put(player.getUUID(), kept);
		}
	}

	/** 玩家登出清理（DISCONNECT 调用）：暂存的誓约配饰若从未走 restoreFrom，直接写回其附件存档 */
	public static void onDisconnect(net.minecraft.world.entity.player.Player player) {
		ItemStack[] kept = DEATH_KEPT.remove(player.getUUID());
		if (kept != null) {
			// 玩家实体登出时仍可写 attachment（随 save 落盘），物品不会凭空消失
			AccessoryAttachments.set(player, kept);
		}
		ATTRIBUTE_CACHE.remove(player.getUUID());
		REGEN_COUNTDOWN.remove(player.getUUID());
	}

	/**
	 * 玩家重生 / 维度切换后强制重建属性修改器（restoreFrom TAIL 调用）。
	 * 修复：缓存按 UUID 键控，但瞬态修改器随旧实体销毁——重登/重生后缓存命中导致
	 * 加成静默丢失（直到数值变化才恢复）。清缓存让下一 tick 全量重写。
	 */
	public static void onPlayerRestore(ServerPlayer newPlayer) {
		ATTRIBUTE_CACHE.remove(newPlayer.getUUID());
	}

	/**
	 * restoreFrom TAIL：暂存的誓约配饰回插；否则 keepEverything / 旁观者 /
	 * keepInventory 时整体搬运（Attachment 非 copyOnDeath，须显式跟随）。
	 */
	public static void onRestoreFrom(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean keepEverything) {
		// 修复 #13：瞬态属性修改器随旧实体销毁，必须让新玩家全量重写（清值缓存）
		ATTRIBUTE_CACHE.remove(newPlayer.getUUID());
		ItemStack[] kept = DEATH_KEPT.remove(oldPlayer.getUUID());
		if (kept != null) {
			AccessoryAttachments.set(newPlayer, kept);
			return;
		}
		if (keepEverything || oldPlayer.isSpectator()
				|| oldPlayer.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY)) {
			AccessoryAttachments.set(newPlayer, AccessoryAttachments.slots(oldPlayer));
		}
	}

	// ============ tooltip / 共鸣计件辅助 ============

	/** 宝石被动百分比文本（按传导率折算，tooltip 用） */
	public static String gemPassivePercent(String gemPath, double conductivity) {
		double percent = switch (gemPath) {
			case "guard_jade" -> GEM_GUARD_REDUCTION;
			case "storm_stone" -> GEM_STORM_SPEED;
			case "blade_shard" -> GEM_BLADE_SPEED;
			case "sprout_crystal" -> GEM_NATURE_REGEN;
			case "wind_feather" -> GEM_WIND_REDUCTION;
			case "ember_heart" -> GEM_EMBER_REDUCTION;
			case "tide_pearl" -> GEM_TIDE_SWIM;
			default -> 0.0D;
		};
		return String.format(Locale.ROOT, "%.0f%%", percent * 100.0D * conductivity);
	}

	/** 读配饰上指定附魔等级（本模块私有读法，stack 空 → 0） */
	static int enchantmentLevel(ItemStack stack, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
		if (stack == null || stack.isEmpty()) {
			return 0;
		}
		ItemEnchantments enchantments = stack.getEnchantments();
		for (Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(key)) {
				return enchantments.getLevel(enchantment);
			}
		}
		return 0;
	}
}
