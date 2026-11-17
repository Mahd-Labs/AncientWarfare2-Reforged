package net.shadowmage.ancientwarfare.automation.tile.warehouse2;

import net.neoforged.neoforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.automation.tile.ISidedItemHandlerTile;
import net.minecraft.core.BlockPos;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.shadowmage.ancientwarfare.automation.container.ContainerWarehouseInterface;
import net.shadowmage.ancientwarfare.core.interfaces.IInteractableTile;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.tile.IBlockBreakHandler;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TileWarehouseInterface extends TileControlled implements ISidedItemHandlerTile, IInteractableTile, IBlockBreakHandler {

	public TileWarehouseInterface(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.WAREHOUSE_INTERFACE_TILE.get(), pos, state);
	}

	private final ItemStackHandler inventory = new ItemStackHandler(9) {
		@Override
		protected void onContentsChanged(int slot) {
			markDirty();
			recalcRequests();
		}
	};

	private boolean init = false;
	private final List<InterfaceFillRequest> fillRequests = new ArrayList<>();
	private final List<InterfaceEmptyRequest> emptyRequests = new ArrayList<>();
	private List<WarehouseInterfaceFilter> filters = new ArrayList<>();
	private List<ContainerWarehouseInterface> viewers = new ArrayList<>();

	public void addViewer(ContainerWarehouseInterface viewer) {
		if (!(level != null) || level.isClientSide) {
			return;
		}
		viewers.add(viewer);
	}

	public void removeViewer(ContainerWarehouseInterface viewer) {
		viewers.remove(viewer);
	}

	private void updateViewers() {
		for (ContainerWarehouseInterface v : viewers) {
			v.onInterfaceFiltersChanged();
		}
	}

	public List<WarehouseInterfaceFilter> getFilters() {
		return filters;
	}

	public void setFilters(List<WarehouseInterfaceFilter> filters) {
		this.filters.clear();
		this.filters.addAll(filters);
		recalcRequests();
		updateViewers();
		markDirty();
	}

	@Override
	protected void updateTile() {
		if (level.isClientSide) {
			return;
		}
		if (!init) {
			init = true;
			recalcRequests();
		}
	}

	@Override
	public boolean onBlockClicked(Player player, @Nullable InteractionHand hand) {
		if (!player.level().isClientSide) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_WAREHOUSE_OUTPUT, worldPosition);
		}
		return true;
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		NBTHelper.deserializeHandler(inventory, tag.getCompound("inventory"));
		filters = NBTHelper.deserializeListFrom(tag, "filterList", WarehouseInterfaceFilter::new);
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		tag.put("inventory", NBTHelper.serializeHandler(inventory));
		NBTHelper.writeSerializablesTo(tag, "filterList", getFilters());
	}

	public void recalcRequests() {
		if (level.isClientSide) {
			return;
		}
		fillRequests.clear();
		emptyRequests.clear();
		for (int i = 0; i < inventory.getSlots(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (!matchesFilter(stack)) {
				emptyRequests.add(new InterfaceEmptyRequest(i, stack.getCount()));
			} else//matches, remove extras
			{
				int count = InventoryTools.getCountOf(inventory, stack);
				int max = getFilterQuantity(stack);
				if (count > max) {
					emptyRequests.add(new InterfaceEmptyRequest(i, count - max));
				}
			}
		}

		for (WarehouseInterfaceFilter filter : filters) {
			if (filter.getFilterItem().isEmpty()) {
				continue;
			}
			int count = InventoryTools.getCountOf(inventory, filter.getFilterItem());
			if (count < filter.getFilterQuantity()) {
				fillRequests.add(new InterfaceFillRequest(filter.getFilterItem().copy(), filter.getFilterQuantity() - count));
			}
		}
		getController().ifPresent(controller -> controller.onIterfaceInventoryChanged(this));
	}

	private boolean matchesFilter(ItemStack stack) {
		if (filters.isEmpty()) {
			return false;
		}
		for (WarehouseInterfaceFilter filter : filters) {
			if (filter.apply(stack)) {
				return true;
			}
		}
		return false;
	}

	private int getFilterQuantity(ItemStack stack) {
		int qty = 0;
		for (WarehouseInterfaceFilter filter : filters) {
			if (filter.apply(stack)) {
				qty += filter.getFilterQuantity();
			}
		}
		return qty;
	}

	public List<InterfaceFillRequest> getFillRequests() {
		return fillRequests;
	}

	public List<InterfaceEmptyRequest> getEmptyRequests() {
		return emptyRequests;
	}

	@Override
	public void onBlockBroken(BlockState state) {
		InventoryTools.dropItemsInWorld(level, inventory, worldPosition);
	}

	public static class InterfaceFillRequest {
		final ItemStack requestedItem;
		final int requestAmount;

		private InterfaceFillRequest(ItemStack item, int amount) {
			requestedItem = item;
			requestAmount = amount;
		}
	}

	public static class InterfaceEmptyRequest {
		final int slotNum;
		final int count;

		private InterfaceEmptyRequest(int slot, int count) {
			slotNum = slot;
			this.count = count;
		}
	}

	@Nullable
	@Override
	public IItemHandler getItemHandler(@Nullable Direction facing) {
		return inventory;
	}
}
