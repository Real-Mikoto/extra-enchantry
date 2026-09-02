package realmikoto.extraenchantry;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 自定义状态效果注册：
 * - 余辉（Afterglow）：劫后余辉的锁血计时载体（BENEFICIAL）
 * - 余烬（Emberfall）：金胸甲免死后的虚弱计时载体（HARMFUL），
 *   前 5 秒为锁血窗口（剩余时长判定），全程伴随缓慢与挖掘疲劳。
 * 两者均为纯视觉/计时载体——无属性修改，具体逻辑在各自的 Mixin 拦截里。
 * 注册后药水状态栏自动显示图标与倒计时
 * （纹理 assets/extra-enchantry/textures/mob_effect/<id>.png）。
 */
public final class ExtraEnchantryEffects {

	/** 余辉：暖金色调，结束粒子用图腾再生环境粒子（构造器 protected，匿名子类暴露） */
	public static final Holder<MobEffect> AFTERGLOW = Holder.direct(new MobEffect(
			MobEffectCategory.BENEFICIAL, 0xFFC850, ParticleTypes.TOTEM_OF_UNDYING) {
	});

	/** 余烬：暗红色调（HARMFUL 红框），结束粒子用灵魂火焰 */
	public static final Holder<MobEffect> EMBERFALL = Holder.direct(new MobEffect(
			MobEffectCategory.HARMFUL, 0x8B2500, ParticleTypes.SOUL_FIRE_FLAME) {
	});

	private ExtraEnchantryEffects() {
	}

	public static void register() {
		net.minecraft.core.Registry.register(BuiltInRegistries.MOB_EFFECT,
				ExtraEnchantry.id("afterglow"), AFTERGLOW.value());
		net.minecraft.core.Registry.register(BuiltInRegistries.MOB_EFFECT,
				ExtraEnchantry.id("emberfall"), EMBERFALL.value());
	}
}
