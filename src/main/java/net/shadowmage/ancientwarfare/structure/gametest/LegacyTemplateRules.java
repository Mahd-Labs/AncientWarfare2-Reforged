package net.shadowmage.ancientwarfare.structure.gametest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.api.TemplateRule;
import net.shadowmage.ancientwarfare.structure.template.StructurePluginManager;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Test helper: walks every structure template file shipped in the mod (assets/ancientwarfare/template, the 1.12 pack)
 * and hands every block rule to a visitor: the raw rule tag as written in the file and the rule as the port parses it
 * (StructurePluginManager.getRule, the same path the template loader takes, mod dependencies not checked).
 */
final class LegacyTemplateRules {
	private LegacyTemplateRules() {}

	static final String TEMPLATE_DIRECTORY = "assets/ancientwarfare/template";

	record Entry(String file, String template, String plugin, int number, CompoundTag raw, TemplateRule rule) {}

	/* returns the number of template files read */
	static int forEach(Consumer<Entry> visitor) {
		Path root = net.neoforged.fml.ModList.get().getModFileById(AncientWarfareStructure.MOD_ID).getFile().findResource(TEMPLATE_DIRECTORY.split("/"));
		List<Path> files;
		try (Stream<Path> walk = Files.walk(root)) {
			files = walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".aws")).sorted().collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new IllegalStateException("cannot list the templates", e);
		}
		for (Path file : files) {
			List<String> lines;
			try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
				lines = reader.lines().filter(l -> !l.startsWith("#")).collect(Collectors.toList());
			}
			catch (IOException e) {
				throw new IllegalStateException("cannot read " + file, e);
			}
			readFile(root.relativize(file).toString(), lines, visitor);
		}
		return files.size();
	}

	private static void readFile(String file, List<String> lines, Consumer<Entry> visitor) {
		StructureTemplate.Version version = StructureTemplate.Version.NONE;
		String name = "";
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.startsWith("version=") && name.isEmpty()) {
				version = new StructureTemplate.Version(line.substring("version=".length()));
			} else if (line.startsWith("name=") && name.isEmpty()) {
				name = line.substring("name=".length());
			} else if (line.startsWith("rule:")) {
				List<String> group = new ArrayList<>();
				String plugin = "";
				int number = -1;
				CompoundTag raw = new CompoundTag();
				for (; i < lines.size(); i++) {
					line = lines.get(i);
					group.add(line);
					if (line.startsWith("plugin=")) {
						plugin = line.substring("plugin=".length());
					} else if (line.startsWith("number=")) {
						number = Integer.parseInt(line.substring("number=".length()).trim());
					} else if (line.startsWith(TemplateRule.JSON_PREFIX)) {
						try {
							raw = TagParser.parseTag(line.substring(TemplateRule.JSON_PREFIX.length()));
						}
						catch (Exception e) {
							raw = new CompoundTag();
						}
					}
					if (line.startsWith(":endrule")) {
						break;
					}
				}
				TemplateRule rule;
				try {
					rule = StructurePluginManager.getRule(version, group, "rule").getData();
				}
				catch (Exception e) {
					rule = null;
				}
				visitor.accept(new Entry(file, name, plugin, number, raw, rule));
			}
		}
	}
}
