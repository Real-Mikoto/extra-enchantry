package realmikoto.extraenchantry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.util.TriState;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.world.item.enchantment.ItemEnchantments;

	// 1.4.0 配饰（防火判定）
	import net.minecraft.core.component.DataComponents;
	import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtraEnchantry implements ModInitializer {
	public static final String MOD_ID = "extra-enchantry";

	// 凋零保护附魔（数据驱动定义于 data/extra-enchantry/enchantment/wither_protection.json）
	public static final ResourceKey<Enchantment> WITHER_PROTECTION =
			ResourceKey.create(Registries.ENCHANTMENT, id("wither_protection"));

	// 炽焰行者附魔（数据驱动定义于 data/extra-enchantry/enchantment/blazing_walker.json）
	public static final ResourceKey<Enchantment> BLAZING_WALKER =
			ResourceKey.create(Registries.ENCHANTMENT, id("blazing_walker"));

	// 破限附魔（数据驱动定义于 data/extra-enchantry/enchantment/limit_break.json）
	public static final ResourceKey<Enchantment> LIMIT_BREAK =
			ResourceKey.create(Registries.ENCHANTMENT, id("limit_break"));

	// 拓阶附魔（数据驱动定义于 data/extra-enchantry/enchantment/tier_break.json）
	public static final ResourceKey<Enchantment> TIER_BREAK =
			ResourceKey.create(Registries.ENCHANTMENT, id("tier_break"));

	// 触及附魔（数据驱动定义于 data/extra-enchantry/enchantment/reach.json）
	public static final ResourceKey<Enchantment> REACH =
			ResourceKey.create(Registries.ENCHANTMENT, id("reach"));

	// 假象附魔（数据驱动定义于 data/extra-enchantry/enchantment/decoy.json）
	public static final ResourceKey<Enchantment> DECOY =
			ResourceKey.create(Registries.ENCHANTMENT, id("decoy"));

	// 原版摔落保护（破限腿甲解锁跨部位附魔的钥匙）
	public static final ResourceKey<Enchantment> FEATHER_FALLING =
			ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace("feather_falling"));

	// 腿甲标签（破限解锁摔落保护的适用部位）
	public static final TagKey<Item> LEG_ARMOR_TAG =
			TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("enchantable/leg_armor"));

	// 汲取附魔（数据驱动定义于 data/extra-enchantry/enchantment/siphon.json）
	public static final ResourceKey<Enchantment> SIPHON =
			ResourceKey.create(Registries.ENCHANTMENT, id("siphon"));

	// 蚀命附魔（数据驱动定义于 data/extra-enchantry/enchantment/life_erosion.json）
	public static final ResourceKey<Enchantment> LIFE_EROSION =
			ResourceKey.create(Registries.ENCHANTMENT, id("life_erosion"));

	// 活力附魔（数据驱动定义于 data/extra-enchantry/enchantment/vitality.json）
	public static final ResourceKey<Enchantment> VITALITY =
			ResourceKey.create(Registries.ENCHANTMENT, id("vitality"));

	// 壁垒附魔（数据驱动定义于 data/extra-enchantry/enchantment/bulwark.json）
	public static final ResourceKey<Enchantment> BULWARK =
			ResourceKey.create(Registries.ENCHANTMENT, id("bulwark"));

	// 劫后余辉附魔（数据驱动定义于 data/extra-enchantry/enchantment/afterglow.json）
	public static final ResourceKey<Enchantment> AFTERGLOW =
			ResourceKey.create(Registries.ENCHANTMENT, id("afterglow"));

	// 誓约附魔（数据驱动定义于 data/extra-enchantry/enchantment/oathbound.json）
	public static final ResourceKey<Enchantment> OATHBOUND =
			ResourceKey.create(Registries.ENCHANTMENT, id("oathbound"));

	// 空跃附魔（数据驱动定义于 data/extra-enchantry/enchantment/skyward.json）
	public static final ResourceKey<Enchantment> SKYWARD =
			ResourceKey.create(Registries.ENCHANTMENT, id("skyward"));

	// 破阵附魔（数据驱动定义于 data/extra-enchantry/enchantment/cleave.json）
	public static final ResourceKey<Enchantment> CLEAVE =
			ResourceKey.create(Registries.ENCHANTMENT, id("cleave"));

	// 御风附魔（数据驱动定义于 data/extra-enchantry/enchantment/windrider.json）
	public static final ResourceKey<Enchantment> WINDRIDER =
			ResourceKey.create(Registries.ENCHANTMENT, id("windrider"));

	// 无踪附魔（数据驱动定义于 data/extra-enchantry/enchantment/unseen.json）
	public static final ResourceKey<Enchantment> UNSEEN =
			ResourceKey.create(Registries.ENCHANTMENT, id("unseen"));

	// 断罪附魔（数据驱动定义于 data/extra-enchantry/enchantment/judgement.json）
	public static final ResourceKey<Enchantment> JUDGEMENT =
			ResourceKey.create(Registries.ENCHANTMENT, id("judgement"));

	// 疾风附魔（数据驱动定义于 data/extra-enchantry/enchantment/gale.json）
	public static final ResourceKey<Enchantment> GALE =
			ResourceKey.create(Registries.ENCHANTMENT, id("gale"));

	// 余烬附魔（数据驱动定义于 data/extra-enchantry/enchantment/emberfall.json）
	public static final ResourceKey<Enchantment> EMBERFALL =
			ResourceKey.create(Registries.ENCHANTMENT, id("emberfall"));

	// 冲阵附魔（数据驱动定义于 data/extra-enchantry/enchantment/shield_charge.json）
	public static final ResourceKey<Enchantment> SHIELD_CHARGE =
			ResourceKey.create(Registries.ENCHANTMENT, id("shield_charge"));

	// 不屈附魔（数据驱动定义于 data/extra-enchantry/enchantment/defiance.json）
	public static final ResourceKey<Enchantment> DEFIANCE =
			ResourceKey.create(Registries.ENCHANTMENT, id("defiance"));

	// 庇护附魔（数据驱动定义于 data/extra-enchantry/enchantment/sanctuary.json）
	public static final ResourceKey<Enchantment> SANCTUARY =
			ResourceKey.create(Registries.ENCHANTMENT, id("sanctuary"));

	// 坚壁附魔（数据驱动定义于 data/extra-enchantry/enchantment/aegis.json）
	public static final ResourceKey<Enchantment> AEGIS =
			ResourceKey.create(Registries.ENCHANTMENT, id("aegis"));

	// 归羽附魔（数据驱动定义于 data/extra-enchantry/enchantment/homing_plume.json）
	public static final ResourceKey<Enchantment> HOMING_PLUME =
			ResourceKey.create(Registries.ENCHANTMENT, id("homing_plume"));

	// 坠星附魔（数据驱动定义于 data/extra-enchantry/enchantment/starfall.json）
	public static final ResourceKey<Enchantment> STARFALL =
			ResourceKey.create(Registries.ENCHANTMENT, id("starfall"));

	// 霆霓附魔（数据驱动定义于 data/extra-enchantry/enchantment/stormsurge.json）
	public static final ResourceKey<Enchantment> STORMSURGE =
			ResourceKey.create(Registries.ENCHANTMENT, id("stormsurge"));

	// 藏锋附魔（数据驱动定义于 data/extra-enchantry/enchantment/sheathed_edge.json）
	public static final ResourceKey<Enchantment> SHEATHED_EDGE =
			ResourceKey.create(Registries.ENCHANTMENT, id("sheathed_edge"));

	// 渊息附魔（数据驱动定义于 data/extra-enchantry/enchantment/tideheart.json）
	public static final ResourceKey<Enchantment> TIDEHEART =
			ResourceKey.create(Registries.ENCHANTMENT, id("tideheart"));

	// 丰壤附魔（数据驱动定义于 data/extra-enchantry/enchantment/loam.json）
	public static final ResourceKey<Enchantment> LOAM =
			ResourceKey.create(Registries.ENCHANTMENT, id("loam"));

	// ============ 1.4.0「环佩与獠牙」附魔 ============

	// 配饰附魔 ×8（数据驱动定义于 data/extra-enchantry/enchantment/*.json，计入八系共鸣）
	public static final ResourceKey<Enchantment> SOUL_CHIME =
			ResourceKey.create(Registries.ENCHANTMENT, id("soul_chime"));

	public static final ResourceKey<Enchantment> SHIELD_PENDANT =
			ResourceKey.create(Registries.ENCHANTMENT, id("shield_pendant"));

	public static final ResourceKey<Enchantment> THUNDER_CLASP =
			ResourceKey.create(Registries.ENCHANTMENT, id("thunder_clasp"));

	public static final ResourceKey<Enchantment> VERDANT_DROP =
			ResourceKey.create(Registries.ENCHANTMENT, id("verdant_drop"));

	public static final ResourceKey<Enchantment> BLADE_RING =
			ResourceKey.create(Registries.ENCHANTMENT, id("blade_ring"));

	public static final ResourceKey<Enchantment> PLUME_RING =
			ResourceKey.create(Registries.ENCHANTMENT, id("plume_ring"));

	public static final ResourceKey<Enchantment> EMBER_BRACELET =
			ResourceKey.create(Registries.ENCHANTMENT, id("ember_bracelet"));

	public static final ResourceKey<Enchantment> TIDE_BRACELET =
			ResourceKey.create(Registries.ENCHANTMENT, id("tide_bracelet"));

	// 狼铠附魔 ×3（铁砧上书，不计玩家共鸣）
	public static final ResourceKey<Enchantment> SHARP_FANG =
			ResourceKey.create(Registries.ENCHANTMENT, id("sharp_fang"));

	public static final ResourceKey<Enchantment> VIGIL =
			ResourceKey.create(Registries.ENCHANTMENT, id("vigil"));

	public static final ResourceKey<Enchantment> RENEWAL =
			ResourceKey.create(Registries.ENCHANTMENT, id("renewal"));

	// ============ 1.6.0「宣战与归一」器魂附魔（编号 41–46） ============

	// 明目（41，深暗境守望者）：黑暗时长按级 −35%（III 级免疫）——数据驱动 clearsight.json
	public static final ResourceKey<Enchantment> CLEARSIGHT =
			ResourceKey.create(Registries.ENCHANTMENT, id("clearsight"));

	// 枯刃（42，下界境烬骨王）：命中附带凋零 I（I 3 s / II 6 s）——数据驱动 witherblade.json
	public static final ResourceKey<Enchantment> WITHERBLADE =
			ResourceKey.create(Registries.ENCHANTMENT, id("witherblade"));

	// 潮涌（43，海洋境渊潮之主）：水中/雨中伤害 +8%/级 + 挖掘疲劳时长 −35%/级——tidesurge.json
	public static final ResourceKey<Enchantment> TIDESURGE =
			ResourceKey.create(Registries.ENCHANTMENT, id("tidesurge"));

	// 辟邪（44，沼泽境巫后）：有害效果时长 −20%/级（排除凋零/黑暗/疲劳三大专属反制位）——hexbreak.json
	public static final ResourceKey<Enchantment> HEXBREAK =
			ResourceKey.create(Registries.ENCHANTMENT, id("hexbreak"));

	// 虚闪（45，末地境末影领主）：被弹射物命中前 10%/级 概率侧移闪避——voidblink.json
	public static final ResourceKey<Enchantment> VOIDBLINK =
			ResourceKey.create(Registries.ENCHANTMENT, id("voidblink"));

	// 五境同辉（46，传说 T0）：八系共鸣判定阈值 −1（4/7 → 3/6），单件生效——realms_unity.json
	public static final ResourceKey<Enchantment> REALMS_UNITY =
			ResourceKey.create(Registries.ENCHANTMENT, id("realms_unity"));

	/**
	 * 破限可提升一级上限的附魔：所有在逻辑上可以增加一级的附魔（原版 + 本 mod）。
	 * 带破限的输入在铁砧融合时，这些附魔的等级上限从原版最大值提升 1
	 * （例：两个保护 IV → 保护 V）。见 AnvilMenuMixin#extraenchantry$levelUpCap。
	 */
	public static final Set<ResourceKey<Enchantment>> LEVEL_UP_ENCHANTMENTS = Set.of(
			vanillaEnchantment("unbreaking"), vanillaEnchantment("protection"),
			vanillaEnchantment("fire_protection"), vanillaEnchantment("blast_protection"),
			vanillaEnchantment("projectile_protection"), vanillaEnchantment("feather_falling"),
			vanillaEnchantment("thorns"), vanillaEnchantment("respiration"),
			vanillaEnchantment("depth_strider"), vanillaEnchantment("soul_speed"),
			vanillaEnchantment("sharpness"), vanillaEnchantment("smite"),
			vanillaEnchantment("bane_of_arthropods"), vanillaEnchantment("looting"),
			vanillaEnchantment("knockback"), vanillaEnchantment("sweeping_edge"),
			vanillaEnchantment("efficiency"), vanillaEnchantment("breach"),
			vanillaEnchantment("density"), vanillaEnchantment("wind_burst"),
			vanillaEnchantment("impaling"), vanillaEnchantment("lunge"),
			vanillaEnchantment("power"), vanillaEnchantment("punch"),
			vanillaEnchantment("quick_charge"), vanillaEnchantment("multishot"),
			vanillaEnchantment("loyalty"), vanillaEnchantment("riptide"),
			vanillaEnchantment("piercing"), vanillaEnchantment("fortune"),
			vanillaEnchantment("luck_of_the_sea"), vanillaEnchantment("lure"),
			WITHER_PROTECTION, REACH, SIPHON, LIFE_EROSION, VITALITY, BULWARK, SKYWARD
	);

	/** 原版附魔的 ResourceKey 快捷构造 */
	private static ResourceKey<Enchantment> vanillaEnchantment(String path) {
		return ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace(path));
	}

	/** 判断附魔是否支持破限提升一级上限 */
	public static boolean supportsLevelUp(Holder<Enchantment> holder) {
		return LEVEL_UP_ENCHANTMENTS.stream().anyMatch(holder::is);
	}

	/** 在物品的附魔里找指定附魔的 Holder（无则 Optional.empty） */
	public static java.util.Optional<Holder<Enchantment>> findHolder(ItemStack stack, ResourceKey<Enchantment> key) {
		for (Holder<Enchantment> holder : stack.getEnchantments().keySet()) {
			if (holder.is(key)) {
				return java.util.Optional.of(holder);
			}
		}
		return java.util.Optional.empty();
	}

	/** 家族共鸣小加成：物品上指定附魔的共鸣有效等级（玩家无共鸣时原样返回） */
	public static int effectiveLevel(LivingEntity entity, ItemStack stack, ResourceKey<Enchantment> key, int baseLevel) {
		if (entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			java.util.Optional<Holder<Enchantment>> holder = findHolder(stack, key);
			if (holder.isPresent()) {
				return FamilyResonanceManager.effectiveLevel(serverPlayer, holder.get(), baseLevel);
			}
		}
		return baseLevel;
	}

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * 破限附魔书来源标记组件（布尔）：闪电苦力怕代杀监守者掉落的破限书带此标记，
	 * 用于拾取时区分「雷霆之礼」与「极限之证」两个隐藏进度。普通来源不打标记。
	 */
	public static final DataComponentType<Boolean> LIMIT_BREAK_SOURCE =
			DataComponentType.<Boolean>builder().persistent(com.mojang.serialization.Codec.BOOL).build();

	// ============ 1.3.0「铭刻与试炼」 ============

	/** 共鸣秘典（八系共鸣状态入口：总览/家族详情/试炼三页，潜行右键切页）
	 *  26.2：Item 构造时即要求 Properties 已设置物品 ID（setId(ResourceKey)） */
	public static final Item RESONANCE_CODEX = new ResonanceCodexItem(
			new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, id("resonance_codex")))
					.stacksTo(1).rarity(Rarity.EPIC));

	// ============ 1.3.1「铭文纪元」 ============

	/** 来者手札：动态 lore 容器（页码按玩家 lore 触发进度开放） */
	public static final Item WELCOME_LETTER = new WelcomeLetterItem(
			new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, id("welcome_letter")))
					.stacksTo(1).rarity(Rarity.UNCOMMON));

	/** 家族铭文：一个物品类型 + family_id 组件表达八面（首 FULL 派发） */
	public static final Item FAMILY_INSCRIPTION = new FamilyInscriptionItem(
			new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, id("family_inscription")))
					.stacksTo(1).rarity(Rarity.RARE));

	/** 破限残页：一个物品类型 + shard_id 组件表达四幕（浩劫每波完成派发） */
	public static final Item LIMIT_BREAK_SHARD = new LimitBreakShardItem(
			new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, id("limit_break_shard")))
					.stacksTo(1).rarity(Rarity.RARE));

	/** 编年史卷轴：大共鸣者终局 lore（全文硬编码 lang） */
	public static final Item CHRONICLE_SCROLL = new ChronicleScrollItem(
			new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, id("chronicle_scroll")))
					.stacksTo(1).rarity(Rarity.EPIC));

	/** 八系家族附魔总标签（数据定义 tags/enchantment/families.json，引用八个 family_* 标签）：
	 *  onboarding「首次拾取本模附魔书」检测用 */
	public static final TagKey<Enchantment> ANY_FAMILY_TAG =
			TagKey.create(Registries.ENCHANTMENT, id("families"));

	/** 火焰家族附魔标签（family_fire）：防火判定依据（带任一火焰系附魔的物品免烧毁/免火损耐久） */
	public static final TagKey<Enchantment> FIRE_FAMILY_TAG =
			TagKey.create(Registries.ENCHANTMENT, id("family_fire"));

	/**
	 * 火焰家族物品判定（1.4.0 新增设定「烬火不侵」）：
	 * 1) 附魔命中 #extra-enchantry:family_fire（炽焰行者/余烬/烬镯/劫后余辉，含附魔书 STORED_ENCHANTMENTS）；
	 * 2) 配饰镶嵌烬心石（宝石即火焰家族凭证，未附魔也防火）。
	 * 动态判定即时生效，砂轮磨掉附魔即失效——用于 ItemEntity 烧毁免疫与穿戴耐久过滤。
	 */
	public static boolean isFireFamilyItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		for (Holder<Enchantment> enchantment : stack.getEnchantments().keySet()) {
			if (enchantment.is(FIRE_FAMILY_TAG)) {
				return true;
			}
		}
		var stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
		if (stored != null) {
			for (Holder<Enchantment> enchantment : stored.keySet()) {
				if (enchantment.is(FIRE_FAMILY_TAG)) {
					return true;
				}
			}
		}
		return "ember_heart".equals(Accessories.socketedGem(stack));
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// Attachment 持有类显式注册（静态块注册 attachment，必须早于任何玩家数据读取；
		// 曾因方法引用不触发 <clinit> 导致 lore_triggers 被当未知类型丢弃 → 一次性引导重复触发）
		LoreTriggerManager.register();
		AttunementManager.register();
		AccessoryAttachments.register();

		ExtraEnchantryCreativeTab.register();
		ExtraEnchantryEffects.register();

		// 破限附魔书来源标记组件（区分隐藏进度来源）
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("limit_break_source"), LIMIT_BREAK_SOURCE);

		// 共鸣秘典物品（1.3.0）：八系共鸣状态入口
		Registry.register(BuiltInRegistries.ITEM, id("resonance_codex"), RESONANCE_CODEX);

		// 家族铭印物品 + family_id 数据组件（1.3.0：一个物品 + 组件表达八枚铭印）
		FamilySigils.register();

		// 共鸣规则数据化（1.3.0）：SERVER_DATA 阶段读取 resonance/families/*.json，
		// /reload 与服务器启动都会执行；失败回退内置默认
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
				id("resonance_rules"), provider -> new ResonanceConfig.RulesLoader());

		// ============ 1.3.1「铭文纪元」注册 ============

		// lore 物品（不进创造栏——仅通过进度获得）+ shard_id 组件
		Registry.register(BuiltInRegistries.ITEM, id("welcome_letter"), WELCOME_LETTER);
		Registry.register(BuiltInRegistries.ITEM, id("family_inscription"), FAMILY_INSCRIPTION);
		Registry.register(BuiltInRegistries.ITEM, id("limit_break_shard"), LIMIT_BREAK_SHARD);
		Registry.register(BuiltInRegistries.ITEM, id("chronicle_scroll"), CHRONICLE_SCROLL);
		LimitBreakShardItem.register();

		// ============ 1.4.0「环佩与獠牙」注册 ============

		// 环佩物品与组件（16 配饰 + 8 宝石 + socketed_gem 组件）
		Accessories.register();


		// 配饰结算：属性类 + 自然恢复 tick（每玩家）
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				AccessoryManager.tick(player);
			}
		});

		// 配饰击杀类被动（魂铃 / 魂珀）
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer killer) {
				AccessoryManager.onKillMob(killer, entity);
			}
		});

		// lore 文本数据化（1.3.1）：SERVER_DATA 阶段读取 lore/ 目录（五类 schema，逐条容错）
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
				id("lore"), provider -> new LoreLoader.Loader());

		// 被动引导（1.3.1）：登录追发 + 秒级首次拾书检测
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				OnboardingManager.onJoin(handler.player));
		ServerTickEvents.END_SERVER_TICK.register(OnboardingManager::tick);

		// 主调铭刻/查询/诊断命令组（1.3.0）
		ExtraEnchantryCommands.register();

		// 破限附魔书的掉落（监守者 0.05% / 闪电苦力怕代杀 0.5%）
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) ->
				LimitBreakManager.onWardenDeath(entity, source));

		// 陷阱混编队成员死亡判定：击败计数凑满整队 → 「全歼」进度
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) ->
				CavalryManager.onTrapRiderDeath(entity, source));

		// 家族共鸣（1.2.0）：秒级扫描档位 + FULL 属性/被动 + 死亡结算（猎魂/深渊回响）
		ServerTickEvents.END_SERVER_TICK.register(FamilyResonanceManager::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) ->
				FamilyResonanceManager.onLivingDeath(entity, source));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) ->
				ShieldChargeManager.onLivingDeath(entity, source));

		// 八系共鸣试炼（1.3.0）：窗口型计数（秒级/逐 tick）+ 击杀类判定
		ServerTickEvents.END_SERVER_TICK.register(FamilyTrialsManager::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) ->
				FamilyTrialsManager.onLivingDeath(entity, source));

		// 1.7.3 谱系器魂检测：已并入 FamilyResonanceManager 秒级扫描（事件驱动 + 签名缓存）

		// 死而不僵（隐秘挑战）：劫后余辉锁血期间击杀攻击者
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer killer
					&& AfterglowManager.isLocked(killer)) {
				FamilyResonanceManager.onAfterglowKill(killer);
			}
		});

		// 诸界浩劫挑战节拍：超时/死亡判定、苦力怕生成、清波检测、下一波启动
		ServerTickEvents.END_SERVER_TICK.register(CavalryManager::tickCataclysms);

		// ============ 1.5.0「五境领主」注册 ============

		// 五境材料与宝匣（5 材料 + 5 宝匣）
		RealmTreasures.register();

		// 五境领主刷怪蛋（1.7.3）：管理/测试便捷入口，生成领主子类实例
		LordSpawnEggs.register();

		// ============ 1.6.0「宣战与归一」注册 ============

		// 宣战图腾 / 觉醒徽记 / 归一印记 / 归一心核 / 归一宝匣 + realm_id 组件
		WarArtifacts.register();

		// 归一之战链式调度：回合推进 / 超时 / 单血条五色切换
		ServerTickEvents.END_SERVER_TICK.register(ConvergenceManager::tick);

		// ============ 1.7.0「谱系与传承」注册 ============

		// 谱系回响匣（铭印补齐渠道）+ 遗辉纹饰模板（armor trim）
		LineageEchoItem.register();
		AfterglowTrim.register();

		// 归一之战重启打断提示（上线检查）
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ConvergenceManager.onJoin(handler.player);
			// 修复 #13：重登是新实体（瞬态修改器已丢），清缓存强制下一 tick 全量重写属性
			AccessoryManager.onPlayerRestore(handler.player);
		});

		// 精英遭遇规则数据化：elite_encounter/<境>.json + elite_encounter_global/global.json
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
				id("elite_encounter_rules"), provider -> new EliteEncounterConfig.RealmLoader());
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
				id("elite_encounter_global"), provider -> new EliteEncounterConfig.GlobalLoader());

		// 遭遇节拍：触发计数（浸泡 / 愤怒采样）+ 阶段机推进 + 取消判定
		ServerTickEvents.END_SERVER_TICK.register(EliteEncounterManager::tick);

		// 领主死亡 → 击杀 / 隐藏 / 五境巡礼进度；女巫死亡 → 沼泽境路径 B 计数
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			EliteEncounterManager.onLordDeath(entity, source);
			if (entity instanceof net.minecraft.world.entity.monster.Witch
					&& source.getEntity() instanceof ServerPlayer killer) {
				EliteEncounterManager.noteWitchKill(killer);
			}
		});

		// 玩家离线清理（避免计数与维度锁残留）
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
				(handler, server) -> {
					var player = handler.player;
					java.util.UUID pid = player.getUUID();
					EliteEncounterManager.onPlayerLeave(player);
					CavalryManager.onDisconnect(pid);
					ConvergenceManager.onDisconnect(pid);
					FamilyResonanceManager.onDisconnect(pid);
					FamilyTrialsManager.onDisconnect(pid);
					AccessoryManager.onDisconnect(player);
					DecoyManager.onDisconnect(pid);
					VoidblinkManager.onDisconnect(pid);
					SanctuaryManager.onDisconnect(pid);
					LoamManager.onDisconnect(pid);
					OnboardingManager.onDisconnect(pid);
					LineageManager.onDisconnect(pid);
					ResonanceCodexItem.onDisconnect(pid);
					JudgementManager.onDisconnect(pid);
					SheathedEdgeManager.onDisconnect(pid);
					ShieldChargeManager.onDisconnect(pid);
				});

		// 服务器停止全清（修复：单 JVM 内跨存档残留全部静态运行时状态）
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(
				server -> {
					EliteEncounterManager.onServerStopped();
					CavalryManager.onServerStopped();
					ConvergenceManager.onServerStopped();
					FamilyResonanceManager.onServerStopped();
					FamilyTrialsManager.onServerStopped();
					DecoyManager.onServerStopped();
					FxHelper.clearThrottle();
				});

		// 破限腿甲跨部位解锁：可附魔原版摔落保护（经 fabric-item-api 的官方事件，
		// 避免 Mixin Redirect 与其 AnvilMenuMixin 冲突）
		EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, stack, context) -> {
			if (enchantment.is(FEATHER_FALLING)
					&& hasLimitBreak(stack)
					&& stack.is(itemHolder -> itemHolder.is(LEG_ARMOR_TAG))) {
				return TriState.TRUE;
			}
			return TriState.DEFAULT;
		});

		// 庇护（Sanctuary）：追加进试炼密室基础/稀有奖励箱
		// （26.2 盾牌机制为 BlocksAttacks 数据组件，庇护走 fabric-loot-api-v3 的
		// MODIFY 事件追加战利品池，避免整表覆盖丢失原版内容）
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (!source.isBuiltin()
					|| (!key.equals(BuiltInLootTables.TRIAL_CHAMBERS_REWARD)
							&& !key.equals(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE))) {
				return;
			}
			Holder<Enchantment> sanctuary =
					registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(SANCTUARY);
			tableBuilder.withPool(LootPool.lootPool()
					.setRolls(ConstantValue.exactly(1.0F))
					.add(EmptyLootItem.emptyItem().setWeight(90))
					.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(7)
							.apply(new SetEnchantmentsFunction.Builder()
									.withEnchantment(sanctuary, ConstantValue.exactly(1.0F))))
					.add(LootItem.lootTableItem(Items.SHIELD).setWeight(3)
							.apply(new SetEnchantmentsFunction.Builder()
									.withEnchantment(sanctuary, UniformGenerator.between(1.0F, 3.0F)))));
		});

		// 渊息（Tideheart）：追加进海洋系宝箱（沉船三类/埋藏的宝藏/海底废墟大小）
		// 与钓鱼宝藏池——主题绑定海洋，不进 on_random_loot 通用随机池
		// （treasure 标签已将其挡在附魔台之外，同庇护的事件追加法）
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (!source.isBuiltin()
					|| (!key.equals(BuiltInLootTables.SHIPWRECK_SUPPLY)
							&& !key.equals(BuiltInLootTables.SHIPWRECK_MAP)
							&& !key.equals(BuiltInLootTables.SHIPWRECK_TREASURE)
							&& !key.equals(BuiltInLootTables.BURIED_TREASURE)
							&& !key.equals(BuiltInLootTables.UNDERWATER_RUIN_BIG)
							&& !key.equals(BuiltInLootTables.UNDERWATER_RUIN_SMALL)
							&& !key.equals(BuiltInLootTables.FISHING_TREASURE))) {
				return;
			}
			Holder<Enchantment> tideheart =
					registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(TIDEHEART);
			tableBuilder.withPool(LootPool.lootPool()
					.setRolls(ConstantValue.exactly(1.0F))
					.add(EmptyLootItem.emptyItem().setWeight(85))
					.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(15)
							.apply(new SetEnchantmentsFunction.Builder()
									.withEnchantment(tideheart, UniformGenerator.between(1.0F, 3.0F)))));
		});

		// 归羽（Homing Plume）：落空箭矢的延迟返还节拍（1 秒飞回动画窗口）
		ServerTickEvents.END_SERVER_TICK.register(HomingPlumeManager::tick);

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** 壁垒各等级的单次伤害上限（HP，下标 = 等级 - 1）：5.5/5/4.5/4/3.5/3/2.5/2/1.5/1 颗心（每级递减 0.5 心） */
	private static final float[] BULWARK_CAP =
			{11.0F, 10.0F, 9.0F, 8.0F, 7.0F, 6.0F, 5.0F, 4.0F, 3.0F, 2.0F, 1.5F};

	/**
	 * 壁垒（Bulwark）：把**防御减免之后**的实际承伤钳制到胸甲壁垒等级对应的上限。
	 * 钳制发生在护甲/魔抗减免之后、吸收盾结算之前——约束的是实际承伤（吸收+掉血合计），
	 * 不会被护甲二次削减（若钳在 hurtServer 入口，30 伤害先钳 2 再被护甲减 80% 只剩 0.4，出现过强 bug）。
	 *
	 * 修复 #23：排除即死 / 系统性伤害源——旧实现可挡下 /kill、坠落虚空、饥饿等，
	 * 与汲取组合形成近战永动机的"绝对免死"。
	 *
	 * 两个调用点共用本方法：`LivingEntity#actuallyHurt`（非玩家生物）与 `Player#actuallyHurt`
	 * （玩家专用，Player 重写了 actuallyHurt 且不调 super，必须单独拦截）。
	 */
	public static float applyBulwarkCap(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source,
			float reduced) {
		if (reduced <= 0.0F) {
			return reduced;
		}
		// 修复 #23：即死与系统性伤害不受壁垒保护
		if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
				|| source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)
				|| source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
				|| source.is(net.minecraft.world.damagesource.DamageTypes.STARVE)) {
			return reduced;
		}
		// 玩家看胸甲（BODY 槽恒空）；马匹看 BODY 槽的马铠（CHEST 槽恒空），二者取其一
		int level = Math.max(
				getBulwarkLevel(entity.getItemBySlot(EquipmentSlot.CHEST)),
				getBulwarkLevel(entity.getItemBySlot(EquipmentSlot.BODY)));
		if (level <= 0 || level > BULWARK_CAP.length) {
			return reduced;
		}
		float capped = Math.min(reduced, BULWARK_CAP[level - 1]);
		// 壁垒格挡确认（DESIGN_aesthetics P0）：实际钳制 ≥4 点或减免 ≥20% 时给反馈——
		// 修复 #30：旧阈值 4 点让 VII+ 级（上限 ≤6 HP）几乎永不触发反馈，设计目标落空
		float blocked = reduced - capped;
		if (blocked >= 4.0F || blocked >= reduced * 0.2F) {
			if (blocked >= 4.0F && entity instanceof net.minecraft.server.level.ServerPlayer player
					&& entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
				FxHelper.burst(serverLevel, entity, net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
						6, 0.3D);
				FxHelper.play(serverLevel, entity, net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, 1.0F, 0.6F);
				player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
						"message.extra-enchantry.bulwark_blocked",
						String.format(java.util.Locale.ROOT, "%.1f", blocked)).withStyle(
						net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
				// 实战成就「铜墙铁壁」：壁垒挡下 ≥4 点伤害
				Advancements.award(player, Advancements.BULWARK_SAVE);
			}
		}
		return capped;
	}

	/** 获取物品上的劫后余辉附魔等级（无则返回 0，stack 可为 null） */
	public static int getAfterglowLevel(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return 0;
		}
		ItemEnchantments enchantments = stack.getEnchantments();
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(AFTERGLOW)) {
				return enchantments.getLevel(enchantment);
			}
		}
		return 0;
	}

	/** 获取物品上的誓约附魔等级（无则返回 0，stack 可为 null） */
	public static int getOathboundLevel(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return 0;
		}
		ItemEnchantments enchantments = stack.getEnchantments();
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(OATHBOUND)) {
				return enchantments.getLevel(enchantment);
			}
		}
		return 0;
	}

	/** 获取物品上的空跃附魔等级（无则返回 0，stack 可为 null） */
	public static int getSkywardLevel(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return 0;
		}
		ItemEnchantments enchantments = stack.getEnchantments();
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(SKYWARD)) {
				return enchantments.getLevel(enchantment);
			}
		}
		return 0;
	}

	/** 获取物品上的破阵附魔等级（无则返回 0，stack 可为 null） */
	public static int getCleaveLevel(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return 0;
		}
		ItemEnchantments enchantments = stack.getEnchantments();
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(CLEAVE)) {
				return enchantments.getLevel(enchantment);
			}
		}
		return 0;
	}

	// ============ 破限（Limit Break）辅助判定 ============

	/** 判断物品是否带有破限附魔 */
	public static boolean hasLimitBreak(ItemStack stack) {
		ItemEnchantments enchantments = stack.getEnchantments();
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(LIMIT_BREAK)) {
				return true;
			}
		}
		return false;
	}

	/** 判断实体的任意护甲槽位是否带有破限附魔 */
	public static boolean hasLimitBreakOnArmor(LivingEntity entity) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.isArmor() && hasLimitBreak(entity.getItemBySlot(slot))) {
				return true;
			}
		}
		return false;
	}

	/** 获取物品上的拓阶附魔等级（无则返回 0） */
	public static int getTierBreakLevel(ItemStack stack) {
		ItemEnchantments enchantments = stack.getEnchantments();
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(TIER_BREAK)) {
				return enchantments.getLevel(enchantment);
			}
		}
		return 0;
	}

	/** 获取物品上的壁垒附魔等级（无则返回 0，stack 可为 null） */
	public static int getBulwarkLevel(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return 0;
		}
		ItemEnchantments enchantments = stack.getEnchantments();
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(BULWARK)) {
				return enchantments.getLevel(enchantment);
			}
		}
		return 0;
	}

	/** 获取物品上的活力附魔等级（无则返回 0，stack 可为 null） */
	public static int getVitalityLevel(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return 0;
		}
		ItemEnchantments enchantments = stack.getEnchantments();
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(VITALITY)) {
				return enchantments.getLevel(enchantment);
			}
		}
		return 0;
	}

	/**
	 * 活力：护甲总附魔等级 × 4 点生命；多件叠加，上限 +50。
	 * 任意护甲带破限时上限失效（破限可生效）。
	 * 客户端 HUD 同样调用此方法计算显示值。
	 */
	public static int getVitalityBonus(LivingEntity entity) {
		// 性能（1.7.3）：本方法在 LivingEntity#tick HEAD 对所有生物每 tick 调用；
		// 无附魔时四槽 stack.getEnchantments() 是空组件解引用，仍 worth 短路——
		// 用"任一护甲槽为空即跳过该槽"已有的 isEmpty 检查 + 首槽快速失败即可，
		// 这里补一个廉价的"全槽为空"快速路径（绝大多数生物命中）。
		if (entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
				&& entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
				&& entity.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
				&& entity.getItemBySlot(EquipmentSlot.FEET).isEmpty()
				// 修复：BODY 槽（马铠/狼铠）也要查——isArmor() 覆盖 BODY，
				// 旧快速路径让马/狼直接返回 0，马铠与狼铠活力完全失效
				&& entity.getItemBySlot(EquipmentSlot.BODY).isEmpty()) {
			return 0;
		}
		int totalLevels = 0;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.isArmor()) {
				ItemStack stack = entity.getItemBySlot(slot);
				int level = getVitalityLevel(stack);
				// 家族共鸣小加成：自然族 ≥PARTIAL 时活力按 +1 级结算（仅服务端有共鸣快照；
				// 客户端 HUD 的一致性由 HudMixin 的 syncedVitalityBonus 反推保证——修复 #30）
				if (level > 0 && entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
					level = effectiveLevel(serverPlayer, stack, VITALITY, level);
				}
				totalLevels += level;
			}
		}
		if (totalLevels <= 0) {
			return 0;
		}
		int bonus = totalLevels * 4;
		if (!hasLimitBreakOnArmor(entity)) {
			bonus = Math.min(bonus, 50);
		}
		return bonus;
	}

	/** 获取物品上的蚀命附魔等级（无则返回 0，stack 可为 null） */
	public static int getLifeErosionLevel(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return 0;
		}
		ItemEnchantments enchantments = stack.getEnchantments();
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(LIFE_EROSION)) {
				return enchantments.getLevel(enchantment);
			}
		}
		return 0;
	}

	/** 获取物品上的汲取附魔等级（无则返回 0，stack 可为 null） */
	public static int getSiphonLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, SIPHON);
	}

	/** 通用：读取物品上指定附魔的等级（无则返回 0，stack 可为 null）——供 Manager 层复用的公开入口 */
	public static int getEnchantmentLevelPublic(ItemStack stack, ResourceKey<Enchantment> key) {
		return getEnchantmentLevel(stack, key);
	}

	private static int getEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> key) {
		if (stack == null || stack.isEmpty()) {
			return 0;
		}
		ItemEnchantments enchantments = stack.getEnchantments();
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(key)) {
				return enchantments.getLevel(enchantment);
			}
		}
		return 0;
	}

	// ============ 御风 / 无踪 / 断罪 / 疾风 / 余烬 ============

	/** 御风各等级的滑翔耐久消耗跳过概率（下标 = 等级 - 1）：I/II 级 -50%，III 级再强化 50% → -75% */
	private static final float[] WINDRIDER_DURABILITY_SKIP = {0.5F, 0.5F, 0.75F};

	/** 御风各等级的烟花推进增量倍率：I 级原版，II 级 +50%，III 级再强化 50% → +75% */
	private static final double[] WINDRIDER_BOOST_FACTOR = {1.0D, 1.5D, 1.75D};

	/** 疾风每级提供的移动速度加成（5%/级） */
	private static final double GALE_SPEED_PER_LEVEL = 0.05D;

	/** 获取物品上的御风附魔等级（无则返回 0） */
	public static int getWindriderLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, WINDRIDER);
	}

	/** 获取物品上的无踪附魔等级（无则返回 0） */
	public static int getUnseenLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, UNSEEN);
	}

	/** 获取物品上的断罪附魔等级（无则返回 0） */
	public static int getJudgementLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, JUDGEMENT);
	}

	/** 获取物品上的疾风附魔等级（无则返回 0） */
	public static int getGaleLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, GALE);
	}

	/** 获取物品上的余烬附魔等级（无则返回 0） */
	public static int getEmberfallLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, EMBERFALL);
	}

	/** 获取盾牌上的冲阵附魔等级（无则返回 0） */
	public static int getShieldChargeLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, SHIELD_CHARGE);
	}

	/** 获取盾牌上的不屈附魔等级（无则返回 0） */
	public static int getDefianceLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, DEFIANCE);
	}

	/** 获取盾牌上的庇护附魔等级（无则返回 0） */
	public static int getSanctuaryLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, SANCTUARY);
	}

	/** 获取盾牌上的坚壁附魔等级（无则返回 0） */
	public static int getAegisLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, AEGIS);
	}

	/** 获取弓/弩上的归羽附魔等级（无则返回 0） */
	public static int getHomingPlumeLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, HOMING_PLUME);
	}

	/** 获取弩上的坠星附魔等级（无则返回 0） */
	public static int getStarfallLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, STARFALL);
	}

	/** 获取三叉戟上的霆霓附魔等级（无则返回 0） */
	public static int getStormsurgeLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, STORMSURGE);
	}

	/** 获取近战武器上的藏锋附魔等级（无则返回 0） */
	public static int getSheathedEdgeLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, SHEATHED_EDGE);
	}

	/** 获取头盔上的渊息附魔等级（无则返回 0） */
	public static int getTideheartLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, TIDEHEART);
	}

	/** 获取锄头上的丰壤附魔等级（无则返回 0） */
	public static int getLoamLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, LOAM);
	}

	/** 获取武器上的触及附魔等级（无则返回 0） */
	public static int getReachLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, REACH);
	}

	/** 获取靴子上的炽焰行者附魔等级（无则返回 0） */
	public static int getBlazingWalkerLevel(ItemStack stack) {
		return getEnchantmentLevel(stack, BLAZING_WALKER);
	}

	/** 靴子上的无踪等级（Entity/LivingEntity 注入点快速判定用） */
	public static int getUnseenLevelOnFeet(LivingEntity entity) {
		return getUnseenLevel(entity.getItemBySlot(EquipmentSlot.FEET));
	}

	/** 御风：本次滑翔耐久消耗是否被跳过（概率 = 50%/50%/75%） */
	public static boolean shouldSkipGlideDurability(ItemStack elytra) {
		int level = getWindriderLevel(elytra);
		if (level <= 0 || level > WINDRIDER_DURABILITY_SKIP.length) {
			return false;
		}
		return Math.random() < WINDRIDER_DURABILITY_SKIP[level - 1];
	}

	/** 御风：烟花推进增量倍率（1.0 / 1.5 / 1.6，无附魔返回 1.0） */
	public static double getWindriderBoostFactor(LivingEntity glider) {
		int level = getWindriderLevel(glider.getItemBySlot(EquipmentSlot.CHEST));
		if (level <= 0 || level > WINDRIDER_BOOST_FACTOR.length) {
			return 1.0D;
		}
		return WINDRIDER_BOOST_FACTOR[level - 1];
	}

	/**
	 * 疾风：护腿提供的移动速度加成（+5%/级），潜行时为 0（保留潜行的战术价值）。
	 * 疾风 III 级只能由两个 II 级在带破限的铁砧上融合得到（见 AnvilMenuMixin）。
	 */
	public static double getGaleSpeedBonus(LivingEntity entity) {
		if (entity.isCrouching()) {
			return 0.0D;
		}
		return getGaleLevel(entity.getItemBySlot(EquipmentSlot.LEGS)) * GALE_SPEED_PER_LEVEL;
	}

	/**
	 * 断罪的 boss 豁免判定：26.2 无统一的 isBoss()/boss 标签，
	 * 故按带 boss 血条的三个原版 boss 显式判定（末影龙/凋灵/监守者）。
	 */
	public static boolean isBossLike(LivingEntity entity) {
		return entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
				|| entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss
				|| entity instanceof net.minecraft.world.entity.monster.warden.Warden
				// 1.5.0「五境领主」：领主级进入 boss 判定（断罪不斩杀改 ×2、曳钩不可拉拽）
				|| entity instanceof EliteLord;
	}
}
