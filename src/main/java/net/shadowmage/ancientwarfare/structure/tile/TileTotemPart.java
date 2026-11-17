package net.shadowmage.ancientwarfare.structure.tile;

import net.shadowmage.ancientwarfare.structure.block.BlockTotemPart;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.block.BlockTotemPart.Variant;

import java.util.Set;

import static net.shadowmage.ancientwarfare.core.render.property.CoreProperties.FACING;

public class TileTotemPart extends TileMulti {
	private static final String VARIANT_TAG = "variant";
	private Variant variant = Variant.BASE;
	private Variant dropVariant = Variant.BASE;

	public TileTotemPart(BlockPos pos, BlockState state) {
		this(AWStructureBlocks.TOTEM_PART_TILE.get(), pos, state);
	}

	protected TileTotemPart(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		//Port notes: the variant is a real block state property now; a block placed without tile data takes it from the state
		if (state.hasProperty(BlockTotemPart.VARIANT)) {
			variant = state.getValue(BlockTotemPart.VARIANT);
			dropVariant = variant;
		}
	}

	public void setVariant(Variant variant) {
		this.variant = variant;
		this.dropVariant = variant;
		net.shadowmage.ancientwarfare.structure.block.BlockBaseStructure.refresh(this);
	}

	public Variant getVariant() {
		return variant;
	}

	@Override
	public void setMainBlockPos(BlockPos mainBlockPos) {
		super.setMainBlockPos(mainBlockPos);
		getMainBlockPos().flatMap(mainPos -> WorldTools.getTile(level, mainPos, TileTotemPart.class)).ifPresent(te -> dropVariant = te.getVariant());
	}

	@Override
	protected void readNBT(CompoundTag compound) {
		super.readNBT(compound);
		variant = Variant.fromId(compound.getByte(VARIANT_TAG));
		getMainBlockPos().flatMap(mainPos -> WorldTools.getTile(level, mainPos, TileTotemPart.class)).ifPresent(te -> dropVariant = te.getVariant());
	}

	@Override
	protected void writeNBT(CompoundTag compound) {
		super.writeNBT(compound);
		compound.putByte(VARIANT_TAG, (byte) variant.getId());
	}

	@Override
	public Set<BlockPos> getAdditionalPositions(BlockState state) {
		return getVariant().getAdditionalPartPositions(worldPosition, state.getValue(FACING));
	}

	public Variant getDropVariant() {
		return dropVariant;
	}
}
