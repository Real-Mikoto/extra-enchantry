package realmikoto.extraenchantry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.function.Supplier;

/**
 * 五境领主刷怪蛋（1.7.3）：管理/测试用便捷入口，创造模式物品栏可见。
 *
 * 26.2 机制：{@link SpawnEggItem} 无参构造，刷怪目标与 NBT 全部由
 * {@link DataComponents#ENTITY_DATA}（{@code TypedEntityData<EntityType<?>>}）组件携带。
 * 生成的是本模领主子类实例（如 {@code OverwardenEntity}）而非原版基底，
 * 需要自定义生成逻辑 → 覆写 {@code useOn}（拦截原版 spawnMob 后改走领主工厂）。
 *
 * 注意：领主 {@code shouldBeSaved()=false}（防孤儿持久化）——蛋生成的领主同样不保存，
 * 重启消失，符合「领主是遭遇事件而非世界生物」的既定设计。
 */
public final class LordSpawnEggs {

	/** 五境刷怪蛋（顺序 = 难度梯度：沼泽 → 下界 → 海洋 → 深暗 → 末地） */
	public static final Item HAG_SPAWN_EGG = egg("hag_spawn_egg", "hag", () -> EntityTypes.WITCH);
	public static final Item EMBERBONE_SPAWN_EGG = egg("emberbone_spawn_egg", "emberbone", () -> EntityTypes.WITHER_SKELETON);
	public static final Item TIDAL_SPAWN_EGG = egg("tidal_spawn_egg", "tidal", () -> EntityTypes.ELDER_GUARDIAN);
	public static final Item OVERWARDEN_SPAWN_EGG = egg("overwarden_spawn_egg", "overwarden", () -> EntityTypes.WARDEN);
	public static final Item ENDER_SPAWN_EGG = egg("ender_spawn_egg", "ender", () -> EntityTypes.ENDERMAN);

	/** 创造栏展示顺序 */
	public static final List<Item> ALL = List.of(
			HAG_SPAWN_EGG, EMBERBONE_SPAWN_EGG, TIDAL_SPAWN_EGG, OVERWARDEN_SPAWN_EGG, ENDER_SPAWN_EGG);

	/** 创造栏用：构造带 ENTITY_DATA 组件的蛋 ItemStack（内部类透传） */
	public static ItemStack stack(Item egg, int count) {
		return egg instanceof LordEggItem lordEgg ? lordEgg.createStack(count) : new ItemStack(egg, count);
	}

	private LordSpawnEggs() {
	}

	public static void register() {
		net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,
				ExtraEnchantry.id("hag_spawn_egg"), HAG_SPAWN_EGG);
		net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,
				ExtraEnchantry.id("emberbone_spawn_egg"), EMBERBONE_SPAWN_EGG);
		net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,
				ExtraEnchantry.id("tidal_spawn_egg"), TIDAL_SPAWN_EGG);
		net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,
				ExtraEnchantry.id("overwarden_spawn_egg"), OVERWARDEN_SPAWN_EGG);
		net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,
				ExtraEnchantry.id("ender_spawn_egg"), ENDER_SPAWN_EGG);
	}

	private static Item egg(String id, String realmId, Supplier<EntityType<? extends Mob>> type) {
		return new LordEggItem(new Item.Properties()
				.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id(id)))
				.stacksTo(16).rarity(Rarity.EPIC), realmId, type);
	}

	/**
	 * 领主蛋：右键方块生成对应领主子类实例（复用 1.5.0 领主工厂 + 觉醒前属性），
	 * 生成时给满血 + 短暂无敌帧，与遭遇觉醒一致；仇恨不锁定（自由测试用）。
	 */
	private static final class LordEggItem extends SpawnEggItem {

		private final String realmId;
		private final Supplier<EntityType<? extends Mob>> type;

		private LordEggItem(Properties properties, String realmId, Supplier<EntityType<? extends Mob>> type) {
			super(properties);
			this.realmId = realmId;
			this.type = type;
		}

		/** 供创造栏构造：带 ENTITY_DATA 组件的 ItemStack（26.2 原版蛋逻辑读取所需） */
		public ItemStack createStack(int count) {
			ItemStack stack = new ItemStack(this, count);
			CompoundTag tag = new CompoundTag();
			tag.putString("id", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
					.getKey(type.get()).toString());
			stack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(type.get(), tag));
			return stack;
		}

		@Override
		public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
			// 拦截原版生成（原版只会生成基底实体），改走本模领主工厂
			if (context.getLevel() instanceof ServerLevel level && context.getPlayer() != null) {
				Mob lord = EliteEncounterManager.createLordForEgg(level, realmId);
				if (lord != null) {
					lord.snapTo(context.getClickLocation().x, context.getClickLocation().y,
							context.getClickLocation().z,
							level.getRandom().nextFloat() * 360.0F, 0.0F);
					lord.setHealth(lord.getMaxHealth());
					lord.invulnerableTime = 40;
					level.addFreshEntity(lord);
					if (!context.getPlayer().getAbilities().instabuild) {
						context.getItemInHand().shrink(1);
					}
					return net.minecraft.world.InteractionResult.SUCCESS;
				}
			}
			return super.useOn(context);
		}
	}
}
