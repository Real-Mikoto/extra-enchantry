package realmikoto.extraenchantry.client.mixin;

import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * 访问器：{@code ClientAdvancements#progress}（私有进度表，随服务器同步更新）。
 * 锁定视觉用：本地玩家未完成「无敌」进度时，破限附魔名按锁定态渲染。
 */
@Mixin(ClientAdvancements.class)
public interface ClientAdvancementsAccessor {

	@Accessor("progress")
	Map<net.minecraft.advancements.AdvancementHolder, AdvancementProgress> extraenchantry$progress();
}
