package net.shadowmage.ancientwarfare.structure.block.altar;

import net.minecraft.world.level.block.EntityBlock;
import net.shadowmage.ancientwarfare.structure.block.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.block.BlockBaseStructure;
import net.shadowmage.ancientwarfare.structure.tile.TileColored;

import javax.annotation.Nullable;

public class BlockAltarShortCloth extends BlockBaseStructure implements EntityBlock {
	public BlockAltarShortCloth() {
		super(Material.WOOD.props(), "altar_short_cloth");
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileColored(pos, state);
	}

	@Override
	public ItemStack getPickBlock(BlockState state, HitResult target, Level world, BlockPos pos, Player player) {
		return WorldTools.getTile(world, pos, TileColored.class).map(TileColored::getPickBlock).orElse(ItemStack.EMPTY);
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
		WorldTools.getTile(world, pos, TileColored.class).ifPresent(t -> drops.add(t.getPickBlock()));
	}
}
