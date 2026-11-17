package net.shadowmage.ancientwarfare.core.util;

import net.minecraft.nbt.Tag;

/*
 * Port addition: the 1.12 Forge INBTSerializable shape (no HolderLookup.Provider parameters, unlike the NeoForge
 * 1.21.1 interface of the same name), so that ported classes keep serializeNBT() / deserializeNBT(tag).
 */
public interface INBTSerializable<T extends Tag> {
	T serializeNBT();

	void deserializeNBT(T nbt);
}
