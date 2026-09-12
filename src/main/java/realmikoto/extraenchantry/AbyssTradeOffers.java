package realmikoto.extraenchantry;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 渊息者交易表（1.8.2「群」，1.8.0.3 补齐资源产出）：以物易物。
 *
 * 设计稿 §5.2 资源获取：
 * - **渊息之泪**：渊息者交易 / 潮汐层 → 本表以回响结晶或海晶碎片换取；
 * - **幽匿丝**：幽匿尖塔采集 → 本表以记忆之尘换取（另见幼体掉落）；
 * - **回响结晶**：回声层矿脉 / 声纹湖底 → 矿脉已生成，本表亦可以幽匿块换取。
 *
 * 设计稿 §6.9 探索引导：不可交易时给出**方向性线索**（"往声音消失的地方走"）。
 */
public final class AbyssTradeOffers {

	private AbyssTradeOffers() {
	}

	/**
	 * @return 交易结果（给出物列表）；不可交易的物品返回 null（PASS → 走指引分支）
	 */
	public static List<ItemStack> trade(ItemStack offered) {
		var random = ThreadLocalRandom.current();
		// 幽匿块 ×8 → 回响结晶（回声层矿脉的等价交换；必须先于普通幽匿块分支）
		if (offered.is(Items.SCULK) && offered.getCount() >= 8) {
			return List.of(new ItemStack(AbyssResources.ECHO_CRYSTAL, 1 + random.nextInt(2)));
		}
		// 幽匿块 → 紫水晶（幽渊的"零钱"）
		if (offered.is(Items.SCULK)) {
			return List.of(new ItemStack(Items.AMETHYST_SHARD, 2 + random.nextInt(3)));
		}
		// 回响碎片 → 钻石（碎片是渊族的心跳）
		if (offered.is(Items.ECHO_SHARD)) {
			return List.of(new ItemStack(Items.DIAMOND, 1 + random.nextInt(2)));
		}
		// 紫水晶 ×4 → 铁锭（凡人货币）
		if (offered.is(Items.AMETHYST_SHARD) && offered.getCount() >= 4) {
			return List.of(new ItemStack(Items.IRON_INGOT, 2 + random.nextInt(3)));
		}
		// 回响结晶 → 渊息之泪（渊息者的乡愁：以回声换深海的泪）
		if (offered.is(AbyssResources.ECHO_CRYSTAL)) {
			return List.of(new ItemStack(AbyssResources.TIDE_TEAR, 1));
		}
		// 海晶碎片 ×4 → 渊息之泪（潮汐层的物产）
		if (offered.is(Items.PRISMARINE_SHARD) && offered.getCount() >= 4) {
			return List.of(new ItemStack(AbyssResources.TIDE_TEAR, 1));
		}
		// 记忆之尘 ×2 → 幽匿丝 ×2（记忆织成丝）
		if (offered.is(AbyssResources.MEMORY_DUST) && offered.getCount() >= 2) {
			return List.of(new ItemStack(AbyssResources.SCULK_SILK, 2));
		}
		return null;
	}

	/** 探索引导（设计稿 §6.9）：不可交易时给出方向性线索 */
	public static String guidance(net.minecraft.server.level.ServerPlayer player) {
		int layer = AbyssSpawning.layerOf(player);
		var random = ThreadLocalRandom.current();
		return switch (layer) {
			case 0 -> random.nextBoolean()
					? "§7「往声音消失的地方走——潮汐层的水下有初民的旧路。」"
					: "§7「带着回响结晶来，我用深海的泪跟你换。」";
			case 1 -> random.nextBoolean()
					? "§7「回声层埋着会发光的晶簇，往最吵的地方挖。」"
					: "§7「幽匿的母株在深处——斩断它，整片脉络都会安静。」";
			case 2 -> random.nextBoolean()
					? "§7「记忆层踩在骨上，折忆石里藏着尘。」"
					: "§7「别碰记忆之门，除非你只想看着。」";
			default -> random.nextBoolean()
					? "§7「渊心之上，连我也不敢出声。」"
					: "§7「听到心跳了吗？那不是你的。」";
		};
	}
}
