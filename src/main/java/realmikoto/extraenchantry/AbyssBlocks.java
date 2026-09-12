package realmikoto.extraenchantry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

/**
 * 幽渊方块注册（v1.8.0.1）。
 */
public final class AbyssBlocks {

	/** 深渊之门：门框内填充体（无碰撞、发光、幽匿粒子） */
	public static final Block ABYSS_PORTAL = new AbyssPortalBlock(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, ExtraEnchantry.id("abyss_portal")))
			.strength(-1.0F)                                  // 爆炸不可毁（同下界门）
			.sound(SoundType.SCULK)
			.noLootTable()
			.lightLevel(state -> 11)                          // 门体自发光
			.instrument(NoteBlockInstrument.BASEDRUM));

	/** 静默之石：静默区源（3×3×3 内声纹归零） */
	public static final Block SILENCE_STONE = new Block(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, ExtraEnchantry.id("silence_stone")))
			.strength(3.5F)
			.sound(SoundType.SCULK)
			.instrument(NoteBlockInstrument.BASEDRUM));

	/** 回声结晶矿（§5.2「回声层矿脉」）：幽渊回声层产出回响结晶 */
	public static final Block ECHO_CRYSTAL_ORE = new Block(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, ExtraEnchantry.id("echo_crystal_ore")))
			.strength(3.0F, 3.0F)
			.sound(SoundType.DEEPSLATE)
			.requiresCorrectToolForDrops()
			.instrument(NoteBlockInstrument.BASEDRUM));

	/** 幽匿母株（§2.2）：幽匿的源头，破坏时反噬脉络 */
	public static final Block SCULK_ROOT_MOTHER = new SculkRootMotherBlock(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, ExtraEnchantry.id("sculk_root_mother")))
			.strength(4.0F)
			.sound(SoundType.SCULK)
			.randomTicks()
			.instrument(NoteBlockInstrument.BASEDRUM));

	/** 记忆折叠方块（§1.4）：在"完好"与"废墟"两态间切换 */
	public static final Block FOLDING_STONE = new FoldingBlock(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, ExtraEnchantry.id("folding_stone")))
			.strength(3.0F)
			.sound(SoundType.DEEPSLATE)
			.randomTicks()
			.instrument(NoteBlockInstrument.BASEDRUM));

	/** 呼吸之门（§4.3）：周期性张合的裂谷闸门 */
	public static final Block BREATHING_GATE = new BreathingGateBlock(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, ExtraEnchantry.id("breathing_gate")))
			.strength(-1.0F)
			.sound(SoundType.SCULK)
			.noLootTable()
			.instrument(NoteBlockInstrument.BASEDRUM));

	/** 记忆之门（§6.4）：右键进入可玩闪回 */
	public static final Block MEMORY_GATE = new MemoryGateBlock(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, ExtraEnchantry.id("memory_gate")))
			.strength(-1.0F)
			.sound(SoundType.SCULK)
			.noLootTable()
			.lightLevel(state -> 7)
			.instrument(NoteBlockInstrument.BASEDRUM));

	// ============ v1.8.0 §3：深渊祭坛三重共鸣仪式方块 ============

	/** 声纹柱（§3.1）：八根家族柱——凹槽嵌入回响碎片 / 按序敲击 */
	public static final Block RESONANCE_PILLAR = new ResonancePillarBlock(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, ExtraEnchantry.id("resonance_pillar")))
			.strength(4.0F)
			.sound(SoundType.DEEPSLATE_TILES)
			.requiresCorrectToolForDrops()
			.instrument(NoteBlockInstrument.BASEDRUM));

	/** 尖啸核心（§3.1）：祭坛中心的巨型幽匿尖啸体——第三重仪式触发 */
	public static final Block SHRIEK_CORE = new ShriekCoreBlock(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, ExtraEnchantry.id("shriek_core")))
			.strength(3.0F)
			.sound(SoundType.SCULK)
			.lightLevel(state -> 5)
			.instrument(NoteBlockInstrument.BASEDRUM));

	/** 会呼吸的黑暗（§3.1）：门框中央的半透明暗幕——仪式完成替换为深渊之门 */
	public static final Block BREATHING_DARK = new BreathingDarkBlock(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, ExtraEnchantry.id("breathing_dark")))
			.strength(-1.0F)
			.sound(SoundType.SCULK)
			.noLootTable()
			.noOcclusion()
			.lightLevel(state -> 1)
			.instrument(NoteBlockInstrument.BASEDRUM));

	/** 回响锚（§3.4）：幽渊侧锚定建筑——门稳定态 + 精确返回的前提 */
	public static final Block ECHO_ANCHOR = new EchoAnchorBlock(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, ExtraEnchantry.id("echo_anchor")))
			.strength(3.0F)
			.sound(SoundType.AMETHYST)
			.lightLevel(state -> 7)
			.instrument(NoteBlockInstrument.BASEDRUM));

	/** 液态幽匿（§4.3/§6.5）：声纹湖与潮汐层水体——无碰撞、踏入生涟漪、浸没消耗氧气 */
	public static final Block LIQUID_SCULK = new LiquidSculkBlock(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, ExtraEnchantry.id("liquid_sculk")))
			.strength(-1.0F)
			.sound(SoundType.SCULK)
			.noLootTable()
			.replaceable()
			.noOcclusion()
			.lightLevel(state -> 2)
			.instrument(NoteBlockInstrument.BASEDRUM));

	/** 无相之尘矿（§5.2/§6.6）：渊心（Y>176）稀有产出「无相之尘」——第九相的实体化来源 */
	public static final Block AETHER_DUST_ORE = new Block(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, ExtraEnchantry.id("aether_dust_ore")))
			.strength(3.0F, 3.0F)
			.sound(SoundType.DEEPSLATE)
			.requiresCorrectToolForDrops()
			.lightLevel(state -> 3)
			.instrument(NoteBlockInstrument.BASEDRUM));

	private AbyssBlocks() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK, ExtraEnchantry.id("abyss_portal"), ABYSS_PORTAL);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("abyss_portal"),
				new BlockItem(ABYSS_PORTAL, new Item.Properties()
						.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("abyss_portal")))
						.rarity(Rarity.EPIC)));
		AbyssGate.PORTAL_BLOCK = ABYSS_PORTAL;

		Registry.register(BuiltInRegistries.BLOCK, ExtraEnchantry.id("echo_crystal_ore"), ECHO_CRYSTAL_ORE);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("echo_crystal_ore"),
				new BlockItem(ECHO_CRYSTAL_ORE, new Item.Properties()
						.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("echo_crystal_ore")))
						.rarity(Rarity.UNCOMMON)));

		Registry.register(BuiltInRegistries.BLOCK, ExtraEnchantry.id("silence_stone"), SILENCE_STONE);
		// 1.8.0.2 新方块（母株 / 折叠 / 呼吸之门 / 记忆之门）
		Registry.register(BuiltInRegistries.BLOCK, ExtraEnchantry.id("sculk_root_mother"), SCULK_ROOT_MOTHER);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("sculk_root_mother"),
				new BlockItem(SCULK_ROOT_MOTHER, new Item.Properties()
						.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("sculk_root_mother")))
						.rarity(Rarity.EPIC)));
		Registry.register(BuiltInRegistries.BLOCK, ExtraEnchantry.id("folding_stone"), FOLDING_STONE);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("folding_stone"),
				new BlockItem(FOLDING_STONE, new Item.Properties()
						.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("folding_stone")))
						.rarity(Rarity.RARE)));
		Registry.register(BuiltInRegistries.BLOCK, ExtraEnchantry.id("breathing_gate"), BREATHING_GATE);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("breathing_gate"),
				new BlockItem(BREATHING_GATE, new Item.Properties()
						.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("breathing_gate")))
						.rarity(Rarity.EPIC)));
		Registry.register(BuiltInRegistries.BLOCK, ExtraEnchantry.id("memory_gate"), MEMORY_GATE);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("memory_gate"),
				new BlockItem(MEMORY_GATE, new Item.Properties()
						.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("memory_gate")))
						.rarity(Rarity.EPIC)));

		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("silence_stone_block"),
				new BlockItem(SILENCE_STONE, new Item.Properties()
						.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("silence_stone_block")))
						.rarity(Rarity.RARE)));

		// v1.8.0 §3：祭坛仪式方块（柱 / 核心 / 回响锚有物品形态；会呼吸的黑暗仅结构方块）
		Registry.register(BuiltInRegistries.BLOCK, ExtraEnchantry.id("resonance_pillar"), RESONANCE_PILLAR);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("resonance_pillar"),
				new BlockItem(RESONANCE_PILLAR, new Item.Properties()
						.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("resonance_pillar")))
						.rarity(Rarity.UNCOMMON)));
		Registry.register(BuiltInRegistries.BLOCK, ExtraEnchantry.id("shriek_core"), SHRIEK_CORE);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("shriek_core"),
				new BlockItem(SHRIEK_CORE, new Item.Properties()
						.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("shriek_core")))
						.rarity(Rarity.EPIC)));
		Registry.register(BuiltInRegistries.BLOCK, ExtraEnchantry.id("breathing_dark"), BREATHING_DARK);
		Registry.register(BuiltInRegistries.BLOCK, ExtraEnchantry.id("echo_anchor"), ECHO_ANCHOR);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("echo_anchor"),
				new BlockItem(ECHO_ANCHOR, new Item.Properties()
						.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("echo_anchor")))
						.rarity(Rarity.RARE)));
		Registry.register(BuiltInRegistries.BLOCK, ExtraEnchantry.id("liquid_sculk"), LIQUID_SCULK);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("liquid_sculk"),
				new BlockItem(LIQUID_SCULK, new Item.Properties()
						.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("liquid_sculk")))
						.rarity(Rarity.EPIC)));
		Registry.register(BuiltInRegistries.BLOCK, ExtraEnchantry.id("aether_dust_ore"), AETHER_DUST_ORE);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("aether_dust_ore"),
				new BlockItem(AETHER_DUST_ORE, new Item.Properties()
						.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("aether_dust_ore")))
						.rarity(Rarity.EPIC)));
	}
}
