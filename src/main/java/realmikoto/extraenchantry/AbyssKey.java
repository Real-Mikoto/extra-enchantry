package realmikoto.extraenchantry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * 幽渊维度（1.8.0「门」）：维度注册键与工具方法。
 *
 * 维度本体由数据包定义（data/extra-enchantry/dimension/abyss.json +
 * dimension_type/abyss.json + worldgen/*），此处只持引用键。
 * 世界加载时机：首次有玩家进入时创建（原版对 data-driven 维度的默认行为）。
 */
public final class AbyssKey {

	/** 幽渊维度 Level 键 */
	public static final ResourceKey<Level> ABYSS =
			ResourceKey.create(Registries.DIMENSION, id("abyss"));

	/** 幽渊位置名（进度 / 提示用） */
	public static final String DISPLAY_NAME = "幽渊";

	private AbyssKey() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ExtraEnchantry.MOD_ID, path);
	}

	/** 玩家当前是否在幽渊 */
	public static boolean isIn(net.minecraft.world.entity.Entity entity) {
		return entity.level().dimension() == ABYSS;
	}
}
