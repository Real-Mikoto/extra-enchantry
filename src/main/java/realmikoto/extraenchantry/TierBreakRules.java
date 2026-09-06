package realmikoto.extraenchantry;

import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 拓阶（Tier Break）挖掘等级规则（从 ItemStackMixin 提取的公共逻辑）：
 * ItemStackMixin 的掉落/速度判定与 BlockMixin 的拓阶特效触发共用同一套规则，
 * 避免两处各写一份导致口径漂移。
 */
public final class TierBreakRules {

	private TierBreakRules() {
	}

	/** 根据工具的 incorrect 标签返回其挖掘等级（0-3），未知标签返回 -1 */
	static int tierOf(TagKey<Block> tag) {
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
	static TagKey<Block> restrictionFor(int effectiveTier) {
		return switch (Math.min(effectiveTier, 3)) {
			case 0 -> BlockTags.INCORRECT_FOR_WOODEN_TOOL;
			case 1 -> BlockTags.INCORRECT_FOR_STONE_TOOL;
			case 2 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
			default -> null;
		};
	}

	/** 获取工具"挖掘规则"（correctForDrops=true 的规则）的方块集合，无则 null */
	static HolderSet<Block> getMinesBlocks(Tool tool) {
		for (Tool.Rule rule : tool.rules()) {
			if (rule.correctForDrops().isPresent() && rule.correctForDrops().get()) {
				return rule.blocks();
			}
		}
		return null;
	}

	/** 获取工具"挖掘规则"的速度（如镐子的材质速度），无则 1.0 */
	public static float getMinesSpeed(Tool tool) {
		for (Tool.Rule rule : tool.rules()) {
			if (rule.correctForDrops().isPresent() && rule.correctForDrops().get()) {
				return rule.speed().orElse(1.0F);
			}
		}
		return 1.0F;
	}

	/** 基岩特殊判定：下界合金镐 + 3 级拓阶 */
	public static boolean canMineBedrock(ItemStack stack, int level) {
		return level >= 3 && stack.is(Items.NETHERITE_PICKAXE);
	}

	/** 判断拓阶生效后该方块是否可正常掉落（工具类别匹配且有效挖掘等级足够） */
	public static boolean isBoostedHarvestable(ItemStack stack, BlockState state, int level) {
		Tool tool = stack.get(DataComponents.TOOL);
		if (tool == null) {
			return false;
		}

		HolderSet<Block> minesBlocks = getMinesBlocks(tool);
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
				int tier = tierOf(incorrectTag);
				if (tier < 0) {
					continue;
				}
				int effective = tier + level;
				TagKey<Block> restriction = restrictionFor(effective);
				// 有效等级 >= 3 无任何限制；否则检查方块是否超出限制
				return restriction == null || !state.is(restriction);
			}
		}
		return false;
	}
}
