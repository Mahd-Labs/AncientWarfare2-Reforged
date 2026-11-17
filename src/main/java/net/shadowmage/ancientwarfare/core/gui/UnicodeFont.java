package net.shadowmage.ancientwarfare.core.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/*
 * Replacement for 1.12 ClientProxy.getUnicodeFontRenderer() (a second FontRenderer with the unicode flag forced,
 * used by the manual and TextButton). 1.21.1 has no per-renderer unicode flag; the same glyph set is the
 * "minecraft:uniform" font, selected through the text style. Method names follow the 1.12 FontRenderer.
 *
 *   ClientProxy.getUnicodeFontRenderer().drawString(s, x, y, c)             -> UnicodeFont.drawString(graphics, s, x, y, c)
 *   ClientProxy.getUnicodeFontRenderer().drawSplitString(s, x, y, w, c)     -> UnicodeFont.drawSplitString(graphics, s, x, y, w, c)
 *   ...getStringWidth(s) / getWordWrappedHeight(s, w) / listFormattedStringToWidth(s, w) / FONT_HEIGHT
 */
public final class UnicodeFont {
	private UnicodeFont() {}

	public static final int FONT_HEIGHT = 9;

	private static Font font() {
		return Minecraft.getInstance().font;
	}

	public static Component of(String text) {
		return Component.literal(text).withStyle(style -> style.withFont(Minecraft.UNIFORM_FONT));
	}

	public static int getStringWidth(String text) {
		return font().width(of(text));
	}

	public static int getWordWrappedHeight(String text, int wrapWidth) {
		return FONT_HEIGHT * font().split(of(text), wrapWidth).size();
	}

	/*
	 * Breaks text into lines of at most wrapWidth. Like the 1.12 FontRenderer method the lines are plain strings
	 * and a continuation line starts with the legacy formatting codes that were active at the break.
	 */
	public static List<String> listFormattedStringToWidth(String text, int wrapWidth) {
		List<String> lines = new ArrayList<>();
		Style base = Style.EMPTY.withFont(Minecraft.UNIFORM_FONT);
		font().getSplitter().splitLines(text, wrapWidth, base, false, (style, start, end) -> {
			String line = text.substring(start, end);
			while (line.endsWith("\n")) {
				line = line.substring(0, line.length() - 1);
			}
			lines.add(getFormatCodes(style) + line);
		});
		return lines;
	}

	private static String getFormatCodes(Style style) {
		StringBuilder codes = new StringBuilder();
		TextColor color = style.getColor();
		if (color != null) {
			for (ChatFormatting formatting : ChatFormatting.values()) {
				if (formatting.isColor() && formatting.getColor() != null && formatting.getColor() == color.getValue()) {
					codes.append(formatting);
					break;
				}
			}
		}
		if (style.isBold()) {
			codes.append(ChatFormatting.BOLD);
		}
		if (style.isItalic()) {
			codes.append(ChatFormatting.ITALIC);
		}
		if (style.isUnderlined()) {
			codes.append(ChatFormatting.UNDERLINE);
		}
		if (style.isStrikethrough()) {
			codes.append(ChatFormatting.STRIKETHROUGH);
		}
		if (style.isObfuscated()) {
			codes.append(ChatFormatting.OBFUSCATED);
		}
		return codes.toString();
	}

	public static int drawString(GuiGraphics graphics, String text, int x, int y, int color) {
		return graphics.drawString(font(), of(text), x, y, color, false);
	}

	public static int drawString(GuiGraphics graphics, FormattedCharSequence text, int x, int y, int color) {
		return graphics.drawString(font(), text, x, y, color, false);
	}

	public static void drawSplitString(GuiGraphics graphics, String text, int x, int y, int wrapWidth, int color) {
		for (FormattedCharSequence line : font().split(of(text), wrapWidth)) {
			graphics.drawString(font(), line, x, y, color, false);
			y += FONT_HEIGHT;
		}
	}
}
