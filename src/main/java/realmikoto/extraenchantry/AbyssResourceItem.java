package realmikoto.extraenchantry;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * 幽渊资源（1.8.3「藏」）：六种深渊材料的统一物品类。
 *
 * 覆盖（对齐 DESIGN/1.8.0-design.md §5.2）：
 * - echo_crystal  回响结晶  回声层矿脉 / 声纹湖底 → 共鸣强化、传送门重开
 * - tide_tear     渊息之泪  渊息者交易 → 深潜装备
 * - sculk_silk    幽匿丝    幽匿尖塔采集 → 静默斗篷、回响锚
 * - memory_dust   记忆之尘  记忆层 → 铭刻 / 重铸、记忆残影召唤
 * - silence_stone  静默之石  静默区稀有 / 无声者掉落 → 消除声纹（静默区方块）
 * - abyssal_heart 深渊之心  渊心守望者 100% → 终局材料
 */
public class AbyssResourceItem extends Item {

	public enum Kind {
		ECHO_CRYSTAL("echo_crystal", Rarity.RARE),
		TIDE_TEAR("tide_tear", Rarity.UNCOMMON),
		SCULK_SILK("sculk_silk", Rarity.UNCOMMON),
		MEMORY_DUST("memory_dust", Rarity.RARE),
		SILENCE_STONE("silence_stone", Rarity.RARE),
		AETHER_DUST("aether_dust", Rarity.EPIC),
		ABYSSAL_HEART("abyssal_heart", Rarity.EPIC);

		public final String id;
		public final Rarity rarity;

		Kind(String id, Rarity rarity) {
			this.id = id;
			this.rarity = rarity;
		}
	}

	private final Kind kind;

	public AbyssResourceItem(Kind kind) {
		super(new Item.Properties()
				.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id(kind.id)))
				.stacksTo(64).rarity(kind.rarity));
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.extra-enchantry." + kind.id)
				.withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
	}
}
