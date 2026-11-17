package net.shadowmage.ancientwarfare.npc.orders;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.shadowmage.ancientwarfare.core.util.INBTSerializable;
import net.shadowmage.ancientwarfare.core.interfaces.IWorkSite;
import net.shadowmage.ancientwarfare.core.interfaces.IWorker;
import net.shadowmage.ancientwarfare.core.util.OrderingList;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.item.ItemWorkOrder;

import java.util.List;

/*
 * Port notes: stack NBT lives in the custom data component (ItemTools), layout unchanged. WorkEntry.dimension is a
 * ResourceKey<Level> ("dim" written as id string, old ints accepted, see DimensionTag).
 */
public class WorkOrder extends OrderingList<WorkOrder.WorkEntry> implements INBTSerializable<CompoundTag> {
	private static final int MAX_SIZE = 8;
	private WorkPriorityType priorityType = WorkPriorityType.ROUTE;
	private boolean nightShift;

	public boolean isNightShift() {
		return nightShift;
	}

	public void toggleShift() {
		nightShift = !nightShift;
	}

	public WorkPriorityType getPriorityType() {
		return priorityType;
	}

	public List<WorkEntry> getEntries() {
		return points;
	}

	//return true if successfully added
	public boolean addWorkPosition(Level world, BlockPos position) {
		if (position != null && size() < MAX_SIZE) {
			add(new WorkEntry(position, world.dimension(), 0));
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "Work Orders size: " + size() + " of type: " + priorityType;
	}

	public static WorkOrder getWorkOrder(ItemStack stack) {
		if (!stack.isEmpty() && stack.getItem() instanceof ItemWorkOrder) {
			WorkOrder order = new WorkOrder();
			//noinspection ConstantConditions
			if (ItemTools.hasTag(stack) && ItemTools.getTag(stack).contains("orders")) {
				order.deserializeNBT(ItemTools.getTag(stack).getCompound("orders"));
			}
			return order;
		}
		return null;
	}

	public void write(ItemStack stack) {
		if (!stack.isEmpty() && stack.getItem() instanceof ItemWorkOrder) {
			ItemTools.setTagInfo(stack, "orders", serializeNBT());
		}
	}

	public void togglePriority() {
		WorkPriorityType[] type = WorkPriorityType.values();
		priorityType = type[(priorityType.ordinal() + 1) % type.length];
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		ListTag entryList = new ListTag();
		for (WorkEntry entry : points) {
			entryList.add(entry.writeToNBT(new CompoundTag()));
		}
		tag.put("entryList", entryList);
		tag.putInt("priorityType", priorityType.ordinal());
		tag.putBoolean("nightShift", nightShift);
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		clear();
		ListTag entryList = tag.getList("entryList", Tag.TAG_COMPOUND);
		for (int i = 0; i < entryList.size(); i++) {
			add(new WorkEntry(entryList.getCompound(i)));
		}
		priorityType = WorkPriorityType.values()[tag.getInt("priorityType")];
		nightShift = tag.getBoolean("nightShift");
	}

	public static final class WorkEntry {

		private BlockPos position;
		private ResourceKey<Level> dimension;
		private int workLength;

		private WorkEntry(CompoundTag tag) {
			readFromNBT(tag);
		}//nbt constructor

		private WorkEntry(BlockPos position, ResourceKey<Level> dimension, int workLength) {
			this.setPos(position);
			this.dimension = dimension;
			this.setWorkLength(workLength);
		}

		public void readFromNBT(CompoundTag tag) {
			setPos(BlockPos.of(tag.getLong("pos")));
			dimension = DimensionTag.read(tag, "dim");
			setWorkLength(tag.getInt("length"));
		}

		public CompoundTag writeToNBT(CompoundTag tag) {
			tag.putLong("pos", blockPosition().asLong());
			DimensionTag.write(tag, "dim", dimension);
			tag.putInt("length", getWorkLength());
			return tag;
		}

		/*
		 * @return the block
		 */
		public Block getBlock(Level world) {
			return world.getBlockState(blockPosition()).getBlock();
		}

		/*
		 * @return the position
		 */
		public BlockPos blockPosition() {
			return position;
		}

		/*
		 * @param position the position to set
		 */
		public void setPos(BlockPos position) {
			this.position = position;
		}

		/*
		 * @return the workLength
		 */
		public int getWorkLength() {
			return workLength;
		}

		/*
		 * @param workLength the workLength to set
		 */
		public void setWorkLength(int workLength) {
			this.workLength = workLength;
		}
	}

	public enum WorkPriorityType {
		SITE_NEED {
			@Override
			public int getNextWorkIndex(int current, List<WorkEntry> orders, NpcBase npc) {
				for (int i = 0; i < orders.size(); i++) {
					if (WorldTools.getTile(npc.level(), orders.get(i).blockPosition(), IWorkSite.class)
							.filter(site -> ((IWorker) npc).canWorkAt(site.getWorkType()) && site.hasWork()).isPresent()) {
						return i;
					}
				}
				return 0;
			}
		}, ROUTE, TIMED;

		public int getNextWorkIndex(int current, List<WorkEntry> orders, NpcBase npcBase) {
			if (current + 1 >= orders.size()) {
				return 0;
			}
			return current + 1;
		}

		public boolean isTimed() {
			return this == TIMED;
		}
	}

}
