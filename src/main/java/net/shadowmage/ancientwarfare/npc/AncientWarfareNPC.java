package net.shadowmage.ancientwarfare.npc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.gamedata.WorldData;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.PacketBase;
import net.shadowmage.ancientwarfare.core.registry.RegistryLoader;
import net.shadowmage.ancientwarfare.npc.command.CommandDebugAI;
import net.shadowmage.ancientwarfare.npc.command.CommandFaction;
import net.shadowmage.ancientwarfare.npc.command.CommandTeams;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;
import net.shadowmage.ancientwarfare.npc.container.ContainerCombatOrder;
import net.shadowmage.ancientwarfare.npc.container.ContainerNpcBard;
import net.shadowmage.ancientwarfare.npc.container.ContainerNpcCreativeControls;
import net.shadowmage.ancientwarfare.npc.container.ContainerNpcFactionBard;
import net.shadowmage.ancientwarfare.npc.container.ContainerNpcFactionTradeSetup;
import net.shadowmage.ancientwarfare.npc.container.ContainerNpcFactionTradeView;
import net.shadowmage.ancientwarfare.npc.container.ContainerNpcInventory;
import net.shadowmage.ancientwarfare.npc.container.ContainerNpcPlayerOwnedTrade;
import net.shadowmage.ancientwarfare.npc.container.ContainerRoutingOrder;
import net.shadowmage.ancientwarfare.npc.container.ContainerTownHall;
import net.shadowmage.ancientwarfare.npc.container.ContainerTradeOrder;
import net.shadowmage.ancientwarfare.npc.container.ContainerUpkeepOrder;
import net.shadowmage.ancientwarfare.npc.container.ContainerWorkOrder;
import net.shadowmage.ancientwarfare.npc.crafting.AWNpcCrafting;
import net.shadowmage.ancientwarfare.npc.faction.FactionTracker;
import net.shadowmage.ancientwarfare.npc.init.AWNPCBlocks;
import net.shadowmage.ancientwarfare.npc.init.AWNPCEntities;
import net.shadowmage.ancientwarfare.npc.init.AWNPCItems;
import net.shadowmage.ancientwarfare.npc.init.AWNPCSounds;
import net.shadowmage.ancientwarfare.npc.network.PacketExtendedReachAttack;
import net.shadowmage.ancientwarfare.npc.network.PacketFactionUpdate;
import net.shadowmage.ancientwarfare.npc.network.PacketNpcCommand;
import net.shadowmage.ancientwarfare.npc.network.PacketTeamMembershipUpdate;
import net.shadowmage.ancientwarfare.npc.network.PacketTeamStandingUpdate;
import net.shadowmage.ancientwarfare.npc.network.PacketTeamStandingsUpdate;
import net.shadowmage.ancientwarfare.npc.registry.FactionRegistry;
import net.shadowmage.ancientwarfare.npc.registry.FactionTradeListRegistry;
import net.shadowmage.ancientwarfare.npc.registry.NpcDefaultsRegistry;
import net.shadowmage.ancientwarfare.npc.registry.TargetRegistry;
import net.shadowmage.ancientwarfare.npc.skin.NpcSkinManager;
import org.apache.logging.log4j.Logger;

/*
 * Module entry point. 1.12 had one @Mod per module; the port is a single mod ("ancientwarfare") and
 * AncientWarfare calls init() of every module from the mod constructor.
 *
 * 1.12 life cycle -> port:
 *  preInit (config, containers, packets, registry parsers, event handlers) -> init(), i.e. mod construction
 *  registry events (items, blocks, entities, sounds, recipes)               -> DeferredRegisters set up in init()
 *  postInit (skins, sub type equipment, faction tracker, faction blocks)    -> FMLCommonSetupEvent, after core's
 *           RegistryLoader.load() (core registers its common setup listener first and both enqueue their work)
 *  serverStarting (commands)                                                -> RegisterCommandsEvent
 * The client proxy is replaced by AncientWarfareNPCClient.
 * Dropped: compat (Epic Siege, Twilight Forest, Electroblob's Wizardry incl. GUI_NPC_FACTION_SPELLCASTER_WIZARDRY),
 * the loot entity property "rides_vehicle" (vehicle module absent).
 */
public final class AncientWarfareNPC {
	private AncientWarfareNPC() {}

	public static final String MOD_ID = "ancientwarfare";
	public static final String MOD_PREFIX = MOD_ID + ":";

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = AWNPCTab.TAB;

	public static final Logger LOG = AncientWarfare.LOG;
	public static AWNPCStatics statics;

	public static void init(IEventBus modBus, ModContainer container) {
		statics = new AWNPCStatics("ancientwarfare-npc");
		statics.register(modBus, container);

		AWNPCBlocks.register(modBus);
		AWNPCItems.register(modBus);
		AWNPCEntities.register(modBus);
		AWNPCSounds.register(modBus);
		AWNPCTab.register(modBus);
		AWNpcCrafting.register(modBus);

		NeoForge.EVENT_BUS.register(net.shadowmage.ancientwarfare.npc.event.EventHandler.INSTANCE);
		NeoForge.EVENT_BUS.register(FactionTracker.INSTANCE);
		NeoForge.EVENT_BUS.addListener(AncientWarfareNPC::registerCommands);
		NeoForge.EVENT_BUS.addListener(AncientWarfareNPC::worldLoaded);

		NetworkHandler.registerContainer(NetworkHandler.GUI_NPC_INVENTORY, ContainerNpcInventory::new);
		//the GUIs a client may switch to from another GUI of the same NPC. The inventory needs command
		//permission over the NPC (or creative mode), the creative controls and the faction trade setup creative mode.
		java.util.function.BiPredicate<net.minecraft.world.entity.player.Player, net.shadowmage.ancientwarfare.core.container.ContainerBase> commandsNpc = (player, open) ->
				open instanceof net.shadowmage.ancientwarfare.core.container.ContainerEntityBase<?> c
				&& c.entity instanceof net.shadowmage.ancientwarfare.npc.entity.NpcBase npc
				&& (player.isCreative() || npc.hasCommandPermissions(player.getUUID(), player.getGameProfile().getName()));
		NetworkHandler.allowClientOpen(NetworkHandler.GUI_NPC_INVENTORY, commandsNpc);
		//the "alt" screens the NPC inventory switches to (NpcBard, NpcTrader, NpcFactionBard.openAltGui)
		NetworkHandler.allowClientOpen(NetworkHandler.GUI_NPC_BARD, commandsNpc);
		NetworkHandler.allowClientOpen(NetworkHandler.GUI_NPC_PLAYER_OWNED_TRADE, commandsNpc);
		NetworkHandler.allowClientOpen(NetworkHandler.GUI_NPC_FACTION_BARD, commandsNpc);
		NetworkHandler.allowClientOpen(NetworkHandler.GUI_NPC_CREATIVE, (player, open) -> player.isCreative());
		NetworkHandler.allowClientOpen(NetworkHandler.GUI_NPC_FACTION_TRADE_SETUP, (player, open) -> player.isCreative());
		NetworkHandler.registerContainer(NetworkHandler.GUI_NPC_WORK_ORDER, ContainerWorkOrder::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_NPC_UPKEEP_ORDER, ContainerUpkeepOrder::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_NPC_COMBAT_ORDER, ContainerCombatOrder::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_NPC_FACTION_TRADE_SETUP, ContainerNpcFactionTradeSetup::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_NPC_FACTION_TRADE_VIEW, ContainerNpcFactionTradeView::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_NPC_ROUTING_ORDER, ContainerRoutingOrder::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_NPC_TOWN_HALL, ContainerTownHall::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_NPC_BARD, ContainerNpcBard::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_NPC_CREATIVE, ContainerNpcCreativeControls::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_NPC_TRADE_ORDER, ContainerTradeOrder::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_NPC_PLAYER_OWNED_TRADE, ContainerNpcPlayerOwnedTrade::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_NPC_FACTION_BARD, ContainerNpcFactionBard::new);

		PacketBase.registerPacketType(NetworkHandler.PACKET_NPC_COMMAND, PacketNpcCommand.class, PacketNpcCommand::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_SERVER);
		PacketBase.registerPacketType(NetworkHandler.PACKET_FACTION_UPDATE, PacketFactionUpdate.class, PacketFactionUpdate::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_CLIENT);
		PacketBase.registerPacketType(NetworkHandler.PACKET_EXTENDED_REACH_ATTACK, PacketExtendedReachAttack.class, PacketExtendedReachAttack::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_SERVER);
		// TODO-CROSS(structure): PacketBase.registerPacketType(NetworkHandler.PACKET_STRUCTURE_ENTRY, PacketStructureEntry.class, PacketStructureEntry::new) - the structure module registers its own packet when it is ported
		PacketBase.registerPacketType(NetworkHandler.PACKET_TEAM_MEMBERSHIP_UPDATE, PacketTeamMembershipUpdate.class, PacketTeamMembershipUpdate::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_CLIENT);
		PacketBase.registerPacketType(NetworkHandler.PACKET_TEAM_STANDINGS_UPDATE, PacketTeamStandingsUpdate.class, PacketTeamStandingsUpdate::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_CLIENT);
		PacketBase.registerPacketType(NetworkHandler.PACKET_TEAM_STANDING_UPDATE, PacketTeamStandingUpdate.class, PacketTeamStandingUpdate::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_CLIENT);

		RegistryLoader.registerParser(new FactionRegistry.FactionParser());
		RegistryLoader.registerParser(new TargetRegistry.TargetListParser());
		RegistryLoader.registerParser(new NpcDefaultsRegistry.FactionNpcDefaultsParser());
		RegistryLoader.registerParser(new NpcDefaultsRegistry.OwnedNpcDefaultsParser());
		RegistryLoader.registerParser(new FactionTradeListRegistry.Parser());

		modBus.addListener(AncientWarfareNPC::commonSetup);
	}

	/*
	 * 1.12 postInit()
	 */
	private static void commonSetup(FMLCommonSetupEvent evt) {
		evt.enqueueWork(() -> {
			NpcSkinManager.loadSkins();
			AWNPCEntities.loadNpcSubtypeEquipment();
			AWNPCItems.addFactionBlocks();
		});
	}

	private static void registerCommands(RegisterCommandsEvent evt) {
		new CommandFaction().register(evt.getDispatcher());
		new CommandTeams().register(evt.getDispatcher());
		new CommandDebugAI().register(evt.getDispatcher());
	}

	private static void worldLoaded(LevelEvent.Load evt) {
		if (evt.getLevel() instanceof ServerLevel) {
			WorldData d = AWGameData.INSTANCE.getPerWorldData((ServerLevel) evt.getLevel(), WorldData.class);
			if (d != null) {
				AWNPCStatics.npcAIDebugMode = d.get("NpcAIDebugMode");
			}
		}
	}
}
