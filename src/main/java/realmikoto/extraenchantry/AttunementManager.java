package realmikoto.extraenchantry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

import java.util.Locale;

/**
 * 主调铭刻（1.3.0「铭刻与试炼」§2.2）：FULL 家族的唯一主调选择与 5 分钟切换冷却。
 *
 * 规则：
 * <ul>
 *   <li>仅当目标家族处于 FULL 时允许铭刻；同一玩家同时只能有一个主调家族；</li>
 *   <li>铭刻/切换后进入 5 分钟冷却（6000 tick，按服务器游戏时间而非系统时间）；</li>
 *   <li>主调状态持久化（Fabric Data Attachment，{@code copyOnDeath} 原生覆盖
 *       死亡重生与跨维度搬运），旧存档首次加载默认「未铭刻」；</li>
 *   <li>收益只强化 FULL 被动，不提高附魔有效等级（见各被动接入点）；</li>
 *   <li>失去 FULL 时主调选择保留、收益暂停（{@link #attunementActive} 返回 false），
 *       重新达到 FULL 后自动恢复。</li>
 * </ul>
 *
 * 冷却锚点使用主世界 gameTime：该值随存档持久化，重启/重登后剩余冷却依旧正确。
 */
public final class AttunementManager {

	/** 铭刻/切换冷却（tick）：5 分钟 = 6000 tick */
	public static final long COOLDOWN_TICKS = 6000L;

	/**
	 * 主调持久化数据：{@code family} 为空串表示未铭刻；冷却锚定 overworld gameTime。
	 * 全字段 optional：旧存档玩家无 attachment 时初始为 ("" , 0)。
	 */
	public record AttunementData(String family, long cooldownUntilGameTime) {
		public static final Codec<AttunementData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.optionalFieldOf("family", "").forGetter(AttunementData::family),
				Codec.LONG.optionalFieldOf("cooldown_until", 0L).forGetter(AttunementData::cooldownUntilGameTime)
		).apply(instance, AttunementData::new));

		public boolean hasFamily() {
			return family != null && !family.isEmpty();
		}
	}

	/**
	 * 主调 Attachment：persistent Codec 落玩家 NBT（按实体 UUID 存储），
	 * copyOnDeath 保证死亡/重生/跨维度不丢失——直接满足 P0「存量世界迁移」与「死亡不清除」。
	 */
	private static final AttachmentType<AttunementData> ATTUNEMENT =
			AttachmentRegistry.<AttunementData>create(ExtraEnchantry.id("attunement"),
					builder -> builder.persistent(AttunementData.CODEC).copyOnDeath());

	/** 铭刻尝试结果（反馈文案统一由本类发送，调用方只做流程处理） */
	public enum AttuneResult {
		/** 铭刻成功 */
		OK,
		/** 目标家族未达 FULL */
		NOT_FULL,
		/** 冷却中 */
		COOLDOWN,
		/** 该家族已是当前主调 */
		ALREADY_ATTUNED
	}

	private AttunementManager() {
	}

	/** 显式触发静态初始化（onInitialize 早期调用）：主调 Attachment 注册于静态块，
	 *  需早于任何玩家数据读取，否则存量主调记录被当未知 attachment 丢弃。 */
	public static void register() {
	}

	// ============ 查询 API ============

	/** 玩家当前主调家族（未铭刻/数据非法 → null） */
	public static FamilyResonanceManager.Family attunedFamily(ServerPlayer player) {
		AttunementData data = player.getAttached(ATTUNEMENT);
		return data == null ? null : parseFamily(data.family());
	}

	/**
	 * 主调是否生效：选择保留且当前该家族处于 FULL。
	 * 失去 FULL 时收益暂停（返回 false），恢复 FULL 自动恢复——各被动接入点以此为准。
	 */
	public static boolean attunementActive(ServerPlayer player, FamilyResonanceManager.Family family) {
		if (attunedFamily(player) != family) {
			return false;
		}
		return FamilyResonanceManager.tierOf(player, family) == FamilyResonanceManager.Tier.FULL;
	}

	/** 剩余冷却（tick，服务器游戏时间；无冷却返回 0） */
	public static long cooldownRemaining(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return 0L;
		}
		AttunementData data = player.getAttached(ATTUNEMENT);
		if (data == null) {
			return 0L;
		}
		return Math.max(0L, data.cooldownUntilGameTime() - server.overworld().getGameTime());
	}

	// ============ 铭刻流程 ============

	/**
	 * 尝试铭刻主调：FULL 门槛 → 重复检查 → 冷却检查 → 写入并反馈。
	 * 失败原因在此处发送提示，调用方按结果决定后续（如秘典翻页）。
	 */
	public static AttuneResult tryAttune(ServerPlayer player, FamilyResonanceManager.Family family) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return AttuneResult.NOT_FULL;
		}
		if (FamilyResonanceManager.tierOf(player, family) != FamilyResonanceManager.Tier.FULL) {
			player.sendSystemMessage(Component.translatable("message.extra-enchantry.attunement.not_full",
					Component.translatable("family.extra-enchantry." + family.name().toLowerCase(Locale.ROOT))));
			return AttuneResult.NOT_FULL;
		}
		AttunementData data = player.getAttached(ATTUNEMENT);
		if (parseFamily(data == null ? "" : data.family()) == family) {
			player.sendSystemMessage(Component.translatable("message.extra-enchantry.attunement.already",
					Component.translatable("family.extra-enchantry." + family.name().toLowerCase(Locale.ROOT))));
			return AttuneResult.ALREADY_ATTUNED;
		}
		long now = server.overworld().getGameTime();
		if (data != null && now < data.cooldownUntilGameTime()) {
			long remainTicks = data.cooldownUntilGameTime() - now;
			player.sendSystemMessage(Component.translatable("message.extra-enchantry.attunement.cooldown",
					String.format(Locale.ROOT, "%.1f", remainTicks / 20.0D)));
			return AttuneResult.COOLDOWN;
		}
		player.setAttached(ATTUNEMENT, new AttunementData(family.name().toLowerCase(Locale.ROOT), now + COOLDOWN_TICKS));
		// 成功反馈：聊天 + 钟声 + 星辉粒子（与升档反馈同级别的仪式感）
		player.sendSystemMessage(Component.translatable("message.extra-enchantry.attunement.ok",
				Component.translatable("family.extra-enchantry." + family.name().toLowerCase(Locale.ROOT))));
		if (player.level() instanceof ServerLevel serverLevel) {
			FxHelper.play(serverLevel, player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.8F);
			FxHelper.burst(serverLevel, player, ParticleTypes.END_ROD, 12, 0.4D);
		}
		// 首次铭刻：家族格言 + 引导进度（1.3.1「铭文纪元」）
		OnboardingManager.onFirstAttune(player, family);
		// 1.7.0 谱系主线：铭刻之章首节点
		LineageManager.onFirstAttune(player);
		return AttuneResult.OK;
	}

	/** 数据加载/解析辅助：小写家族名 → 枚举（非法值 → null，降级为未铭刻，不崩溃） */
	private static FamilyResonanceManager.Family parseFamily(String name) {
		if (name == null || name.isEmpty()) {
			return null;
		}
		try {
			return FamilyResonanceManager.Family.valueOf(name.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
