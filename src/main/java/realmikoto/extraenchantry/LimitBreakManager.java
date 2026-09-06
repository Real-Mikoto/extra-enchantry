package realmikoto.extraenchantry;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.damagesource.DamageSource;

import java.util.Optional;

/**
 * 破限（Limit Break）附魔书的掉落与隐藏挑战进度。
 *
 * 获取途径（替代原先覆盖监守者战利品表的做法）：
 *   - 监守者被击杀：0.05% 概率掉落 1 级破限附魔书；
 *   - 监守者被【闪电苦力怕】（Creeper#isPowered）击杀：0.5% 概率掉落。
 *   两种概率互斥（闪电苦力怕分支独立判定，不叠加普通分支）。
 *
 * 隐藏挑战进度（方案 B 双分支，均以"玩家拾取到破限附魔书"为判定）：
 *   - 极限之证（obtain_limit_break_book）：拾取任意来源的破限附魔书；
 *   - 雷霆之礼（obtain_limit_break_book_charged）：拾取由闪电苦力怕代杀监守者
 *     而掉落的破限附魔书（该书生成时被打上特殊组件标记，拾取时据此区分来源）。
 *
 * 设计取舍：
 *   - 掉落走 Fabric {@code ServerLivingEntityEvents.AFTER_DEATH}（击杀者信息完整，
 *     能拿到闪电苦力怕 attacker），而非数据驱动战利品表（无法表达"闪电苦力怕击杀"
 *     条件、也无法给掉落物打来源标记）；
 *   - "获得"以 {@code Player#addItem} 为判定（Mixin 注入，见 PlayerMixin），
 *     天然覆盖"闪电苦力怕代杀→玩家捡书"与"亲手击杀→直接进背包"两条路径。
 */
public final class LimitBreakManager {

	/** 监守者被普通击杀的掉率 */
	private static final double NORMAL_DROP_CHANCE = 0.0005D; // 0.05%

	/** 监守者被闪电苦力怕击杀的掉率 */
	private static final double CHARGED_DROP_CHANCE = 0.005D; // 0.5%

	/** 进度判定准则名（advancement 的 criterion） */
	private static final String CRITERION = "extra-enchantry:obtained";

	/** 普通掉落对应的隐藏进度 */
	private static final ResourceKey<net.minecraft.advancements.Advancement> ADVANCE_NORMAL =
			ResourceKey.create(Registries.ADVANCEMENT, ExtraEnchantry.id("hidden_challenges/obtain_limit_break_book"));

	/** 闪电苦力怕代杀对应的隐藏进度 */
	private static final ResourceKey<net.minecraft.advancements.Advancement> ADVANCE_CHARGED =
			ResourceKey.create(Registries.ADVANCEMENT, ExtraEnchantry.id("hidden_challenges/obtain_limit_break_book_charged"));

	private LimitBreakManager() {
	}

	/**
	 * 掉落入口：挂 Fabric {@code ServerLivingEntityEvents.AFTER_DEATH}。
	 * 仅当死亡实体是监守者时判定掉落。
	 */
	public static void onWardenDeath(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof Warden)) {
			return;
		}
		ServerLevel level = (ServerLevel) entity.level();
		boolean chargedKill = isChargedCreeper(source.getEntity());
		double chance = chargedKill ? CHARGED_DROP_CHANCE : NORMAL_DROP_CHANCE;
		if (level.getRandom().nextDouble() >= chance) {
			return;
		}
		ItemStack book = createLimitBreakBook(level, chargedKill);
		entity.spawnAtLocation(level, book);
	}

	/** 判定击杀者是否为闪电苦力怕（原版 Creeper 是 Entity，挂在爆炸/环境伤害时可能是 null） */
	private static boolean isChargedCreeper(net.minecraft.world.entity.Entity attacker) {
		return attacker instanceof Creeper creeper && creeper.isPowered();
	}

	/**
	 * 构造破限附魔书。若为闪电苦力怕代杀来源，额外打一个自定义组件标记
	 * （供拾取时区分进度来源）；普通来源不打标记。
	 */
	private static ItemStack createLimitBreakBook(ServerLevel level, boolean charged) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		Holder<Enchantment> enchantment = level.registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(ExtraEnchantry.LIMIT_BREAK);
		mutable.set(enchantment, 1);
		book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
		if (charged) {
			book.set(ExtraEnchantry.LIMIT_BREAK_SOURCE, true);
		}
		return book;
	}

	/**
	 * 拾取判定入口：挂 {@code Player#addItem}（见 PlayerMixin）。
	 * 玩家获得破限附魔书时授予对应隐藏进度（普通来源→极限之证，闪电苦力怕来源→雷霆之礼）。
	 */
	public static void onItemObtained(ServerPlayer player, ItemStack stack) {
		if (stack.isEmpty() || !stack.is(Items.ENCHANTED_BOOK)) {
			return;
		}
		if (!hasLimitBreakEnchantment(stack)) {
			return;
		}
		ExtraEnchantry.LOGGER.info("[extra-enchantry] 检测到 {} 获得破限附魔书", player.getName().getString());
		boolean chargedSource = Boolean.TRUE.equals(stack.get(ExtraEnchantry.LIMIT_BREAK_SOURCE));
		ResourceKey<net.minecraft.advancements.Advancement> target =
				chargedSource ? ADVANCE_CHARGED : ADVANCE_NORMAL;
		awardAdvancement(player, target);
		// 破限书进度触发：同步召唤僵尸马骑兵队与强化骷髅小队
		// （CavalryManager 内含每玩家刷新保护与和平难度跳过）
		CavalryManager.onLimitBreakBookAchieved(player);
	}

	/** 判断附魔书的存储附魔是否包含破限 */
	private static boolean hasLimitBreakEnchantment(ItemStack stack) {
		ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
		if (stored == null) {
			return false;
		}
		for (Holder<Enchantment> enchantment : stored.keySet()) {
			if (enchantment.is(ExtraEnchantry.LIMIT_BREAK)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 按 ResourceKey 授予进度（幂等：原版 award 已获得时不会重复触发）。
	 * 26.2 的进度不是注册表（ServerAdvancementManager 按 Identifier 查
	 * AdvancementHolder），走 level.registryAccess() 会报 Missing registry。
	 */
	private static void awardAdvancement(ServerPlayer player, ResourceKey<net.minecraft.advancements.Advancement> key) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		net.minecraft.advancements.AdvancementHolder holder = server.getAdvancements().get(key.identifier());
		if (holder != null) {
			player.getAdvancements().award(holder, CRITERION);
		} else {
			ExtraEnchantry.LOGGER.warn("[extra-enchantry] 进度未找到: {}", key.identifier());
		}
	}
}
