package net.shadowmage.ancientwarfare.automation.tile.warehouse2;

import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.shadowmage.ancientwarfare.automation.AncientWarfareAutomation;
import net.shadowmage.ancientwarfare.core.inventory.ItemHashEntry;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class WarehouseDebugger {
	private static final String DEBUGGER_DATA_NAME = "AWWarehouseDebuggerData";

	@SubscribeEvent
	public void tick(LevelTickEvent.Post event) {
			getData(event.getLevel()).ifPresent(data -> tickData(event.getLevel(), data));
	}

	private static void tickData(Level level, WarehouseDebuggerData data) {
		if (data.decreaseAndCheckCooldown()) {
			reviewInventories(level, data);
		}
	}

	private static void reviewInventories(Level level, WarehouseDebuggerData data) {
		Set<BlockPos> invalidWarehouses = new HashSet<>();
		for (BlockPos warehousePos : data.getTrackedWarehouses()) {
			if (level.isLoaded(warehousePos)) {
				Optional<TileWarehouse> wh = WorldTools.getTile(level, warehousePos, TileWarehouse.class);

				if (!wh.isPresent()) {
					invalidWarehouses.add(warehousePos);
					continue;
				}

				reviewInventory(wh.get(), data.getItemCounts(warehousePos));
			}
		}

		invalidWarehouses.forEach(data::removeTrackedWarehouse);
	}

	private static void reviewInventory(TileWarehouse warehouse, Map<ItemStack, Integer> debugData) {
		HashMap<ItemStack, Integer> copy = new HashMap<>(debugData);
		for (Map.Entry<ItemHashEntry, Integer> entry : warehouse.cachedItemMap.getItemCounts().entrySet()) {
			reviewStackCount(warehouse.getBlockPos(), debugData, copy, entry);
		}

		for (Map.Entry<ItemStack, Integer> entry : copy.entrySet()) {
			AncientWarfareAutomation.LOG.info("Warehouse {} misses {} of {} that is in debugData", warehouse.getBlockPos(), entry.getValue(),
					NBTHelper.writeItemStack(entry.getKey()).toString());
			debugData.remove(entry.getKey());
		}
	}

	private static void reviewStackCount(BlockPos warehousePos, Map<ItemStack, Integer> debugData, Map<ItemStack, Integer> debugDataCopy, Map.Entry<ItemHashEntry, Integer> warehouseEntry) {
		ItemStack warehouseStack = warehouseEntry.getKey().getItemStack();
		int warehouseCount = warehouseEntry.getValue();

		Iterator<Map.Entry<ItemStack, Integer>> it = debugDataCopy.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<ItemStack, Integer> entry = it.next();
			if (InventoryTools.doItemStacksMatch(warehouseStack, entry.getKey())) {
				if (warehouseCount != entry.getValue()) {
					AncientWarfareAutomation.LOG.info("Warehouse {} has {} {} of {} than what's tracked in debug data", warehousePos,
							Math.abs(entry.getValue() - warehouseCount), warehouseCount > entry.getValue() ? "more" : "less",
							NBTHelper.writeItemStack(warehouseStack).toString());
					debugData.put(entry.getKey(), warehouseCount);
				}
				it.remove();
				return;
			}
		}

		AncientWarfareAutomation.LOG.info("Warehouse {} has {} of {} which is missing in debug data", warehousePos, warehouseCount,
				NBTHelper.writeItemStack(warehouseStack).toString());
		debugData.put(warehouseStack, warehouseCount);
	}

	public static void startTrackingWarehouse(Level level, BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return;
		}
		getData(level).ifPresent(data -> {
			data.addTrackedWarehouse(pos);
			initializeItemCounts(level, pos, data);
		});
	}

	private static void initializeItemCounts(Level level, BlockPos pos, WarehouseDebuggerData debugData) {
		WorldTools.getTile(level, pos, TileWarehouse.class).ifPresent((TileWarehouse wh) -> {
			Map<ItemStack, Integer> warehouseItems = new HashMap<>();
			for (Map.Entry<ItemHashEntry, Integer> itemCount : wh.cachedItemMap.getItemCounts().entrySet()) {
				warehouseItems.put(itemCount.getKey().getItemStack().copy(), itemCount.getValue());
			}
			debugData.setItemCounts(pos, warehouseItems);
		});
	}

	public static void stopTrackingWarehouse(Level level, BlockPos pos) {
		getData(level).ifPresent(data -> data.removeTrackedWarehouse(pos));
	}

	public static void removeItem(Level level, BlockPos pos, ItemStack stack, int count) {
		getData(level).ifPresent(data -> data.removeItem(pos, stack, count));
	}

	public static void addItem(Level level, BlockPos pos, ItemStack stack, int count) {
		getData(level).ifPresent(data -> data.addItem(pos, stack, count));
	}

	public static void reinitializeItemCounts(Level level, BlockPos pos) {
		getData(level).ifPresent(data -> {
			if (data.isTrackingWarehouse(pos)) {
				data.removeTrackedWarehouse(pos);
				data.addTrackedWarehouse(pos);
				initializeItemCounts(level, pos, data);
			}
		});
	}

	private static Optional<WarehouseDebuggerData> getData(Level level) {
		return WorldTools.getWorldSavedData(level, WarehouseDebuggerData.class, DEBUGGER_DATA_NAME, true);
	}

	public static void changeItemQuantity(Level level, BlockPos pos, ItemStack stack, int change) {
		getData(level).ifPresent(data -> {
			if (change > 0) {
				data.addItem(pos, stack, change);
			} else if (change < 0) {
				data.removeItem(pos, stack, -change);
			}
		});
	}
}
