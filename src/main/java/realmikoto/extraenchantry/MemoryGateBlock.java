package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 记忆之门（v1.8.0.2，设计稿 §6.4）：右键进入可玩闪回。
 */
public class MemoryGateBlock extends Block {

	public MemoryGateBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (level.isClientSide() || !(player instanceof ServerPlayer sp)) {
			return InteractionResult.SUCCESS;
		}
		if (!AbyssKey.isIn(sp)) {
			sp.sendSystemMessage(Component.literal("§7记忆之门在光下沉睡——它只开在幽渊。"));
			return InteractionResult.FAIL;
		}
		// 依序进入未看过的记忆
		int next = 1;
		for (int i = 1; i <= 3; i++) {
			if (!LoreTriggerManager.hasFired(sp, "abyss_memory_" + i)) {
				next = i;
				break;
			}
			next = 1;
		}
		LoreTriggerManager.fireOnce(sp, "abyss_flashback_" + next);
		MemoryFlashback.enter(sp, next);
		return InteractionResult.SUCCESS;
	}
}
