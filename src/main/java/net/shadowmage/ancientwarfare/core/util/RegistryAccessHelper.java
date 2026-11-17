package net.shadowmage.ancientwarfare.core.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.function.Supplier;

/*
 * Port addition. 1.21.1 needs a HolderLookup.Provider to (de)serialize ItemStacks and other registry-backed data.
 * The 1.12 code serialized stacks from static helpers everywhere, so the provider is resolved globally here:
 * the running server's registry access if there is one, else the client's (set by the client setup), else the
 * builtin registries (only static registries, good enough for vanilla + mod items/blocks).
 */
public final class RegistryAccessHelper {
	private RegistryAccessHelper() {}

	private static Supplier<HolderLookup.Provider> clientProvider = () -> null;

	public static void setClientProvider(Supplier<HolderLookup.Provider> provider) {
		clientProvider = provider;
	}

	public static HolderLookup.Provider get() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			return server.registryAccess();
		}
		HolderLookup.Provider client = clientProvider.get();
		if (client != null) {
			return client;
		}
		return RegistryAccess.EMPTY;
	}
}
