package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * 深渊祭钥（v1.8.0.1 改版）：幽渊传送门的「打火石」。
 *
 * 用法（对齐下界传送门心智）：
 * 1. 在远古城市内用**强化深板岩**搭竖直门框（任意 2×3 及以上的内洞）；
 * 2. 手持深渊祭钥**右键门框内的空位** → 校验框架 → 空位填充「深渊之门」；
 * 3. 步入深渊之门 → 抵达幽渊；幽渊侧同法搭建即可返回。
 *
 * 耐久 64（同打火石），每次点火 -1；创造模式不消耗。
 * 点火成功：SONIC_BOOM 环 + 幽匿尖啸音效；失败：低沉音效 + 提示框架无效。
 */
public class AbyssGateItem extends Item {

	/** 耐久：同打火石 */
	private static final int DURABILITY = 64;

	public AbyssGateItem(Properties properties) {
		super(properties.durability(DURABILITY));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos();
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();

		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		// 点火目标：点击面内侧的空位（同打火石点亮下界门的判定习惯）
		BlockPos inside = clicked.relative(context.getClickedFace());

		// 框架校验（v1.8.0.5 收紧）：仅强化深板岩 + 必须位于远古城市结构范围内
		BlockState clickedState = server.getBlockState(clicked);
		if (!clickedState.is(AbyssGate.FRAME_BLOCK)) {
			hint(serverPlayer, Component.translatable("message.extra-enchantry.gate.wrong_block"));
			return InteractionResult.FAIL;
		}
		// §3.1「位于远古城市最深处的祭所」：门框必须坐落在远古城市结构范围内
		var structure = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
				.getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.ANCIENT_CITY).value();
		var structureStart = server.structureManager().getStructureAt(clicked, structure);
		if (structureStart == net.minecraft.world.level.levelgen.structure.StructureStart.INVALID_START
				|| !structureStart.getBoundingBox().isInside(clicked)) {
			hint(serverPlayer, Component.translatable("message.extra-enchantry.gate.not_in_city"));
			FxHelper.play(server, serverPlayer, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 0.6F, 0.5F);
			return InteractionResult.FAIL;
		}

		// 框架校验 + 填充
		if (!AbyssGate.tryIgnite(server, inside)) {
			hint(serverPlayer, Component.translatable("message.extra-enchantry.gate.bad_frame"));
			FxHelper.play(server, serverPlayer, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 0.6F, 0.5F);
			return InteractionResult.FAIL;
		}

		// §3.4「稳定态」：存在回响锚 → 重开只需嵌入 1 枚回响碎片（低成本，不耗耐久）
		boolean stable = AbyssGateState.stateOf(server, clicked) == AbyssGateState.State.STABLE
				|| AbyssGate.hasAnyAnchor(server);
		if (stable) {
			if (!serverPlayer.getInventory().hasAnyOf(java.util.Set.of(net.minecraft.world.item.Items.ECHO_SHARD))) {
				hint(serverPlayer, Component.translatable("message.extra-enchantry.gate.need_shard"));
				server.setBlock(inside, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
				return InteractionResult.FAIL;
			}
			consumeShard(serverPlayer);
		} else if (!player.getAbilities().instabuild) {
			stack.hurtAndBreak(1, serverPlayer, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
		}
		// 通知门状态机（待唤醒 → 开启 → 收束 / 稳定）
		AbyssGateState.ignite(server, clicked);
		FxHelper.play(server, serverPlayer, SoundEvents.WARDEN_SONIC_CHARGE, 1.0F, 1.4F);
		FxHelper.ring(server, serverPlayer, 6.0D, ParticleTypes.SONIC_BOOM, 12);
		serverPlayer.sendSystemMessage(Component.translatable("message.extra-enchantry.gate.ignited"));
		return InteractionResult.SUCCESS;
	}

	/** 消耗背包中 1 枚回响碎片（稳定态重开代价，§3.4） */
	private static void consumeShard(ServerPlayer player) {
		var inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.is(net.minecraft.world.item.Items.ECHO_SHARD)) {
				s.shrink(1);
				return;
			}
		}
	}

	private static void hint(ServerPlayer player, Component text) {
		player.sendSystemMessage(text);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.extra-enchantry.abyss_gate_key")
				.withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
	}
}
