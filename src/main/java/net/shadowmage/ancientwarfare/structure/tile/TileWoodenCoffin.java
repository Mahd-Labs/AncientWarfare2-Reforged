package net.shadowmage.ancientwarfare.structure.tile;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.google.common.collect.ImmutableSet;
import net.shadowmage.ancientwarfare.structure.block.BlockCoffin;
import net.shadowmage.ancientwarfare.structure.block.BlockWoodenCoffin;
import net.shadowmage.ancientwarfare.structure.init.AWStructureSounds;

import java.util.Set;

public class TileWoodenCoffin extends TileCoffin {
	private static final int TOTAL_OPEN_TIME = 20;

	private BlockWoodenCoffin.Variant variant = BlockWoodenCoffin.Variant.OAK;
	private boolean upright = false;

	public TileWoodenCoffin(BlockPos pos, BlockState state) {
		this(AWStructureBlocks.WOODEN_COFFIN_TILE.get(), pos, state);
	}

	protected TileWoodenCoffin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected int getTotalOpenTime() {
		return TOTAL_OPEN_TIME;
	}

	@Override
	public Set<BlockPos> getAdditionalPositions(BlockState state) {
		return upright ? ImmutableSet.of(worldPosition.above(), worldPosition.above().above()) :
				ImmutableSet.of(worldPosition.relative(direction.getFacing()), worldPosition.relative(direction.getFacing()).relative(direction.getFacing()));
	}

	public void setVariant(BlockWoodenCoffin.Variant variant) {
		this.variant = variant;
	}

	@Override
	public BlockWoodenCoffin.Variant getVariant() {
		return getValueFromMain(TileWoodenCoffin.class, TileWoodenCoffin::getVariant, variant, () -> BlockWoodenCoffin.Variant.OAK);
	}

	@Override
	public void setPlacementDirection(Level level, BlockPos pos, BlockState state, Direction horizontalFacing, float rotationYaw) {
		setDirection(upright ? BlockCoffin.CoffinDirection.fromYaw(rotationYaw) : BlockCoffin.CoffinDirection.fromFacing(horizontalFacing));
	}

	public AABB getRenderBoundingBox() {
		if (upright) {
			return new AABB(Vec3.atLowerCornerOf(worldPosition.offset(-1, 0, -1)), Vec3.atLowerCornerOf(worldPosition.offset(2, 3, 2)));
		}

		Vec3i vec = direction.getFacing().getNormal();
		return new AABB(Vec3.atLowerCornerOf(worldPosition.offset(-1, 0, -1)), Vec3.atLowerCornerOf(worldPosition.offset(2, 1, 2))).inflate(vec.getX(), vec.getY(), vec.getZ());
	}

	@Override
	protected void readNBT(CompoundTag compound) {
		super.readNBT(compound);
		upright = compound.getBoolean("upright");
		variant = BlockWoodenCoffin.Variant.fromName(compound.getString("variant"));
	}

	@Override
	protected void writeNBT(CompoundTag compound) {
		super.writeNBT(compound);
		compound.putBoolean("upright", upright);
		compound.putString("variant", variant.getSerializedName());
	}

	@Override
	protected void playSound() {
		level.playSound(null, worldPosition, AWStructureSounds.COFFIN_OPENS.get(), SoundSource.BLOCKS, 1, 1);
	}

	public void setUpright(boolean upright) {
		this.upright = upright;
	}

	public boolean getUpright() {
		return upright;
	}
}
