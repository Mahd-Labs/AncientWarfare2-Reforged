package net.shadowmage.ancientwarfare.npc.registry;

import net.minecraft.util.GsonHelper;
import net.shadowmage.ancientwarfare.AncientWarfare;
import com.google.gson.JsonObject;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.registry.IRegistryDataParser;
import net.shadowmage.ancientwarfare.core.util.parsing.JsonHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TargetRegistry {
	private TargetRegistry() {}

	static final String TARGETS = "targets";
	private static final String LISTS = "lists";
	private static final String INCLUDE = "include";
	private static final String EXCLUDE = "exclude";
	static Map<String, Set<String>> targetLists = new HashMap<>();

	public static Optional<Set<String>> parseTargets(JsonObject json) {
		if (!json.has(TARGETS)) {
			return Optional.empty();
		}

		JsonObject targets = GsonHelper.getAsJsonObject(json, TARGETS);
		Set<String> entitiesToTarget = new HashSet<>();
		if (targets.has(LISTS)) {
			Set<String> lists = JsonHelper.setFromJson(targets.get(LISTS), e -> GsonHelper.convertToString(e, ""));

			for (String list : lists) {
				if (targetLists.containsKey(list)) {
					entitiesToTarget.addAll(targetLists.get(list));
				} else {
					AncientWarfare.LOG.error("Skipping unknown target list - {}", list);
				}
			}
		}

		if (targets.has(INCLUDE)) {
			entitiesToTarget.addAll(JsonHelper.setFromJson(targets.get(INCLUDE), e -> GsonHelper.convertToString(e, "")));
		}

		if (targets.has(EXCLUDE)) {
			entitiesToTarget.removeAll(JsonHelper.setFromJson(targets.get(EXCLUDE), e -> GsonHelper.convertToString(e, "")));
		}

		return Optional.of(entitiesToTarget);
	}

	public static class TargetListParser implements IRegistryDataParser {
		@Override
		public String getName() {
			return "target_lists";
		}

		@SuppressWarnings("squid:S2696")
		@Override
		public void parse(JsonObject json) {
			targetLists = JsonHelper.mapFromJson(json, Map.Entry::getKey, entry -> JsonHelper.setFromJson(entry.getValue(), e -> GsonHelper.convertToString(e, "")));
		}
	}
}
