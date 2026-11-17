package net.shadowmage.ancientwarfare.core.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.container.ContainerBase;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/*
 * Port notes (1.12 -> 1.21.1):
 * - IGuiHandler is gone. Every original integer GUI id maps to one MenuType registered as "ancientwarfare:gui_<id>".
 *   The (x, y, z) ints that 1.12 passed through openGui travel as menu extra data.
 * - Containers keep their (player, x, y, z) constructor shape; registerContainer takes a factory instead of a class.
 *   MenuType and window id reach ContainerBase through a thread local set right before the factory is invoked.
 * - registerContainer / registerPacketType must run during mod construction (the registry and payload events fire later).
 * - registerGui (client screens) lives in NetworkHandlerClient so that this class never touches client classes.
 */
public final class NetworkHandler {

	public static final NetworkHandler INSTANCE = new NetworkHandler();

	//public static final int PACKET_TEST = 0;//unused id
	private static final int PACKET_GUI = 1;
	private static final int PACKET_GUI_TO_CLIENT = 38;//port addition
	public static final int PACKET_STRUCTURE = 2;
	public static final int PACKET_ITEM_KEY_INTERFACE = 3;
	public static final int PACKET_ENTITY = 5;
	public static final int PACKET_RESEARCH_INIT = 6;
	public static final int PACKET_RESEARCH_ADD = 7;
	public static final int PACKET_RESEARCH_START = 8;
	public static final int PACKET_STRUCTURE_REMOVE = 11;
	public static final int PACKET_NPC_COMMAND = 12;
	public static final int PACKET_FACTION_UPDATE = 13;
	public static final int PACKET_BLOCK_EVENT = 14;

	public static final int PACKET_AIM_UPDATE = 15;
	public static final int PACKET_AMMO_SELECT = 16;
	public static final int PACKET_AMMO_UPDATE = 17;
	public static final int PACKET_FIRE_UPDATE = 18;
	public static final int PACKET_PACK_COMMAND = 19;
	public static final int PACKET_SINGLE_AMMO_UPDATE = 20;
	public static final int PACKET_TURRET_ANGLES_UPDATE = 21;
	public static final int PACKET_UPGRADE_UPDATE = 22;
	public static final int PACKET_VEHICLE_INPUT = 23;
	public static final int PACKET_VEHICLE_MOVE = 24;

	public static final int PACKET_JEI_TRANSFER_RECIPE = 25;

	public static final int PACKET_MANUAL_RELOAD = 26;

	public static final int PACKET_EXTENDED_REACH_ATTACK = 27;

	public static final int PACKET_STRUCTURE_MAP = 28;
	public static final int PACKET_STRUCTURE_ENTRY = 29;

	public static final int PACKET_SOUND_BLOCK_PLAYER_SPEC_VALUES = 30;

	public static final int PACKET_TEAM_MEMBERSHIP_UPDATE = 31;
	public static final int PACKET_TEAM_STANDINGS_UPDATE = 32;
	public static final int PACKET_TEAM_STANDING_UPDATE = 33;

	public static final int PACKET_HIGHLIGHT_BLOCK = 34;
	public static final int PACKET_SHOW_BBS = 35;
	public static final int PACKET_ITEM_MOUSE_SCROLL = 36;
	public static final int PACKET_STRUCTURE_REQUEST = 37;//port addition
	// ids 100+ are free for packets that did not exist in 1.12 (port additions)

	public static final int GUI_CRAFTING = 0;
	public static final int GUI_SCANNER = 1;
	public static final int GUI_BUILDER = 2;
	public static final int GUI_NPC_INVENTORY = 4;
	public static final int GUI_WORKSITE_INVENTORY_SIDE_ADJUST = 5;
	public static final int GUI_NPC_TRADE_ORDER = 6;
	public static final int GUI_SPAWNER_ADVANCED = 7;
	public static final int GUI_SPAWNER_ADVANCED_BLOCK = 8;
	public static final int GUI_SPAWNER_ADVANCED_INVENTORY = 9;
	public static final int GUI_SPAWNER_ADVANCED_BLOCK_INVENTORY = 10;
	public static final int GUI_GATE_CONTROL = 11;
	public static final int GUI_RESEARCH_STATION = 12;
	public static final int GUI_DRAFTING_STATION = 13;
	public static final int GUI_WORKSITE_ANIMAL_CONTROL = 14;
	public static final int GUI_WORKSITE_AUTO_CRAFT = 15;
	public static final int GUI_WORKSITE_FISH_CONTROL = 16;
	public static final int GUI_MAILBOX_INVENTORY = 17;
	public static final int GUI_WAREHOUSE_CONTROL = 18;
	public static final int GUI_WAREHOUSE_STORAGE = 19;
	public static final int GUI_WAREHOUSE_STOCK = 20;
	public static final int GUI_WAREHOUSE_OUTPUT = 21;
	public static final int GUI_WAREHOUSE_CRAFTING = 22;
	public static final int GUI_CHUNK_LOADER_DELUXE = 23;
	public static final int GUI_WORKSITE_QUARRY = 24;
	public static final int GUI_WORKSITE_TREE_FARM = 25;
	public static final int GUI_WORKSITE_ANIMAL_FARM = 26;
	public static final int GUI_WORKSITE_CROP_FARM = 27;
	public static final int GUI_WORKSITE_FISH_FARM = 29;
	public static final int GUI_WORKSITE_QUARRY_BOUNDS = 30;
	public static final int GUI_STIRLING_GENERATOR = 31;
	public static final int GUI_WAREHOUSE_STOCK_LINKER = 32;
	public static final int GUI_NPC_WORK_ORDER = 34;
	public static final int GUI_NPC_UPKEEP_ORDER = 35;
	public static final int GUI_NPC_COMBAT_ORDER = 36;
	public static final int GUI_NPC_ROUTING_ORDER = 37;
	public static final int GUI_NPC_FACTION_TRADE_SETUP = 39;
	public static final int GUI_BACKPACK = 40;
	public static final int GUI_NPC_TOWN_HALL = 41;
	public static final int GUI_NPC_FACTION_TRADE_VIEW = 42;
	public static final int GUI_NPC_BARD = 43;
	public static final int GUI_NPC_CREATIVE = 44;
	public static final int GUI_RESEARCH_BOOK = 45;
	public static final int GUI_WORKSITE_BOUNDS = 46;
	public static final int GUI_NPC_PLAYER_OWNED_TRADE = 47;
	public static final int GUI_SOUND_BLOCK = 48;
	public static final int GUI_NPC_FACTION_BARD = 49;

	public static final int GUI_VEHICLE_AMMO_SELECTION = 50;
	public static final int GUI_VEHICLE_INVENTORY = 51;
	public static final int GUI_VEHICLE_STATS = 52;

	public static final int GUI_WORKSITE_FRUIT_FARM = 53;

	public static final int GUI_TOWN_BUILDER = 54;
	public static final int GUI_LOOT_CHEST_PLACER = 55;
	public static final int GUI_MANUAL = 56;
	public static final int GUI_INFO_TOOL = 57;

	public static final int GUI_GATE_CONTROL_CREATIVE = 58;

	public static final int GUI_LOOT_BASKET = 59;
	public static final int GUI_STAKE = 60;
	public static final int GUI_STATUE = 61;
	public static final int GUI_NPC_FACTION_SPELLCASTER_WIZARDRY = 62;
	// ids 100+ are free for GUIs that did not exist in 1.12 (port additions)

	@FunctionalInterface
	public interface ContainerFactory {
		ContainerBase create(Player player, int x, int y, int z);
	}

	/* MenuType + window id handed to the ContainerBase constructor */
	public record PendingMenu(MenuType<?> type, int windowId) {}

	private static final ThreadLocal<PendingMenu> PENDING = new ThreadLocal<>();

	public static PendingMenu consumePendingMenu() {
		PendingMenu p = PENDING.get();
		if (p == null) {
			throw new IllegalStateException("AW containers must be created through NetworkHandler.openGui / their registered MenuType");
		}
		PENDING.remove();
		return p;
	}

	private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AncientWarfare.MOD_ID);

	private final Map<Integer, ContainerFactory> containerFactories = new HashMap<>();
	private final Map<Integer, Supplier<MenuType<ContainerBase>>> menuTypes = new HashMap<>();

	private NetworkHandler() {
	}

	public void registerNetwork(IEventBus modBus) {
		MENUS.register(modBus);
		modBus.addListener(this::registerPayloads);
		PacketBase.registerPacketType(PACKET_GUI, PacketGui.class, PacketGui::new, PacketBase.Direction.TO_SERVER);
		PacketBase.registerPacketType(PACKET_GUI_TO_CLIENT, PacketGuiToClient.class, PacketGuiToClient::new, PacketBase.Direction.TO_CLIENT);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void registerPayloads(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");
		PacketBase.frozen = true;
		for (PacketBase.Registration reg : PacketBase.packetTypes.values()) {
			switch (reg.direction) {
				case TO_CLIENT -> registrar.playToClient(reg.type, reg.codec, (pkt, ctx) -> ((PacketBase) pkt).handle(ctx.player()));
				case TO_SERVER -> registrar.playToServer(reg.type, reg.codec, (pkt, ctx) -> ((PacketBase) pkt).handle(ctx.player()));
				default -> registrar.playBidirectional(reg.type, reg.codec, (pkt, ctx) -> ((PacketBase) pkt).handle(ctx.player()));
			}
		}
	}

	public static void sendToServer(PacketBase pkt) {
		PacketDistributor.sendToServer(pkt);
	}

	/*
	 * Port addition: a player whose connection never negotiated the mod's channels (fake players of other mods, the
	 * mock players of GameTests) cannot be sent an AW packet; NeoForge throws for it. Such players are skipped.
	 */
	private static boolean canReceive(ServerPlayer player, PacketBase pkt) {
		return player.connection != null && net.neoforged.neoforge.network.registration.NetworkRegistry.hasChannel(player.connection, pkt.type().id());
	}

	public static void sendToPlayer(ServerPlayer player, PacketBase pkt) {
		if (pkt.getClass() == PacketGui.class) {
			pkt = new PacketGuiToClient((PacketGui) pkt);
		}
		if (canReceive(player, pkt)) {
			PacketDistributor.sendToPlayer(player, pkt);
		}
	}

	public static void sendToAllPlayers(PacketBase pkt) {
		if (ServerLifecycleHooks.getCurrentServer() != null) {
			for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
				sendToPlayer(player, pkt);
			}
		}
	}

	public static void sendToAllTracking(Entity e, PacketBase pkt) {
		PacketDistributor.sendToPlayersTrackingEntity(e, pkt);
	}

	public static void sendToAllTrackingChunk(Level world, int cx, int cz, PacketBase pkt) {
		if (world instanceof ServerLevel) {
			PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) world, new ChunkPos(cx, cz), pkt);
		}
	}

	/*
	 * Must be called during mod construction.
	 */
	public static void registerContainer(int id, ContainerFactory factory) {
		if (INSTANCE.containerFactories.put(id, factory) != null) {
			throw new IllegalStateException("Duplicate AW gui id " + id);
		}
		final Supplier<MenuType<ContainerBase>>[] holder = new Supplier[1];
		holder[0] = MENUS.register("gui_" + id, () -> IMenuTypeExtension.create((windowId, inv, buf) -> {
			int x = buf.readInt();
			int y = buf.readInt();
			int z = buf.readInt();
			return INSTANCE.createContainer(holder[0].get(), windowId, id, inv.player, x, y, z, true);
		}));
		INSTANCE.menuTypes.put(id, holder[0]);
	}

	@Nullable
	public static MenuType<ContainerBase> getMenuType(int id) {
		Supplier<MenuType<ContainerBase>> sup = INSTANCE.menuTypes.get(id);
		return sup == null ? null : sup.get();
	}

	public static Iterable<Integer> getRegisteredGuiIds() {
		return INSTANCE.menuTypes.keySet();
	}

	@Nullable
	private ContainerBase createContainer(MenuType<?> type, int windowId, int id, Player player, int x, int y, int z, boolean client) {
		ContainerFactory factory = containerFactories.get(id);
		if (factory == null) {
			return client ? ContainerBase.createPlaceholder(player, type, windowId) : null;
		}
		PENDING.set(new PendingMenu(type, windowId));
		try {
			return factory.create(player, x, y, z);
		}
		catch (Exception e) {
			AncientWarfare.LOG.error("Error instantiating GUI container {} on {}: ", id, client ? "client" : "server", e);
			if (client) {
				PENDING.remove();
				return ContainerBase.createPlaceholder(player, type, windowId); //no disconnect, the screen closes itself
			}
			return null;
		}
		finally {
			PENDING.remove();
		}
	}

	/*
	 * which GUIs a client may ask for (PacketGui "openGui"). 1.12 opened any id at any position or entity for
	 * any client. Every other GUI is opened by the server itself (block / item / entity interaction). A client request
	 * is only accepted for a registered id, from an open Ancient Warfare GUI on the same block or entity (the buttons
	 * that switch between the GUIs of one thing), and when the rule of the id agrees.
	 */
	private final Map<Integer, java.util.function.BiPredicate<Player, ContainerBase>> clientOpenRules = new HashMap<>();

	public static void allowClientOpen(int id, java.util.function.BiPredicate<Player, ContainerBase> rule) {
		INSTANCE.clientOpenRules.put(id, rule);
	}

	public static void allowClientOpen(int id) {
		allowClientOpen(id, (player, open) -> true);
	}

	public boolean clientMayOpen(Player player, int id, int x, int y, int z) {
		java.util.function.BiPredicate<Player, ContainerBase> rule = clientOpenRules.get(id);
		if (rule == null || !(player.containerMenu instanceof ContainerBase open)) {
			return false;
		}
		boolean sameTarget;
		if (open instanceof net.shadowmage.ancientwarfare.core.container.ContainerTileBase<?> tile) {
			sameTarget = tile.tileEntity.getBlockPos().equals(new BlockPos(x, y, z));
		} else if (open instanceof net.shadowmage.ancientwarfare.core.container.ContainerEntityBase<?> ent) {
			sameTarget = ent.entity.getId() == x;
		} else {
			sameTarget = false;
		}
		return sameTarget && open.canInteractWith(player) && rule.test(player, open);
	}

	public final void openGui(Player player, int id, BlockPos pos) {
		openGui(player, id, pos.getX(), pos.getY(), pos.getZ());
	}

	public final void openGui(Player player, int guiId) {
		openGui(player, guiId, 0);
	}

	public final void openGui(Player player, int guiId, int entityId) {
		openGui(player, guiId, entityId, 0, 0);
	}

	/*
	 * GameTests: the server container of a GUI without opening it for the player. A mock player has no connection
	 * that takes the open-screen payload, and a mock survival player is no ServerPlayer at all.
	 */
	@Nullable
	public static ContainerBase createContainerForTest(int id, Player player, int x, int y, int z) {
		return INSTANCE.createContainer(null, 0, id, player, x, y, z, false);
	}

	public final void openGui(Player player, int id, int x, int y, int z) {
		if (player.level().isClientSide) {
			PacketGui pkt = new PacketGui();
			pkt.setOpenGui(id, x, y, z);
			sendToServer(pkt);
		} else {
			MenuType<ContainerBase> type = getMenuType(id);
			if (type == null) {
				AncientWarfare.LOG.error("Attempt to open unregistered AW gui id {}", id);
				return;
			}
			MenuProvider provider = new SimpleMenuProvider((windowId, inv, p) -> createContainer(type, windowId, id, p, x, y, z, false), Component.empty());
			player.openMenu(provider, buf -> {
				buf.writeInt(x);
				buf.writeInt(y);
				buf.writeInt(z);
			});
			if (player.containerMenu instanceof ContainerBase) {
				((ContainerBase) player.containerMenu).sendInitData();
			}
		}
	}
}
