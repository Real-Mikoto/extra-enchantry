package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * 幽渊装备三件套（1.8.4「忆」，设计稿 §5.4）。
 *
 * - HUSH 静默斗篷：**随身持有即生效**——声纹半径 ×0.35（潜行时近乎无声）；
 * - DEEPDIVE 深息面罩：随身持有 → 幽渊内微光视野 + 水中呼吸补偿；
 * - CASKET 记忆之匣：右键 → 重放最近听过的一段渊之记忆（收藏向，不消耗）。
 *
 * 说明：不做护甲槽注册（避免与既有 16 配饰/原版护甲冲突），采用"背包持有即生效"
 * 的轻量实现——与设计稿"降低声纹半径 / 延长深息"的意图一致，且零冲突风险。
 */
public class AbyssGearItem extends Item {

	public enum Effect {
		HUSH,
		DEEPDIVE,
		CASKET
	}

	private final Effect effect;

	public AbyssGearItem(String id, Effect effect, Rarity rarity) {
		super(new Item.Properties()
				.setId(net.minecraft.resources.ResourceKey.create(
						net.minecraft.core.registries.Registries.ITEM, ExtraEnchantry.id(id)))
				.stacksTo(1).rarity(rarity));
		this.effect = effect;
	}

	public Effect effect() {
		return effect;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		if (effect == Effect.CASKET) {
			// 重放最近一段记忆（不消耗）
			int last = 1;
			for (int i = 1; i <= 3; i++) {
				if (LoreTriggerManager.hasFired(serverPlayer, "abyss_memory_" + i)) {
					last = i;
				}
			}
			serverPlayer.sendSystemMessage(Component.literal("§8════ 记忆之匣 · 回放 ════"));
			for (String line : AbyssMemories.get(last)) {
				serverPlayer.sendSystemMessage(Component.literal(line));
			}
			FxHelper.play((ServerLevel) level, serverPlayer, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.6F);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	/** 随身持有检测（EchoManager / AbyssEnchantments 调用） */
	public static boolean holds(Player player, Effect effect) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.getItem() instanceof AbyssGearItem gear && gear.effect() == effect) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.extra-enchantry." + effect.name().toLowerCase(java.util.Locale.ROOT))
				.withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
	}
}
