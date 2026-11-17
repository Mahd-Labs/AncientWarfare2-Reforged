package net.shadowmage.ancientwarfare.core.gui.elements;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.shadowmage.ancientwarfare.core.util.RenderTools;
import org.joml.Matrix4f;

public class Line extends GuiElement {

	int lineWidth;
	int color;
	int x2, y2;

	/*
	 * @param color RGBA color
	 */
	public Line(int topLeftX, int topLeftY, int x2, int y2, int lineWidth, int color) {
		super(topLeftX, topLeftY, x2 - topLeftX, y2 - topLeftY);
		this.color = color;
		this.x2 = x2 - topLeftX;
		this.y2 = y2 - topLeftY;
		this.lineWidth = lineWidth;
	}

	/*
	 * 1.12 drew a GL line of lineWidth * guiScale pixels (= lineWidth gui units). There are no wide GL lines in
	 * the core profile, so the same area is drawn as a quad centered on the line.
	 */
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		float r, g, b, a;
		//kept from 1.12 including the signed shift of the red channel (a red value >= 0x80 turns negative and clamps to 0)
		r = (color & 0xff000000) >> 24;
		g = (color & 0x00ff0000) >> 16;
		b = (color & 0x0000ff00) >> 8;
		a = (color & 0x000000ff) >> 0;
		r = RenderTools.clamp(r / 255.f);
		g = RenderTools.clamp(g / 255.f);
		b = RenderTools.clamp(b / 255.f);
		a = RenderTools.clamp(a / 255.f);

		float len = (float) Math.sqrt((double) x2 * x2 + (double) y2 * y2);
		if (len <= 0) {
			return;
		}
		float hw = lineWidth / 2.f;
		float nx = -y2 / len * hw;
		float ny = x2 / len * hw;

		Matrix4f matrix = graphics.pose().last().pose();
		VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());
		consumer.addVertex(matrix, renderX - nx, renderY - ny, 0).setColor(r, g, b, a);
		consumer.addVertex(matrix, renderX + nx, renderY + ny, 0).setColor(r, g, b, a);
		consumer.addVertex(matrix, renderX + x2 + nx, renderY + y2 + ny, 0).setColor(r, g, b, a);
		consumer.addVertex(matrix, renderX + x2 - nx, renderY + y2 - ny, 0).setColor(r, g, b, a);
		graphics.flush();
	}

}
