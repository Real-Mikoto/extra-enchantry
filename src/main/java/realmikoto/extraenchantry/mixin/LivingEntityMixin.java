package realmikoto.extraenchantry.mixin;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.CleaveManager;
import realmikoto.extraenchantry.DecoyManager;
import realmikoto.extraenchantry.EmberfallManager;
import realmikoto.extraenchantry.ExtraEnchantry;
import realmikoto.extraenchantry.JudgementManager;
import realmikoto.extraenchantry.OathboundManager;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	/**
	 * dropExperience 声明于 LivingEntity 自身（protected），@Shadow 可正常解析，
	 * 供誓约经验球 Redirect 在非四件套路径上透传原版调用。
	 */
	@Shadow
	protected abstract void dropExperience(ServerLevel level, Entity attacker);

	/** 蚀命各等级的目标最大生命值伤害比例（下标 = 等级 - 1） */
	private static final float[] EXTRAENCHANTRY$LIFE_EROSION_RATIO = {0.14F, 0.15F, 0.17F};

	/** 当前已应用的活力加成（-1 = 未初始化，用于变更检测） */
	@Unique
	private int extraenchantry$vitalityBonus = -1;

	/** 当前已应用的疾风速度加成（-1 = 未初始化，用于变更检测） */
	@Unique
	private double extraenchantry$galeBonus = -1.0D;

	/**
	 * 活力（Vitality）：玩家每 tick 校验护甲上的活力总等级，向 MAX_HEALTH
	 * 属性写入/更新瞬态修改器（+4/级，多件叠加上限 +50，破限可突破上限）。
	 * tick 在双端运行，客户端同步获得正确的最大生命值供 HUD 使用。
	 */
	@Inject(method = "tick", at = @At("HEAD"))
	private void extraenchantry$applyVitalityHealth(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof Player)) {
			return;
		}
		int bonus = ExtraEnchantry.getVitalityBonus(self);
		if (bonus == extraenchantry$vitalityBonus) {
			return;
		}
		extraenchantry$vitalityBonus = bonus;
		AttributeInstance maxHealth = self.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}
		if (bonus <= 0) {
			maxHealth.removeModifier(ExtraEnchantry.id("vitality"));
		} else {
			maxHealth.addOrUpdateTransientModifier(new AttributeModifier(
					ExtraEnchantry.id("vitality"), bonus, AttributeModifier.Operation.ADD_VALUE));
		}
		// 加成减少时钳制当前生命值，避免血量残留超出上限
		if (self.getHealth() > self.getMaxHealth()) {
			self.setHealth(self.getMaxHealth());
		}
	}

	/**
	 * 疾风（Gale）：护腿带疾风时移动速度 +5%/级（I/II/III → 5%/10%/15%），
	 * 潜行时不生效（getGaleSpeedBonus 返回 0，修改器被移除）。
	 * 疾风 III 级仅能由两个 II 级在带破限的铁砧上融合得到（见 AnvilMenuMixin）。
	 */
	@Inject(method = "tick", at = @At("HEAD"))
	private void extraenchantry$applyGaleSpeed(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof Player)) {
			return;
		}
		double bonus = ExtraEnchantry.getGaleSpeedBonus(self);
		if (bonus == extraenchantry$galeBonus) {
			return;
		}
		extraenchantry$galeBonus = bonus;
		AttributeInstance speed = self.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		if (bonus <= 0.0D) {
			speed.removeModifier(ExtraEnchantry.id("gale"));
		} else {
			speed.addOrUpdateTransientModifier(new AttributeModifier(
					ExtraEnchantry.id("gale"), bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
	}

	/**
	 * 假象（Decoy）PvP 触发：被其他玩家造成伤害时执行假象判定
	 * （内部含 1 秒判定间隔与全局冷却，由 DecoyManager 控制）。
	 */
	@Inject(method = "hurtServer", at = @At("HEAD"))
	private void extraenchantry$decoyPvpTrigger(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof ServerPlayer victim
				&& source.getEntity() instanceof ServerPlayer attacker
				&& attacker != victim) {
			DecoyManager.tryTrigger(victim);
		}
	}

	/**
	 * 蚀命（Life Erosion）：被带蚀命附魔的武器/工具/远程武器命中时，额外受到
	 * 百分比最大生命值伤害（I/II/III 级 → 14%/15%/17%）。
	 * 远程（箭矢/掷出的三叉戟等非直接伤害）效果减少 1/4（即 ×0.75）。
	 * 直接加在 hurtServer 入参 amount 上，后续护甲/魔抗照常结算。
	 */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float extraenchantry$lifeErosionBonusDamage(float amount, ServerLevel level, DamageSource source) {
		if (amount <= 0.0F || source.getEntity() == null || source.getEntity() == (Object) this
				|| CleaveManager.isCleaving()) {
			return amount;
		}
		int erosionLevel = ExtraEnchantry.getLifeErosionLevel(source.getWeaponItem());
		if (erosionLevel <= 0 || erosionLevel > EXTRAENCHANTRY$LIFE_EROSION_RATIO.length) {
			return amount;
		}
		float ratio = EXTRAENCHANTRY$LIFE_EROSION_RATIO[erosionLevel - 1];
		if (!source.isDirect()) {
			ratio *= 0.75F;
		}
		LivingEntity self = (LivingEntity) (Object) this;
		return amount + self.getMaxHealth() * ratio;
	}

	/**
	 * 断罪（Judgement）：目标生命低于阈值时直接斩杀（I/II 级 → 10%/20% 最大生命），
	 * boss 不斩杀改为双倍伤害，触发后攻击者 5 秒冷却。
	 * 定义在蚀命之后 → 蚀命追加的百分比伤害也参与斩杀结算；
	 * 具体逻辑在 JudgementManager。
	 */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float extraenchantry$judgementExecute(float amount, ServerLevel level, DamageSource source) {
		return JudgementManager.applyJudgement(level, (LivingEntity) (Object) this, source, amount);
	}

	/**
	 * 壁垒（Bulwark）：胸甲带壁垒时，单次攻击受到的伤害上限被锁定
	 * （I~X 级 → 5.5/5/4.5/4/3.5/3/2.5/2/1.5/1 颗心，每级递减 0.5 心）。
	 * 钳制发生在护甲/魔抗减免**之后**、吸收盾结算之前——
	 * 即上限约束的是防御减免后的实际承伤（吸收+掉血合计），不会被护甲二次削减。
	 *
	 * 注入在 getDamageAfterMagicAbsorb 方法本身的 RETURN：
	 * Player 重写了 actuallyHurt 且不调 super，但其内部对 getDamageAfterMagicAbsorb
	 * 是虚调用，同样派发到 LivingEntity 的这一份实现——单点注入即同时覆盖
	 * 玩家（Player#actuallyHurt）与非玩家生物（LivingEntity#actuallyHurt），
	 * 且不影响本 Mixin 对其内部 CombatRules 调用的 Redirect（破限 100% 保护）。
	 */
	@Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true)
	private void extraenchantry$bulwarkCapDamage(DamageSource source, float amount,
			CallbackInfoReturnable<Float> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		cir.setReturnValue(ExtraEnchantry.applyBulwarkCap(self, cir.getReturnValueF()));
	}

	/**
	 * 汲取（Siphon）：用带汲取附魔的武器/工具成功造成伤害时回复自身生命。
	 * 近战每级回复 1 颗心；远程（箭矢/掷出的三叉戟等非直接伤害）回复减半。
	 * 武器来自 DamageSource#getWeaponItem：近战为攻击者主手，箭矢为发射时的弓/弩。
	 */
	@Inject(method = "hurtServer", at = @At("RETURN"))
	private void extraenchantry$siphonHeal(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() || !(source.getEntity() instanceof LivingEntity attacker)
				|| attacker == (Object) this || CleaveManager.isCleaving()) {
			return;
		}
		int siphonLevel = ExtraEnchantry.getSiphonLevel(source.getWeaponItem());
		if (siphonLevel <= 0) {
			return;
		}
		float hearts = siphonLevel;
		if (!source.isDirect()) {
			hearts *= 0.5F;
		}
		attacker.heal(hearts * 2.0F);
	}

	/**
	 * 破阵（Cleave）：近战命中主目标后，对周围额外目标造成百分比伤害。
	 * 具体逻辑在 CleaveManager（目标筛选、比例表、ThreadLocal 短路）。
	 */
	@Inject(method = "hurtServer", at = @At("RETURN"))
	private void extraenchantry$cleaveSplash(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		CleaveManager.tryCleave(level, (LivingEntity) (Object) this, source, amount, cir.getReturnValueZ());
	}

	/**
	 * 凋零保护：受到凋零效果时，按装备上的附魔总等级缩短效果持续时间。
	 * 每级减少 15%，穿戴多件时叠加，总减少量上限 90%。
	 */
	@ModifyVariable(
			method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At("HEAD"),
			argsOnly = true
	)
	private MobEffectInstance extraenchantry$reduceWitherDuration(MobEffectInstance effect) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (effect == null || !effect.is(MobEffects.WITHER) || effect.isInfiniteDuration()) {
			return effect;
		}

		int totalLevels = 0;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (!slot.isArmor()) {
				continue;
			}
			ItemStack stack = self.getItemBySlot(slot);
			ItemEnchantments enchantments = stack.getEnchantments();
			for (Holder<Enchantment> enchantment : enchantments.keySet()) {
				if (enchantment.is(ExtraEnchantry.WITHER_PROTECTION)) {
					totalLevels += enchantments.getLevel(enchantment);
				}
			}
		}
		if (totalLevels <= 0) {
			return effect;
		}

		float reduction = Math.min(0.9F, totalLevels * 0.15F);
		return effect.withScaledDuration(1.0F - reduction);
	}

	/**
	 * 破限：穿任意带破限附魔的护甲时，保护类附魔的伤害减免上限从 80%（EPF 上限 20）提升到 100%（EPF 上限 25）。
	 * 原版公式：damage * (1 - clamp(EPF, 0, 20) / 25)
	 */
	@Redirect(
			method = "getDamageAfterMagicAbsorb",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterMagicAbsorb(FF)F"
			)
	)
	private float extraenchantry$uncappedProtection(float damage, float protection) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (ExtraEnchantry.hasLimitBreakOnArmor(self)) {
			float clamped = Math.max(0.0F, Math.min(25.0F, protection));
			return damage * (1.0F - clamped / 25.0F);
		}
		return CombatRules.getDamageAfterMagicAbsorb(damage, protection);
	}

	/**
	 * 无踪（Unseen）II 级：降低怪物索敌可见度。
	 * getVisibilityPercent 是原版潜行/隐身影响索敌距离的唯一系数（潜行 ×0.8），
	 * 在 RETURN 处乘 0.7 即“索敌范围 -30%”，与原版机制同构。
	 */
	@Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
	private void extraenchantry$unseenReducedVisibility(Entity observer, CallbackInfoReturnable<Double> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (ExtraEnchantry.getUnseenLevelOnFeet(self) >= 2) {
			cir.setReturnValue(cir.getReturnValueD() * 0.7D);
		}
	}

	/**
	 * 御风（Windrider）I~III 级：降低滑翔耐久消耗。
	 * 26.2 的鞘翅滑翔耐久在 {@code updateFallFlying} 内每 20 tick 对随机一个
	 * 可滑翔装备槽 hurtAndBreak(1)；重定向该调用，按等级概率跳过
	 * （I/II 级 50%、III 级 75% → 等效耐久消耗 -50%/-75%）。
	 */
	@Redirect(
			method = "updateFallFlying",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"
			)
	)
	private void extraenchantry$windriderGlideDurability(ItemStack stack, int amount, LivingEntity glider,
			EquipmentSlot slot) {
		if (ExtraEnchantry.shouldSkipGlideDurability(stack)) {
			return;
		}
		stack.hurtAndBreak(amount, glider, slot);
	}

	/**
	 * 余烬（Emberfall）免死：仅当不死图腾未能救命时尝试（图腾优先）。
	 * 26.2 链路：hurtServer 在 isDeadOrDying() 后调 checkTotemDeathProtection，
	 * 返回 true 即跳过 die()——本注入在 RETURN 处把 false 改写为 true。
	 */
	@Inject(method = "checkTotemDeathProtection", at = @At("RETURN"), cancellable = true)
	private void extraenchantry$emberfallDeathSave(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && EmberfallManager.trySave((LivingEntity) (Object) this)) {
			cir.setReturnValue(true);
		}
	}

	/**
	 * 余烬锁血（非玩家生物路径）：触发后的前 5 秒取消扣血。
	 * 玩家走 PlayerMixin（Player 重写了 actuallyHurt 且不调 super）。
	 */
	@Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$emberfallLockHealth(ServerLevel level, DamageSource source, float amount,
			CallbackInfo ci) {
		if (EmberfallManager.isLocked((LivingEntity) (Object) this)) {
			ci.cancel();
		}
	}

	/**
	 * 誓约（Oathbound）四件套的经验保留——第一半：跳过死亡经验球。
	 * 26.2 反编译确认：keepInventory 关闭时玩家死亡经
	 * {@code dropAllDeathLoot → dropExperience} 掉出 min(等级×7, 100) 点经验球，
	 * 经验本身靠"旧玩家对象弃置"丢失。四件套要让经验全额保留，
	 * 必须同时掐掉经验球（否则保留 + 掉落 = 重复），搬运在 ServerPlayerMixin 完成。
	 */
	@Redirect(
			method = "dropAllDeathLoot",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;dropExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V"
			)
	)
	private void extraenchantry$skipOathboundXpDrop(LivingEntity self, ServerLevel level, Entity attacker) {
		if (self instanceof ServerPlayer player
				&& OathboundManager.hasFullOathboundArmor(player)
				&& !level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
			return; // 四件套：不掉经验球，经验由 restoreFrom 搬运保留
		}
		this.dropExperience(level, attacker);
	}
}
