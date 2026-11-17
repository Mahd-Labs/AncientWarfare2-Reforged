package net.shadowmage.ancientwarfare.core.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.FormattedText;

public class Label extends GuiElement {

	private String text;
	private boolean renderCentered = false;
	private int color = 0xffffffff;
	private boolean renderShadow;

	public Label(int topLeftX, int topLeftY, String text) {
		super(topLeftX, topLeftY);
		setText(text);
	}

	public Label setRenderCentered() {
		this.renderCentered = true;
		return this;
	}

	public Label setColor(int color) {
		this.color = color;
		return this;
	}

	public Label setShadow(boolean renderShadow) {
		this.renderShadow = renderShadow;
		return this;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (visible) {
			if (renderCentered) {
				drawText(graphics, width / 2);
			} else {
				drawText(graphics, 0);
			}
		}
	}

	private void drawText(GuiGraphics graphics, int offset) {
		if (width < Minecraft.getInstance().font.width(text)) {
			graphics.drawWordWrap(Minecraft.getInstance().font, FormattedText.of(text), renderX - offset, renderY, width, 0xDDDDDD);
		} else {
			if (renderShadow) {
				graphics.drawString(Minecraft.getInstance().font, text, renderX - offset, renderY, color, true);
			} else {
				graphics.drawString(Minecraft.getInstance().font, text, renderX - offset, renderY, color, false);
			}
		}
	}

	public void setText(String text) {
		if (text == null)
			text = "";
		this.text = I18n.get(text);
		this.setTooltipIfFound(text);
		this.width = Minecraft.getInstance().font.width(this.text);
		this.height = 8;
	}

	public String getText() {
		return text;
	}

}
