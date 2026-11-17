package net.shadowmage.ancientwarfare.automation.render;

import net.shadowmage.ancientwarfare.automation.block.BlockTorqueBase;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueBase;
import net.shadowmage.ancientwarfare.core.render.lib.CCModel;
import net.shadowmage.ancientwarfare.core.render.lib.CCRenderState;
import net.shadowmage.ancientwarfare.core.render.property.CoreProperties;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;
import net.shadowmage.ancientwarfare.core.util.WorldTools;



public abstract class BaseTorqueRenderer<T extends TileTorqueBase> extends AnimatedBlockRenderer {
	protected BaseTorqueRenderer(String modelPath) {
		super(modelPath);
	}

	@Override
	protected Collection<CCModel> applyModelTransforms(Collection<CCModel> modelGroups, Direction face, IExtendedBlockState state) {
		Set<CCModel> transformedGroups = Sets.newHashSet();

		Direction frontFacing = state.getValue(CoreProperties.UNLISTED_FACING);

		if (state.getValue(AutomationProperties.DYNAMIC)) {
			float[] rotations = new float[6];
			for (Direction facing : Direction.values()) {
				if (state.getValue(AutomationProperties.ROTATIONS[facing.ordinal()]) != null) {
					rotations[facing.ordinal()] = state.getValue(AutomationProperties.ROTATIONS[facing.ordinal()]);
				}
			}
			transformMovingParts(transformedGroups, frontFacing, rotations, state);
		} else {
			transformedGroups.addAll(rotateFacing(modelGroups, frontFacing));
		}

		return transformedGroups;
	}

	@Override
	protected void renderItemModels(CCRenderState ccrs, ItemStack stack) {
		super.renderItemModels(ccrs, stack);
		Set<CCModel> movingParts = Sets.newHashSet();
		transformMovingParts(movingParts, Direction.NORTH, new float[6], null);

		movingParts.forEach(m -> m.render(ccrs, getIconTransform(stack)));
	}

	protected abstract void transformMovingParts(Collection<CCModel> transformedGroups, Direction frontFacing, float[] rotations,
			@Nullable IExtendedBlockState state);

	@Override
	public IExtendedBlockState handleState(IExtendedBlockState state, BlockGetter access, BlockPos pos) {
		Direction facing = state.hasProperty(BlockTorqueBase.FACING) ? state.getValue(BlockTorqueBase.FACING) : Direction.NORTH;
		Optional<TileTorqueBase> tileentity = WorldTools.getTile(access, pos, TileTorqueBase.class);

		if (tileentity.isPresent()) {
			TileTorqueBase torquePart = tileentity.get();
			facing = torquePart.getPrimaryFacing();
		}

		IExtendedBlockState updatedState = state.withProperty(CoreProperties.UNLISTED_FACING, facing);
		updatedState = updatedState.withProperty(AutomationProperties.DYNAMIC, false);
		for (Direction f : Direction.values()) {
			updatedState = updatedState.withProperty(AutomationProperties.ROTATIONS[f.ordinal()], 0f);
		}

		if (tileentity.isPresent()) {
			//noinspection unchecked
			updatedState = handleAdditionalProperties(updatedState, (T) tileentity.get());
		}

		return updatedState;
	}

	protected IExtendedBlockState handleAdditionalProperties(IExtendedBlockState state, T tileEntity) {
		return state;
	}
}
