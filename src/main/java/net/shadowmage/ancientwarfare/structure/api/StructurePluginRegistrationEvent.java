package net.shadowmage.ancientwarfare.structure.api;

import net.neoforged.bus.api.Event;
/*
 * Called when StructurePluginManager tries to load plugins
 */
public class StructurePluginRegistrationEvent extends Event {

	public final IStructurePluginRegister register;

	public StructurePluginRegistrationEvent(IStructurePluginRegister register) {
		this.register = register;
	}
}
