package net.shadowmage.ancientwarfare.core.tile;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/*
 * Port replacement for 1.12 ITickable on tile entities. update() runs on both sides, like 1.12.
 * Blocks return ITickableTile.ticker() from EntityBlock.getTicker.
 */
public interface ITickableTile {
	void update();

	@Nullable
	static <T extends BlockEntity> BlockEntityTicker<T> ticker() {
		return (level, pos, state, be) -> {
			if (be instanceof ITickableTile) {
				((ITickableTile) be).update();
			}
		};
	}

	@Nullable
	static <T extends BlockEntity> BlockEntityTicker<T> ticker(Level level, BlockState state, BlockEntityType<T> type) {
		return ticker();
	}
}
