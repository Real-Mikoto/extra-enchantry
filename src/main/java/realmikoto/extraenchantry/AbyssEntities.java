package realmikoto.extraenchantry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.server.level.ServerLevel;
import realmikoto.extraenchantry.entity.abyss.EchoWraithEntity;
import realmikoto.extraenchantry.entity.abyss.ResonanceBeastEntity;
import realmikoto.extraenchantry.entity.abyss.TidebornEntity;
import realmikoto.extraenchantry.HeartWardenEntity;

/**
 * 幽渊实体注册（1.8.2「群」）。
 *
 * 声纹驱动的生态：刷新完全走幽渊 biome 的空 spawners + 本类运行时规则——
 * 幽渊内所有自然刷新统一由 {@link AbyssSpawning} 控制（越响越危险）。
 * 渊息者为「居民」，只经结构/锚点附近生成（1.8.2 简化：不自然刷新，创造蛋 + 锚点随行）。
 */
public final class AbyssEntities {

	// ============ EntityType 注册（26.2：build(ResourceKey) 直接以注册键命名） ============

	public static final EntityType<EchoWraithEntity> ECHO_WRAITH = EntityType.Builder
			.of(EchoWraithEntity::new, MobCategory.MONSTER)
			.sized(0.7F, 2.2F)
			.clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, ExtraEnchantry.id("echo_wraith")));

	public static final EntityType<TidebornEntity> TIDEBORN = EntityType.Builder
			.of(TidebornEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.95F)
			.clientTrackingRange(10)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, ExtraEnchantry.id("tideborn")));

	public static final EntityType<ResonanceBeastEntity> RESONANCE_BEAST = EntityType.Builder
			.of(ResonanceBeastEntity::new, MobCategory.CREATURE)
			.sized(0.9F, 1.0F)
			.clientTrackingRange(10)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, ExtraEnchantry.id("resonance_beast")));

	/** 幽匿幼体（1.8.2 补齐）：成群、被声音激活的小型敌对 */
	public static final EntityType<realmikoto.extraenchantry.entity.abyss.SculkLarvaEntity> SCULK_LARVA =
			EntityType.Builder.of(realmikoto.extraenchantry.entity.abyss.SculkLarvaEntity::new, MobCategory.MONSTER)
					.sized(0.5F, 0.5F)
					.clientTrackingRange(8)
					.build(ResourceKey.create(Registries.ENTITY_TYPE, ExtraEnchantry.id("sculk_larva")));

	/** 记忆残影（1.8.4 补齐）：中立可对话的封印纪元残影 */
	public static final EntityType<realmikoto.extraenchantry.entity.abyss.MemoryShadeEntity> MEMORY_SHADE =
			EntityType.Builder.of(realmikoto.extraenchantry.entity.abyss.MemoryShadeEntity::new, MobCategory.CREATURE)
					.sized(0.6F, 1.9F)
					.clientTrackingRange(10)
					.build(ResourceKey.create(Registries.ENTITY_TYPE, ExtraEnchantry.id("memory_shade")));

	/** 无声者（1.8.2 补齐）：完全无声的精英（对"声即光"的反噬） */
	public static final EntityType<realmikoto.extraenchantry.entity.abyss.SoundlessEntity> SOUNDLESS =
			EntityType.Builder.of(realmikoto.extraenchantry.entity.abyss.SoundlessEntity::new, MobCategory.MONSTER)
					.sized(0.7F, 2.2F)
					.clientTrackingRange(10)
					.build(ResourceKey.create(Registries.ENTITY_TYPE, ExtraEnchantry.id("soundless")));

	/** 渊心守望者（1.8.5）：幽渊终局 Boss（MONSTER + 不持久化，遭遇事件纪律） */
	public static final EntityType<HeartWardenEntity> HEART_WARDEN = EntityType.Builder
			.of(HeartWardenEntity::new, MobCategory.MONSTER)
			.sized(0.9F, 2.9F)
			.clientTrackingRange(12)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, ExtraEnchantry.id("heart_warden")));

	private AbyssEntities() {
	}


	public static void register() {
		Registry.register(BuiltInRegistries.ENTITY_TYPE, ExtraEnchantry.id("echo_wraith"), ECHO_WRAITH);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, ExtraEnchantry.id("tideborn"), TIDEBORN);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, ExtraEnchantry.id("resonance_beast"), RESONANCE_BEAST);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, ExtraEnchantry.id("heart_warden"), HEART_WARDEN);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, ExtraEnchantry.id("sculk_larva"), SCULK_LARVA);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, ExtraEnchantry.id("memory_shade"), MEMORY_SHADE);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, ExtraEnchantry.id("soundless"), SOUNDLESS);

		FabricDefaultAttributeRegistry.register(ECHO_WRAITH, EchoWraithEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(TIDEBORN, TidebornEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(RESONANCE_BEAST, ResonanceBeastEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(HEART_WARDEN, HeartWardenEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SCULK_LARVA,
				realmikoto.extraenchantry.entity.abyss.SculkLarvaEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(MEMORY_SHADE,
				realmikoto.extraenchantry.entity.abyss.MemoryShadeEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SOUNDLESS,
				realmikoto.extraenchantry.entity.abyss.SoundlessEntity.createAttributes());
	}
}
