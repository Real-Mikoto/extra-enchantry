package realmikoto.extraenchantry;

import net.minecraft.world.item.ItemStack;

/**
 * 归羽（Homing Plume）箭矢快照接口：由 AbstractArrowMixin 实现，
 * 供发射管线（ProjectileWeaponItemMixin）写入等级、
 * HomingPlumeManager 在延迟返还时读取待返还物品。
 */
public interface HomingPlumeAccess {

	/** 写入发射时的归羽等级快照（0 = 无归羽） */
	void extraenchantry$setHomingLevel(int level);

	/** 读取发射时的归羽等级快照 */
	int extraenchantry$getHomingLevel();

	/** 该箭矢被拾取/返还时对应的物品栈（透传 protected 的 getPickupItem） */
	ItemStack extraenchantry$getPickupItem();
}
