package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.shadowmage.ancientwarfare.core.util.NBTBuilder;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.tile.TileFlag;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;

public class BlockFlag extends BlockBaseStructure {
	private static final AABB STANDING_AABB = new AABB(0.25D, 0.0D, 0.25D, 0.75D, 1.0D, 0.75D);
	public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 15);

	private Set<BlockFlag.FlagDefinition> flagDefinitions = new LinkedHashSet<>();

	BlockFlag(BlockBehaviour.Properties material, String regname) {
		super(material.noOcclusion().sound(SoundType.WOOD), regname);
	}

	@Override
	public AABB getCollisionBoundingBox(BlockState blockState, BlockGetter worldIn, BlockPos pos) {
		return NULL_AABB;
	}

	@Override
	public boolean isPossibleToRespawnInThis(BlockState state) {
		return true;
	}

	@Override
	public AABB getBoundingBox(BlockState state, BlockGetter source, BlockPos pos) {
		return STANDING_AABB;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(ROTATION, rot.rotate(state.getValue(ROTATION), 16));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.setValue(ROTATION, mirrorIn.mirror(state.getValue(ROTATION), 16));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ROTATION);
	}

	@Override
	public void playerDestroy(Level worldIn, Player player, BlockPos pos, BlockState state, BlockEntity te, ItemStack stack) {
		if (te instanceof TileFlag) {
			Block.popResource(worldIn, pos, ((TileFlag) te).getItemStack());
		} else {
			super.playerDestroy(worldIn, player, pos, state, te, stack);
		}
	}

	@Override
	public ItemStack getPickBlock(BlockState state, HitResult target, Level world, BlockPos pos, Player player) {
		return WorldTools.getTile(world, pos, TileFlag.class).map(TileFlag::getItemStack).orElse(new ItemStack(this));
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
		drops.add(WorldTools.getTile(world, pos, TileFlag.class).map(TileFlag::getItemStack).orElse(new ItemStack(this)));
	}

	@Override
	public void onBlockPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		WorldTools.getTile(world, pos, TileFlag.class).ifPresent(te -> te.setFromStack(stack));
	}

	@Override
	public BlockState getStateForPlacement(Level worldIn, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, int meta, LivingEntity placer) {
		int rotation = Mth.floor((double) ((placer.getYRot() + 180.0F) * 16.0F / 360.0F) + 0.5D) & 15;
		return defaultBlockState().setValue(ROTATION, rotation);
	}

	public Set<BlockFlag.FlagDefinition> getFlagDefinitions() {
		return flagDefinitions;
	}

	public void addFlagDefinition(BlockFlag.FlagDefinition flagDefinition) {
		flagDefinitions.add(flagDefinition);
	}

	public static class FlagDefinition {

		private String name;

		public FlagDefinition(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
		return true; //1.12 isPassable: true
	}
}
