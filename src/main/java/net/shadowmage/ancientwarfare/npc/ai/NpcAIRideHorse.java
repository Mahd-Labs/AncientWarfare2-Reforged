package net.shadowmage.ancientwarfare.npc.ai;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;

import java.util.ArrayList;
import java.util.List;

public class NpcAIRideHorse<T extends NpcBase> extends NpcAI<T> {
	private static final AttributeModifier FOLLOW_RANGE_MODIFIER = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("ancientwarfare", "modifier.npc_horse_path_extension"), 24.d, AttributeModifier.Operation.ADD_VALUE);
	private final AttributeModifier moveSpeedModifier;

	protected Mob horse;
	private final List<WrappedGoal> horseAI = new ArrayList<>();

	public NpcAIRideHorse(T npc, double speedFactor) {
		super(npc);
		moveSpeedModifier = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("ancientwarfare", "modifier.npc_ride_speed"), speedFactor, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	}

	@Override
	public boolean canUse() {
		return super.canUse() && shouldRideHorse();
	}

	protected boolean shouldRideHorse() {
		return horse == null && npc.getVehicle() instanceof Horse;
	}

	@Override
	public void start() {
		horse = (Mob) npc.getVehicle();
		onMountHorse();
	}

	protected void onMountHorse() {
		removeHorseAI();
		if (horse instanceof AbstractHorse) {
			AbstractHorse h = (AbstractHorse) horse;
			h.equipSaddle(true ? new ItemStack(Items.SADDLE) : ItemStack.EMPTY, null);
			h.setEating(false);
			h.setStanding(false);
		}
		applyModifiers();
	}

	public void onKilled() {
		if (horse != null) {
			onDismountHorse();
			horse = null;
		}
	}

	protected void onDismountHorse() {
		addHorseAI();
		if (horse instanceof AbstractHorse) {
			((AbstractHorse) horse).equipSaddle(true ? new ItemStack(Items.SADDLE) : ItemStack.EMPTY, null);
			removeModifiers();
		}
	}

	private void applyModifiers() {
		if (horse instanceof AbstractHorse) {
			removeModifiers();
			horse.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(moveSpeedModifier);
			horse.getAttribute(Attributes.FOLLOW_RANGE).addTransientModifier(FOLLOW_RANGE_MODIFIER);
		}
	}

	private void removeModifiers() {
		horse.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(moveSpeedModifier);
		horse.getAttribute(Attributes.FOLLOW_RANGE).removeModifier(FOLLOW_RANGE_MODIFIER);
	}

	private void removeHorseAI() {
		horseAI.clear();
		horseAI.addAll(horse.goalSelector.getAvailableGoals());
		for (WrappedGoal task : horseAI) {
			horse.goalSelector.removeGoal(task.getGoal());
		}
	}

	private void addHorseAI() {
		if (horse.goalSelector.getAvailableGoals().isEmpty()) {
			for (WrappedGoal task : horseAI) {
				horse.goalSelector.addGoal(task.getPriority(), task.getGoal());
			}
		}
		horseAI.clear();
	}
}
