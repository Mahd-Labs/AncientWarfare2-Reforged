package net.shadowmage.ancientwarfare.automation.render;

import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;

import net.shadowmage.ancientwarfare.automation.tile.torque.multiblock.TileFlywheelStorage;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;



public class FlywheelStorageAnimationRenderer extends BaseAnimationRenderer<TileFlywheelStorage> {

	public FlywheelStorageAnimationRenderer() {
		super(FlywheelStorageRenderer.INSTANCE);
	}

	@Override
	protected IExtendedBlockState handleState(TileFlywheelStorage te, float partialTicks, IExtendedBlockState state) {
		state = state.withProperty(AutomationProperties.DYNAMIC, true);
		state = state.withProperty(AutomationProperties.IS_CONTROL, te.isControl);
		state = state.withProperty(AutomationProperties.WIDTH, te.setWidth);
		state = state.withProperty(AutomationProperties.HEIGHT, te.setHeight);
		state = state.withProperty(AutomationProperties.ROTATION, (float) te.rotation);

		return state;
	}
}
