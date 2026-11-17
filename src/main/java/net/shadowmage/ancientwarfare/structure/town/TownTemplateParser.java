package net.shadowmage.ancientwarfare.structure.town;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonParser;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.util.CompatUtils;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.town.TownTemplate.TownStructureEntry;
import net.shadowmage.ancientwarfare.structure.town.TownTemplate.TownWallEntry;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class TownTemplateParser {
	private TownTemplateParser() {}

	public static Optional<TownTemplate> parseTemplate(List<String> lines) {
		TownTemplate template = new TownTemplate();
		Iterator<String> it = lines.iterator();
		String line;

		while (it.hasNext() && (line = it.next()) != null) {
			line = line.toLowerCase(Locale.ENGLISH);
			if (line.startsWith("header:")) {
				if (!parseHeader(it, template)) {
					return Optional.empty();
				}
			} else if (line.startsWith("walls:")) {
				parseWalls(it, template);
			} else if (line.startsWith("wallpatterns:")) {
				parseWallPatterns(it, template);
			} else if (line.startsWith("uniquestructures:")) {
				parseUniqueStructures(it, template);
			} else if (line.startsWith("mainstructures:")) {
				parseMainStructures(it, template);
			} else if (line.startsWith("housestructures:")) {
				parseHouseStructures(it, template);
			} else if (line.startsWith("cosmeticstructures:")) {
				parseCosmetics(it, template);
			} else if (line.startsWith("exteriorstructures:")) {
				parseExteriorStructures(it, template);
			}
		}
		if (template.isValid()) {
			return Optional.of(template);
		}
		return Optional.empty();
	}

	private static final Map<String, BiConsumer<String, TownTemplate>> headerVariableSetters = new ImmutableMap.Builder<String, BiConsumer<String, TownTemplate>>()
			.put("name", (value, template) -> template.setTownTypeName(value))
			.put("minsize", (value, template) -> template.setMinSize(Integer.parseInt(value)))
			.put("maxsize", (value, template) -> template.setMaxSize(Integer.parseInt(value)))
			.put("buildingexpansion", (value, template) -> template.setTownBuildingWidthExpansion(Integer.parseInt(value)))
			.put("selectionweight", (value, template) -> template.setSelectionWeight(Integer.parseInt(value)))
			.put("clustervalue", (value, template) -> template.setClusterValue(Integer.parseInt(value)))
			.put("townblocksize", (value, template) -> template.setTownBlockSize(Integer.parseInt(value)))
			.put("townplotsize", (value, template) -> template.setTownPlotSize(Integer.parseInt(value)))
			.put("wallstyle", (value, template) -> template.setWallStyle(Integer.parseInt(value)))
			.put("wallsize", (value, template) -> template.setWallSize(Integer.parseInt(value)))
			.put("exteriorsize", (value, template) -> template.setExteriorSize(Integer.parseInt(value)))
			.put("exteriorplotsize", (value, template) -> template.setExteriorPlotSize(Integer.parseInt(value)))
			.put("interioremptyplotchance", (value, template) -> template.setInteriorEmtpyPlotChance(Integer.parseInt(value)))
			.put("randomvillagersperchunk", (value, template) -> template.setRandomVillagersPerChunk(Float.parseFloat(value)))
			.put("preventnaturalhostilespawns", (value, template) -> template.setPreventNaturalHostileSpawns(Boolean.parseBoolean(value)))
			.put("roadblocks", (value, template) -> {
				String[] roadFillBlocks = value.split("\\|");
								for (String roadFillBlock : roadFillBlocks) {
					try {
						template.addRoadFillBlock(getRoadBlockState(JsonParser.parseString(roadFillBlock).getAsJsonObject()));
					}
					catch (RuntimeException ex) {
						AncientWarfareStructure.LOG.error("Error parsing roadBlock JSON \"{}\": ", roadFillBlock, ex);
					}
				}

			})
			.put("biomewhitelist", (value, template) -> template.setBiomeWhiteList(Boolean.parseBoolean(value)))
			.put("biomelist", (value, template) -> template.setBiomeList(parseBiomeList(value)))
			.put("dimensionwhitelist", (value, template) -> template.setDimensionWhiteList(Boolean.parseBoolean(value)))
			.put("dimensionlist", (value, template) -> template.setDimensionList(parseDimensionList(value)))
			.put("lamp", (value, template) -> template.setLamp(new TownStructureEntry(value, 1)))
			.put("biomereplacement", (value, template) -> template.setBiomeReplacement(ResourceLocation.tryParse(value.trim().toLowerCase(Locale.ENGLISH))))
			.put("territoryname", (value, template) -> template.setTerritoryName(value))
			.build();

	/*
	 * Port addition: the road blocks are 1.12 block states ({"name":"minecraft:grass_path"}); they go through the same
	 * upgrade as the block rules of the templates
	 */
	private static net.minecraft.world.level.block.state.BlockState getRoadBlockState(com.google.gson.JsonObject json) {
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
		String name = net.minecraft.util.GsonHelper.getAsString(json, "name");
		tag.putString("blockName", name);
		if (json.has("properties")) {
			net.minecraft.nbt.CompoundTag properties = new net.minecraft.nbt.CompoundTag();
			net.minecraft.util.GsonHelper.getAsJsonObject(json, "properties").entrySet().forEach(e -> properties.putString(e.getKey(), e.getValue().getAsString()));
			if (name.equals("minecraft:dirt") && !properties.contains("snowy")) {
				properties.putString("snowy", "false"); //the fixers match complete 1.12 states only
			}
			tag.put("properties", properties);
		}
		return net.shadowmage.ancientwarfare.core.util.NBTHelper.getBlockState(net.shadowmage.ancientwarfare.structure.util.LegacyBlockStates.upgrade(tag));
	}

	private static boolean parseHeader(Iterator<String> it, TownTemplate template) {
		String line;
		while (it.hasNext() && (line = it.next()) != null) {
			String[] variableAndValue = line.split("=");
			String variableName = variableAndValue[0].toLowerCase();
			if (variableName.startsWith(":endheader")) {
				break;
			} else if (variableName.equals("mods") && variableAndValue.length > 1) {
				if (!CompatUtils.areModsLoaded(variableAndValue[1].split(","))) {
					return false;
				}
			} else if (headerVariableSetters.containsKey(variableName) && variableAndValue.length > 1) {
				headerVariableSetters.get(variableName).accept(variableAndValue[1], template);
			}
		}
		if (template.getRoadFillBlocks().isEmpty()) {
			template.addRoadFillBlock(Blocks.GRAVEL.defaultBlockState());
		}
		return true;
	}

	private static void parseUniqueStructures(Iterator<String> it, TownTemplate template) {
		String line;
		while (it.hasNext() && (line = it.next()) != null) {
			if (line.toLowerCase(Locale.ENGLISH).startsWith(":enduniquestructures")) {
				break;
			} else {
				TownStructureEntry e = parseStructureName(line);
				template.getUniqueStructureEntries().add(e);
			}
		}
	}

	private static void parseMainStructures(Iterator<String> it, TownTemplate template) {
		String line;
		while (it.hasNext() && (line = it.next()) != null) {
			if (line.toLowerCase(Locale.ENGLISH).startsWith(":endmainstructures")) {
				break;
			} else {
				TownStructureEntry e = parseStructureName(line);
				template.getMainStructureEntries().add(e);
			}
		}
	}

	private static void parseHouseStructures(Iterator<String> it, TownTemplate template) {
		String line;
		while (it.hasNext() && (line = it.next()) != null) {
			if (line.toLowerCase(Locale.ENGLISH).startsWith(":endhousestructures")) {
				break;
			} else {
				TownStructureEntry e = parseStructureWeight(line);
				template.getHouseStructureEntries().add(e);
			}
		}
	}

	private static void parseCosmetics(Iterator<String> it, TownTemplate template) {
		String line;
		while (it.hasNext() && (line = it.next()) != null) {
			if (line.toLowerCase(Locale.ENGLISH).startsWith(":endcosmeticstructures")) {
				break;
			} else {
				TownStructureEntry e = parseStructureWeight(line);
				template.getCosmeticEntries().add(e);
			}
		}
	}

	private static void parseExteriorStructures(Iterator<String> it, TownTemplate template) {
		String line;
		while (it.hasNext() && (line = it.next()) != null) {
			if (line.toLowerCase(Locale.ENGLISH).startsWith(":endexteriorstructures")) {
				break;
			} else {
				TownStructureEntry e = parseStructureWeight(line);
				template.getExteriorStructureEntries().add(e);
			}
		}
	}

	private static void parseWalls(Iterator<String> it, TownTemplate template) {
		String line;
		while (it.hasNext() && (line = it.next()) != null) {
			if (line.toLowerCase(Locale.ENGLISH).startsWith(":endwalls")) {
				break;
			} else {
				TownWallEntry e = parseWall(line);
				template.addWall(e);
			}
		}
	}

	private static void parseWallPatterns(Iterator<String> it, TownTemplate template) {
		String line;
		while (it.hasNext() && (line = it.next()) != null) {
			if (line.toLowerCase(Locale.ENGLISH).startsWith(":endwallpaterns")) {
				break;
			} else {
				String[] bits = line.split(":", -1);
				int size = NumberUtils.toInt(bits[0]);
				int[] pattern = parseWallPattern(bits[1]);
				template.addWallPattern(size, pattern);
			}
		}
	}

	private static List<String> parseBiomeList(String line) {
		String[] bits = line.split(",", -1);
		if (bits.length <= 0) {
			return Collections.emptyList();
		}
		List<String> names = new ArrayList<>();
		for (String bit : bits) {
			names.add(bit.toLowerCase(Locale.ENGLISH));
		}
		return names;
	}

	private static List<Integer> parseDimensionList(String line) {
		String[] bits = line.split(",", -1);
		if (bits.length <= 0) {
			return Collections.emptyList();
		}
		List<Integer> dims = new ArrayList<>();
		for (String bit : bits) {
			dims.add(NumberUtils.toInt(bit.toLowerCase(Locale.ENGLISH)));
		}
		return dims;
	}

	private static TownStructureEntry parseStructureName(String line) {
		String[] bits = line.split(":", -1);
		return new TownStructureEntry(bits[0], 0);
	}

	private static TownStructureEntry parseStructureWeight(String line) {
		String[] bits = line.split(":", -1);
		return new TownStructureEntry(bits[0], NumberUtils.toInt(bits[1]));
	}

	private static TownWallEntry parseWall(String line) {
		String[] bits = line.split(":", -1);
		return new TownWallEntry(bits[0], bits[1], NumberUtils.toInt(bits[2]), NumberUtils.toInt(bits[3]));
	}

	private static int[] parseWallPattern(String line) {
		String[] bits = line.split("-", -1);
		int[] pattern = new int[bits.length];
		for (int i = 0; i < bits.length; i++) {
			pattern[i] = NumberUtils.toInt(bits[i]);
		}
		return pattern;
	}

}
