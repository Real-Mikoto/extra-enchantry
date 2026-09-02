package realmikoto.extraenchantry.mixin;

import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.ExtraEnchantry;

/**
 * 拓阶（Tier Break）挖掘等级提升：
 * 每级 +1 挖掘等级，使低级工具可以采集本不可掉落的方块（如木镐+1 可挖铁矿石），并以正常工具速度挖掘。
 * 等级体系（由 incorrect_for_X_tool 标签嵌套关系推导）：
 * 木/金=0，石/铜=1，铁=2，钻石/下界合金=3。
 * 基岩特殊：下界合金镐 + 3 级拓阶 才视为有效工具（掉落判定与挖掘速度）。
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

	/** 根据工具的 incorrect 标签返回其挖掘等级（0-3），未知标签返回 -1 */
	private static int extraenchantry$tierOf(TagKey<Block> tag) {
		if (tag.equals(BlockTags.INCORRECT_FOR_WOODEN_TOOL) || tag.equals(BlockTags.INCORRECT_FOR_GOLD_TOOL)) {
			return 0;
		}
		if (tag.equals(BlockTags.INCORRECT_FOR_STONE_TOOL) || tag.equals(BlockTags.INCORRECT_FOR_COPPER_TOOL)) {
			return 1;
		}
		if (tag.equals(BlockTags.INCORRECT_FOR_IRON_TOOL)) {
			return 2;
		}
		if (tag.equals(BlockTags.INCORRECT_FOR_DIAMOND_TOOL) || tag.equals(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)) {
			return 3;
		}
		return -1;
	}

	/** 有效挖掘等级对应的 incorrect 限制标签（3 级及以上无限制，返回 null） */
	private static TagKey<Block> extraenchantry$restrictionFor(int effectiveTier) {
		return switch (Math.min(effectiveTier, 3)) {
			case 0 -> BlockTags.INCORRECT_FOR_WOODEN_TOOL;
			case 1 -> BlockTags.INCORRECT_FOR_STONE_TOOL;
			case 2 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
			default -> null;
		};
	}

	/** 获取工具"挖掘规则"（correctForDrops=true 的规则）的方块集合，无则 null */
	private static HolderSet<Block> extraenchantry$getMinesBlocks(Tool tool) {
		for (Tool.Rule rule : tool.rules()) {
			if (rule.correctForDrops().isPresent() && rule.correctForDrops().get()) {
				return rule.blocks();
			}
		}
		return null;
	}

	/** 获取工具"挖掘规则"的速度（如镐子的材质速度），无则 1.0 */
	private static float extraenchantry$getMinesSpeed(Tool tool) {
		for (Tool.Rule rule : tool.rules()) {
			if (rule.correctForDrops().isPresent() && rule.correctForDrops().get()) {
				return rule.speed().orElse(1.0F);
			}
		}
		return 1.0F;
	}

	/** 基岩特殊判定：下界合金镐 + 3 级拓阶 */
	private static boolean extraenchantry$canMineBedrock(ItemStack stack, int level) {
		return level >= 3 && stack.is(Items.NETHERITE_PICKAXE);
	}

	/** 判断拓阶生效后该方块是否可正常掉落（工具类别匹配且有效挖掘等级足够） */
	private static boolean extraenchantry$isBoostedHarvestable(ItemStack stack, BlockState state, int level) {
		Tool tool = stack.get(DataComponents.TOOL);
		if (tool == null) {
			return false;
		}

		HolderSet<Block> minesBlocks = extraenchantry$getMinesBlocks(tool);
		if (minesBlocks == null || !state.is(minesBlocks)) {
			return false;
		}

		// 找到工具的 incorrect 限制标签（correctForDrops=false 的规则），确定其挖掘等级
		for (Tool.Rule rule : tool.rules()) {
			if (rule.correctForDrops().isPresent() && !rule.correctForDrops().get()) {
				TagKey<Block> incorrectTag = rule.blocks().unwrapKey().orElse(null);
				if (incorrectTag == null) {
					continue;
				}
				int tier = extraenchantry$tierOf(incorrectTag);
				if (tier < 0) {
					continue;
				}
				int effective = tier + level;
				TagKey<Block> restriction = extraenchantry$restrictionFor(effective);
				// 有效等级 >= 3 无任何限制；否则检查方块是否超出限制
				return restriction == null || !state.is(restriction);
			}
		}
		return false;
	}

	@Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$tierBreakDrops(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		ItemStack stack = (ItemStack) (Object) this;
		int level = ExtraEnchantry.getTierBreakLevel(stack);
		if (level <= 0) {
			return;
		}

		// 基岩：下界合金镐 + 3 级拓阶视为有效工具（有掉落）
		if (state.is(Blocks.BEDROCK)) {
			if (extraenchantry$canMineBedrock(stack, level)) {
				cir.setReturnValue(true);
			}
			return;
		}

		// 常规挖掘等级提升
		if (extraenchantry$isBoostedHarvestable(stack, state, level)) {
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
			if (extraenchantry$canMineBedrock(stack, level)) {
				cir.setReturnValue(extraenchantry$getMinesSpeed(tool));
			}
			return;
		}

		// 挖掘等级提升后：以正常工具速度挖掘（否则错误等级会退化为手速）
		if (extraenchantry$isBoostedHarvestable(stack, state, level)) {
			cir.setReturnValue(extraenchantry$getMinesSpeed(tool));
		}
	}
}
