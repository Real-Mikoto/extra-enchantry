package realmikoto.extraenchantry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.FileToIdConverter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

/**
 * 五境领主「精英遭遇战」规则数据化（1.5.0「五境领主」§7.5 配置项）。
 *
 * 数据源：{@code data/<namespace>/elite_encounter/<realm>.json}（每境一份；
 * realm ∈ {overwarden, emberbone, tidal, hag, ender}），
 * 另有 {@code elite_encounter/global.json} 控制全局开关与冷却。
 *
 * 容错策略（与 ResonanceConfig 一致）：
 *   - 每个字段 {@code optionalFieldOf} 带内置默认值，缺失字段自动回退；
 *   - 单个文件解析失败 → 日志记录文件与原因，该境回退内置默认，其余境不受影响；
 *   - 数据包重载（/reload）后整体重新应用；移除数据包后回退默认。
 *
 * 内置默认值 = 设计文档 §1~§5 属性表与 §7.5 配置表。
 */
public final class EliteEncounterConfig {

	// ============ 全局规则 ============

	public record Global(boolean enabled, int cooldownMinutes, int interruptedCooldownMinutes,
			int maxActivePerDimension) {

		public static final Global DEFAULT = new Global(true, 10, 5, 1);

		public static final MapCodec<Global> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.BOOL.optionalFieldOf("enabled", DEFAULT.enabled).forGetter(Global::enabled),
				ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("cooldown_minutes", DEFAULT.cooldownMinutes)
						.forGetter(Global::cooldownMinutes),
				ExtraCodecs.NON_NEGATIVE_INT
						.optionalFieldOf("interrupted_cooldown_minutes", DEFAULT.interruptedCooldownMinutes)
						.forGetter(Global::interruptedCooldownMinutes),
				ExtraCodecs.POSITIVE_INT
						.optionalFieldOf("max_active_per_dimension", DEFAULT.maxActivePerDimension)
						.forGetter(Global::maxActivePerDimension)
		).apply(instance, Global::new));
	}

	// ============ 单境规则 ============

	public record Realm(boolean enabled, int preludeTicks, int unfoldTicks, int crackTicks,
			Stats stats, Thresholds thresholds) {

		/** 领主数值（设计表 §1.4 / §2.4 / §3.4 / §4.4 / §5.4） */
		public record Stats(float health, float meleeDamage, float scale, double followRange, int xp) {
		}

		/** 触发阈值与窗口（各境互不相同，见设计 §0.3 总览表） */
		public record Thresholds(
				int wardenAngerTicks, double wardenRange,
				int witherHoldTicks, int witherWindowTicks,
				int soakTicks, int soakWindowTicks, int laserHits, int laserWindowTicks,
				int potionHits, int witchKills, int hagWindowTicks, int nightStart, int nightEnd,
				int pearlUses, int pearlWindowTicks) {
		}

		private static MapCodec<Stats> statsCodec(Stats defaults) {
			return RecordCodecBuilder.mapCodec(instance -> instance.group(
					Codec.FLOAT.optionalFieldOf("health", defaults.health).forGetter(Stats::health),
					Codec.FLOAT.optionalFieldOf("melee_damage", defaults.meleeDamage).forGetter(Stats::meleeDamage),
					Codec.FLOAT.optionalFieldOf("scale", defaults.scale).forGetter(Stats::scale),
					Codec.DOUBLE.optionalFieldOf("follow_range", defaults.followRange).forGetter(Stats::followRange),
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("xp", defaults.xp).forGetter(Stats::xp)
			).apply(instance, Stats::new));
		}

		private static MapCodec<Thresholds> thresholdsCodec(Thresholds defaults) {
			return RecordCodecBuilder.mapCodec(instance -> instance.group(
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("warden_anger_ticks", defaults.wardenAngerTicks)
							.forGetter(Thresholds::wardenAngerTicks),
					Codec.DOUBLE.optionalFieldOf("warden_range", defaults.wardenRange)
							.forGetter(Thresholds::wardenRange),
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("wither_hold_ticks", defaults.witherHoldTicks)
							.forGetter(Thresholds::witherHoldTicks),
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("wither_window_ticks", defaults.witherWindowTicks)
							.forGetter(Thresholds::witherWindowTicks),
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("soak_ticks", defaults.soakTicks)
							.forGetter(Thresholds::soakTicks),
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("soak_window_ticks", defaults.soakWindowTicks)
							.forGetter(Thresholds::soakWindowTicks),
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("laser_hits", defaults.laserHits)
							.forGetter(Thresholds::laserHits),
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("laser_window_ticks", defaults.laserWindowTicks)
							.forGetter(Thresholds::laserWindowTicks),
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("potion_hits", defaults.potionHits)
							.forGetter(Thresholds::potionHits),
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("witch_kills", defaults.witchKills)
							.forGetter(Thresholds::witchKills),
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("hag_window_ticks", defaults.hagWindowTicks)
							.forGetter(Thresholds::hagWindowTicks),
					Codec.INT.optionalFieldOf("night_start", defaults.nightStart).forGetter(Thresholds::nightStart),
					Codec.INT.optionalFieldOf("night_end", defaults.nightEnd).forGetter(Thresholds::nightEnd),
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("pearl_uses", defaults.pearlUses)
							.forGetter(Thresholds::pearlUses),
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("pearl_window_ticks", defaults.pearlWindowTicks)
							.forGetter(Thresholds::pearlWindowTicks)
			).apply(instance, Thresholds::new));
		}

		public static MapCodec<Realm> codecOf(Realm defaults) {
			return RecordCodecBuilder.mapCodec(instance -> instance.group(
					Codec.BOOL.optionalFieldOf("enabled", defaults.enabled).forGetter(Realm::enabled),
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("prelude_ticks", defaults.preludeTicks)
							.forGetter(Realm::preludeTicks),
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("unfold_ticks", defaults.unfoldTicks)
							.forGetter(Realm::unfoldTicks),
					ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("crack_ticks", defaults.crackTicks)
							.forGetter(Realm::crackTicks),
					statsCodec(defaults.stats()).forGetter(Realm::stats),
					thresholdsCodec(defaults.thresholds()).forGetter(Realm::thresholds)
			).apply(instance, Realm::new));
		}
	}

	// ============ 内置默认值 ============

	private static final Map<String, Realm> DEFAULTS = buildDefaults();

	private static Map<String, Realm> buildDefaults() {
		Map<String, Realm> map = new HashMap<>();
		// 深暗境：愤怒持续 30 s、阶段 30/90/60，生命 300（对齐凋零，1.7.3）、音波 24（melee 45 走属性表）
		map.put("overwarden", new Realm(true, 30, 90, 60,
				new Realm.Stats(300.0F, 45.0F, 1.4F, 24.0D, 100),
				new Realm.Thresholds(600, 32.0D, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
		// 下界境：凋零累计 60 s / 120 s 窗口，阶段 30/120/0（凝聚直接觉醒）
		map.put("emberbone", new Realm(true, 30, 120, 0,
				new Realm.Stats(300.0F, 24.0F, 1.6F, 24.0D, 80),
				new Realm.Thresholds(0, 0.0D, 1200, 2400, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
		// 海洋境：浸泡 180 s / 300 s 窗口，激光 5 次 / 120 s 窗口
		map.put("tidal", new Realm(true, 30, 120, 0,
				new Realm.Stats(300.0F, 20.0F, 1.7F, 32.0D, 100),
				new Realm.Thresholds(0, 0.0D, 0, 0, 3600, 6000, 5, 2400, 0, 0, 0, 0, 0, 0, 0)));
		// 沼泽境：药水命中 3 或 击杀女巫 5（60 s 窗口），夜限 13000–23000
		map.put("hag", new Realm(true, 30, 120, 0,
				new Realm.Stats(300.0F, 15.0F, 1.35F, 24.0D, 80),
				new Realm.Thresholds(0, 0.0D, 0, 0, 0, 0, 0, 0, 3, 5, 1200, 13000, 23000, 0, 0)));
		// 末地境：末影珍珠 10 次 / 120 s 窗口
		map.put("ender", new Realm(true, 30, 120, 0,
				new Realm.Stats(300.0F, 20.0F, 1.5F, 32.0D, 100),
				new Realm.Thresholds(0, 0.0D, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 2400)));
		return Map.copyOf(map);
	}

	private static volatile Global ACTIVE_GLOBAL = Global.DEFAULT;
	private static volatile Map<String, Realm> ACTIVE = DEFAULTS;

	public static Global global() {
		return ACTIVE_GLOBAL;
	}

	/** 取某境当前生效规则（无数据/加载失败 → 内置默认） */
	public static Realm realm(String realmId) {
		Realm loaded = ACTIVE.get(realmId);
		if (loaded != null) {
			return loaded;
		}
		return DEFAULTS.getOrDefault(realmId, DEFAULTS.get("emberbone"));
	}

	// ============ 数据包加载器 ============

	/** 单境规则重载监听（{@code elite_encounter/<realm>.json}） */
	public static final class RealmLoader extends SimpleJsonResourceReloadListener<Realm>
			implements IdentifiableResourceReloadListener {

		private static final Identifier ID = ExtraEnchantry.id("elite_encounter_rules");

		public RealmLoader() {
			super(Realm.codecOf(DEFAULTS.get("emberbone")).codec(),
					FileToIdConverter.json("elite_encounter"));
		}

		@Override
		public Identifier getFabricId() {
			return ID;
		}

		@Override
		protected void apply(Map<Identifier, Realm> loaded, ResourceManager resourceManager,
				ProfilerFiller profiler) {
			if (loaded.isEmpty()) {
				ACTIVE = DEFAULTS;
				return;
			}
			Map<String, Realm> merged = new HashMap<>(DEFAULTS);
			int applied = 0;
			for (Map.Entry<Identifier, Realm> entry : loaded.entrySet()) {
				String realmId = entry.getKey().getPath();
				if (!DEFAULTS.containsKey(realmId)) {
					ExtraEnchantry.LOGGER.warn("[extra-enchantry] 精英遭遇配置文件 {} 不匹配任何境，已跳过",
							entry.getKey());
					continue;
				}
				merged.put(realmId, entry.getValue());
				applied++;
			}
			ACTIVE = Map.copyOf(merged);
			ExtraEnchantry.LOGGER.info("[extra-enchantry] 精英遭遇规则已加载：{} 个境（数据包重载后即刻生效）", applied);
		}
	}

	/** 全局规则重载监听（{@code elite_encounter/global.json}） */
	public static final class GlobalLoader extends SimpleJsonResourceReloadListener<Global>
			implements IdentifiableResourceReloadListener {

		private static final Identifier ID = ExtraEnchantry.id("elite_encounter_global");

		public GlobalLoader() {
			super(Global.CODEC.codec(), FileToIdConverter.json("elite_encounter_global"));
		}

		@Override
		public Identifier getFabricId() {
			return ID;
		}

		@Override
		protected void apply(Map<Identifier, Global> loaded, ResourceManager resourceManager,
				ProfilerFiller profiler) {
			// 约定文件名固定为 global.json（目录 elite_encounter_global/）
			Global found = loaded.get(ExtraEnchantry.id("global"));
			ACTIVE_GLOBAL = found != null ? found : Global.DEFAULT;
		}
	}

	private EliteEncounterConfig() {
	}
}
