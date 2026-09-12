package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 尖啸核心（v1.8.0 §3.1）：祭坛中心的巨型幽匿尖啸体。
 *
 * 第三重 · 尖啸：前两重完成后右键激活——引发「深渊回响」，
 * 召唤监守者（60 秒倒计时压力），玩家须站定在门框内完成开门（§3.2）。
 */
public class ShriekCoreBlock extends Block {

	public ShriekCoreBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel server && player instanceof net.minecraft.server.level.ServerPlayer sp) {
			AbyssRitual.onCoreUse(server, pos, sp);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.CONSUME;
	}
}
