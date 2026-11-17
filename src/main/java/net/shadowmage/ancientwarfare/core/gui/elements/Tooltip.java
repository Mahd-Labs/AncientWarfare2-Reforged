package net.shadowmage.ancientwarfare.core.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/*
 * Port note: renderTooltip takes the GuiGraphics as first parameter. The tooltip is drawn at z + 400 (the offset
 * vanilla tooltips use) so that it stays above item models, which 1.12 achieved by disabling the depth test.
 */
public class Tooltip {

	private List<GuiElement> children = new ArrayList<>();
	int width, height;

	/*
	 * actual size of the tooltip is width+8, height+8 (it draws some borders around the internals)
	 */
	public Tooltip(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		//1.12 compared against the display size in real pixels (not gui units); kept
		int screenWidth = Minecraft.getInstance().getWindow().getWidth();
		int screenHeight = Minecraft.getInstance().getWindow().getHeight();
		if (mouseX + this.width > screenWidth) {
			mouseX -= 28 + this.width;
		}
		if (mouseY + this.height + 6 > screenHeight) {
			mouseY = screenHeight - this.height - 6;
		}

		// tooltip needs a right-offset from cursor
		mouseX += 12;

		graphics.enableScissor(mouseX - 4, mouseY - 4, mouseX - 4 + this.width + 8, mouseY - 4 + this.height + 8);

		for (GuiElement element : this.children) {
			element.updateGuiPosition(mouseX, mouseY);
		}

		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 400);
		drawBackground(graphics, mouseX, mouseY);

		for (GuiElement element : this.children) {
			element.render(graphics, -1000, -1000, partialTick);
		}
		graphics.pose().popPose();
		graphics.disableScissor();
	}

	public final void addTooltipElement(GuiElement element) {
		if (this.width < element.width) {
			this.width = element.width + 2;
		}
		if (this.height < element.height) {
			this.height = element.height + 2;
		}
		this.children.add(element);
	}

	private void drawBackground(GuiGraphics graphics, int mouseX, int mouseY) {
		int color1 = -267386864;
		this.drawGradientRect(graphics, mouseX - 3, mouseY - 4, mouseX + this.width + 3, mouseY - 3, color1, color1);
		this.drawGradientRect(graphics, mouseX - 3, mouseY + this.height + 3, mouseX + this.width + 3, mouseY + this.height + 4, color1, color1);
		this.drawGradientRect(graphics, mouseX - 3, mouseY - 3, mouseX + this.width + 3, mouseY + this.height + 3, color1, color1);
		this.drawGradientRect(graphics, mouseX - 4, mouseY - 3, mouseX - 3, mouseY + this.height + 3, color1, color1);
		this.drawGradientRect(graphics, mouseX + this.width + 3, mouseY - 3, mouseX + this.width + 4, mouseY + this.height + 3, color1, color1);
		int color2 = 1347420415;
		int color3 = (color2 & 16711422) >> 1 | color2 & -16777216;
		this.drawGradientRect(graphics, mouseX - 3, mouseY - 3 + 1, mouseX - 3 + 1, mouseY + this.height + 3 - 1, color2, color3);
		this.drawGradientRect(graphics, mouseX + this.width + 2, mouseY - 3 + 1, mouseX + this.width + 3, mouseY + this.height + 3 - 1, color2, color3);
		this.drawGradientRect(graphics, mouseX - 3, mouseY - 3, mouseX + this.width + 3, mouseY - 3 + 1, color2, color2);
		this.drawGradientRect(graphics, mouseX - 3, mouseY + this.height + 2, mouseX + this.width + 3, mouseY + this.height + 3, color3, color3);
	}

	private void drawGradientRect(GuiGraphics graphics, int par1, int par2, int par3, int par4, int par5, int par6) {
		graphics.fillGradient(par1, par2, par3, par4, par5, par6);
	}

}
