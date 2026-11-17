package net.shadowmage.ancientwarfare.structure.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.npc.skin.NpcSkinManager;
import net.shadowmage.ancientwarfare.npc.skin.NpcSkinSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/*
 * rc.2 (owner's play check: "Angry Townsman" rendered with the missing texture): every NPC skin that a bundled template
 * can select - a fixed skin (npcTypeSkin) or a skin group (npcTypeName) in a spawner or entity rule of any .aws / .awt
 * file - resolves to a skin_pack file in the mod, through the same reading as an NPC loaded from the template.
 */
@GameTestHolder(AncientWarfare.MOD_ID)
@PrefixGameTestTemplate(false)
public class SkinReferenceGameTests {
	private static final String TEMPLATE = "automation_empty";
	private static final Pattern SKIN = Pattern.compile("npcTypeSkin:\"([^\"]*)\"");
	private static final Pattern GROUP = Pattern.compile("npcTypeName:\"([^\"]*)\"");

	//own batch: the scan reads every template file in one tick, which must not stall timing-sensitive tests
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 400, batch = "skin_references")
	public static void everyTemplateSkinReferenceExists(GameTestHelper helper) {
		Path root = net.neoforged.fml.ModList.get().getModFileById(AncientWarfare.MOD_ID).getFile().findResource("assets", "ancientwarfare", "template");
		Path skins = net.neoforged.fml.ModList.get().getModFileById(AncientWarfare.MOD_ID).getFile().findResource("assets", "ancientwarfare");
		Set<String> skinRefs = new TreeSet<>();
		Set<String> groups = new TreeSet<>();
		int files = 0;
		try (Stream<Path> walk = Files.walk(root)) {
			for (Path file : (Iterable<Path>) walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".aws") || p.toString().endsWith(".awt"))::iterator) {
				files++;
				String text = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
				Matcher m = SKIN.matcher(text);
				while (m.find()) {
					skinRefs.add(m.group(1));
				}
				m = GROUP.matcher(text);
				while (m.find()) {
					groups.add(m.group(1));
				}
			}
		}
		catch (IOException e) {
			helper.fail("cannot read the templates: " + e);
			return;
		}
		List<String> missing = new ArrayList<>();
		for (String ref : skinRefs) {
			CompoundTag tag = new CompoundTag();
			tag.putString("skinTypeSelected", "npc_type");
			tag.putBoolean("random", false);
			tag.putString("npcTypeSkin", ref);
			ResourceLocation rl = NpcSkinSettings.deserializeNBT(tag).getNpcTypeSkin().orElse(null);
			if (rl == null || !rl.getNamespace().equals(AncientWarfare.MOD_ID) || !Files.isRegularFile(skins.resolve(rl.getPath()))) {
				missing.add(ref + " -> " + rl);
			}
		}
		Set<String> knownGroups = NpcSkinManager.getSkinTypes();
		List<String> unknownGroups = groups.stream().filter(g -> !knownGroups.contains(g)).toList();
		if (files < 1000 || skinRefs.isEmpty()) {
			helper.fail("only " + files + " template files / " + skinRefs.size() + " skin references found");
			return;
		}
		if (!missing.isEmpty() || !unknownGroups.isEmpty()) {
			helper.fail(missing.size() + " of " + skinRefs.size() + " fixed skins missing, e.g. " + missing.stream().limit(3).toList()
					+ "; " + unknownGroups.size() + " unknown skin groups " + unknownGroups.stream().limit(5).toList());
			return;
		}
		AncientWarfare.LOG.info("everyTemplateSkinReferenceExists: {} template files, {} fixed skins, {} skin groups resolve", files, skinRefs.size(), groups.size());
		helper.succeed();
	}
}
