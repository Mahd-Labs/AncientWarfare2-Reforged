package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.structure.tile.TileDecorativeFlag;

import javax.annotation.Nullable;

public class BlockDecorativeFlag extends BlockFlag implements EntityBlock {
	public BlockDecorativeFlag() {
		//1.12 setHardness(2) + setResistance(5): explosion resistance 5 * 3 / 5
		super(Material.WOOD.props().strength(2.0F, 3.0F), "decorative_flag");
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileDecorativeFlag(pos, state);
	}

}
