package net.shadowmage.ancientwarfare.core.gui.manual.elements;

import com.google.common.collect.ImmutableList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.shadowmage.ancientwarfare.core.gui.UnicodeFont;
import net.shadowmage.ancientwarfare.core.gui.manual.GuiManual;
import net.shadowmage.ancientwarfare.core.gui.manual.IElementWrapperCreator;
import net.shadowmage.ancientwarfare.core.manual.HeadingElement;

import java.util.List;

public class HeadingElementWrapper extends BaseElementWrapper<HeadingElement> {

	private static final int TOP_PADDING = 3;
	private static final int BOTTOM_PADDING = 3;

	public HeadingElementWrapper(GuiManual gui, int topLeftY, int width, int height, HeadingElement element) {
		super(gui, 0, topLeftY, width, height, element);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		ChatFormatting prefix = getElement().getLevel() == 1 ? ChatFormatting.BOLD : ChatFormatting.UNDERLINE;
		UnicodeFont.drawSplitString(graphics, prefix + getElement().getText() + ChatFormatting.RESET, renderX, renderY + TOP_PADDING, width, 0x000000);
	}

	@Override
	public int getHeight() {
		return super.getHeight() + TOP_PADDING + BOTTOM_PADDING;
	}

	@Override
	public boolean shouldKeepWithNext() {
		return true;
	}

	public static class Creator implements IElementWrapperCreator<HeadingElement> {
		@Override
		public List<BaseElementWrapper<HeadingElement>> construct(GuiManual gui, int topLeftY, int width, int remainingPageHeight, int emptyPageHeight, HeadingElement element) {
			int textHeight = UnicodeFont.getWordWrappedHeight(element.getText(), width);
			return ImmutableList.of(new HeadingElementWrapper(gui, topLeftY, width, textHeight + TOP_PADDING + BOTTOM_PADDING, element));
		}
	}
}
