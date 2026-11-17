package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockGravestone;
import net.shadowmage.ancientwarfare.structure.tile.TileGravestone;

import javax.annotation.Nullable;

import static net.shadowmage.ancientwarfare.core.render.property.CoreProperties.FACING;

public class BlockGravestone extends BlockBaseStructure implements EntityBlock {
	private static final AABB AABB_NORTH = new AABB(0, 0, 6 / 16D, 1, 19 / 16D, 10 / 16D);
	private static final AABB AABB_SOUTH = new AABB(0, 0, 6 / 16D, 1, 19 / 16D, 10 / 16D);
	private static final AABB AABB_WEST = new AABB(6 / 16D, 0, 0, 10 / 16D, 19 / 16D, 1D);
	private static final AABB AABB_EAST = new AABB(6 / 16D, 0, 0, 10 / 16D, 19 / 16D, 1D);

	public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 1, 8);

	public BlockGravestone() {
		super(Material.ROCK.props().noOcclusion().strength(4F).sound(SoundType.STONE), "gravestone");
	}

	@Override
	public BlockState getActualState(BlockState state, BlockGetter world, BlockPos pos) {
		return state.setValue(VARIANT, WorldTools.getTile(world, pos, TileGravestone.class).map(TileGravestone::getVariant).orElse(1));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(VARIANT, FACING);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileGravestone(pos, state);
	}

	@Override
	public void onBlockPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (!ItemTools.hasTag(stack)) {
			return;
		}

		world.setBlockAndUpdate(pos, state.setValue(VARIANT, ItemBlockGravestone.getVariant(stack)));
		WorldTools.getTile(world, pos, TileGravestone.class).ifPresent(te -> te.setPrimaryFacing(placer.getDirection().getOpposite()));
		WorldTools.getTile(world, pos, TileGravestone.class).ifPresent(te -> te.setVariant(ItemBlockGravestone.getVariant(stack)));
		super.onBlockPlacedBy(world, pos, state, placer, stack);
	}

	@Override
	public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
		if (!player.getAbilities().instabuild) {
			WorldTools.getTile(world, pos, TileGravestone.class)
					.ifPresent(te -> InventoryTools.dropItemInWorld(world, ItemBlockGravestone.getVariantStack(te.getVariant()), pos));
		}
		return super.playerWillDestroy(world, pos, state, player);
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
		//drops handled in onBlockHarvested
	}

	@Override
	public boolean onBlockActivated(Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		WorldTools.getTile(world, pos, TileGravestone.class).ifPresent(te -> te.activate(player)); // only runestones can be activated
		return true;
	}

	@Override
	public BlockState getStateForPlacement(Level worldIn, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, int meta, LivingEntity placer) {
		return defaultBlockState().setValue(FACING, placer.getDirection().getOpposite()).setValue(VARIANT, ItemBlockGravestone.getVariant(placer.getUseItem()));
	}

	@Override
	public AABB getBoundingBox(BlockState state, BlockGetter source, BlockPos pos) {
		switch (state.getValue(FACING)) {
			case NORTH:
				return AABB_NORTH;
			case SOUTH:
				return AABB_SOUTH;
			case WEST:
				return AABB_WEST;
			case EAST:
				return AABB_EAST;
			default:
		}
		return new AABB(0, 0, 6 / 16D, 1, 19 / 16D, 10 / 16D);
	}

	public void spawnPotionParticle() { // TODO: if we want the runestones to spawn some particles
	}
}