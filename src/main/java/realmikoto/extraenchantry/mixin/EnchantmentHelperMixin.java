package realmikoto.extraenchantry.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.ExtraEnchantry;

import java.util.List;
import java.util.stream.Stream;

/**
 * 破限（Limit Break）附魔台逻辑：
 * 对带破限的物品再次附魔时，跳过互斥过滤，使互斥附魔可同时出现在附魔台选项中。
 * selectEnchantment 是静态方法且 filterCompatibleEnchantments 无物品上下文，
 * 故通过 ThreadLocal 传递当前物品。
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

	private static final ThreadLocal<ItemStack> extraenchantry$enchantingStack = new ThreadLocal<>();

	@Inject(method = "selectEnchantment", at = @At("HEAD"))
	private static void extraenchantry$beginSelect(RandomSource random, ItemStack stack, int cost,
			Stream<Holder<Enchantment>> stream, CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
		extraenchantry$enchantingStack.set(stack);
	}

	@Inject(method = "selectEnchantment", at = @At("RETURN"))
	private static void extraenchantry$endSelect(RandomSource random, ItemStack stack, int cost,
			Stream<Holder<Enchantment>> stream, CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
		extraenchantry$enchantingStack.remove();
	}

	@Inject(method = "filterCompatibleEnchantments", at = @At("HEAD"), cancellable = true)
	private static void extraenchantry$skipCompatibilityFilter(List<EnchantmentInstance> list,
			EnchantmentInstance instance, CallbackInfo ci) {
		ItemStack stack = extraenchantry$enchantingStack.get();
		if (stack != null && ExtraEnchantry.hasLimitBreak(stack)) {
			ci.cancel();
		}
	}
}
