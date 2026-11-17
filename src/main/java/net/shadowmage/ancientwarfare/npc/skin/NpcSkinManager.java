package net.shadowmage.ancientwarfare.npc.skin;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Port notes: 1.12 walked assets/ancientwarfare/skin_pack in the mod file (and config/ancientwarfare/npc/skins) and
 * registered every png as a dynamic texture "ancientwarfare:skinpack/<file>". In 1.21.1 the pack files are ordinary
 * resources, so a skin is simply the texture "ancientwarfare:skin_pack/<file>" (file names lower-cased, see
 * skin_pack/_renamed.txt) and no image loading is needed; this class is common code (the type lists are needed by
 * the containers as well). Files are listed in sorted order (1.12: directory order of the jar).
 * Reduced: skins from the config folder and player-name skins (Mojang profile lookup) are not loaded - the client
 * may install playerSkinLookup; without it a "player" skin falls back to the default as it did in 1.12 when the
 * profile could not be loaded.
 */
public class NpcSkinManager {
	private NpcSkinManager() {}

	private static final HashMap<String, List<ResourceLocation>> npcSkins = new HashMap<>();

	private static final Random rng = new Random();

	private static final String DEFAULT_SKINS = "assets/ancientwarfare/skin_pack";

	public static Function<String, Optional<ResourceLocation>> playerSkinLookup = name -> Optional.empty();

	public static Optional<ResourceLocation> getPlayerSkin(String playerName) {
		return playerSkinLookup.apply(playerName);
	}

	public static Optional<ResourceLocation> getNpcTexture(String type, long idlsb) {
		rng.setSeed(idlsb);
		return npcSkins.containsKey(type) ? Optional.of(npcSkins.get(type).get(rng.nextInt(npcSkins.get(type).size()))) : Optional.empty();
	}

	public static void loadSkins() {
		npcSkins.clear();
		if (AWNPCStatics.loadDefaultSkinPack) {
			try {
				Path root = ModList.get().getModFileById(AncientWarfare.MOD_ID).getFile().findResource(DEFAULT_SKINS);
				loadSkinsFromSource(root);
			}
			catch (Exception e) {
				AncientWarfare.LOG.error("Error loading default skin pack: ", e);
			}
		}
	}

	private static void loadSkinsFromSource(Path root) throws IOException {
		if (!Files.exists(root)) {
			return;
		}
		HashMap<String, Set<String>> imageMap = new HashMap<>();
		Path fPath = root.resolve("skin_pack.meta");
		if (Files.exists(fPath)) {
			try (BufferedReader reader = Files.newBufferedReader(fPath)) {
				readImageMap(reader, imageMap);
			}
		}
		List<String> files;
		try (Stream<Path> stream = Files.list(root)) {
			files = stream.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".png")).sorted().collect(Collectors.toList());
		}
		for (String relative : files) {
			if (!ResourceLocation.isValidPath("skin_pack/" + relative)) {
				continue;
			}
			List<String> skinMappings = imageMap.entrySet().stream().filter(e -> e.getValue().contains(relative))
					.map(Map.Entry::getKey).sorted().collect(Collectors.toList());
			ResourceLocation skinRegistryName = ResourceLocation.fromNamespaceAndPath(AncientWarfare.MOD_ID, "skin_pack/" + relative);
			if (!skinMappings.isEmpty()) {
				skinMappings.forEach(skinMapping -> addNpcSkin(skinMapping, skinRegistryName));
			} else {
				addNpcSkin("custom", skinRegistryName);
			}
		}
	}

	private static void readImageMap(BufferedReader br, HashMap<String, Set<String>> imageMap) throws IOException {
		String line;
		while ((line = br.readLine()) != null) {
			String[] lineBits = line.trim().split("=");
			if (lineBits.length > 1) {
				if (!imageMap.containsKey(lineBits[0])) {
					imageMap.put(lineBits[0], new HashSet<>());
				}
				imageMap.get(lineBits[0]).add(lineBits[1]);
			}
		}
	}

	private static void addNpcSkin(String npcType, ResourceLocation texture) {
		if (!npcSkins.containsKey(npcType)) {
			npcSkins.put(npcType, new ArrayList<>());
		}
		npcSkins.get(npcType).add(texture);
	}

	public static Set<String> getSkinTypes() {
		return npcSkins.keySet();
	}

	public static List<ResourceLocation> getTypeSkins(String typeName) {
		return npcSkins.getOrDefault(typeName, new ArrayList<>());
	}
}
