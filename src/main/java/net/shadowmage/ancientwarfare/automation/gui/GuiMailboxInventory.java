package net.shadowmage.ancientwarfare.automation.gui;

import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.shadowmage.ancientwarfare.automation.container.ContainerMailbox;
import net.shadowmage.ancientwarfare.core.container.ContainerBase;
import net.shadowmage.ancientwarfare.core.gui.GuiContainerBase;
import net.shadowmage.ancientwarfare.core.gui.elements.Button;
import net.shadowmage.ancientwarfare.core.gui.elements.Checkbox;
import net.shadowmage.ancientwarfare.core.gui.elements.Label;

public class GuiMailboxInventory extends GuiContainerBase<ContainerMailbox> {

	private Label inputName, outputName;
	private Checkbox autoExport, privateBox;

	public GuiMailboxInventory(ContainerBase par1Container) {
		super(par1Container, 178, 240);
		imageHeight = getContainer().guiHeight;
	}

	@Override
	public void initElements() {
		Button inputNameSelect = new Button(178 - 8 - 75, 8, 75, 12, "guistrings.automation.mailbox_name_select") {
			@Override
			protected void onPressed() {
				getContainer().removeSlots();
				double x = Minecraft.getInstance().mouseHandler.xpos();
				double y = Minecraft.getInstance().mouseHandler.ypos();
				Minecraft.getInstance().setScreen(new GuiMailboxNameSelect(GuiMailboxInventory.this, true));
				GLFW.glfwSetCursorPos(Minecraft.getInstance().getWindow().getWindow(), x, y);
			}
		};
		addGuiElement(inputNameSelect);

		Button outputNameSelect = new Button(178 - 8 - 75, 8 + 12 + 2 * 18, 75, 12, "guistrings.automation.mailbox_target_select") {
			@Override
			protected void onPressed() {
				getContainer().removeSlots();
				double x = Minecraft.getInstance().mouseHandler.xpos();
				double y = Minecraft.getInstance().mouseHandler.ypos();
				Minecraft.getInstance().setScreen(new GuiMailboxNameSelect(GuiMailboxInventory.this, false));
				GLFW.glfwSetCursorPos(Minecraft.getInstance().getWindow().getWindow(), x, y);
			}
		};
		addGuiElement(outputNameSelect);

		Button sideSelectButton = new Button(178 - 8 - 75, imageHeight - 8 - 12 - 12, 75, 12, "guistrings.inventory.setsides") {
			@Override
			protected void onPressed() {
				getContainer().removeSlots();
				double x = Minecraft.getInstance().mouseHandler.xpos();
				double y = Minecraft.getInstance().mouseHandler.ypos();
				Minecraft.getInstance().setScreen(new GuiMailboxInventorySideSetup(GuiMailboxInventory.this));
				GLFW.glfwSetCursorPos(Minecraft.getInstance().getWindow().getWindow(), x, y);
			}
		};
		addGuiElement(sideSelectButton);

		autoExport = new Checkbox(8, imageHeight - 12 - 12 - 8, 12, 12, "guistrings.automation.auto_output") {
			@Override
			public void onToggled() {
				getContainer().handleAutoExportToggle(checked());
			}
		};
		autoExport.setChecked(getContainer().autoExport);
		addGuiElement(autoExport);

		privateBox = new Checkbox(8, imageHeight - 12 - 8, 12, 12, "guistrings.automation.private_mailbox") {
			@Override
			public void onToggled() {
				getContainer().handlePrivateBoxToggle(checked());
			}
		};
		privateBox.setChecked(getContainer().privateBox);
		addGuiElement(privateBox);

		inputName = new Label(8, 8, "");
		addGuiElement(inputName);

		outputName = new Label(8, 8 + 12 + 2 * 18, "");
		addGuiElement(outputName);
	}

	@Override
	public void setupElements() {
		inputName.setText(getContainer().mailboxName);
		outputName.setText(getContainer().targetName);
		autoExport.setChecked(getContainer().autoExport);
		privateBox.setChecked(getContainer().privateBox);
	}

}
