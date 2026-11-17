package net.shadowmage.ancientwarfare.core.gui.elements;

import net.minecraft.client.gui.GuiGraphics;
import net.shadowmage.ancientwarfare.core.util.RenderTools;

public class Rectangle extends GuiElement {

	private final int color, hoverColor;

	/*
	 * @param color      RGBA color
	 * @param hoverColor RGBA color
	 */
	public Rectangle(int topLeftX, int topLeftY, int width, int height, int color, int hoverColor) {
		super(topLeftX, topLeftY, width, height);
		this.color = color;
		this.hoverColor = hoverColor;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int color = getColor(mouseX, mouseY);
		float r, g, b, a;
		r = (color >> 24) & 255;
		g = (color >> 16) & 255;
		b = (color >> 8) & 255;
		a = (color >> 0) & 255;
		r = (float) r / 255.f;
		g = (float) g / 255.f;
		b = (float) b / 255.f;
		a = (float) a / 255.f;
		RenderTools.renderColoredQuad(graphics, renderX, renderY, renderX + width, renderY + height, r, g, b, a);
	}

	protected int getColor(int mouseX, int mouseY) {
		return isMouseOverElement(mouseX, mouseY) ? hoverColor : color;
	}

}
