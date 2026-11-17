package net.shadowmage.ancientwarfare.core.gamedata;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/*
 * Port notes: 1.12 getData used the global map storage (stored with the overworld), getPerWorldData the storage of the
 * given dimension; same here. Data classes extend AWSavedData and keep their (String name) constructor. Server only.
 */
public final class AWGameData {

	public static final AWGameData INSTANCE = new AWGameData();

	public <T extends SavedData> T getData(Level world, Class<T> clz) {
		if (!(world instanceof ServerLevel)) {
			throw new IllegalStateException("Unable to get WorldSaveData - Map storage hasn't been initialized yet");
		}
		return initData(((ServerLevel) world).getServer().overworld().getDataStorage(), clz, "AW" + clz.getSimpleName());
	}

	public <T extends SavedData> T getPerWorldData(Level world, Class<T> clz) {
		if (!(world instanceof ServerLevel)) {
			throw new IllegalStateException("Unable to get WorldSaveData - Map storage hasn't been initialized yet");
		}
		return initData(((ServerLevel) world).getDataStorage(), clz, "AW" + clz.getSimpleName());
	}

	public <T extends SavedData> T getNamedData(ServerLevel world, Class<T> clz, String name, boolean perWorldStorage) {
		return initData(perWorldStorage ? world.getDataStorage() : world.getServer().overworld().getDataStorage(), clz, name);
	}

	private <T extends SavedData> T initData(DimensionDataStorage storage, Class<T> clz, String name) {
		return storage.computeIfAbsent(new SavedData.Factory<>(() -> create(clz, name), (tag, registries) -> {
			T data = create(clz, name);
			((AWSavedData) data).readFromNBT(tag);
			return data;
		}), name);
	}

	private static <T extends SavedData> T create(Class<T> clz, String name) {
		if (!AWSavedData.class.isAssignableFrom(clz)) {
			throw new IllegalStateException("WorldSaveData of type " + clz + " must extend AWSavedData");
		}
		try {
			return clz.getConstructor(String.class).newInstance(name);
		}
		catch (Exception e) {
			throw new IllegalStateException("Error getting WorldSaveData of type " + clz.toString(), e);
		}
	}
}
