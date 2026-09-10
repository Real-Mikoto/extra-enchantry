package realmikoto.extraenchantry;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 虚闪（Voidblink，45 号・末地境）：被弹射物（AbstractArrow 系）命中前，
 * 10%/级 概率瞬移至侧后 4 格并免疫该次伤害（末影人式的箭矢闪避）。
 *
 * 约束（1.6.0 §2.5）：
 *   - 激光 / 药水等非 AbstractArrow 伤害不可闪避（克制的留白）；
 *   - 内置 3 s 触发冷却（60 tick），防连锁触发；
 *   - 侧移落点自写安全算法（EnderMan#teleport 为 protected，不复制——§5.3 结论 3）。
 */
public final class VoidblinkManager {

	/** 触发内冷却（tick）＝3 s */
	private static final int INTERNAL_COOLDOWN_TICKS = 60;

	/** 侧移距离（格） */
	private static final double DODGE_DISTANCE = 4.0D;

	/** 内冷却表（玩家 UUID → 到期 gameTime） */
	private static final Map<UUID, Long> BLINK_READY_AT = new ConcurrentHashMap<>();

	private VoidblinkManager() {
	}

	/** 玩家登出清理（DISCONNECT 调用） */
	public static void onDisconnect(UUID playerId) {
		BLINK_READY_AT.remove(playerId);
	}

	/** 胸甲上的虚闪等级（0 = 无） */
	public static int blinkLevel(ServerPlayer player) {
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		ItemEnchantments enchantments = chest.getEnchantments();
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(ExtraEnchantry.VOIDBLINK)) {
				return enchantments.getLevel(enchantment);
			}
		}
		return 0;
	}

	/** 闪避判定：内冷却已过且概率命中（10%/级） */
	public static boolean tryBlink(ServerPlayer player, int level) {
		long now = player.level().getGameTime();
		Long readyAt = BLINK_READY_AT.get(player.getUUID());
		if (readyAt != null && now < readyAt) {
			return false;
		}
		// 10%/级：I 级 10% / II 级 20%
		if (player.getRandom().nextDouble() >= level * 0.10D) {
			return false;
		}
		BLINK_READY_AT.put(player.getUUID(), now + INTERNAL_COOLDOWN_TICKS);
		sideStepTeleport(player);
		return true;
	}

	/**
	 * 侧移瞬移：目标侧后 4 格的安全落点（自写算法）。
	 * 取视线垂直方向（左右各试）+ 落点实体不悬空、脚下方块可站立——
	 * 三候选（右侧 / 左侧 / 正后）按序探测，全部失败则原地（仍取消伤害）。
	 */
	private static void sideStepTeleport(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 look = player.getLookAngle();
		Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize();
		Vec3[] candidates = {
				player.position().add(side.scale(DODGE_DISTANCE)).subtract(look.scale(1.0D)),
				player.position().subtract(side.scale(DODGE_DISTANCE)).subtract(look.scale(1.0D)),
				player.position().subtract(look.scale(DODGE_DISTANCE)),
		};
		for (Vec3 target : candidates) {
			int x = (int) Math.floor(target.x);
			int z = (int) Math.floor(target.z);
			for (int y = (int) Math.floor(player.getY()) + 2; y >= (int) Math.floor(player.getY()) - 2; y--) {
				var floor = level.getBlockState(new net.minecraft.core.BlockPos(x, y - 1, z));
				var head = level.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
				var above = level.getBlockState(new net.minecraft.core.BlockPos(x, y + 1, z));
				if (!floor.isSolid() || !head.isAir() || !above.isAir()) {
					continue;
				}
				player.teleportTo(x + 0.5D, y, z + 0.5D);
				return;
			}
		}
		// 三候选全失败：原地闪避（伤害仍取消——虚闪的"免伤"部分始终兑现）
	}

	/** 闪避反馈（L2）：末影粒子环 + 末影人瞬移音 */
	public static void dodgeFx(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		FxHelper.ring(level, player, 1.5D, ParticleTypes.PORTAL, 12);
		FxHelper.play(level, player, SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.2F);
	}
}
