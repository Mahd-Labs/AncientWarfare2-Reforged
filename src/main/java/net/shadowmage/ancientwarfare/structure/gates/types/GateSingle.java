package net.shadowmage.ancientwarfare.structure.gates.types;

import java.util.function.Supplier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.entity.EntityGate;

public class GateSingle extends Gate {
	/*
	 * @param id
	 * @param textureLocation
	 */
	public GateSingle(int id, String textureLocation, Supplier<SoundEvent> moveSound, Supplier<SoundEvent> hurtSound, Supplier<SoundEvent> breakSound, int maxHealth) {
		super(id, textureLocation, moveSound, hurtSound, breakSound, maxHealth);
	}

	@Override
	public void setInitialBounds(EntityGate gate, BlockPos pos1, BlockPos pos2) {
		BlockPos min = BlockTools.getMin(pos1, pos2);
		BlockPos max = BlockTools.getMax(pos1, pos2);
		boolean wideOnXAxis = min.getX() != max.getX();
		float width = wideOnXAxis ? max.getX() - min.getX() + 1 : max.getZ() - min.getZ() + 1;
		float xOffset = wideOnXAxis ? width * 0.5f : 0.5f;
		float zOffset = wideOnXAxis ? 0.5f : width * 0.5f;
		gate.setPositions(pos1, pos2);
		gate.setRenderBoundingBox(new AABB(Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max)));
		gate.edgeMax = width;
		gate.setPos(min.getX() + xOffset, min.getY(), min.getZ() + zOffset);
	}
}
