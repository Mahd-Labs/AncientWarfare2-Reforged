package net.shadowmage.ancientwarfare.npc.orders;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.core.util.INBTSerializable;
import net.shadowmage.ancientwarfare.core.util.OrderingList;
import net.shadowmage.ancientwarfare.npc.item.ItemCombatOrder;

/*
 * Port notes: stack NBT lives in the custom data component (ItemTools); layout unchanged ("orders" compound with "dim"
 * and "pointList"). patrolDimensionId is a ResourceKey<Level> now ("dim" is written as the dimension id string, old
 * ints are accepted, see DimensionTag); getPatrolDimension() returns the key.
 */
public class CombatOrder extends OrderingList<BlockPos> implements INBTSerializable<CompoundTag> {

	ResourceKey<Level> patrolDimensionId = Level.OVERWORLD;

	public CombatOrder() {

	}

	public void addPatrolPoint(Level world, BlockPos point) {
		if (world.dimension() != patrolDimensionId) {
			clear();
			patrolDimensionId = world.dimension();
		}
		add(point);
	}

	public ResourceKey<Level> getPatrolDimension() {
		return patrolDimensionId;
	}

	public static CombatOrder getCombatOrder(ItemStack stack) {
		if (!stack.isEmpty() && stack.getItem() instanceof ItemCombatOrder) {
			CombatOrder order = new CombatOrder();
			if (ItemTools.hasTag(stack) && ItemTools.getTag(stack).contains("orders")) {
				order.deserializeNBT(ItemTools.getTag(stack).getCompound("orders"));
			}
			return order;
		}
		return null;
	}

	public void write(ItemStack stack) {
		if (!stack.isEmpty() && stack.getItem() instanceof ItemCombatOrder) {
			ItemTools.setTagInfo(stack, "orders", serializeNBT());
		}
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		DimensionTag.write(tag, "dim", patrolDimensionId);
		if (!isEmpty()) {
			//Port notes: 1.12 wrote a list of long tags. Item custom data goes through the NBT codecs in 1.21.1, which
			//turn such a list into a long array, so a long array is written (and both forms are read).
			long[] list = new long[points.size()];
			int i = 0;
			for (BlockPos point : points) {
				list[i++] = point.asLong();
			}
			tag.putLongArray("pointList", list);
		}
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		clear();
		patrolDimensionId = DimensionTag.read(tag, "dim");
		if (tag.contains("pointList", Tag.TAG_LONG_ARRAY)) {
			for (long point : tag.getLongArray("pointList")) {
				add(BlockPos.of(point));
			}
		} else if (tag.contains("pointList")) {
			ListTag list = tag.getList("pointList", Tag.TAG_LONG);
			for (int i = 0; i < list.size(); i++) {
				add(BlockPos.of(((LongTag) list.get(i)).getAsLong()));
			}
		}
	}
}
