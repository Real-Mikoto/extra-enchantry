package realmikoto.extraenchantry;

/**
 * 坠星（Starfall）烟花火箭快照接口：由 FireworkRocketEntityMixin 实现，
 * 供 CrossbowItemMixin 在发射时写入标记，爆炸增强（ModifyConstant）读取。
 */
public interface StarfallAccess {

	/** 标记该火箭来自坠星弩 */
	void extraenchantry$setStarfall();

	/** 该火箭是否带坠星增强 */
	boolean extraenchantry$isStarfall();
}
