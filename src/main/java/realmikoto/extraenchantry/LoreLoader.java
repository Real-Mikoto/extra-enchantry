package realmikoto.extraenchantry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lore 数据加载器（1.3.1「铭文纪元」§7）：全部叙事文本数据化。
 *
 * 数据源（{@code data/<namespace>/lore/}，单文件失败仅该条目回退内置默认并记日志）：
 * <ul>
 *   <li>{@code onboarding.json}：silence_onboarding 全局静默开关 + 来者手札第 1/3/4 页文本；</li>
 *   <li>{@code family_inscriptions/<family>.json} ×8：家族铭文（职能 / 陨落 / 铭文残响来历）；</li>
 *   <li>{@code limit_break_shards/act_<n>.json} ×4：破限残页（浩劫四幕叙事）；</li>
 *   <li>{@code cataclysm/act_<n>.json} ×4：波次完成时的幕名 + 格言 chat；</li>
 *   <li>{@code attunement/first_attune.json}：八家族首次铭刻格言。</li>
 * </ul>
 *
 * 与 {@link ResonanceConfig.RulesLoader} 同款容错：字段缺失 / JSON 破损 → 内置默认，
 * 其余条目不受影响；{@code /reload} 后整体重新应用（lore 均为无状态文本，无需清缓存）。
 * 内置默认即「铭文纪元」编年总纲（DESIGN/1.3.1-design.md §二）。
 */
public final class LoreLoader {

	// ============ 数据记录 ============

	/** 家族铭文：标题 + 行文本 */
	public record FamilyInscription(String title, List<String> lines) {
	}

	/** 破限残页：幕号 + 标题 + 副题（幕名）+ 行文本 */
	public record ShardLore(int act, String title, String subtitle, List<String> lines) {
	}

	/** 浩劫一幕：幕号 + 标题 + 副题 + 波次完成 chat 文本 */
	public record CataclysmAct(int act, String title, String subtitle, String waveCompleteChat) {
	}

	/** 来者手札静态页：页标题 + 行文本（家族铭文页由已解锁铭文动态拼装） */
	public record LetterPage(String title, List<String> lines) {
	}

	// ============ 内置默认（「铭文纪元」编年总纲） ============

	private static Map<FamilyResonanceManager.Family, FamilyInscription> buildDefaultInscriptions() {
		Map<FamilyResonanceManager.Family, FamilyInscription> map = new EnumMap<>(FamilyResonanceManager.Family.class);
		map.put(FamilyResonanceManager.Family.SOUL, new FamilyInscription("灵魂铭文", List.of(
				"初民纪元，灵魂家族执掌生者与亡者的边界。",
				"他们的铭文能感应将死者的呼吸，能让亡者显形——生者暂居，死者长眠。",
				"浩劫第四幕「魂灵迷航」中，灵魂家的铭刻最先被击碎。",
				"断罪，是审判的最后一道；蚀命，是死者对生者的回礼；",
				"无踪，是亡者敛息的步法；余烬与劫后余辉，是灵魂自火中归来的证词。")));
		map.put(FamilyResonanceManager.Family.STORM, new FamilyInscription("风暴铭文", List.of(
				"初民纪元，风暴家族统御天象——天不仁而怒，地不言而承。",
				"雷霆是他们的话语，坠星是他们的判笔，假象是雷暴中的蜃影。",
				"浩劫第二幕「风暴失序」，无休的雷暴反噬了执雷者。",
				"破限，正是风暴家族封印浩劫时留下的最后一道雷霆之礼。")));
		map.put(FamilyResonanceManager.Family.BLADE, new FamilyInscription("锋刃铭文", List.of(
				"初民纪元，锋刃家族司战斗与技艺——一刃既出，万念归尘。",
				"藏锋于鞘，破阵于野，触及八荒；汲取是刃饮血的自证。",
				"浩劫中锋刃家损失了最多的战士，铭文却随战技流传。",
				"冲阵是盾与刃合流的孤例——那是守护家与锋刃家并肩的遗痕。")));
		map.put(FamilyResonanceManager.Family.NATURE, new FamilyInscription("自然铭文", List.of(
				"初民纪元，自然家族育万灵、掌收获——根深者不惧风。",
				"丰壤让土地慷慨，活力让血肉坚韧，汲取与庇护是草木的呼吸。",
				"浩劫过后，自然家的铭文沉入土壤，随作物一同生长。",
				"拾起它的人，会在丰收时听见大地回赠的低语。")));
		map.put(FamilyResonanceManager.Family.GUARD, new FamilyInscription("守护铭文", List.of(
				"初民纪元，守护家族立誓为盾——盾碎之时，魂已无伤。",
				"壁垒、坚壁、不屈、凋零保护，誓约是守护者最古老的盟证。",
				"封印浩劫的代价由守护家一力承担：八柱倾颓，唯盾不倒。",
				"如今每一面挡下致命一击的盾，都有他们的执念回响。")));
		map.put(FamilyResonanceManager.Family.WIND, new FamilyInscription("风铭文", List.of(
				"初民纪元，风家族掌流动与轻盈——无翅亦可凌云。",
				"御风者乘烟而上，疾风者步履如梭，空跃者踏风为阶，归羽者箭不虚发。",
				"浩劫的风暴撕碎了风家的羽翼，铭文散入高天。",
				"坠落的人若听见风声扶了一把，那是风家在还愿。")));
		map.put(FamilyResonanceManager.Family.FIRE, new FamilyInscription("火焰铭文", List.of(
				"初民纪元，火焰家族司净化与重生——烬中余温，未灭之心。",
				"炽焰行者踏火不留痕，拓阶者以火开山，余烬是火家不灭的芯。",
				"浩劫第三幕「火焰焚烧」，下界火种倒灌主世界，火家以身饲火。",
				"劫后余辉的暖金色，是火焰家留给生者的最后一点体温。")));
		map.put(FamilyResonanceManager.Family.WATER, new FamilyInscription("水铭文", List.of(
				"初民纪元，水家族掌渊流与深息——静水流深，渊息不竭。",
				"渊息者的肺是深海的赠礼，霆霓是水与雷的混血儿。",
				"浩劫第一幕「深渊苏醒」，监守者自深岩涌出，渊流被搅浑。",
				"潜得足够深的人会发现：水家从未离开，他们只是沉在光到不了的地方。")));
		return map;
	}

	private static Map<Integer, ShardLore> buildDefaultShards() {
		Map<Integer, ShardLore> map = new HashMap<>();
		map.put(1, new ShardLore(1, "破限残页·壹", "深渊苏醒", List.of(
				"监守者自深岩中涌出，深渊的具现反噬其守护者。",
				"灵魂悸动，幽匿蔓延——此为浩劫第一幕「深渊苏醒」。",
				"残页上的铭文残缺不全，拼图的第一个角……")));
		map.put(2, new ShardLore(2, "破限残页·贰", "风暴失序", List.of(
				"雷暴无尽，劫掠兽部族在雷光中失控。",
				"天象紊乱，袭击连绵——此为浩劫第二幕「风暴失序」。",
				"残页边缘焦黑蜷曲，仿佛被雷火燎过……")));
		map.put(3, new ShardLore(3, "破限残页·叁", "火焰焚烧", List.of(
				"凋灵与下界火种倒灌主世界，烬火遮天。",
				"万物成薪，唯志不燃——此为浩劫第三幕「火焰焚烧」。",
				"残页中段透出微弱暖光，像未熄的余烬……")));
		map.put(4, new ShardLore(4, "破限残页·肆", "魂灵迷航", List.of(
				"末影维度入侵，亡魂在异界迷航。",
				"黑曜之柱间，龙影双悬——此为浩劫第四幕「魂灵迷航」。",
				"最后一块残页。合上它，图样完整了——「极限之器」的铭文重现于世。")));
		return map;
	}

	private static Map<Integer, CataclysmAct> buildDefaultActs() {
		Map<Integer, CataclysmAct> map = new HashMap<>();
		map.put(1, new CataclysmAct(1, "诸界浩劫·第一幕", "深渊苏醒",
				"「深渊苏醒」——监守者不再沉默。破限残页·壹已收。"));
		map.put(2, new CataclysmAct(2, "诸界浩劫·第二幕", "风暴失序",
				"「风暴失序」——雷暴中失控的部族已肃清。破限残页·贰已收。"));
		map.put(3, new CataclysmAct(3, "诸界浩劫·第三幕", "火焰焚烧",
				"「火焰焚烧」——烬火燃尽，志士不折。破限残页·叁已收。"));
		map.put(4, new CataclysmAct(4, "诸界浩劫·第四幕", "魂灵迷航",
				"「魂灵迷航」——迷航的亡魂得以安息。破限残页·肆已收。"));
		return map;
	}

	private static Map<FamilyResonanceManager.Family, List<String>> buildDefaultFirstAttune() {
		Map<FamilyResonanceManager.Family, List<String>> map = new EnumMap<>(FamilyResonanceManager.Family.class);
		map.put(FamilyResonanceManager.Family.SOUL, List.of("你选择了凝视深渊，", "深渊将回报你以真名。"));
		map.put(FamilyResonanceManager.Family.STORM, List.of("你选择了驾驭雷霆，", "雷霆将承认你为使者。"));
		map.put(FamilyResonanceManager.Family.BLADE, List.of("你选择了锋刃之路，", "刃将永不卷口。"));
		map.put(FamilyResonanceManager.Family.NATURE, List.of("你选择了倾听大地，", "大地将回赠你以收获。"));
		map.put(FamilyResonanceManager.Family.WATER, List.of("你选择了潜入渊底，", "渊息者将与你同行。"));
		map.put(FamilyResonanceManager.Family.WIND, List.of("你选择了追风而行，", "风将负你以羽翼。"));
		map.put(FamilyResonanceManager.Family.GUARD, List.of("你选择了为盾，", "盾将护你至最后一击。"));
		map.put(FamilyResonanceManager.Family.FIRE, List.of("你选择了浴火，", "烬中余温将永不熄灭。"));
		return map;
	}

	private static final LetterPage DEFAULT_LETTER_INTRO = new LetterPage("铭文纪元 · 序", List.of(
			"这个世界之前，还有一个世界。",
			"八个家族各执一根世界支柱，以「铭文」为载体传递力量——",
			"灵魂掌生死，风暴御天象，锋刃司战斗，自然育万灵，",
			"深渊行无形，风逐流动，守护立不倒，火焰燃不熄。",
			"后来，诸界浩劫四幕连降。八家共举「极限之器」封印浩劫，",
			"代价是器碎文散——铭文散作碎片，沉入你如今所见的每一件附魔。",
			"你手上的附魔书，就是铭文的残响。",
			"用紫水晶碎片、书与青金石合成「共鸣秘典」，聆听它们的声音。"));

	private static final LetterPage DEFAULT_LETTER_CATACLYSM = new LetterPage("极限之器 · 铭刻图样", List.of(
			"四张残页拼合成完整的图样：那是一件器物的铭文——「极限之器」。",
			"深渊苏醒，风暴失序，火焰焚烧，魂灵迷航——四幕浩劫曾以此器终结。",
			"重铸它的过程，写在每一场诸界浩劫的挑战里：拾起破限之书者，",
			"即是新一代的天选共鸣者。浩劫会考验你，器成之日，无人能挡。"));

	private static final LetterPage DEFAULT_LETTER_FINALE = new LetterPage("铭文纪元 · 终章", List.of(
			"八枚铭印归于一体，八种铭文向你回响。",
			"集齐碎片的共鸣者啊——极限之器在你手中重铸，",
			"初民纪元的记忆自此由你执笔。编年史的新一页，从你开始。"));

	// ============ 当前生效数据（volatile 供 /reload 后跨线程可见） ============

	private static volatile boolean SILENCED = false;
	private static volatile Map<FamilyResonanceManager.Family, FamilyInscription> INSCRIPTIONS = buildDefaultInscriptions();
	private static volatile Map<Integer, ShardLore> SHARDS = buildDefaultShards();
	private static volatile Map<Integer, CataclysmAct> ACTS = buildDefaultActs();
	private static volatile Map<FamilyResonanceManager.Family, List<String>> FIRST_ATTUNE = buildDefaultFirstAttune();
	private static volatile LetterPage LETTER_INTRO = DEFAULT_LETTER_INTRO;
	private static volatile LetterPage LETTER_CATACLYSM = DEFAULT_LETTER_CATACLYSM;
	private static volatile LetterPage LETTER_FINALE = DEFAULT_LETTER_FINALE;

	private LoreLoader() {
	}

	// ============ 查询 API ============

	/** 全局静默开关（lore/onboarding.json · silence_onboarding；静默只关提示，物品照发） */
	public static boolean silenced() {
		return SILENCED;
	}

	/** 家族铭文（未加载 / 解析失败 → 内置默认） */
	public static FamilyInscription inscription(FamilyResonanceManager.Family family) {
		FamilyInscription lore = INSCRIPTIONS.get(family);
		return lore != null ? lore : INSCRIPTIONS.get(FamilyResonanceManager.Family.SOUL);
	}

	/** 破限残页（act 越界 → 第 1 幕兜底） */
	public static ShardLore shard(int act) {
		ShardLore lore = SHARDS.get(act);
		return lore != null ? lore : SHARDS.get(1);
	}

	/** 浩劫一幕（act 越界 → 第 1 幕兜底） */
	public static CataclysmAct act(int act) {
		CataclysmAct lore = ACTS.get(act);
		return lore != null ? lore : ACTS.get(1);
	}

	/** 家族首次铭刻格言 */
	public static List<String> firstAttuneLines(FamilyResonanceManager.Family family) {
		return FIRST_ATTUNE.getOrDefault(family, List.of());
	}

	public static LetterPage letterIntro() {
		return LETTER_INTRO;
	}

	public static LetterPage letterCataclysm() {
		return LETTER_CATACLYSM;
	}

	public static LetterPage letterFinale() {
		return LETTER_FINALE;
	}

	// ============ 数据包加载器 ============

	/**
	 * Lore 重载监听：整目录按 {@code ExtraCodecs.JSON} 读原始 JsonElement，
	 * {@link #apply} 内按路径分派手工解析——五类 schema 不同，逐条独立容错。
	 */
	public static final class Loader extends SimpleJsonResourceReloadListener<JsonElement>
			implements IdentifiableResourceReloadListener {

		private static final Identifier ID = ExtraEnchantry.id("lore");

		public Loader() {
			super(ExtraCodecs.JSON, FileToIdConverter.json("lore"));
		}

		@Override
		public Identifier getFabricId() {
			return ID;
		}

		@Override
		protected void apply(Map<Identifier, JsonElement> loaded, ResourceManager resourceManager,
				ProfilerFiller profiler) {
			boolean silenced = false;
			Map<FamilyResonanceManager.Family, FamilyInscription> inscriptions = buildDefaultInscriptions();
			Map<Integer, ShardLore> shards = buildDefaultShards();
			Map<Integer, CataclysmAct> acts = buildDefaultActs();
			Map<FamilyResonanceManager.Family, List<String>> firstAttune = buildDefaultFirstAttune();
			LetterPage intro = DEFAULT_LETTER_INTRO;
			LetterPage cataclysm = DEFAULT_LETTER_CATACLYSM;
			LetterPage finale = DEFAULT_LETTER_FINALE;

			for (Map.Entry<Identifier, JsonElement> entry : loaded.entrySet()) {
				String path = entry.getKey().getPath();
				try {
					JsonObject json = entry.getValue().getAsJsonObject();
					switch (path) {
						case "onboarding" -> {
							silenced = readBool(json, "silence_onboarding", false);
							intro = readLetterPage(json, "intro", intro);
							cataclysm = readLetterPage(json, "cataclysm_complete", cataclysm);
							finale = readLetterPage(json, "grand_resonator", finale);
						}
						case "attunement/first_attune" -> {
							JsonObject lines = json.getAsJsonObject("first_attune_lines");
							if (lines != null) {
								for (FamilyResonanceManager.Family family : FamilyResonanceManager.Family.values()) {
									List<String> parsed = readLines(lines, family.name().toLowerCase(Locale.ROOT));
									if (parsed != null) {
										firstAttune.put(family, parsed);
									}
								}
							}
						}
						default -> {
							if (path.startsWith("family_inscriptions/")) {
								FamilyResonanceManager.Family family = familyOf(
										path.substring("family_inscriptions/".length()));
								List<String> lines = readLines(json, "lines");
								if (family != null && lines != null) {
									inscriptions.put(family, new FamilyInscription(
											readString(json, "title", ""), lines));
								}
							} else if (path.startsWith("limit_break_shards/")) {
								int act = readInt(json, "act", 1);
								List<String> lines = readLines(json, "lines");
								if (lines != null) {
									shards.put(act, new ShardLore(act,
											readString(json, "title", ""),
											readString(json, "subtitle", ""),
											lines));
								}
							} else if (path.startsWith("cataclysm/")) {
								int act = readInt(json, "act", 1);
								acts.put(act, new CataclysmAct(act,
										readString(json, "title", ""),
										readString(json, "subtitle", ""),
										readString(json, "wave_complete_chat", "")));
							} else {
								ExtraEnchantry.LOGGER.warn("[extra-enchantry] lore 文件 {} 不属于任何已知类别，已跳过",
										entry.getKey());
							}
						}
					}
				} catch (Exception e) {
					ExtraEnchantry.LOGGER.warn("[extra-enchantry] lore 文件 {} 解析失败，该条目回退内置默认: {}",
							entry.getKey(), e.getMessage());
				}
			}
			SILENCED = silenced;
			INSCRIPTIONS = Map.copyOf(inscriptions);
			SHARDS = Map.copyOf(shards);
			ACTS = Map.copyOf(acts);
			FIRST_ATTUNE = Map.copyOf(firstAttune);
			LETTER_INTRO = intro;
			LETTER_CATACLYSM = cataclysm;
			LETTER_FINALE = finale;
			ExtraEnchantry.LOGGER.info("[extra-enchantry] lore 已加载：铭文 {} / 残页 {} / 浩劫幕 {}（静默: {}）",
					inscriptions.size(), shards.size(), acts.size(), silenced);
		}

		// ---- 手工解析辅助（字段缺失回退，单条目不整体失败） ----

		private static String readString(JsonObject json, String field, String fallback) {
			var member = json.get(field);
			return member != null && member.isJsonPrimitive() ? member.getAsString() : fallback;
		}

		private static int readInt(JsonObject json, String field, int fallback) {
			var member = json.get(field);
			return member != null && member.isJsonPrimitive() ? member.getAsInt() : fallback;
		}

		private static boolean readBool(JsonObject json, String field, boolean fallback) {
			var member = json.get(field);
			return member != null && member.isJsonPrimitive() ? member.getAsBoolean() : fallback;
		}

		/** 读对象内 field 名的字符串数组；缺失 / 非数组 → null（由调用方保留默认） */
		private static List<String> readLines(JsonObject json, String field) {
			var member = json.get(field);
			if (member == null || !member.isJsonArray()) {
				return null;
			}
			List<String> lines = new java.util.ArrayList<>();
			for (JsonElement line : member.getAsJsonArray()) {
				if (line.isJsonPrimitive()) {
					lines.add(line.getAsString());
				}
			}
			return lines;
		}

		/** 来者手札页：welcome_letter.pages[] 按 id 匹配 */
		private static LetterPage readLetterPage(JsonObject json, String pageId, LetterPage fallback) {
			var letter = json.getAsJsonObject("welcome_letter");
			if (letter == null) {
				return fallback;
			}
			var pages = letter.getAsJsonArray("pages");
			if (pages == null) {
				return fallback;
			}
			for (JsonElement element : pages) {
				if (!element.isJsonObject()) {
					continue;
				}
				JsonObject page = element.getAsJsonObject();
				if (pageId.equals(readString(page, "id", ""))) {
					List<String> lines = readLines(page, "lines");
					if (lines != null) {
						return new LetterPage(readString(page, "title", fallback.title()), lines);
					}
				}
			}
			return fallback;
		}

		private static FamilyResonanceManager.Family familyOf(String path) {
			try {
				return FamilyResonanceManager.Family.valueOf(path.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
	}
}
