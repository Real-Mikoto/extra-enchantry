package realmikoto.extraenchantry;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.advancements.AdvancementHolder;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 家族共鸣（1.2.0「共鸣与臻藏」→ 1.3.0「铭刻与试炼」）：附魔家族的等级累计与档位结算。
 *
 * 计件规则（DESIGN_v1.2.0 §1 修正版）：装备四件 + 主副手上的每个附魔，
 * 按**其等级**计入所属家族（多家族附魔同时计入多系，见 family_* 标签；
 * 霆霓双归水/风暴，余烬双归火/灵魂）。
 *
 * 档位：PARTIAL / FULL（阈值数据化，默认 3/5），多系可同时激活。
 * - 升档反馈：聊天提示 + 音效；降档静默（主调家族失去 FULL 时提示一次）；
 * - 状态栏：RESONANCE 效果滚动续期（40 tick），amplifier = 激活家族数 - 1。
 *
 * 小加成（PARTIAL，§2.2）：多等级附魔结算时视为 +1 级
 * （{@link #effectiveLevel(ServerPlayer, Holder, int)} 供各 Manager 接入），
 * 数值由调用方钳制在该附魔的设计上限内。
 *
 * 完整共鸣（FULL）八系被动（数值由 {@link ResonanceConfig} 数据化）：
 *   灵魂 猎魂：击杀生物后隐身（AFTER_DEATH 钩子 → onLivingDeath）
 *   风暴 天象感应：雨/雷暴天气移速加成（临时属性修饰符）
 *   锋刃 连击：窗口内连续命中伤害递增（hurtServer 结算钩子）
 *   守护 岿然：击退抗性（临时属性修饰符）
 *   自然 扎根：静止后每秒回复（tick 位置检测，受伤暂停为主调专属）
 *   水 潮汐亲和：水下移速 + 耗氧减免（属性 + tick 补气）
 *   风 轻盈：摔落伤害减免（hurtServer 结算钩子）
 *   火焰 炽热之躯：免疫火焰 / 熔岩伤害（hurtServer 取消）；熔岩移速为主调专属
 *
 * 主调铭刻（1.3.0 §2.2）：{@link AttunementManager} 唯一主调，收益只强化 FULL 被动
 * （{@code attunementActive} = 选择保留且当前 FULL，失去 FULL 自动暂停）。
 *
 * 性能（1.3.0 §2.5）：装备签名缓存——签名未变且未到兜底间隔时跳过附魔遍历，
 * 沿用档位/累计快照；装备、维度、登录或数据包重载（RulesLoader 清缓存）后立即全量。
 *
 * 附带隐秘挑战追踪（§4）：劫火余生（骑余烬金马铠在熔岩上累计行进 50 格）。
 */
public final class FamilyResonanceManager {

	/** 扫描间隔（tick）＝ 1 秒（DESIGN_v1.2.0 §2.1） */
	private static final int SCAN_INTERVAL_TICKS = 20;

	/** 装备签名未变时的全量扫描兜底间隔（tick）：每 5 秒强制重算一次 */
	private static final long FULL_SCAN_INTERVAL_TICKS = 100L;

	/** RESONANCE 效果滚动续期时长（tick），> 扫描间隔保证不断档 */
	private static final int RESONANCE_REFRESH_TICKS = 40;

	/** 参与计分的六个槽位（装备四件 + 主副手） */
	private static final EquipmentSlot[] SCORED_SLOTS = {
			EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	// ============ 家族定义 ============

	public enum Family {
		SOUL("soul"), STORM("storm"), BLADE("blade"), NATURE("nature"),
		GUARD("guard"), WIND("wind"), FIRE("fire"), WATER("water");

		final TagKey<Enchantment> tag;
		/** 1.7.0 原版谱系标签（vanilla_lineage/<family>）：原版附魔书的家族归属声明 */
		final TagKey<Enchantment> vanillaTag;

		Family(String path) {
			this.tag = TagKey.create(Registries.ENCHANTMENT, ExtraEnchantry.id("family_" + path));
			this.vanillaTag = TagKey.create(Registries.ENCHANTMENT, ExtraEnchantry.id("vanilla_lineage/" + path));
		}

		/** 双标签命中（本模 family_xxx ∪ 原版 vanilla_lineage/xxx，1.7.0 计件合并） */
		public boolean matches(Holder<Enchantment> holder) {
			return holder.is(tag) || holder.is(vanillaTag);
		}
	}

	// ============ 状态 ============

	/** 玩家 UUID → 各家族档位（全量扫描时刷新，快照分支沿用） */
	private static final Map<UUID, Map<Family, Tier>> TIERS = new ConcurrentHashMap<>();
	/** 玩家 UUID → 上次扫描的档位（升档检测用） */
	private static final Map<UUID, Map<Family, Tier>> PREV_TIERS = new ConcurrentHashMap<>();
	/** 玩家 UUID → 各家族累计等级快照（秘典/命令/试炼的唯一口径，P0） */
	private static final Map<UUID, Map<Family, Integer>> TOTALS = new ConcurrentHashMap<>();
	/** 装备签名缓存（附魔组件 + 维度；未变则跳过附魔遍历） */
	private static final Map<UUID, Integer> EQUIP_SIGS = new ConcurrentHashMap<>();
	/** 上次全量扫描的 gameTime（签名未变时的兜底重算间隔） */
	private static final Map<UUID, Long> LAST_FULL_SCAN = new ConcurrentHashMap<>();
	/** 自然扎根：静止起始（overworld gameTime，1.3.0 起不再使用系统毫秒） */
	private static final Map<UUID, Long> NATURE_STILL_SINCE = new ConcurrentHashMap<>();
	/** 自然扎根：最近受伤时刻（受伤暂停，主调自然专属） */
	private static final Map<UUID, Long> LAST_HURT_TICK = new ConcurrentHashMap<>();
	/** 通用上次位置（自然扎根 / 熔岩骑乘测距共用） */
	private static final Map<UUID, Vec3> LAST_POS = new ConcurrentHashMap<>();
	/** 劫火余生：熔岩骑乘累计距离（格） */
	private static final Map<UUID, Double> LAVA_RIDE_DISTANCE = new ConcurrentHashMap<>();

	/** 锋刃连击：玩家 UUID → 上次命中（overworld gameTime） */
	private static final Map<UUID, Long> BLADE_LAST_HIT = new ConcurrentHashMap<>();
	/** 锋刃连击：当前连击层数 */
	private static final Map<UUID, Integer> BLADE_COMBO = new ConcurrentHashMap<>();

	public enum Tier {
		NONE, PARTIAL, FULL
	}

	private FamilyResonanceManager() {
	}

	// ============ 主扫描（ServerTickEvents 注册） ============

	/** 挂 Fabric {@code ServerTickEvents.END_SERVER_TICK}：每秒错峰扫描玩家共鸣 */
	public static void tick(MinecraftServer server) {
		long gameTime = server.overworld().getGameTime();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isDeadOrDying() || player.isSpectator()) {
				continue;
			}
			// 错峰：按玩家 UUID 哈希把扫描摊开到 20 tick 内
			if ((gameTime + (player.getUUID().hashCode() & 0x7FFFFFFF)) % SCAN_INTERVAL_TICKS != 0) {
				continue;
			}
			scanPlayer(player, gameTime);
			tickPassives(player, gameTime);
		}
	}

	/**
	 * 扫描一名玩家：装备签名未变且未到兜底间隔 → 沿用快照（跳过附魔遍历）；
	 * 否则全量重算（附魔累计 → 档位 → 升档反馈）。两分支均续期 RESONANCE 与属性被动。
	 */
	private static void scanPlayer(ServerPlayer player, long gameTime) {
		UUID playerId = player.getUUID();
		int signature = equipmentSignature(player);
		Integer prevSignature = EQUIP_SIGS.get(playerId);
		long lastFull = LAST_FULL_SCAN.getOrDefault(playerId, Long.MIN_VALUE / 2);
		boolean fullScan = prevSignature == null || prevSignature.intValue() != signature
				|| gameTime - lastFull >= FULL_SCAN_INTERVAL_TICKS;

		if (fullScan) {
			// ---- 全量扫描：附魔按等级计入所属家族 → 档位 ----
			Map<Family, Integer> totals = new EnumMap<>(Family.class);
			for (Family family : Family.values()) {
				totals.put(family, 0);
			}
			for (EquipmentSlot slot : SCORED_SLOTS) {
				ItemStack stack = player.getItemBySlot(slot);
				if (stack.isEmpty()) {
					continue;
				}
				ItemEnchantmentsLoop:
				for (Holder<Enchantment> holder : stack.getEnchantments().keySet()) {
					int enchantLevel = stack.getEnchantments().getLevel(holder);
					for (Family family : Family.values()) {
						if (holder.is(family.tag)) {
							totals.merge(family, enchantLevel, Integer::sum);
							continue ItemEnchantmentsLoop;
						}
					}
				}
			}
			// 1.4.0：配饰 4 槽（Attachment）同样计入家族计件——宝石不计数，只算附魔等级
			for (ItemStack stack : AccessoryAttachments.slots(player)) {
				if (stack.isEmpty()) {
					continue;
				}
				AccessoryLoop:
				for (Holder<Enchantment> holder : stack.getEnchantments().keySet()) {
					int enchantLevel = stack.getEnchantments().getLevel(holder);
					for (Family family : Family.values()) {
						if (holder.is(family.tag)) {
							totals.merge(family, enchantLevel, Integer::sum);
							continue AccessoryLoop;
						}
					}
				}
			}
		Map<Family, Tier> tiers = new EnumMap<>(Family.class);
		for (Family family : Family.values()) {
			int total = totals.get(family);
			tiers.put(family, tierOf(total, family, player));
		}
			Map<Family, Tier> prev = PREV_TIERS.put(playerId, tiers);
			TIERS.put(playerId, tiers);
			TOTALS.put(playerId, totals);
			EQUIP_SIGS.put(playerId, signature);
			LAST_FULL_SCAN.put(playerId, gameTime);

			// 升档反馈（降档静默；主调失去 FULL 时提示一次）
			for (Family family : Family.values()) {
				Tier now = tiers.get(family);
				Tier before = prev != null ? prev.get(family) : Tier.NONE;
				if (now != Tier.NONE && now != before) {
					player.sendSystemMessage(Component.translatable(
							"message.extra-enchantry.resonance.tier_" + now.name().toLowerCase(),
							Component.translatable("family.extra-enchantry." + family.name().toLowerCase())));
					FxHelper.play((ServerLevel) player.level(), player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
					// 首次共鸣入门提示（全局一次，1.3.1 被动引导）
					OnboardingManager.onFamilyPartial(player);
					// 1.7.0 谱系主线：首次 PARTIAL / FULL 授予
					if (now == Tier.PARTIAL) {
						LineageManager.onFirstPartial(player);
					}
				}
				if (now == Tier.FULL && before != Tier.FULL) {
					// 首次 FULL → 显示试炼进度树根节点 + 派发家族铭文（1.3.1）
					FamilyTrialsManager.onReachFull(player);
					OnboardingManager.onFamilyFull(player, family);
					// 1.7.0 谱系主线：首次 FULL 授予
					LineageManager.onFirstFull(player);
				}
				if (before == Tier.FULL && now != Tier.FULL
						&& AttunementManager.attunedFamily(player) == family) {
					// 主调家族失去 FULL：保留选择、收益暂停（不静默删除）
					player.sendSystemMessage(Component.translatable(
							"message.extra-enchantry.attunement.paused",
							Component.translatable("family.extra-enchantry." + family.name().toLowerCase())));
				}
			}
		}

		// ---- 公共：RESONANCE 效果滚动续期（状态栏展示，amplifier = 激活家族数 - 1） ----
		Map<Family, Tier> tiers = TIERS.get(playerId);
		if (tiers != null) {
			int activeFamilies = 0;
			for (Family family : Family.values()) {
				if (tiers.get(family) != Tier.NONE) {
					activeFamilies++;
				}
			}
			if (activeFamilies > 0) {
				player.addEffect(new MobEffectInstance(ExtraEnchantryEffects.RESONANCE,
						RESONANCE_REFRESH_TICKS, Math.max(0, activeFamilies - 1), true, false, true));
			} else if (player.hasEffect(ExtraEnchantryEffects.RESONANCE)) {
				player.removeEffect(ExtraEnchantryEffects.RESONANCE);
			}
		}
		applyAttributePassives(player);
	}

	/** 累计等级 → 档位（阈值数据化；1.6.0 五境同辉：四护甲任一携带 → 两阈值各 −1） */
	private static Tier tierOf(int total, Family family, ServerPlayer player) {
		ResonanceConfig.FamilyRules rules = ResonanceConfig.rules(family);
		int partial = rules.partialThreshold();
		int full = rules.fullThreshold();
		if (hasRealmsUnity(player)) {
			partial = Math.max(1, partial - 1);
			full = Math.max(1, full - 1);
		}
		return total >= full ? Tier.FULL
				: total >= partial ? Tier.PARTIAL : Tier.NONE;
	}

	/** 五境同辉（46 号传说）：四护甲槽任一携带即生效（单件生效，多件不叠加） */
	private static boolean hasRealmsUnity(ServerPlayer player) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (!slot.isArmor()) {
				continue;
			}
			ItemStack stack = player.getItemBySlot(slot);
			ItemEnchantments enchantments = stack.getEnchantments();
			for (Holder<Enchantment> enchantment : enchantments.keySet()) {
				if (enchantment.is(ExtraEnchantry.REALMS_UNITY)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 装备签名：六槽（物品 + 附魔组件）+ 当前维度。
	 * 附魔组件（ItemEnchantments record）与物品 identity 足以判定计分输入是否变化。
	 */
	private static int equipmentSignature(ServerPlayer player) {
		int signature = 0;
		for (EquipmentSlot slot : SCORED_SLOTS) {
			ItemStack stack = player.getItemBySlot(slot);
			signature = signature * 31 + (stack.isEmpty() ? 0
					: Objects.hash(stack.getItem(), stack.get(DataComponents.ENCHANTMENTS)));
		}
		// 1.4.0：配饰 4 槽纳入签名（物品 + 附魔组件变化即触发全量重算）
		for (ItemStack stack : AccessoryAttachments.slots(player)) {
			signature = signature * 31 + (stack.isEmpty() ? 0
					: Objects.hash(stack.getItem(), stack.get(DataComponents.ENCHANTMENTS)));
		}
		signature = signature * 31 + player.level().dimension().hashCode();
		return signature;
	}

	// ============ FULL 属性类被动（数值数据化 + 主调强化） ============

	private static void applyAttributePassives(ServerPlayer player) {
		// 风暴 天象感应：雨天/雷暴移速（主调 → +25%）
		ResonanceConfig.FamilyRules stormRules = ResonanceConfig.rules(Family.STORM);
		double stormBonus = AttunementManager.attunementActive(player, Family.STORM)
				? stormRules.attunedStormSpeedBonus() : stormRules.stormSpeedBonus();
		boolean stormActive = tierOf(player, Family.STORM) == Tier.FULL && player.level().isRaining();
		applyModifier(player, Attributes.MOVEMENT_SPEED, "storm_weather", stormActive ? stormBonus : 0.0D,
				AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		// 守护 岿然：击退抗性（主调 → +70%）
		ResonanceConfig.FamilyRules guardRules = ResonanceConfig.rules(Family.GUARD);
		double guardValue = AttunementManager.attunementActive(player, Family.GUARD)
				? guardRules.attunedGuardKnockbackResist() : guardRules.guardKnockbackResist();
		boolean guardActive = tierOf(player, Family.GUARD) == Tier.FULL;
		applyModifier(player, Attributes.KNOCKBACK_RESISTANCE, "guard_resist",
				guardActive ? guardValue : 0.0D, AttributeModifier.Operation.ADD_VALUE);
		// 水 潮汐亲和：水下移速（主调 → +40%）与补气（主调 → 40% 消耗）
		ResonanceConfig.FamilyRules waterRules = ResonanceConfig.rules(Family.WATER);
		boolean attunedWater = AttunementManager.attunementActive(player, Family.WATER);
		double waterBonus = attunedWater ? waterRules.attunedWaterSpeedBonus() : waterRules.waterSpeedBonus();
		boolean waterActive = tierOf(player, Family.WATER) == Tier.FULL && player.isEyeInFluid(FluidTags.WATER);
		applyModifier(player, Attributes.WATER_MOVEMENT_EFFICIENCY, "water_affinity",
				waterActive ? waterBonus : 0.0D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		// 水 补气：水下且空气未满时每秒补 max/divisor（原版 1 秒扣 15）
		if (waterActive && player.getAirSupply() < player.getMaxAirSupply()) {
			int divisor = attunedWater ? waterRules.attunedWaterAirReplenishDivisor()
					: waterRules.waterAirReplenishDivisor();
			player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + player.getMaxAirSupply() / divisor));
		}
		// 火焰主调：熔岩中移速加成（FULL + 主调 + 正在熔岩中）
		boolean fireLava = tierOf(player, Family.FIRE) == Tier.FULL
				&& AttunementManager.attunementActive(player, Family.FIRE)
				&& player.isInLava();
		applyModifier(player, Attributes.MOVEMENT_SPEED, "fire_lava_speed",
				fireLava ? ResonanceConfig.rules(Family.FIRE).fireLavaSpeedBonus() : 0.0D,
				AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	}

	private static void applyModifier(ServerPlayer player, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
			String name, double value, AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		Identifier id = ExtraEnchantry.id("resonance_" + name);
		if (value <= 0.0D) {
			instance.removeModifier(id);
		} else {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, value, operation));
		}
	}

	// ============ 每秒被动（自然扎根 + 劫火余生挑战） ============

	private static void tickPassives(ServerPlayer player, long gameTime) {
		tickNatureRegen(player, gameTime);
		tickLavaRide(player);
	}

	/** 自然 扎根：静止后每秒回复（主调 1.5 HP/秒；受伤暂停 2 秒为主调专属） */
	private static void tickNatureRegen(ServerPlayer player, long gameTime) {
		boolean natureFull = tierOf(player, Family.NATURE) == Tier.FULL;
		ResonanceConfig.FamilyRules rules = ResonanceConfig.rules(Family.NATURE);
		boolean attuned = AttunementManager.attunementActive(player, Family.NATURE);
		long now = gameTime;
		Vec3 pos = player.position();
		Vec3 last = LAST_POS.put(player.getUUID(), pos);
		boolean moved = last != null && last.distanceToSqr(pos) > 0.001D;
		boolean canRegen = natureFull && player.isAlive() && player.getFoodData().getFoodLevel() > 0
				&& player.getHealth() < player.getMaxHealth();
		if (moved || !canRegen) {
			NATURE_STILL_SINCE.put(player.getUUID(), now);
			if (moved) {
				// 自然·复苏试炼：单次站定被移动打断
				FamilyTrialsManager.onNatureMoved(player);
			}
			return;
		}
		// 受伤暂停：最近受伤未过暂停窗口则跳过本秒（静止计时保留，不重置）
		Long lastHurt = LAST_HURT_TICK.get(player.getUUID());
		if (attuned && lastHurt != null && now - lastHurt < rules.natureHurtPauseTicks()) {
			return;
		}
		long since = NATURE_STILL_SINCE.getOrDefault(player.getUUID(), now);
		if (now - since >= rules.natureStillTicks()) {
			float heal = attuned ? (float) rules.attunedNatureRegenHp() : rules.natureRegenHp();
			player.heal(heal);
			if (player.level() instanceof ServerLevel serverLevel) {
				FxHelper.burstAt(serverLevel, pos.x, pos.y + 0.3D, pos.z,
						ParticleTypes.COMPOSTER, 3, 0.2D);
			}
			// 每秒一次：本次回血后重置窗口；自然·复苏试炼累计
			NATURE_STILL_SINCE.put(player.getUUID(), now);
			FamilyTrialsManager.onNatureHeal(player, heal);
		}
	}

	/** 劫火余生：骑余烬金马铠在熔岩上累计行进 50 格（按秒采样累计水平位移） */
	private static void tickLavaRide(ServerPlayer player) {
		UUID playerId = player.getUUID();
		Vec3 pos = player.position();
		if (!(player.getVehicle() instanceof LivingEntity mount) || !mount.isInLava()
				|| ExtraEnchantry.getEmberfallLevel(mount.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.BODY)) <= 0) {
			LAST_POS.remove(playerId);
			return;
		}
		Vec3 last = LAST_POS.put(playerId, pos);
		if (last == null) {
			return;
		}
		double added = Math.sqrt((pos.x - last.x) * (pos.x - last.x) + (pos.z - last.z) * (pos.z - last.z));
		double total = LAVA_RIDE_DISTANCE.merge(playerId, added, Double::sum);
		if (total >= 50.0D) {
			LAVA_RIDE_DISTANCE.remove(playerId);
			awardHidden(player, ExtraEnchantry.id("hidden_challenges/lava_ride"));
		}
	}

	// ============ 伤害结算回调（LivingEntityMixin） ============

	/** 自然扎根受伤暂停的记账（Mixin hurtServer 回调，仅自然 FULL 玩家记录） */
	public static void onPlayerHurt(ServerPlayer player) {
		if (tierOf(player, Family.NATURE) == Tier.FULL) {
			MinecraftServer server = player.level().getServer();
			if (server != null) {
				LAST_HURT_TICK.put(player.getUUID(), server.overworld().getGameTime());
			}
		}
	}

	// ============ 死亡结算入口（AFTER_DEATH 注册） ============

	/** 共鸣与挑战的死亡结算：灵魂被动（猎魂）+ 深渊回响挑战 */
	public static void onLivingDeath(LivingEntity victim, net.minecraft.world.damagesource.DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer killer)) {
			return;
		}
		Map<Family, Tier> tiers = TIERS.get(killer.getUUID());
		// 灵魂 猎魂：FULL 灵魂击杀生物后隐身（主调延长；对玩家击杀同样生效，设计未排除）
		if (tiers != null && tiers.get(Family.SOUL) == Tier.FULL) {
			ResonanceConfig.FamilyRules rules = ResonanceConfig.rules(Family.SOUL);
			int invisTicks = AttunementManager.attunementActive(killer, Family.SOUL)
					? rules.attunedSoulInvisibilityTicks() : rules.soulInvisibilityTicks();
			killer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, invisTicks, 0, false, false, true));
			if (killer.level() instanceof ServerLevel serverLevel) {
				FxHelper.burst(serverLevel, killer, ParticleTypes.SMOKE, 6, 0.2D);
			}
		}
		// 深渊回响：携带渊息在水下击杀远古守卫者
		if (victim.getType() == EntityTypes.ELDER_GUARDIAN
				&& killer.isEyeInFluid(FluidTags.WATER)
				&& ExtraEnchantry.getTideheartLevel(killer.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)) > 0) {
			awardHidden(killer, ExtraEnchantry.id("hidden_challenges/abyssal_echo"));
		}
	}

	/** 死而不僵：劫后余辉锁血期间击杀攻击者（AfterglowManager 锁血结算回调） */
	public static void onAfterglowKill(ServerPlayer killer) {
		awardHidden(killer, ExtraEnchantry.id("hidden_challenges/afterglow_revenge"));
	}

	/** 百步穿杨：归羽箭命中距发射点 ≥40 格的生物（AbstractArrowMixin 命中钩子回调） */
	public static void onHomingLongShot(ServerPlayer player) {
		awardHidden(player, ExtraEnchantry.id("hidden_challenges/homing_snipe"));
	}

	/** 丰收之神的赞许：丰壤连锁收获计数回调（LoamManager 每 crop 调用一次） */
	public static void onLoamChainCrop(ServerPlayer player, int chainCount) {
		if (chainCount >= 64) {
			awardHidden(player, ExtraEnchantry.id("hidden_challenges/harvest_blessing"));
		}
	}

	/** 以彼之道：冲阵撞击命中的劫掠兽被同一玩家击杀（ShieldChargeManager 回调） */
	public static void onRavagerChargeKill(ServerPlayer killer) {
		awardHidden(killer, ExtraEnchantry.id("hidden_challenges/like_for_like"));
	}

	/** 授予隐秘挑战进度（幂等），键含 hidden_challenges/ 子目录前缀 */
	private static void awardHidden(ServerPlayer player, Identifier advancementId) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		AdvancementHolder holder = server.getAdvancements().get(advancementId);
		if (holder != null) {
			player.getAdvancements().award(holder, "triggered");
		}
	}

	// ============ 查询 API（唯一口径，P0） ============

	/** 当前玩家是否至少处于该家族 PARTIAL 档 */
	public static boolean partialOrBetter(ServerPlayer player, Family family) {
		return tierOf(player, family) != Tier.NONE;
	}

	/** 玩家在该家族的档位（离线/无数据 → NONE） */
	public static Tier tierOf(ServerPlayer player, Family family) {
		Map<Family, Tier> tiers = TIERS.get(player.getUUID());
		return tiers != null ? tiers.getOrDefault(family, Tier.NONE) : Tier.NONE;
	}

	/** 玩家在该家族的累计附魔等级快照（秘典/命令展示用；离线/无数据 → 0） */
	public static int familyTotal(ServerPlayer player, Family family) {
		Map<Family, Integer> totals = TOTALS.get(player.getUUID());
		return totals != null ? totals.getOrDefault(family, 0) : 0;
	}

	/** 诊断：装备签名缓存新鲜度（debug 命令用；uncached = 尚未全量扫描） */
	public static String debugCacheState(ServerPlayer player) {
		UUID playerId = player.getUUID();
		Integer signature = EQUIP_SIGS.get(playerId);
		Long last = LAST_FULL_SCAN.get(playerId);
		if (signature == null || last == null) {
			return "uncached";
		}
		MinecraftServer server = player.level().getServer();
		long age = server != null ? server.overworld().getGameTime() - last : -1L;
		return "sig=" + Integer.toHexString(signature) + " age=" + age + "t";
	}

	/** 数据包重载后标脏全部玩家缓存：下一次扫描（≤1 秒）全量重算（验收 6.6：2 秒内应用） */
	public static void clearScanCaches() {
		EQUIP_SIGS.clear();
		LAST_FULL_SCAN.clear();
	}

	/**
	 * 共鸣小加成：家族 ≥PARTIAL 时多等级附魔按 +1 级结算。
	 * 调用方负责数值钳制在该附魔的设计上限内。
	 */
	public static int effectiveLevel(ServerPlayer player, Holder<Enchantment> holder, int baseLevel) {
		for (Family family : Family.values()) {
			if (holder.is(family.tag) && tierOf(player, family) != Tier.NONE) {
				return baseLevel + 1;
			}
		}
		return baseLevel;
	}

	// ============ hurtServer 结算参数（Mixin 调用） ============

	/**
	 * 风 轻盈：摔落伤害乘数（FULL 风 1 - 减免；主调 0.65；其余 1.0）。
	 */
	public static float windFallMultiplier(ServerPlayer player) {
		if (tierOf(player, Family.WIND) != Tier.FULL) {
			return 1.0F;
		}
		ResonanceConfig.FamilyRules rules = ResonanceConfig.rules(Family.WIND);
		double reduction = AttunementManager.attunementActive(player, Family.WIND)
				? rules.attunedWindFallReduction() : rules.windFallReduction();
		return (float) (1.0D - reduction);
	}

	/**
	 * 风暴主调：雷击伤害乘数（FULL 风暴 + 主调 → 1 - attuned_lightning_reduction；其余 1.0）。
	 */
	public static float lightningDamageMultiplier(ServerPlayer player) {
		if (tierOf(player, Family.STORM) != Tier.FULL
				|| !AttunementManager.attunementActive(player, Family.STORM)) {
			return 1.0F;
		}
		return (float) (1.0D - ResonanceConfig.rules(Family.STORM).attunedLightningReduction());
	}

	/**
	 * 锋刃连击：命中时更新连击层并返回本次伤害倍率（FULL 锋刃专属；
	 * 主调层数上限提升）。窗口按服务器游戏时间（1.3.0 起不再使用系统毫秒）。
	 */
	public static float bladeComboMultiplier(ServerPlayer attacker) {
		if (tierOf(attacker, Family.BLADE) != Tier.FULL) {
			return 1.0F;
		}
		ResonanceConfig.FamilyRules rules = ResonanceConfig.rules(Family.BLADE);
		boolean attuned = AttunementManager.attunementActive(attacker, Family.BLADE);
		int maxHits = attuned ? rules.attunedBladeComboMaxHits() : rules.bladeComboMaxHits();
		MinecraftServer server = attacker.level().getServer();
		if (server == null) {
			return 1.0F;
		}
		long now = server.overworld().getGameTime();
		Long last = BLADE_LAST_HIT.get(attacker.getUUID());
		int combo = (last != null && now - last <= rules.bladeComboWindowTicks())
				? Math.min(BLADE_COMBO.getOrDefault(attacker.getUUID(), 0) + 1, maxHits)
				: 1;
		BLADE_LAST_HIT.put(attacker.getUUID(), now);
		BLADE_COMBO.put(attacker.getUUID(), combo);
		if (combo >= maxHits) {
			// 锋刃·百炼试炼：增伤叠至上限标记
			FamilyTrialsManager.onBladeComboMax(attacker);
		}
		return 1.0F + (float) rules.bladeComboPerHit() * (combo - 1);
	}
}
