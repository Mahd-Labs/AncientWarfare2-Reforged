package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.render.BlockStateKeyGenerator;
import net.shadowmage.ancientwarfare.core.util.ModelLoaderHelper;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.tile.TileSoundBlock;

/*
 * Port notes: the 1.12 unlisted "disguise" property + CCL bakery (the block was drawn with the model of the block
 * it is disguised as) is client only: the disguise state lives in TileSoundBlock and is drawn by the block entity
 * renderer registered in AncientWarfareStructureClient.
 */
public class BlockSoundBlock extends BlockBaseStructure implements EntityBlock {

	public BlockSoundBlock() {
		super(Material.ROCK.props(), "sound_block");
	}

	@Override
	public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		return net.shadowmage.ancientwarfare.core.tile.ITickableTile.ticker();
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileSoundBlock(pos, state);
	}

	@Override
	public boolean onBlockActivated(Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		ItemStack itemStack = player.getItemInHand(hand);
		if (!itemStack.isEmpty() && itemStack.getItem() instanceof BlockItem) {
			WorldTools.getTile(world, pos, TileSoundBlock.class).ifPresent(s -> s.setDisguiseState(itemStack));
		}
		if (!world.isClientSide) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_SOUND_BLOCK, pos);
		}
		return true;
	}

}
