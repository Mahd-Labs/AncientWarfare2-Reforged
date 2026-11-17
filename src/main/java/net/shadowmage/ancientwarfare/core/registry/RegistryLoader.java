package net.shadowmage.ancientwarfare.core.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.util.parsing.JsonHelper;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/*
 * Port notes: still loads the json registry files from assets/ancientwarfare/registry inside the mod file, after the
 * override files in <game dir>/config/ancientwarfare/registry (a file there replaces the built in file with the same
 * relative name). load() runs from FMLCommonSetupEvent (1.12: init), when all blocks and items are registered.
 */
public class RegistryLoader {
	private RegistryLoader() {}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, IRegistryDataParser> parsers = new HashMap<>();

	public static void registerParser(IRegistryDataParser parser) {
		parsers.put(parser.getName(), parser);
	}

	private static final Map<ResourceLocation, String> loadedRegistries = new HashMap<>();

	private static final List<DependentFile> loadLater = new ArrayList<>();

	public static void load() {
		load(p -> true);
	}

	public static void reload(String type) {
		loadedRegistries.entrySet().removeIf(entry -> entry.getValue().equals(type));
		load(p -> p.getName().equals(type));
	}

	public static void load(Predicate<IRegistryDataParser> include) {
		String modId = AncientWarfare.MOD_ID;
		Path registryOverridesFolder = FMLPaths.CONFIGDIR.get().resolve("ancientwarfare").resolve("registry");
		if (Files.exists(registryOverridesFolder)) {
			loadRegistries(modId, registryOverridesFolder, include);
		}
		Path root = ModList.get().getModFileById(modId).getFile().findResource("assets", modId, "registry");
		loadRegistries(modId, root, include);
	}

	@SuppressWarnings("squid:S3725") //ZipPath doesn't have toFile support
	private static void loadRegistries(String mod, Path root, Predicate<IRegistryDataParser> include) {
		if (!Files.exists(root)) {
			return;
		}

		List<Path> files;
		try (Stream<Path> stream = Files.walk(root)) {
			files = stream.sorted().toList();
		}
		catch (IOException e) {
			AncientWarfare.LOG.error("Error iterating filesystem for: {}", root, e);
			return;
		}
		Iterator<Path> itr = files.iterator();
		while (itr.hasNext()) {
			loadFile(mod, root, itr.next(), include);
		}
		loadDependents(mod, include);
	}

	private static void loadDependents(String mod, Predicate<IRegistryDataParser> include) {
		int lastCountLoadLater = loadLater.size();
		while (!loadLater.isEmpty()) {
			Iterator<DependentFile> iterator = loadLater.iterator();
			while (iterator.hasNext()) {
				DependentFile dependentFile = iterator.next();
				if (areDependenciesLoaded(dependentFile.getDependencies())) {
					loadFile(mod, dependentFile.getPath(), include, false, dependentFile.getName());
					iterator.remove();
				}
			}

			if (lastCountLoadLater <= loadLater.size()) {
				logIncorrectDependencies();
				break;
			}

			lastCountLoadLater = loadLater.size();
		}
	}

	private static void logIncorrectDependencies() {
		for (DependentFile dependentFile : loadLater) {
			AncientWarfare.LOG.error("Non existent or circular load after dependencies in {} - {}", dependentFile.getPath().toString(), String.join(",", dependentFile.getDependencies()));
		}
	}

	private static void loadFile(String mod, Path root, Path file, Predicate<IRegistryDataParser> include) {
		if (!"json".equals(FilenameUtils.getExtension(file.toString()))) {
			return;
		}

		String relative = root.relativize(file).toString();
		String name = FilenameUtils.removeExtension(relative).replaceAll("\\\\", "/");
		loadFile(mod, file, include, true, name);
	}

	private static void loadFile(String mod, Path file, Predicate<IRegistryDataParser> include, boolean checkDependencies, String name) {
		String shortName = name.substring(name.lastIndexOf('/') + 1);
		ResourceLocation registryName = ResourceLocation.tryBuild(mod, name.toLowerCase());
		if (registryName == null) {
			AncientWarfare.LOG.error("Registry file name {} is not a valid resource path", name);
			return;
		}

		try (BufferedReader reader = Files.newBufferedReader(file)) {
			JsonObject json = GsonHelper.fromJson(GSON, reader, JsonObject.class);
			if (json == null) {
				return;
			}
			Optional<IRegistryDataParser> parser = getParser(shortName, json);
			if (!parser.isPresent()) {
				AncientWarfare.LOG.error("No parser defined for file name {}", shortName);
				return;
			}

			if (checkDependencies && json.has("load_after")) {
				Set<String> dependencies = JsonHelper.setFromJson(json.get("load_after"), e -> GsonHelper.convertToString(e, ""));
				if (!areDependenciesLoaded(dependencies)) {
					loadLater.add(new DependentFile(name, file, dependencies));
					return;
				}
			}

			if (loadedRegistries.containsKey(registryName)) {
				AncientWarfare.LOG.info("Registry {} has already been loaded in overrides, skipping...", registryName.toString());
				return;
			}
			loadedRegistries.put(registryName, parser.get().getName());

			if (!include.test(parser.get()) || isDisabled(json) || !isModLoaded(json)) {
				return;
			}
			parser.get().parse(json);
		}
		catch (JsonParseException e) {
			AncientWarfare.LOG.error("Parsing error loading registry {}", registryName, e);
		}
		catch (MissingResourceException e) {
			AncientWarfare.LOG.error(e.getMessage());
		}
		catch (IOException e) {
			AncientWarfare.LOG.error("Couldn't read registry {} from {}", registryName, file, e);
		}
		catch (RuntimeException e) {
			//1.12 could only fail with the two exceptions above: new ResourceLocation(..) accepted any text. In 1.21 an id
			//with an invalid character (ResourceLocationException) or a value of the wrong json type would otherwise end
			//mod loading; the rest of this file is skipped like for a parse error
			AncientWarfare.LOG.error("Error loading registry {} from {}, the rest of the file is skipped", registryName, file, e);
		}
	}

	private static boolean areDependenciesLoaded(Set<String> dependencies) {
		for (String dependency : dependencies) {
			if (!loadedRegistries.containsValue(dependency)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isModLoaded(JsonObject json) {
		return !GsonHelper.isValidNode(json, "mod") || ModList.get().isLoaded(GsonHelper.getAsString(json, "mod"));
	}

	private static boolean isDisabled(JsonObject json) {
		return json.has("disabled") && GsonHelper.getAsBoolean(json, "disabled");
	}

	private static Optional<IRegistryDataParser> getParser(String fileName, JsonObject json) {
		String parserName = fileName;

		if (json.has("type")) {
			parserName = GsonHelper.getAsString(json, "type");
		}

		return parsers.containsKey(parserName) ? Optional.of(parsers.get(parserName)) : Optional.empty();
	}

	private static class DependentFile {
		private final String name;
		private final Path path;
		private final Set<String> dependencies;

		private DependentFile(String name, Path path, Set<String> dependencies) {
			this.name = name;
			this.path = path;
			this.dependencies = dependencies;
		}

		public Set<String> getDependencies() {
			return dependencies;
		}

		public Path getPath() {
			return path;
		}

		public String getName() {
			return name;
		}
	}
}
