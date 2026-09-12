package realmikoto.extraenchantry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.StructureManager;

import java.util.Optional;

/**
 * 深渊祭坛（v1.8.0 §3.1）：远古城市最深处的传送门祭所。
 *
 * 结构生成（自定义 Structure，可用 /locate extra-enchantry:abyssal_altar 定位）：
 * - 生成于深暗之域（deep_dark 生物群系，即远古城市所在），埋于城市底面之下
 *   的深板岩中——玩家需向下挖掘穿过被幽匿缝合的"封石层"（§3.1 原文）；
 * - 祭所 = 幽匿门框（中央「会呼吸的黑暗」）+ 八根声纹柱（对应八族）+ 尖啸核心。
 *
 * 三重共鸣仪式（§3.2）的状态机与判定在 {@link AbyssRitual}。
 */
public final class AbyssStructures {

	// ============ 注册 ============

	public static final StructurePieceType ALTAR_PIECE = Registry.register(
			BuiltInRegistries.STRUCTURE_PIECE, ExtraEnchantry.id("abyssal_altar"),
			(StructurePieceSerializationContext context, CompoundTag tag) -> new AbyssalAltarPiece(tag));

	public static final StructureType<AbyssalAltarStructure> ABYSSAL_ALTAR = Registry.register(
			BuiltInRegistries.STRUCTURE_TYPE, ExtraEnchantry.id("abyssal_altar"),
			new StructureType<>() {
				@Override
				public MapCodec<AbyssalAltarStructure> codec() {
					return AbyssalAltarStructure.CODEC;
				}
			});

	public static void register() {
		// 触发静态初始化（piece/type 均在此完成注册）
	}

	private AbyssStructures() {
	}

	// ============ 结构定义 ============

	/** 祭坛地板 Y（远古城市底面之下的封石层之下） */
	public static final int ALTAR_FLOOR_Y = -61;
	/** 祭坛内室半宽（含墙） */
	public static final int ROOM_HALF = 8;
	/** 内室总高（含顶底） */
	public static final int ROOM_HEIGHT = 8;

	/**
	 * 深渊祭坛结构：固定深度埋设，不做地形贴合（深暗之下本就是实心深板岩）。
	 */
	public static final class AbyssalAltarStructure extends Structure {

		public static final MapCodec<AbyssalAltarStructure> CODEC = simpleCodec(AbyssalAltarStructure::new);

		public AbyssalAltarStructure(StructureSettings settings) {
			super(settings);
		}

		@Override
		public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
			// 固定 Y 埋设（不贴地）：远古城市（深暗）之下的封石层下方
			ChunkPos chunkPos = context.chunkPos();
			BlockPos center = new BlockPos(chunkPos.getMiddleBlockX(), ALTAR_FLOOR_Y + 1, chunkPos.getMiddleBlockZ());
			return Optional.of(new GenerationStub(center,
					builder -> generatePieces(builder, context)));
		}

		private static void generatePieces(StructurePiecesBuilder builder, GenerationContext context) {
			ChunkPos chunkPos = context.chunkPos();
			int x = chunkPos.getMinBlockX() + 8 - ROOM_HALF;
			int z = chunkPos.getMinBlockZ() + 8 - ROOM_HALF;
			builder.addPiece(new AbyssalAltarPiece(
					new BoundingBox(x, ALTAR_FLOOR_Y - 1, z,
							x + ROOM_HALF * 2, ALTAR_FLOOR_Y + ROOM_HEIGHT, z + ROOM_HALF * 2),
					Direction.SOUTH));
		}

		@Override
		public StructureType<?> type() {
			return ABYSSAL_ALTAR;
		}
	}

	// ============ 结构部件 ============

	/**
	 * 祭坛部件：房间（深板岩壳）+ 门框（强化深板岩 + 会呼吸的黑暗）
	 * + 八根声纹柱（八族方位）+ 尖啸核心 + 战利品箱。
	 */
	public static final class AbyssalAltarPiece extends StructurePiece {

		AbyssalAltarPiece(BoundingBox box, Direction direction) {
			super(ALTAR_PIECE, 0, box);
			this.setOrientation(direction);
		}

		public AbyssalAltarPiece(CompoundTag tag) {
			super(ALTAR_PIECE, tag);
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			// 无额外数据（布局确定）
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager,
				ChunkGenerator chunkGenerator, RandomSource random, BoundingBox box,
				ChunkPos chunkPos, BlockPos origin) {
			BoundingBox b = this.getBoundingBox();
			int floorY = b.minY() + 1;
			int ceilY = b.maxY() - 1;
			BlockState shell = Blocks.DEEPSLATE.defaultBlockState();
			BlockState polished = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
			BlockState chiseled = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
			BlockState sculk = Blocks.SCULK.defaultBlockState();
			BlockState vein = Blocks.SCULK_VEIN.defaultBlockState();

			// 房间壳：外墙深板岩 + 内衬磨制深板岩；地板/天花在 box 内
			for (int x = b.minX(); x <= b.maxX(); x++) {
				for (int z = b.minZ(); z <= b.maxZ(); z++) {
					for (int y = b.minY(); y <= b.maxY(); y++) {
						boolean isEdge = x == b.minX() || x == b.maxX() || z == b.minZ() || z == b.maxZ();
						boolean isFloor = y == b.minY() || y == floorY;
						boolean isCeil = y == b.maxY() || y == ceilY;
						BlockState state;
						if (isFloor) {
							// 地板：磨制深板岩拼花 + 幽匿纹路
							state = (((x + z) % 4) + 4) % 4 == 0 ? chiseled : polished;
							if ((((x * 7 + z * 13) % 11) + 11) % 11 == 0) {
								state = sculk;
							}
						} else if (isCeil) {
							state = polished;
							if ((((x * 5 + z * 3) % 9) + 9) % 9 == 0) {
								state = vein;
							}
						} else if (isEdge) {
							state = shell;
						} else {
							state = Blocks.CAVE_AIR.defaultBlockState();
						}
						place(level, box, state, x, y, z);
					}
				}
			}

			// 中轴：门框（北侧墙中央）+ 通道缺口
			int cx = (b.minX() + b.maxX()) / 2;
			int cz = (b.minZ() + b.maxZ()) / 2;
			int coreY = floorY + 1;

			// 门框：x = cx..cx+1，y = coreY..coreY+2（2 宽 3 高）——会呼吸的黑暗
			for (int dx = 0; dx <= 1; dx++) {
				for (int dy = 0; dy <= 2; dy++) {
					place(level, box, AbyssBlocks.BREATHING_DARK.defaultBlockState(),
							cx + dx, coreY + dy, b.minZ() + 1);
				}
			}
			// 门框沿（强化深板岩）
			for (int dx = -1; dx <= 2; dx++) {
				place(level, box, Blocks.REINFORCED_DEEPSLATE.defaultBlockState(),
						cx + dx, coreY - 1, b.minZ() + 1);
				place(level, box, Blocks.REINFORCED_DEEPSLATE.defaultBlockState(),
						cx + dx, coreY + 3, b.minZ() + 1);
			}
			for (int dy = 0; dy <= 2; dy++) {
				place(level, box, Blocks.REINFORCED_DEEPSLATE.defaultBlockState(),
						cx - 1, coreY + dy, b.minZ() + 1);
				place(level, box, Blocks.REINFORCED_DEEPSLATE.defaultBlockState(),
						cx + 2, coreY + dy, b.minZ() + 1);
			}
			// 门前平台
			for (int dx = -2; dx <= 3; dx++) {
				for (int dz = 2; dz <= 4; dz++) {
					place(level, box, polished, cx + dx, floorY, b.minZ() + dz);
				}
			}

			// 尖啸核心：房间中央（底层祭台 + 核心本体）
			place(level, box, chiseled, cx, floorY, cz);
			place(level, box, sculk, cx, floorY, cz - 1);
			place(level, box, sculk, cx, floorY, cz + 1);
			place(level, box, AbyssBlocks.SHRIEK_CORE.defaultBlockState(), cx, coreY, cz);

			// 八根声纹柱（§3.1：环绕门框，每根对应一个家族）：核心四周 8 方位
			// 柱身 3 高，柱顶即"凹槽"（交互判定见 AbyssRitual）
			int[][] pillarOffsets = {
					{4, 0}, {3, 3}, {0, 4}, {-3, 3}, {-4, 0}, {-3, -3}, {0, -4}, {3, -3}
			};
			for (int[] off : pillarOffsets) {
				int px = cx + off[0];
				int pz = cz + off[1];
				for (int dy = 0; dy < 3; dy++) {
					place(level, box, AbyssBlocks.RESONANCE_PILLAR.defaultBlockState(),
							px, floorY + 1 + dy, pz);
				}
			}

			// 封石层提示：顶盖留幽匿密集层（挖掘时先见幽匿——"会呼吸"的封石）
			for (int dx = -2; dx <= 3; dx++) {
				for (int dz = -2; dz <= 3; dz++) {
					if (Math.abs(dx) + Math.abs(dz) <= 3 && random.nextInt(2) == 0) {
						place(level, box, sculk, cx + dx, b.maxY(), cz + dz);
					}
				}
			}

			// 两个战利品箱（远古城市遗物）：门两侧
			// （createChest 在 26.2 结构生成期静默失败——改为直接放置 + 绑定战利品表）
			placeLootChest(level, box, random, cx - 2, floorY + 1, cz + 2);
			placeLootChest(level, box, random, cx + 3, floorY + 1, cz + 2);
		}

		/** 直接放置战利品箱并绑定 loot table（替代 StructurePiece#createChest） */
		private void placeLootChest(WorldGenLevel level, BoundingBox clip, RandomSource random,
				int x, int y, int z) {
			BlockPos pos = new BlockPos(x, y, z);
			if (!clip.isInside(pos)) {
				return;
			}
			level.setBlock(pos, Blocks.CHEST.defaultBlockState()
					.setValue(net.minecraft.world.level.block.ChestBlock.FACING,
							net.minecraft.core.Direction.SOUTH), 2);
			if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container) {
				container.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,
						ExtraEnchantry.id("chests/abyssal_altar")));
				container.setLootTableSeed(random.nextLong());
			}
		}

		private void place(WorldGenLevel level, BoundingBox clip, BlockState state, int x, int y, int z) {
			if (clip.isInside(new BlockPos(x, y, z))) {
				level.setBlock(new BlockPos(x, y, z), state, 2);
			}
		}
	}
}
