package realmikoto.extraenchantry;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 归羽（Homing Plume）返还调度：落空箭矢插地方后延迟 1 秒"飞回"发射者背包。
 *
 * 延迟窗口的两个作用：保留箭矢插地的视觉（不显得箭凭空消失），
 * 并给玩家留出手动拾取的时间——窗口内箭被捡走则实体移除，任务天然作废。
 * 命中过实体的箭不返还（AbstractArrowMixin 已置标记）；DISALLOWED 拾取态
 * （如无限弓的箭）不返还——无限与归羽本就互斥，这里只是兜底。
 */
public final class HomingPlumeManager {

	/** 落地方到返还的延迟（tick）：1 秒 */
	private static final int RETURN_DELAY_TICKS = 20;

	private static final List<Pending> PENDING = new ArrayList<>();

	private HomingPlumeManager() {
	}

	/** 登记一笔延迟返还任务（由 AbstractArrowMixin 在 onHitBlock RETURN 调用） */
	public static void schedule(AbstractArrow arrow, ServerPlayer owner) {
		PENDING.add(new Pending(arrow, owner, RETURN_DELAY_TICKS));
	}

	/** 服务端节拍：倒计时结束且箭仍存在（未被捡走）→ 返还并移除箭 */
	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
			Pending pending = it.next();
			if (pending.arrow.isRemoved() || pending.owner.isRemoved()) {
				it.remove();
				continue;
			}
			if (--pending.ticksLeft > 0) {
				continue;
			}
			it.remove();
			returnArrow(pending);
		}
	}

	/** 执行返还：物品进背包（满了掉在脚下），移除箭实体（提示音已在起飞时播放） */
	private static void returnArrow(Pending pending) {
		ItemStack stack = ((HomingPlumeAccess) pending.arrow).extraenchantry$getPickupItem().copy();
		if (!stack.isEmpty()) {
			if (!pending.owner.getInventory().add(stack)) {
				pending.owner.drop(stack, false);
			}
		}
		pending.arrow.discard();
		// 实战成就「倦鸟归林」：归羽首次自动返手
		Advancements.award(pending.owner, Advancements.RETURN_HOME);
	}

	/** 一笔待返还任务（可变倒计时） */
	private static final class Pending {
		private final AbstractArrow arrow;
		private final ServerPlayer owner;
		private int ticksLeft;

		private Pending(AbstractArrow arrow, ServerPlayer owner, int ticksLeft) {
			this.arrow = arrow;
			this.owner = owner;
			this.ticksLeft = ticksLeft;
		}
	}
}
