package realmikoto.extraenchantry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

/**
 * 五境领主的材料与宝匣（1.5.0「五境领主」§0.2.5 / §7.1）。
 *
 * 材料 ×5（幽匿核心 / 烬核 / 渊潮之泪 / 魔药精华 / 虚空碎片）：领主 100% 掉落，
 * 全部可作为铁砧修复材料（修复 50% 耐久、固定 5 级经验，见 AnvilMenuMixin）；
 * 宝匣 ×5（幽匿 / 烬火 / 渊潮 / 魔药 / 虚空）：合成 3 材料 + 1 采集物，
 * 右键开启后随机获得本境主题附魔书（一次性消耗）。
 *
 * 材料与宝匣各境互不通用（经济闭环自洽），但铁砧修复行为统一。
 */
public final class RealmTreasures {

	// ============ 材料 ============

	public static final Item SCULK_CORE = material("sculk_core", Rarity.RARE);
	public static final Item EMBER_CORE = material("ember_core", Rarity.RARE);
	public static final Item TIDAL_TEAR = material("tidal_tear", Rarity.RARE);
	public static final Item POTION_ESSENCE = material("potion_essence", Rarity.RARE);
	public static final Item VOID_SHARD = material("void_shard", Rarity.RARE);

	/** 五种材料（铁砧修复材料集合判定用） */
	public static final Set<Item> MATERIALS =
			Set.of(SCULK_CORE, EMBER_CORE, TIDAL_TEAR, POTION_ESSENCE, VOID_SHARD);

	// ============ 宝匣 ============

	/** 幽匿宝匣：蚀命 I–III / 无踪 I–II / 御风 I–III */
	public static final Item SCULK_CASKET = casket("sculk_casket", List.of(
			new BookDrop(ExtraEnchantry.LIFE_EROSION, 1, 3),
			new BookDrop(ExtraEnchantry.UNSEEN, 1, 2),
			new BookDrop(ExtraEnchantry.WINDRIDER, 1, 3)));

	/** 烬火宝匣：炽焰行者 I–II / 拓阶 I–II */
	public static final Item EMBER_CASKET = casket("ember_casket", List.of(
			new BookDrop(ExtraEnchantry.BLAZING_WALKER, 1, 2),
			new BookDrop(ExtraEnchantry.TIER_BREAK, 1, 2)));

	/** 渊潮宝匣：空跃 I–II / 无踪 I–II */
	public static final Item TIDAL_CASKET = casket("tidal_casket", List.of(
			new BookDrop(ExtraEnchantry.SKYWARD, 1, 2),
			new BookDrop(ExtraEnchantry.UNSEEN, 1, 2)));

	/** 魔药宝匣：断罪 I / 假象 I–II */
	public static final Item POTION_CASKET = casket("potion_casket", List.of(
			new BookDrop(ExtraEnchantry.JUDGEMENT, 1, 1),
			new BookDrop(ExtraEnchantry.DECOY, 1, 2)));

	/** 虚空宝匣：御风 I–II / 誓约 I */
	public static final Item VOID_CASKET = casket("void_casket", List.of(
			new BookDrop(ExtraEnchantry.WINDRIDER, 1, 2),
			new BookDrop(ExtraEnchantry.OATHBOUND, 1, 1)));

	/** 创造模式物品栏展示顺序（材料 ×5 + 宝匣 ×5） */
	public static final List<Item> CREATIVE_ITEMS = List.of(
			SCULK_CORE, EMBER_CORE, TIDAL_TEAR, POTION_ESSENCE, VOID_SHARD,
			SCULK_CASKET, EMBER_CASKET, TIDAL_CASKET, POTION_CASKET, VOID_CASKET);

	/** 宝匣掉落池：附魔 + 等级区间（右键开启时等概率先选条目，再在区间内随机等级） */
	public record BookDrop(ResourceKey<Enchantment> enchantment, int minLevel, int maxLevel) {
	}

	private RealmTreasures() {
	}

	// ============ 注册 ============

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("sculk_core"), SCULK_CORE);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("ember_core"), EMBER_CORE);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("tidal_tear"), TIDAL_TEAR);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("potion_essence"), POTION_ESSENCE);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("void_shard"), VOID_SHARD);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("sculk_casket"), SCULK_CASKET);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("ember_casket"), EMBER_CASKET);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("tidal_casket"), TIDAL_CASKET);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("potion_casket"), POTION_CASKET);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("void_casket"), VOID_CASKET);
	}

	private static Item material(String id, Rarity rarity) {
		return new Item(new Item.Properties()
				.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id(id)))
				.rarity(rarity));
	}

	private static Item casket(String id, List<BookDrop> pool) {
		return new CasketItem(new Item.Properties()
				.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id(id)))
				.stacksTo(16).rarity(Rarity.UNCOMMON), pool);
	}

	// ============ 领主掉落 ============

	/**
	 * 领主死亡掉落（覆写 dropCustomDeathLoot 调用）：100% 本境材料 + 按概率的附魔书。
	 * 不写任何原版掉落表 / 附魔 JSON（设计 §9.2 差异化验收）。
	 */
	public static void dropLordLoot(LivingEntity lord, EncounterDef def, ServerLevel level) {
		ItemStack material = new ItemStack(def.material(), def.materialCount());
		lord.spawnAtLocation(level, material);
		for (EncounterDef.LordDrop drop : def.bookDrops()) {
			if (level.getRandom().nextFloat() < drop.chance()) {
				lord.spawnAtLocation(level, enchantedBook(level, drop.enchantment(), drop.level()));
			}
		}
		// 1.6.0 普通领主器魂小概率掉落（20% I–II，§1.5 表）
		maybeDropSoulBook(lord, def, level);
	}

	// ============ 1.6.0 觉醒掉落与器魂（§1.5 / §2） ============

	/** 各境器魂附魔（realm id → 附魔 Key，§2 各条） */
	private static final java.util.Map<String, ResourceKey<Enchantment>> SOUL_ENCHANTMENTS =
			java.util.Map.of(
					"overwarden", ExtraEnchantry.CLEARSIGHT,
					"emberbone", ExtraEnchantry.WITHERBLADE,
					"tidal", ExtraEnchantry.TIDESURGE,
					"hag", ExtraEnchantry.HEXBREAK,
					"ender", ExtraEnchantry.VOIDBLINK);

	/** 器魂附魔书掉落（境 id → 等级数组 [普通概率, 普通最高级, 觉醒最高级]） */
	public static ResourceKey<Enchantment> soulEnchantmentOf(String realmId) {
		return SOUL_ENCHANTMENTS.get(realmId);
	}

	/** 器魂书构造（等级区间随机） */
	public static ItemStack soulBook(ServerLevel level, String realmId, int minLevel, int maxLevel) {
		ResourceKey<Enchantment> key = soulEnchantmentOf(realmId);
		int lvl = maxLevel > minLevel
				? minLevel + level.getRandom().nextInt(maxLevel - minLevel + 1)
				: minLevel;
		return enchantedBook(level, key, lvl);
	}

	/**
	 * 觉醒领主掉落（1.6.0 §1.5）：境材料 ×2 + 器魂书 II–III 100% + 觉醒徽记 100%。
	 * 既有附魔书掉落（原 35% 蚀命 III 等）照旧走 dropLordLoot——由调用方先调原掉落再调本方法。
	 */
	public static void dropAwakenedLoot(LivingEntity lord, EncounterDef def, ServerLevel level,
			net.minecraft.server.level.ServerPlayer killer) {
		ItemStack material = new ItemStack(def.material(), def.materialCount() * 2);
		lord.spawnAtLocation(level, material);
		lord.spawnAtLocation(level, soulBook(level, def.id(), 2, 3));
		ItemStack sigil = WarArtifacts.sigil(def.id());
		if (!killer.getInventory().add(sigil)) {
			lord.spawnAtLocation(level, sigil);
		}
		FxHelper.burstAt(level, lord.getX(), lord.getY(0.5D), lord.getZ(),
				net.minecraft.core.particles.ParticleTypes.END_ROD, 24, 0.6D);
		// 1.6.0+ 隐藏进度：器魂入包即触发「五魂俱全」扫描（装备形态由 LineageManager.tick 兜底）
		LineageManager.checkSoulCollector(killer);
	}

	/** 普通领主的器魂小概率掉落（20%，I–II 级）——并入 dropLordLoot 调用链 */
	public static void maybeDropSoulBook(LivingEntity lord, EncounterDef def, ServerLevel level) {
		if (level.getRandom().nextFloat() < 0.20F) {
			lord.spawnAtLocation(level, soulBook(level, def.id(), 1, 2));
		}
	}

	/** 构造指定等级的附魔书（STORED_ENCHANTMENTS 组件，与创造栏一致） */
	public static ItemStack enchantedBook(ServerLevel level, ResourceKey<Enchantment> key, int level1) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		var holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
		ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(
				ItemEnchantments.EMPTY);
		stored.set(holder, level1);
		book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
		return book;
	}

	// ============ 宝匣物品 ============

	/**
	 * 宝匣：右键开启，随机获得本境主题附魔书中的一本（一次性消耗）。
	 * 与手札 / 卷轴等 lore 类物品同源写法：不进创造栏的随机产出，服务端判定。
	 */
	private static final class CasketItem extends Item {

		private final List<BookDrop> pool;

		private CasketItem(Properties properties, List<BookDrop> pool) {
			super(properties);
			this.pool = pool;
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			ServerLevel serverLevel = (ServerLevel) level;
			BookDrop drop = pool.get(serverLevel.getRandom().nextInt(pool.size()));
			int enchLevel = drop.minLevel();
			if (drop.maxLevel() > drop.minLevel()) {
				enchLevel = drop.minLevel()
						+ serverLevel.getRandom().nextInt(drop.maxLevel() - drop.minLevel() + 1);
			}
			ItemStack book = enchantedBook(serverLevel, drop.enchantment(), enchLevel);
			stack.consume(1, player);
			if (!player.getInventory().add(book)) {
				player.drop(book, false);
			}
			FxHelper.burst(serverLevel, player, net.minecraft.core.particles.ParticleTypes.ENCHANT, 12, 0.5D);
			FxHelper.play(serverLevel, player, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.8F, 1.2F);
			return InteractionResult.CONSUME;
		}
	}
}
