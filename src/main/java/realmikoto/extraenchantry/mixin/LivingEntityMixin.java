package realmikoto.extraenchantry.mixin;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import realmikoto.extraenchantry.CavalryManager;
import realmikoto.extraenchantry.CleaveManager;
import realmikoto.extraenchantry.DecoyManager;
import realmikoto.extraenchantry.EmberfallManager;
import realmikoto.extraenchantry.ExtraEnchantry;
import realmikoto.extraenchantry.FamilyResonanceManager;
import realmikoto.extraenchantry.FamilyTrialsManager;
import realmikoto.extraenchantry.FxHelper;
import realmikoto.extraenchantry.JudgementManager;
import realmikoto.extraenchantry.OathboundManager;
import realmikoto.extraenchantry.SanctuaryManager;
import realmikoto.extraenchantry.SheathedEdgeManager;
import realmikoto.extraenchantry.ShieldChargeManager;
import realmikoto.extraenchantry.StormsurgeManager;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	/**
	 * dropExperience 声明于 LivingEntity 自身（protected），@Shadow 可正常解析，
	 * 供誓约经验球 Redirect 在非四件套路径上透传原版调用。
	 */
	@Shadow
	protected abstract void dropExperience(ServerLevel level, Entity attacker);

	/** 蚀命各等级的目标最大生命值伤害比例（下标 = 等级 - 1） */
	private static final float[] EXTRAENCHANTRY$LIFE_EROSION_RATIO = {0.14F, 0.15F, 0.17F, 0.19F};

	/** 坚壁各等级的不可格挡类伤害减免比例（下标 = 等级 - 1） */
	private static final float[] EXTRAENCHANTRY$AEGIS_REDUCTION = {0.30F, 0.45F, 0.60F};

	/** 当前已应用的活力加成（-1 = 未初始化，用于变更检测） */
	@Unique
	private int extraenchantry$vitalityBonus = -1;

	/** 当前已应用的疾风速度加成（-1 = 未初始化，用于变更检测） */
	@Unique
	private double extraenchantry$galeBonus = -1.0D;

	/**
	 * tick 统一分发器（性能优化 1.7.4）。
	 *
	 * <p>改动前：本 Mixin 对 {@code LivingEntity#tick} 挂了**六个**独立
	 * {@code @Inject(HEAD)}——世界内每个生物每 tick 都要触发 6 次 Mixin 回调，
	 * 且其中四个各自重复做 {@code instanceof} 类型判定。tick 在双端运行，
	 * 客户端同样要为渲染范围内的每个实体付这笔开销，实体密集场景下直接抬高
	 * 客户端 tick 耗时（挤占同线程的渲染帧预算）。</p>
	 *
	 * <p>改动后：收敛为单一注入点，一次性完成类型判定后按序分发。
	 * 分发顺序与原六个独立注入完全一致，各子逻辑内部实现零改动——
	 * 六个 tick 逻辑彼此不依赖对方的副作用（活力/庇护/粒子/疾风/渊息/狼铠
	 * 各写各自的属性修改器与视觉），故合并对行为无影响。</p>
	 *
	 * <p>顺序：1) 活力（所有生物，含无甲快路径）→ 2) 庇护（仅服务端玩家）
	 * → 3) 足下氛围粒子（仅服务端，15 tick 节流）→ 4) 疾风 / 5) 渊息
	 * （仅玩家，{@code instanceof Player} 只判一次，供两者共用）
	 * → 6) 狼铠（内部 {@code instanceof Wolf}）。</p>
	 */
	@Inject(method = "tick", at = @At("HEAD"))
	private void extraenchantry$onLivingTick(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		// 1) 活力：所有生物（含马铠 BODY 槽），内部有"四槽全空"快路径
		extraenchantry$tickVitality(self);
		// 2) 庇护：仅服务端玩家的格挡光环
		if (self instanceof ServerPlayer sanctuaryPlayer) {
			SanctuaryManager.tick(sanctuaryPlayer);
		}
		// 3) 足下氛围粒子：仅服务端，内部 15 tick 节流
		extraenchantry$tickAmbientFootFx(self);
		// 4+5) 玩家专属：疾风移速 + 渊息水下挖掘（类型判定合并为一次）
		if (self instanceof Player) {
			extraenchantry$tickGaleSpeed(self);
			extraenchantry$tickTideheart(self);
		}
		// 6) 狼铠：内部按 instanceof Wolf 分支
		realmikoto.extraenchantry.WolfArmorManager.tick(self);
	}

	/**
	 * 活力（Vitality）：每 tick 校验护甲上的活力总等级，向 MAX_HEALTH
	 * 属性写入/更新瞬态修改器（+4/级，多件叠加上限 +50，破限可突破上限）。
	 * 26.2 的 isArmor() 同时覆盖人形四件套与动物 BODY 槽——马铠上的活力
	 * 直接对马生效；骑兵等持械生物的下界合金套同样照常结算。
	 * tick 在双端运行，客户端同步获得正确的最大生命值供 HUD 使用。
	 */
	private void extraenchantry$tickVitality(LivingEntity self) {
		int bonus = ExtraEnchantry.getVitalityBonus(self);
		if (bonus == extraenchantry$vitalityBonus) {
			return;
		}
		// 活力穿戴确认（P2）：加成上升时一次性心形 + 轻竖琴音
		// （previous < 0 是登录/生成的首次初始化，不播）
		if (extraenchantry$vitalityBonus >= 0 && bonus > extraenchantry$vitalityBonus
				&& self.level() instanceof ServerLevel serverLevel) {
			FxHelper.burst(serverLevel, self, net.minecraft.core.particles.ParticleTypes.HEART, 3, 0.3D);
			FxHelper.play(serverLevel, self, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HARP, 0.2F, 1.0F);
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
	 * 疾风 / 炽焰行者的足下氛围粒子（P2，L1 常驻级，15 tick 节流）：
	 * - 疾风：移速加成生效且地面移动中 → 脚下云雾（III 级升级为 POOF 烟缕，"足下生风"）；
	 * - 炽焰行者：站在岩浆块 / 岩浆上 → 脚下火苗 + 偶发熔岩滴（"每一步都有火迹"）。
	 */
	private void extraenchantry$tickAmbientFootFx(LivingEntity self) {
		if (!(self.level() instanceof ServerLevel serverLevel)
				|| serverLevel.getGameTime() % 15L != 0L) {
			return;
		}
		// 疾风：足下生风
		int galeLevel = ExtraEnchantry.getGaleLevel(self.getItemBySlot(EquipmentSlot.LEGS));
		if (galeLevel > 0 && !self.isCrouching() && self.onGround()
				&& self.getDeltaMovement().horizontalDistanceSqr() > 0.01D) {
			FxHelper.burstAt(serverLevel, self.getX(), self.getY() + 0.1D, self.getZ(),
					galeLevel >= 3
							? net.minecraft.core.particles.ParticleTypes.POOF
							: net.minecraft.core.particles.ParticleTypes.CLOUD,
					1, 0.15D);
		}
		// 炽焰行者：火迹
		if (ExtraEnchantry.getBlazingWalkerLevel(self.getItemBySlot(EquipmentSlot.FEET)) > 0 && self.onGround()
				&& (self.level().getBlockState(self.blockPosition().below())
						.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
						|| self.isInLava())) {
			FxHelper.burstAt(serverLevel, self.getX(), self.getY() + 0.05D, self.getZ(),
					net.minecraft.core.particles.ParticleTypes.FLAME, 2, 0.2D);
			if (serverLevel.getGameTime() % 30L == 0L) {
				FxHelper.burstAt(serverLevel, self.getX(), self.getY() + 0.1D, self.getZ(),
						net.minecraft.core.particles.ParticleTypes.LAVA, 1, 0.1D);
			}
		}
	}

	/**
	 * 疾风（Gale）：护腿带疾风时移动速度 +5%/级（I/II/III → 5%/10%/15%），
	 * 潜行时不生效（getGaleSpeedBonus 返回 0，修改器被移除）。
	 * 疾风 III 级仅能由两个 II 级在带破限的铁砧上融合得到（见 AnvilMenuMixin）。
	 */
	private void extraenchantry$tickGaleSpeed(LivingEntity self) {
		// 类型判定已在分发器 extraenchantry$onLivingTick 中完成（self instanceof Player）
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

	/** 当前已应用的渊息水下挖掘加成（-1 = 未初始化，用于变更检测） */
	@Unique
	private double extraenchantry$tideheartMiningBonus = -1.0D;

	/** 渊息：上一 tick 眼部是否在水中（入水沿检测用） */
	@Unique
	private boolean extraenchantry$wasEyeInWater = false;

	/**
	 * 渊息（Tideheart）III 级——水下挖掘不再减速：
	 * 26.2 反编译确认，水下挖掘惩罚是 {@code Player#getDestroySpeed} 里的
	 * {@code Attributes.SUBMERGED_MINING_SPEED} 属性乘算（原版基础值 0.2），
	 * 不是分支代码——写入 +0.8 瞬态修改器即恢复到 1.0（与活力/疾风同一模式，
	 * 双端执行保证客户端挖掘进度预测一致）。
	 *
	 * 附带入水反馈（P1）：佩戴渊息头盔首次没入水中时气泡柱 + 水下环境音。
	 */
	private void extraenchantry$tickTideheart(LivingEntity self) {
		// 类型判定已在分发器 extraenchantry$onLivingTick 中完成（self instanceof Player）
		int tideheartLevel = ExtraEnchantry.getTideheartLevel(self.getItemBySlot(EquipmentSlot.HEAD));
		// 入水沿反馈：眼部从无水→有水且佩戴渊息
		boolean eyeInWater = self.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
		if (eyeInWater && !extraenchantry$wasEyeInWater && tideheartLevel > 0
				&& self.level() instanceof ServerLevel serverLevel) {
			FxHelper.burst(serverLevel, self,
					net.minecraft.core.particles.ParticleTypes.BUBBLE_COLUMN_UP, 8, 0.3D);
			FxHelper.play(serverLevel, self, net.minecraft.sounds.SoundEvents.AMBIENT_UNDERWATER_ENTER,
					0.6F, 1.0F);
			// 实战成就「深海呼吸」：渊息首次入水换气
			if (self instanceof ServerPlayer serverPlayer) {
				realmikoto.extraenchantry.Advancements.award(serverPlayer,
						realmikoto.extraenchantry.Advancements.DEEP_BREATH);
			}
		}
		extraenchantry$wasEyeInWater = eyeInWater;

		double bonus = tideheartLevel >= 3 ? 0.8D : 0.0D;
		if (bonus == extraenchantry$tideheartMiningBonus) {
			return;
		}
		extraenchantry$tideheartMiningBonus = bonus;
		AttributeInstance submerged = self.getAttribute(Attributes.SUBMERGED_MINING_SPEED);
		if (submerged == null) {
			return;
		}
		if (bonus <= 0.0D) {
			submerged.removeModifier(ExtraEnchantry.id("tideheart_mining"));
		} else {
			submerged.addOrUpdateTransientModifier(new AttributeModifier(
					ExtraEnchantry.id("tideheart_mining"), bonus, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	// ============ 1.4.0「环佩与獠牙」============

	/**
	 * 配饰受伤减免（盾坠/盾纹玉全伤害、羽环/风羽晶弹射物、烬镯/烬心石火）：
	 * victim 侧 ModifyVariable，结算全部收敛在 AccessoryManager。
	 */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float extraenchantry$accessoryIncoming(float amount, ServerLevel level, DamageSource source) {
		if (amount <= 0.0F || !((Object) this instanceof net.minecraft.server.level.ServerPlayer)) {
			return amount;
		}
		return realmikoto.extraenchantry.AccessoryManager.incomingDamage((LivingEntity) (Object) this, amount, source);
	}

	/**
	 * 五境领主触发计数（1.5.0）：玩家受伤来源判定。
	 *   - 来源为守卫者 / 远古守卫者 → 海洋境路径 B（激光命中）；
	 *   - 来源为女巫（投掷药水） → 沼泽境路径 A（药水命中）；
	 * 另记录最低血量比例（末地境隐藏进度「无惧虚空」判定）。
	 * 仅新增分支，不改任何既有伤害数值。
	 */
	@Inject(method = "hurtServer", at = @At("HEAD"))
	private void extraenchantry$eliteTriggerHurt(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		LivingEntity victim = (LivingEntity) (Object) this;
		if (!(victim instanceof net.minecraft.server.level.ServerPlayer player)) {
			return;
		}
		realmikoto.extraenchantry.EliteEncounterManager.notePlayerHurt(player, source);
		net.minecraft.world.entity.Entity attacker = source.getEntity();
		if (attacker instanceof net.minecraft.world.entity.monster.Guardian) {
			realmikoto.extraenchantry.EliteEncounterManager.noteGuardianHit(player);
		} else if (attacker instanceof net.minecraft.world.entity.monster.Witch) {
			realmikoto.extraenchantry.EliteEncounterManager.noteWitchPotionHit(player);
		}
	}

	/**
	 * 配饰造成伤害加成（雷鸣扣：雷雨天气全伤害 +4%/级）：attacker 侧 ModifyVariable。
	 */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float extraenchantry$accessoryOutgoing(float amount, ServerLevel level, DamageSource source) {
		if (amount <= 0.0F) {
			return amount;
		}
		return realmikoto.extraenchantry.AccessoryManager.outgoingDamage((LivingEntity) (Object) this, amount, source);
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
	 * 八系试炼与自然受伤记账（1.3.0）：在所有伤害修改（锋刃/蚀命等）之前
	 * 把原始伤害传给试炼计数——守护·不动按减免前原始伤害累计；
	 * 玩家受害者另记自然扎根的受伤暂停时刻。
	 * 声明在锋刃 @ModifyVariable 之前以保证捕获原始入参。
	 */
	@Inject(method = "hurtServer", at = @At("HEAD"))
	private void extraenchantry$trialHurtRecord(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		if (amount <= 0.0F) {
			return;
		}
		LivingEntity self = (LivingEntity) (Object) this;
		FamilyTrialsManager.onHurtServer(self, source, amount);
		if (self instanceof ServerPlayer victim) {
			FamilyResonanceManager.onPlayerHurt(victim);
		}
	}

	/**
	 * 锋刃连击（家族共鸣 FULL 锋刃，1.2.0）：3 秒内连续命中伤害递增，最高 +15%。
	 * 声明在最前：连击倍率先于其余加伤/减伤结算。
	 */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float extraenchantry$bladeCombo(float amount, ServerLevel level, DamageSource source) {
		if (amount <= 0.0F || !source.isDirect()
				|| !(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer attacker)) {
			return amount;
		}
		return amount * FamilyResonanceManager.bladeComboMultiplier(attacker);
	}

	/**
	 * 风 轻盈（家族共鸣 FULL 风，1.2.0）：摔落伤害减免（基线 50%），
	 * 1.3.0 数值数据化，主调提升至 65%。
	 */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float extraenchantry$windFallReduction(float amount, ServerLevel level, DamageSource source) {
		if (amount <= 0.0F
				|| !(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer victim)
				|| FamilyResonanceManager.tierOf(victim, FamilyResonanceManager.Family.WIND)
						!= FamilyResonanceManager.Tier.FULL) {
			return amount;
		}
		return amount * FamilyResonanceManager.windFallMultiplier(victim);
	}

	/**
	 * 风暴主调（1.3.0）：FULL 风暴 + 主调时雷击伤害降低 25%。
	 * 声明在风轻盈之后、其余加伤/减伤之前。
	 */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float extraenchantry$stormLightningReduction(float amount, ServerLevel level, DamageSource source) {
		if (amount <= 0.0F || !source.is(DamageTypeTags.IS_LIGHTNING)
				|| !(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer victim)) {
			return amount;
		}
		return amount * FamilyResonanceManager.lightningDamageMultiplier(victim);
	}

	/**
	 * 火焰 炽热之躯（家族共鸣 FULL 火焰，1.2.0）：免疫火焰 / 熔岩伤害。
	 * 在蚀命/断罪等结算之前整体取消。
	 */
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$fireImmunity(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		if (amount > 0.0F && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
				&& (Object) this instanceof net.minecraft.server.level.ServerPlayer victim
				&& FamilyResonanceManager.tierOf(victim, FamilyResonanceManager.Family.FIRE)
						== FamilyResonanceManager.Tier.FULL) {
			cir.setReturnValue(false);
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
		// 家族共鸣小加成：自然族 ≥PARTIAL 时蚀命按 +1 级结算（表内已含等级 4）
		if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
			erosionLevel = ExtraEnchantry.effectiveLevel(sp, source.getWeaponItem(),
					ExtraEnchantry.LIFE_EROSION, erosionLevel);
			if (erosionLevel > EXTRAENCHANTRY$LIFE_EROSION_RATIO.length) {
				erosionLevel = EXTRAENCHANTRY$LIFE_EROSION_RATIO.length;
			}
		}
		float ratio = EXTRAENCHANTRY$LIFE_EROSION_RATIO[erosionLevel - 1];
		if (!source.isDirect()) {
			ratio *= 0.75F;
		}
		LivingEntity self = (LivingEntity) (Object) this;
		// 蚀命反馈（P1）：受害者灵魂粒子 + 极轻灵魂逸散音（节流 10 tick）——
		// 受害者视角才知道自己被侵蚀了（与断罪同族但量级减半，不串味）
		if (self.level() instanceof ServerLevel erosionLevel2 && FxHelper.throttle(self, "life_erosion", 10)) {
			FxHelper.burst(erosionLevel2, self, net.minecraft.core.particles.ParticleTypes.SOUL, 4, 0.3D);
			FxHelper.play(erosionLevel2, self, net.minecraft.sounds.SoundEvents.SOUL_ESCAPE, 0.2F, 1.2F);
		}
		return amount + self.getMaxHealth() * ratio;
	}

	/**
	 * 冲阵（Shield Charge）：疾跑持盾近战撞击——命中额外 4/6/8 伤害 + 强力击退，
	 * 攻击者 1 秒冷却。定义在蚀命之后、断罪之前：冲阵加伤参与断罪斩杀阈值结算。
	 */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float extraenchantry$shieldChargeBonus(float amount, ServerLevel level, DamageSource source) {
		return ShieldChargeManager.applyCharge((LivingEntity) (Object) this, source, amount);
	}

	/**
	 * 霆霓（Stormsurge）：雨天/水中掷出的三叉戟命中额外 2/4 伤害并连锁 2 格内 1 个目标。
	 * 定义在冲阵之后、断罪之前：霆霓加伤参与断罪斩杀阈值结算。
	 */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float extraenchantry$stormsurgeBonus(float amount, ServerLevel level, DamageSource source) {
		return StormsurgeManager.applyBonus((LivingEntity) (Object) this, level, source, amount);
	}

	/**
	 * 藏锋（Sheathed Edge）：脱战 5 秒后的首次近战命中额外 2/4/6 伤害。
	 * 定义在霆霓之后、断罪之前：藏锋加伤参与断罪斩杀阈值结算。
	 */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float extraenchantry$sheathedEdgeBonus(float amount, ServerLevel level, DamageSource source) {
		return SheathedEdgeManager.applyBonus((LivingEntity) (Object) this, level, source, amount);
	}

	/**
	 * 坚壁（Aegis）：格挡中受到"不可格挡类伤害"（bypasses_shield 标签：音波/魔法等）
	 * 时按盾牌坚壁等级减免 30/45/60%——原版格挡对这类伤害完全无效
	 * （BlocksAttacks#bypassedBy 直接放行），此处直接在伤害入口打折。
	 */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float extraenchantry$aegisReduction(float amount, ServerLevel level, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (amount <= 0.0F || !(self instanceof Player player) || !player.isBlocking()) {
			return amount;
		}
		ItemStack blocking = player.getItemBlockingWith();
		if (blocking == null) {
			return amount;
		}
		int aegisLevel = ExtraEnchantry.getAegisLevel(blocking);
		if (aegisLevel <= 0 || aegisLevel > EXTRAENCHANTRY$AEGIS_REDUCTION.length
				|| !source.is(DamageTypeTags.BYPASSES_SHIELD)) {
			return amount;
		}
		// 坚壁反馈（P1）：附魔打击粒子 + 高音盾格挡音（"魔法被弹开"，与壁垒的"重击硬吃"区分）
		if (self.level() instanceof ServerLevel aegisServerLevel && FxHelper.throttle(self, "aegis", 10)) {
			FxHelper.burst(aegisServerLevel, self, net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
					4, 0.3D);
			FxHelper.play(aegisServerLevel, self, net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, 1.0F, 1.2F);
		}
		return amount * (1.0F - EXTRAENCHANTRY$AEGIS_REDUCTION[aegisLevel - 1]);
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
		// 家族共鸣小加成：自然族 ≥PARTIAL 时汲取按 +1 级结算
		if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
			siphonLevel = ExtraEnchantry.effectiveLevel(sp, source.getWeaponItem(),
					ExtraEnchantry.SIPHON, siphonLevel);
		}
		float hearts = siphonLevel;
		if (!source.isDirect()) {
			hearts *= 0.5F;
		}
		attacker.heal(hearts * 2.0F);
		// 汲取反馈（P1）：攻击者心形 ×2 + 极轻竖琴音（节流 5 tick，回血不该吵）
		if (attacker.level() instanceof ServerLevel attackerLevel
				&& FxHelper.throttle(attacker, "siphon", 5)) {
			FxHelper.burst(attackerLevel, attacker, net.minecraft.core.particles.ParticleTypes.HEART, 2, 0.3D);
			FxHelper.play(attackerLevel, attacker, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HARP,
					0.3F, 1.2F);
		}
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
	 * 藏锋（Sheathed Edge）战斗记账：伤害实际生效后，受害者与攻击者
	 * 双方都刷新"最近参与战斗"时间戳（脱战 5 秒判定的数据源）。
	 */
	@Inject(method = "hurtServer", at = @At("RETURN"))
	private void extraenchantry$sheathedEdgeRecordCombat(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()) {
			return;
		}
		LivingEntity self = (LivingEntity) (Object) this;
		SheathedEdgeManager.recordCombat(self);
		if (source.getEntity() instanceof LivingEntity attacker && attacker != self) {
			SheathedEdgeManager.recordCombat(attacker);
		}
	}

	/**
	 * 触及（Reach）剑气反馈（P1）：命中超出原版近战距离（3 格）的目标时，
	 * 沿攻击线排 3 点暴击粒子（"剑气够到了"的延长线视觉）。
	 * 触及是常态效果，不配音效（不该每刀都响）。
	 */
	@Inject(method = "hurtServer", at = @At("RETURN"))
	private void extraenchantry$reachTrail(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() || !source.isDirect()
				|| !(source.getEntity() instanceof LivingEntity attacker)) {
			return;
		}
		LivingEntity self = (LivingEntity) (Object) this;
		if (attacker == self || ExtraEnchantry.getReachLevel(source.getWeaponItem()) <= 0
				|| attacker.distanceTo(self) <= 3.0F) {
			return;
		}
		FxHelper.trail(level,
				attacker.getEyePosition(), self.getEyePosition(),
				net.minecraft.core.particles.ParticleTypes.CRIT, 3);
	}

	/**
	 * 统一状态效果时长修饰（1.6.0 §5.4 五分支纪律——单注入多分支互斥命中）：
	 *   凋零 → 凋零保护（既有）+ 烬骨王计数（1.5.0）；
	 *   黑暗 → 明目（41，头盔，−35%/级）；
	 *   挖掘疲劳 → 潮涌（43，胸甲，−35%/级）；
	 *   其余有害 → 辟邪（44，护腿，−20%/级，排除集 = 凋零/黑暗/疲劳三大专属位）。
	 * 一个效果一次只走一个缩短分支（专属优先、辟邪兜底）；禁止对 addEffect 新增第二注入点。
	 */
	@ModifyVariable(
			method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At("HEAD"),
			argsOnly = true
	)
	private MobEffectInstance extraenchantry$reduceWitherDuration(MobEffectInstance effect) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (effect == null || effect.isInfiniteDuration()) {
			return effect;
		}

		// 分支 1+2：凋零（保护缩短 + 烬骨王触发计数，先计数后缩短）
		if (effect.is(MobEffects.WITHER)) {
			if (self instanceof net.minecraft.server.level.ServerPlayer witherPlayer) {
				realmikoto.extraenchantry.EliteEncounterManager.noteWitherApplication(
						witherPlayer, effect.getDuration());
			}
			return scaleWitherProtection(self, effect);
		}

		// 分支 3：黑暗 → 明目（头盔，每级 −35%）
		if (effect.is(MobEffects.DARKNESS)) {
			int clearsight = armorLevels(self, ExtraEnchantry.CLEARSIGHT, EquipmentSlot.HEAD);
			return scaleDuration(self, effect, clearsight * 0.35F, "clearsight");
		}

		// 分支 4：挖掘疲劳 → 潮涌（胸甲，每级 −35%）
		if (effect.is(MobEffects.MINING_FATIGUE)) {
			int tidesurge = armorLevels(self, ExtraEnchantry.TIDESURGE, EquipmentSlot.CHEST);
			return scaleDuration(self, effect, tidesurge * 0.35F, "tidesurge_fatigue");
		}

		// 分支 5：其余有害 → 辟邪（护腿，每级 −20%，上限 60%）
		if (!effect.getEffect().value().isBeneficial()) {
			int hexbreak = armorLevels(self, ExtraEnchantry.HEXBREAK, EquipmentSlot.LEGS);
			return scaleDuration(self, effect, Math.min(0.6F, hexbreak * 0.20F), "hexbreak");
		}
		return effect;
	}

	/** 凋零保护既有逻辑（装备总等级缩短，上限 90%）——从原注入抽出为内部方法 */
	private MobEffectInstance scaleWitherProtection(LivingEntity self, MobEffectInstance effect) {
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
		if (self.level() instanceof ServerLevel serverLevel && FxHelper.throttle(self, "wither_prot", 20)) {
			FxHelper.burst(serverLevel, self,
					net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR, 4, 0.3D);
			FxHelper.play(serverLevel, self, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_FLUTE, 0.3F, 1.2F);
		}
		float reduction = Math.min(0.9F, totalLevels * 0.15F);
		return effect.withScaledDuration(1.0F - reduction);
	}

	/** 单槽位指定附魔的等级（明目头 / 潮涌胸 / 辟邪腿） */
	private static int armorLevels(LivingEntity self,
			ResourceKey<Enchantment> key, EquipmentSlot slot) {
		ItemStack stack = self.getItemBySlot(slot);
		ItemEnchantments enchantments = stack.getEnchantments();
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(key)) {
				return enchantments.getLevel(enchantment);
			}
		}
		return 0;
	}

	/** 通用时长缩放（reduction ≥ 1.0 = 完全免疫 → 时长归 0；带 L1 节流反馈） */
	private static MobEffectInstance scaleDuration(LivingEntity self, MobEffectInstance effect,
			float reduction, String fxKey) {
		if (reduction <= 0.0F) {
			return effect;
		}
		if (reduction >= 1.0F) {
			return effect.withScaledDuration(0.0F);
		}
		if (self.level() instanceof ServerLevel serverLevel
				&& FxHelper.throttle(self, fxKey, 40)) {
			FxHelper.burst(serverLevel, self,
					net.minecraft.core.particles.ParticleTypes.END_ROD, 3, 0.3D);
		}
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
	 * 诸界浩劫：挑战生物掉落经验翻倍。
	 * getExperienceReward 是 26.2 统一的经验结算入口（dropExperience 内部调用，
	 * public final——所有生物共用这一份实现，单点注入全覆盖）。
	 */
	@Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
	private void extraenchantry$cataclysmDoubleXp(ServerLevel level, Entity killer,
			CallbackInfoReturnable<Integer> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		// 五境领主经验（1.5.0）：按配置覆盖（getExperienceReward 是 public final，
		// 无法覆写，统一走本注入点；优先于诸界浩劫翻倍判定）
		int lordXp = realmikoto.extraenchantry.EliteEncounterManager.lordExperience(self);
		if (lordXp >= 0) {
			cir.setReturnValue(lordXp);
			return;
		}
		if (self instanceof Mob mob && CavalryManager.isChallengeMob(mob)) {
			cir.setReturnValue(cir.getReturnValueI() * 2);
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
