package realmikoto.extraenchantry;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * 劫后余辉（Afterglow）状态管理：不死图腾被触发时给予的"余辉"增益。
 *
 * 效果（单级）：
 *   1. 生命值立即回满
 *   2. 10 秒锁血——期间生命值不会因任何伤害而降低
 *   3. 5 颗伤害吸收的黄色心（10 HP，取自金苹果的伤害吸收效果）
 *
 * 锁血与状态栏共用同一个 {@code extra-enchantry:afterglow} MobEffectInstance：
 * 施加后药水状态栏自动显示余辉图标与倒计时；isLocked 直接读取该效果
 * 是否存在（效果到期自动消失，锁血随之结束，无需独立计时器）。
 *
 * 增益只存在于内存中（不写物品、不做 NBT 持久化）：图腾触发时即被消耗，
 * 附魔随之消失，锁血计时到期后状态自动清除，无需额外清理逻辑。
 */
public final class AfterglowManager {

	/** 锁血持续时长（tick）＝ 10 秒 */
	private static final int LOCK_DURATION_TICKS = 200;

	/** 伤害吸收的黄色心数：5 颗心 = 10 HP */
	private static final int ABSORPTION_HEARTS = 5;
	private static final float ABSORPTION_HP = ABSORPTION_HEARTS * 2.0F;

	/** 伤害吸收效果时长（tick）——与金苹果一致（2 分钟） */
	private static final int ABSORPTION_DURATION_TICKS = 2400;

	private AfterglowManager() {
	}

	/**
	 * 触发余辉：回满生命值 + 施加 10 秒余辉效果（状态栏图标与锁血共用）+ 5 颗伤害吸收黄心。
	 * 调用时机必须晚于原版不死图腾的 {@code DeathProtection#applyEffects}
	 * （其首个效果是 ClearAllStatusEffects，提前施加会被一并清除）。
	 */
	public static void trigger(Player player) {
		player.setHealth(player.getMaxHealth());

		player.addEffect(new MobEffectInstance(ExtraEnchantryEffects.AFTERGLOW, LOCK_DURATION_TICKS, 0, false, true, true));

		// 4×(1+amplifier) 的阶梯（4/8/12 HP）给不出 10 HP，故先挂上金苹果的
		// 伤害吸收效果（决定黄心的效果来源与时长），再直接写入 10 HP 精确覆盖
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ABSORPTION_DURATION_TICKS, 0));
		player.setAbsorptionAmount(ABSORPTION_HP);
	}

	/** 判断玩家是否处于锁血状态（余辉效果实例仍在即锁定） */
	public static boolean isLocked(Player player) {
		return player.hasEffect(ExtraEnchantryEffects.AFTERGLOW);
	}
}
