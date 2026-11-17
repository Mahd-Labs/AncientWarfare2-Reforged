package net.shadowmage.ancientwarfare.core.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.shadowmage.ancientwarfare.core.container.ContainerBackpack;
import net.shadowmage.ancientwarfare.core.container.ContainerBase;

public class GuiBackpack extends GuiContainerBase<ContainerBackpack> {

	public GuiBackpack(ContainerBase container) {
		super(container, 178, 192);
		this.imageHeight = this.getContainer().guiHeight;
	}

	@Override
	public void initElements() {
	}

	@Override
	public void setupElements() {
	}

	@Override
	protected boolean checkHotbarKeyPressed(int keyCode, int scanCode)//this code handles whether to allow the backpack to be moved from its slot via the number keys
	{
		boolean callSuper = true;
		for (int slot = 0; slot < 9; slot++) {
			if (this.minecraft.options.keyHotbarSlots[slot].isActiveAndMatches(InputConstants.getKey(keyCode, scanCode)) && slot == getContainer().backpackSlotIndex) {
				callSuper = false;
			}
		}
		return callSuper && super.checkHotbarKeyPressed(keyCode, scanCode);
	}

	/*
	 * 1.12 routed hotbar keys bound to mouse buttons through checkHotbarKeys as well; in 1.21.1 that path is private,
	 * so the swap with the backpack's own hotbar slot is also refused here.
	 */
	@Override
	protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
		if (type == ClickType.SWAP && mouseButton == getContainer().backpackSlotIndex) {
			return;
		}
		super.slotClicked(slot, slotId, mouseButton, type);
	}

}
