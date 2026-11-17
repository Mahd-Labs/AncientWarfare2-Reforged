package net.shadowmage.ancientwarfare.structure.item;

import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.tile.TileColored;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ItemBlockColored extends ItemBlockBaseStructure {
	private static final String DYE_COLOR_TAG = "dyeColor";
	private static final int WHITE = 16383998;
	private static final String COLOR_TAG = "color";
	private static final String CUSTOM_DATA_TAG = "customData";
	private final Set<CompoundTag> customItemTags = new HashSet<>();

	public ItemBlockColored(Block block) {
		super(block);
	}

	@SuppressWarnings("ConstantConditions")
		public int getColor(ItemStack stack) {
		if (ItemTools.hasTag(stack)) {
			CompoundTag tag = ItemTools.getTag(stack);
			if (tag.contains(COLOR_TAG)) {
				return tag.getInt(COLOR_TAG);
			}
			Optional<DyeColor> dyeColor = getDyeColor(stack);
			if (dyeColor.isPresent()) {
				return dyeColor.get().getTextureDiffuseColor();
			}
		}
		return WHITE;
	}

	public Set<CompoundTag> getCustomItemTags() {
		return customItemTags;
	}

	public void addCustomItemTag(CompoundTag tag) {
		customItemTags.add(tag);
	}

	@SuppressWarnings("ConstantConditions")
	private Optional<DyeColor> getDyeColor(ItemStack stack) {
		if (ItemTools.hasTag(stack) && ItemTools.getTag(stack).contains(DYE_COLOR_TAG)) {
			return Optional.of(DyeColor.byId(15 - (ItemTools.getTag(stack).getInt(DYE_COLOR_TAG))));
		}

		return Optional.empty();
	}


	@SuppressWarnings("ConstantConditions")
	@Override
	public boolean placeBlockAt(ItemStack stack, Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, BlockState newState) {
		boolean result = super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);

		if (world.getBlockState(pos).getBlock() == getBlock()) {
			WorldTools.getTile(world, pos, TileColored.class).ifPresent(t -> t.setFromStack(stack));
		}
		return result;
	}

	public String getItemStackDisplayName(ItemStack stack) {
		if (!ItemTools.hasTag(stack)) {
			return getLegacyName(stack);
		}

		CompoundTag tag = ItemTools.getTag(stack);

		if (tag.contains(CUSTOM_DATA_TAG)) {
			return net.shadowmage.ancientwarfare.structure.util.TextHelper.translate((getLegacyName(stack) + "." + tag.getString("unlocalizedNamePart") + ".name").trim(),
					StringUtils.capitalize(tag.getString(CUSTOM_DATA_TAG)));
		}

		String color = net.shadowmage.ancientwarfare.structure.util.TextHelper.translate(getDyeColor(stack).map(DyeColor::getName).orElse("white"));
		return net.shadowmage.ancientwarfare.structure.util.TextHelper.translate((getLegacyName(stack) + ".name").trim(), color);
	}

}
