package net.shadowmage.ancientwarfare.structure.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;
import net.shadowmage.ancientwarfare.core.util.NBTBuilder;

public class TileColored extends TileUpdatable {
	private static final String DYE_COLOR_TAG = "dyeColor";
	private static final String COLOR_TAG = "color";
	private static final String UNLOCALIZED_NAME_PART_TAG = "unlocalizedNamePart";
	private static final String CUSTOM_DATA_TAG = "customData";
	private boolean customColor = false;
	private int dyeColor = -1;
	private int color = -1;
	private String customData;
	private String unlocalizedNamePart;

	public TileColored(BlockPos pos, BlockState state) {
		this(AWStructureBlocks.COLORED_TILE.get(), pos, state);
	}

	protected TileColored(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public void setDyeColor(int dyeColor) {
		this.dyeColor = dyeColor;
		customColor = false;
	}

	public void setColor(int color) {
		this.color = color;
		customColor = true;
	}

		public int getColor() {
		if (customColor) {
			return color;
		}

		return DyeColor.byId(15 - (dyeColor)).getTextureDiffuseColor();
	}

	public ItemStack getPickBlock() {
		ItemStack item = new ItemStack(level.getBlockState(worldPosition).getBlock());
		if (customColor) {
			ItemTools.setTag(item, 
					new NBTBuilder().setInteger(COLOR_TAG, color).setString(CUSTOM_DATA_TAG, customData).setString(UNLOCALIZED_NAME_PART_TAG, unlocalizedNamePart).build());
		} else {
			ItemTools.setTagInfo(item,DYE_COLOR_TAG, IntTag.valueOf(dyeColor));
		}
		return item;
	}

	@Override
	public void readFromNBT(CompoundTag compound) {
		super.readFromNBT(compound);
		readNBT(compound);
	}

	protected void readNBT(CompoundTag compound) {
		customColor = compound.getBoolean("customColor");
		dyeColor = compound.getInt(DYE_COLOR_TAG);
		if (compound.contains(COLOR_TAG)) {
			color = compound.getInt(COLOR_TAG);
		}
		customData = compound.getString(CUSTOM_DATA_TAG);
		unlocalizedNamePart = compound.getString(UNLOCALIZED_NAME_PART_TAG);
	}

	@Override
	public void writeToNBT(CompoundTag compound) {
		writeNBT(compound);
		super.writeToNBT(compound);
	}

	protected void writeNBT(CompoundTag compound) {
		compound.putBoolean("customColor", customColor);
		if (dyeColor != -1) {
			compound.putInt(DYE_COLOR_TAG, dyeColor);
		}
		if (color != -1) {
			compound.putInt(COLOR_TAG, color);
		}
		if (customData != null) {
			compound.putString(CUSTOM_DATA_TAG, customData);
		}
		if (unlocalizedNamePart != null) {
			compound.putString(UNLOCALIZED_NAME_PART_TAG, unlocalizedNamePart);
		}
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		writeNBT(tag);
		super.writeUpdateNBT(tag);
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		super.handleUpdateNBT(tag);
		readNBT(tag);
	}

	public void setCustomData(String customData) {
		this.customData = customData;
	}

	@SuppressWarnings("ConstantConditions")
	public void setFromStack(ItemStack stack) {
		if (!ItemTools.hasTag(stack)) {
			return;
		}
		CompoundTag tag = ItemTools.getTag(stack);
		if (tag.contains(DYE_COLOR_TAG)) {
			setDyeColor(tag.getInt(DYE_COLOR_TAG));
		} else if (tag.contains(COLOR_TAG)) {
			setColor(tag.getInt(COLOR_TAG));
			customData = tag.getString(CUSTOM_DATA_TAG);
			unlocalizedNamePart = tag.getString(UNLOCALIZED_NAME_PART_TAG);
		}
	}
}
