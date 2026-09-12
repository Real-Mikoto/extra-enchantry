package realmikoto.extraenchantry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 共鸣调式（v1.8.0.2 补齐，设计稿 §6.6）：在八相之间"调音"。
 *
 * 三种调式（用「共鸣音叉」右键循环切换）：
 * - 静默调式：声纹 ×0.6（更安全，但回声视觉更弱）——默认；
 * - 轰鸣调式：声纹 ×1.5（视野/收获更猛，代价是危险）——需回响结晶解锁；
 * - 无相调式：声纹 ×0（完全隐匿）——需「无相之尘」解锁（第九相线索实体化）。
 *
 * 调式影响声纹半径与被动（§6.6「不同调式影响声纹与被动」；被动见
 * {@link #attackBonus} / {@link #gazeImmune}，由伤害结算与凝视累积处读取），
 * 不改既有共鸣 PARTIAL/FULL 阈值（设计稿 §11 纪律）。
 *
 * 持久化（Fabric Data Attachment + copyOnDeath）：与 AttunementManager 同款方案，
 * 调式选择与解锁状态跨重登/死亡保留。
 */
public final class AbyssTuning {

	public enum Mode {
		SILENT("silent", 0.6F),
		ROARING("roaring", 1.5F),
		AETHERLESS("aetherless", 0.0F);

		public final String id;
		public final float echoMultiplier;

		Mode(String id, float echoMultiplier) {
			this.id = id;
			this.echoMultiplier = echoMultiplier;
		}

		public static Mode byId(String id) {
			return switch (id == null ? "" : id) {
				case "roaring" -> ROARING;
				case "aetherless" -> AETHERLESS;
				default -> SILENT;
			};
		}
	}

	/** 调式持久化数据：当前调式 + 轰鸣/无相解锁标记 */
	public record TuningData(String modeId, boolean roarUnlocked, boolean aetherUnlocked) {
		public static final Codec<TuningData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.optionalFieldOf("mode", "silent").forGetter(TuningData::modeId),
				Codec.BOOL.optionalFieldOf("roar_unlocked", false).forGetter(TuningData::roarUnlocked),
				Codec.BOOL.optionalFieldOf("aether_unlocked", false).forGetter(TuningData::aetherUnlocked)
		).apply(instance, TuningData::new));

		public static final TuningData DEFAULT = new TuningData("silent", false, false);

		public Mode mode() {
			return Mode.byId(modeId);
		}
	}

	private static final AttachmentType<TuningData> TUNING =
			AttachmentRegistry.<TuningData>create(ExtraEnchantry.id("abyss_tuning"),
					builder -> builder.persistent(TuningData.CODEC).copyOnDeath());

	/** 显式触发静态初始化（onInitialize 早期调用，同 AttunementManager 纪律） */
	public static void register() {
	}

	private AbyssTuning() {
	}

	public static Mode modeOf(ServerPlayer player) {
		return dataOf(player).mode();
	}

	private static TuningData dataOf(ServerPlayer player) {
		TuningData data = player.getAttached(TUNING);
		return data == null ? TuningData.DEFAULT : data;
	}

	private static void save(ServerPlayer player, TuningData data) {
		player.setAttached(TUNING, data);
	}

	/** 右键共鸣音叉：解锁检查 + 循环切换（解锁标记持久化） */
	public static void cycle(ServerPlayer player) {
		TuningData data = dataOf(player);
		// 解锁判定：背包里有过对应材料即永久解锁
		boolean roarUnlocked = data.roarUnlocked() || hasItem(player, AbyssResources.ECHO_CRYSTAL);
		boolean aetherUnlocked = data.aetherUnlocked() || hasItem(player, AbyssResources.AETHER_DUST);
		Mode current = data.mode();
		Mode next = switch (current) {
			case SILENT -> roarUnlocked ? Mode.ROARING : Mode.SILENT;
			case ROARING -> aetherUnlocked ? Mode.AETHERLESS : Mode.SILENT;
			case AETHERLESS -> Mode.SILENT;
		};
		save(player, new TuningData(next.id, roarUnlocked, aetherUnlocked));
		player.sendSystemMessage(Component.translatable(
				"message.extra-enchantry.tuning.switch." + next.id));
	}

	/** 声纹倍率（EchoManager.emit 读取） */
	public static float multiplierFor(ServerPlayer player) {
		return modeOf(player).echoMultiplier;
	}

	// ============ 调式被动（§6.6「不同调式影响声纹与被动」） ============

	/**
	 * 轰鸣调式被动：近战伤害 +10%（以更大的声音换取更强的输出）。
	 * LivingEntityMixin 修伤路径不重复查询——由攻击结算处调用。
	 */
	public static float attackBonus(ServerPlayer player) {
		return modeOf(player) == Mode.ROARING ? 1.10F : 1.0F;
	}

	/**
	 * 静默调式被动：潜行移动速度提升 15%（轻声细步）。
	 * LivingEntityMixin 属性修改处调用（0.15/级线性属性修饰）。
	 */
	public static boolean sneakBonus(ServerPlayer player) {
		return modeOf(player) == Mode.SILENT;
	}

	/**
	 * 无相调式被动：免疫深渊凝视（声纹之外的存在，凝视无从凝聚）。
	 * EchoManager 凝视累积处查询。
	 */
	public static boolean gazeImmune(ServerPlayer player) {
		return modeOf(player) == Mode.AETHERLESS;
	}

	private static boolean hasItem(ServerPlayer player, net.minecraft.world.item.Item item) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i).is(item)) {
				return true;
			}
		}
		return false;
	}
}
