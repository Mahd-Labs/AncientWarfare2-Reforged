package net.shadowmage.ancientwarfare.vehicle.helpers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.util.INBTSerializable;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.Function2;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFaction;
import net.shadowmage.ancientwarfare.npc.entity.vehicle.NpcSiegeEngineer;
import net.shadowmage.ancientwarfare.vehicle.AncientWarfareVehicles;
import net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.missiles.IAmmo;
import net.shadowmage.ancientwarfare.vehicle.network.PacketAmmoSelect;
import net.shadowmage.ancientwarfare.vehicle.network.PacketAmmoUpdate;
import net.shadowmage.ancientwarfare.vehicle.network.PacketSingleAmmoUpdate;
import net.shadowmage.ancientwarfare.vehicle.registry.AmmoRegistry;
import net.shadowmage.ancientwarfare.vehicle.registry.VehicleAmmoEntry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public class VehicleAmmoHelper implements INBTSerializable<CompoundTag> {

	private static final String CURRENT_AMMO_TYPE_TAG = "currentAmmoType";
	private VehicleBase vehicle;

	private ResourceLocation currentAmmoType = null;

	private NavigableMap<ResourceLocation, VehicleAmmoEntry> ammoEntries = new TreeMap<>();

	public VehicleAmmoHelper(VehicleBase vehicle) {
		this.vehicle = vehicle;
	}

	public int getCountOf(IAmmo type) {
		VehicleAmmoEntry ammo = ammoEntries.get(type.getRegistryName());
		return ammo == null ? 0 : ammo.ammoCount;
	}

	/**
	 * SERVER ONLY relays changes to clients to update a single ammo type, also handles updating underlying inventory...
	 */
	void decreaseCurrentAmmo() {
		if (vehicle.level().isClientSide) {
			return;
		}
		if (currentAmmoType != null && ammoEntries.containsKey(currentAmmoType)) {
			int removed = InventoryTools.removeItems(vehicle.inventory.ammoInventory, new ItemStack(AmmoRegistry.getItem(currentAmmoType)), 1).getCount();
			VehicleAmmoEntry entry = this.ammoEntries.get(this.currentAmmoType);
			int origCount = entry.ammoCount;
			entry.ammoCount -= removed;
			if (entry.ammoCount < 0) {
				entry.ammoCount = 0;
			}
			if (entry.ammoCount != origCount) {
				NetworkHandler.sendToAllTracking(vehicle, new PacketSingleAmmoUpdate(vehicle, currentAmmoType.toString(), entry.ammoCount));
			}
			selectLoadedAmmoIfEmpty();
		}
	}

	/*
	 * Improvement over 1.12 (config improvements.ammo_fallback): when the selected ammunition type is no longer in the
	 * weapon (used up or taken out), the next loaded type in the weapon's order is selected, and the clients are told
	 * (PacketAmmoSelect, as for a selection by key), so the overlay shows it. 1.12 stays on the empty type.
	 */
	public void selectLoadedAmmoIfEmpty() {
		if (!AWVehicleStatics.improvements.ammoFallback || vehicle.level().isClientSide || currentAmmoType == null || !ammoEntries.containsKey(currentAmmoType)) {
			return;
		}
		//never in the middle of a firing sequence: a hwacha barrage whose rockets run out stops, as in 1.12, and does
		//not carry on with another type (user report 2026-11-14: one trigger pull fired three rocket types)
		if (vehicle.firingHelper != null && vehicle.firingHelper.isInFiringSequence()) {
			return;
		}
		//"Empty" is what the weapon itself acts on: its count of the selected type (getCurrentAmmoCount), not the
		//inventory. After firing, that count is one lower than the inventory (kept 1.12 quirk: the last round of a stack
		//is not fired until the inventory is touched), so the weapon reports 0 while one round still lies in it. The
		//first version of this improvement looked at the inventory and therefore never switched after firing (user
		//report 2026-11-14: overlay on "Count: 0" with other rocket types loaded).
		if (ammoEntries.get(currentAmmoType).ammoCount > 0) {
			return;
		}
		Map.Entry<ResourceLocation, VehicleAmmoEntry> entry = getHigherWrapped(ammoEntries, currentAmmoType);
		while (!currentAmmoType.equals(entry.getKey())) {
			if (entry.getValue().ammoCount > 0) {
				updateSelectedAmmo(entry.getKey().toString());
				return;
			}
			entry = getHigherWrapped(ammoEntries, entry.getKey());
		}
	}

	/**
	 * get the current EFFECTIVE ammo count (not actual).  Soldiers use this to fool
	 * the vehicle into firing infinite rounds.
	 */
	public int getCurrentAmmoCount() {
		if (vehicle.getFirstPassenger() instanceof NpcFaction || AWVehicleStatics.generalSettings.ownedSoldiersUseAmmo && vehicle.getFirstPassenger() instanceof NpcSiegeEngineer) {
			return 64;
		}
		if (currentAmmoType != null && ammoEntries.containsKey(currentAmmoType)) {
			return this.ammoEntries.get(currentAmmoType).ammoCount;
		}
		return 0;
	}

	boolean doesntUseAmmo() {
		return vehicle.vehicleType.getValidAmmoTypes().isEmpty();
	}

	/* port addition: setVehicleType can run more than once (constructor default, then the real type) */
	public void clearUseableAmmo() {
		this.ammoEntries.clear();
	}

	public void addUseableAmmo(IAmmo ammo) {
		VehicleAmmoEntry ent = new VehicleAmmoEntry(ammo);
		this.ammoEntries.put(ammo.getRegistryName(), ent);
	}

	public void setNextAmmo() {
		getAvailable(this::getHigherWrapped).ifPresent(this::setCurrentAmmo);
	}

	private <K, V> Map.Entry<K, V> getHigherWrapped(NavigableMap<K, V> map, K key) {
		Map.Entry<K, V> entry = map.higherEntry(key);
		return entry == null ? map.firstEntry() : entry;
	}

	private void setCurrentAmmo(ResourceLocation registryName) {
		currentAmmoType = registryName;
		handleClientAmmoSelection(currentAmmoType);
	}

	private Optional<ResourceLocation> getAvailable(
			Function2<NavigableMap<ResourceLocation, VehicleAmmoEntry>, ResourceLocation, Map.Entry<ResourceLocation, VehicleAmmoEntry>> getNextEntry) {
		if (currentAmmoType == null) {
			return Optional.empty();
		}
		Map.Entry<ResourceLocation, VehicleAmmoEntry> entry = getNextEntry.apply(ammoEntries, currentAmmoType);
		while (entry.getValue().ammoCount <= 0 && !currentAmmoType.equals(entry.getKey())) {
			entry = getNextEntry.apply(ammoEntries, entry.getKey());
		}
		return Optional.of(entry.getKey());
	}

	private <K, V> Map.Entry<K, V> getLowerWrapped(NavigableMap<K, V> map, K key) {
		Map.Entry<K, V> entry = map.lowerEntry(key);
		return entry == null ? map.lastEntry() : entry;
	}

	public void setPreviousAmmo() {
		getAvailable(this::getLowerWrapped).ifPresent(this::setCurrentAmmo);
	}

	public void handleClientAmmoSelection(ResourceLocation ammoRegistryName) {
		NetworkHandler.sendToServer(new PacketAmmoSelect(vehicle, ammoRegistryName.toString()));
	}

	public void updateSelectedAmmo(String ammoRegistryName) {
		if (!ammoRegistryName.equals(currentAmmoType == null ? "" : currentAmmoType.toString()) && net.shadowmage.ancientwarfare.core.util.RegistryTools.parseOrNull(ammoRegistryName) != null) {
			this.currentAmmoType = net.shadowmage.ancientwarfare.core.util.RegistryTools.parseOrNull(ammoRegistryName);
			if (!vehicle.level().isClientSide) {
				NetworkHandler.sendToAllTracking(vehicle, new PacketAmmoSelect(vehicle, ammoRegistryName));
			}
			float maxPower = vehicle.firingHelper.getAdjustedMaxMissileVelocity();
			if (!vehicle.canAimPower()) {
				vehicle.localLaunchPower = maxPower;
			} else if (vehicle.localLaunchPower > maxPower) {
				vehicle.localLaunchPower = maxPower;
				if (vehicle.level().isClientSide && vehicle.firingHelper.clientLaunchSpeed > maxPower) {
					vehicle.firingHelper.clientLaunchSpeed = maxPower;
				}
			}
		}
	}

	/*
	 * Port addition: ammo names saved by 1.12 (structure templates) carry the old mod id "ancientwarfarevehicle".
	 */
	private static ResourceLocation parseAmmoName(String registryName) {
		ResourceLocation rl = net.shadowmage.ancientwarfare.core.util.RegistryTools.parseOrNull(registryName);
		return rl == null ? null : "ancientwarfarevehicle".equals(rl.getNamespace()) ? ResourceLocation.fromNamespaceAndPath(AncientWarfareVehicles.MOD_ID, rl.getPath()) : rl;
	}

	public void updateAmmoCount(String registryName, int count) {
		ResourceLocation rl = parseAmmoName(registryName);
		updateAmmoCount(rl, count);
	}

	private void updateAmmoCount(ResourceLocation registryNameLocation, int count) {
		if (ammoEntries.containsKey(registryNameLocation)) {
			this.ammoEntries.get(registryNameLocation).ammoCount = count;
		}
	}

	public void updateAmmoCounts() {
		if (vehicle.level().isClientSide) {
			return;
		}

		for (VehicleAmmoEntry ent : this.ammoEntries.values()) {
			ent.ammoCount = 0;
		}
		List<VehicleAmmoEntry> counts = vehicle.inventory.getAmmoCounts();

		for (VehicleAmmoEntry count : counts) {
			updateAmmoCount(count.baseAmmoType.getRegistryName(), count.ammoCount);
			if (currentAmmoType == null && count.ammoCount > 0) {
				updateSelectedAmmo(count.baseAmmoType.getRegistryName().toString());
			}
		}
		selectLoadedAmmoIfEmpty();
		PacketAmmoUpdate pkt = new PacketAmmoUpdate(vehicle, serializeAmmo(new CompoundTag()));
		NetworkHandler.sendToAllTracking(vehicle, pkt);
	}

	//TODO replace with optional
	public IAmmo getCurrentAmmoType() {
		if (currentAmmoType != null && ammoEntries.containsKey(currentAmmoType)) {
			VehicleAmmoEntry entry = this.ammoEntries.get(currentAmmoType);
			if (entry != null && (!(vehicle.getFirstPassenger() instanceof NpcBase) || entry.ammoCount > 0)) {
				return entry.baseAmmoType;
			}
		}
		if (vehicle.getFirstPassenger() instanceof NpcFaction || !AWVehicleStatics.generalSettings.ownedSoldiersUseAmmo && vehicle.getFirstPassenger() instanceof NpcSiegeEngineer) {
			NpcBase npc = (NpcBase) vehicle.getFirstPassenger();
			return vehicle.vehicleType.getAmmoForSoldierRank(npc.getLevelingStats().getLevel());
		}
		return null;
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		if (currentAmmoType != null) {
			tag.putString(CURRENT_AMMO_TYPE_TAG, currentAmmoType.toString());
		}
		serializeAmmo(tag);
		return tag;
	}

	private CompoundTag serializeAmmo(CompoundTag tag) {
		ListTag tagList = new ListTag();
		for (VehicleAmmoEntry ent : this.ammoEntries.values()) {
			CompoundTag entryTag = new CompoundTag();
			entryTag.putString("type", ent.baseAmmoType.getRegistryName().toString());
			entryTag.putInt("count", ent.ammoCount);
			tagList.add(entryTag);
		}
		tag.put("list", tagList);
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		for (VehicleAmmoEntry ent : this.ammoEntries.values()) {
			ent.ammoCount = 0;
		}
		if (tag.contains(CURRENT_AMMO_TYPE_TAG)) {
			currentAmmoType = parseAmmoName(tag.getString(CURRENT_AMMO_TYPE_TAG));
		}
		deserializeAmmo(tag);
	}

	public void updateAmmo(CompoundTag tag) {
		for (VehicleAmmoEntry ent : this.ammoEntries.values()) {
			ent.ammoCount = 0;
		}
		deserializeAmmo(tag);
	}

	private void deserializeAmmo(CompoundTag tag) {
		ListTag ammo = tag.getList("list", Tag.TAG_COMPOUND);
		for (int i = 0; i < ammo.size(); i++) {
			CompoundTag entryTag = (CompoundTag) ammo.get(i);
			String type = entryTag.getString("type");
			int count = entryTag.getInt("count");
			updateAmmoCount(type, count);
		}
	}

}
