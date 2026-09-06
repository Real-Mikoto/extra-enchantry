package realmikoto.extraenchantry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 余烬（Emberfall）状态管理：金胸甲与金马铠的免死机制。
 *
 * 金胸甲（玩家穿戴，CHEST 槽）：受到致命伤害时免死一次——保留 1 颗心 + 5 秒锁血，
 * 随后进入 60 秒「余烬」虚弱（缓慢 I + 挖掘疲劳 I，状态栏显示自定义图标与倒计时）；
 * 每次触发消耗金胸甲 50% 最大耐久（112 → 56），剩余耐久不足则不触发。
 *
 * 金马铠（马匹穿戴，BODY 槽）：**马与骑手共享余烬**——马或骑手任一方受到致命
 * 伤害都触发余烬，被救者保留 1 颗心 + 5 秒锁血 + 同款虚弱，另一方获得等长
 * 锁血窗口（仅同步「余烬」效果，不复制缓慢/挖掘疲劳）。触发后**马铠就地消耗
 * 消失**（播放装备耐久清零音效）——每件马铠仅一次免死，无需冷却计时。
 *
 * 锁血与状态栏共用同一个 {@code extra-enchantry:emberfall} MobEffectInstance：
 * 效果总时长 60 秒，其中**剩余时长 > 55 秒**（即触发后的前 5 秒）为锁血窗口，
 * 无需独立计时器。
 *
 * 与不死图腾的优先级：注入点在 {@code LivingEntity#checkTotemDeathProtection} 的
 * RETURN，仅当图腾未能救命时才尝试余烬——图腾优先。
 */
public final class EmberfallManager {

	/** 余烬状态总时长（tick）＝ 60 秒 */
	private static final int EMBER_DURATION_TICKS = 1200;

	/** 锁血窗口（tick）＝ 5 秒：效果剩余时长 > (总时长 - 窗口) 即处于锁血期 */
	private static final int LOCK_WINDOW_TICKS = 100;

	/** 免死后保留的生命值：1 颗心 = 2 HP（比不死图腾的半颗心宽裕） */
	private static final float SURVIVE_HEALTH = 2.0F;

	private EmberfallManager() {
	}

	/**
	 * 尝试触发余烬免死。返回 true 表示已救命（调用方据此取消死亡流程）。
	 * 触发条件（三选一）：
	 *   - CHEST 槽（金胸甲）带余烬 + 可掉耐久 + 剩余耐久 > 50% 最大耐久（金胸甲路径）；
	 *   - BODY 槽（金马铠）带余烬——自身（马）死亡触发；
	 *   - 坐骑 BODY 槽的金马铠带余烬——骑手死亡触发（马与骑手共享余烬）。
	 */
	public static boolean trySave(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return false;
		}
		ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
		if (ExtraEnchantry.getEmberfallLevel(chest) > 0 && chest.isDamageableItem()) {
			return triggerChestplate(entity, chest, level);
		}
		ItemStack body = entity.getItemBySlot(EquipmentSlot.BODY);
		if (ExtraEnchantry.getEmberfallLevel(body) > 0) {
			return triggerHorseArmor(entity, entity, level);
		}
		if (entity.getVehicle() instanceof LivingEntity mount) {
			ItemStack mountBody = mount.getItemBySlot(EquipmentSlot.BODY);
			if (ExtraEnchantry.getEmberfallLevel(mountBody) > 0) {
				return triggerHorseArmor(entity, mount, level);
			}
		}
		return false;
	}

	/** 免死生效后的统一收尾：实战成就「余烬不灭」（仅被救者是玩家时授予） */
	private static void awardEmberSave(LivingEntity saved) {
		if (saved instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			Advancements.award(serverPlayer, Advancements.EMBER_SAVE);
		}
	}

	/** 金胸甲路径：耐久消耗 + 免死 + 效果（原版行为） */
	private static boolean triggerChestplate(LivingEntity entity, ItemStack chest, ServerLevel level) {
		int cost = Math.max(1, chest.getMaxDamage() / 2);
		int remaining = chest.getMaxDamage() - chest.getDamageValue();
		if (remaining <= cost) {
			return false; // 耐久不足，不触发
		}
		chest.setDamageValue(chest.getDamageValue() + cost);
		applySurvivalEffects(entity);
		playSaveFx(entity, level);
		awardEmberSave(entity);
		return true;
	}

	/**
	 * 金马铠路径：被救者（马或骑手）免死 + 另一方同步锁血窗口 + **马铠就地消耗消失**
	 * （装备耐久清零音效）。每件马铠仅此一次，无需冷却。
	 */
	private static boolean triggerHorseArmor(LivingEntity saved, LivingEntity mount, ServerLevel level) {
		applySurvivalEffects(saved);
		// 共享：被救者与坐骑互相同步「余烬」效果（5 秒免伤锁血窗口）；
		// 不复制缓慢/挖掘疲劳——未濒死的一方不承担虚弱代价
		LivingEntity other = saved == mount
				? (mount.getFirstPassenger() instanceof LivingEntity rider ? rider : null)
				: mount;
		if (other != null && other.isAlive()) {
			other.addEffect(new MobEffectInstance(
					ExtraEnchantryEffects.EMBERFALL, EMBER_DURATION_TICKS, 0, false, true, true));
		}
		// 马铠一次性消耗：卸下并播放装备耐久清零音效（同原版装备耐久归零）
		mount.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
		level.playSound(null, mount.getX(), mount.getY(), mount.getZ(),
				SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 1.0F,
				0.8F + level.getRandom().nextFloat() * 0.4F);
		playSaveFx(saved, level);
		awardEmberSave(saved);
		return true;
	}

	/** 免死共用结算：保留 1 颗心 + 清空吸收 + 60 秒余烬/缓慢/挖掘疲劳 */
	private static void applySurvivalEffects(LivingEntity entity) {
		entity.setHealth(SURVIVE_HEALTH);
		entity.setAbsorptionAmount(0.0F);
		entity.addEffect(new MobEffectInstance(
				ExtraEnchantryEffects.EMBERFALL, EMBER_DURATION_TICKS, 0, false, true, true));
		entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, EMBER_DURATION_TICKS, 0));
		entity.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, EMBER_DURATION_TICKS, 0));
	}

	/** 触发视觉：灵魂火焰自身体升起（服务端生成，自动广播附近玩家） */
	private static void playSaveFx(LivingEntity entity, ServerLevel level) {
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				entity.getX(), entity.getY(0.5D), entity.getZ(),
				40, entity.getBbWidth() * 0.6D, entity.getBbHeight() * 0.6D, entity.getBbWidth() * 0.6D, 0.02D);
	}

	/** 是否处于余烬锁血窗口（触发后的前 5 秒，生命值与吸收心不因伤害降低） */
	public static boolean isLocked(LivingEntity entity) {
		MobEffectInstance ember = entity.getEffect(ExtraEnchantryEffects.EMBERFALL);
		return ember != null && ember.getDuration() > EMBER_DURATION_TICKS - LOCK_WINDOW_TICKS;
	}
}
