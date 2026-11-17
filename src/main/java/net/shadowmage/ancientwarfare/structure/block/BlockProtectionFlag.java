package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.structure.tile.TileProtectionFlag;

import javax.annotation.Nullable;
import java.util.Random;

public class BlockProtectionFlag extends BlockFlag implements EntityBlock {
	public BlockProtectionFlag() {
		//1.12 setResistance(6000000): explosion resistance * 3 / 5
		super(Material.WOOD.props().lightLevel(s -> 13).explosionResistance(3600000F), "protection_flag");
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter worldIn, BlockPos pos) {
		float original = super.getDestroyProgress(state, player, worldIn, pos);
		return WorldTools.getTile(worldIn, pos, TileProtectionFlag.class).map(te -> te.getPlayerRelativeBlockHardness(player, original))
				.orElse(original);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileProtectionFlag(pos, state);
	}

	@Override
	public boolean onBlockActivated(Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		WorldTools.getTile(world, pos, TileProtectionFlag.class).ifPresent(te -> te.onActivatedBy(player));
		return true;
	}

	@Override
	public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, RandomSource random) {
		int maxParticles = worldIn.random.nextInt(10);
		for (int i = 0; i < maxParticles; i++) {

			double d0 = (double) pos.getX() + worldIn.random.nextFloat();
			double d1 = (double) pos.getY() + 1.9D * worldIn.random.nextFloat();
			double d2 = (double) pos.getZ() + worldIn.random.nextFloat();
			worldIn.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0D, 0.0D, 0.0D);
			net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().addSunParticle(worldIn, d0, d1, d2);
		}
	}
}
