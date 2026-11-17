package net.shadowmage.ancientwarfare.structure.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.event.IBoxRenderer;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBuilderTicked;
import net.shadowmage.ancientwarfare.structure.tile.TileStructureBuilder;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBlockStructureBuilder extends ItemBlockBaseStructure implements IBoxRenderer {
	public static final String STRUCTURE_NAME_TAG = "structureName";

	public ItemBlockStructureBuilder(Block block) {
		super(block);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		String name = "corrupt_item";
		//noinspection ConstantConditions
		if (ItemTools.hasTag(stack) && ItemTools.getTag(stack).contains(STRUCTURE_NAME_TAG)) {
			name = ItemTools.getTag(stack).getString(STRUCTURE_NAME_TAG);
		}
		tooltip.add(Component.literal(net.shadowmage.ancientwarfare.structure.util.TextHelper.translate("guistrings.structure.structure_name") + ": " + name));
	}

	@Override
	public boolean placeBlockAt(ItemStack stack, Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, BlockState newState) {
		//noinspection ConstantConditions
		if (!ItemTools.hasTag(stack) || !ItemTools.getTag(stack).contains(STRUCTURE_NAME_TAG)) {
			return false;
		}
		boolean val = super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
		if (!world.isClientSide && val) {
			WorldTools.getTile(world, pos, TileStructureBuilder.class).ifPresent(t -> setupStructureBuilder(stack, player, world, pos, t));
		}
		return val;
	}

	@SuppressWarnings("ConstantConditions")
	private void setupStructureBuilder(ItemStack stack, Player player, Level world, BlockPos pos, TileStructureBuilder tb) {
		tb.setOwner(player);
		CompoundTag tag = ItemTools.getTag(stack);
		String name = tag.getString(STRUCTURE_NAME_TAG);
		Direction face = player.getDirection();
		setupStructureBuilder(world, pos, tb, name, face);
		if (tag.contains("progress")) {
			StructureBuilderTicked builder = tb.getBuilder();
			builder.deserializeProgressData(tag.getCompound("progress"));
		}
	}

	public void setupStructureBuilder(Level world, BlockPos pos, TileStructureBuilder tb, String name, Direction face) {
		StructureTemplateManager.getTemplate(name).ifPresent(t -> {
			BlockPos p = pos.relative(face, t.getSize().getZ() - 1 - t.getOffset().getZ() + 1);
			tb.setBuilder(new StructureBuilderTicked(world, t, face, p));
		});
	}

}
