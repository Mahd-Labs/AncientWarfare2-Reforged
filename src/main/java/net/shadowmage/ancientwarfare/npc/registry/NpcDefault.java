package net.shadowmage.ancientwarfare.npc.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;

import javax.annotation.concurrent.Immutable;
import java.util.Map;

@Immutable
public abstract class NpcDefault {
	protected Map<String, Double> attributes;
	protected int experienceDrop;
	protected boolean canSwim;
	protected boolean canBreakDoors;
	protected Map<Integer, Item> equipment;

	public NpcDefault(Map<String, Double> attributes, int experienceDrop, boolean canSwim, boolean canBreakDoors, Map<Integer, Item> equipment) {
		this.attributes = attributes;
		this.experienceDrop = experienceDrop;
		this.canSwim = canSwim;
		this.canBreakDoors = canBreakDoors;
		this.equipment = equipment;
	}

	public abstract NpcDefault setExperienceDrop(int experienceDrop);

	public abstract NpcDefault setCanSwim(boolean canSwim);

	public abstract NpcDefault setCanBreakDoors(boolean canBreakDoors);

	public abstract NpcDefault setAttributes(Map<String, Double> additionalAttributes);

	public abstract NpcDefault setEquipment(Map<Integer, Item> additionalEquipment);

	public int getExperienceDrop() {
		return experienceDrop;
	}

	public double getBaseHealth() {
		String healthKey = "generic.maxHealth";
		return (attributes.containsKey(healthKey)) ? attributes.get(healthKey) : 0;
	}

	public double getBaseAttack() {
		String attackKey = "generic.attackDamage";
		return (attributes.containsKey(attackKey)) ? attributes.get(attackKey) : 0;
	}

	public void applyAttributes(NpcBase npc) {
		attributes.forEach((name, value) -> applyAttribute(npc, name, value));
	}

	private void applyAttribute(NpcBase npc, String attributeName, double baseValue) {
		//Port notes: the registry json keeps the 1.12 attribute names (generic.maxHealth); 1.21 ids are snake case (generic.max_health)
		String id = attributeName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
		ResourceLocation key = ResourceLocation.tryParse(id);
		AttributeInstance attribute = key == null ? null : net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(key).map(npc::getAttribute).orElse(null);
		if (attribute != null) {
			attribute.setBaseValue(baseValue);

			if (attribute.getAttribute().is(Attributes.MAX_HEALTH)) {
				npc.setHealth((float) baseValue);
			}
		}
	}

	public void applyPathSettings(GroundPathNavigation navigator) {
		navigator.setCanFloat(canSwim);
		navigator.setCanOpenDoors(canBreakDoors);
	}

	public void applyEquipment(NpcBase npc) {
		equipment.forEach((slot, item) -> npc.setItemSlot(slot, new ItemStack(item)));
	}
}
