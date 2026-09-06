package realmikoto.extraenchantry.client.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.client.CollectorState;

/**
 * 臻藏光晕（1.2.0 收藏家奖励）：本地玩家获得臻藏资格后，
 * 其背包中带本模组附魔的附魔书恒定显示附魔光效
 * （含未附魔书袋内的空白书——臻藏专属的"会发光的收藏"）。
 *
 * 26.2 光效判定在 {@code ItemStack#hasFoil}（enchantment_glint_override
 * 组件优先，否则走 isFoil）。客户端按本地臻藏标记改写返回值。
 * 自定义颜色的 glint 渲染层为后续增强，本版使用原版光效外观。
 */
@Mixin(ItemStack.class)
public abstract class ItemStackFoilMixin {

	@Inject(method = "hasFoil", at = @At("RETURN"), cancellable = true)
	private void extraenchantry$collectorGlint(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			return;
		}
		ItemStack self = (ItemStack) (Object) this;
		if (!self.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) {
			return;
		}
		var stored = self.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);
		if (stored == null || stored.isEmpty()) {
			return;
		}
		for (var holder : stored.keySet()) {
			if (holder.unwrapKey().isPresent()
					&& holder.unwrapKey().get().identifier().getNamespace().equals("extra-enchantry")) {
				if (CollectorState.isCollector()) {
					cir.setReturnValue(true);
				}
				return;
			}
		}
	}
}
