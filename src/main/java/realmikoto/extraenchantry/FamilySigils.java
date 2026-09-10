package realmikoto.extraenchantry;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * 家族铭印与「大共鸣者」终局奖励（1.3.0「铭刻与试炼」§2.4）。
 *
 * 设计：一个物品类型 + {@code family_id} 数据组件表达八枚铭印，避免注册八个近似物品。
 * - 首次完成家族试炼时由 {@link FamilyTrialsManager#complete} 授予（幂等，防重复）；
 * - 背包满时掉落在玩家脚下（不丢失、不复制）；
 * - 非法 family_id 降级为「失效铭印」并保留物品（不崩溃）；
 * - 八枚集齐（八项试炼进度全部完成）授予 {@code family_trials/grand_resonator} 进度；
 *   棱彩臻藏视觉在客户端按该进度判定（{@code GrandResonatorState}），服务端零依赖。
 */
public final class FamilySigils {

	/** 家族铭印物品（唯一类型，家族身份由 family_id 数据组件携带）
	 *  26.2：Item 构造时即要求 Properties 已设置物品 ID（setId(ResourceKey)） */
	public static final Item FAMILY_SIGIL = new SigilItem(new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("family_sigil")))
			.stacksTo(1).rarity(Rarity.EPIC));

	/** 铭印家族标识组件（String，family path，如 "storm"） */
	public static final DataComponentType<String> FAMILY_ID =
			DataComponentType.<String>builder().persistent(Codec.STRING).build();

	private FamilySigils() {
	}

	/** 注册铭印物品与 family_id 组件（onInitialize 调用一次） */
	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("family_sigil"), FAMILY_SIGIL);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ExtraEnchantry.id("family_id"), FAMILY_ID);
	}

	// ============ 授予与终局判定 ============

	/**
	 * 授予一枚家族铭印：背包可容纳则入包，否则掉落在玩家脚下。
	 * 幂等性由调用方保证（仅试炼进度首次 award 返回 true 时调用一次）。
	 */
	public static void grant(ServerPlayer player, FamilyResonanceManager.Family family) {
		ItemStack sigil = new ItemStack(FAMILY_SIGIL);
		sigil.set(FAMILY_ID, family.name().toLowerCase(Locale.ROOT));
		if (!player.getInventory().add(sigil)) {
			player.drop(sigil, false);
			player.sendSystemMessage(Component.translatable("message.extra-enchantry.sigil.dropped"));
		}
		if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			FxHelper.play(serverLevel, player, SoundEvents.PLAYER_LEVELUP, 0.8F, 1.4F);
		}
		// 1.7.0 谱系主线：铭印授予节点（first_sigil 数据驱动补授 / two_sigils 计数）
		LineageManager.onSigilGranted(player);
	}

	/**
	 * 大共鸣者判定：八项家族试炼进度全部完成 → 授予 grand_resonator（幂等）。
	 * 在每次铭印授予后调用；进度缺失（数据包未加载）时静默跳过，下次授予再查。
	 */
	public static void awardGrandResonatorIfComplete(ServerPlayer player) {
		var server = player.level().getServer();
		if (server == null) {
			return;
		}
		var advancements = player.getAdvancements();
		for (FamilyResonanceManager.Family family : FamilyResonanceManager.Family.values()) {
			AdvancementHolder trial = server.getAdvancements().get(ExtraEnchantry.id(
					"family_trials/" + family.name().toLowerCase(Locale.ROOT)));
			if (trial == null) {
				return; // 进度树未加载（数据包缺失）→ 不授予
			}
			AdvancementProgress progress = advancements.getOrStartProgress(trial);
			if (!progress.isDone()) {
				return;
			}
		}
		// 1.7.0 谱系重定向：大共鸣者节点从 family_trials/ 迁至 lineage/ 树（去重）
		AdvancementHolder grand = server.getAdvancements().get(
				LineageManager.GRAND_RESONATOR_KEY.identifier());
		boolean wasDone = grand != null && advancements.getOrStartProgress(grand).isDone();
		LineageManager.onGrandResonator(player);
		if (!wasDone && grand != null) {
			// 首次达成大共鸣者 → 派发编年史卷轴（1.3.1「铭文纪元」）
			OnboardingManager.onGrandResonator(player);
		}
	}

	// ============ 查询辅助 ============

	/** 读取铭印上的家族（无组件/非法值 → null，物品降级为失效铭印） */
	public static FamilyResonanceManager.Family familyOf(ItemStack stack) {
		String path = stack.get(FAMILY_ID);
		if (path == null || path.isEmpty()) {
			return null;
		}
		try {
			return FamilyResonanceManager.Family.valueOf(path.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/** 八系家族展示色（铭印名与 tooltip 着色，与八家族主题一致） */
	public static ChatFormatting familyColor(FamilyResonanceManager.Family family) {
		return switch (family) {
			case SOUL -> ChatFormatting.AQUA;
			case STORM -> ChatFormatting.YELLOW;
			case BLADE -> ChatFormatting.GRAY;
			case GUARD -> ChatFormatting.GOLD;
			case NATURE -> ChatFormatting.GREEN;
			case WIND -> ChatFormatting.WHITE;
			case FIRE -> ChatFormatting.RED;
			case WATER -> ChatFormatting.BLUE;
		};
	}

	// ============ 物品类 ============

	/** 铭印物品：名字与 tooltip 按携带的家族渲染 */
	private static final class SigilItem extends Item {

		private SigilItem(Properties properties) {
			super(properties);
		}

		@Override
		public Component getName(ItemStack stack) {
			FamilyResonanceManager.Family family = familyOf(stack);
			if (family == null) {
				return Component.translatable("item.extra-enchantry.family_sigil.invalid")
						.withStyle(ChatFormatting.DARK_GRAY);
			}
			MutableComponent name = Component.translatable("item.extra-enchantry.family_sigil",
					Component.translatable("family.extra-enchantry." + family.name().toLowerCase(Locale.ROOT)));
			return name.withStyle(familyColor(family));
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
				Consumer<Component> tooltip, TooltipFlag flag) {
			FamilyResonanceManager.Family family = familyOf(stack);
			if (family == null) {
				tooltip.accept(Component.translatable("tooltip.extra-enchantry.family_sigil.invalid")
						.withStyle(ChatFormatting.DARK_GRAY));
				return;
			}
			tooltip.accept(Component.translatable(
					"tooltip.extra-enchantry.family_sigil",
					Component.translatable("family.extra-enchantry." + family.name().toLowerCase(Locale.ROOT)))
					.withStyle(ChatFormatting.GRAY));
		}
	}
}
