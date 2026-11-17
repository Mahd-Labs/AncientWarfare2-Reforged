package net.shadowmage.ancientwarfare.automation.container;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.automation.tile.worksite.TileWorksiteFarm;
import net.shadowmage.ancientwarfare.core.container.ContainerTileBase;
import net.shadowmage.ancientwarfare.core.interfaces.IBoundedSite;
import net.shadowmage.ancientwarfare.core.util.BlockTools;

public class ContainerWorksiteBoundsAdjust extends ContainerTileBase {

	public BlockPos min, max;

	public ContainerWorksiteBoundsAdjust(Player player, int x, int y, int z) {
		super(player, x, y, z);
		if (tileEntity instanceof IBoundedSite) {
			min = getWorksite().getWorkBoundsMin();
			max = getWorksite().getWorkBoundsMax();
		} else
			throw new IllegalArgumentException("Couldn't find work site");
	}

	@Override
	public void sendInitData() {
		if (tileEntity instanceof TileWorksiteFarm) {
			TileWorksiteFarm twub = (TileWorksiteFarm) tileEntity;
			CompoundTag tag = new CompoundTag();
			tag.putByteArray("checkedMap", twub.getTargetMap());
			sendDataToGui(tag);
		}
	}

	/*
	 * the bounds a client sends are checked on the server as the GUI limits them on the client: not wider
	 * than the worksite allows, and near the worksite (1.12 relied on Forge's chunk limit per ticket for the rest).
	 */
	public static boolean acceptsBounds(int maxWidth, BlockPos site, BlockPos min, BlockPos max) {
		if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ()) {
			return false;
		}
		if (max.getX() - min.getX() + 1 > maxWidth || max.getZ() - min.getZ() + 1 > maxWidth) {
			return false;
		}
		int reach = maxWidth + 16;
		return Math.abs(min.getX() - site.getX()) <= reach && Math.abs(max.getX() - site.getX()) <= reach
				&& Math.abs(min.getZ() - site.getZ()) <= reach && Math.abs(max.getZ() - site.getZ()) <= reach
				&& Math.abs(min.getY() - site.getY()) <= 400 && Math.abs(max.getY() - site.getY()) <= 400;
	}

	@Override
	public void handlePacketData(CompoundTag tag) {
		if (tag.contains("guiClosed")) {
			if (tag.contains("min") && tag.contains("max")) {
				BlockPos min = BlockPos.of(tag.getLong("min"));
				BlockPos max = BlockPos.of(tag.getLong("max"));
				if (!player.level().isClientSide && !acceptsBounds(getWorksite().getBoundsMaxWidth(), getBlockPos(), min, max)) {
					net.shadowmage.ancientwarfare.automation.AncientWarfareAutomation.LOG.warn("Refused work bounds {} - {} for the worksite at {} sent by {}", min, max, getBlockPos(), player.getGameProfile().getName());
					return;
				}
				getWorksite().setWorkBoundsMin(min);
				getWorksite().setWorkBoundsMax(max);
				getWorksite().onBoundsAdjusted();
				getWorksite().onPostBoundsAdjusted();
			}
			if (tag.contains("checkedMap") && tileEntity instanceof TileWorksiteFarm) {
				TileWorksiteFarm twub = (TileWorksiteFarm) tileEntity;
				byte[] map = tag.getByteArray("checkedMap");
				twub.setTargetBlocks(map);
			}
			BlockTools.notifyBlockUpdate(player.level(), getBlockPos());
		}
	}

	public void onClose(boolean boundsAdjusted, boolean targetsAdjusted, byte[] checkedMap) {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("guiClosed", true);
		if (boundsAdjusted) {
			tag.putLong("min", min.asLong());
			tag.putLong("max", max.asLong());
		}
		if (targetsAdjusted && tileEntity instanceof TileWorksiteFarm) {
			tag.putByteArray("checkedMap", checkedMap);
		}
		sendDataToServer(tag);
	}

	public BlockPos getBlockPos() {
		return tileEntity.getBlockPos();
	}

	public int getX() {
		return tileEntity.getBlockPos().getX();
	}

	public int getY() {
		return tileEntity.getBlockPos().getY();
	}

	public int getZ() {
		return tileEntity.getBlockPos().getZ();
	}

	public IBoundedSite getWorksite() {
		return (IBoundedSite) tileEntity;
	}
}
