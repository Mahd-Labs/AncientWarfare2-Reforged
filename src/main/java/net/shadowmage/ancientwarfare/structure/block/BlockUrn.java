package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.structure.init.AWStructureSoundTypes;
import net.shadowmage.ancientwarfare.structure.tile.TileUrn;

import javax.annotation.Nullable;
import java.util.Random;

public class BlockUrn extends BlockBaseStructure implements EntityBlock {
	public BlockUrn() {
		super(Material.CLAY.props().noOcclusion().strength(0.4F).noLootTable(), "urn");
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileUrn(pos, state);
	}

	@Override
	public AABB getBoundingBox(BlockState state, BlockGetter source, BlockPos pos) {
		return new AABB(2 / 16D, 0, 2 / 16D, 14 / 16D, 15 / 16D, 14 / 16D);
	}

	@Override
	public SoundType getSoundType(BlockState state, LevelReader world, BlockPos pos, Entity entity) {
		return AWStructureSoundTypes.URN;
	}

}
