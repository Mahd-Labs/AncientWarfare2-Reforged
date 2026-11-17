package net.shadowmage.ancientwarfare.core.manual;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.shadowmage.ancientwarfare.core.registry.IRegistryDataParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/*
 * Port notes: client only in 1.12 (registered by the ClientProxy, asked Minecraft for the current language).
 * The data side lives in common code now; the client installs the language supplier (setCurrentLanguage) and the
 * parser is only registered on the client by its entry point. Without a supplier "en_us" is assumed.
 */
public class ManualContentRegistry {
	private ManualContentRegistry() {}

	private static Supplier<String> currentLanguage = () -> "en_us";

	/*
	 * client hook: supplier of the selected language code (e.g. "en_us")
	 */
	public static void setCurrentLanguage(Supplier<String> languageCode) {
		currentLanguage = languageCode;
	}

	private static Map<String, List<IContentElement>> englishCategoryContents = new HashMap<>();
	private static Map<String, List<IContentElement>> categoryContents = new HashMap<>();

	public static List<IContentElement> getCategoryContent(String category) {
		if (categoryContents.containsKey(category)) {
			return categoryContents.get(category);
		} else if (englishCategoryContents.containsKey(category)) {
			return englishCategoryContents.get(category);
		}
		return Collections.emptyList();
	}

	public static void clearContents() {
		englishCategoryContents.clear();
		categoryContents.clear();
	}

	public static class ManualContentParser implements IRegistryDataParser {

		private static final IContentElement EMPTY_ELEMENT = new IContentElement() {};

		@Override
		public String getName() {
			return "manual_content";
		}

		@Override
		public void parse(JsonObject json) {
			String lang = GsonHelper.getAsString(json, "lang").toLowerCase();
			String currentLang = currentLanguage.get();

			if (!lang.equals("en_us") && !lang.equals(currentLang)) {
				return;
			}

			String category = GsonHelper.getAsString(json, "category");
			List<IContentElement> contents = new ArrayList<>();
			if (lang.equals("en_us") && !currentLang.equals(lang)) {
				englishCategoryContents.put(category, contents);
			} else {
				categoryContents.put(category, contents);
			}

			JsonArray elements = GsonHelper.getAsJsonArray(json, "content");

			for (JsonElement el : elements) {
				JsonObject elementJson = GsonHelper.convertToJsonObject(el, "");
				contents.add(parseElement(elementJson));
			}
		}

		private IContentElement parseElement(JsonObject elementJson) {
			String contentType = GsonHelper.getAsString(elementJson, "content_type");

			switch (contentType) {
				case "text":
					return TextElement.parse(elementJson);
				case "heading":
					return HeadingElement.parse(elementJson);
				case "toc_list":
					return TableOfContentsElement.parse(elementJson);
				case "image":
					return ImageElement.parse(elementJson);
				case "item":
					return ItemElement.parse(elementJson);
				default:
					return EMPTY_ELEMENT;
			}
		}
	}
}
