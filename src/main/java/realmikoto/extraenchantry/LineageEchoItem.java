package realmikoto.extraenchantry;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 谱系回响匣（1.7.0「谱系与传承」§3.1）：铭印的收集向补齐渠道。
 *
 * 右键开启：玩家至少一个家族 FULL 时，从已 FULL 家族中随机选一族，
 * 授予一枚该族铭印（FamilySigils 语义一致——但**不走试炼授予**，
 * 直接构造物品入包，避免误触发试炼章节的 onSigilGranted 计数语义）。
 *
 * 防刷：单玩家 30 s 开启冷却；未 FULL 家族时开启失败（物品不消耗）。
 */
public final class LineageEchoItem extends Item {

	/** 开启冷却（tick）＝30 s */
	private static final int OPEN_COOLDOWN_TICKS = 600;
	private static final Map<UUID, Long> LAST_OPEN_AT = new ConcurrentHashMap<>();

	public static final Item LINEAGE_ECHO = new LineageEchoItem(new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("lineage_echo")))
			.stacksTo(16).rarity(Rarity.EPIC));

	private LineageEchoItem(Properties properties) {
		super(properties);
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("lineage_echo"), LINEAGE_ECHO);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		ServerPlayer serverPlayer = (ServerPlayer) player;
		ServerLevel serverLevel = (ServerLevel) level;
		long now = level.getGameTime();
		Long last = LAST_OPEN_AT.get(player.getUUID());
		if (last != null && now - last < OPEN_COOLDOWN_TICKS) {
			player.sendOverlayMessage(Component.translatable(
					"message.extra-enchantry.lineage_echo.cooldown"));
			return InteractionResult.FAIL;
		}
		// 收集已 FULL 家族
		List<FamilyResonanceManager.Family> fullFamilies = new ArrayList<>();
		for (FamilyResonanceManager.Family family : FamilyResonanceManager.Family.values()) {
			if (FamilyResonanceManager.tierOf(serverPlayer, family)
					== FamilyResonanceManager.Tier.FULL) {
				fullFamilies.add(family);
			}
		}
		if (fullFamilies.isEmpty()) {
			player.sendOverlayMessage(Component.translatable(
					"message.extra-enchantry.lineage_echo.no_full"));
			return InteractionResult.FAIL;
		}
		// 随机一族授予铭印
		FamilyResonanceManager.Family family =
				fullFamilies.get(serverLevel.getRandom().nextInt(fullFamilies.size()));
		ItemStack sigil = new ItemStack(FamilySigils.FAMILY_SIGIL);
		sigil.set(FamilySigils.FAMILY_ID, family.name().toLowerCase(java.util.Locale.ROOT));
		stack.consume(1, player);
		if (!player.getInventory().add(sigil)) {
			player.drop(sigil, false);
		}
		LAST_OPEN_AT.put(player.getUUID(), now);
		player.sendSystemMessage(Component.translatable("message.extra-enchantry.lineage_echo.opened",
				Component.translatable("family.extra-enchantry." + family.name().toLowerCase())));
		FxHelper.burst(serverLevel, player, ParticleTypes.END_ROD, 16, 0.5D);
		FxHelper.play(serverLevel, player, SoundEvents.PLAYER_LEVELUP, 0.8F, 1.3F);
		return InteractionResult.CONSUME;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.extra-enchantry.lineage_echo"));
	}
}
