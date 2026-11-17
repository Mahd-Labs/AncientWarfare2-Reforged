package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.init.AWCoreItems;

/*
 * Port notes: flattened, see ItemMulti. Registry names (AWCoreItems.COMPONENTS, index = 1.12 meta):
 * component_wooden_gear, component_iron_gear, component_steel_gear, component_wooden_bearings,
 * component_iron_bearings, component_steel_bearings, component_wooden_shaft, component_iron_shaft,
 * component_steel_shaft.
 */
public class ItemComponent extends ItemMulti {

	public static final int WOODEN_GEAR_SET = 0;
	public static final int IRON_GEAR_SET = 1;
	public static final int STEEL_GEAR_SET = 2;
	public static final int WOODEN_BEARINGS = 3;
	public static final int IRON_BEARINGS = 4;
	public static final int STEEL_BEARINGS = 5;
	public static final int WOODEN_TORQUE_SHAFT = 6;
	public static final int IRON_TORQUE_SHAFT = 7;
	public static final int STEEL_TORQUE_SHAFT = 8;

	public static final String[] SUB_NAMES = {"wooden_gear", "iron_gear", "steel_gear", "wooden_bearings", "iron_bearings", "steel_bearings", "wooden_shaft", "iron_shaft", "steel_shaft"};

	public ItemComponent(int num) {
		super(AncientWarfareCore.MOD_ID, "component", num);
	}

	/*
	 * 1.12 ItemMulti.getSubItem(num)
	 */
	public static ItemStack getSubItem(int num) {
		return new ItemStack(AWCoreItems.COMPONENTS.get(num).get());
	}
}
