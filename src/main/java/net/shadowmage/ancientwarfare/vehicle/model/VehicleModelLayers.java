package net.shadowmage.ancientwarfare.vehicle.model;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/*
 * Client-only. Registers the layer definitions of every vehicle model; call from the module client entry point
 * (mod bus listener for EntityRenderersEvent.RegisterLayerDefinitions).
 */
public final class VehicleModelLayers {
	private VehicleModelLayers() {}

	public static void register(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(ModelArrow2.LAYER, ModelArrow2::createLayer);
		event.registerLayerDefinition(ModelBallistaStand.LAYER, ModelBallistaStand::createLayer);
		event.registerLayerDefinition(ModelCannonStandFixed.LAYER, ModelCannonStandFixed::createLayer);
		event.registerLayerDefinition(ModelCannonStandTurret.LAYER, ModelCannonStandTurret::createLayer);
		event.registerLayerDefinition(ModelCatapultStandFixed.LAYER, ModelCatapultStandFixed::createLayer);
		event.registerLayerDefinition(ModelCatapultStandTurret.LAYER, ModelCatapultStandTurret::createLayer);
		event.registerLayerDefinition(ModelHwacha.LAYER, ModelHwacha::createLayer);
		event.registerLayerDefinition(ModelShot.LAYER, ModelShot::createLayer);
		event.registerLayerDefinition(ModelTrebuchetStandFixed.LAYER, ModelTrebuchetStandFixed::createLayer);
		event.registerLayerDefinition(ModelTrebuchetStandTurret.LAYER, ModelTrebuchetStandTurret::createLayer);
	}
}
