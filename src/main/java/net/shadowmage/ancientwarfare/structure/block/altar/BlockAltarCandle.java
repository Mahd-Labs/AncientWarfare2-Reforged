package net.shadowmage.ancientwarfare.structure.block.altar;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.EntityBlock;
import net.shadowmage.ancientwarfare.structure.block.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.tile.TileAltarCandle;
import net.shadowmage.ancientwarfare.structure.tile.TileColored;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;

public class BlockAltarCandle extends BlockAltarTop implements EntityBlock {
	private static final AABB CANDLE_AABB = new AABB(6 / 16D, 0, 6 / 16D, 10 / 16D, 10 / 16D, 10 / 16D);

	public BlockAltarCandle() {
		super(Material.IRON.props().lightLevel(s -> (int) (15.0F * (12 / 15F))).randomTicks(), "altar_candle");
	}

	@Override
	public AABB getCollisionBoundingBox(BlockState blockState, BlockGetter worldIn, BlockPos pos) {
		return NULL_AABB;
	}

	@Override
	public AABB getBoundingBox(BlockState state, BlockGetter source, BlockPos pos) {
		return CANDLE_AABB;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileAltarCandle(pos, state);
	}

	@Override
	public ItemStack getPickBlock(BlockState state, HitResult target, Level world, BlockPos pos, Player player) {
		return WorldTools.getTile(world, pos, TileColored.class).map(TileColored::getPickBlock).orElse(ItemStack.EMPTY);
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
		WorldTools.getTile(world, pos, TileAltarCandle.class).ifPresent(t -> drops.add(t.getPickBlock()));
	}

	@Override
	public void animateTick(BlockState stateIn, Level world, BlockPos pos, RandomSource random) {
		double d0 = (double) pos.getX() + 0.5D;
		double d1 = (double) pos.getY() + 0.7D;
		double d2 = (double) pos.getZ() + 0.5D;

		Optional<TileAltarCandle> te = WorldTools.getTile(world, pos, TileAltarCandle.class);
		if (!te.isPresent()) {
			return;
		}
		if (te.get().isFlameSmoke()) {
			world.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0D, 0.0D, 0.0D);
		} else if (te.get().getFlameColor() == -1) {
			world.addParticle(ParticleTypes.FLAME, d0, d1, d2, 0.0D, 0.0D, 0.0D);
		} else {
			net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().addColoredFlameParticle(world, d0, d1, d2, 0.0D, 0.0D, 0.0D, te.get().getFlameColor());
		}

	}
}
