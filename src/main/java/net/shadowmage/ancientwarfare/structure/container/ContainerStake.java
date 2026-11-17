package net.shadowmage.ancientwarfare.structure.container;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.core.container.ContainerBase;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.tile.TileStake;

import java.util.Optional;

public class ContainerStake extends ContainerBase {
	private TileStake stake;

	public ContainerStake(Player player, int x, int y, int z) {
		super(player);
		Optional<TileStake> te = WorldTools.getTile(player.level(), new BlockPos(x, y, z), TileStake.class);
		if (te.isPresent()) {
			stake = te.get();
		} else {
			throw new IllegalArgumentException("Stake not found");
		}
	}

	@Override
	public void handlePacketData(CompoundTag tag) {
		stake.readFromNBT(tag);
		//1.12 world.checkLight(pos) on the server, so other players see the stake light up or go dark
		if (stake.getLevel() != null && !stake.getLevel().isClientSide) {
			for (int dy = 0; dy <= 2; dy++) {
				stake.getLevel().getChunkSource().getLightEngine().checkBlock(stake.getBlockPos().above(dy));
			}
			net.shadowmage.ancientwarfare.core.util.BlockTools.notifyBlockUpdate(stake);
		}
	}

	public void updateServer() {
		CompoundTag stakeTag = new CompoundTag();
		stake.writeToNBT(stakeTag);
		sendDataToServer(stakeTag);
	}

	public TileStake getStake() {
		return stake;
	}
}
