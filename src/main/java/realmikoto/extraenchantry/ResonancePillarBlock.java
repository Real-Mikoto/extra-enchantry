package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 声纹柱（v1.8.0 §3.1）：深渊祭坛的八根家族柱，柱顶凹槽可嵌入回响碎片。
 *
 * 交互（三重共鸣仪式，判定在 {@link AbyssRitual}）：
 * - 手持回响碎片右键 → 嵌入（第一重 · 回响）；
 * - 空手右键 → 敲击（第二重 · 旋律，须按「封印旋律」顺序）。
 */
public class ResonancePillarBlock extends Block {

	public ResonancePillarBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
			BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level instanceof ServerLevel server && player instanceof net.minecraft.server.level.ServerPlayer sp) {
			if (stack.is(net.minecraft.world.item.Items.ECHO_SHARD)) {
				// 嵌入：仪式接受时消耗 1 枚回响碎片
				if (AbyssRitual.onPillarUse(server, pos, sp, stack)) {
					stack.shrink(1);
					return InteractionResult.SUCCESS;
				}
				return InteractionResult.FAIL;
			}
			// 非碎片物品持握视为敲击
			AbyssRitual.onPillarUse(server, pos, sp, ItemStack.EMPTY);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.CONSUME;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel server && player instanceof net.minecraft.server.level.ServerPlayer sp) {
			// 敲击（空手 / 非碎片物品）
			AbyssRitual.onPillarUse(server, pos, sp, ItemStack.EMPTY);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.CONSUME;
	}
}
