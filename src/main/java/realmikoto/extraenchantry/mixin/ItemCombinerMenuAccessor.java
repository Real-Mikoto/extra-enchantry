package realmikoto.extraenchantry.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问器：{@code ItemCombinerMenu#player}（protected final，且声明于父类，
 * AnvilMenuMixin 无法直接 @Shadow——见 README 踩坑记录“@Shadow 只在目标类本类解析”）。
 * 破限门禁（AnvilMenuMixin）用它取当前玩家查「无敌」进度。
 */
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {

	@Accessor("player")
	Player extraenchantry$player();
}
