package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 门的呼吸状态机（v1.8.0.2 完整实现，设计稿 §3.3）。
 *
 * 五态：
 * | 状态 | 表现 | 触发 |
 * | 闭合 CLOSED | 门框幽匿密合，无反应 | 初始 / 收束完成 |
 * | 待唤醒 AWAKENING | 门框缓慢起伏，漏出极淡幽光 | 完成点火（前 100 tick）|
 * | 开启 OPEN | 中央"黑暗"张开为可通行的门 | 待唤醒结束 |
 * | 收束 CONVERGING | 门缓慢闭合（粒子收缩） | 开启 1 主世界日 / 玩家离开 |
 * | 稳定 STABLE | 长期开启，低成本重开 | 幽渊侧建立回响锚后 |
 *
 * 实现：以「门框基点（GlobalPos）」为键记录状态与计时；每 20 tick 推进一次，
 * 状态切换时广播给附近玩家（音效 + 提示）。
 */
public final class AbyssGateState {

	public enum State {
		CLOSED, AWAKENING, OPEN, CONVERGING, STABLE
	}

	/** 门记录 */
	public static final class Gate {
		public State state;
		public int ticksInState;
		/** OPEN 态附近无玩家的持续 tick（§3.3「玩家离开触发收束」） */
		public int unattendedTicks;
		public final ResourceKey<Level> dimension;
		public final BlockPos base;

		Gate(State state, ResourceKey<Level> dimension, BlockPos base) {
			this.state = state;
			this.dimension = dimension;
			this.base = base;
		}
	}

	/** 开启持续时长：1 主世界日 */
	private static final int OPEN_DURATION = 24000;
	/** 待唤醒时长 */
	private static final int AWAKENING_TICKS = 100;
	/** 收束时长 */
	private static final int CONVERGING_TICKS = 200;
	/** 无人值守收束阈值：30 秒 */
	private static final int UNATTENDED_TICKS = 600;

	private static final Map<String, Gate> GATES = new ConcurrentHashMap<>();
	private static int counter = 0;

	private AbyssGateState() {
	}

	private static String key(ResourceKey<Level> dim, BlockPos base) {
		return dim.identifier() + "@" + base.asLong();
	}

	/** 点火成功：注册门并进入待唤醒 */
	public static void ignite(ServerLevel level, BlockPos base) {
		String k = key(level.dimension(), base);
		Gate gate = GATES.get(k);
		if (gate == null) {
			gate = new Gate(State.AWAKENING, level.dimension(), base);
			GATES.put(k, gate);
		} else {
			// 已存在（稳定态重开 / 收束中重启）
			gate.state = State.AWAKENING;
			gate.ticksInState = 0;
		}
		broadcast(level, base, "message.extra-enchantry.gate.state_awakening");
		FxHelper.playAt(level, base, SoundEvents.WARDEN_HEARTBEAT, 1.0F, 0.6F);
	}

	/** 幽渊侧建立回响锚 → 关联门进入稳定态 */
	public static void markStable(ServerLevel level, BlockPos base) {
		Gate gate = GATES.get(key(level.dimension(), base));
		if (gate != null) {
			gate.state = State.STABLE;
			gate.ticksInState = 0;
			broadcast(level, base, "message.extra-enchantry.gate.state_stable");
		}
	}

	/** 每 20 tick（ServerTickEvents 调用） */
	public static void tick(MinecraftServer server) {
		if (++counter < 20) {
			return;
		}
		counter = 0;
		GATES.entrySet().removeIf(entry -> {
			Gate gate = entry.getValue();
			ServerLevel level = server.getLevel(gate.dimension);
			if (level == null) {
				return true;
			}
			// 门框基点方块若已被破坏 → 移除记录（门消失）
			if (!AbyssGate.isFrameBlock(level.getBlockState(gate.base))
					&& !level.getBlockState(gate.base).is(AbyssGate.PORTAL_BLOCK)) {
				return true;
			}
			gate.ticksInState++;
			switch (gate.state) {
				case AWAKENING -> {
					// 起伏：粒子明暗 + 淡光
					if (gate.ticksInState % 10 == 0) {
						level.sendParticles(ParticleTypes.SCULK_SOUL,
								gate.base.getX() + 0.5, gate.base.getY() + 1, gate.base.getZ() + 0.5,
								2, 1.0, 1.0, 1.0, 0.0);
					}
					if (gate.ticksInState >= AWAKENING_TICKS) {
						gate.state = State.OPEN;
						gate.ticksInState = 0;
						broadcast(level, gate.base, "message.extra-enchantry.gate.state_open");
						FxHelper.playAt(level, gate.base, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.4F);
					}
				}
				case OPEN -> {
					// §3.3「玩家离开触发收束」：附近 64 格无玩家持续 30 秒 → 提前收束
					// （独立计数器——不占用 1 主世界日的开启总时长）
					if (gate.ticksInState % 20 == 0) {
						boolean near = level.players().stream()
								.anyMatch(p -> p.blockPosition().closerThan(gate.base, 64));
						gate.unattendedTicks = near ? 0 : gate.unattendedTicks + 20;
						if (gate.unattendedTicks >= UNATTENDED_TICKS) {
							gate.state = State.CONVERGING;
							gate.ticksInState = 0;
							broadcast(level, gate.base, "message.extra-enchantry.gate.state_converging");
							return false;
						}
					}
					// 开启 1 主世界日 → 收束；若已有玩家建立回响锚 → 转稳定
					if (hasStableAnchor(level)) {
						gate.state = State.STABLE;
						gate.ticksInState = 0;
						broadcast(level, gate.base, "message.extra-enchantry.gate.state_anchored");
					} else if (gate.ticksInState >= OPEN_DURATION) {
						gate.state = State.CONVERGING;
						gate.ticksInState = 0;
						broadcast(level, gate.base, "message.extra-enchantry.gate.state_converging");
					}
				}
				case CONVERGING -> {
					// 收束：粒子向内收缩
					if (gate.ticksInState % 5 == 0) {
						level.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
								gate.base.getX() + 0.5, gate.base.getY() + 1, gate.base.getZ() + 0.5,
								4, 1.5, 1.5, 1.5, -0.02);
					}
					if (gate.ticksInState >= CONVERGING_TICKS) {
						close(level, gate.base);
						broadcast(level, gate.base, "message.extra-enchantry.gate.state_closed");
						return true;
					}
				}
				case STABLE -> {
					// 稳定态：低频粒子
					if (gate.ticksInState % 40 == 0) {
						level.sendParticles(ParticleTypes.SCULK_SOUL,
								gate.base.getX() + 0.5, gate.base.getY() + 1, gate.base.getZ() + 0.5,
								1, 0.6, 0.6, 0.6, 0.0);
					}
				}
				default -> {
				}
			}
			return false;
		});
	}

	/** 关闭：清除门框区域内的门方块 */
	private static void close(ServerLevel level, BlockPos base) {
		for (int dx = -AbyssGate.MAX_FRAME; dx <= AbyssGate.MAX_FRAME; dx++) {
			for (int dy = -AbyssGate.MAX_FRAME; dy <= AbyssGate.MAX_FRAME; dy++) {
				for (int dz = -AbyssGate.MAX_FRAME; dz <= AbyssGate.MAX_FRAME; dz++) {
					BlockPos p = base.offset(dx, dy, dz);
					if (level.getBlockState(p).is(AbyssGate.PORTAL_BLOCK)) {
						level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
					}
				}
			}
		}
	}

	/** 是否存在任一玩家在幽渊建立了回响锚（§3.4：持久化于 AbyssRitual.AbyssRitualData） */
	private static boolean hasStableAnchor(ServerLevel level) {
		return AbyssGate.hasAnyAnchor(level);
	}

	private static void broadcast(ServerLevel level, BlockPos pos, String key) {
		for (ServerPlayer p : level.players()) {
			if (p.blockPosition().closerThan(pos, 64)) {
				p.sendSystemMessage(Component.translatable(key));
			}
		}
	}

	/**
	 * 门是否可通行（设计稿 §3.3）：仅 OPEN / STABLE 两态放行——
	 * 待唤醒（AWAKENING）与收束（CONVERGING）阶段禁止传送。
	 *
	 * @param portalPos 门方块位置（据此匹配所属门框基点）
	 */
	public static boolean isPassable(ServerLevel level, BlockPos portalPos) {
		for (Gate gate : GATES.values()) {
			if (!gate.dimension.equals(level.dimension())) {
				continue;
			}
			// 门框基点与门方块同属一个门（3D 曼哈顿距离在扫描半径内）
			if (gate.base.distManhattan(portalPos) <= MAX_GATE_SPAN) {
				return gate.state == State.OPEN || gate.state == State.STABLE;
			}
		}
		// 无记录（如重启后残留门方块）→ 放行，避免卡死玩家
		return true;
	}

	/** 门框基点与门方块的最大曼哈顿跨度（2×MAX_FRAME 保证覆盖整扇门） */
	private static final int MAX_GATE_SPAN = AbyssGateRules.MAX_FRAME * 2;

	public static State stateOf(ServerLevel level, BlockPos base) {
		Gate g = GATES.get(key(level.dimension(), base));
		return g == null ? State.CLOSED : g.state;
	}

	public static void onServerStopped() {
		GATES.clear();
	}
}
