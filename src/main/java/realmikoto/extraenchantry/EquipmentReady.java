package realmikoto.extraenchantry;

/**
 * EquipmentReady duck 接口（独立类，避开 mixin 包）。
 *
 * 历史教训：此接口原先定义在 {@code mixin.LivingEntityMixin} 的内部接口，
 * EntityMixin 直接引用它会触发 "IllegalClassLoadError: is in a defined mixin
 * package ... and cannot be referenced directly"——跨 Mixin 读取 duck 接口时，
 * 接口类必须放在正常包（v1.7.4 README「跨 Mixin 读取标记用 duck 接口」的完整形态）。
 *
 * 语义：LivingEntity 的 equipment 在其自身构造体内赋值（早于 {@code <init>} TAIL），
 * LivingEntityMixin 在构造 TAIL 置位标记；查询方通过本接口读取。
 */
public interface EquipmentReady {

	boolean extraenchantry$isEquipmentReady();
}
