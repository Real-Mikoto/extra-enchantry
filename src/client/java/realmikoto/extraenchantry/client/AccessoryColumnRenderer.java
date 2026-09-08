package realmikoto.extraenchantry.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import realmikoto.extraenchantry.Accessories;
import realmikoto.extraenchantry.ExtraEnchantry;

/**
 * 配饰栏集成背包渲染器（1.4.0，纯客户端）。
 *
 * <p>坐标约定（与原版对齐）：槽位逻辑坐标 (x, y) = 16×16 物品渲染区；
 * 18×18 槽框绘制在 (x-1, y-1)；空槽幽灵图标绘制在 (x, y)（与物品同位）。</p>
 *
 * <p>衬底：配饰列 / 滑出按钮列悬浮于面板外（游戏世界上方）时，
 * 以面板色 {@link #PANEL_FILL} 打底（在面板上不可见、在世界处成形），
 * 避免槽列在夜空下呈"漂浮暗块"。</p>
 */
public final class AccessoryColumnRenderer {

	private static final Identifier SLOT_BG = ExtraEnchantry.id("textures/gui/slot_bg.png");
	private static final Identifier BUTTON = ExtraEnchantry.id("textures/gui/accessory_button.png");
	private static final Identifier BUTTON_ACTIVE = ExtraEnchantry.id("textures/gui/accessory_button_active.png");
	private static final Identifier PANEL_FILL = ExtraEnchantry.id("textures/gui/panel_fill.png");
	/** 幽灵图标精灵（gui/sprites/ 目录自动进 GUI 图集，与 getNoItemIcon 同源） */
	private static final Identifier[] GHOSTS = new Identifier[Accessories.SlotType.values().length];

	/** 滑入位移幅度（像素）：从左侧滑入 */
	private static final float SLIDE_DISTANCE = 22.0F;

	static {
		for (Accessories.SlotType slotType : Accessories.SlotType.values()) {
			// blitSprite 走 GUI 图集：sprite id = ghost_<slot>（directory source 按
			// 「prefix + 文件相对路径」生成 id——文件 gui/sprites/ghost_<slot>.png
			// 配 prefix "" 即裸文件名，原版同例 hud/heart/full；与 getNoItemIcon 同源同 id）
			GHOSTS[slotType.ordinal()] = ExtraEnchantry.id("ghost_" + slotType.path);
		}
	}

	private AccessoryColumnRenderer() {
	}

	/** 推进动画（每背景帧） */
	public static void tickAnimation(float partialTick) {
		AccessoryHudState.tick(partialTick);
	}

	// ============ 通用元素 ============

	/** 面板色衬底（相对面板原点；在面板上不可见、在世界处成形） */
	public static void drawBacking(GuiGraphicsExtractor extractor, int left, int top, int x, int y, int w, int h) {
		extractor.blit(RenderPipelines.GUI_TEXTURED, PANEL_FILL, left + x, top + y,
				0.0F, 0.0F, w, h, 64, 128);
	}

	/** 槽框（18×18，绘制在槽位 -1,-1 偏移处，与原版槽框对齐） */
	public static void drawSlotBg(GuiGraphicsExtractor extractor, int left, int top, int x, int y) {
		extractor.blit(RenderPipelines.GUI_TEXTURED, SLOT_BG, left + x - 1, top + y - 1,
				0.0F, 0.0F, 18, 18, 18, 18);
	}

	/** 配饰按钮（18×18 槽框 + 指环图标；绘制在槽位 -1,-1 处，命中区同步 -1） */
	public static void drawButton(GuiGraphicsExtractor extractor, int left, int top, int x, int y) {
		extractor.blit(RenderPipelines.GUI_TEXTURED,
				AccessoryHudState.expanded() ? BUTTON_ACTIVE : BUTTON,
				left + x - 1, top + y - 1, 0.0F, 0.0F, 18, 18, 18, 18);
	}

	/** 按钮命中判定（鼠标屏幕坐标 vs 槽位 -1 的 18×18 区域） */
	public static boolean buttonHit(int left, int top, double mouseX, double mouseY, int x, int y) {
		int dx = (int) Math.round(mouseX) - left - x + 1;
		int dy = (int) Math.round(mouseY) - top - y + 1;
		return dx >= 0 && dx < 18 && dy >= 0 && dy < 18;
	}

	/** 切换展开状态（含 UI 音） */
	public static void toggle() {
		AccessoryHudState.toggle();
		Minecraft.getInstance().getSoundManager().play(
				SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
	}

	// ============ 配饰列（生存单列 / 创造 2×2） ============

	/**
	 * 绘制配饰槽列：衬底 + 槽框 + 空槽幽灵图标 + 动画帧物品。
	 * 仅在动画进行中或已展开时绘制（折叠时整个列不出现）。
	 *
	 * @param backingX/backingY/backingW/backingH 衬底区域（相对面板原点；传 0 高度可跳过）
	 */
	public static void drawColumn(GuiGraphicsExtractor extractor, int left, int top,
			AbstractContainerMenu menu, int[] slotX, int[] slotY,
			int backingX, int backingY, int backingW, int backingH) {
		float progress = AccessoryHudState.progress();
		if (progress <= 0.0F) {
			return;
		}
		float eased = AccessoryHudState.eased();
		int slideOff = Math.round(-SLIDE_DISTANCE * (1.0F - eased));
		boolean animating = progress < 1.0F;

		// 衬底（面板色：面板上不可见、世界上成形）
		if (backingH > 0 && backingW > 0) {
			drawBacking(extractor, left, top, backingX, backingY, backingW, backingH);
		}

		int accessoryBase = menu.slots.size() - Accessories.SlotType.values().length;
		for (int i = 0; i < slotX.length; i++) {
			int x = left + slotX[i] + slideOff;
			int y = top + slotY[i];
			extractor.blit(RenderPipelines.GUI_TEXTURED, SLOT_BG, x - 1, y - 1,
					0.0F, 0.0F, 18, 18, 18, 18);
			ItemStack stack = accessoryStack(menu, accessoryBase + i);
			if (stack.isEmpty()) {
				// 空槽幽灵图标（图集精灵，与真实槽位 getNoItemIcon 同源）
				extractor.blitSprite(RenderPipelines.GUI_TEXTURED, GHOSTS[i], x, y, 16, 16);
			} else if (animating) {
				// 动画帧：物品由本层自绘（动画完成后交还真实槽位渲染）
				extractor.item(stack, x, y);
			}
		}
	}

	/** 读配饰槽内容（菜单末 4 槽） */
	private static ItemStack accessoryStack(AbstractContainerMenu menu, int index) {
		if (index < 0 || index >= menu.slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = menu.slots.get(index);
		return slot == null ? ItemStack.EMPTY : slot.getItem();
	}
}
