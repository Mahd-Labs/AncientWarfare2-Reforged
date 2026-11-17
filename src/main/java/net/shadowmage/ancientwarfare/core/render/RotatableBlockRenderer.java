package net.shadowmage.ancientwarfare.core.render;

import com.google.common.collect.Sets;
import net.minecraft.core.Direction;
import net.shadowmage.ancientwarfare.core.render.lib.CCModel;
import net.shadowmage.ancientwarfare.core.render.lib.Rotation;
import net.shadowmage.ancientwarfare.core.render.lib.Vector3;
import net.shadowmage.ancientwarfare.core.render.property.CoreProperties;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class RotatableBlockRenderer extends BaseBakery {

	protected RotatableBlockRenderer(String modelPath) {
		super(modelPath);
	}

	@Override
	protected Collection<CCModel> applyModelTransforms(Collection<CCModel> modelGroups, Direction face, IExtendedBlockState state) {
		Set<CCModel> transformedGroups = Sets.newHashSet();
		for (CCModel group : modelGroups) {
			transformedGroups.add(rotateFacing(group.copy(), getFacing(state)));
		}
		return transformedGroups;
	}

	protected Direction getFacing(IExtendedBlockState state) {
		return state.getValue(CoreProperties.UNLISTED_HORIZONTAL_FACING);
	}

	protected Collection<CCModel> rotateFacing(Collection<CCModel> groups, Direction frontFacing) {
		return groups.stream().map(e -> rotateFacing(e.copy(), frontFacing)).collect(Collectors.toSet());
	}

	protected CCModel rotateFacing(CCModel group, Direction frontFacing) {
		//1.12: getFrontOffsetY -> getStepY, getHorizontalIndex -> get2DDataValue (same numbering: S=0, W=1, N=2, E=3)
		double angle = Math.PI / 2d * (frontFacing.getAxis() == Direction.Axis.Y ? frontFacing.getStepY() : -((frontFacing.get2DDataValue() + 2) & 3));
		return group.apply(new Rotation(angle, frontFacing.getAxis() == Direction.Axis.Y ? 1 : 0, frontFacing.getAxis() != Direction.Axis.Y ? 1 : 0, 0).at(Vector3.center));
	}
}
