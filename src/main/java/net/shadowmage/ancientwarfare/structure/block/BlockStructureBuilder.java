package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.EntityBlock;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.util.NBTBuilder;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;
import net.shadowmage.ancientwarfare.structure.tile.TileStructureBuilder;

import javax.annotation.Nullable;
import java.util.Set;

public class BlockStructureBuilder extends BlockBaseStructure implements EntityBlock {

	private NonNullList<ItemStack> displayCache = null;

	public BlockStructureBuilder() {
		super(Material.ROCK.props().strength(2.f), "structure_builder_ticked");
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileStructureBuilder(pos, state);
	}

	@Override
	public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		return net.shadowmage.ancientwarfare.core.tile.ITickableTile.ticker();
	}

	@Override
	public boolean onBlockActivated(Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		if (!world.isClientSide) {
			WorldTools.getTile(world, pos, TileStructureBuilder.class).ifPresent(b -> b.onBlockClicked(player));
		}
		return true;
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
		ItemStack drop = new ItemStack(this);
		WorldTools.getTile(world, pos, TileStructureBuilder.class)
				.ifPresent(t -> ItemTools.setTag(drop, new NBTBuilder()
						.setString("structureName", t.getBuilder().getTemplate().name)
						.setTag("progress", t.getBuilder().serializeProgressData())
						.build()));
		drops.add(drop);
	}

	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
		//If it will harvest, delay deletion of the block until after getDrops
		return willHarvest || super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
	}

	@Override
	public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, BlockEntity te, ItemStack tool) {
		super.playerDestroy(world, player, pos, state, te, tool);
		world.removeBlock(pos, false);
	}

}
