package net.shadowmage.ancientwarfare.automation.render.property;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.shadowmage.ancientwarfare.automation.block.TorqueTier;

/*
 * Port notes: TIER is the only real block state property here (1.12 meta 0/1/2). The 1.12 unlisted properties are
 * ModelProperty keys now (see core.render.property.IExtendedBlockState); they only carry tile data to the bakeries.
 * The unlisted properties 1.12 declared on the block classes (BlockTorqueTransportShaft.HAS_PREVIOUS / HAS_NEXT,
 * BlockTorqueTransportSided.CONNECTIONS, BlockFlywheelController.FLYWHEEL_ROTATION, BlockWindmillBlade.FORMED) live
 * here as well. ModelProperty is a plain NeoForge class, so this class is safe on a dedicated server (like
 * core CoreProperties).
 */
public class AutomationProperties {
	private AutomationProperties() {}

	public static final EnumProperty<TorqueTier> TIER = EnumProperty.create("tier", TorqueTier.class);

	public static final ModelProperty<Boolean> ACTIVE = new ModelProperty<>();
	public static final ModelProperty<Boolean> DYNAMIC = new ModelProperty<>();
	@SuppressWarnings("unchecked")
	public static final ModelProperty<Float>[] ROTATIONS = new ModelProperty[6];
	public static final ModelProperty<Boolean> USE_INPUT = new ModelProperty<>();
	public static final ModelProperty<Float> INPUT_ROTATION = new ModelProperty<>();
	public static final ModelProperty<Boolean> IS_CONTROL = new ModelProperty<>();
	public static final ModelProperty<Integer> HEIGHT = new ModelProperty<>();
	public static final ModelProperty<Integer> WIDTH = new ModelProperty<>();
	public static final ModelProperty<Float> ROTATION = new ModelProperty<>();

	public static final ModelProperty<Boolean> HAS_PREVIOUS = new ModelProperty<>();
	public static final ModelProperty<Boolean> HAS_NEXT = new ModelProperty<>();
	@SuppressWarnings("unchecked")
	public static final ModelProperty<Boolean>[] CONNECTIONS = new ModelProperty[6];
	public static final ModelProperty<Float> FLYWHEEL_ROTATION = new ModelProperty<>();
	public static final ModelProperty<Boolean> FORMED = new ModelProperty<>();

	static {
		for (Direction facing : Direction.values()) {
			ROTATIONS[facing.ordinal()] = new ModelProperty<>();
			CONNECTIONS[facing.ordinal()] = new ModelProperty<>();
		}
	}
}
