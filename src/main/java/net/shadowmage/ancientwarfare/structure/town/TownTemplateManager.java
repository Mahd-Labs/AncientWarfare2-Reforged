package net.shadowmage.ancientwarfare.structure.town;

import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.structure.template.WorldGenStructureManager;
import net.shadowmage.ancientwarfare.structure.util.LegacyBiomes;
import net.shadowmage.ancientwarfare.structure.worldgen.Territory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TownTemplateManager {

	public static final TownTemplateManager INSTANCE = new TownTemplateManager();

	private final HashMap<String, TownTemplate> templates;
	private final List<TownTemplate> searchCache;

	private TownTemplateManager() {
		templates = new HashMap<>();
		searchCache = new ArrayList<>();
	}

	public void loadTemplate(TownTemplate template) {
		templates.put(template.getTownTypeName(), template);
	}

	public Optional<TownTemplate> getTemplate(String name) {
		return Optional.ofNullable(templates.get(name));
	}

	public Collection<TownTemplate> getTemplates() {
		return templates.values();
	}

	/* Port addition: the largest area any loaded town template can use (chunks), 0 without templates */
	int getLargestMaxSize() {
		return templates.values().stream().mapToInt(TownTemplate::getMaxSize).max().orElse(0);
	}

	List<TownTemplate> getTemplatesValidAtPosition(Level world, int x, int z) {
		String biomeName = LegacyBiomes.getSurfaceBiomeName(world, x, z);
		if (biomeName.isEmpty()) {
			return Collections.emptyList();
		}
		int dimension = WorldGenStructureManager.getDimensionId(world);
		return templates.values().stream().filter(t -> isDimensionValid(dimension, t) && isBiomeValid(biomeName, t)).collect(Collectors.toList());
	}

	Optional<TownTemplate> selectTemplateFittingArea(Level world, TownBoundingArea area, List<TownTemplate> templates, Territory territory) {
		TownTemplate selection = null;
		int width = area.getChunkWidth();
		int length = area.getChunkLength();

		int min = Math.min(width, length);
		int templateMinimumSize;

		int totalWeight = 0;
		for (TownTemplate t : templates) {
			templateMinimumSize = t.getMinSize();
			if (min >= templateMinimumSize && isCorrectTerritory(territory.getTerritoryName(), t) && territory.getRemainingClusterValue() > t.getClusterValue()) {
				searchCache.add(t);
				totalWeight += t.getSelectionWeight();
			}
		}
		if (!searchCache.isEmpty() && totalWeight > 0) {
			totalWeight = world.random.nextInt(totalWeight);
			for (TownTemplate t : searchCache) {
				totalWeight -= t.getSelectionWeight();
				if (totalWeight < 0) {
					selection = t;
					break;
				}
			}
		}
		searchCache.clear();
		if (selection == null) {
			net.shadowmage.ancientwarfare.structure.AncientWarfareStructure.LOG.debug("No town template for an area of {} x {} chunks in territory \"{}\" (remaining cluster value {}, {} templates valid for the biome)",
					width, length, territory.getTerritoryName(), territory.getRemainingClusterValue(), templates.size());
		}
		return Optional.ofNullable(selection);
	}

	private boolean isCorrectTerritory(String territoryName, TownTemplate t) {
		return t.getTerritoryName().equals(territoryName) || t.getTerritoryName().isEmpty();
	}

	private boolean isBiomeValid(String biome, TownTemplate t) {
		boolean contains = t.getTranslatedBiomes().contains(biome);
		boolean wl = t.isBiomeWhiteList();
		return (wl && contains) || (!wl && !contains);
	}

	private boolean isDimensionValid(int dimension, TownTemplate t) {
		return t.getDimensionList().contains(dimension) == t.isDimensionWhiteList();
	}

	/*
	 * Port addition (GameTests)
	 */
	public void removeTemplate(String name) {
		templates.remove(name);
	}

	public void removeAll() {
		templates.clear();
	}
}
