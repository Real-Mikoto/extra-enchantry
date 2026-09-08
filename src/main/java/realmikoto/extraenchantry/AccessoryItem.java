package realmikoto.extraenchantry;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * 配饰物品（1.4.0）：右键快捷穿戴（占用则交换回主手）；tooltip 显示宝石被动（按材质传导后数值）。
 * 槽位身份由 {@link Accessories.SlotType} 携带，材质由 {@link Accessories.Material} 携带。
 */
public class AccessoryItem extends Item {

	private final Accessories.SlotType slotType;
	private final Accessories.Material material;

	public AccessoryItem(Accessories.SlotType slotType, Accessories.Material material, Properties properties) {
		super(properties);
		this.slotType = slotType;
		this.material = material;
	}

	public Accessories.SlotType slotType() {
		return slotType;
	}

	public Accessories.Material material() {
		return material;
	}

	/** 右键快捷穿戴：服务端写入 Attachment，占用则交换回主手 */
	@Override
	public InteractionResult use(net.minecraft.world.level.Level level, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			ItemStack swapped = AccessoryManager.equip(serverPlayer, slotType, held.copy());
			if (swapped.isEmpty()) {
				player.setItemInHand(hand, ItemStack.EMPTY);
			} else {
				player.setItemInHand(hand, swapped);
			}
			if (level instanceof ServerLevel serverLevel) {
				AccessoryManager.playEquipSound(serverLevel, player, material);
				// 家族铭印美术效果（1.4.0）：已镶嵌宝石 → 穿戴爆发（L2，音高按材质档位变调）
				String gem = Accessories.socketedGem(held);
				if (gem != null) {
					GemFx.onEquip(serverLevel, serverPlayer, gem, material);
				}
			}
			// 首次穿戴任意配饰 → 「环佩琳琅」usage 成就（award 幂等）
			OnboardingManager.onAccessoryEquipped(serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context,
			net.minecraft.world.item.component.TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		String gem = Accessories.socketedGem(stack);
		if (gem != null) {
			tooltip.accept(Component.translatable(
					"tooltip.extra-enchantry.accessory.gem." + gem,
					AccessoryManager.gemPassivePercent(gem, material.conductivity))
					.withStyle(net.minecraft.ChatFormatting.GRAY));
		} else {
			tooltip.accept(Component.translatable("tooltip.extra-enchantry.accessory.no_gem")
					.withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
		}
	}
}
