package realmikoto.extraenchantry.mixin;

import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import realmikoto.extraenchantry.ExtraEnchantry;

/**
 * 附魔等级限制（随机来源）：
 * enchant_randomly 战利品函数（宝箱附魔书、图书管理员交易等）原版在
 * [minLevel, maxLevel] 均匀随机等级。
 * - 触及/壁垒：仅能获得 1 级（钳到 minLevel），更高等级只能铁砧融合；
 * - 疾风：最高只能随机到 2 级，III 级仅能由两个 II 级在带破限的铁砧上融合。
 * 附魔台与钓鱼（enchant_with_levels）走 cost 窗口算法，
 * 已由各自 JSON 的 min_cost 曲线限制（触及/壁垒 level 2+ 需 cost≥105，疾风 level 3 需 cost≥45）。
 */
@Mixin(EnchantRandomlyFunction.class)
public abstract class EnchantRandomlyFunctionMixin {

	/** 疾风从随机来源可获得的最大等级 */
	private static final int EXTRAENCHANTRY$GALE_RANDOM_MAX_LEVEL = 2;

	@Redirect(
			method = "enchantItem",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I"
			)
	)
	private int extraenchantry$clampSingleLevelEnchantment(RandomSource random, int min, int max,
			ItemStack stack, Holder<Enchantment> enchantment, LootContext context) {
		if (enchantment.is(ExtraEnchantry.REACH) || enchantment.is(ExtraEnchantry.BULWARK)) {
			return min;
		}
		if (enchantment.is(ExtraEnchantry.GALE)) {
			return Mth.nextInt(random, min, Math.min(max, EXTRAENCHANTRY$GALE_RANDOM_MAX_LEVEL));
		}
		return Mth.nextInt(random, min, max);
	}
}
