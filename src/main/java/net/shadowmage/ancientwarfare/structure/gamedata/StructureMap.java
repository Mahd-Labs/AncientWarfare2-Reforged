//TODO world capability

package net.shadowmage.ancientwarfare.structure.gamedata;

import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.gamedata.AWSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.Zone;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;
import net.shadowmage.ancientwarfare.structure.network.PacketStructureEntry;
import net.shadowmage.ancientwarfare.structure.util.ConquerHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class StructureMap extends AWSavedData {
	/*
	 * Port notes: AWGameData is server only. The client keeps one StructureMap of its own (filled by
	 * PacketStructureEntry, like the 1.12 client side world data); get(world) picks the right one.
	 * Dimension keys: 1.12 int ids -> the dimension id string.
	 */
	private static StructureMap clientInstance = new StructureMap("AWStructureMap");

	public static StructureMap get(Level world) {
		return world.isClientSide ? clientInstance : AWGameData.INSTANCE.getPerWorldData(world, StructureMap.class);
	}

	public static void resetClientInstance() {
		clientInstance = new StructureMap("AWStructureMap");
	}

	public static String dim(Level world) {
		return world.dimension().location().toString();
	}

	private static final Cache<Zone, Set<StructureEntry>> CHUNK_STRUCTURE_ENTRIES = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build();

	private StructureDimensionMap map;

	public StructureMap(String name) {
		super(name);
		map = new StructureDimensionMap();
	}

	@Override
	public void readFromNBT(CompoundTag nbttagcompound) {
		CompoundTag mapTag = nbttagcompound.getCompound("map");
		map.readFromNBT(this, mapTag);
	}

	@Override
	public CompoundTag writeToNBT(CompoundTag nbttagcompound) {
		CompoundTag mapTag = new CompoundTag();
		map.writeToNBT(mapTag);
		nbttagcompound.put("map", mapTag);
		return nbttagcompound;
	}

	public boolean shouldPreventSpawnAtPos(Level world, BlockPos pos) {
		boolean preventSpawn = false;
		for (StructureEntry entry : getStructuresInChunk(world, pos)) {
			if (entry.getBB().contains(pos) && entry.shouldPreventNaturalHostileSpawns() && !entry.getConquered()) {
				if (entry.hasProtectionFlag() || ConquerHelper.checkBBNotConquered(world, entry.getBB())) {
					return true;
				} else {
					entry.setConquered();
				}
			}
		}
		return preventSpawn;
	}

	private Set<StructureEntry> getStructuresInChunk(Level world, BlockPos pos) {
		Set<StructureEntry> structures;
		ChunkPos chunkPos = new ChunkPos(pos);
		BlockPos min = new BlockPos(chunkPos.x * 16, 1, chunkPos.z * 16);
		BlockPos max = new BlockPos(chunkPos.x * 16 + 15, 255, chunkPos.z * 16 + 15);
		Zone chunkZone = new Zone(min, max);
		try {
			structures = CHUNK_STRUCTURE_ENTRIES.get(chunkZone, () -> getStructuresIn(world, chunkZone));
		}
		catch (ExecutionException e) {
			AncientWarfareNPC.LOG.error("Error getting structure entries in chunk for hostile entity check: ", e);
			return new HashSet<>();
		}
		return structures;
	}

	public Collection<StructureEntry> getEntriesNear(Level world, int worldX, int worldZ, int chunkRadius, boolean expandBySize, Collection<StructureEntry> list) {
		int cx = worldX >> 4;
		int cz = worldZ >> 4;
		return map.getEntriesNear(dim(world), cx, cz, chunkRadius, expandBySize, list);
	}

	private Set<StructureEntry> getStructuresIn(Level world, Zone zone) {
		Set<StructureEntry> ret = new HashSet<>();
		for (StructureEntry structure : getEntriesNear(world, zone.min.getX(), zone.min.getZ(), 1, true, new ArrayList<>())) {
			if (structure.getBB().intersects(zone)) {
				ret.add(structure);
			}
		}
		return ret;
	}

	public Optional<StructureEntry> getStructureAt(Level world, BlockPos pos) {
		for (StructureEntry structure : getEntriesNear(world, pos.getX(), pos.getZ(), 1, true, new ArrayList<>())) {
			if (structure.getBB().contains(pos)) {
				return Optional.of(structure);
			}
		}
		return Optional.empty();
	}

	public Optional<StructureEntry> getStructureAt(Level world, int chunkX, int chunkZ) {
		return map.getEntryAt(dim(world), chunkX, chunkZ);
	}

	public void setGeneratedAt(Level world, int worldX, int worldZ, StructureEntry entry, boolean unique) {
		entry.setStructureMap(this);
		int cx = worldX >> 4;
		int cz = worldZ >> 4;
		String dimension = dim(world);
		setGeneratedAt(dimension, cx, cz, entry, unique);
	}

	public void setGeneratedAt(String dimension, int cx, int cz, StructureEntry entry, boolean unique) {
		entry.setStructureMap(this);
		map.setGeneratedAt(dimension, cx, cz, entry, unique);
		markDirty();
		NetworkHandler.sendToAllPlayers(new PacketStructureEntry(dimension, cx, cz, entry));
	}

	public boolean isGeneratedUnique(String name) {
		return this.map.generatedUniques.contains(name);
	}

	private static class StructureDimensionMap {
		private HashMap<String, StructureWorldMap> mapsByDimension = new HashMap<>();
		private Set<String> generatedUniques = new HashSet<>();

		private Collection<StructureEntry> getEntriesNear(String dimension, int chunkX, int chunkZ, int chunkRadius, boolean expandBySize, Collection<StructureEntry> list) {
			if (mapsByDimension.containsKey(dimension)) {
				return mapsByDimension.get(dimension).getEntriesNear(chunkX, chunkZ, chunkRadius, expandBySize, list);
			}
			return Collections.emptyList();
		}

		private Optional<StructureEntry> getEntryAt(String dimension, int chunkX, int chunkZ) {
			if (!mapsByDimension.containsKey(dimension)) {
				return Optional.empty();
			}
			return mapsByDimension.get(dimension).getEntryAt(chunkX, chunkZ);
		}

		private void setGeneratedAt(String dimension, int chunkX, int chunkZ, StructureEntry entry, boolean unique) {
			if (!this.mapsByDimension.containsKey(dimension)) {
				this.mapsByDimension.put(dimension, new StructureWorldMap());
			}
			this.mapsByDimension.get(dimension).setGeneratedAt(chunkX, chunkZ, entry);
			if (unique) {
				generatedUniques.add(entry.name);
			}
		}

		public void readFromNBT(StructureMap structureMap, CompoundTag nbttagcompound) {
			ListTag uniquesList = nbttagcompound.getList("uniques", Tag.TAG_STRING);
			ListTag dimensionList = nbttagcompound.getList("dimensions", Tag.TAG_COMPOUND);

			CompoundTag dimensionTag;
			String dim;
			for (int i = 0; i < dimensionList.size(); i++) {
				dimensionTag = dimensionList.getCompound(i);
				dim = dimensionTag.getString("dim");
				if (!this.mapsByDimension.containsKey(dim)) {
					this.mapsByDimension.put(dim, new StructureWorldMap());
				}
				this.mapsByDimension.get(dim).readFromNBT(structureMap, dimensionTag.getCompound("data"));
			}

			for (int i = 0; i < uniquesList.size(); i++) {
				generatedUniques.add(uniquesList.getString(i));
			}
		}

		public void writeToNBT(CompoundTag nbttagcompound) {
			ListTag dimensionsList = new ListTag();
			ListTag uniquesList = new ListTag();
			CompoundTag dimensionTag;
			CompoundTag dimensionData;
			for (Map.Entry<String, StructureWorldMap> dim : mapsByDimension.entrySet()) {
				dimensionTag = new CompoundTag();
				dimensionData = new CompoundTag();
				dimensionTag.putString("dim", dim.getKey());
				dim.getValue().writeToNBT(dimensionData);
				dimensionTag.put("data", dimensionData);
				dimensionsList.add(dimensionTag);
			}

			for (String name : this.generatedUniques) {
				uniquesList.add(StringTag.valueOf(name));
			}
			nbttagcompound.put("dimensions", dimensionsList);
			nbttagcompound.put("uniques", uniquesList);
		}
	}//end structure dimension map

	private static class StructureWorldMap {

		private HashMap<Integer, HashMap<Integer, StructureEntry>> worldMap = new HashMap<>();
		private int largestGeneratedX;
		private int largestGeneratedZ;

		public Collection<StructureEntry> getEntriesNear(int chunkX, int chunkZ, int chunkRadius, boolean expandBySize, Collection<StructureEntry> list) {
			StructureEntry entry;
			int crx = chunkRadius;
			int crz = chunkRadius;
			if (expandBySize) {
				crx += largestGeneratedX / 16;
				crz += largestGeneratedZ / 16;
			}
			for (int x = chunkX - crx; x <= chunkX + crx; x++) {
				if (worldMap.containsKey(x)) {
					for (int z = chunkZ - crz; z <= chunkZ + crz; z++) {
						entry = worldMap.get(x).get(z);
						if (entry != null) {
							list.add(entry);
						}
					}
				}
			}
			return list;
		}

		public void setGeneratedAt(int chunkX, int chunkZ, StructureEntry entry) {
			if (!this.worldMap.containsKey(chunkX)) {
				this.worldMap.put(chunkX, new HashMap<>());
			}
			this.worldMap.get(chunkX).put(chunkZ, entry);
			int x = entry.bb.getXSize();
			int z = entry.bb.getZSize();
			if (x > largestGeneratedX) {
				largestGeneratedX = x;
			}
			if (z > largestGeneratedZ) {
				largestGeneratedZ = z;
			}
		}

		public Optional<StructureEntry> getEntryAt(int chunkX, int chunkZ) {
			if (!worldMap.containsKey(chunkX)) {
				return Optional.empty();
			}
			return Optional.ofNullable(worldMap.get(chunkX).get(chunkZ));
		}

		public void readFromNBT(StructureMap structureMap, CompoundTag nbttagcompound) {
			ListTag entryList = nbttagcompound.getList("entries", Tag.TAG_COMPOUND);
			StructureEntry entry;
			CompoundTag entryTag;
			int x;
			int z;
			for (int i = 0; i < entryList.size(); i++) {
				entryTag = entryList.getCompound(i);
				x = entryTag.getInt("x");
				z = entryTag.getInt("z");
				entry = new StructureEntry();
				entry.setStructureMap(structureMap);
				entry.readFromNBT(entryTag);
				if (!this.worldMap.containsKey(x)) {
					this.worldMap.put(x, new HashMap<>());
				}
				this.worldMap.get(x).put(z, entry);
			}
			this.largestGeneratedX = nbttagcompound.getInt("largestX");
			this.largestGeneratedZ = nbttagcompound.getInt("largestZ");
		}

		public void writeToNBT(CompoundTag nbttagcompound) {
			ListTag entryList = new ListTag();
			CompoundTag entryTag;
			for (Map.Entry<Integer, HashMap<Integer, StructureEntry>> x : worldMap.entrySet()) {
				for (Map.Entry<Integer, StructureEntry> z : x.getValue().entrySet()) {
					entryTag = new CompoundTag();
					entryTag.putInt("x", x.getKey());
					entryTag.putInt("z", z.getKey());
					z.getValue().writeToNBT(entryTag);
					entryList.add(entryTag);
				}
			}
			nbttagcompound.putInt("largestX", largestGeneratedX);
			nbttagcompound.putInt("largestZ", largestGeneratedZ);
			nbttagcompound.put("entries", entryList);
		}
	}//end structure X Map

}
