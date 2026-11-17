package net.shadowmage.ancientwarfare.core.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.shadowmage.ancientwarfare.core.gui.GuiContainerBase.ActivationEvent;
import net.shadowmage.ancientwarfare.core.gui.Listener;
import net.shadowmage.ancientwarfare.core.interfaces.ITooltipRenderer;
import net.shadowmage.ancientwarfare.core.util.RenderTools;

/*
 * Port note: renderStackSize takes the GuiGraphics as first parameter.
 */
public class ItemSlot extends GuiElement {
	private ItemStack item = ItemStack.EMPTY;
	protected ITooltipRenderer render;
	protected boolean highlightOnMouseOver = true;
	protected boolean renderItemQuantity = true;
	protected boolean renderSlotBackground = true;
	protected boolean renderLabel = false;

	public ItemSlot(int topLeftX, int topLeftY, ItemStack item, ITooltipRenderer render) {
		super(topLeftX - 1, topLeftY - 1, 18, 18);
		this.item = item;
		this.render = render;

		Listener listener = new Listener(Listener.MOUSE_DOWN) {
			@Override
			public boolean onEvent(GuiElement widget, ActivationEvent evt) {
				if (widget.isMouseOverElement(evt.mx, evt.my)) {
					ItemStack stack = Minecraft.getInstance().player.containerMenu.getCarried();
					onSlotClicked(stack, evt.mButton == 1);
				}
				return true;
			}
		};
		addNewListener(listener);
	}

	public void setRenderLabel(boolean val) {
		this.renderLabel = val;
	}

	public void setItem(ItemStack item) {
		this.item = item;
	}

	public ItemSlot setRenderTooltip(boolean val) {
		this.renderTooltip = val;
		return this;
	}

	public ItemSlot setRenderItemQuantity(boolean val) {
		this.renderItemQuantity = val;
		return this;
	}

	public ItemSlot setHighlightOnMouseOver(boolean val) {
		this.highlightOnMouseOver = val;
		return this;
	}

	public ItemSlot setRenderSlotBackground(boolean val) {
		this.renderSlotBackground = val;
		return this;
	}

	public ItemStack getStack() {
		return item;
	}

	/*
	 * 1.12 stack.getItem().getFontRenderer(stack), falling back to the default font
	 */
	static Font getFont(ItemStack stack) {
		Font font = IClientItemExtensions.of(stack).getFont(stack, IClientItemExtensions.FontContext.ITEM_COUNT);
		if (font == null) {
			font = Minecraft.getInstance().font;
		}
		return font;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (visible) {
			if (renderSlotBackground) {
				RenderTools.renderQuarteredTexture(graphics, widgetTexture1, 256, 256, 152, 120, 18, 18, renderX, renderY, width, height);
			}

			if (!this.item.isEmpty()) {
				Font font = getFont(item);

				graphics.renderItem(item, renderX + 1, renderY + 1);
				if (renderItemQuantity && item.getCount() > 1) {
					graphics.renderItemDecorations(font, item, renderX + 1, renderY + 1, "");
					renderStackSize(graphics, renderX + 1, renderY + 1, item.getCount(), font);
				}
				if (renderLabel) {
					int x = renderX + 18;
					int y = renderY + 3;
					graphics.setColor(1.f, 1.f, 1.f, 1.f);
					graphics.drawString(font, item.getHoverName().getString(), x, y, 0xffffffff, true);
				}
			}

			if (isMouseOverElement(mouseX, mouseY)) {
				if (highlightOnMouseOver) {
					/*
					 *  TODO -- find proper alpha for blend..it is close now, but probably not an exact match for vanilla
                     *  highlighting
                     */
					//1.12 drew this blended quad at z + 200 (above the item); the gui overlay render type has no depth test
					RenderTools.renderColoredQuad(graphics, RenderType.guiOverlay(), renderX + 1, renderY + 1, renderX + 1 + (width - 2), renderY + 1 + (height - 2), 1.f, 1.f, 1.f, 0.55f);
				}
				if (renderTooltip && this.render != null) {
					if (this.tooltip != null) {
						this.render.handleElementTooltipRender(tooltip, mouseX, mouseY);
					} else if (!this.item.isEmpty()) {
						this.render.handleItemStackTooltipRender(item, mouseX, mouseY);
					}
				}
			}
			graphics.setColor(1, 1, 1, 1);
		}
	}

	public void renderStackSize(GuiGraphics graphics, int renderX, int renderY, int stackSize, Font fr) {
		graphics.pose().pushPose();
		float ox = renderX + 16, oy = renderY + 8;
		//z 200: above the item model, same offset vanilla uses for item decorations
		graphics.pose().translate(ox + 0.5f, oy + 0.5f, 200);

		String s1 = String.valueOf(stackSize);

		float w = fr.width(s1);
		float scale = stackSize > 99 ? 0.5f : 1.f;
		int oy1 = stackSize > 99 ? 6 : 0;

		graphics.pose().scale(scale, scale, 1.f);

		graphics.drawString(fr, s1, -(int) w, oy1, 16777215, true);

		graphics.pose().popPose();
	}

	public void onSlotClicked(ItemStack stack, boolean rightClicked) {

	}

}
