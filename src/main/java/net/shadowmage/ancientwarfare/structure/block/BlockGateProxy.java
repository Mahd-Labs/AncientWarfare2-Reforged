package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.tile.TEGateProxy;

/*
 * Port notes: 1.12 BlockContainer(Material.ROCK) + setBlockUnbreakable + setResistance(6000000), no creative tab.
 * The 1.12 block was drawn by GateProxyRenderer (nothing but the break particles): RenderShape.INVISIBLE here.
 * The zero thickness collision planes are kept (BlockBaseStructure.shapeOf gives them a minimal thickness, a
 * VoxelShape cannot be flat).
 */
public final class BlockGateProxy extends BlockBaseStructure implements EntityBlock {
	private static final AABB Z_AXIS_AABB = new AABB(8D / 16D, 0, 0, 8D / 16D, 1, 1);
	private static final AABB X_AXIS_AABB = new AABB(0, 0, 8D / 16D, 1, 1, 8D / 16D);

	private static final AABB NO_AABB = new AABB(0, 0, 0, 0, 0, 0);

	public BlockGateProxy() {
		super(Material.ROCK.props().noOcclusion().strength(-1.0F, 3600000.0F).noLootTable(), "gate_proxy");
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TEGateProxy(pos, state);
	}

	@Override
	public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		return net.shadowmage.ancientwarfare.core.tile.ITickableTile.ticker();
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
		//nothing gets dropped
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public AABB getCollisionBoundingBox(BlockState state, BlockGetter world, BlockPos pos) {
		return WorldTools.getTile(world, pos, TEGateProxy.class).map(TEGateProxy::isOpen).orElse(false) ? NULL_AABB :
				getCorrectAxisAABB(world, pos);
	}

	private AABB getCorrectAxisAABB(BlockGetter world, BlockPos pos) {
		return (world.getBlockState(pos.relative(Direction.WEST)).getBlock() == this || world.getBlockState(pos.relative(Direction.EAST)).getBlock() == this)
				? X_AXIS_AABB : Z_AXIS_AABB;
	}

	//Actually "can go through", for mob pathing
	@Override
	public AABB getBoundingBox(BlockState state, BlockGetter source, BlockPos pos) {
		return WorldTools.getTile(source, pos, TEGateProxy.class).map(TEGateProxy::isOpen).orElse(false) ? NO_AABB : FULL_BLOCK_AABB;
	}

	@Override
	public boolean canEntityDestroy(BlockState state, BlockGetter world, BlockPos pos, Entity entity) {
		return false;
	}

	@Override
	protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
		return pathfindableByShape(state, type); //1.12 isPassable: while the gate is open; the shape follows the gate
	}
}
