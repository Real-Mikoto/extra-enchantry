package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.AABB;
import realmikoto.extraenchantry.entity.DecoyEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 假象（Decoy）状态管理：
 * 触发判定（1 秒判定间隔 + 全局冷却）、诱饵生成/销毁、活跃诱饵查询（供仇恨重定向）。
 *
 * 等级配置：
 *   I  : 25% 触发，1 个诱饵，8 生命，12 秒持续，25 秒冷却
 *   II : 40% 触发，1 个诱饵，16 生命，18 秒持续，20 秒冷却
 *   III: 55% 触发，2 个诱饵，20 生命，25 秒持续，15 秒冷却
 */
public final class DecoyManager {

	/** 等级参数（chance, count, health, durationTicks, cooldownMs） */
	private record DecoyConfig(float chance, int count, float health, int durationTicks, long cooldownMs) {
	}

	private static final DecoyConfig[] CONFIGS = {
			new DecoyConfig(0.25F, 1, 8.0F, 240, 25_000L),
			new DecoyConfig(0.40F, 1, 16.0F, 360, 20_000L),
			new DecoyConfig(0.55F, 2, 20.0F, 500, 15_000L)
	};

	/** 判定间隔（毫秒） */
	private static final long JUDGE_INTERVAL_MS = 10_000L;
	/** 阿西莫夫级联彩蛋概率：诱饵攻击主人和其他诱饵 */
	private static final float ASIMOV_CHANCE = 0.005F;
	/** 嵌套诱饵彩蛋概率：受击的诱饵生成自己的更弱诱饵 */
	private static final float NESTED_CHANCE = 0.05F;
	/** 嵌套深度上限（防无限链） */
	private static final int MAX_NESTING_DEPTH = 5;

	private static final Map<UUID, PlayerState> STATES = new HashMap<>();

	private static final class PlayerState {
		long lastJudgeMs;
		long cooldownUntilMs;
		final List<DecoyEntity> decoys = new ArrayList<>();
	}

	private DecoyManager() {
	}

	/** 读取玩家头盔上的假象附魔等级（无则 0） */
	public static int getDecoyLevel(net.minecraft.world.entity.player.Player player) {
		ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
		ItemEnchantments enchantments = helmet.getEnchantments();
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(ExtraEnchantry.DECOY)) {
				return enchantments.getLevel(enchantment);
			}
		}
		return 0;
	}

	/**
	 * 触发判定：内部包含 1 秒判定间隔与全局冷却。
	 * 判定通过则生成诱饵并进入冷却（冷却期间无论受多少次攻击都不会再次生成）。
	 */
	public static boolean tryTrigger(ServerPlayer player) {
		PlayerState state = STATES.computeIfAbsent(player.getUUID(), k -> new PlayerState());
		long now = System.currentTimeMillis();
		if (now < state.cooldownUntilMs) {
			return false;
		}
		if (now - state.lastJudgeMs < JUDGE_INTERVAL_MS) {
			return false;
		}
		int level = getDecoyLevel(player);
		if (level <= 0) {
			return false;
		}
		state.lastJudgeMs = now;

		DecoyConfig config = CONFIGS[level - 1];
		if (player.getRandom().nextFloat() >= config.chance()) {
			return false;
		}

		spawnDecoys(player, config, 0);
		state.cooldownUntilMs = now + config.cooldownMs();
		return true;
	}

	/** 生成一批诱饵（含阿西莫夫级联判定），并将主人周围的仇恨立即转移至首个诱饵 */
	private static void spawnDecoys(ServerPlayer player, DecoyConfig config, int nestingDepth) {
		ServerLevel level = (ServerLevel) player.level();
		boolean asimov = player.getRandom().nextFloat() < ASIMOV_CHANCE;

		DecoyEntity first = null;
		for (int i = 0; i < config.count(); i++) {
			double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 2.0;
			double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 2.0;
			double y = findSafeSpawnY(level, x, player.getY(), z);
			DecoyEntity decoy = new DecoyEntity(level, x, y, z);
			decoy.configure(player, config.health(), config.durationTicks(), nestingDepth, asimov);
			if (level.addFreshEntity(decoy)) {
				STATES.computeIfAbsent(player.getUUID(), k -> new PlayerState()).decoys.add(decoy);
				DecoyEntity.spawnHologramParticles(level, decoy.position());
				if (first == null) {
					first = decoy;
				}
			}
		}

		if (first != null) {
			retargetNearbyMobs(player, first);
		}
	}

	/**
	 * 从基准高度向上扫描安全落点（最多 4 格）：
	 * 诱饵碰撞箱（约 0.5×1.975 格）所在的两格必须是可穿透方块（空气等），
	 * 避免生成时嵌入方块导致敌对生物的攻击射线被阻挡。
	 */
	private static double findSafeSpawnY(ServerLevel level, double x, double baseY, double z) {
		int blockX = (int) Math.floor(x);
		int blockZ = (int) Math.floor(z);
		int baseBlockY = (int) Math.floor(baseY);
		for (int i = 0; i <= 4; i++) {
			int y = baseBlockY + i;
			if (level.getBlockState(new BlockPos(blockX, y, blockZ)).getCollisionShape(null, BlockPos.ZERO).isEmpty()
					&& level.getBlockState(new BlockPos(blockX, y + 1, blockZ)).getCollisionShape(null, BlockPos.ZERO).isEmpty()) {
				return y;
			}
		}
		return baseY;
	}

	/** 嵌套彩蛋：受击诱饵 5% 概率生成自己的诱饵（生命与持续减半，越来越弱） */
	public static void tryNestedSpawn(DecoyEntity parent) {
		if (parent.getNestingDepth() >= MAX_NESTING_DEPTH) {
			return;
		}
		Entity owner = parent.level().getPlayerByUUID(parent.getOwnerUuid());
		if (!(owner instanceof ServerPlayer player)) {
			return;
		}
		if (player.getRandom().nextFloat() >= NESTED_CHANCE) {
			return;
		}

		ServerLevel level = (ServerLevel) parent.level();
		float nestedHealth = Math.max(1.0F, parent.getBaseHealth() * 0.5F);
		int nestedDuration = Math.max(60, parent.getBaseDurationTicks() / 2);

		DecoyEntity nested = new DecoyEntity(level, parent.getX(), parent.getY(), parent.getZ());
		nested.configure(player, nestedHealth, nestedDuration, parent.getNestingDepth() + 1, false);
		if (level.addFreshEntity(nested)) {
			STATES.computeIfAbsent(player.getUUID(), k -> new PlayerState()).decoys.add(nested);
			DecoyEntity.spawnHologramParticles(level, nested.position());
			retargetNearbyMobs(player, nested);
		}
	}

	/** 销毁诱饵（超时或被摧毁），移出列表并散发全息粒子 */
	public static void destroyDecoy(DecoyEntity decoy, boolean byDamage) {
		if (decoy.level() instanceof ServerLevel level) {
			DecoyEntity.spawnHologramParticles(level, decoy.position());
		}
		PlayerState state = STATES.get(decoy.getOwnerUuid());
		if (state != null) {
			state.decoys.remove(decoy);
		}
		decoy.discard();
	}

	/** 玩家当前存活的诱饵（自动清理已失效项，返回不可变快照） */
	public static List<DecoyEntity> getAliveDecoys(ServerPlayer player) {
		return getDecoysOf(player.getUUID());
	}

	/** 指定主人 UUID 的存活诱饵（清理失效引用后返回不可变快照，避免外部共享可变状态） */
	public static List<DecoyEntity> getDecoysOf(UUID ownerUuid) {
		PlayerState state = STATES.get(ownerUuid);
		if (state == null) {
			return List.of();
		}
		state.decoys.removeIf(d -> d == null || d.isRemoved() || !d.isAlive());
		return List.copyOf(state.decoys);
	}

	/** 将主人周围正在仇恨其的怪物立即改判为追击指定诱饵 */
	private static void retargetNearbyMobs(ServerPlayer player, DecoyEntity decoy) {
		AABB box = player.getBoundingBox().inflate(32.0);
		List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, box, mob -> mob.getTarget() == player);
		for (Mob mob : mobs) {
			mob.setTarget(decoy);
		}
	}
}
