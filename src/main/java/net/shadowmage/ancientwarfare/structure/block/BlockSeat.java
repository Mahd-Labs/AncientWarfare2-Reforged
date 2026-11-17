package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.structure.entity.EntitySeat;
import net.shadowmage.ancientwarfare.structure.util.RotationLimit;

import java.util.List;

public abstract class BlockSeat extends BlockBaseStructure {
	private static final Vec3 DEFAULT_SEAT_OFFSET = new Vec3(0.5, 0.5, 0.5);

	public BlockSeat(BlockBehaviour.Properties material, String regName) {
		super(material.noOcclusion(), regName);
	}

	@Override
	public boolean onBlockActivated(Level world, BlockPos pos, BlockState state, Player playerIn, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		if (!world.isClientSide && !isOccupied(world, pos)) {
			EntitySeat seatEntity = new EntitySeat(world, new Vec3(pos.getX(), pos.getY(), pos.getZ()).add(getSeatOffset()), pos);
			world.addFreshEntity(seatEntity);
			playerIn.startRiding(seatEntity);
		}
		return true;
	}

	protected Vec3 getSeatOffset() {
		return DEFAULT_SEAT_OFFSET;
	}

	private boolean isOccupied(Level world, BlockPos pos) {
		List<EntitySeat> seats = world.getEntitiesOfClass(EntitySeat.class, new AABB(pos).inflate(1));
		for (EntitySeat seat : seats) {
			if (seat.getSeatPos().equals(pos)) {
				return seat.isVehicle();
			}
		}
		return false;
	}

	public abstract RotationLimit getRotationLimit(Level world, BlockPos seatPos, BlockState state);
}
