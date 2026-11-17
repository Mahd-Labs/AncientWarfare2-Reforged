package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableBlock;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableTile;
import net.shadowmage.ancientwarfare.core.owner.IOwnable;
import net.shadowmage.ancientwarfare.core.util.BlockTools;

/*
 * Port notes: translation key keeps the 1.12 meta suffix ("tile.<regName>.<meta>.name", meta 0 unless given).
 */
public class ItemBlockRotatableMetaTile extends ItemBlockBase {
	private IRotatableBlock rotatable;
	private final int meta;

	public ItemBlockRotatableMetaTile(Block block) {
		this(block, 0);
	}

	public ItemBlockRotatableMetaTile(Block block, int meta) {
		super(block);
		if (!(block instanceof IRotatableBlock)) {
			throw new IllegalArgumentException("Must be a rotatable block!!");
		}
		rotatable = (IRotatableBlock) block;
		this.meta = meta;
	}

	@Override
	protected boolean placeBlock(BlockPlaceContext context, BlockState newState) {
		boolean val = super.placeBlock(context, newState);
		Player player = context.getPlayer();
		if (val && player != null) {
			BlockEntity te = player.level().getBlockEntity(context.getClickedPos());
			if (te instanceof IOwnable) {
				((IOwnable) te).setOwner(player);
			}
			if (te instanceof IRotatableTile) {
				((IRotatableTile) te).setPrimaryFacing(BlockRotationHandler.getFaceForPlacement(player, rotatable, context.getClickedFace()));
			}
			BlockTools.notifyBlockUpdate(context.getLevel(), context.getClickedPos());
		}
		return val;
	}

	@Override
	public String getDescriptionId() {
		return ItemBlockMeta.getMetaDescriptionId(getBlock(), meta);
	}

	@Override
	public String getDescriptionId(ItemStack stack) {
		return getDescriptionId();
	}
}
