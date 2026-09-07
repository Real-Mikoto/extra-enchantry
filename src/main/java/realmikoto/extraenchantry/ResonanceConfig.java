package realmikoto.extraenchantry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 共鸣规则数据化（1.3.0「铭刻与试炼」§2.5）：
 * 八系阈值、FULL 被动参数、主调强化参数与试炼关键阈值全部数据化。
 *
 * 数据源：{@code data/<namespace>/resonance/families/<family>.json}（每家族一份）。
 * 容错策略（P0 验收）：
 *   - 每个字段 {@code optionalFieldOf} 缺省内置默认值——缺失字段自动回退；
 *   - 单个家族文件解析失败（字段非法/JSON 破损）→ 日志记录文件与原因，该家族回退内置默认，
 *     其余家族不受影响；
 *   - 数据包重载（/reload）后整体重新应用；移除数据包后回退默认。
 *
 * 内置默认值即 1.2.0 FULL 被动与 1.3.0 设计表主调数值（final_draft.md §2.2）。
 */
public final class ResonanceConfig {

	/** 单家族规则（全字段 optional：JSON 只写需要覆盖的字段） */
	public record FamilyRules(
			int partialThreshold,
			int fullThreshold,
			// 灵魂
			int soulInvisibilityTicks,
			int attunedSoulInvisibilityTicks,
			// 风暴
			double stormSpeedBonus,
			double attunedStormSpeedBonus,
			double attunedLightningReduction,
			// 锋刃
			double bladeComboPerHit,
			int bladeComboMaxHits,
			int attunedBladeComboMaxHits,
			long bladeComboWindowTicks,
			// 守护
			double guardKnockbackResist,
			double attunedGuardKnockbackResist,
			// 自然
			long natureStillTicks,
			float natureRegenHp,
			double attunedNatureRegenHp,
			long natureHurtPauseTicks,
			// 水
			double waterSpeedBonus,
			double attunedWaterSpeedBonus,
			int waterAirReplenishDivisor,
			int attunedWaterAirReplenishDivisor,
			// 风
			double windFallReduction,
			double attunedWindFallReduction,
			// 火焰
			double fireLavaSpeedBonus,
			// 试炼阈值
			int trialSoulKills,
			float trialBladeTargetHealth,
			int trialStormSeconds,
			double trialStormDistance,
			int trialGuardSeconds,
			float trialGuardDamage,
			double trialGuardMaxDisplacement,
			float trialNatureHealTotal,
			int trialWaterSeconds,
			int trialWindMinFallDistance,
			int trialFireSeconds,
			double trialFireDistance
	) {
		/** 全字段缺省的 Codec：JSON 未写的字段回退 defaultRules 对应值。
		 *  36 个字段超出 {@code RecordCodecBuilder.group} 的 16 参上限，
		 *  拆为 Head/Mid 两个中间 MapCodec（16+16）+ 外层 4 字段；
		 *  嵌套 MapCodec 在 group 中字段平铺到同一 JSON 层级，格式保持完全平铺。 */
		public static Codec<FamilyRules> codecOf(FamilyRules defaultRules) {
			MapCodec<Head> head = RecordCodecBuilder.mapCodec(hi -> hi.group(
					net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("partial_threshold", defaultRules.partialThreshold).forGetter(Head::partialThreshold),
					net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("full_threshold", defaultRules.fullThreshold).forGetter(Head::fullThreshold),
					net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("soul_invisibility_ticks", defaultRules.soulInvisibilityTicks).forGetter(Head::soulInvisibilityTicks),
					net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("attuned_soul_invisibility_ticks", defaultRules.attunedSoulInvisibilityTicks).forGetter(Head::attunedSoulInvisibilityTicks),
					Codec.DOUBLE.optionalFieldOf("storm_speed_bonus", defaultRules.stormSpeedBonus).forGetter(Head::stormSpeedBonus),
					Codec.DOUBLE.optionalFieldOf("attuned_storm_speed_bonus", defaultRules.attunedStormSpeedBonus).forGetter(Head::attunedStormSpeedBonus),
					Codec.DOUBLE.optionalFieldOf("attuned_lightning_reduction", defaultRules.attunedLightningReduction).forGetter(Head::attunedLightningReduction),
					Codec.DOUBLE.optionalFieldOf("blade_combo_per_hit", defaultRules.bladeComboPerHit).forGetter(Head::bladeComboPerHit),
					net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("blade_combo_max_hits", defaultRules.bladeComboMaxHits).forGetter(Head::bladeComboMaxHits),
					net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("attuned_blade_combo_max_hits", defaultRules.attunedBladeComboMaxHits).forGetter(Head::attunedBladeComboMaxHits),
					Codec.LONG.optionalFieldOf("blade_combo_window_ticks", defaultRules.bladeComboWindowTicks).forGetter(Head::bladeComboWindowTicks),
					Codec.DOUBLE.optionalFieldOf("guard_knockback_resist", defaultRules.guardKnockbackResist).forGetter(Head::guardKnockbackResist),
					Codec.DOUBLE.optionalFieldOf("attuned_guard_knockback_resist", defaultRules.attunedGuardKnockbackResist).forGetter(Head::attunedGuardKnockbackResist),
					Codec.LONG.optionalFieldOf("nature_still_ticks", defaultRules.natureStillTicks).forGetter(Head::natureStillTicks),
					Codec.FLOAT.optionalFieldOf("nature_regen_hp", defaultRules.natureRegenHp).forGetter(Head::natureRegenHp),
					Codec.DOUBLE.optionalFieldOf("attuned_nature_regen_hp", defaultRules.attunedNatureRegenHp).forGetter(Head::attunedNatureRegenHp)
			).apply(hi, Head::new));
			MapCodec<Mid> mid = RecordCodecBuilder.mapCodec(mi -> mi.group(
					Codec.LONG.optionalFieldOf("nature_hurt_pause_ticks", defaultRules.natureHurtPauseTicks).forGetter(Mid::natureHurtPauseTicks),
					Codec.DOUBLE.optionalFieldOf("water_speed_bonus", defaultRules.waterSpeedBonus).forGetter(Mid::waterSpeedBonus),
					Codec.DOUBLE.optionalFieldOf("attuned_water_speed_bonus", defaultRules.attunedWaterSpeedBonus).forGetter(Mid::attunedWaterSpeedBonus),
					net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("water_air_replenish_divisor", defaultRules.waterAirReplenishDivisor).forGetter(Mid::waterAirReplenishDivisor),
					net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("attuned_water_air_replenish_divisor", defaultRules.attunedWaterAirReplenishDivisor).forGetter(Mid::attunedWaterAirReplenishDivisor),
					Codec.DOUBLE.optionalFieldOf("wind_fall_reduction", defaultRules.windFallReduction).forGetter(Mid::windFallReduction),
					Codec.DOUBLE.optionalFieldOf("attuned_wind_fall_reduction", defaultRules.attunedWindFallReduction).forGetter(Mid::attunedWindFallReduction),
					Codec.DOUBLE.optionalFieldOf("fire_lava_speed_bonus", defaultRules.fireLavaSpeedBonus).forGetter(Mid::fireLavaSpeedBonus),
					net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("trial_soul_kills", defaultRules.trialSoulKills).forGetter(Mid::trialSoulKills),
					Codec.FLOAT.optionalFieldOf("trial_blade_target_health", defaultRules.trialBladeTargetHealth).forGetter(Mid::trialBladeTargetHealth),
					net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("trial_storm_seconds", defaultRules.trialStormSeconds).forGetter(Mid::trialStormSeconds),
					Codec.DOUBLE.optionalFieldOf("trial_storm_distance", defaultRules.trialStormDistance).forGetter(Mid::trialStormDistance),
					net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("trial_guard_seconds", defaultRules.trialGuardSeconds).forGetter(Mid::trialGuardSeconds),
					Codec.FLOAT.optionalFieldOf("trial_guard_damage", defaultRules.trialGuardDamage).forGetter(Mid::trialGuardDamage),
					Codec.DOUBLE.optionalFieldOf("trial_guard_max_displacement", defaultRules.trialGuardMaxDisplacement).forGetter(Mid::trialGuardMaxDisplacement),
					Codec.FLOAT.optionalFieldOf("trial_nature_heal_total", defaultRules.trialNatureHealTotal).forGetter(Mid::trialNatureHealTotal)
			).apply(mi, Mid::new));
			return RecordCodecBuilder.create(instance -> instance.group(
					head.forGetter(r -> new Head(
							r.partialThreshold(), r.fullThreshold(),
							r.soulInvisibilityTicks(), r.attunedSoulInvisibilityTicks(),
							r.stormSpeedBonus(), r.attunedStormSpeedBonus(), r.attunedLightningReduction(),
							r.bladeComboPerHit(), r.bladeComboMaxHits(), r.attunedBladeComboMaxHits(), r.bladeComboWindowTicks(),
							r.guardKnockbackResist(), r.attunedGuardKnockbackResist(),
							r.natureStillTicks(), r.natureRegenHp(), r.attunedNatureRegenHp())),
					mid.forGetter(r -> new Mid(
							r.natureHurtPauseTicks(),
							r.waterSpeedBonus(), r.attunedWaterSpeedBonus(),
							r.waterAirReplenishDivisor(), r.attunedWaterAirReplenishDivisor(),
							r.windFallReduction(), r.attunedWindFallReduction(),
							r.fireLavaSpeedBonus(),
							r.trialSoulKills(), r.trialBladeTargetHealth(),
							r.trialStormSeconds(), r.trialStormDistance(),
							r.trialGuardSeconds(), r.trialGuardDamage(), r.trialGuardMaxDisplacement(),
							r.trialNatureHealTotal())),
					net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("trial_water_seconds", defaultRules.trialWaterSeconds).forGetter(FamilyRules::trialWaterSeconds),
					net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("trial_wind_min_fall_distance", defaultRules.trialWindMinFallDistance).forGetter(FamilyRules::trialWindMinFallDistance),
					net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("trial_fire_seconds", defaultRules.trialFireSeconds).forGetter(FamilyRules::trialFireSeconds),
					Codec.DOUBLE.optionalFieldOf("trial_fire_distance", defaultRules.trialFireDistance).forGetter(FamilyRules::trialFireDistance)
			).apply(instance, (h, m, waterSeconds, windFall, fireSeconds, fireDistance) -> new FamilyRules(
					h.partialThreshold(), h.fullThreshold(),
					h.soulInvisibilityTicks(), h.attunedSoulInvisibilityTicks(),
					h.stormSpeedBonus(), h.attunedStormSpeedBonus(), h.attunedLightningReduction(),
					h.bladeComboPerHit(), h.bladeComboMaxHits(), h.attunedBladeComboMaxHits(), h.bladeComboWindowTicks(),
					h.guardKnockbackResist(), h.attunedGuardKnockbackResist(),
					h.natureStillTicks(), h.natureRegenHp(), h.attunedNatureRegenHp(),
					m.natureHurtPauseTicks(),
					m.waterSpeedBonus(), m.attunedWaterSpeedBonus(),
					m.waterAirReplenishDivisor(), m.attunedWaterAirReplenishDivisor(),
					m.windFallReduction(), m.attunedWindFallReduction(),
					m.fireLavaSpeedBonus(),
					m.trialSoulKills(), m.trialBladeTargetHealth(),
					m.trialStormSeconds(), m.trialStormDistance(),
					m.trialGuardSeconds(), m.trialGuardDamage(), m.trialGuardMaxDisplacement(),
					m.trialNatureHealTotal(),
					waterSeconds, windFall, fireSeconds, fireDistance)));
		}

		/** codecOf 拆分用：字段 1–16（阈值/灵魂/风暴/锋刃/守护/自然回血） */
		private record Head(
				int partialThreshold,
				int fullThreshold,
				int soulInvisibilityTicks,
				int attunedSoulInvisibilityTicks,
				double stormSpeedBonus,
				double attunedStormSpeedBonus,
				double attunedLightningReduction,
				double bladeComboPerHit,
				int bladeComboMaxHits,
				int attunedBladeComboMaxHits,
				long bladeComboWindowTicks,
				double guardKnockbackResist,
				double attunedGuardKnockbackResist,
				long natureStillTicks,
				float natureRegenHp,
				double attunedNatureRegenHp
		) {
		}

		/** codecOf 拆分用：字段 17–32（自然暂停/水/风/火焰/试炼阈值前半） */
		private record Mid(
				long natureHurtPauseTicks,
				double waterSpeedBonus,
				double attunedWaterSpeedBonus,
				int waterAirReplenishDivisor,
				int attunedWaterAirReplenishDivisor,
				double windFallReduction,
				double attunedWindFallReduction,
				double fireLavaSpeedBonus,
				int trialSoulKills,
				float trialBladeTargetHealth,
				int trialStormSeconds,
				double trialStormDistance,
				int trialGuardSeconds,
				float trialGuardDamage,
				double trialGuardMaxDisplacement,
				float trialNatureHealTotal
		) {
		}
	}

	// ============ 内置默认值（1.2.0 FULL 被动 + 1.3.0 设计表主调） ============

	private static final Map<FamilyResonanceManager.Family, FamilyRules> DEFAULTS = buildDefaults();

	private static Map<FamilyResonanceManager.Family, FamilyRules> buildDefaults() {
		Map<FamilyResonanceManager.Family, FamilyRules> map = new EnumMap<>(FamilyResonanceManager.Family.class);
		FamilyRules base = new FamilyRules(
				3, 5,
				60, 100,
				0.20D, 0.25D, 0.25D,
				0.05D, 3, 4, 60L,
				0.50D, 0.70D,
				60L, 1.0F, 1.5D, 40L,
				0.30D, 0.40D, 40, 33,
				0.50D, 0.65D,
				0.15D,
				5, 100.0F, 120, 600.0D,
				30, 80.0F, 8.0D,
				30.0F, 180, 80,
				60, 100.0D);
		for (FamilyResonanceManager.Family family : FamilyResonanceManager.Family.values()) {
			map.put(family, base);
		}
		return Map.copyOf(map);
	}

	/** 当前生效规则（volatile 保证 /reload 后跨线程可见） */
	private static volatile Map<FamilyResonanceManager.Family, FamilyRules> ACTIVE = DEFAULTS;

	/** 取某家族当前生效规则（无加载/加载失败 → 内置默认） */
	public static FamilyRules rules(FamilyResonanceManager.Family family) {
		return ACTIVE.getOrDefault(family, DEFAULTS.get(family));
	}

	// ============ 数据包加载器 ============

	/**
	 * 共鸣规则重载监听：读 {@code resonance/families/<family>.json}。
	 * 注册在 SERVER_DATA 阶段（/reload 与服务器启动都会执行）。
	 */
	public static final class RulesLoader extends SimpleJsonResourceReloadListener<FamilyRules>
			implements IdentifiableResourceReloadListener {

		private static final Identifier ID = ExtraEnchantry.id("resonance_rules");

		public RulesLoader() {
			// 用默认规则做缺省兜底的 Codec（各字段 optionalFieldOf）
			super(FamilyRules.codecOf(DEFAULTS.get(FamilyResonanceManager.Family.SOUL)),
					net.minecraft.resources.FileToIdConverter.json("resonance/families"));
		}

		@Override
		public Identifier getFabricId() {
			return ID;
		}

		@Override
		protected void apply(Map<Identifier, FamilyRules> loaded, ResourceManager resourceManager,
				ProfilerFiller profiler) {
			// 数据包重载后标脏共鸣扫描缓存：下一次扫描（≤1 秒）全量重算
			FamilyResonanceManager.clearScanCaches();
			if (loaded.isEmpty()) {
				// 数据包未提供任何规则文件 → 回退内置默认
				ACTIVE = DEFAULTS;
				return;
			}
			Map<FamilyResonanceManager.Family, FamilyRules> merged = new HashMap<>();
			for (FamilyResonanceManager.Family family : FamilyResonanceManager.Family.values()) {
				merged.put(family, DEFAULTS.get(family));
			}
			int applied = 0;
			for (Map.Entry<Identifier, FamilyRules> entry : loaded.entrySet()) {
				FamilyResonanceManager.Family family = familyFromPath(entry.getKey());
				if (family == null) {
					ExtraEnchantry.LOGGER.warn("[extra-enchantry] 共鸣规则文件 {} 不匹配任何家族，已跳过", entry.getKey());
					continue;
				}
				merged.put(family, entry.getValue());
				applied++;
			}
			ACTIVE = Map.copyOf(merged);
			ExtraEnchantry.LOGGER.info("[extra-enchantry] 共鸣规则已加载：{} 个家族（数据包重载后即刻生效）", applied);
		}

		/** 文件路径（family_soul 等）→ 家族枚举 */
		private static FamilyResonanceManager.Family familyFromPath(Identifier id) {
			String path = id.getPath();
			for (FamilyResonanceManager.Family family : FamilyResonanceManager.Family.values()) {
				if (path.equals(family.name().toLowerCase())) {
					return family;
				}
			}
			return null;
		}
	}

	private ResonanceConfig() {
	}
}
