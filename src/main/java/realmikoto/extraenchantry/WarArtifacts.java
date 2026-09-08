package realmikoto.extraenchantry;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/**
 * 宣战与归一的四个关键物品（1.6.0「宣战与归一」§1 / §3）。
 *
 * - 宣战图腾：realm_id 组件五型（component select 分发贴图，纯数据零代码），
 *   主手右键在本境群系召唤觉醒境主（跳过触发累积，{@link EliteEncounterManager#startChallenged}）；
 * - 觉醒徽记：realm_id 组件五型，觉醒领主 100% 掉落的进度凭证（持久化，正式物品）；
 * - 归一印记：五徽记 + 五材料合成，右键开启归一之战（{@link ConvergenceManager}）；
 * - 归一心核：归一之战 100% 掉落——铁砧万能修复 75% / 归一宝匣 / 遗辉纹饰的钥匙材料。
 */
public final class WarArtifacts {

	/** 境标识组件（String，realm path：overwarden / emberbone / tidal / hag / ender） */
	public static final DataComponentType<String> REALM_ID =
			DataComponentType.<String>builder().persistent(com.mojang.serialization.Codec.STRING).build();

	public static final Item WAR_TOTEM = new TotemItem(new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("war_totem")))
			.stacksTo(1).rarity(Rarity.RARE));

	public static final Item REALM_SIGIL = new SigilItem(new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("realm_sigil")))
			.stacksTo(1).rarity(Rarity.EPIC));

	public static final Item CONVERGENCE_MARK = new MarkItem(new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("convergence_mark")))
			.stacksTo(1).rarity(Rarity.EPIC).fireResistant());

	public static final Item CONVERGENCE_CORE = new Item(new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("convergence_core")))
			.stacksTo(16).rarity(Rarity.EPIC).fireResistant());

	/** 归一宝匣：心核 + 五材料合成，右键随机开出器魂书（41–45，II–III 级） */
	public static final Item CONVERGENCE_CASKET = new CasketItem(new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("convergence_casket")))
			.stacksTo(16).rarity(Rarity.EPIC));

	/** 1.6.0 新物品的创造栏顺序 */
	public static final List<Item> CREATIVE_ITEMS =
			List.of(WAR_TOTEM, REALM_SIGIL, CONVERGENCE_MARK, CONVERGENCE_CORE, CONVERGENCE_CASKET);

	private WarArtifacts() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("war_totem"), WAR_TOTEM);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("realm_sigil"), REALM_SIGIL);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("convergence_mark"), CONVERGENCE_MARK);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("convergence_core"), CONVERGENCE_CORE);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("convergence_casket"), CONVERGENCE_CASKET);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
				ExtraEnchantry.id("realm_id"), REALM_ID);
	}

	// ============ 组件读写 ============

	public static String realmOf(ItemStack stack) {
		String realm = stack.get(REALM_ID);
		return realm != null && EncounterDef.byId(realm) != null ? realm : null;
	}

	public static ItemStack totem(String realm) {
		ItemStack stack = new ItemStack(WAR_TOTEM);
		stack.set(REALM_ID, realm);
		return stack;
	}

	public static ItemStack sigil(String realm) {
		ItemStack stack = new ItemStack(REALM_SIGIL);
		stack.set(REALM_ID, realm);
		return stack;
	}

	// ============ 物品实现 ============

	/** 宣战图腾：本境群系内主手右键 → 消耗图腾 → 主动开启觉醒遭遇 */
	private static final class TotemItem extends Item {

		private TotemItem(Properties properties) {
			super(properties);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (!(player instanceof ServerPlayer serverPlayer)
					|| hand != InteractionHand.MAIN_HAND
					|| !player.gameMode().isSurvival()) {
				return InteractionResult.PASS;
			}
			String realm = realmOf(stack);
			if (realm == null) {
				return InteractionResult.PASS;
			}
			ServerLevel serverLevel = (ServerLevel) level;
			EliteEncounterManager.ChallengeResult result =
					EliteEncounterManager.startChallenged(serverPlayer, realm);
			switch (result) {
				case OK -> {
					stack.consume(1, player);
					return InteractionResult.CONSUME;
				}
				case NOT_IN_REALM -> player.sendOverlayMessage(Component.translatable(
						"message.extra-enchantry.war_totem.wrong_realm"));
				case ACTIVE_OR_COOLDOWN -> player.sendOverlayMessage(Component.translatable(
						"message.extra-enchantry.war_totem.busy"));
				case DISABLED -> {
				}
			}
			return InteractionResult.FAIL;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
				Consumer<Component> tooltip, TooltipFlag flag) {
			String realm = realmOf(stack);
			tooltip.accept(Component.translatable(realm != null
					? "tooltip.extra-enchantry.war_totem." + realm
					: "tooltip.extra-enchantry.war_totem.invalid"));
		}
	}

	/** 觉醒徽记：纯组件凭证物品（tooltip 展示境名） */
	private static final class SigilItem extends Item {

		private SigilItem(Properties properties) {
			super(properties);
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
				Consumer<Component> tooltip, TooltipFlag flag) {
			String realm = realmOf(stack);
			tooltip.accept(Component.translatable(realm != null
					? "tooltip.extra-enchantry.realm_sigil." + realm
					: "tooltip.extra-enchantry.realm_sigil.invalid"));
		}
	}

	/** 归一印记：主手右键 → 开启归一之战（五回合链式） */
	private static final class MarkItem extends Item {

		private MarkItem(Properties properties) {
			super(properties);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (!(player instanceof ServerPlayer serverPlayer)
					|| hand != InteractionHand.MAIN_HAND
					|| !player.gameMode().isSurvival()) {
				return InteractionResult.PASS;
			}
			// 印记在成功结算时才消耗（挑战期间锁定保留——失败保留可重试，设计 §3.5）
			ConvergenceManager.StartResult result =
					ConvergenceManager.start(serverPlayer, (ServerLevel) level);
			switch (result) {
				case OK -> {
					return InteractionResult.CONSUME;
				}
				case ACTIVE -> player.sendOverlayMessage(Component.translatable(
						"message.extra-enchantry.convergence.busy"));
				case COOLDOWN -> player.sendOverlayMessage(Component.translatable(
						"message.extra-enchantry.convergence.cooldown"));
			}
			return InteractionResult.FAIL;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
				Consumer<Component> tooltip, TooltipFlag flag) {
			tooltip.accept(Component.translatable("tooltip.extra-enchantry.convergence_mark"));
		}
	}

	/** 归一宝匣：右键开启，随机获得器魂附魔书（41–45，II–III 级，§3.6） */
	private static final class CasketItem extends Item {

		private static final String[] SOUL_REALMS =
				{"overwarden", "emberbone", "tidal", "hag", "ender"};

		private CasketItem(Properties properties) {
			super(properties);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			ServerLevel serverLevel = (ServerLevel) level;
			String realm = SOUL_REALMS[serverLevel.getRandom().nextInt(SOUL_REALMS.length)];
			ItemStack book = RealmTreasures.soulBook(serverLevel, realm, 2, 3);
			stack.consume(1, player);
			if (!player.getInventory().add(book)) {
				player.drop(book, false);
			}
			FxHelper.burst(serverLevel, player,
					net.minecraft.core.particles.ParticleTypes.END_ROD, 12, 0.5D);
			FxHelper.play(serverLevel, player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.1F);
			return InteractionResult.CONSUME;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
				Consumer<Component> tooltip, TooltipFlag flag) {
			tooltip.accept(Component.translatable("tooltip.extra-enchantry.convergence_casket"));
		}
	}

	/** 进度查询小工具（ConvergenceManager 共用）：进度是否已完成 */
	static boolean advancementDone(ServerPlayer player, Identifier advancementId) {
		net.minecraft.server.MinecraftServer server = player.level().getServer();
		if (server == null) {
			return false;
		}
		AdvancementHolder holder = server.getAdvancements().get(advancementId);
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}
}
