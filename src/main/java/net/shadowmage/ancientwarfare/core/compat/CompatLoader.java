package net.shadowmage.ancientwarfare.core.compat;

import net.neoforged.fml.ModList;

import java.util.HashSet;
import java.util.Set;

/*
 * Port note: the hook is kept; the 1.12 optional integrations (FTB team viewer, JEI plugin) are dropped.
 */
public class CompatLoader {
	private static Set<ICompat> compats = new HashSet<>();

	public static void registerCompat(ICompat compat) {
		compats.add(compat);
	}

	public static void init() {
		for (ICompat compat : compats) {
			if (ModList.get().isLoaded(compat.getModId())) {
				compat.init();
			}
		}
	}
}
