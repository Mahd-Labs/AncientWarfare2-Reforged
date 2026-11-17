package net.shadowmage.ancientwarfare.core.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/*
 * Created by Olivier on 05/02/2015.
 */
public class ContainerTileBase<T extends BlockEntity> extends ContainerBase {

	public final T tileEntity;

	@SuppressWarnings("unchecked")
	public ContainerTileBase(Player player, int x, int y, int z) {
		super(player);
		tileEntity = (T) player.level().getBlockEntity(new BlockPos(x, y, z));
		if (tileEntity == null) {
			throw new IllegalArgumentException("Tile is null");
		}
	}

	@Override
	public boolean canInteractWith(Player player) {
		return !tileEntity.isRemoved() && tileEntity.getBlockPos().distToCenterSqr(player.getX(), player.getY(), player.getZ()) <= 64D;
	}

	@Override
	public final boolean equals(Object o) {
		return this == o || o instanceof ContainerTileBase && tileEntity.equals(((ContainerTileBase<?>) o).tileEntity);
	}

	@Override
	public final int hashCode() {
		return tileEntity.hashCode();
	}
}
