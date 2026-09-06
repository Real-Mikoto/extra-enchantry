package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;
import java.util.Map;

/**
 * 丰壤（Loam）：锄头专属的农业附魔。
 *
 * 效果（I/II/III 级）：收获完全成熟的作物时 20%/35%/50% 概率双倍掉落
 * （复制一份含时运等加成后的完整掉落，含种子）；III 级额外 3×3 范围收获
 * ——仅破坏同种且已成熟的作物，未成熟的不动，每个邻格各自走双倍判定。
 *
 * 注入点：{@code Block#playerDestroy} RETURN（玩家破坏结算完成、原版掉落已生成）。
 * 双倍判定通过 {@code Block.getDrops} 重新计算一份掉落并弹出，不影响原版流程。
 * 范围收获经 {@code ServerLevel#destroyBlock} 走原版破坏流程，邻格的
 * playerDestroy 会再次进入本管理器——双倍判定照常生效，
 * 但范围扩展由 ThreadLocal 深度标记短路（防连锁扩散）。
 */
public final class LoamManager {

	/** 各等级的双倍掉落概率（下标 = 等级 - 1） */
	private static final float[] DOUBLE_DROP_CHANCE = {0.20F, 0.35F, 0.50F};

	/** 范围收获深度标记：true = 当前正在处理邻格，不再向外扩展 */
	private static final ThreadLocal<Boolean> AREA_HARVESTING = new ThreadLocal<>();

	/** 隐秘挑战「丰收之神的赞许」：玩家 UUID → 连锁收获累计株数与最近收获时刻 */
	private static final Map<UUID, Integer> CHAIN_COUNTS = new java.util.concurrent.ConcurrentHashMap<>();
	private static final Map<UUID, Long> CHAIN_LAST_MS = new java.util.concurrent.ConcurrentHashMap<>();
	/** 连锁窗口（毫秒）：超过则重新计数 */
	private static final long CHAIN_WINDOW_MS = 3000L;
	/** 挑战阈值：单次连锁 64 株 */
	private static final int CHAIN_CHALLENGE_COUNT = 64;

	private LoamManager() {
	}

	/** 玩家破坏方块后调用（BlockMixin 注入点），仅处理成熟作物 + 丰壤锄头 */
	public static void onCropHarvest(Level level, Player player, BlockPos pos, BlockState state,
			BlockEntity blockEntity, ItemStack tool) {
		if (!(level instanceof ServerLevel serverLevel)
				|| !(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) {
			return;
		}
		int loamLevel = ExtraEnchantry.getLoamLevel(tool);
		if (loamLevel <= 0 || loamLevel > DOUBLE_DROP_CHANCE.length) {
			return;
		}
		// 隐秘挑战计数：连锁窗口内累计收获株数（中心 + 邻格都算）
		if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			long now = System.currentTimeMillis();
			long last = CHAIN_LAST_MS.getOrDefault(serverPlayer.getUUID(), 0L);
			int count = (now - last <= CHAIN_WINDOW_MS)
					? CHAIN_COUNTS.merge(serverPlayer.getUUID(), 1, Integer::sum) : 1;
			CHAIN_LAST_MS.put(serverPlayer.getUUID(), now);
			FamilyResonanceManager.onLoamChainCrop(serverPlayer, count);
			if (count >= CHAIN_CHALLENGE_COUNT) {
				CHAIN_COUNTS.remove(serverPlayer.getUUID());
			}
		}
		// 双倍掉落：重算一份完整掉落（含时运上下文）弹出
		if (player.getRandom().nextFloat() < DOUBLE_DROP_CHANCE[loamLevel - 1]) {
			List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, blockEntity, player, tool);
			for (ItemStack drop : drops) {
				Block.popResource(serverLevel, pos, drop);
			}
			// 丰收反馈（P1）：绿星 + 堆肥"啵"声（3×3 多格连响自然形成"哗啦一片"）
			FxHelper.burstAt(serverLevel, pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D,
					net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, 5, 0.3D);
			FxHelper.play(serverLevel, player, net.minecraft.sounds.SoundEvents.COMPOSTER_FILL_SUCCESS,
					0.6F, 1.2F);
			// 实战成就「丰收时刻」：丰壤首次双倍收获
			if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
				Advancements.award(serverPlayer, Advancements.HARVEST);
			}
		}
		// III 级 3×3 范围收获（仅同种且成熟的邻格）
		if (loamLevel >= 3 && !Boolean.TRUE.equals(AREA_HARVESTING.get())) {
			harvestArea(serverLevel, player, pos, crop, tool);
		}
	}

	/** 3×3 范围收获：邻格走原版破坏流程（触发各自的双倍判定），每格消耗 1 点耐久 */
	private static void harvestArea(ServerLevel level, Player player, BlockPos center,
			CropBlock crop, ItemStack tool) {
		AREA_HARVESTING.set(Boolean.TRUE);
		try {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dz == 0) {
						continue;
					}
					BlockPos neighbor = center.offset(dx, 0, dz);
					BlockState neighborState = level.getBlockState(neighbor);
					if (neighborState.getBlock() == crop && crop.isMaxAge(neighborState)
							&& level.destroyBlock(neighbor, true, player) && !player.isCreative()) {
						tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
					}
				}
			}
		} finally {
			AREA_HARVESTING.remove();
		}
	}
}
