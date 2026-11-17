package net.shadowmage.ancientwarfare.structure.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.input.InputHandler;
import net.shadowmage.ancientwarfare.core.input.IItemKeyInterface;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.event.IBoxRenderer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class ItemConstructionTool extends ItemBaseStructure implements IItemKeyInterface, IBoxRenderer {

	public ItemConstructionTool(String name) {
		super(name, new Item.Properties().stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		ConstructionSettings settings = getSettings(stack);
		if (settings == null) {
			return;
		}
		String text = net.shadowmage.ancientwarfare.structure.util.TextHelper.translate("guistrings.construction.mode") + ": " + settings.type;
		tooltip.add(Component.literal(text));
		text = net.shadowmage.ancientwarfare.structure.util.TextHelper.translate("guistrings.construction.fill_block") + ": " + BuiltInRegistries.BLOCK.getKey(settings.block);
		tooltip.add(Component.literal(text));

		String keyText;
		text = "RMB" + " = " + net.shadowmage.ancientwarfare.structure.util.TextHelper.translate("guistrings.construction.do_action");
		tooltip.add(Component.literal(text));

		keyText = net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().getKeyName(1);
		text = keyText + " = " + net.shadowmage.ancientwarfare.structure.util.TextHelper.translate("guistrings.construction.toggle_mode");
		tooltip.add(Component.literal(text));

		keyText = net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().getKeyName(2);
		text = keyText + " = " + net.shadowmage.ancientwarfare.structure.util.TextHelper.translate("guistrings.construction.set_fill_block");
		tooltip.add(Component.literal(text));

		keyText = net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().getKeyName(3);
		text = keyText + " = " + net.shadowmage.ancientwarfare.structure.util.TextHelper.translate("guistrings.construction.set_pos_1");
		tooltip.add(Component.literal(text));

		keyText = net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().getKeyName(4);
		text = keyText + " = " + net.shadowmage.ancientwarfare.structure.util.TextHelper.translate("guistrings.construction.set_pos_2");
		tooltip.add(Component.literal(text));

		keyText = net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().getKeyName(5);
		text = keyText + " = " + net.shadowmage.ancientwarfare.structure.util.TextHelper.translate("guistrings.construction.clear_positions");
		tooltip.add(Component.literal(text));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (world.isClientSide) {
			return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
		}
		ConstructionSettings settings = getSettings(stack);
		if (settings == null) {
			return new InteractionResultHolder<>(InteractionResult.PASS, stack);
		}
		switch (settings.type) {
			case SOLID_FILL: {
				handleSolidFill(player, settings);
			}
			break;
			case BOX_FILL: {
				handleBoxFill(player, settings);
			}
			break;
			case LAKE_FILL: {
				handleLakeFill(player, settings);
			}
			break;
			case LAYER_FILL: {
				handleLayerFill(player, settings);
			}
			break;
		}
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
	}

	//TODO change all settings.block, settings.meta to use blockstate if possible
	private void handleSolidFill(Player player, ConstructionSettings settings) {
		if (settings.pos1 != null && settings.pos2 != null && settings.block != null) {
			BlockPos min = BlockTools.getMin(settings.pos1, settings.pos2);
			BlockPos max = BlockTools.getMax(settings.pos1, settings.pos2);
			for (int x = min.getX(); x <= max.getX(); x++) {
				for (int z = min.getZ(); z <= max.getZ(); z++) {
					for (int y = min.getY(); y <= max.getY(); y++) {
						player.level().setBlock(new BlockPos(x, y, z), settings.block.defaultBlockState(), 3);
					}
				}
			}
		}
	}

	private void handleBoxFill(Player player, ConstructionSettings settings) {
		if (settings.pos1 != null && settings.pos2 != null && settings.block != null) {
			BlockPos min = BlockTools.getMin(settings.pos1, settings.pos2);
			BlockPos max = BlockTools.getMax(settings.pos1, settings.pos2);

			for (int x = min.getX(); x <= max.getX(); x++) {
				for (int z = min.getZ(); z <= max.getZ(); z++) {
					player.level().setBlock(new BlockPos(x, max.getY(), z), settings.block.defaultBlockState(), 3);
					player.level().setBlock(new BlockPos(x, min.getY(), z), settings.block.defaultBlockState(), 3);
				}
			}
			for (int x = min.getX(); x <= max.getX(); x++) {
				for (int y = min.getY(); y <= max.getY(); y++) {
					player.level().setBlock(new BlockPos(x, y, min.getZ()), settings.block.defaultBlockState(), 3);
					player.level().setBlock(new BlockPos(x, y, max.getZ()), settings.block.defaultBlockState(), 3);
				}
			}
			for (int z = min.getZ(); z <= max.getZ(); z++) {
				for (int y = min.getY(); y <= max.getY(); y++) {
					player.level().setBlock(new BlockPos(min.getX(), y, z), settings.block.defaultBlockState(), 3);
					player.level().setBlock(new BlockPos(max.getX(), y, z), settings.block.defaultBlockState(), 3);
				}
			}
		}
	}

	private void handleLakeFill(Player player, ConstructionSettings settings) {
		if (settings.block != null) {
			BlockPos pos = BlockTools.getBlockClickedOn(player, player.level(), player.isShiftKeyDown());
			if (pos != null) {
				BlockState state = player.level().getBlockState(pos);
				Block block = state.getBlock();
				Set<BlockPos> toFill = new FloodFillPathfinder(player.level(), pos, block, state, false, true).doFloodFill();
				for (BlockPos p1 : toFill) {
					player.level().setBlock(BlockPos.containing(p1.getX(), p1.getY(), p1.getZ()), settings.block.defaultBlockState(), 3);
				}
			}
		}
	}

	private void handleLayerFill(Player player, ConstructionSettings settings) {
		if (settings.block != null) {
			BlockPos pos = BlockTools.getBlockClickedOn(player, player.level(), player.isShiftKeyDown());
			if (pos != null) {
				BlockState state = player.level().getBlockState(pos);
				Block block = state.getBlock();
				Set<BlockPos> toFill = new FloodFillPathfinder(player.level(), pos, block, state, false, false).doFloodFill();
				for (BlockPos p1 : toFill) {
					player.level().setBlock(p1, settings.block.defaultBlockState(), 3);
				}
			}
		}
	}

	@Override
	public boolean onKeyActionClient(Player player, ItemStack stack, ItemAltFunction altFunction) {
		return true;
	}

	@Override
	public void onKeyAction(Player player, ItemStack stack, ItemAltFunction altFunction) {
		ConstructionSettings settings = getSettings(stack);
		if (settings == null) {
			return;
		}
		switch (altFunction) {
			case ALT_FUNCTION_1://toggle mode
			{
				settings.type = settings.type.next();
			}
			break;
			case ALT_FUNCTION_2://source block
			{
				BlockPos pos = BlockTools.getBlockClickedOn(player, player.level(), player.isShiftKeyDown());
				if (pos != null) {
					BlockState state = player.level().getBlockState(pos);
					settings.block = state.getBlock();
					settings.meta = 0; //Port notes: no block meta; the tool fills with the default state of the picked block
					writeConstructionSettings(stack, settings);
				}
			}
			return;
			case ALT_FUNCTION_3://pos1
			{
				BlockPos pos = BlockTools.getBlockClickedOn(player, player.level(), player.isShiftKeyDown());
				if (pos != null) {
					settings.pos1 = pos;
					writeConstructionSettings(stack, settings);
				}
			}
			return;
			case ALT_FUNCTION_4://pos2
			{
				BlockPos pos = BlockTools.getBlockClickedOn(player, player.level(), player.isShiftKeyDown());
				if (pos != null) {
					settings.pos2 = pos;
					writeConstructionSettings(stack, settings);
				}
			}
			return;
			case ALT_FUNCTION_5://clear pos
			{
				settings.pos1 = null;
				settings.pos2 = null;
			}
			break;
		}
		writeConstructionSettings(stack, settings);
	}

	public static ConstructionSettings getSettings(ItemStack item) {
		if (item.getItem() instanceof ItemConstructionTool) {
			ConstructionSettings settings = new ConstructionSettings();
			if (ItemTools.hasTag(item) && ItemTools.getTag(item).contains("constructionSettings")) {
				settings.readFromNBT(ItemTools.getTag(item).getCompound("constructionSettings"));
			}
			return settings;
		}
		return null;
	}

	public static void writeConstructionSettings(ItemStack item, ConstructionSettings settings) {
		if (item.getItem() instanceof ItemConstructionTool) {
			ItemTools.setTagInfo(item,"constructionSettings", settings.writeToNBT(new CompoundTag()));
		}
	}


	public static final class ConstructionSettings {
		Block block;
		int meta;
		BlockPos pos1;
		BlockPos pos2;
		ConstructionType type = ConstructionType.SOLID_FILL;

		protected void readFromNBT(CompoundTag tag) {
			if (tag.contains("pos1")) {
				pos1 = BlockPos.of(tag.getLong("pos1"));
			}
			if (tag.contains("pos2")) {
				pos2 = BlockPos.of(tag.getLong("pos2"));
			}
			if (tag.contains("block") && net.shadowmage.ancientwarfare.core.util.RegistryTools.parseOrNull(tag.getString("block")) != null) {
				block = BuiltInRegistries.BLOCK.get(net.shadowmage.ancientwarfare.core.util.RegistryTools.parseOrNull(tag.getString("block")));
			}
			if (tag.contains("meta")) {
				meta = tag.getInt("meta");
			}
			if (tag.contains("type")) {
				type = ConstructionType.get(tag.getInt("type"));
			}
		}

		protected CompoundTag writeToNBT(CompoundTag tag) {
			if (block != null) {
				tag.putString("block", BuiltInRegistries.BLOCK.getKey(block).toString());
			}
			tag.putInt("meta", meta);
			if (pos1 != null) {
				tag.putLong("pos1", pos1.asLong());
			}
			if (pos2 != null) {
				tag.putLong("pos2", pos2.asLong());
			}
			tag.putInt("type", type.ordinal());
			return tag;
		}

		public boolean hasPos1() {
			return pos1 != null;
		}

		public boolean hasPos2() {
			return pos2 != null;
		}

		public BlockPos pos1() {
			return pos1;
		}

		public BlockPos pos2() {
			return pos2;
		}

	}

	public enum ConstructionType {

		/*
		 * Fills current layer and downwards with chosen block
		 */
		LAKE_FILL,

		/*
		 * fills current layer only with chosen block
		 */
		LAYER_FILL,

		/*
		 * fills entire bounding box with chosen block
		 */
		SOLID_FILL,

		/*
		 * creates a box around chosen area with chosen block
		 */
		BOX_FILL;

		public ConstructionType next() {
			int ordinal = ordinal();
			ordinal++;
			if (ordinal >= values().length) {
				ordinal = 0;
			}
			return values()[ordinal];
		}

		public static ConstructionType get(int index) {
			if (index >= 0 && index < values().length) {
				return values()[index];
			}
			return SOLID_FILL;
		}
	}

}
