package net.shadowmage.ancientwarfare.core.gamedata;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/*
 * Port addition: base for every 1.12 WorldSavedData subclass. Keeps the 1.12 shape (constructor with the data name,
 * readFromNBT, writeToNBT, markDirty) so that AWGameData can still create and load the data from its class alone.
 */
public abstract class AWSavedData extends SavedData {
	public final String mapName;

	public AWSavedData(String name) {
		this.mapName = name;
	}

	public abstract void readFromNBT(CompoundTag tag);

	public abstract CompoundTag writeToNBT(CompoundTag tag);

	@Override
	public final CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		return writeToNBT(tag);
	}

	/* 1.12 alias */
	public void markDirty() {
		setDirty();
	}
}
