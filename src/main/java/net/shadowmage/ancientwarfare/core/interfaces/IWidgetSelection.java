package net.shadowmage.ancientwarfare.core.interfaces;

import net.shadowmage.ancientwarfare.core.gui.elements.GuiElement;

/*
 * CLIENT ONLY (references gui element classes)
 */
public interface IWidgetSelection {

	public void onWidgetSelected(GuiElement element);

	public void onWidgetDeselected(GuiElement element);

}
