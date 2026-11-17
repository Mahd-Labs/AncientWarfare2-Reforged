package net.shadowmage.ancientwarfare.structure.util;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/*
 * Port addition. Two things world generation needs that 1.12 had for free:
 *
 * 1. Biome names. The templates (and town templates) list 1.12 biome registry names. translate() maps the 1.12
 *    vanilla names to the 1.21 biome ids that cover the same terrain (a 1.12 biome that was split up maps to all of
 *    its parts, e.g. "minecraft:ocean" -> ocean, cold / lukewarm / warm ocean); names that are biome ids of the
 *    running game pass through; everything else (biomes of 1.12 mods) is skipped with a debug line.
 * 2. The biome of a column. 1.12 biomes were 2D and the code asked for the biome at y = 1; 1.21 biomes are 3D and
 *    y = 1 is a cave biome. getSurfaceBiome() samples the biome source at the top of the world, which is the surface
 *    biome of the column, without loading or generating the chunk.
 */
public class LegacyBiomes {
	private LegacyBiomes() {}

	private static final Map<String, String[]> LEGACY = new HashMap<>();
	private static final Set<String> LOGGED_UNKNOWN = new HashSet<>();

	private static void map(String legacy, String... current) {
		String[] ids = new String[current.length];
		for (int i = 0; i < current.length; i++) {
			ids[i] = "minecraft:" + current[i];
		}
		LEGACY.put("minecraft:" + legacy, ids);
	}

	static {
		map("ocean", "ocean", "cold_ocean", "lukewarm_ocean", "warm_ocean");
		map("deep_ocean", "deep_ocean", "deep_cold_ocean", "deep_lukewarm_ocean");
		map("frozen_ocean", "frozen_ocean", "deep_frozen_ocean");
		map("plains", "plains");
		map("mutated_plains", "sunflower_plains");
		map("desert", "desert");
		map("desert_hills", "desert");
		map("mutated_desert", "desert");
		map("extreme_hills", "windswept_hills", "stony_peaks");
		map("smaller_extreme_hills", "windswept_hills");
		map("extreme_hills_with_trees", "windswept_forest");
		map("mutated_extreme_hills", "windswept_gravelly_hills");
		map("mutated_extreme_hills_with_trees", "windswept_gravelly_hills");
		map("forest", "forest");
		map("forest_hills", "forest");
		map("mutated_forest", "flower_forest");
		map("taiga", "taiga");
		map("taiga_hills", "taiga");
		map("mutated_taiga", "taiga");
		map("swampland", "swamp");
		map("mutated_swampland", "swamp");
		map("river", "river");
		map("frozen_river", "frozen_river");
		map("hell", "nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas");
		map("sky", "the_end", "end_highlands", "end_midlands", "small_end_islands", "end_barrens");
		map("void", "the_void");
		map("ice_flats", "snowy_plains");
		map("mutated_ice_flats", "ice_spikes");
		map("ice_mountains", "snowy_slopes", "frozen_peaks", "jagged_peaks", "grove");
		map("mushroom_island", "mushroom_fields");
		map("mushroom_island_shore", "mushroom_fields");
		map("beaches", "beach");
		map("stone_beach", "stony_shore");
		map("cold_beach", "snowy_beach");
		map("jungle", "jungle", "bamboo_jungle");
		map("jungle_hills", "jungle", "bamboo_jungle");
		map("mutated_jungle", "jungle");
		map("jungle_edge", "sparse_jungle");
		map("mutated_jungle_edge", "sparse_jungle");
		map("birch_forest", "birch_forest");
		map("birch_forest_hills", "birch_forest");
		map("mutated_birch_forest", "old_growth_birch_forest");
		map("mutated_birch_forest_hills", "old_growth_birch_forest");
		map("roofed_forest", "dark_forest");
		map("mutated_roofed_forest", "dark_forest");
		map("taiga_cold", "snowy_taiga");
		map("taiga_cold_hills", "snowy_taiga");
		map("mutated_taiga_cold", "snowy_taiga");
		map("redwood_taiga", "old_growth_pine_taiga");
		map("redwood_taiga_hills", "old_growth_pine_taiga");
		map("mutated_redwood_taiga", "old_growth_spruce_taiga");
		map("mutated_redwood_taiga_hills", "old_growth_spruce_taiga");
		map("savanna", "savanna");
		map("savanna_rock", "savanna_plateau");
		map("mutated_savanna", "windswept_savanna");
		map("mutated_savanna_rock", "windswept_savanna");
		map("mesa", "badlands");
		map("mesa_rock", "wooded_badlands");
		map("mesa_clear_rock", "badlands");
		map("mutated_mesa", "eroded_badlands");
		map("mutated_mesa_rock", "wooded_badlands");
		map("mutated_mesa_clear_rock", "badlands");
	}

	/*
	 * the biome registry of the running server, empty while no server runs (the biome registry is world data in 1.21)
	 */
	public static Optional<Registry<Biome>> getRegistry() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		return server == null ? Optional.empty() : Optional.of(server.registryAccess().registryOrThrow(Registries.BIOME));
	}

	public static boolean exists(String biomeName) {
		ResourceLocation id = ResourceLocation.tryParse(biomeName);
		return id != null && getRegistry().map(r -> r.containsKey(id)).orElse(false);
	}

	/*
	 * 1.21 biome ids (that exist in the running server) for a biome name from a template
	 */
	public static Set<String> translate(String biomeName) {
		String name = biomeName.trim().toLowerCase(java.util.Locale.ROOT);
		if (name.isEmpty()) {
			return Collections.emptySet();
		}
		Set<String> ret = new LinkedHashSet<>();
		String[] mapped = LEGACY.get(name);
		if (mapped != null) {
			for (String id : mapped) {
				if (exists(id)) {
					ret.add(id);
				}
			}
		}
		if (exists(name)) {
			ret.add(name);
		}
		if (ret.isEmpty() && LOGGED_UNKNOWN.add(name)) {
			AncientWarfareStructure.LOG.debug("Biome \"{}\" does not exist in this game, skipped", name);
		}
		return ret;
	}

	public static Set<String> translate(Collection<String> biomeNames) {
		Set<String> ret = new LinkedHashSet<>();
		for (String name : biomeNames) {
			ret.addAll(translate(name));
		}
		return ret;
	}

	public static Holder<Biome> getSurfaceBiome(Level world, int x, int z) {
		int y = world.getMaxBuildHeight() - 1;
		if (world instanceof ServerLevel) {
			return ((ServerLevel) world).getUncachedNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
		}
		return world.getBiome(new net.minecraft.core.BlockPos(x, y, z));
	}

	public static String getSurfaceBiomeName(Level world, int x, int z) {
		return getSurfaceBiome(world, x, z).unwrapKey().map(k -> k.location().toString()).orElse("");
	}
}
