package net.shadowmage.ancientwarfare.automation.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.block.TorqueTier;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueBase;
import net.shadowmage.ancientwarfare.core.block.BlockBase;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableBlock;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;
import net.shadowmage.ancientwarfare.core.owner.IOwnable;

import javax.annotation.Nullable;
import java.util.Map;

/*
 * Port notes: 1.12 was one item with meta 0/1/2 for tiered blocks. Flattened: one item per tier
 * (<block>_light / _medium / _heavy) that places the block with that tier state; blocks without tiers keep a single
 * item (tier == null). The translation key keeps the 1.12 meta suffix ("tile.<regName>.<meta>.name").
 */
public class ItemBlockTorqueTile extends ItemBlockBase {

	private IRotatableBlock rotatable;
	@Nullable
	private final TorqueTier tier;

	public ItemBlockTorqueTile(Block block) {
		this(block, null);
	}

	public ItemBlockTorqueTile(Block block, @Nullable TorqueTier tier) {
		super(block);
		if (!(block instanceof IRotatableBlock)) {
			throw new IllegalArgumentException("Must be a rotatable block!!");
		}
		rotatable = (IRotatableBlock) block;
		this.tier = tier;
	}

	@Nullable
	public TorqueTier getTier() {
		return tier;
	}

	@Nullable
	@Override
	protected BlockState getPlacementState(BlockPlaceContext context) {
		BlockState state = super.getPlacementState(context);
		if (state != null && tier != null) {
			state = state.setValue(AutomationProperties.TIER, tier);
		}
		return state;
	}

	@Override
	protected boolean placeBlock(BlockPlaceContext context, BlockState newState) {
		boolean val = super.placeBlock(context, newState);
		Player player = context.getPlayer();
		if (val && player != null) {
			TileTorqueBase te = (TileTorqueBase) player.level().getBlockEntity(context.getClickedPos());
			if (te instanceof IOwnable) {
				((IOwnable) te).setOwner(player);
			}
			//noinspection ConstantConditions
			te.setPrimaryFacing(BlockRotationHandler.getFaceForPlacement(player, rotatable, context.getClickedFace()));
		}
		return val;
	}

	/*
	 * only the light (or only) item is the block's item (Block.asItem)
	 */
	@Override
	public void registerBlocks(Map<Block, Item> blockToItemMap, Item item) {
		if (tier == null || tier == TorqueTier.LIGHT) {
			super.registerBlocks(blockToItemMap, item);
		}
	}

	@Override
	public String getDescriptionId() {
		return "tile." + ((BlockBase) getBlock()).getRegName() + "." + (tier == null ? 0 : tier.getMeta()) + ".name";
	}

	@Override
	public String getDescriptionId(ItemStack stack) {
		return getDescriptionId();
	}
}
