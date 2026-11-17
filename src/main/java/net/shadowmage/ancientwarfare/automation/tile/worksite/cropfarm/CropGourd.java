package net.shadowmage.ancientwarfare.automation.tile.worksite.cropfarm;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockState;

public class CropGourd extends CropBreakOnly {
	@Override
	public boolean matches(BlockState state) {
		return (state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON) || state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN));
	}
}
