package realmikoto.extraenchantry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 庇护（Sanctuary）：格挡时的团队治疗光环。
 *
 * 玩家格挡（isBlocking）且格挡中的盾牌带庇护时：每 2 秒对 8 格内所有玩家
 * （含持有者）回复 1/2/4 HP（I/II/III），每次脉冲消耗盾牌 1 点耐久
 * （耐久耗尽格挡自然中断，光环随之停止）。
 *
 * 视觉：受疗玩家头顶心形粒子 + 持有者周身环绕光点（服务端 sendParticles 广播）。
 * 脉冲计时按玩家维度 gameTime 记录；首次举盾起算 2 秒后才首次脉冲。
 */
public final class SanctuaryManager {

	/** 脉冲间隔（tick）＝ 2 秒 */
	private static final int PULSE_INTERVAL_TICKS = 40;

	/** 各等级每次脉冲的回复量（下标 = 等级 - 1） */
	private static final float[] HEAL_PER_PULSE = {1.0F, 2.0F, 4.0F};

	/** 每次脉冲消耗的盾牌耐久 */
	private static final int DURABILITY_PER_PULSE = 1;

	/** 光环半径平方（8 格） */
	private static final double AURA_RADIUS_SQR = 64.0D;

	/** 玩家 UUID → 下次脉冲时刻（gameTime） */
	private static final Map<UUID, Long> NEXT_PULSE = new ConcurrentHashMap<>();

	private SanctuaryManager() {
	}

	/** 每 tick 判定（仅服务端玩家），到点执行治疗脉冲 */
	public static void tick(ServerPlayer player) {
		if (player.isDeadOrDying() || !player.isBlocking()) {
			return;
		}
		ItemStack shield = player.getItemBlockingWith();
		if (shield == null || !shield.is(Items.SHIELD)) {
			return;
		}
		int level = ExtraEnchantry.getSanctuaryLevel(shield);
		if (level <= 0 || level > HEAL_PER_PULSE.length) {
			return;
		}
		long now = player.level().getGameTime();
		Long next = NEXT_PULSE.get(player.getUUID());
		if (next == null) {
			NEXT_PULSE.put(player.getUUID(), now + PULSE_INTERVAL_TICKS);
			return;
		}
		if (now < next) {
			return;
		}
		NEXT_PULSE.put(player.getUUID(), now + PULSE_INTERVAL_TICKS);
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		float heal = HEAL_PER_PULSE[level - 1];
		for (ServerPlayer target : serverLevel.players()) {
			if (!target.isAlive() || target.isSpectator()
					|| target.distanceToSqr(player) > AURA_RADIUS_SQR) {
				continue;
			}
			target.heal(heal);
			serverLevel.sendParticles(ParticleTypes.HEART,
					target.getX(), target.getY(1.2D), target.getZ(), 2, 0.3D, 0.4D, 0.3D, 0.0D);
		}
		// 光环视觉：持有者周身环绕光点
		serverLevel.sendParticles(ParticleTypes.END_ROD,
				player.getX(), player.getY(0.6D), player.getZ(), 8, 0.7D, 0.3D, 0.7D, 0.01D);
		// 持续消耗耐久
		shield.hurtAndBreak(DURABILITY_PER_PULSE, player, player.getUsedItemHand().asEquipmentSlot());
	}
}
