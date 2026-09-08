package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import realmikoto.extraenchantry.entity.EmberboneKingEntity;
import realmikoto.extraenchantry.entity.EnderLordEntity;
import realmikoto.extraenchantry.entity.HagSovereignEntity;
import realmikoto.extraenchantry.entity.OverwardenEntity;
import realmikoto.extraenchantry.entity.TidalSovereignEntity;

import java.util.List;
import java.util.Map;

/**
 * 单条精英遭遇定义（1.5.0「五境领主」§0.2.1 / §7.2）。
 *
 * 一条定义 = 一个境：维度 + 群系过滤 + 触发机制 + 领主工厂 + 掉落 + 三条进度。
 * 阶段时长与数值不写死在本类，运行时统一从 {@link EliteEncounterConfig} 读取
 * （数据包可覆盖，见配置类注释）。
 *
 * 五境触发机制互不相同（设计 §9.2 差异化验收第一条），由 {@link Trigger} 区分：
 * 愤怒持续 / 状态累积 / 浸泡或激光 / 夜晚双路径 / 物品使用计数。
 */
public record EncounterDef(
		String id,
		String nameKey,
		BossEvent.BossBarColor barColor,
		Trigger trigger,
		BiomeFilter filter,
		LordFactory factory,
		Item material,
		int materialCount,
		Item casket,
		ResourceKey<net.minecraft.advancements.Advancement> triggerAdv,
		ResourceKey<net.minecraft.advancements.Advancement> killAdv,
		ResourceKey<net.minecraft.advancements.Advancement> hiddenAdv,
		List<LordDrop> bookDrops) {

	/** 领主掉落条目：附魔 + 固定等级 + 概率（100% 之外的补充掉落） */
	public record LordDrop(ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantment,
			int level, float chance) {
	}

	/** 触发机制（五境互不相同，且均 ≠ 雷雨 / 闪电 / 靠近） */
	public enum Trigger {
		/** 深暗境：32 格内普通监守者被激怒至敌对并持续 N 秒 */
		WARDEN_ANGER,
		/** 下界境：滑动窗口内累计承受凋零 N 秒 */
		WITHER_ACCUMULATION,
		/** 海洋境：浸泡/游泳累计 N 秒，或被守卫者激光命中 N 次 */
		SOAK_OR_LASER,
		/** 沼泽境：夜晚限定，被女巫药水命中 N 次 或 击杀女巫 N 只 */
		NIGHT_WITCH,
		/** 末地境：滑动窗口内使用末影珍珠 N 次 */
		PEARL_USES
	}

	/** 群系/维度过滤（26.2 无 deep_dark / swamp 群系标签，故按 ID + 维度判定） */
	public enum BiomeFilter {
		DEEP_DARK, NETHER_ANY, OCEAN_NON_COLD, SWAMP, END_OUTER
	}

	/** 领主工厂：复用原版实体类型构造自定义子类（DecoyEntity 模式，零实体注册） */
	@FunctionalInterface
	public interface LordFactory {
		Mob create(ServerLevel level);
	}

	// ============ 注册表 ============

	private static final Map<String, EncounterDef> DEFS = Map.of(
			"overwarden", new EncounterDef("overwarden", "entity.extra-enchantry.overwarden",
					BossEvent.BossBarColor.PURPLE, Trigger.WARDEN_ANGER, BiomeFilter.DEEP_DARK,
					OverwardenEntity::new, RealmTreasures.SCULK_CORE, 1, RealmTreasures.SCULK_CASKET,
					adv("overwarden_omen"), adv("overwarden_slain"), adv("overwarden_hidden"),
					List.of(new LordDrop(ExtraEnchantry.LIFE_EROSION, 3, 0.35F),
							new LordDrop(ExtraEnchantry.UNSEEN, 2, 0.25F))),

			"emberbone", new EncounterDef("emberbone", "entity.extra-enchantry.emberbone_king",
					BossEvent.BossBarColor.RED, Trigger.WITHER_ACCUMULATION, BiomeFilter.NETHER_ANY,
					EmberboneKingEntity::new, RealmTreasures.EMBER_CORE, 1, RealmTreasures.EMBER_CASKET,
					adv("emberbone_omen"), adv("emberbone_slain"), adv("emberbone_hidden"),
					List.of(new LordDrop(ExtraEnchantry.BLAZING_WALKER, 2, 0.30F),
							new LordDrop(ExtraEnchantry.TIER_BREAK, 1, 0.15F))),

			"tidal", new EncounterDef("tidal", "entity.extra-enchantry.tidal_sovereign",
					BossEvent.BossBarColor.BLUE, Trigger.SOAK_OR_LASER, BiomeFilter.OCEAN_NON_COLD,
					TidalSovereignEntity::new, RealmTreasures.TIDAL_TEAR, 2, RealmTreasures.TIDAL_CASKET,
					adv("tidal_omen"), adv("tidal_slain"), adv("tidal_hidden"),
					List.of(new LordDrop(ExtraEnchantry.SKYWARD, 1, 0.25F))),

			"hag", new EncounterDef("hag", "entity.extra-enchantry.hag_sovereign",
					BossEvent.BossBarColor.GREEN, Trigger.NIGHT_WITCH, BiomeFilter.SWAMP,
					HagSovereignEntity::new, RealmTreasures.POTION_ESSENCE, 2, RealmTreasures.POTION_CASKET,
					adv("hag_omen"), adv("hag_slain"), adv("hag_hidden"),
					List.of(new LordDrop(ExtraEnchantry.JUDGEMENT, 1, 0.20F),
							new LordDrop(ExtraEnchantry.DECOY, 2, 0.10F))),

			"ender", new EncounterDef("ender", "entity.extra-enchantry.ender_lord",
					BossEvent.BossBarColor.PURPLE, Trigger.PEARL_USES, BiomeFilter.END_OUTER,
					EnderLordEntity::new, RealmTreasures.VOID_SHARD, 2, RealmTreasures.VOID_CASKET,
					adv("ender_omen"), adv("ender_slain"), adv("ender_hidden"),
					List.of(new LordDrop(ExtraEnchantry.WINDRIDER, 1, 0.25F),
							new LordDrop(ExtraEnchantry.OATHBOUND, 1, 0.15F))));

	/** 全部境定义（顺序 = 难度梯度：沼泽 → 下界 → 海洋 → 深暗 → 末地） */
	public static final List<EncounterDef> ALL =
			List.of(DEFS.get("hag"), DEFS.get("emberbone"), DEFS.get("tidal"),
					DEFS.get("overwarden"), DEFS.get("ender"));

	public static EncounterDef byId(String id) {
		return DEFS.get(id);
	}

	private static ResourceKey<net.minecraft.advancements.Advancement> adv(String path) {
		return ResourceKey.create(Registries.ADVANCEMENT, ExtraEnchantry.id("lords/" + path));
	}

	// ============ 位置与模式判定 ============

	/**
	 * 位置是否符合本境（维度 + 群系）。
	 * 26.2 无 {@code is_deep_dark} / {@code is_swamp} 群系标签（仅有 is_ocean / is_deep_ocean /
	 * is_nether / is_end），故深暗与沼泽按群系 ID 判定、外岛按距原点距离判定。
	 */
	public boolean matches(ServerLevel level, BlockPos pos) {
		var biome = level.getBiome(pos);
		return switch (filter) {
			case DEEP_DARK -> level.dimension() == Level.OVERWORLD
					&& biomeId(biome).equals("minecraft:deep_dark");
			case NETHER_ANY -> level.dimension() == Level.NETHER;
			case OCEAN_NON_COLD -> level.dimension() == Level.OVERWORLD
					&& (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN))
					&& !isColdOcean(biomeId(biome));
			case SWAMP -> level.dimension() == Level.OVERWORLD
					&& (biomeId(biome).equals("minecraft:swamp")
					|| biomeId(biome).equals("minecraft:mangrove_swamp"));
			case END_OUTER -> level.dimension() == Level.END
					&& Math.sqrt(pos.getX() * pos.getX() + pos.getZ() * pos.getZ()) > OUTER_ISLAND_RADIUS;
		};
	}

	/** 是否允许该玩家触发（和平 / 旁观不触发；创造模式可观赏，按设计 §6.2 正常触发） */
	public static boolean canTrigger(ServerPlayer player) {
		return !player.level().getDifficulty().equals(net.minecraft.world.Difficulty.PEACEFUL)
				&& !player.isSpectator()
				&& (player.gameMode().isSurvival() || player.gameMode().isCreative());
	}

	/** 外岛半径（格）：末地外岛自 1008 格起，取 1000 作保守阈值 */
	private static final double OUTER_ISLAND_RADIUS = 1000.0;

	private static boolean isColdOcean(String biomeId) {
		return biomeId.equals("minecraft:frozen_ocean") || biomeId.equals("minecraft:deep_frozen_ocean")
				|| biomeId.equals("minecraft:cold_ocean") || biomeId.equals("minecraft:deep_cold_ocean");
	}

	private static String biomeId(net.minecraft.core.Holder<Biome> biome) {
		return biome.unwrapKey()
				.map(key -> key.identifier().toString())
				.orElse("");
	}
}
