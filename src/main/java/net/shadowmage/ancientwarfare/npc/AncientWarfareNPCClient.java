package net.shadowmage.ancientwarfare.npc;

import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.NetworkHandlerClient;
import net.shadowmage.ancientwarfare.npc.gui.GuiNpcInventory;
import net.shadowmage.ancientwarfare.npc.gui.GuiNpcFactionTradeSetup;
import net.shadowmage.ancientwarfare.npc.gui.GuiNpcFactionTradeView;
import net.shadowmage.ancientwarfare.npc.gui.GuiWorkOrder;
import net.shadowmage.ancientwarfare.npc.gui.GuiUpkeepOrder;
import net.shadowmage.ancientwarfare.npc.gui.GuiCombatOrder;
import net.shadowmage.ancientwarfare.npc.gui.GuiRoutingOrder;
import net.shadowmage.ancientwarfare.npc.gui.GuiTownHallInventory;
import net.shadowmage.ancientwarfare.npc.gui.GuiNpcBard;
import net.shadowmage.ancientwarfare.npc.gui.GuiNpcCreativeControls;
import net.shadowmage.ancientwarfare.npc.gui.GuiTradeOrder;
import net.shadowmage.ancientwarfare.npc.gui.GuiNpcPlayerOwnedTrade;
import net.shadowmage.ancientwarfare.npc.gui.GuiNpcFactionBard;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredItem;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.init.AWCoreItems;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.shadowmage.ancientwarfare.npc.client.ExtendedReachHandler;
import net.shadowmage.ancientwarfare.npc.client.NPCItemColors;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFaction;
import net.shadowmage.ancientwarfare.npc.init.AWNPCEntities;
import net.shadowmage.ancientwarfare.npc.init.AWNPCItems;
import net.shadowmage.ancientwarfare.npc.item.ItemNpcSpawner;
import net.shadowmage.ancientwarfare.npc.item.ItemShield;
import net.shadowmage.ancientwarfare.npc.registry.FactionRegistry;
import net.shadowmage.ancientwarfare.npc.registry.NpcDefaultsRegistry;
import net.shadowmage.ancientwarfare.npc.render.RenderCommandOverlay;
import net.shadowmage.ancientwarfare.npc.render.RenderNpcBase;
import net.shadowmage.ancientwarfare.npc.render.RenderNpcFaction;
import net.shadowmage.ancientwarfare.npc.render.RenderWorkLines;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/*
 * Client-only module entry point (replaces the 1.12 NpcClientProxy). Called from AncientWarfareClient,
 * which only loads on the physical client. Never reference this class from common code.
 *
 * Ported: entity renderers (RenderNpcBase for the owned types, RenderNpcFaction for the faction types; the no
 * friendly fire potion uses the vanilla potion entity type and renderer), the work line and command overlays, the
 * extended reach click handler, item colours, item properties (shield "blocking", spawner "npc_variant").
 * GUI screens (npc.gui) are registered below. Not ported yet: the client options screen entries.
 */
public final class AncientWarfareNPCClient {
	private AncientWarfareNPCClient() {}

	/* order of models/item/npc_spawner_variants.txt: the item property value is index / 100 */
	private static final List<String> SPAWNER_VARIANTS = Arrays.asList("siege_engineer", "archer", "bard", "commander", "courier", "craftsman", "engineer", "farmer",
			"lumberjack", "medic", "miner", "priest", "researcher", "trader", "soldier", "civilian_female", "civilian_male", "spellcaster");

	public static void init(IEventBus modBus, ModContainer container) {
		//1.12 NpcClientProxy.registerGui calls (the Electroblob wizardry spellcaster gui is dropped with that integration)
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_NPC_INVENTORY, GuiNpcInventory::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_NPC_FACTION_TRADE_SETUP, GuiNpcFactionTradeSetup::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_NPC_FACTION_TRADE_VIEW, GuiNpcFactionTradeView::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_NPC_WORK_ORDER, GuiWorkOrder::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_NPC_UPKEEP_ORDER, GuiUpkeepOrder::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_NPC_COMBAT_ORDER, GuiCombatOrder::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_NPC_ROUTING_ORDER, GuiRoutingOrder::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_NPC_TOWN_HALL, GuiTownHallInventory::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_NPC_BARD, GuiNpcBard::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_NPC_CREATIVE, GuiNpcCreativeControls::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_NPC_TRADE_ORDER, GuiTradeOrder::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_NPC_PLAYER_OWNED_TRADE, GuiNpcPlayerOwnedTrade::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_NPC_FACTION_BARD, GuiNpcFactionBard::new);

		modBus.addListener(AncientWarfareNPCClient::registerRenderers);
		modBus.addListener(NPCItemColors::init);
		modBus.addListener(AncientWarfareNPCClient::clientSetup);

		NeoForge.EVENT_BUS.register(RenderWorkLines.INSTANCE);
		NeoForge.EVENT_BUS.register(RenderCommandOverlay.INSTANCE);
		net.shadowmage.ancientwarfare.npc.item.ItemCommandBaton.clientEntities = level -> level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel
				? clientLevel.entitiesForRendering() : java.util.Collections.emptyList();
		NeoForge.EVENT_BUS.register(new ExtendedReachHandler());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		for (AWNPCEntities.NpcDeclaration declaration : AWNPCEntities.getNpcMap().values()) {
			if (declaration instanceof AWNPCEntities.NpcFactionDeclaration) {
				event.registerEntityRenderer((EntityType<NpcFaction>) declaration.getEntityType(), RenderNpcFaction::new);
			} else {
				event.registerEntityRenderer((EntityType) declaration.getEntityType(), (EntityRendererProvider) (EntityRendererProvider<NpcBase>) RenderNpcBase::new);
			}
		}
	}

	private static void clientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			//1.12 ItemShield "blocking" property override
			ResourceLocation blocking = ResourceLocation.withDefaultNamespace("blocking");
			for (DeferredItem<? extends Item> holder : AWNPCItems.allItems()) {
				if (holder.get() instanceof ItemShield) {
					ItemProperties.register(holder.get(), blocking, (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
				}
			}
			//1.12 ItemNpcSpawner custom mesh definition (model variant per npc type / sub type)
			ItemProperties.register(AWNPCItems.NPC_SPAWNER.get(), ResourceLocation.fromNamespaceAndPath(AncientWarfare.MOD_ID, "npc_variant"), (stack, level, entity, seed) -> {
				String variant = ItemNpcSpawner.getModelVariant(stack);
				int index = variant == null ? -1 : SPAWNER_VARIANTS.indexOf(variant);
				return index < 0 ? 0 : index / 100.0F;
			});

			if (net.shadowmage.ancientwarfare.core.util.ShowcaseHooks.isEnabled()) {
				registerShowcaseVariants();
			}
		});
	}

	/*
	 * Dev showcase: one picture per owned sub type (main hand item decides the sub type) and per faction of every
	 * faction npc type, named like the 1.12 pictures (entity__aw_npc_combat@archer, entity__faction.archer@amazon).
	 */
	private static void registerShowcaseVariants() {
		Map<String, ItemStack> combat = new TreeMap<>();
		combat.put("commander", new ItemStack(AWNPCItems.IRON_COMMAND_BATON.get()));
		combat.put("soldier", new ItemStack(Items.IRON_SWORD));
		combat.put("archer", new ItemStack(Items.BOW));
		combat.put("engineer", new ItemStack(AWCoreItems.IRON_HAMMER.get()));
		combat.put("medic", new ItemStack(Items.IRON_AXE));
		Map<String, ItemStack> worker = new TreeMap<>();
		worker.put("farmer", new ItemStack(Items.IRON_HOE));
		worker.put("miner", new ItemStack(Items.IRON_PICKAXE));
		worker.put("lumberjack", new ItemStack(Items.IRON_AXE));
		worker.put("researcher", new ItemStack(AWCoreItems.IRON_QUILL.get()));
		worker.put("craftsman", new ItemStack(AWCoreItems.IRON_HAMMER.get()));
		registerEquipmentVariants("combat", combat);
		registerEquipmentVariants("worker", worker);

		for (AWNPCEntities.NpcDeclaration declaration : AWNPCEntities.getNpcMap().values()) {
			if (!(declaration instanceof AWNPCEntities.NpcFactionDeclaration)) {
				continue;
			}
			ResourceLocation typeId = ResourceLocation.fromNamespaceAndPath(AncientWarfare.MOD_ID, declaration.name());
			String shortType = declaration.getNpcType().substring("faction.".length());
			for (String faction : new java.util.TreeSet<>(FactionRegistry.getFactionNames())) {
				if (NpcDefaultsRegistry.getFactionNpcDefault(faction, shortType).isEnabled()) {
					CompoundTag nbt = new CompoundTag();
					nbt.putString("factionName", faction);
					nbt.putBoolean(NpcFaction.APPLY_DEFAULTS_TAG, true);
					nbt.put("HandItems", new ListTag());
					nbt.put("ArmorItems", new ListTag());
					net.shadowmage.ancientwarfare.core.util.ShowcaseHooks.registerEntityVariant(typeId, faction, nbt);
				}
			}
		}
	}

	private static void registerEquipmentVariants(String npcType, Map<String, ItemStack> equipment) {
		ResourceLocation typeId = ResourceLocation.fromNamespaceAndPath(AncientWarfare.MOD_ID, AWNPCEntities.getNpcDeclaration(npcType).name());
		equipment.forEach((subType, stack) -> {
			CompoundTag nbt = new CompoundTag();
			ListTag hands = new ListTag();
			hands.add(NBTHelper.writeItemStack(stack, new CompoundTag()));
			hands.add(new CompoundTag());
			nbt.put("HandItems", hands);
			net.shadowmage.ancientwarfare.core.util.ShowcaseHooks.registerEntityVariant(typeId, subType, nbt);
		});
	}
}
