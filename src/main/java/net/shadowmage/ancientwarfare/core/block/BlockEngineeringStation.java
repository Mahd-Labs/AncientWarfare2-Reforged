package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.tile.TileEngineeringStation;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

public class BlockEngineeringStation extends BlockBaseCore implements EntityBlock {
	/* the property 1.12 took from CoreProperties.FACING (BlockHorizontal.FACING) */
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public BlockEngineeringStation() {
		super(baseProperties().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).ignitedByLava().noOcclusion(), "engineering_station");
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEngineeringStation(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (!world.isClientSide) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_CRAFTING, pos);
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	public void breakBlock(Level world, BlockPos pos, BlockState state) {
		WorldTools.getTile(world, pos, TileEngineeringStation.class).ifPresent(TileEngineeringStation::onBlockBreak);
		super.breakBlock(world, pos, state);
	}

	@Override
	public boolean invertFacing() {
		return true;
	}

	@Override
	public String getDescriptionId() {
		return "tile.engineering_station.0.name";
	}
}
