package net.shadowmage.ancientwarfare.structure.template.load;

import net.minecraft.util.Tuple;
import net.shadowmage.ancientwarfare.core.config.ModConfiguration;
import net.shadowmage.ancientwarfare.core.util.FileUtils;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.config.AWStructureStatics;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;
import net.shadowmage.ancientwarfare.structure.template.datafixes.FixResult;
import net.shadowmage.ancientwarfare.structure.template.save.TemplateExporter;
import net.shadowmage.ancientwarfare.structure.town.TownTemplate;
import net.shadowmage.ancientwarfare.structure.town.TownTemplateManager;
import net.shadowmage.ancientwarfare.structure.town.TownTemplateParser;
import net.shadowmage.ancientwarfare.structure.util.LegacyBiomes;
import net.shadowmage.ancientwarfare.structure.worldgen.TerritoryManager;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class TemplateLoader {

	private static final String DEFAULT_TEMPLATE_DIRECTORY = "assets/ancientwarfare/template";
	public static final String OUTPUT_DIRECTORY = ModConfiguration.configPathForFiles + "structures/export/";
	public static final String INCLUDE_DIRECTORY = ModConfiguration.configPathForFiles + "structures/included/";

	private List<TownTemplate> parsedTownTemplates = new ArrayList<>();
	private int failedTemplates;
	private int skippedForMods;

	public static final TemplateLoader INSTANCE = new TemplateLoader();

	private TemplateLoader() {
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void initializeAndExportDefaults() {
		/*
		 * create default dirs if they don't exist...
		 */
		File existTest = new File(OUTPUT_DIRECTORY);
		if (!existTest.exists()) {
			existTest.mkdirs();
		}

		existTest = new File(INCLUDE_DIRECTORY);
		if (!existTest.exists()) {
			existTest.mkdirs();
		}
	}

	public void loadTemplates() {
		int loadedCount = 0;
		failedTemplates = 0;
		skippedForMods = 0;
		TemplateParser.INSTANCE.totalSkippedRules = 0;
		TemplateParser.INSTANCE.templatesWithSkippedRules = 0;
		parsedTownTemplates.clear(); //1.12 kept the list across reloads
		if (AWStructureStatics.loadDefaultPack) {
			//noinspection ConstantConditions
			loadedCount += loadTemplatesFromSource(null, DEFAULT_TEMPLATE_DIRECTORY, false); //null = the templates shipped in the mod file
		}
		loadedCount += loadTemplatesFromSource(new File(INCLUDE_DIRECTORY), "", true);

		AncientWarfareStructure.LOG.info("Loaded {} structure(s)", loadedCount);
		//Port addition: summary of what the tolerant loading left out (details at debug level)
		AncientWarfareStructure.LOG.info("Templates: {} skipped (they need mods that are absent), {} unreadable, {} rules skipped in {} templates",
				skippedForMods, failedTemplates, TemplateParser.INSTANCE.totalSkippedRules, TemplateParser.INSTANCE.templatesWithSkippedRules);

		validateTownTemplates();
	}

	public void reloadAll() {
		StructureTemplateManager.removeAll();
		TownTemplateManager.INSTANCE.removeAll();

		loadTemplates();
	}

	private int loadTemplatesFromSource(File source, String base, boolean saveFixedTemplate) {
		Map<String, List<String>> loaded = new TreeMap<>();
		walk(source, base, (root, file) -> {
			String relative = root.relativize(file).toString();

			@SuppressWarnings("squid:S4784")
			String name = FilenameUtils.removeExtension(relative).replaceAll("\\\\", "/");

			String extension = FilenameUtils.getExtension(file.toString());

			if (extension.equals(AWStructureStatics.townTemplateExtension) || extension.equals(AWStructureStatics.templateExtension)) {
				List<String> lines;
				try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
					lines = reader.lines().filter(l -> !l.startsWith("#")).collect(Collectors.toList());
					if (extension.equals(AWStructureStatics.townTemplateExtension)) {
						loadTownTemplate(lines);
					} else {
						loadTemplate(file, lines, saveFixedTemplate).ifPresent(res -> {
							if (!loaded.containsKey(res.getA())) {
								loaded.put(res.getA(), new ArrayList<>());
							}
							loaded.get(res.getA()).add(res.getB());
						});
					}
				}
				catch (IOException e) {
					AncientWarfareStructure.LOG.error("Couldn't read template data {} from {}", name, file, e);
				}
				catch (UncheckedIOException e) {
					AncientWarfareStructure.LOG.error("Couldn't read template data {} from {} - most probable cause is incorrect encoding, should be ISO-8859-1", name, file, e);
				}
			}
		});

		for (Map.Entry<String, List<String>> entry : loaded.entrySet()) {
			if (entry.getValue().size() > 1) {
				AncientWarfareStructure.LOG.warn("Template name \"{}\" is present in multiple locations only one of them was used:\n{}", entry::getKey,
						() -> String.join("\n", entry.getValue()));
			}
		}

		return loaded.size();
	}

	private Optional<Tuple<String, String>> loadTemplate(Path fileName, List<String> lines, boolean saveFixedTemplate) {
		Optional<FixResult<StructureTemplate>> result;
		try {
			result = TemplateParser.INSTANCE.parseTemplate(fileName.toString(), lines);
		}
		catch (RuntimeException e) {
			//Port addition: one broken template must not stop the loading of the rest (1.12 let the exception through)
			failedTemplates++;
			AncientWarfareStructure.LOG.warn("Template {} not loaded: {}", fileName, e.toString());
			return Optional.empty();
		}
		if (!result.isPresent()) {
			skippedForMods++;
			return Optional.empty();
		}

		FixResult<StructureTemplate> loadedTemplate = result.get();
		StructureTemplate template = loadedTemplate.getData();

		if (loadedTemplate.isModified()) {
			AncientWarfareStructure.LOG.info("Template {} had following fixes applied: {}", () -> fileName,
					() -> String.join(", ", loadedTemplate.getFixesApplied()));
		}

		if (saveFixedTemplate && loadedTemplate.isModified()) {
			TemplateExporter.exportTo(template, fileName.getParent().toFile());
			AncientWarfareStructure.LOG.info("Changes saved to {}", fileName);
		}

		AncientWarfareStructure.LOG.debug("Loaded Structure Template: [{}] WorldGen: {}  Survival: {}", template.name, template.getValidationSettings().isWorldGenEnabled(), template.getValidationSettings().isSurvival());
		StructureTemplateManager.addTemplate(template);
		return Optional.of(new Tuple<>(template.name, fileName.toString()));
	}

	private void loadTownTemplate(List<String> lines) {
		TownTemplateParser.parseTemplate(lines).ifPresent(parsedTownTemplates::add);
	}

	/*
	 * Port notes: 1.12 registerTerritoryBiomes ran when a town template was parsed; the biome registry only exists
	 * while a server runs, so WorldGenStructureManager.rebuild() calls this at server start / after a reload.
	 */
	public void registerTownTerritoryBiomes() {
		for (TownTemplate t : parsedTownTemplates) {
			t.setTranslatedBiomes(LegacyBiomes.translate(t.getBiomeList()));
			registerTerritoryBiomes(t);
		}
	}

	private void registerTerritoryBiomes(TownTemplate t) {
		if (t.isBiomeWhiteList()) {
			t.getTranslatedBiomes().forEach(biomeName -> TerritoryManager.addTerritoryInBiome(t.getTerritoryName(), biomeName));
		} else {
			LegacyBiomes.getRegistry().ifPresent(registry -> registry.keySet().forEach(id -> {
				String biomeName = id.toString();
				if (!t.getTranslatedBiomes().contains(biomeName)) {
					TerritoryManager.addTerritoryInBiome(t.getTerritoryName(), biomeName);
				}
			}));
		}
	}

	private void validateTownTemplates() {
		if (!this.parsedTownTemplates.isEmpty()) {
			AncientWarfareStructure.LOG.info("Loading Town Templates: ");
			for (TownTemplate t : this.parsedTownTemplates) {
				AncientWarfareStructure.LOG.debug("Loading town template: {}", t.getTownTypeName());
				t.validateStructureEntries();
				TownTemplateManager.INSTANCE.loadTemplate(t);
			}
			AncientWarfareStructure.LOG.info("Loaded : {} Town Templates.", parsedTownTemplates.size());
		}
	}

	/*
	 * Port notes: 1.12 FileUtils.findFiles(Loader.instance().activeModContainer().getSource(), ..) -> the mod file's
	 * resource path (works for the jar and for the development resource folders).
	 */
	private void walk(File source, String base, java.util.function.BiConsumer<Path, Path> processor) {
		if (source != null) {
			FileUtils.findFiles(source, base, processor);
			return;
		}
		Path root = net.neoforged.fml.ModList.get().getModFileById(AncientWarfareStructure.MOD_ID).getFile().findResource(base.split("/"));
		if (!Files.exists(root)) {
			return;
		}
		try (java.util.stream.Stream<Path> files = Files.walk(root)) {
			files.filter(Files::isRegularFile).forEach(file -> processor.accept(root, file));
		}
		catch (IOException e) {
			AncientWarfareStructure.LOG.error("Error reading the default templates: ", e);
		}
	}
}
