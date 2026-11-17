package net.shadowmage.ancientwarfare.automation.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.block.TorqueTier;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.core.item.ItemBlockMeta;

import javax.annotation.Nullable;
import java.util.Map;

/*
 * Port addition: 1.12 used the plain ItemBlockMeta with meta 0/1/2 for the flywheel storage block. Flattened into one
 * item per tier (flywheel_storage_light / _medium / _heavy) that places the block with that tier state.
 */
public class ItemBlockFlywheelStorage extends ItemBlockMeta {
	private final TorqueTier tier;

	public ItemBlockFlywheelStorage(Block block, TorqueTier tier) {
		super(block, tier.getMeta());
		this.tier = tier;
	}

	public TorqueTier getTier() {
		return tier;
	}

	@Nullable
	@Override
	protected BlockState getPlacementState(BlockPlaceContext context) {
		BlockState state = super.getPlacementState(context);
		return state == null ? null : state.setValue(AutomationProperties.TIER, tier);
	}

	@Override
	public void registerBlocks(Map<Block, Item> blockToItemMap, Item item) {
		if (tier == TorqueTier.LIGHT) {
			super.registerBlocks(blockToItemMap, item);
		}
	}
}
