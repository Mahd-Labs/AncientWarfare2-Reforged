package net.shadowmage.ancientwarfare.npc.registry;

import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.GsonHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.shadowmage.ancientwarfare.core.registry.IRegistryDataParser;
import net.shadowmage.ancientwarfare.core.util.parsing.JsonHelper;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class FactionRegistry {
	private FactionRegistry() {}

	private static final Map<String, FactionDefinition> factions = new HashMap<>();

	public static Set<String> getFactionNames() {
		return factions.keySet();
	}

	public static FactionDefinition getFaction(String name) {
		return factions.getOrDefault(name, EMPTY_FACTION);
	}

	public static Collection<FactionDefinition> getFactionDefinitions() {
		return factions.values();
	}

	public static class FactionParser implements IRegistryDataParser {
		private static final String PLAYER_DEFAULT_STANDING = "player_default_standing";
		private static final String STANDING_CAN_CHANGE = "standing_can_change";
		private static final String HOSTILE_TOWARDS_FACTIONS = "hostile_towards_factions";
		private static final String THEMED_BLOCKS = "themed_blocks";
		private static final String STANDING_CHANGES = "standing_changes";
		private static final String STANDING_SETTINGS = "standing_settings";

		@Override
		public String getName() {
			return "factions";
		}

		@Override
		public void parse(JsonObject json) {
			JsonObject defaults = GsonHelper.getAsJsonObject(json, "defaults");
			FactionDefinition defaultDefinition = new FactionDefinition(
					parseStandingSettings(defaults),
					parseHostileTowards(defaults).entrySet().stream().filter(Entry::getValue).map(Entry::getKey).collect(Collectors.toCollection(HashSet::new)),
					TargetRegistry.parseTargets(defaults).orElse(new HashSet<>()));

			JsonArray factionsArray = GsonHelper.getAsJsonArray(json, "factions");

			for (JsonElement e : factionsArray) {
				JsonObject faction = GsonHelper.convertToJsonObject(e, "faction");
				String factionName = GsonHelper.getAsString(faction, "name");
				FactionDefinition.CopyBuilder builder = defaultDefinition.copy(factionName, Integer.parseInt(GsonHelper.getAsString(faction, "color"), 16));
				if (faction.has(STANDING_SETTINGS)) {
					overrideStandingSettings(faction, builder);
				}
				builder.removeHostileTowards(factionName);
				if (faction.has(HOSTILE_TOWARDS_FACTIONS)) {
					Map<String, Boolean> hostileTowards = parseHostileTowards(faction);
					hostileTowards.entrySet().stream().filter(Entry::getValue).map(Entry::getKey).forEach(builder::addHostileTowards);
					hostileTowards.entrySet().stream().filter(entry -> !entry.getValue()).map(Entry::getKey).forEach(builder::removeHostileTowards);
				}

				TargetRegistry.parseTargets(faction).ifPresent(builder::overrideTargetList);

				if (faction.has(THEMED_BLOCKS)) {
					builder.overrideThemedBlocksTags(parseThemedBLocks(faction, factionName));
				}

				factions.put(factionName, builder.build());
			}
		}

		private void overrideStandingSettings(JsonObject faction, FactionDefinition.CopyBuilder builder) {
			JsonObject standingSettingsJson = GsonHelper.getAsJsonObject(faction, STANDING_SETTINGS);
			builder.setStandingSettings(standingSettings -> {
				if (standingSettingsJson.has(PLAYER_DEFAULT_STANDING)) {
					standingSettings.setPlayerDefaultStanding(GsonHelper.getAsInt(standingSettingsJson, PLAYER_DEFAULT_STANDING));
				}
				if (standingSettingsJson.has(STANDING_CAN_CHANGE)) {
					standingSettings.setStandingCanChange(GsonHelper.getAsBoolean(standingSettingsJson, STANDING_CAN_CHANGE));
				}
				if (standingSettingsJson.has(STANDING_CHANGES)) {
					standingSettings.overrideStandingChanges(parseStandingChanges(standingSettingsJson));
				}
			});
		}

		private FactionDefinition.StandingSettings parseStandingSettings(JsonObject factionSettings) {
			JsonObject standingSettings = GsonHelper.getAsJsonObject(factionSettings, STANDING_SETTINGS);
			return new FactionDefinition.StandingSettings(GsonHelper.getAsInt(standingSettings, PLAYER_DEFAULT_STANDING), GsonHelper.getAsBoolean(standingSettings, STANDING_CAN_CHANGE), parseStandingChanges(standingSettings));
		}

		private Map<String, Integer> parseStandingChanges(JsonObject json) {
			return JsonHelper.mapFromJson(json, STANDING_CHANGES, Entry::getKey, entry -> GsonHelper.convertToInt(entry.getValue(), entry.getKey()));
		}

		private Map<String, CompoundTag> parseThemedBLocks(JsonObject json, String factionName) {
			return JsonHelper.mapFromJson(json, THEMED_BLOCKS, Entry::getKey, entry -> getThemedBlocksNBT(entry.getValue(), factionName));
		}

		private CompoundTag getThemedBlocksNBT(JsonElement json, String factionName) {
			CompoundTag nbt;
			try {
				nbt = TagParser.parseTag(json.toString());
				nbt.putString("customData", factionName);
			}
			catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
				AncientWarfareNPC.LOG.error("Error parsing themed blocks nbt", e);
				nbt = new CompoundTag();
			}
			return nbt;
		}

		private Map<String, Boolean> parseHostileTowards(JsonObject json) {
			return JsonHelper.mapFromJson(json, HOSTILE_TOWARDS_FACTIONS, Entry::getKey, entry -> GsonHelper.convertToBoolean(entry.getValue(), ""));
		}

	}

	private static final FactionDefinition EMPTY_FACTION = new FactionDefinition(new FactionDefinition.StandingSettings(0, false, new HashMap<>()), new HashSet<>(), new HashSet<>()).copy("", -1).build();
}
