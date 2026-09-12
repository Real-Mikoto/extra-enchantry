package realmikoto.extraenchantry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幽渊附魔结算（1.8.3「藏」）：附魔 47–52 的运行时效果。
 *
 * 数据定义在 data/extra-enchantry/enchantment/*.json（47 echo / 48 stillness /
 * 49 deepdive / 50 reverie / 51 resonance_ench / 52 aetheric_inscription），
 * 本类集中处理效果注入（与 AccessoryManager 的静态入口模式一致）。
 *
 * 效果清单（对齐 DESIGN/1.8.0-design.md §5.3）：
 * - 回声 Echo（胸甲）：受击时以声纹标记攻击者——对其显形（GLOWING）+ 缓慢（失聪拟态），
 *   且被标记期间玩家对其伤害 +15%/级（§5.3「对其增伤」）；
 * - 静默 Stillness（靴子）：移动完全不产生声纹（§5.3「移动不产生声纹」）；
 *   代价：自身失去回声视觉（客户端光照脉冲与回响灯自我显形均被抑制）；
 * - 深潜 Deepdive（头盔）：深息时间 ×(1 + 0.5/级)（效果近似：水中持续补给 OXYGEN 补偿）；
 *   无光环境获得夜视（黑暗环境中触发）；
 * - 溯忆 Reverie（任意护甲）：死亡时原地留下记忆残影（拾回 15%/级 经验）；
 * - 共鸣 Resonance（武器）：命中产生声纹（8 + 2/级）并使目标显形 + 缓慢 I（失聪拟态）；
 * - 无相之铭 Aetheric Inscription（任意护甲，T0）：穿戴者声纹完全隐匿（EchoManager 查询）。
 */
public final class AbyssEnchantments {

	private AbyssEnchantments() {
	}

	// ============ 回声标记（§5.3「对其增伤」） ============

	/** 被声纹标记的实体：显形 + 缓慢持续期间，玩家对其伤害加成 */
	public record EchoMark(long untilTick, int level) {
	}

	private static final Map<UUID, EchoMark> MARKED = new ConcurrentHashMap<>();

	/** 受击声纹反击（LivingEntityMixin#hurtServer AFTER 分支调用）：显形 + 失聪 + 标记增伤 */
	private static void markAttacker(ServerPlayer player, LivingEntity attacker, long now) {
		int level = getLevel(player, EquipmentSlot.CHEST, ExtraEnchantry.ECHO);
		if (level > 0) {
			// 声纹反击：攻击者显形并短暂失聪（缓慢拟态）
			attacker.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60 + 20 * level, 0, false, true));
			attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40 + 20 * level, 0, false, true));
			MARKED.put(attacker.getUUID(), new EchoMark(now + 60 + 20L * level, level));
			if (player.level() instanceof ServerLevel server) {
				FxHelper.ring(server, attacker, 6.0D, ParticleTypes.SONIC_BOOM, 8);
				FxHelper.play(server, player, SoundEvents.SCULK_CLICKING, 1.0F, 1.4F);
			}
		}
	}

	/**
	 * 回声增伤（§5.3）：目标处于声纹标记期 → 玩家对其伤害 ×(1 + 0.15/级)。
	 * LivingEntityMixin 修伤路径调用。
	 */
	public static float echoDamageBonus(ServerPlayer attacker, LivingEntity target) {
		EchoMark mark = MARKED.get(target.getUUID());
		if (mark == null) {
			return 1.0F;
		}
		if (attacker.level().getGameTime() > mark.untilTick()) {
			MARKED.remove(target.getUUID());
			return 1.0F;
		}
		return 1.0F + 0.15F * mark.level();
	}

	/** LivingEntityMixin#hurtServer 的 AFTER 分支调用（受害者 = player） */
	public static void onPlayerHurt(ServerPlayer player, LivingEntity attacker) {
		if (attacker == null || !AbyssKey.isIn(player)) {
			return;
		}
		markAttacker(player, attacker, player.level().getGameTime());
	}

	/** 共鸣武器命中（近战伤害结算处调用） */
	public static void onWeaponHit(ServerPlayer attacker, LivingEntity target) {
		int level = getLevelOnMainhand(attacker, ExtraEnchantry.RESONANCE_ENCH);
		if (level > 0) {
			EchoManager.emit(attacker, 8.0F + 2.0F * level);
			target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40 + 20 * level, 0, false, true));
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 0, false, true));
		}
	}

	/** 是否具备深潜手段（深潜附魔或深息面罩）——供渊族试炼联动判定 */
	public static boolean hasDeepdive(Player player) {
		return getLevel(player, EquipmentSlot.HEAD, ExtraEnchantry.DEEPDIVE) > 0
				|| AbyssGearItem.holds(player, AbyssGearItem.Effect.DEEPDIVE);
	}

	/**
	 * 静默 Stillness（靴子）检测（EchoManager 移动声纹路径查询）：
	 * 移动完全不产生声纹；代价：失去回声视觉（客户端 + 回响灯两侧抑制）。
	 */
	public static boolean hasStillness(Player player) {
		return getLevel(player, EquipmentSlot.FEET, ExtraEnchantry.STILLNESS) > 0;
	}

	/** 无相之铭：声纹完全隐匿（EchoManager.emit 入口查询） */
	public static boolean hasAethericInscription(Player player) {
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
				EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			if (getLevel(player, slot, ExtraEnchantry.AETHERIC_INSCRIPTION) > 0) {
				return true;
			}
		}
		return false;
	}

	/** 深潜 Deepdive：黑暗环境夜视（EchoManager per-player tick 调用，幽渊内持续补给） */
	public static void applyDeepdive(ServerPlayer player) {
		int level = getLevel(player, EquipmentSlot.HEAD, ExtraEnchantry.DEEPDIVE);
		// 深息面罩（装备持有）等价 I 级深潜
		if (level == 0 && AbyssGearItem.holds(player, AbyssGearItem.Effect.DEEPDIVE)) {
			level = 1;
		}
		if (level > 0 && player.level() instanceof ServerLevel server) {
			// 永续 3s 的夜视补给（幽渊无光环境）+ 零伤害水环境呼吸补偿
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 60, 0, true, false, false));
			boolean submerged = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)
					|| LiquidSculkBlock.isInLiquidSculk(player);
			if (submerged) {
				player.setAirSupply(Math.min(player.getMaxAirSupply(),
						player.getAirSupply() + 20 * level));
			}
		}
	}

	/** 溯忆 Reverie：死亡经验拾回比例（0.15/级；ServerLivingEntityEvents.AFTER_DEATH 调用） */
	public static float reverieExperienceKeep(ServerPlayer player) {
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
				EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			int level = getLevel(player, slot, ExtraEnchantry.REVERIE);
			if (level > 0) {
				return 0.15F * level;
			}
		}
		return 0.0F;
	}

	// ============ 附魔等级查询（26.2 约定：getEnchantments 遍历 + holder.is(KEY)） ============

	private static int getLevel(Player player, EquipmentSlot slot, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
		return levelOf(player.getItemBySlot(slot), key);
	}

	private static int getLevelOnMainhand(Player player, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
		return levelOf(player.getMainHandItem(), key);
	}

	private static int levelOf(ItemStack stack, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
		if (stack == null || stack.isEmpty()) {
			return 0;
		}
		var enchantments = stack.getEnchantments();
		for (var holder : enchantments.keySet()) {
			if (holder.is(key)) {
				return enchantments.getLevel(holder);
			}
		}
		return 0;
	}

	/** 服务器停止清理（ExtraEnchantry SERVER_STOPPED 调用） */
	public static void onServerStopped() {
		MARKED.clear();
	}
}
