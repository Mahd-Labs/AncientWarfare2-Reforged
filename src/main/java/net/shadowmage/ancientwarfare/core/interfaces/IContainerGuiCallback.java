package net.shadowmage.ancientwarfare.core.interfaces;

import net.minecraft.nbt.CompoundTag;

public interface IContainerGuiCallback {

	/*
	 * called from container when the gui should re-init its elements on the next tick
	 */
	void refreshGui();

	/*
	 * called from container when a gui-directed packet arrives (ContainerBase.sendDataToGui)
	 */
	void handlePacketData(CompoundTag data);
}
