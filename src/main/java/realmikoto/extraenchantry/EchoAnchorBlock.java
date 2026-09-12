package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 回响锚（v1.8.0 §3.4）：幽渊侧的锚定建筑——「用回响结晶 + 幽匿丝建造」。
 *
 * 放置于幽渊即注册为锚点（归属首个建立的玩家，§3.5）：
 * - 门进入「稳定」态（§3.3）：此后每次重开只需 1 枚回响碎片；
 * - 返回主世界时落点精确（未建锚 = 迷航：随机落点 + 失明 + 回声失聪，§3.4）。
 */
public class EchoAnchorBlock extends Block {

	public EchoAnchorBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
			boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (level instanceof ServerLevel server && !movedByPiston) {
			// 归属：最后一位放置者（多人协作时记录首建；putIfAbsent 保证首建归属）
			var nearest = server.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 8.0D, false);
			if (nearest != null) {
				AbyssRitual.registerAnchor(server, pos, nearest.getUUID());
			}
		}
	}
}
