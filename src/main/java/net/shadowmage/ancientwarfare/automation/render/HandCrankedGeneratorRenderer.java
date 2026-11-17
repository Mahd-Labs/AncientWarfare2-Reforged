package net.shadowmage.ancientwarfare.automation.render;

import java.util.Collection;
import net.minecraft.core.Direction;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileHandCrankedGenerator;
import net.shadowmage.ancientwarfare.core.render.lib.CCModel;
import net.shadowmage.ancientwarfare.core.render.lib.Rotation;
import net.shadowmage.ancientwarfare.core.render.lib.Vector3;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;



public class HandCrankedGeneratorRenderer extends BaseTorqueRenderer<TileHandCrankedGenerator> {

	public static final HandCrankedGeneratorRenderer INSTANCE = new HandCrankedGeneratorRenderer();

	private final Collection<CCModel> outputGear;
	private final Collection<CCModel> inputGear;

	private HandCrankedGeneratorRenderer() {
		super("automation/hand_cranked_generator.obj");

		outputGear = removeGroups(s -> s.startsWith("base.outputGear."));
		inputGear = removeGroups(s -> s.startsWith("base.inputGear."));
	}

	@Override
	protected void transformMovingParts(Collection<CCModel> transformedGroups, Direction frontFacing, float[] rotations, IExtendedBlockState state) {
		float inR = rotations[Direction.UP.ordinal()];
		float outR = rotations[frontFacing.ordinal()];
		transformedGroups.addAll(rotateModels(outputGear, frontFacing, new Rotation(outR, 0, 0, 1).at(new Vector3(8d / 16d, 8d / 16d, 8d / 16d))));
		transformedGroups.addAll(rotateModels(inputGear, frontFacing, new Rotation(inR, 0, 1, 0).at(new Vector3(8d / 16d, 10.5d / 16d, 11.5d / 16d))));
	}
}