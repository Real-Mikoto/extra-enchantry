package realmikoto.extraenchantry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 三重共鸣仪式（v1.8.0 §3.2）：深渊祭坛的开启流程——收集 + 解谜 + 风险。
 *
 * <pre>
 * 第一重 · 回响（Echo）：在八根声纹柱的凹槽各嵌入 1 枚回响碎片（共 8 枚）；
 * 第二重 · 旋律（Melody）：按「封印旋律」顺序敲击八柱（线索：家族铭文 /
 *          编年史卷轴 / 记忆残影情报）；敲错 → 全柱熄灭 + 回声惩罚（吸引幽渊生物）；
 * 第三重 · 尖啸（Shriek）：激活尖啸核心 → 召唤监守者（60 秒倒计时）——
 *          玩家须站定在门框内，倒计时结束即开门。
 * </pre>
 *
 * 持久化：SavedData（rituals + 回响锚），门/仪式进度跨重启保留（§3.3/§3.4）。
 * 多人规则（§3.5）：仪式允许多人协作（进度全服共享）；捣乱者敲错柱同样触发回声惩罚。
 */
public final class AbyssRitual {

	// ============ 仪式布局（与 AbyssStructures.AbyssalAltarPiece 对应） ============

	/** 八柱相对核心的 XZ 偏移（下标即柱号；柱顶 y = floorY + 3） */
	public static final int[][] PILLAR_OFFSETS = {
			{4, 0}, {3, 3}, {0, 4}, {-3, 3}, {-4, 0}, {-3, -3}, {0, -4}, {3, -3}
	};

	/**
	 * 封印旋律 = 八柱的正确敲击顺序（下标序）。
	 * 线索对照（对齐家族铭文末句序）：灵魂起调，风暴应和，锋刃劈开，自然扎根；
	 * 守护沉默，风掠其上，火焰低语，流水封印。——与 FamilyResonanceManager.Family 枚举序一致。
	 */
	public static final int[] MELODY = {0, 1, 2, 3, 4, 5, 6, 7};

	/** 核心位置（含维度）唯一键 */
	private static String keyOf(ServerLevel level, BlockPos pos) {
		return level.dimension().identifier() + "@" + pos.asLong();
	}

	// ============ 仪式状态（SavedData 持久化） ============

	public enum Stage {
		SHARDS, MELODY, AWAITING_SHRIEK, SHRIEKING, OPENED
	}

	/** 单座祭坛的仪式进度 */
	public static final class Ritual {
		public Stage stage = Stage.SHARDS;
		/** 八柱嵌入状态 */
		public final boolean[] shards = new boolean[8];
		/** 旋律进度（已正确敲击数） */
		public int melodyProgress = 0;
		/** 尖啸倒计时（tick，仅 SHRIEKING 有效） */
		public int shriekCountdown = 0;
	}

	/** 回响锚记录（归属首个建立的玩家，§3.5） */
	public record Anchor(BlockPos pos, java.util.UUID owner, long placedAt) {
	}

	public static final class AbyssRitualData extends SavedData {
		public final Map<String, Ritual> rituals = new HashMap<>();
		/** 回响锚：key = pos.asLong()（幽渊维度限定） */
		public final Map<Long, Anchor> anchors = new HashMap<>();

		/** 序列化用的仪式快照 */
		private record RitualSnap(String stage, List<Boolean> shards, int melody) {
			static final Codec<RitualSnap> CODEC = RecordCodecBuilder.create(i -> i.group(
					Codec.STRING.optionalFieldOf("stage", "SHARDS").forGetter(RitualSnap::stage),
					Codec.BOOL.listOf().optionalFieldOf("shards", List.of()).forGetter(RitualSnap::shards),
					Codec.INT.optionalFieldOf("melody", 0).forGetter(RitualSnap::melody)
			).apply(i, RitualSnap::new));

			static RitualSnap of(Ritual r) {
				List<Boolean> list = new java.util.ArrayList<>(8);
				for (boolean b : r.shards) {
					list.add(b);
				}
				return new RitualSnap(r.stage.name(), list, r.melodyProgress);
			}

			Ritual toRitual() {
				Ritual r = new Ritual();
				try {
					r.stage = Stage.valueOf(stage);
				} catch (IllegalArgumentException ignored) {
					r.stage = Stage.SHARDS;
				}
				for (int i = 0; i < 8 && i < shards.size(); i++) {
					r.shards[i] = shards.get(i);
				}
				r.melodyProgress = melody;
				return r;
			}
		}

		/** 序列化用的锚点快照（UUID 以字符串存储） */
		private record AnchorSnap(BlockPos pos, String owner) {
			static final Codec<AnchorSnap> CODEC = RecordCodecBuilder.create(i -> i.group(
					BlockPos.CODEC.fieldOf("pos").forGetter(AnchorSnap::pos),
					Codec.STRING.fieldOf("owner").forGetter(AnchorSnap::owner)
			).apply(i, AnchorSnap::new));

			Anchor toAnchor() {
				try {
					return new Anchor(pos, java.util.UUID.fromString(owner), 0L);
				} catch (IllegalArgumentException e) {
					return new Anchor(pos, new java.util.UUID(0L, 0L), 0L);
				}
			}
		}

		public static final Codec<AbyssRitualData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.unboundedMap(Codec.STRING, RitualSnap.CODEC).optionalFieldOf("rituals", Map.of())
						.forGetter(d -> {
							Map<String, RitualSnap> out = new HashMap<>();
							d.rituals.forEach((k, v) -> out.put(k, RitualSnap.of(v)));
							return out;
						}),
				AnchorSnap.CODEC.listOf().optionalFieldOf("anchors", List.of())
						.forGetter(d -> {
							List<AnchorSnap> out = new java.util.ArrayList<>();
							d.anchors.values().forEach(a -> out.add(new AnchorSnap(a.pos(), a.owner().toString())));
							return out;
						})
		).apply(instance, (Map<String, RitualSnap> rituals, List<AnchorSnap> anchors) -> {
			AbyssRitualData data = new AbyssRitualData();
			rituals.forEach((k, v) -> data.rituals.put(k, v.toRitual()));
			anchors.forEach(a -> data.anchors.put(a.pos.asLong(), a.toAnchor()));
			return data;
		}));
	}

	public static final SavedDataType<AbyssRitualData> DATA_TYPE = new SavedDataType<>(
			ExtraEnchantry.id("abyss_ritual"),
			AbyssRitualData::new,
			AbyssRitualData.CODEC,
			(DataFixTypes) null);

	private static AbyssRitualData data(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(DATA_TYPE);
	}

	// ============ 交互：回响碎片嵌入（第一重） ============

	/**
	 * 声纹柱被右键。
	 *
	 * @return true = 本次为嵌入且已接受（调用方应消耗 1 枚回响碎片）
	 */
	public static boolean onPillarUse(ServerLevel level, BlockPos pillarPos, ServerPlayer player,
			ItemStack held) {
		Optional<CoreContext> core = findCore(level, pillarPos);
		if (core.isEmpty()) {
			return false;
		}
		CoreContext ctx = core.get();
		int pillarIndex = pillarIndex(ctx.corePos(), pillarPos);
		if (pillarIndex < 0) {
			return false;
		}
		AbyssRitualData data = data(level);
		String key = keyOf(level, ctx.corePos());
		Ritual ritual = data.rituals.computeIfAbsent(key, k -> new Ritual());

		boolean consumed;
		if (held.is(net.minecraft.world.item.Items.ECHO_SHARD)) {
			consumed = embedShard(level, ritual, pillarIndex, pillarPos, player);
		} else {
			strikePillar(level, ctx, ritual, pillarIndex, pillarPos, player);
			consumed = false;
		}
		data.setDirty();
		return consumed;
	}

	/** 第一重 · 回响：嵌入回响碎片（返回是否接受嵌入） */
	private static boolean embedShard(ServerLevel level, Ritual ritual, int index, BlockPos pillarPos,
			ServerPlayer player) {
		if (ritual.stage != Stage.SHARDS) {
			player.sendSystemMessage(Component.translatable("message.extra-enchantry.ritual.shards_done"));
			return false;
		}
		if (ritual.shards[index]) {
			player.sendSystemMessage(Component.translatable("message.extra-enchantry.ritual.shard_dup"));
			return false;
		}
		ritual.shards[index] = true;
		int remaining = 0;
		for (boolean b : ritual.shards) {
			if (!b) {
				remaining++;
			}
		}
		FxHelper.burstAt(level, pillarPos.getX() + 0.5, pillarPos.getY() + 1.2, pillarPos.getZ() + 0.5,
				ParticleTypes.END_ROD, 10, 0.3);
		FxHelper.play(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.7F);
		if (remaining == 0) {
			ritual.stage = Stage.MELODY;
			broadcastRitual(level, "message.extra-enchantry.ritual.stage2_hint");
		} else {
			player.sendSystemMessage(Component.translatable(
					"message.extra-enchantry.ritual.shard_embedded", remaining));
		}
		return true;
	}

	/** 第二重 · 旋律：按序敲击八柱（敲错 = 全柱熄灭 + 回声惩罚） */
	private static void strikePillar(ServerLevel level, CoreContext ctx, Ritual ritual,
			int index, BlockPos pillarPos, ServerPlayer player) {
		switch (ritual.stage) {
			case SHARDS -> player.sendSystemMessage(Component.translatable(
					"message.extra-enchantry.ritual.need_shards"));
			case MELODY -> {
				if (index == MELODY[ritual.melodyProgress]) {
					ritual.melodyProgress++;
					FxHelper.play(level, player, SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F,
							0.6F + ritual.melodyProgress * 0.08F);
					FxHelper.burstAt(level, pillarPos.getX() + 0.5, pillarPos.getY() + 1.2,
							pillarPos.getZ() + 0.5, ParticleTypes.NOTE, 4, 0.2);
					if (ritual.melodyProgress >= MELODY.length) {
						ritual.stage = Stage.AWAITING_SHRIEK;
						broadcastRitual(level, "message.extra-enchantry.ritual.stage3_hint");
					}
				} else {
					// 敲错：全柱熄灭、重置，并触发一次回声惩罚（§3.2）
					ritual.melodyProgress = 0;
					player.sendSystemMessage(Component.translatable("message.extra-enchantry.ritual.wrong_note"));
					broadcastRitual(level, "message.extra-enchantry.ritual.wrong_note_all");
					echoPunishment(level, ctx.corePos());
				}
			}
			default -> {
			}
		}
	}

	/** 回声惩罚（§3.2）：一圈声纹扩散，吸引幽渊生物 */
	private static void echoPunishment(ServerLevel level, BlockPos corePos) {
		double cx = corePos.getX() + 0.5;
		double cy = corePos.getY() + 1.0;
		double cz = corePos.getZ() + 0.5;
		EchoManager.emitAt(level, cx, cy, cz, 24.0F);
		FxHelper.ringAt(level, cx, cy, cz, 16.0D, ParticleTypes.SONIC_BOOM, 24);
		FxHelper.playAt(level, corePos, SoundEvents.WARDEN_SONIC_BOOM, 1.2F, 1.2F);
		// 吸引：惊动半径内的怪物（显形——声纹即广播）
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
				new net.minecraft.world.phys.AABB(corePos).inflate(32.0D),
				e -> e.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER)) {
			e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, true));
		}
	}

	// ============ 交互：尖啸核心（第三重） ============

	/** 尖啸核心被右键 */
	public static void onCoreUse(ServerLevel level, BlockPos corePos, ServerPlayer player) {
		AbyssRitualData data = data(level);
		String key = keyOf(level, corePos);
		Ritual ritual = data.rituals.computeIfAbsent(key, k -> new Ritual());

		switch (ritual.stage) {
			case AWAITING_SHRIEK -> {
				ritual.stage = Stage.SHRIEKING;
				ritual.shriekCountdown = 20 * 60;
				// 全维度级别的「深渊回响」：监守者被召唤而来（§3.2）
				summonWarden(level, corePos);
				broadcastRitual(level, "message.extra-enchantry.ritual.shriek_start");
				FxHelper.playAt(level, corePos, SoundEvents.WARDEN_SONIC_CHARGE, 1.5F, 0.5F);
				FxHelper.ringAt(level, corePos.getX() + 0.5, corePos.getY() + 1.0,
						corePos.getZ() + 0.5, 24.0D, ParticleTypes.SONIC_BOOM, 32);
			}
			case SHRIEKING -> player.sendSystemMessage(Component.translatable(
					"message.extra-enchantry.ritual.shriek_counting",
					ritual.shriekCountdown / 20));
			case OPENED -> player.sendSystemMessage(Component.translatable(
					"message.extra-enchantry.ritual.already_open"));
			default -> player.sendSystemMessage(Component.translatable(
					"message.extra-enchantry.ritual.core_dormant"));
		}
		data.setDirty();
	}

	/** 召唤监守者（距核心 ~24 格）——仪式风险（§3.2） */
	private static void summonWarden(ServerLevel level, BlockPos corePos) {
		java.util.Random r = new java.util.Random();
		double angle = r.nextDouble() * Math.PI * 2;
		double dist = 20 + r.nextDouble() * 8;
		BlockPos spawn = corePos.offset((int) (Math.cos(angle) * dist), 0, (int) (Math.sin(angle) * dist));
		Warden warden = net.minecraft.world.entity.EntityTypes.WARDEN.create(level, EntitySpawnReason.TRIGGERED);
		if (warden != null) {
			warden.snapTo(spawn.getX() + 0.5, corePos.getY() + 4, spawn.getZ() + 0.5, 0.0F, 0.0F);
			level.addFreshEntity(warden);
		}
	}

	// ============ 服务器 tick：尖啸倒计时推进（ExtraEnchantry 注册） ============

	public static void tickServer(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		AbyssRitualData data = overworld.getDataStorage().computeIfAbsent(DATA_TYPE);
		boolean dirty = false;
		for (Map.Entry<String, Ritual> entry : data.rituals.entrySet()) {
			Ritual ritual = entry.getValue();
			if (ritual.stage != Stage.SHRIEKING) {
				continue;
			}
			ritual.shriekCountdown -= 20;
			dirty = true;
			ServerLevel level = server.getLevel(parseDimension(entry.getKey()));
			if (level == null) {
				continue;
			}
			BlockPos corePos = parseCorePos(entry.getKey());
			// 每 10 秒播报倒计时
			if (ritual.shriekCountdown > 0 && ritual.shriekCountdown % 200 == 0) {
				broadcastRitual(level, "message.extra-enchantry.ritual.shriek_count",
						ritual.shriekCountdown / 20);
			}
			if (ritual.shriekCountdown <= 0) {
				// 倒计时结束：玩家须站定在门框内（§3.2「完成最后一步」）
				Optional<BlockPos> gateCenter = findGateCenter(level, corePos);
				boolean playerInFrame = gateCenter.isPresent() && level.players().stream()
						.anyMatch(p -> p.blockPosition().closerThan(gateCenter.get(), 4));
				if (playerInFrame) {
					openGate(level, gateCenter.get());
					ritual.stage = Stage.OPENED;
					broadcastRitual(level, "message.extra-enchantry.ritual.opened");
				} else {
					ritual.stage = Stage.AWAITING_SHRIEK;
					broadcastRitual(level, "message.extra-enchantry.ritual.shriek_faded");
				}
			}
		}
		if (dirty) {
			data.setDirty();
		}
	}

	/** 开门：会呼吸的黑暗 → 深渊之门 + 门状态机点火 */
	private static void openGate(ServerLevel level, BlockPos gateCenter) {
		// 替换门框内的会呼吸的黑暗为深渊之门
		for (BlockPos p : BlockPos.withinManhattan(gateCenter, 8, 8, 8)) {
			BlockState state = level.getBlockState(p);
			if (state.is(AbyssBlocks.BREATHING_DARK)) {
				level.setBlock(p, AbyssGate.PORTAL_BLOCK.defaultBlockState(), 3);
			}
		}
		// 门状态机：直接进入待唤醒（快速开启）
		AbyssGateState.ignite(level, gateCenter);
		FxHelper.playAt(level, gateCenter, SoundEvents.END_PORTAL_SPAWN, 1.0F, 0.6F);
	}

	// ============ 回响锚（§3.4） ============

	/** 放置回响锚（EchoAnchorBlock.onPlace 调用）：注册锚点 → 门进入稳定态 */
	public static void registerAnchor(ServerLevel level, BlockPos pos, java.util.UUID owner) {
		if (level.dimension() != AbyssKey.ABYSS) {
			return;
		}
		AbyssRitualData data = data(level);
		Anchor anchor = new Anchor(pos.immutable(), owner, level.getGameTime());
		if (data.anchors.putIfAbsent(pos.asLong(), anchor) == null) {
			data.setDirty();
			// 进度：「在深渊建立自己的门」
			var holder = level.getServer().getAdvancements().get(
					Identifier.fromNamespaceAndPath(ExtraEnchantry.MOD_ID, "hollow/anchor"));
			ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(owner);
			if (holder != null && ownerPlayer != null) {
				ownerPlayer.getAdvancements().award(holder, "anchor");
			}
			// 锚点归属提示（§3.5：归属首个建立的玩家）
			for (ServerPlayer p : level.players()) {
				if (p.blockPosition().closerThan(pos, 48)) {
					p.sendSystemMessage(Component.translatable("message.extra-enchantry.anchor.placed",
							ownerPlayer != null ? ownerPlayer.getName() : Component.literal("?")));
				}
			}
		}
	}

	public static boolean hasAnyAnchor(ServerLevel anyLevel) {
		return !data(anyLevel).anchors.isEmpty();
	}

	public static BlockPos nearestAnchor(ServerLevel abyss, double x, double z) {
		AbyssRitualData data = data(abyss);
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		for (Anchor a : data.anchors.values()) {
			double d = (a.pos().getX() - x) * (a.pos().getX() - x)
					+ (a.pos().getZ() - z) * (a.pos().getZ() - z);
			if (d < bestDist) {
				bestDist = d;
				best = a.pos();
			}
		}
		return best;
	}

	// ============ 工具 ============

	private record CoreContext(BlockPos corePos) {
	}

	/** 从柱位置找核心（半径 12 内唯一尖啸核心） */
	private static Optional<CoreContext> findCore(ServerLevel level, BlockPos pillarPos) {
		for (BlockPos p : BlockPos.withinManhattan(pillarPos, 12, 8, 12)) {
			if (level.getBlockState(p).is(AbyssBlocks.SHRIEK_CORE)) {
				return Optional.of(new CoreContext(p.immutable()));
			}
		}
		return Optional.empty();
	}

	/** 柱号（按 PILLAR_OFFSETS 的 XZ 偏移匹配；-1 = 不在柱位） */
	private static int pillarIndex(BlockPos corePos, BlockPos pillarPos) {
		int dx = pillarPos.getX() - corePos.getX();
		int dz = pillarPos.getZ() - corePos.getZ();
		for (int i = 0; i < PILLAR_OFFSETS.length; i++) {
			if (PILLAR_OFFSETS[i][0] == dx && PILLAR_OFFSETS[i][1] == dz) {
				return i;
			}
		}
		return -1;
	}

	/** 找门框中心（核心周围 12 格内的会呼吸的黑暗群中心） */
	private static Optional<BlockPos> findGateCenter(ServerLevel level, BlockPos corePos) {
		for (BlockPos p : BlockPos.withinManhattan(corePos, 12, 6, 12)) {
			if (level.getBlockState(p).is(AbyssBlocks.BREATHING_DARK)) {
				return Optional.of(p.immutable());
			}
		}
		return Optional.empty();
	}

	private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> parseDimension(String key) {
		String dim = key.substring(0, key.indexOf('@'));
		return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
				Identifier.parse(dim));
	}

	private static BlockPos parseCorePos(String key) {
		return BlockPos.of(Long.parseLong(key.substring(key.indexOf('@') + 1)));
	}

	private static void broadcastRitual(ServerLevel level, String key, Object... args) {
		for (ServerPlayer p : level.players()) {
			p.sendSystemMessage(Component.translatable(key, args));
		}
	}

	/** 登出清理无（全持久化）；服务器停止无（SavedData 自管） */
	public static void register() {
		// 触发静态初始化（SavedDataType 注册为惰性——无静态注册表，无需处理）
	}
}
