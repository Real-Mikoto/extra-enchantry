package realmikoto.extraenchantry.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.ExtraEnchantry;
import realmikoto.extraenchantry.LimitBreakManager;

/**
 * 破限书获取检测兜底：反编译 GiveCommand#giveItem 确认，/give 直接调用
 * {@code Inventory#add}（不经过 Player#addItem），命令发的书不会走
 * PlayerMixin 的检测。在此补挂同一入口。
 * 与 PlayerMixin#addItem 对拾取路径会各触发一次——进度授予幂等 +
 * 诸界浩劫每玩家 60 秒刷新保护，重复调用无副作用。
 * 异常自吞并记日志：检测失败不允许打断物品入包/命令执行。
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {

	@Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"))
	private void extraenchantry$detectObtainedBook(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		try {
			Inventory inventory = (Inventory) (Object) this;
			if (inventory.player instanceof ServerPlayer serverPlayer) {
				LimitBreakManager.onItemObtained(serverPlayer, stack);
			}
		} catch (Exception e) {
			ExtraEnchantry.LOGGER.error("[extra-enchantry] 破限书获取检测(Inventory#add)异常", e);
		}
	}
}
