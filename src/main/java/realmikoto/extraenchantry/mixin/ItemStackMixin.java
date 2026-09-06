package realmikoto.extraenchantry.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.ExtraEnchantry;
import realmikoto.extraenchantry.TierBreakRules;

/**
 * 拓阶（Tier Break）挖掘等级提升：
 * 每级 +1 挖掘等级，使低级工具可以采集本不可掉落的方块（如木镐+1 可挖铁矿石），并以正常工具速度挖掘。
 * 等级体系（由 incorrect_for_X_tool 标签嵌套关系推导）：
 * 木/金=0，石/铜=1，铁=2，钻石/下界合金=3。
 * 基岩特殊：下界合金镐 + 3 级拓阶 才视为有效工具（掉落判定与挖掘速度）。
 * 规则本体在 {@link TierBreakRules}（与 BlockMixin 的拓阶特效触发共用同一套判定）。
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

	@Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$tierBreakDrops(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		ItemStack stack = (ItemStack) (Object) this;
		int level = ExtraEnchantry.getTierBreakLevel(stack);
		if (level <= 0) {
			return;
		}

		// 基岩：下界合金镐 + 3 级拓阶视为有效工具（有掉落）
		if (state.is(Blocks.BEDROCK)) {
			if (TierBreakRules.canMineBedrock(stack, level)) {
				cir.setReturnValue(true);
			}
			return;
		}

		// 常规挖掘等级提升
		if (TierBreakRules.isBoostedHarvestable(stack, state, level)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$tierBreakSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
		ItemStack stack = (ItemStack) (Object) this;
		int level = ExtraEnchantry.getTierBreakLevel(stack);
		if (level <= 0) {
			return;
		}

		Tool tool = stack.get(DataComponents.TOOL);
		if (tool == null) {
			return;
		}

		// 基岩：返回镐子的正常挖掘速度（使效率等加成生效）
		if (state.is(Blocks.BEDROCK)) {
			if (TierBreakRules.canMineBedrock(stack, level)) {
				cir.setReturnValue(TierBreakRules.getMinesSpeed(tool));
			}
			return;
		}

		// 挖掘等级提升后：以正常工具速度挖掘（否则错误等级会退化为手速）
		if (TierBreakRules.isBoostedHarvestable(stack, state, level)) {
			cir.setReturnValue(TierBreakRules.getMinesSpeed(tool));
		}
	}
}
