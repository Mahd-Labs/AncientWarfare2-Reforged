package net.shadowmage.ancientwarfare.structure.item;

import net.shadowmage.ancientwarfare.structure.block.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;
import net.shadowmage.ancientwarfare.structure.util.MultiBlockHelper;

public abstract class ItemBlockCoffin extends ItemBlockBaseStructure {
	public ItemBlockCoffin(Block block) {
		super(block);
	}

	@Override
	protected MultiBlockHelper.IPlaceChecker getPlaceChecker() {
		return this::mayPlace;
	}

	protected abstract boolean mayPlace(Level world, BlockPos pos, Direction sidePlacedOn, Player placer);

	protected static boolean mayPlaceAt(Level world, BlockPos pos, Direction sidePlacedOn, boolean checkSide) {
		return MultiBlockHelper.mayPlaceAt(world, pos);
	}
}
