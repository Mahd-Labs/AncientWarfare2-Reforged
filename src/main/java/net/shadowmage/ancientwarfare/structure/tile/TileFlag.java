package net.shadowmage.ancientwarfare.structure.tile;

import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import com.mojang.authlib.GameProfile;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;

import javax.annotation.Nullable;

public abstract class TileFlag extends TileUpdatable {
	private static final String NAME_TAG = "name";
	private String name = "";

	protected TileFlag(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public boolean isPlayerOwned() {
		return false;
	}

	public GameProfile getPlayerProfile() {
		return null;
	}

	public String getName() {
		return name;
	}

	public AABB getRenderBoundingBox() {
		return new AABB(Vec3.atLowerCornerOf(worldPosition), Vec3.atLowerCornerOf(worldPosition.offset(1, 3, 1)));
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		super.handleUpdateNBT(tag);
		readNBT(tag);
	}

	@Override
	public void readFromNBT(CompoundTag compound) {
		super.readFromNBT(compound);
		readNBT(compound);
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		super.writeUpdateNBT(tag);
		writeNBT(tag);
	}

	@Override
	public void writeToNBT(CompoundTag compound) {
		super.writeToNBT(compound);
		writeNBT(compound);
	}

	protected void readNBT(CompoundTag tag) {
		name = tag.getString(NAME_TAG);
	}

	protected CompoundTag writeNBT(CompoundTag tag) {
		tag.putString(NAME_TAG, name);
		return tag;
	}

	@SuppressWarnings("ConstantConditions")
	public void setFromStack(ItemStack stack) {
		if (ItemTools.hasTag(stack)) {
			CompoundTag tag = ItemTools.getTag(stack);
			readNBT(tag);
		}
	}

	public abstract ItemStack getItemStack();
}
