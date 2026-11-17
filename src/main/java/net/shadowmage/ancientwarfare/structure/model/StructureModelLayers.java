package net.shadowmage.ancientwarfare.structure.model;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/*
 * Client-only. Registers the layer definitions of every structure model; call from the module client entry point
 * (mod bus listener for EntityRenderersEvent.RegisterLayerDefinitions).
 */
public final class StructureModelLayers {
	private StructureModelLayers() {}

	public static void register(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(ModelCoffin.LAYER, ModelCoffin::createLayer);
		event.registerLayerDefinition(ModelGateBasic.LAYER, ModelGateBasic::createLayer);
		event.registerLayerDefinition(ModelGateBridge.LAYER, ModelGateBridge::createLayer);
		event.registerLayerDefinition(ModelStoneCoffin.LAYER, ModelStoneCoffin::createLayer);
	}
}
