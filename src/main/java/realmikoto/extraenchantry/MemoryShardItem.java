package realmikoto.extraenchantry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
 * 记忆残片（1.8.4「忆」）：可玩化的封印纪元闪回。
 *
 * 获取：记忆之尘 ×4 + 回响碎片 ×1 合成（记忆层的叙事载体）。
 * 右键：进入完整可玩闪回（MemoryFlashback）——以旁观者身份重历一段渊族记忆，
 * 可看、可走、不可改变；每段记忆只播一次（LoreTriggerManager 一次性语义）。
 *
 * 三段记忆对应设计稿 §2.5 叙事闭环：
 * 1. 沉渊（渊族为何退入幽渊）
 * 2. 苏醒（Act I「深渊苏醒」的真相 + 浩劫残念的来源）
 * 3. 无相（八相之争止于无相 + 八族先祖的回声）
 */
public class MemoryShardItem extends Item {

	public MemoryShardItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		int memory = nextMemory(serverPlayer);
		// 完整可玩闪回（§6.4）：以旁观者身份重历——可看、可走、不可改变
		MemoryFlashback.enter(serverPlayer, memory);
		LoreTriggerManager.fireOnce(serverPlayer, "abyss_memory_" + memory);
		if (!serverPlayer.getAbilities().instabuild) {
			player.getItemInHand(hand).shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	/** 依序播放未看过的记忆；全部看过则循环第一段（可重温） */
	private static int nextMemory(ServerPlayer player) {
		for (int i = 1; i <= 3; i++) {
			if (!LoreTriggerManager.hasFired(player, "abyss_memory_" + i)) {
				return i;
			}
		}
		return 1;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.extra-enchantry.memory_shard")
				.withStyle(ChatFormatting.DARK_PURPLE));
	}
}
