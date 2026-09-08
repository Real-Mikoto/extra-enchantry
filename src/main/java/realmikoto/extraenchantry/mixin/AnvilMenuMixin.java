package realmikoto.extraenchantry.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import java.util.Optional;
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

	/**
	 * 判断铁砧两侧输入中是否有带破限的物品。
	 * 1.0.1 起：附加槽的破限**附魔书**（破限存于 STORED_ENCHANTMENTS）同样生效——
	 * 破限书+装备与破限装备+破限书两条路径都需要等级突破。
	 */
	private boolean extraenchantry$hasLimitBreakInput(AnvilMenu menu) {
		ItemStack input = menu.getSlot(0).getItem();
		ItemStack additional = menu.getSlot(1).getItem();
		return CavalryManager.carriesLimitBreak(input) || CavalryManager.carriesLimitBreak(additional);
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

	/**
	 * 破限等级突破（1.0.1）：带破限的输入在铁砧融合时，可成长附魔的等级上限 +1
	 * （例：两个保护 IV 融合 → 保护 V）。上限钳制点在 createResult 内的
	 * `level > enchantment.getMaxLevel()` 两处调用——重定向后带破限时返回原版上限 +1。
	 * 仅对 LEVEL_UP_ENCHANTMENTS 中的"逻辑上可增加一级"的附魔生效。
	 */
	@Redirect(
			method = "createResult",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"
			)
	)
	private int extraenchantry$levelUpCap(Enchantment enchantment) {
		AnvilMenu menu = (AnvilMenu) (Object) this;
		if (extraenchantry$hasLimitBreakInput(menu)) {
			Player player = ((ItemCombinerMenuAccessor) menu).extraenchantry$player();
			if (player != null) {
				Optional<ResourceKey<Enchantment>> key = player.level().registryAccess()
						.lookupOrThrow(Registries.ENCHANTMENT).getResourceKey(enchantment);
				if (key.isPresent() && ExtraEnchantry.LEVEL_UP_ENCHANTMENTS.contains(key.get())) {
					return enchantment.getMaxLevel() + 1;
				}
			}
		}
		return enchantment.getMaxLevel();
	}

	@Inject(method = "createResult", at = @At("TAIL"))
	private void extraenchantry$fixedAnvilCost(CallbackInfo ci) {
		AnvilMenu menu = (AnvilMenu) (Object) this;
		// 仅在实际产出合成结果时固定费用
		if (!menu.getSlot(2).getItem().isEmpty() && extraenchantry$hasLimitBreakInput(menu)) {
			// v1.1.0 平衡：触及 XI 级（破限突破产物）费用特判为 15 级经验——
			// +5.5 格攻击距离在 PvP 是质变，5 级经验过于廉价（机制不动，只动价格）
			this.cost.set(extraenchantry$isReachEleven(menu.getSlot(2).getItem()) ? 15 : 5);
		}
	}

	/** 产出是否带 XI 级触及（破限把 X 级上限突破到 XI 级的产物） */
	private boolean extraenchantry$isReachEleven(ItemStack result) {
		Player player = ((ItemCombinerMenuAccessor) ((AnvilMenu) (Object) this)).extraenchantry$player();
		if (player == null) {
			return false;
		}
		ItemEnchantments enchantments = result.getEnchantments();
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(ExtraEnchantry.REACH)) {
				return enchantments.getLevel(enchantment) >= 11;
			}
		}
		return false;
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

		/**
		 * 獠牙礼赞（1.4.0 usage 成就）：首次在铁砧产出带本模附魔的狼铠时授予。
		 * award 幂等，无重复发放风险；持有者取不到时静默跳过。
		 */
		@Inject(method = "createResult", at = @At("TAIL"))
		private void extraenchantry$wolfArmorAchievement(CallbackInfo ci) {
			AnvilMenu menu = (AnvilMenu) (Object) this;
			ItemStack result = menu.getSlot(2).getItem();
			if (result.isEmpty() || !result.is(net.minecraft.world.item.Items.WOLF_ARMOR)) {
				return;
			}
			for (Holder<Enchantment> enchantment : result.getEnchantments().keySet()) {
				if (enchantment.is(ExtraEnchantry.SHARP_FANG) || enchantment.is(ExtraEnchantry.VIGIL)
						|| enchantment.is(ExtraEnchantry.RENEWAL)) {
					Player player = ((ItemCombinerMenuAccessor) menu).extraenchantry$player();
					if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
						realmikoto.extraenchantry.Advancements.award(serverPlayer,
								realmikoto.extraenchantry.Advancements.WOLF_ARMOR);
					}
					return;
				}
			}
		}
	}
