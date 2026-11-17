package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.EntityBlock;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.tile.SpawnerSettings;
import net.shadowmage.ancientwarfare.structure.tile.TileAdvancedSpawner;

import javax.annotation.Nullable;
import java.util.List;

public class BlockAdvancedSpawner extends BlockBaseStructure implements EntityBlock {
	public static final BooleanProperty TRANSPARENT = BooleanProperty.create("transparent");

	public BlockAdvancedSpawner() {
		super(Material.ROCK.props().noOcclusion().strength(2.f), "advanced_spawner");
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
		//no drops from spawner
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TRANSPARENT);
	}

	@Override
	public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		return net.shadowmage.ancientwarfare.core.tile.ITickableTile.ticker();
	}

	@Override
	public BlockState getActualState(BlockState state, BlockGetter world, BlockPos pos) {
		return super.getActualState(state, world, pos).setValue(TRANSPARENT, WorldTools.getTile(world, pos, TileAdvancedSpawner.class)
				.map(s -> s.getSettings().isTransparent()).orElse(false));
	}

	/*
	 * 1.12 addCollisionBoxToList: only entities that are already in the world collide with the spawner (so the
	 * entities it is about to spawn pass their collision checks)
	 */
	@Override
	protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
		if (context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext) {
			net.minecraft.world.entity.Entity entity = ((net.minecraft.world.phys.shapes.EntityCollisionContext) context).getEntity();
			if (entity != null && entity.isAddedToLevel()) {
				return super.getCollisionShape(state, world, pos, context);
			}
		}
		return net.minecraft.world.phys.shapes.Shapes.empty();
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileAdvancedSpawner(pos, state);
	}

	/* 1.12 getBlockHardness from the tile: the vanilla getDestroyProgress formula with the tile hardness */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
		float f = WorldTools.getTile(world, pos, TileAdvancedSpawner.class).map(TileAdvancedSpawner::getBlockHardness).orElse(state.getDestroySpeed(world, pos));
		if (f == -1.0F) {
			return 0.0F;
		}
		int i = net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(player, state, world, pos) ? 30 : 100;
		return player.getDigSpeed(state, pos) / f / (float) i;
	}

	@Override
	public ItemStack getPickBlock(BlockState state, HitResult target, Level world, BlockPos pos, Player player) {
		return WorldTools.getTile(world, pos, TileAdvancedSpawner.class).map(this::getSpawnerItem).orElse(super.getPickBlock(state, target, world, pos, player));
	}

	private ItemStack getSpawnerItem(TileAdvancedSpawner te) {
		ItemStack item = new ItemStack(this);
		CompoundTag settings = new CompoundTag();
		te.getSettings().writeToNBT(settings);
		ItemTools.setTagInfo(item,"spawnerSettings", settings);
		return item;
	}

	@Override
	public boolean onBlockActivated(Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		if (player.getAbilities().instabuild) {
			if (!world.isClientSide) {
				if (player.isShiftKeyDown()) {
					NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_SPAWNER_ADVANCED_BLOCK_INVENTORY, pos);
				} else {
					NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_SPAWNER_ADVANCED_BLOCK, pos);
				}
			}
			return true;
		}
		return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
	}

}
