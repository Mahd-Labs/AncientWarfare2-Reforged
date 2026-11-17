package net.shadowmage.ancientwarfare.core.render.property;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/*
 * Port note: 1.12 IUnlistedProperty -> NeoForge ModelProperty (carried in IExtendedBlockState / ModelData).
 * This class is safe to load on a dedicated server (ModelProperty is a plain value class without client references),
 * so blocks may keep using CoreProperties.FACING / VISIBLE.
 */
public class CoreProperties {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final ModelProperty<Direction> UNLISTED_HORIZONTAL_FACING = new ModelProperty<>();
	public static final ModelProperty<Direction> UNLISTED_FACING = new ModelProperty<>();
	public static final BooleanProperty VISIBLE = BooleanProperty.create("visible");
}
