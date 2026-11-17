package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.tile.TileStructureScanner;

import javax.annotation.Nullable;
import java.util.Optional;

public class BlockStructureScanner extends BlockBaseStructure implements EntityBlock {
	public static final DirectionProperty FACING = net.minecraft.world.level.block.DirectionalBlock.FACING;

	public BlockStructureScanner() {
		super(Material.WOOD.props(), "structure_scanner_block");
	}

	@Override
	public boolean onBlockActivated(Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		if (!world.isClientSide) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_SCANNER, pos);
		}
		return true;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getActualState(BlockState state, BlockGetter worldIn, BlockPos pos) {
		Optional<TileStructureScanner> tile = WorldTools.getTile(worldIn, pos, TileStructureScanner.class);
		return tile.map(tileStructureScanner -> state.setValue(FACING, tileStructureScanner.getRenderFacing())).orElse(state);
	}

	@Override
	public void onBlockPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		WorldTools.getTile(worldIn, pos, TileStructureScanner.class).ifPresent(t -> t.setFacing(placer.getDirection().getOpposite()));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileStructureScanner(pos, state);
	}

}
