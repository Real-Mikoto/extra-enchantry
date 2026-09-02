package realmikoto.extraenchantry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 余烬（Emberfall）状态管理：**金胸甲专属**的免死机制。
 *
 * 效果（单级）：受到致命伤害时免死一次——保留 1 颗心 + 5 秒锁血，
 * 随后进入 60 秒「余烬」虚弱（缓慢 I + 挖掘疲劳 I）；
 * 每次触发消耗金胸甲 50% 最大耐久（112 → 56），剩余耐久不足则不触发。
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
	 * 触发条件：胸甲带余烬 + 可掉耐久 + 剩余耐久 > 50% 最大耐久（不允许把胸甲打碎）。
	 */
	public static boolean trySave(LivingEntity entity) {
		ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
		if (ExtraEnchantry.getEmberfallLevel(chest) <= 0 || !chest.isDamageableItem()) {
			return false;
		}
		int cost = Math.max(1, chest.getMaxDamage() / 2);
		int remaining = chest.getMaxDamage() - chest.getDamageValue();
		if (remaining <= cost) {
			return false; // 耐久不足，不触发
		}
		if (!(entity.level() instanceof ServerLevel level)) {
			return false;
		}

		chest.setDamageValue(chest.getDamageValue() + cost);
		entity.setHealth(SURVIVE_HEALTH);
		entity.setAbsorptionAmount(0.0F);
		entity.addEffect(new MobEffectInstance(
				ExtraEnchantryEffects.EMBERFALL, EMBER_DURATION_TICKS, 0, false, true, true));
		entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, EMBER_DURATION_TICKS, 0));
		entity.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, EMBER_DURATION_TICKS, 0));

		// 触发视觉：灵魂火焰自身体升起（服务端生成，自动广播附近玩家）
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				entity.getX(), entity.getY(0.5D), entity.getZ(),
				40, entity.getBbWidth() * 0.6D, entity.getBbHeight() * 0.6D, entity.getBbWidth() * 0.6D, 0.02D);
		return true;
	}

	/** 是否处于余烬锁血窗口（触发后的前 5 秒，生命值与吸收心不因伤害降低） */
	public static boolean isLocked(LivingEntity entity) {
		MobEffectInstance ember = entity.getEffect(ExtraEnchantryEffects.EMBERFALL);
		return ember != null && ember.getDuration() > EMBER_DURATION_TICKS - LOCK_WINDOW_TICKS;
	}
}
