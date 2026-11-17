package net.shadowmage.ancientwarfare.structure.tile;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.shadowmage.ancientwarfare.structure.block.BlockCoffin;
import net.shadowmage.ancientwarfare.structure.block.BlockStoneCoffin;
import net.shadowmage.ancientwarfare.structure.init.AWStructureSounds;

import java.util.Map;
import java.util.Set;

public class TileStoneCoffin extends TileCoffin {
	private static final int TOTAL_OPEN_TIME = 60;
	private BlockStoneCoffin.Variant variant = BlockStoneCoffin.Variant.STONE;

	public TileStoneCoffin(BlockPos pos, BlockState state) {
		this(AWStructureBlocks.STONE_COFFIN_TILE.get(), pos, state);
	}

	protected TileStoneCoffin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public void setVariant(BlockStoneCoffin.Variant variant) {
		this.variant = variant;
	}

	@Override
	public BlockStoneCoffin.Variant getVariant() {
		return getValueFromMain(TileStoneCoffin.class, TileStoneCoffin::getVariant, variant, () -> BlockStoneCoffin.Variant.STONE);
	}

	@Override
	public Set<BlockPos> getAdditionalPositions(BlockState state) {
		return
				ImmutableSet.of(
						worldPosition.relative(direction.getFacing()),
						worldPosition.relative(direction.getFacing()).relative(direction.getFacing()),
						worldPosition.relative(direction.getFacing()).relative(direction.getFacing()).relative(direction.getFacing()),
						worldPosition.relative(direction.getFacing().getCounterClockWise()),
						worldPosition.relative(direction.getFacing().getCounterClockWise()).relative(direction.getFacing()),
						worldPosition.relative(direction.getFacing().getCounterClockWise()).relative(direction.getFacing()).relative(direction.getFacing()),
						worldPosition.relative(direction.getFacing().getCounterClockWise()).relative(direction.getFacing()).relative(direction.getFacing()).relative(direction.getFacing())
				);
	}

	private static final Map<BlockCoffin.IVariant, SoundEvent> COFFIN_SOUNDS = ImmutableMap.of(
			BlockStoneCoffin.Variant.STONE, AWStructureSounds.STONE_COFFIN_OPENS.get(),
			BlockStoneCoffin.Variant.SANDSTONE, AWStructureSounds.SANDSTONE_SARCOPHAGUS_OPENS.get(),
			BlockStoneCoffin.Variant.PRISMARINE, AWStructureSounds.PRISMARINE_COFFIN_OPENS.get(),
			BlockStoneCoffin.Variant.DEMONIC, AWStructureSounds.DEMONIC_COFFIN_OPENS.get());

	@Override
	protected void playSound() {
		level.playSound(null, worldPosition, COFFIN_SOUNDS.get(variant), SoundSource.BLOCKS, 1, 1);
	}

	@Override
	protected int getTotalOpenTime() {
		return TOTAL_OPEN_TIME;
	}

	public AABB getRenderBoundingBox() {
		Vec3i vec = direction.getFacing().getNormal();
		return new AABB(Vec3.atLowerCornerOf(worldPosition.offset(-3, 0, -3)), Vec3.atLowerCornerOf(worldPosition.offset(3, 2, 3))).inflate(vec.getX(), vec.getY(), vec.getZ());
	}

	@Override
	protected void readNBT(CompoundTag compound) {
		super.readNBT(compound);
		variant = BlockStoneCoffin.Variant.fromName(compound.getString("variant"));
	}

	@Override
	protected void writeNBT(CompoundTag compound) {
		super.writeNBT(compound);
		compound.putString("variant", variant.getSerializedName());
	}
}
