package realmikoto.extraenchantry.mixin;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import realmikoto.extraenchantry.EchoManager;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 放置方块声纹（§6.1）：幽渊内放置方块产生声纹（半径 7）。
 */
@Mixin(BlockItem.class)
public class BlockItemMixin {

	@Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
			at = @At("TAIL"))
	private void extraenchantry$echoOnPlace(BlockPlaceContext context,
			CallbackInfoReturnable<InteractionResult> cir) {
		InteractionResult result = cir.getReturnValue();
		if (result.consumesAction()
				&& context.getPlayer() instanceof ServerPlayer player) {
			EchoManager.emit(player, 7.0F);
		}
	}
}
