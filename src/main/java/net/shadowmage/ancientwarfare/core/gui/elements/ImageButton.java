package net.shadowmage.ancientwarfare.core.gui.elements;

import net.minecraft.client.gui.GuiGraphics;
import net.shadowmage.ancientwarfare.core.gui.GuiContainerBase;
import net.shadowmage.ancientwarfare.core.gui.Listener;
import net.shadowmage.ancientwarfare.core.gui.TextureData;
import net.shadowmage.ancientwarfare.core.util.RenderTools;

import java.awt.Color;

public abstract class ImageButton extends GuiElement {
	private final TextureData textureData;
	private final Color colorOutOfFocus;
	private final Color colorInFocus;
	private boolean pressed = false;

	public ImageButton(int topLeftX, int topLeftY, int width, int height, TextureData textureData, Color colorOutOfFocus, Color colorInFocus) {
		super(topLeftX, topLeftY, width, height);
		this.textureData = textureData;
		this.colorOutOfFocus = colorOutOfFocus;
		this.colorInFocus = colorInFocus;

		this.addNewListener(new Listener(Listener.MOUSE_UP) {
			@Override
			public boolean onEvent(GuiElement widget, GuiContainerBase.ActivationEvent evt) {
				if (pressed && enabled && visible && isMouseOverElement(evt.mx, evt.my)) {
					playClickSound();
					onPressed(evt.mButton);
				}
				pressed = false;
				return true;
			}
		});
		this.addNewListener(new Listener(Listener.MOUSE_DOWN) {
			@Override
			public boolean onEvent(GuiElement widget, GuiContainerBase.ActivationEvent evt) {
				if (enabled && visible && isMouseOverElement(evt.mx, evt.my)) {
					pressed = true;
				}
				return true;
			}
		});
	}

	protected abstract void onPressed(int mButton);

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (visible) {
			Color color = isMouseOverElement(mouseX, mouseY) ? colorInFocus : colorOutOfFocus;
			//1.12 passed the 0-255 ints as float colour components, which GL clamped to 1 (see RenderTools class comment)
			graphics.setColor(RenderTools.clamp(color.getRed()), RenderTools.clamp(color.getGreen()), RenderTools.clamp(color.getBlue()), RenderTools.clamp(color.getAlpha()));
			RenderTools.renderQuarteredTexture(graphics, textureData.getTexture(), textureData.getTextureWidth(), textureData.getTextureHeight(), textureData.getTextureU(), textureData.getTextureV(),
					textureData.getPartWidth(), textureData.getPartHeight(), renderX, renderY, width, height);
			graphics.setColor(1, 1, 1, 1);
		}
	}
}
