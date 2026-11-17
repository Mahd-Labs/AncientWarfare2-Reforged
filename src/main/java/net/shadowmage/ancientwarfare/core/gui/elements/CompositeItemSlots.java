package net.shadowmage.ancientwarfare.core.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.gui.GuiContainerBase;
import net.shadowmage.ancientwarfare.core.interfaces.ITooltipRenderer;
import net.shadowmage.ancientwarfare.core.util.RenderTools;

import java.util.ArrayList;
import java.util.List;

public class CompositeItemSlots extends CompositeScrolled {
	private List<ItemSlot> itemSlots = new ArrayList<>();
	ITooltipRenderer render;

	public CompositeItemSlots(GuiContainerBase gui, int topLeftX, int topLeftY, int width, int height, ITooltipRenderer render) {
		super(gui, topLeftX, topLeftY, width, height);
		this.render = render;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (!isMouseOverElement(mouseX, mouseY)) {
			mouseX = Integer.MIN_VALUE;
			mouseY = Integer.MIN_VALUE;
		}
		//render background before setting viewport so that it is not cropped
		RenderTools.renderQuarteredTexture(graphics, backgroundTextureLocation, 256, 256, 0, 0, 256, 240, renderX, renderY, width, height);
		setViewport(graphics);
		for (GuiElement element : this.elements) {
			if (element.renderY > renderY + height || element.renderY + element.height < renderY
					|| element.renderX > renderX + width || element.renderX + element.width < renderX) {
				continue;
			}

			if (element instanceof ItemSlot) {
				itemSlots.add((ItemSlot) element);
			} else {
				element.render(graphics, mouseX, mouseY, partialTick);
			}
		}

		graphics.setColor(1.f, 1.f, 1.f, 1.f);
		for (ItemSlot slot : itemSlots) {
			renderSlotBackground(graphics, slot);
		}

		for (ItemSlot slot : itemSlots) {
			renderItemStack(graphics, slot);
		}

		for (ItemSlot slot : itemSlots) {
			renderOverlay(graphics, slot);
		}

		//draw white quad @ alpha for highlight
		for (ItemSlot slot : itemSlots) {
			renderSlotHighlight(graphics, slot, mouseX, mouseY);
		}
		//reset renderabled and render-viewport
		itemSlots.clear();
		popViewport(graphics);

		scrollbar.render(graphics, mouseX, mouseY, partialTick);
	}

	private void renderSlotBackground(GuiGraphics graphics, ItemSlot slot) {
		if (slot.visible && slot.renderSlotBackground) {
			RenderTools.renderQuarteredTexture(graphics, widgetTexture1, 256, 256, 152, 120, 18, 18, slot.renderX, slot.renderY, slot.width, slot.height);
		}
	}

	private void renderSlotHighlight(GuiGraphics graphics, ItemSlot slot, int mouseX, int mouseY) {
		ItemStack stack = slot.getStack();
		if (stack.isEmpty() || !slot.visible) {
			return;
		}
		if (slot.highlightOnMouseOver && slot.isMouseOverElement(mouseX, mouseY)) {
			//1.12: depth test off, blended white quad at 50% alpha over the whole slot
			RenderTools.renderColoredQuad(graphics, RenderType.guiOverlay(), slot.renderX, slot.renderY, (float) slot.renderX + slot.width, (float) slot.renderY + slot.height, 1.f, 1.f, 1.f, 0.5f);
			if (slot.renderTooltip && !slot.getStack().isEmpty() && render != null) {
				if (slot.tooltip != null) {
					this.render.handleElementTooltipRender(slot.tooltip, mouseX, mouseY);
				} else {
					this.render.handleItemStackTooltipRender(slot.getStack(), mouseX, mouseY);
				}
			}
		}
	}

	private void renderOverlay(GuiGraphics graphics, ItemSlot slot) {
		ItemStack stack = slot.getStack();
		if (stack.isEmpty() || !slot.visible) {
			return;
		}
		Font font = ItemSlot.getFont(stack);
		graphics.renderItemDecorations(font, stack, slot.renderX + 1, slot.renderY + 1, "");
		if (slot.renderItemQuantity && slot.getStack().getCount() > 1) {
			slot.renderStackSize(graphics, slot.renderX + 1, slot.renderY + 1, stack.getCount(), font);
		}
	}

	private void renderItemStack(GuiGraphics graphics, ItemSlot slot) {
		ItemStack stack = slot.getStack();
		if (stack.isEmpty() || !slot.visible) {
			return;
		}
		graphics.renderItem(stack, slot.renderX + 1, slot.renderY + 1);
	}

}
