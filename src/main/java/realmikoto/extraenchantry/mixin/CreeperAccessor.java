package realmikoto.extraenchantry.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 苦力怕访问器（诸界浩劫的闪电苦力怕用）：
 * 26.2 反编译确认——充能标记 DATA_IS_POWERED 为私有静态合成数据（无 setPowered 公开方法），
 * 引信时长为私有字段 maxSwell（原版 30 tick，NBT 键 "Fuse"，无公开 setter）。
 * 挑战苦力怕：直接置充能位 + 把引信压到 8 tick（减少 3/4）。
 */
@Mixin(Creeper.class)
public interface CreeperAccessor {

	@Accessor("DATA_IS_POWERED")
	static EntityDataAccessor<Boolean> extraenchantry$dataIsPowered() {
		throw new AssertionError();
	}

	@Accessor("maxSwell")
	void extraenchantry$setMaxSwell(int maxSwell);
}
