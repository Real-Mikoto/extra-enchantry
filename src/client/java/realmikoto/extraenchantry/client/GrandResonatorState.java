package realmikoto.extraenchantry.client;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import realmikoto.extraenchantry.ExtraEnchantry;
import realmikoto.extraenchantry.client.mixin.ClientAdvancementsAccessor;

/**
 * 大共鸣者标记（客户端本地判定，1.3.0 P1）：本地玩家完成
 * 「大共鸣者」进度（family_trials/grand_resonator：集齐八枚家族铭印）
 * 即获得棱彩臻藏资格——本模附魔书与共鸣秘典恒显附魔光效。
 *
 * 与 {@link CollectorState} 同构：进度状态由服务器同步进
 * {@code ClientAdvancements#progress}，进度本身即服务端持久化标记，获得不撤销；
 * 缺少客户端模组时仅视觉降级，服务端逻辑不受影响。
 */
public final class GrandResonatorState {

	private GrandResonatorState() {
	}

	/** 本地玩家是否拥有大共鸣者资格 */
	public static boolean isGrandResonator() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.getConnection() == null) {
			return false;
		}
		ClientAdvancements advancements = minecraft.getConnection().getAdvancements();
		AdvancementHolder holder = advancements.get(ExtraEnchantry.id("family_trials/grand_resonator"));
		if (holder == null) {
			return false;
		}
		AdvancementProgress progress = ((ClientAdvancementsAccessor) advancements).extraenchantry$progress().get(holder);
		return progress != null && progress.isDone();
	}
}
