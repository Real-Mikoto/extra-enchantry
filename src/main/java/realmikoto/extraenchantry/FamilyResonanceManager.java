package realmikoto.extraenchantry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.advancements.AdvancementHolder;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 家族共鸣（1.2.0「共鸣与臻藏」）：附魔家族的等级累计与档位结算。
 *
 * 计件规则（DESIGN_v1.2.0 §1 修正版）：装备四件 + 主副手上的每个附魔，
 * 按**其等级**计入所属家族（多家族附魔同时计入多系，见 family_* 标签；
 * 霆霓双归水/风暴，余烬双归火/灵魂）。
 *
 * 档位：PARTIAL（家族累计 ≥3 级）/ FULL（≥5 级），多系可同时激活。
 * - 升档反馈：聊天提示 + 音效；降档静默；
 * - 状态栏：RESONANCE 效果滚动续期（40 tick），amplifier = 激活家族数 - 1；
 *
 * 小加成（PARTIAL，§2.2）：多等级附魔结算时视为 +1 级
 * （{@link #effectiveLevel(ServerPlayer, Holder, int)} 供各 Manager 接入），
 * 数值由调用方钳制在该附魔的设计上限内。
 *
 * 完整共鸣（FULL，§2.3）八系被动：
 *   灵魂 猎魂：击杀生物后 3 秒隐身（AFTER_DEATH 钩子 → onLivingDeath）
 *   风暴 天象感应：雨/雷暴天气移速 +20%（临时属性修饰符）
 *   锋刃 连击：3 秒内连续命中伤害递增，最高 +15%（hurtServer 结算钩子）
 *   守护 岿然：击退抗性 +50%（临时属性修饰符）
 *   自然 扎根：静止 3 秒后每秒回复 1 HP（tick 位置检测）
 *   水 潮汐亲和：水下移速 +30%、氧气消耗减半（属性 + tick 补气）
 *   风 轻盈：摔落伤害 -50%（hurtServer 结算钩子）
 *   火焰 炽热之躯：免疫火焰 / 熔岩伤害（hurtServer 取消）
 *
 * 附带隐秘挑战追踪（§4）：劫火余生（骑余烬金马铠在熔岩上累计行进 50 格）。
 */
public final class FamilyResonanceManager {

	/** 扫描间隔（tick）＝ 1 秒（DESIGN_v1.2.0 §2.1） */
	private static final int SCAN_INTERVAL_TICKS = 20;

	/** 共鸣档位阈值：家族累计附魔等级 */
	private static final int PARTIAL_THRESHOLD = 3;
	private static final int FULL_THRESHOLD = 5;

	/** RESONANCE 效果滚动续期时长（tick），> 扫描间隔保证不断档 */
	private static final int RESONANCE_REFRESH_TICKS = 40;

	// ============ 八系被动参数 ============

	/** 风暴：雨天移速加成（ADD_MULTIPLIED_BASE） */
	private static final double STORM_SPEED_BONUS = 0.20D;
	/** 守护：击退抗性（ADD_VALUE，属性上限 1.0） */
	private static final double GUARD_KNOCKBACK_RESIST = 0.50D;
	/** 水：水下移速加成（ADD_MULTIPLIED_BASE） */
	private static final double WATER_SPEED_BONUS = 0.30D;
	/** 自然：静止判定阈值（毫秒） */
	private static final long NATURE_STILL_MS = 3000L;
	/** 自然：每秒回复量（1 HP = 半心） */
	private static final float NATURE_REGEN_HP = 1.0F;
	/** 锋刃连击：单层加成与层数上限（3 层 → +15%） */
	private static final float BLADE_COMBO_PER_HIT = 0.05F;
	private static final int BLADE_COMBO_MAX_HITS = 3;
	/** 锋刃连击窗口（毫秒） */
	private static final long BLADE_COMBO_WINDOW_MS = 3000L;

	// ============ 家族定义 ============

	public enum Family {
		SOUL("soul"), STORM("storm"), BLADE("blade"), NATURE("nature"),
		GUARD("guard"), WIND("wind"), FIRE("fire"), WATER("water");

		final TagKey<Enchantment> tag;

		Family(String path) {
			this.tag = TagKey.create(Registries.ENCHANTMENT, ExtraEnchantry.id("family_" + path));
		}
	}

	// ============ 状态 ============

	/** 玩家 UUID → 各家族档位（每次扫描刷新） */
	private static final Map<UUID, Map<Family, Tier>> TIERS = new ConcurrentHashMap<>();
	/** 玩家 UUID → 上次扫描的档位（升档检测用） */
	private static final Map<UUID, Map<Family, Tier>> PREV_TIERS = new ConcurrentHashMap<>();
	/** 自然扎根：静止起始时刻（毫秒） */
	private static final Map<UUID, Long> NATURE_STILL_SINCE = new ConcurrentHashMap<>();
	/** 通用上次位置（自然扎根 / 熔岩骑乘测距共用） */
	private static final Map<UUID, Vec3> LAST_POS = new ConcurrentHashMap<>();
	/** 劫火余生：熔岩骑乘累计距离（格） */
	private static final Map<UUID, Double> LAVA_RIDE_DISTANCE = new ConcurrentHashMap<>();

	/** 锋刃连击：玩家 UUID → 上次命中时刻 */
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
			scanPlayer(player);
			tickPassives(player);
		}
	}

	/** 扫描一名玩家：累计家族等级 → 档位 → 升档反馈 + RESONANCE 续期 + FULL 属性 */
	private static void scanPlayer(ServerPlayer player) {
		UUID playerId = player.getUUID();
		Map<Family, Integer> totals = new EnumMap<>(Family.class);
		for (Family family : Family.values()) {
			totals.put(family, 0);
		}
		// 装备四件 + 主副手：附魔按等级计入所属家族
		for (EquipmentSlot slot : new EquipmentSlot[]{
				EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
				EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
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
		// 档位
		Map<Family, Tier> tiers = new EnumMap<>(Family.class);
		for (Family family : Family.values()) {
			int total = totals.get(family);
			tiers.put(family, total >= FULL_THRESHOLD ? Tier.FULL : total >= PARTIAL_THRESHOLD ? Tier.PARTIAL : Tier.NONE);
		}
		Map<Family, Tier> prev = PREV_TIERS.put(playerId, tiers);
		TIERS.put(playerId, tiers);

		// 升档反馈（降档静默）
		ServerLevel level = (ServerLevel) player.level();
		int activeFamilies = 0;
		for (Family family : Family.values()) {
			Tier now = tiers.get(family);
			Tier before = prev != null ? prev.get(family) : Tier.NONE;
			if (now != Tier.NONE) {
				activeFamilies++;
			}
			if (now != Tier.NONE && now != before) {
				player.sendSystemMessage(Component.translatable(
						"message.extra-enchantry.resonance.tier_" + now.name().toLowerCase(),
						Component.translatable("family.extra-enchantry." + family.name().toLowerCase())));
				FxHelper.play(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
			}
		}
		// RESONANCE 效果滚动续期（状态栏展示，amplifier = 激活家族数 - 1）
		if (activeFamilies > 0) {
			player.addEffect(new MobEffectInstance(ExtraEnchantryEffects.RESONANCE,
					RESONANCE_REFRESH_TICKS, Math.max(0, activeFamilies - 1), true, false, true));
		} else if (player.hasEffect(ExtraEnchantryEffects.RESONANCE)) {
			player.removeEffect(ExtraEnchantryEffects.RESONANCE);
		}
		applyAttributePassives(player, tiers);
	}

	// ============ FULL 属性类被动 ============

	private static void applyAttributePassives(ServerPlayer player, Map<Family, Tier> tiers) {
		// 风暴 天象感应：雨天/雷暴移速 +20%
		boolean stormActive = tiers.get(Family.STORM) == Tier.FULL && player.level().isRaining();
		applyModifier(player, Attributes.MOVEMENT_SPEED, "storm_weather", stormActive ? STORM_SPEED_BONUS : 0.0D,
				AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		// 守护 岿然：击退抗性 +50%
		boolean guardActive = tiers.get(Family.GUARD) == Tier.FULL;
		applyModifier(player, Attributes.KNOCKBACK_RESISTANCE, "guard_resist",
				guardActive ? GUARD_KNOCKBACK_RESIST : 0.0D, AttributeModifier.Operation.ADD_VALUE);
		// 水 潮汐亲和：水下移速 +30%
		boolean waterActive = tiers.get(Family.WATER) == Tier.FULL && player.isEyeInFluid(FluidTags.WATER);
		applyModifier(player, Attributes.WATER_MOVEMENT_EFFICIENCY, "water_affinity",
				waterActive ? WATER_SPEED_BONUS : 0.0D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		// 水 氧气消耗减半：水下且空气未满时每秒补 max/40（原版 1 秒扣 15 → 净消耗约减半）
		if (waterActive && player.isEyeInFluid(FluidTags.WATER)
				&& player.getAirSupply() < player.getMaxAirSupply()) {
			player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + player.getMaxAirSupply() / 40));
		}
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

	private static void tickPassives(ServerPlayer player) {
		tickNatureRegen(player);
		tickLavaRide(player);
	}

	/** 自然 扎根：静止 3 秒后每秒回复 1 HP */
	private static void tickNatureRegen(ServerPlayer player) {
		boolean natureFull = tierOf(player, Family.NATURE) == Tier.FULL;
		long now = System.currentTimeMillis();
		Vec3 pos = player.position();
		Vec3 last = LAST_POS.put(player.getUUID(), pos);
		boolean moved = last != null && last.distanceToSqr(pos) > 0.001D;
		if (!natureFull || !player.isAlive() || player.getFoodData().getFoodLevel() <= 0
				|| player.getHealth() >= player.getMaxHealth() || moved) {
			NATURE_STILL_SINCE.put(player.getUUID(), now);
			return;
		}
		long since = NATURE_STILL_SINCE.getOrDefault(player.getUUID(), now);
		if (now - since >= NATURE_STILL_MS) {
			player.heal(NATURE_REGEN_HP);
			if (player.level() instanceof ServerLevel serverLevel) {
				FxHelper.burstAt(serverLevel, pos.x, pos.y + 0.3D, pos.z,
						ParticleTypes.COMPOSTER, 3, 0.2D);
			}
			// 每秒一次：本次回血后重置窗口
			NATURE_STILL_SINCE.put(player.getUUID(), now);
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

	// ============ 死亡结算入口（AFTER_DEATH 注册） ============

	/** 共鸣与挑战的死亡结算：灵魂被动（猎魂）+ 深渊回响挑战 */
	public static void onLivingDeath(LivingEntity victim, net.minecraft.world.damagesource.DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer killer)) {
			return;
		}
		Map<Family, Tier> tiers = TIERS.get(killer.getUUID());
		// 灵魂 猎魂：FULL 灵魂击杀生物后 3 秒隐身（对玩家击杀同样生效，设计未排除）
		if (tiers != null && tiers.get(Family.SOUL) == Tier.FULL) {
			killer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false, true));
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

	// ============ 小加成 API（§2.2，各 Manager 接入） ============

	/** 当前玩家是否至少处于该家族 PARTIAL 档 */
	public static boolean partialOrBetter(ServerPlayer player, Family family) {
		return tierOf(player, family) != Tier.NONE;
	}

	/** 玩家在该家族的档位（离线/无数据 → NONE） */
	public static Tier tierOf(ServerPlayer player, Family family) {
		Map<Family, Tier> tiers = TIERS.get(player.getUUID());
		return tiers != null ? tiers.getOrDefault(family, Tier.NONE) : Tier.NONE;
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

	/** 锋刃连击：命中时更新连击层并返回本次伤害倍率（FULL 锋刃专属，最高 +15%） */
	public static float bladeComboMultiplier(ServerPlayer attacker) {
		if (tierOf(attacker, Family.BLADE) != Tier.FULL) {
			return 1.0F;
		}
		long now = System.currentTimeMillis();
		Long last = BLADE_LAST_HIT.get(attacker.getUUID());
		int combo = (last != null && now - last <= BLADE_COMBO_WINDOW_MS)
				? Math.min(BLADE_COMBO.getOrDefault(attacker.getUUID(), 0) + 1, BLADE_COMBO_MAX_HITS)
				: 1;
		BLADE_LAST_HIT.put(attacker.getUUID(), now);
		BLADE_COMBO.put(attacker.getUUID(), combo);
		return 1.0F + BLADE_COMBO_PER_HIT * (combo - 1);
	}
}
