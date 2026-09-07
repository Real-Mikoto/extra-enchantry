package realmikoto.extraenchantry;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * 破限残页（1.3.1「铭文纪元」§3.3）：诸界浩劫四幕的叙事碎片。
 *
 * 一个物品类型 + {@code shard_id} 数据组件（1~4 对应四幕）。
 * 完成浩劫每波后由 {@link OnboardingManager#onCataclysmWave} 派发；
 * 四页拼合成「极限之器」铭文图样（来者手札第 3 页）。右键研读该幕叙事。
 * 残页不可合成 / 不进创造栏，仅通过挑战获得；挑战失败不回收（收集进度持久）。
 */
public class LimitBreakShardItem extends Item {

	/** 残页幕号组件（Integer，1~4；非法值降级为空白残页不崩溃） */
	public static final DataComponentType<Integer> SHARD_ID =
			DataComponentType.<Integer>builder().persistent(Codec.INT).build();

	/** 四幕主题色：深岩 / 雷光 / 烬红 / 末影紫 */
	private static final ChatFormatting[] ACT_COLORS = {
			ChatFormatting.GRAY, ChatFormatting.YELLOW, ChatFormatting.RED, ChatFormatting.DARK_PURPLE};

	public LimitBreakShardItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			int act = actOf(player.getItemInHand(hand));
			if (act > 0) {
				serverPlayer.sendSystemMessage(buildLore(act));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** 拼装一页残页的研读输出 */
	static MutableComponent buildLore(int act) {
		LoreLoader.ShardLore lore = LoreLoader.shard(act);
		MutableComponent out = Component.empty();
		out.append(Component.literal("── " + lore.title() + " ──").withStyle(colorOf(act))).append("\n");
		out.append(Component.translatable("lore.extra-enchantry.shard.subtitle",
				Component.literal(lore.subtitle())).withStyle(ChatFormatting.DARK_GRAY)).append("\n");
		for (String line : lore.lines()) {
			out.append(Component.literal(line).withStyle(ChatFormatting.GRAY)).append("\n");
		}
		out.append(Component.translatable("lore.extra-enchantry.shard.footer",
				act, 4).withStyle(ChatFormatting.DARK_GRAY));
		return out;
	}

	/** 读取残页幕号（无组件 / 越界 → 0，物品降级为空白残页） */
	public static int actOf(ItemStack stack) {
		Integer act = stack.get(SHARD_ID);
		return act != null && act >= 1 && act <= 4 ? act : 0;
	}

	public static ChatFormatting colorOf(int act) {
		return ACT_COLORS[Math.floorMod(act - 1, ACT_COLORS.length)];
	}

	@Override
	public Component getName(ItemStack stack) {
		int act = actOf(stack);
		if (act == 0) {
			return Component.translatable("item.extra-enchantry.limit_break_shard.blank")
					.withStyle(ChatFormatting.DARK_GRAY);
		}
		return Component.translatable("item.extra-enchantry.limit_break_shard",
						Component.translatable("lore.extra-enchantry.act_num." + act))
				.withStyle(colorOf(act));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		int act = actOf(stack);
		if (act == 0) {
			tooltip.accept(Component.translatable("tooltip.extra-enchantry.limit_break_shard.blank")
					.withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		tooltip.accept(Component.translatable("tooltip.extra-enchantry.limit_break_shard",
				Component.literal(LoreLoader.shard(act).subtitle())).withStyle(ChatFormatting.GRAY));
	}

	/** 注册 shard_id 组件（onInitialize 调用一次） */
	public static void register() {
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ExtraEnchantry.id("shard_id"), SHARD_ID);
	}
}
