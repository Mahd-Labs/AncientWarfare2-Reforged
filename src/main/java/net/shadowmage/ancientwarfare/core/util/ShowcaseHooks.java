package net.shadowmage.ancientwarfare.core.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.AncientWarfare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Port addition: the only link between the mod and the dev screenshot harness (package "showcase"). The harness is
 * left out of release jars (gradle task releaseJar), so nothing outside that package may name its classes: modules
 * use this class, and the harness is started by name when it is there.
 */
public final class ShowcaseHooks {
	private ShowcaseHooks() {}

	private static final String HARNESS_CLASS = "net.shadowmage.ancientwarfare.showcase.ShowcaseHarness";

	public record EntityVariant(String name, CompoundTag nbt) {}

	private static final Map<ResourceLocation, List<EntityVariant>> entityVariants = new HashMap<>();

	/*
	 * JVM property -Daw.showcase=true (gradle run "showcase")
	 */
	public static boolean isEnabled() {
		return Boolean.getBoolean("aw.showcase");
	}

	/*
	 * Modules call this from their client init (only when isEnabled()) to add pictures of entity sub types:
	 * the nbt is merged into the freshly created entity before it is placed. File: entity__<path>@<name>.png
	 */
	public static void registerEntityVariant(ResourceLocation entityTypeId, String name, CompoundTag nbt) {
		entityVariants.computeIfAbsent(entityTypeId, k -> new ArrayList<>()).add(new EntityVariant(name, nbt));
	}

	public static List<EntityVariant> getEntityVariants(ResourceLocation entityTypeId) {
		return entityVariants.getOrDefault(entityTypeId, List.of());
	}

	/*
	 * Starts the harness (client init, only when isEnabled()). A build without the harness logs a warning and goes on.
	 */
	public static void startHarness() {
		try {
			Class.forName(HARNESS_CLASS).getMethod("init").invoke(null);
		}
		catch (ClassNotFoundException e) {
			AncientWarfare.LOG.warn("aw.showcase is set, but this build does not contain the showcase harness (release jar)");
		}
		catch (ReflectiveOperationException e) {
			AncientWarfare.LOG.error("Could not start the showcase harness", e);
		}
	}
}
