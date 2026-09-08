package realmikoto.extraenchantry;

/**
 * 五境领主统一标记（1.5.0「五境领主」§6.2）。
 *
 * 五境领主各自继承不同的原版实体类型，无法抽公共父类；本接口提供统一识别入口：
 *   - {@code MobMixin}：仇恨锁定裁决（觉醒后 10 秒锁定触发者）；
 *   - {@code ExtraEnchantry#isBossLike}：断罪不斩杀改 ×2、曳钩不可拉拽；
 *   - {@code LivingEntityMixin#getExperienceReward}：领主经验按配置结算；
 *   - {@code EliteEncounterManager}：领主死亡 → 击杀进度与隐藏进度判定。
 */
public interface EliteLord {

	/** 领主共享运行时（技能冷却 / 属性 / boss 血条） */
	LordRuntime lordRuntime();
}
