package net.shadowmage.ancientwarfare.npc.xmod;

/*
 * Port notes: static holder for the cross module hooks of the NPC module (see PORTING.md "Priorities": NPC code that
 * references vehicles / structures keeps working with or without those modules). The owning module installs its
 * implementation from its init(); until then the no-op defaults are used. Call sites are marked
 * "// TODO-CROSS(structure|vehicle): <original class.method>".
 */
public final class NpcXmod {
	private NpcXmod() {}

	private static IVehicleHooks vehicleHooks = IVehicleHooks.NONE;
	private static IStructureHooks structureHooks = IStructureHooks.NONE;

	public static IVehicleHooks vehicle() {
		return vehicleHooks;
	}

	public static IStructureHooks structure() {
		return structureHooks;
	}

	public static void setVehicleHooks(IVehicleHooks hooks) {
		vehicleHooks = hooks == null ? IVehicleHooks.NONE : hooks;
	}

	public static void setStructureHooks(IStructureHooks hooks) {
		structureHooks = hooks == null ? IStructureHooks.NONE : hooks;
	}
}
