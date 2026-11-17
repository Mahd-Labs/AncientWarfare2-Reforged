package net.shadowmage.ancientwarfare.automation.gui;

import net.shadowmage.ancientwarfare.core.container.ContainerBase;
import net.shadowmage.ancientwarfare.core.gui.elements.Button;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;

public class GuiWorksiteQuarry extends GuiWorksiteBase {

	public GuiWorksiteQuarry(ContainerBase par1Container) {
		super(par1Container);
	}

	protected void addQuarryHeightButton() {
		Button height = new Button(108, imageHeight - 8 - 12, 50, 12, "guistrings.automation.height_bounds") {
			@Override
			protected void onPressed() {
				NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_WORKSITE_QUARRY_BOUNDS, getContainer().tileEntity.getBlockPos());
			}
		};
		addGuiElement(height);
	}
	@Override
	public void initElements() {
		addLabels();
		addSideSelectButton();
		addBoundsAdjustButton();
		addQuarryHeightButton();
	}

	@Override
	public void setupElements() {

	}

}
