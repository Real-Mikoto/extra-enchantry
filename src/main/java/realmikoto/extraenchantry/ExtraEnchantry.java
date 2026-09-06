package realmikoto.extraenchantry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.util.TriState;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.world.item.enchantment.ItemEnchantments;

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

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ExtraEnchantryCreativeTab.register();
		ExtraEnchantryEffects.register();

		// 破限附魔书来源标记组件（区分隐藏进度来源）
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("limit_break_source"), LIMIT_BREAK_SOURCE);

		// 破限附魔书的掉落（监守者 0.05% / 闪电苦力怕代杀 0.5%）
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) ->
				LimitBreakManager.onWardenDeath(entity, source));

		// 陷阱混编队成员死亡判定：击败计数凑满整队 → 「全歼」进度
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) ->
				CavalryManager.onTrapRiderDeath(entity, source));

		// 诸界浩劫挑战节拍：超时/死亡判定、苦力怕生成、清波检测、下一波启动
		ServerTickEvents.END_SERVER_TICK.register(CavalryManager::tickCataclysms);

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

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** 壁垒各等级的单次伤害上限（HP，下标 = 等级 - 1）：5.5/5/4.5/4/3.5/3/2.5/2/1.5/1 颗心（每级递减 0.5 心） */
	private static final float[] BULWARK_CAP =
			{11.0F, 10.0F, 9.0F, 8.0F, 7.0F, 6.0F, 5.0F, 4.0F, 3.0F, 2.0F};

	/**
	 * 壁垒（Bulwark）：把**防御减免之后**的实际承伤钳制到胸甲壁垒等级对应的上限。
	 * 钳制发生在护甲/魔抗减免之后、吸收盾结算之前——约束的是实际承伤（吸收+掉血合计），
	 * 不会被护甲二次削减（若钳在 hurtServer 入口，30 伤害先钳 2 再被护甲减 80% 只剩 0.4，出现过强 bug）。
	 *
	 * 两个调用点共用本方法：`LivingEntity#actuallyHurt`（非玩家生物）与 `Player#actuallyHurt`
	 * （玩家专用，Player 重写了 actuallyHurt 且不调 super，必须单独拦截）。
	 */
	public static float applyBulwarkCap(LivingEntity entity, float reduced) {
		if (reduced <= 0.0F) {
			return reduced;
		}
		// 玩家看胸甲（BODY 槽恒空）；马匹看 BODY 槽的马铠（CHEST 槽恒空），二者取其一
		int level = Math.max(
				getBulwarkLevel(entity.getItemBySlot(EquipmentSlot.CHEST)),
				getBulwarkLevel(entity.getItemBySlot(EquipmentSlot.BODY)));
		if (level <= 0 || level > BULWARK_CAP.length) {
			return reduced;
		}
		return Math.min(reduced, BULWARK_CAP[level - 1]);
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
		int totalLevels = 0;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.isArmor()) {
				totalLevels += getVitalityLevel(entity.getItemBySlot(slot));
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

	/** 通用：读取物品上指定附魔的等级（无则返回 0，stack 可为 null） */
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

	/** 御风：烟花推进增量倍率（1.0 / 1.5 / 1.75，无附魔返回 1.0） */
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
				|| entity instanceof net.minecraft.world.entity.monster.warden.Warden;
	}
}
