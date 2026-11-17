package net.shadowmage.ancientwarfare.automation.tile.worksite.fruitfarm;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public interface IFruit {
	boolean matches(BlockState state);

	default boolean matches(ItemStack stack) {
		return false;
	}

	boolean isRipe(BlockState state);

	boolean pick(Level level, BlockState state, BlockPos pos, int fortune, IItemHandler inventory);

	boolean isPlantable();

	default boolean canPlant(Level level, BlockPos currentPos, BlockState state) {
		return false;
	}

	default boolean plant(Level level, BlockPos plantPos) {
		return false;
	}
}
