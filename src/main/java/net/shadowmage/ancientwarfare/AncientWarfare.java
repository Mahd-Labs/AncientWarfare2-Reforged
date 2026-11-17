package net.shadowmage.ancientwarfare;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.shadowmage.ancientwarfare.automation.AncientWarfareAutomation;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.vehicle.AncientWarfareVehicles;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 * Ancient Warfare 2 - NeoForge 1.21.1 port.
 * Original mod Copyright (C) 2013-2015 Shadowmage, 1.12.x maintenance by P3pp3rF1y and contributors. GPL-3.0.
 *
 * 1.12 shipped five @Mod containers (core, automation, npc, structure, vehicle). The port is one mod whose
 * modules are initialised in the original dependency order.
 */
@Mod(AncientWarfare.MOD_ID)
public class AncientWarfare {
	public static final String MOD_ID = "ancientwarfare";
	public static final Logger LOG = LogManager.getLogger(MOD_ID);

	public AncientWarfare(IEventBus modBus, ModContainer container) {
		NetworkHandler.INSTANCE.registerNetwork(modBus);
		AncientWarfareCore.init(modBus, container);
		AncientWarfareAutomation.init(modBus, container);
		AncientWarfareNPC.init(modBus, container);
		AncientWarfareStructure.init(modBus, container);
		AncientWarfareVehicles.init(modBus, container);
	}
}
