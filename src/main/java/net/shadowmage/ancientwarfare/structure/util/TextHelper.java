package net.shadowmage.ancientwarfare.structure.util;

import net.minecraft.locale.Language;

import java.util.IllegalFormatException;

/*
 * Port notes: stands in for the 1.12 I18n.format / net.shadowmage.ancientwarfare.structure.util.TextHelper.translate(Formatted) calls of common code that built
 * tooltip and item name strings by concatenation. Language.getInstance() is the client language on a client and
 * en_us on a dedicated server, which is what the 1.12 calls resolved to.
 */
public class TextHelper {
	private TextHelper() {}

	public static String translate(String key, Object... args) {
		String text = Language.getInstance().getOrDefault(key);
		if (args.length == 0) {
			return text;
		}
		try {
			return String.format(text, args);
		}
		catch (IllegalFormatException e) {
			return "Format error: " + text;
		}
	}

	public static boolean hasKey(String key) {
		return Language.getInstance().has(key);
	}
}
