package realmikoto.extraenchantry.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import realmikoto.extraenchantry.ExtraEnchantry;

/**
 * 拓阶（Tier Break）基岩可挖掘：
 * 原版基岩 destroyTime = -1，getDestroyProgress 直接返回 0（永远不可破坏）。
 * 当玩家主手持下界合金镐且拓阶等级 >= 3 时，将破坏时间视为 100（黑曜石 50 的两倍），
 * 使其可被正常挖掘，且挖掘速度受效率、急迫等加成影响。
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {

	@Redirect(
			method = "getDestroyProgress",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroySpeed(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"
			)
	)
	private float extraenchantry$bedrockDestroySpeed(BlockState state, BlockGetter level, BlockPos pos,
			BlockState originalState, Player player, BlockGetter originalLevel, BlockPos originalPos) {
		if (state.is(Blocks.BEDROCK)) {
			ItemStack mainHand = player.getMainHandItem();
			if (mainHand.is(Items.NETHERITE_PICKAXE) && ExtraEnchantry.getTierBreakLevel(mainHand) >= 3) {
				// 黑曜石 destroyTime = 50，基岩为其两倍 = 100
				return 100.0F;
			}
		}
		return state.getDestroySpeed(level, pos);
	}
}
