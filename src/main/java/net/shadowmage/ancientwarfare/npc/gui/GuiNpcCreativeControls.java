package net.shadowmage.ancientwarfare.npc.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.shadowmage.ancientwarfare.core.container.ContainerBase;
import net.shadowmage.ancientwarfare.core.gui.GuiContainerBase;
import net.shadowmage.ancientwarfare.core.gui.elements.Button;
import net.shadowmage.ancientwarfare.core.gui.elements.Checkbox;
import net.shadowmage.ancientwarfare.core.gui.elements.Label;
import net.shadowmage.ancientwarfare.core.gui.elements.NumberInput;
import net.shadowmage.ancientwarfare.core.gui.elements.Text;
import net.shadowmage.ancientwarfare.npc.container.ContainerNpcCreativeControls;

public class GuiNpcCreativeControls extends GuiContainerBase<ContainerNpcCreativeControls> {
	private Text ownerNameInput;
	private NumberInput attackDamageOverrideInput;
	private NumberInput armorValueOverrideInput;
	private NumberInput maxHealthOverrideInput;
	private Checkbox wanderCheckbox;
	private Checkbox customEquipmentCheckbox;
	private NumberInput levelInput;//improvement (not baseline): only when the server sends the level
	private Label levelRangeLabel;
	private Label levelInfoLabel;
	private Label levelJobLabel;

	private boolean hasChanged = false;

	public GuiNpcCreativeControls(ContainerBase container) {
		super(container);
	}

	@Override
	public void initElements() {
		int totalHeight = 8;
		Label label;

		label = new Label(8, totalHeight + 1, "guistrings.npc.owner_name");
		addGuiElement(label);

		ownerNameInput = new Text(100, totalHeight, 256 - 16 - 100, "", this) {
			@Override
			public void onTextUpdated(String oldText, String newText) {
				getContainer().ownerName = newText;
				hasChanged = true;
			}
		};
		addGuiElement(ownerNameInput);
		totalHeight += 12;

		label = new Label(8, totalHeight + 1, "guistrings.npc.custom_texture");
		addGuiElement(label);

		Button skinButton = new Button(100, totalHeight, 256 - 16 - 100, 12, getContainer().skinSettings.getDescription()) {
			@Override
			protected void onPressed() {
				Minecraft.getInstance().setScreen(new GuiSkinSelection(GuiNpcCreativeControls.this, getContainer()));
			}
		};
		addGuiElement(skinButton);

		totalHeight += 12;

		label = new Label(8, totalHeight + 1, "guistrings.npc.health_override");
		addGuiElement(label);

		maxHealthOverrideInput = new NumberInput(120, totalHeight, 60, 0, this) {
			@Override
			public void onValueUpdated(float value) {
				getContainer().maxHealth = (int) value;
				hasChanged = true;
			}
		};
		addGuiElement(maxHealthOverrideInput);
		maxHealthOverrideInput.setIntegerValue().setAllowNegative();
		totalHeight += 12;

		label = new Label(8, totalHeight + 1, "guistrings.npc.damage_override");
		addGuiElement(label);

		attackDamageOverrideInput = new NumberInput(120, totalHeight, 60, 0, this) {
			@Override
			public void onValueUpdated(float value) {
				getContainer().attackDamage = (int) value;
				hasChanged = true;
			}
		};
		addGuiElement(attackDamageOverrideInput);
		attackDamageOverrideInput.setIntegerValue().setAllowNegative();
		totalHeight += 12;

		label = new Label(8, totalHeight + 1, "guistrings.npc.armor_override");
		addGuiElement(label);

		armorValueOverrideInput = new NumberInput(120, totalHeight, 60, 0, this) {
			@Override
			public void onValueUpdated(float value) {
				getContainer().armorValue = (int) value;
				hasChanged = true;
			}
		};
		addGuiElement(armorValueOverrideInput);
		armorValueOverrideInput.setIntegerValue().setAllowNegative();
		totalHeight += 12;

		wanderCheckbox = new Checkbox(8, totalHeight, 16, 16, "guistrings.npc.allow_wander") {
			@Override
			public void onToggled() {
				getContainer().wander = checked();
				hasChanged = true;
			}
		};
		addGuiElement(wanderCheckbox);

		if (getContainer().isFactionNpc()) {
			customEquipmentCheckbox = new Checkbox(100, totalHeight, 16, 16, "guistrings.npc.custom_equipment") {
				@Override
				public void onToggled() {
					getContainer().hasCustomEquipment = checked();
					hasChanged = true;
				}
			};
			customEquipmentCheckbox.addTooltip(I18n.get("guistrings.npc.custom_equipment.tooltip"));
			addGuiElement(customEquipmentCheckbox);
		}
		totalHeight += 16;

		this.imageHeight = totalHeight + 8;
	}

	@Override
	public void setupElements() {
		ownerNameInput.setText(getContainer().ownerName);
		attackDamageOverrideInput.setValue(getContainer().attackDamage);
		armorValueOverrideInput.setValue(getContainer().armorValue);
		maxHealthOverrideInput.setValue(getContainer().maxHealth);
		wanderCheckbox.setChecked(getContainer().wander);
		if (getContainer().isFactionNpc()) {
			customEquipmentCheckbox.setChecked(getContainer().hasCustomEquipment);
		}
		setupLevelElements();
	}

	/*
	 * Improvement (not baseline, config improvements.npc_level_editor): three more rows below the 1.12 ones, added
	 * when the level data of the server arrives. Without that data the screen stays as in 1.12.
	 */
	private void setupLevelElements() {
		ContainerNpcCreativeControls container = getContainer();
		if (container.level < 0) {
			return;
		}
		if (levelInput == null) {
			int totalHeight = this.imageHeight - 8;
			addGuiElement(new Label(8, totalHeight + 1, "guistrings.npc.level"));
			levelInput = new NumberInput(120, totalHeight, 60, container.levelToSet, this) {
				@Override
				public void onValueUpdated(float value) {
					getContainer().levelToSet = (int) value;
					if (getContainer().levelToSet != getContainer().level) {
						hasChanged = true;
					}
				}
			};
			levelInput.setIntegerValue();
			addGuiElement(levelInput);
			levelRangeLabel = new Label(184, totalHeight + 1, "");
			addGuiElement(levelRangeLabel);
			totalHeight += 12;
			levelInfoLabel = new Label(8, totalHeight + 1, "");
			addGuiElement(levelInfoLabel);
			totalHeight += 12;
			levelJobLabel = new Label(8, totalHeight + 1, "");
			addGuiElement(levelJobLabel);
			totalHeight += 12;
			this.imageHeight = totalHeight + 8;
			this.topPos = (this.height - this.imageHeight) / 2;
		}
		levelInput.setValue(container.levelToSet);
		levelRangeLabel.setText(I18n.get("guistrings.npc.level_range", container.maxLevel));
		String experience = container.levelExperienceNeeded < 0 ? String.valueOf(container.levelExperience) : container.levelExperience + " / " + container.levelExperienceNeeded;
		levelInfoLabel.setText(I18n.get("guistrings.npc.level_info", container.level, experience));
		String jobKey = "entity.ancientwarfarenpc." + container.levelType + ".name";
		levelJobLabel.setText(I18n.get("guistrings.npc.level_job", I18n.exists(jobKey) ? I18n.get(jobKey) : container.levelType));
	}

	@Override
	protected boolean onGuiCloseRequested() {
		/*
		 * if changes were made while gui was open, send these to server
		 */
		if (hasChanged) {
			getContainer().sendChangesToServer();
		}

		/*
		 * force opening of normal gui (whatever that may be for the npc) when advanced controls is closed
		 */
		getContainer().entity.openGUI(player);
		return false;
	}

}
