package realmikoto.extraenchantry.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import realmikoto.extraenchantry.CavalryManager;
import realmikoto.extraenchantry.ExtraEnchantry;

/**
 * 破限（Limit Break）铁砧逻辑：
 * 1. 任一输入物品带破限时，无视附魔互斥（保护类/精准时运/修补无限/锋利系/致密破甲系/多重穿透等全部生效）
 * 2. 任一输入物品带破限时，铁砧合成费用固定为 5 级经验（无视过于昂贵）
 * 3. 疾风（Gale）III 级仅能在带破限时融合得到（无破限时产出降回 II 级）
 * 跨部位附魔解锁（破限腿甲附摔落保护）走 EnchantmentEvents.ALLOW_ENCHANTING 事件
 * （fabric-item-api 已 Redirect canEnchant 调用点，Mixin Redirect 会与之冲突）。
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

	/** 无破限时疾风可达的最高等级 */
	private static final int EXTRAENCHANTRY$GALE_MAX_WITHOUT_LIMIT_BREAK = 2;

	@Shadow
	@Final
	private net.minecraft.world.inventory.DataSlot cost;

	/**
	 * 破限门禁：未击败守护骑兵队（「无敌」进度未达成）时，破限附魔书不可使用。
	 * 附加物（1 号槽）携带破限即视为使用——应用/合并/升级都走这条路径；
	 * 拒绝时清空产出与费用并给动作栏提示。创造模式豁免（与原版铁砧的
	 * hasInfiniteMaterials 豁免规则一致）。
	 */
	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$gateLimitBreakBook(CallbackInfo ci) {
		AnvilMenu menu = (AnvilMenu) (Object) this;
		ItemStack addition = menu.getSlot(1).getItem();
		if (addition.isEmpty() || !CavalryManager.carriesLimitBreak(addition)) {
			return;
		}
		net.minecraft.world.entity.player.Player menuPlayer =
				((ItemCombinerMenuAccessor) menu).extraenchantry$player();
		if (!(menuPlayer instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
				|| serverPlayer.hasInfiniteMaterials()
				|| CavalryManager.isLimitBreakUnlocked(serverPlayer)) {
			return;
		}
		menu.getSlot(2).set(ItemStack.EMPTY);
		this.cost.set(0);
		CavalryManager.notifyLimitBreakLocked(serverPlayer);
		ci.cancel();
	}

	/** 判断铁砧两侧输入中是否有带破限的物品 */
	private boolean extraenchantry$hasLimitBreakInput(AnvilMenu menu) {
		ItemStack input = menu.getSlot(0).getItem();
		ItemStack additional = menu.getSlot(1).getItem();
		return ExtraEnchantry.hasLimitBreak(input) || ExtraEnchantry.hasLimitBreak(additional);
	}

	@Redirect(
			method = "createResult",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/enchantment/Enchantment;areCompatible(Lnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)Z"
			)
	)
	private boolean extraenchantry$ignoreExclusiveSet(Holder<Enchantment> first, Holder<Enchantment> second) {
		AnvilMenu menu = (AnvilMenu) (Object) this;
		if (extraenchantry$hasLimitBreakInput(menu)) {
			return true;
		}
		return Enchantment.areCompatible(first, second);
	}

	@Inject(method = "createResult", at = @At("TAIL"))
	private void extraenchantry$fixedAnvilCost(CallbackInfo ci) {
		AnvilMenu menu = (AnvilMenu) (Object) this;
		// 仅在实际产出合成结果时固定费用
		if (!menu.getSlot(2).getItem().isEmpty() && extraenchantry$hasLimitBreakInput(menu)) {
			this.cost.set(5);
		}
	}

	/**
	 * 疾风 III 级门槛：无破限时把产出上的疾风降回 II 级。
	 * （附魔台/钓鱼由 min_cost 曲线限制、宝箱书/交易由
	 * EnchantRandomlyFunctionMixin 钳制，铁砧融合是 III 级的唯一途径）
	 */
	@Inject(method = "createResult", at = @At("TAIL"))
	private void extraenchantry$clampGaleWithoutLimitBreak(CallbackInfo ci) {
		AnvilMenu menu = (AnvilMenu) (Object) this;
		if (extraenchantry$hasLimitBreakInput(menu)) {
			return;
		}
		ItemStack result = menu.getSlot(2).getItem();
		if (result.isEmpty()
				|| ExtraEnchantry.getGaleLevel(result) <= EXTRAENCHANTRY$GALE_MAX_WITHOUT_LIMIT_BREAK) {
			return;
		}
		ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(result.getEnchantments());
		for (Holder<Enchantment> enchantment : result.getEnchantments().keySet()) {
			if (enchantment.is(ExtraEnchantry.GALE)) {
				mutable.set(enchantment, EXTRAENCHANTRY$GALE_MAX_WITHOUT_LIMIT_BREAK);
				break;
			}
		}
		result.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
	}
}
