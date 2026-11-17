package net.shadowmage.ancientwarfare.showcase;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.automation.gui.GuiChunkLoaderDeluxe;
import net.shadowmage.ancientwarfare.automation.gui.GuiMailboxInventory;
import net.shadowmage.ancientwarfare.automation.gui.GuiMailboxInventorySideSetup;
import net.shadowmage.ancientwarfare.automation.gui.GuiMailboxNameSelect;
import net.shadowmage.ancientwarfare.automation.gui.GuiStirlingGenerator;
import net.shadowmage.ancientwarfare.automation.gui.GuiWarehouseControl;
import net.shadowmage.ancientwarfare.automation.gui.GuiWarehouseCraftingStation;
import net.shadowmage.ancientwarfare.automation.gui.GuiWarehouseInterface;
import net.shadowmage.ancientwarfare.automation.gui.GuiWarehouseStockLinker;
import net.shadowmage.ancientwarfare.automation.gui.GuiWarehouseStockViewer;
import net.shadowmage.ancientwarfare.automation.gui.GuiWarehouseStorage;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteAnimalControl;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteAnimalFarm;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteAutoCrafting;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteBoundsAdjust;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteCropFarm;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteFishControl;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteFishFarm;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteFruitFarm;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteInventorySideSelection;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteQuarry;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteQuarryBounds;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteTreeFarm;
import net.shadowmage.ancientwarfare.automation.tile.worksite.TileWorksiteBase;
import net.shadowmage.ancientwarfare.core.gui.GuiBackpack;
import net.shadowmage.ancientwarfare.core.gui.GuiInfoTool;
import net.shadowmage.ancientwarfare.core.gui.GuiResearchBook;
import net.shadowmage.ancientwarfare.core.gui.crafting.GuiEngineeringStation;
import net.shadowmage.ancientwarfare.core.gui.manual.GuiManual;
import net.shadowmage.ancientwarfare.core.gui.research.GuiResearchStation;
import net.shadowmage.ancientwarfare.core.interfaces.IBoundedSite;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.tile.TileOwned;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.NpcTrader;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionTrader;
import net.shadowmage.ancientwarfare.npc.gui.GuiCombatOrder;
import net.shadowmage.ancientwarfare.npc.gui.GuiNpcBard;
import net.shadowmage.ancientwarfare.npc.gui.GuiNpcCreativeControls;
import net.shadowmage.ancientwarfare.npc.gui.GuiNpcFactionBard;
import net.shadowmage.ancientwarfare.npc.gui.GuiNpcFactionTradeSetup;
import net.shadowmage.ancientwarfare.npc.gui.GuiNpcFactionTradeView;
import net.shadowmage.ancientwarfare.npc.gui.GuiNpcInventory;
import net.shadowmage.ancientwarfare.npc.gui.GuiNpcPlayerOwnedTrade;
import net.shadowmage.ancientwarfare.npc.gui.GuiRoutingOrder;
import net.shadowmage.ancientwarfare.npc.gui.GuiSkinSelection;
import net.shadowmage.ancientwarfare.npc.gui.GuiTownHallDeathList;
import net.shadowmage.ancientwarfare.npc.gui.GuiTownHallInventory;
import net.shadowmage.ancientwarfare.npc.gui.GuiTradeOrder;
import net.shadowmage.ancientwarfare.npc.gui.GuiTradeTemplateSave;
import net.shadowmage.ancientwarfare.npc.gui.GuiUpkeepOrder;
import net.shadowmage.ancientwarfare.npc.gui.GuiWorkOrder;
import net.shadowmage.ancientwarfare.npc.orders.CombatOrder;
import net.shadowmage.ancientwarfare.npc.orders.RoutingOrder;
import net.shadowmage.ancientwarfare.npc.orders.TradeOrder;
import net.shadowmage.ancientwarfare.npc.orders.UpkeepOrder;
import net.shadowmage.ancientwarfare.npc.orders.WorkOrder;
import net.shadowmage.ancientwarfare.npc.registry.FactionRegistry;
import net.shadowmage.ancientwarfare.npc.trade.FactionTrade;
import net.shadowmage.ancientwarfare.structure.gui.GuiDimensionSelection;
import net.shadowmage.ancientwarfare.structure.gui.GuiDraftingStation;
import net.shadowmage.ancientwarfare.structure.gui.GuiGateControl;
import net.shadowmage.ancientwarfare.structure.gui.GuiGateControlCreative;
import net.shadowmage.ancientwarfare.structure.gui.GuiLootBasket;
import net.shadowmage.ancientwarfare.structure.gui.GuiLootChestPlacer;
import net.shadowmage.ancientwarfare.structure.gui.GuiModSelection;
import net.shadowmage.ancientwarfare.structure.gui.GuiSoundBlock;
import net.shadowmage.ancientwarfare.structure.gui.GuiSoundSelect;
import net.shadowmage.ancientwarfare.structure.gui.GuiSpawnerAdvanced;
import net.shadowmage.ancientwarfare.structure.gui.GuiSpawnerAdvancedAddEntity;
import net.shadowmage.ancientwarfare.structure.gui.GuiSpawnerAdvancedEntitySelection;
import net.shadowmage.ancientwarfare.structure.gui.GuiSpawnerAdvancedInventory;
import net.shadowmage.ancientwarfare.structure.gui.GuiStake;
import net.shadowmage.ancientwarfare.structure.gui.GuiStatue;
import net.shadowmage.ancientwarfare.structure.gui.GuiStructureBiomeSelection;
import net.shadowmage.ancientwarfare.structure.gui.GuiStructureIncorrectName;
import net.shadowmage.ancientwarfare.structure.gui.GuiStructureScanner;
import net.shadowmage.ancientwarfare.structure.gui.GuiStructureSelection;
import net.shadowmage.ancientwarfare.structure.gui.GuiStructureSelectionDraftingStation;
import net.shadowmage.ancientwarfare.structure.gui.GuiStructureValidationSettings;
import net.shadowmage.ancientwarfare.structure.tile.SpawnerSettings;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;

/*
 * GUI pass of the dev showcase harness (client only, reached from ShowcaseHarness after the entity subjects).
 * For every entry the integrated server places the needed block / spawns the needed NPC next to the player, hands
 * over the needed item, then opens the gui through NetworkHandler.openGui exactly like the game does. The client
 * waits for the expected screen, optionally swaps in a sub screen (the ones the game opens from a button), saves
 * showcase/gui__<name>.png and closes the screen. Entries are selected by the harness filter against "gui/<name>".
 */
public final class ShowcaseGuis {
	private ShowcaseGuis() {}

	private interface Setup {
		int[] run(ServerLevel level, ServerPlayer player) throws Exception;
	}

	private record Entry(String name, int guiId, Class<?> screen, Setup setup, @Nullable Function<Screen, Screen> sub) {}

	private enum State {NEXT, SETUP, OPENING, SHOWN, CLOSING}

	private static final BlockPos POS = new BlockPos(0, 100, 0);
	private static final int OPEN_TIMEOUT = 60;

	private static final List<Entry> entries = new ArrayList<>();
	private static State state = State.NEXT;
	private static int index = -1;
	private static int ticks = 0;
	private static int frames = 0;
	private static boolean subDone = false;
	private static boolean cursorWarned = false;
	private static volatile int[] args;
	private static volatile String setupError;
	private static File outDir;
	private static List<String> failures;

	public static int count() {
		return entries.size();
	}

	/* @return false when the filter selects no gui entry (the harness then finishes right away) */
	static boolean start(Minecraft mc, File out, List<String> fails) {
		outDir = out;
		failures = fails;
		String filter = System.getProperty("aw.showcase.filter", "");
		Pattern pattern = filter.isEmpty() ? null : Pattern.compile(filter);
		List<Entry> all = new ArrayList<>();
		collect(all);
		for (Entry e : all) {
			if (pattern == null || pattern.matcher("gui/" + e.name()).find()) {
				entries.add(e);
			}
		}
		if (entries.isEmpty()) {
			return false;
		}
		NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Post evt) -> frames++);
		//gui scale 2 (640x360 gui units at 1280x720) so the tall worksite screens fit
		mc.options.guiScale().set(2);
		mc.resizeDisplay();
		IntegratedServer server = mc.getSingleplayerServer();
		if (server != null) {
			server.execute(() -> {
				ServerLevel level = server.overworld();
				for (BlockPos pos : BlockPos.betweenClosed(POS.getX() - 6, POS.getY() - 1, POS.getZ() - 6, POS.getX() + 6, POS.getY() - 1, POS.getZ() + 6)) {
					level.setBlock(pos, Blocks.SMOOTH_STONE.defaultBlockState(), 2);
				}
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					//held items do not exist in spectator mode
					player.setGameMode(GameType.CREATIVE);
					player.getAbilities().flying = true;
					player.onUpdateAbilities();
					player.teleportTo(level, POS.getX() + 0.5, POS.getY(), POS.getZ() + 3.5, 180F, 20F);
				}
			});
		}
		AncientWarfare.LOG.info("[showcase] gui pass, {} screens", entries.size());
		return true;
	}

	/* @return true when the pass is finished */
	static boolean tick(Minecraft mc) {
		IntegratedServer server = mc.getSingleplayerServer();
		if (server == null) {
			failures.add("gui pass: server gone");
			return true;
		}
		switch (state) {
			case NEXT -> {
				index++;
				if (index >= entries.size()) {
					return true;
				}
				Entry entry = entries.get(index);
				args = null;
				setupError = null;
				subDone = false;
				ticks = 0;
				state = State.SETUP;
				server.execute(() -> {
					try {
						ServerLevel level = server.overworld();
						ServerPlayer player = server.getPlayerList().getPlayers().get(0);
						clearStage(level, player);
						args = entry.setup().run(level, player);
					}
					catch (Throwable t) {
						AncientWarfare.LOG.error("[showcase] gui setup failed for {}", entry.name(), t);
						setupError = String.valueOf(t);
					}
				});
			}
			case SETUP -> {
				if (setupError != null) {
					fail("setup: " + setupError);
					state = State.NEXT;
				} else if (args != null && ++ticks >= 10) {
					//the held item / block entity / NPC must have reached the client before its container is built there
					Entry entry = entries.get(index);
					int[] a = args;
					ticks = 0;
					state = State.OPENING;
					server.execute(() -> {
						try {
							NetworkHandler.INSTANCE.openGui(server.getPlayerList().getPlayers().get(0), entry.guiId(), a[0], a[1], a[2]);
						}
						catch (Throwable t) {
							AncientWarfare.LOG.error("[showcase] openGui failed for {}", entry.name(), t);
							setupError = String.valueOf(t);
						}
					});
				}
			}
			case OPENING -> {
				Entry entry = entries.get(index);
				if (entry.screen().isInstance(mc.screen)) {
					if (entry.sub() != null && !subDone) {
						subDone = true;
						mc.setScreen(entry.sub().apply(mc.screen));
					}
					parkCursor(mc);
					ticks = 0;
					frames = 0;
					state = State.SHOWN;
				} else if (++ticks > OPEN_TIMEOUT || setupError != null) {
					fail("screen did not open within " + OPEN_TIMEOUT + " ticks (mc.screen = " + (mc.screen == null ? "null" : mc.screen.getClass().getSimpleName())
							+ (setupError == null ? "" : ", " + setupError) + ")");
					close(mc);
				}
			}
			case SHOWN -> {
				if (++ticks >= 10 && frames >= 3) {
					Entry entry = entries.get(index);
					File file = new File(outDir, "gui__" + entry.name() + ".png");
					try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
						image.writeToFile(file);
					}
					catch (IOException e) {
						fail("screenshot " + e);
					}
					AncientWarfare.LOG.info("[showcase] gui {} -> {}", entry.name(), mc.screen == null ? "null" : mc.screen.getClass().getSimpleName());
					close(mc);
				}
			}
			case CLOSING -> {
				if (++ticks >= 4) {
					if (mc.screen != null) {
						mc.setScreen(null);
					}
					state = State.NEXT;
				}
			}
		}
		return false;
	}

	/*
	 * Moves the cursor position the screens read to the top left corner, so no button or tooltip is in its hover state.
	 * The headless window gets no cursor events and glfwSetCursorPos has no effect on it, hence the (dev only) reflection.
	 */
	private static void parkCursor(Minecraft mc) {
		try {
			for (String name : new String[] {"xpos", "ypos"}) {
				java.lang.reflect.Field field = net.minecraft.client.MouseHandler.class.getDeclaredField(name);
				field.setAccessible(true);
				field.setDouble(mc.mouseHandler, 1.0);
			}
		}
		catch (ReflectiveOperationException | RuntimeException e) {
			if (!cursorWarned) {
				cursorWarned = true;
				AncientWarfare.LOG.warn("[showcase] could not park the cursor: {}", e.toString());
			}
		}
	}

	private static void close(Minecraft mc) {
		if (mc.player != null) {
			mc.player.closeContainer();
		}
		ticks = 0;
		state = State.CLOSING;
	}

	private static void fail(String what) {
		synchronized (failures) {
			failures.add("gui__" + entries.get(index).name() + ": " + what);
		}
	}

	/* ---------------------------------------- server side helpers ---------------------------------------- */

	private static void clearStage(ServerLevel level, ServerPlayer player) {
		for (Entity e : level.getEntities((Entity) null, new AABB(POS).inflate(12), e -> !(e instanceof Player))) {
			e.discard();
		}
		for (BlockPos pos : BlockPos.betweenClosed(POS.getX() - 2, POS.getY(), POS.getZ() - 2, POS.getX() + 2, POS.getY() + 2, POS.getZ() + 2)) {
			if (!level.getBlockState(pos).isAir()) {
				level.removeBlockEntity(pos);
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
			}
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
	}

	private static ResourceLocation rl(String path) {
		return ResourceLocation.fromNamespaceAndPath(AncientWarfare.MOD_ID, path);
	}

	private static int[] at(BlockPos pos) {
		return new int[] {pos.getX(), pos.getY(), pos.getZ()};
	}

	private static int[] place(ServerLevel level, ServerPlayer player, String path) {
		return place(level, player, BuiltInRegistries.BLOCK.get(rl(path)).defaultBlockState());
	}

	private static int[] place(ServerLevel level, ServerPlayer player, BlockState state) {
		if (state.isAir()) {
			throw new IllegalStateException("block not registered");
		}
		level.setBlock(POS, state, 3);
		BlockEntity te = level.getBlockEntity(POS);
		if (te instanceof TileOwned owned) {
			owned.setOwner(player);
		} else if (te instanceof TileWorksiteBase worksite) {
			worksite.setOwner(player);
		}
		if (te instanceof IBoundedSite site && !site.hasWorkBounds()) {
			site.setBounds(POS.offset(-2, 0, -5), POS.offset(2, 0, -1));
		}
		return at(POS);
	}

	private static ItemStack held(ServerPlayer player, String path) {
		ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(rl(path)));
		if (stack.isEmpty()) {
			throw new IllegalStateException("item not registered: " + path);
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return stack;
	}

	private static Entity spawn(ServerLevel level, ServerPlayer player, String path, @Nullable CompoundTag extra) {
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(rl(path)).orElseThrow(() -> new IllegalStateException("entity not registered: " + path));
		Entity entity = type.create(level);
		if (entity == null) {
			throw new IllegalStateException("EntityType.create returned null");
		}
		if (extra != null) {
			CompoundTag merged = entity.saveWithoutId(new CompoundTag());
			merged.merge(extra);
			entity.load(merged);
		}
		entity.moveTo(POS.getX() + 0.5, POS.getY(), POS.getZ() + 0.5, 0F, 0F);
		if (entity instanceof Mob mob) {
			mob.setNoAi(true);
			mob.setPersistenceRequired();
		}
		entity.setSilent(true);
		if (entity instanceof NpcBase npc && extra == null) {
			npc.setOwner(player);
		}
		level.addFreshEntity(entity);
		return entity;
	}

	private static int[] npc(ServerLevel level, ServerPlayer player, String path) {
		return new int[] {spawn(level, player, path, null).getId(), 0, 0};
	}

	private static CompoundTag faction() {
		CompoundTag nbt = new CompoundTag();
		nbt.putString("factionName", new TreeSet<>(FactionRegistry.getFactionNames()).first());
		return nbt;
	}

	/* an advanced spawner without spawn groups removes itself on its first update: give it the default settings of the item */
	private static int[] spawner(ServerLevel level, ServerPlayer player) {
		int[] at = place(level, player, "advanced_spawner");
		if (level.getBlockEntity(POS) instanceof net.shadowmage.ancientwarfare.structure.tile.TileAdvancedSpawner te) {
			SpawnerSettings settings = SpawnerSettings.getDefaultSettings();
			te.setSettings(settings);
		}
		return at;
	}

	private static void chest(ServerLevel level) {
		level.setBlock(POS, Blocks.CHEST.defaultBlockState(), 3);
	}

	/* ---------------------------------------- the screens ---------------------------------------- */

	private static void add(List<Entry> list, String name, int guiId, Class<?> screen, Setup setup) {
		list.add(new Entry(name, guiId, screen, setup, null));
	}

	private static void collect(List<Entry> list) {
		//core
		add(list, "core_crafting", NetworkHandler.GUI_CRAFTING, GuiEngineeringStation.class, (l, p) -> place(l, p, "engineering_station"));
		add(list, "core_research_station", NetworkHandler.GUI_RESEARCH_STATION, GuiResearchStation.class, (l, p) -> place(l, p, "research_station"));
		add(list, "core_backpack", NetworkHandler.GUI_BACKPACK, GuiBackpack.class, (l, p) -> {
			held(p, "backpack_0");
			return new int[3];
		});
		add(list, "core_research_book", NetworkHandler.GUI_RESEARCH_BOOK, GuiResearchBook.class, (l, p) -> {
			held(p, "research_book");
			return new int[3];
		});
		add(list, "core_manual", NetworkHandler.GUI_MANUAL, GuiManual.class, (l, p) -> {
			held(p, "manual");
			return new int[3];
		});
		add(list, "core_info_tool", NetworkHandler.GUI_INFO_TOOL, GuiInfoTool.class, (l, p) -> {
			held(p, "info_tool");
			return new int[3];
		});

		//npc
		add(list, "npc_inventory", NetworkHandler.GUI_NPC_INVENTORY, GuiNpcInventory.class, (l, p) -> npc(l, p, "aw_npc_worker"));
		add(list, "npc_inventory_combat", NetworkHandler.GUI_NPC_INVENTORY, GuiNpcInventory.class, (l, p) -> npc(l, p, "aw_npc_combat"));
		list.add(new Entry("npc_skin_selection", NetworkHandler.GUI_NPC_INVENTORY, GuiNpcInventory.class, (l, p) -> npc(l, p, "aw_npc_worker"),
				s -> new GuiSkinSelection((GuiNpcInventory) s, ((GuiNpcInventory) s).getContainer())));
		add(list, "npc_creative", NetworkHandler.GUI_NPC_CREATIVE, GuiNpcCreativeControls.class, (l, p) -> npc(l, p, "aw_npc_worker"));
		add(list, "npc_bard", NetworkHandler.GUI_NPC_BARD, GuiNpcBard.class, (l, p) -> npc(l, p, "aw_npc_bard"));
		add(list, "npc_player_owned_trade", NetworkHandler.GUI_NPC_PLAYER_OWNED_TRADE, GuiNpcPlayerOwnedTrade.class, (l, p) -> {
			Entity e = spawn(l, p, "aw_npc_trader", null);
			if (e instanceof NpcTrader trader) {
				ItemStack orders = new ItemStack(BuiltInRegistries.ITEM.get(rl("trade_order")));
				TradeOrder order = TradeOrder.getTradeOrder(orders);
				order.getTradeList().addNewTrade();
				order.getTradeList().get(0).setInputStack(0, new ItemStack(Items.EMERALD, 2));
				order.getTradeList().get(0).setOutputStack(0, new ItemStack(Items.BREAD, 6));
				order.write(orders);
				trader.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(BuiltInRegistries.ITEM.get(rl("backpack_0"))));
				trader.ordersStack = orders;
				trader.onOrdersInventoryChanged();
			}
			return new int[] {e.getId(), 0, 0};
		});
		add(list, "npc_faction_trade_view", NetworkHandler.GUI_NPC_FACTION_TRADE_VIEW, GuiNpcFactionTradeView.class, (l, p) -> factionTrader(l, p));
		add(list, "npc_faction_trade_setup", NetworkHandler.GUI_NPC_FACTION_TRADE_SETUP, GuiNpcFactionTradeSetup.class, (l, p) -> factionTrader(l, p));
		list.add(new Entry("npc_trade_template_save", NetworkHandler.GUI_NPC_FACTION_TRADE_SETUP, GuiNpcFactionTradeSetup.class, (l, p) -> factionTrader(l, p),
				s -> new GuiTradeTemplateSave((GuiNpcFactionTradeSetup) s)));
		add(list, "npc_faction_bard", NetworkHandler.GUI_NPC_FACTION_BARD, GuiNpcFactionBard.class, (l, p) -> new int[] {spawn(l, p, "faction.bard", faction()).getId(), 0, 0});
		add(list, "npc_work_order", NetworkHandler.GUI_NPC_WORK_ORDER, GuiWorkOrder.class, (l, p) -> {
			place(l, p, "crop_farm");
			ItemStack stack = held(p, "work_order");
			WorkOrder order = WorkOrder.getWorkOrder(stack);
			order.addWorkPosition(l, POS);
			order.write(stack);
			p.setItemInHand(InteractionHand.MAIN_HAND, stack);
			return new int[3];
		});
		add(list, "npc_upkeep_order", NetworkHandler.GUI_NPC_UPKEEP_ORDER, GuiUpkeepOrder.class, (l, p) -> {
			chest(l);
			ItemStack stack = held(p, "upkeep_order");
			UpkeepOrder order = UpkeepOrder.getUpkeepOrder(stack).orElseThrow();
			order.addUpkeepPosition(l, POS);
			order.write(stack);
			p.setItemInHand(InteractionHand.MAIN_HAND, stack);
			return new int[3];
		});
		add(list, "npc_combat_order", NetworkHandler.GUI_NPC_COMBAT_ORDER, GuiCombatOrder.class, (l, p) -> {
			ItemStack stack = held(p, "combat_order");
			CombatOrder order = CombatOrder.getCombatOrder(stack);
			order.addPatrolPoint(l, POS);
			order.addPatrolPoint(l, POS.offset(5, 0, 5));
			order.write(stack);
			p.setItemInHand(InteractionHand.MAIN_HAND, stack);
			return new int[3];
		});
		add(list, "npc_routing_order", NetworkHandler.GUI_NPC_ROUTING_ORDER, GuiRoutingOrder.class, (l, p) -> {
			chest(l);
			ItemStack stack = held(p, "routing_order");
			RoutingOrder order = RoutingOrder.getRoutingOrder(stack);
			order.addRoutePoint(Direction.UP, POS);
			order.addRoutePoint(Direction.NORTH, POS);
			order.write(stack);
			p.setItemInHand(InteractionHand.MAIN_HAND, stack);
			return new int[3];
		});
		add(list, "npc_trade_order", NetworkHandler.GUI_NPC_TRADE_ORDER, GuiTradeOrder.class, (l, p) -> {
			chest(l);
			ItemStack stack = held(p, "trade_order");
			TradeOrder order = TradeOrder.getTradeOrder(stack);
			order.getTradeList().addNewTrade();
			order.getTradeList().get(0).setInputStack(0, new ItemStack(Items.EMERALD, 2));
			order.getTradeList().get(0).setOutputStack(0, new ItemStack(Items.BREAD, 6));
			order.getRoute().addRoutePoint(POS);
			order.write(stack);
			p.setItemInHand(InteractionHand.MAIN_HAND, stack);
			return new int[3];
		});
		add(list, "npc_town_hall", NetworkHandler.GUI_NPC_TOWN_HALL, GuiTownHallInventory.class, (l, p) -> place(l, p, "town_hall"));
		list.add(new Entry("npc_town_hall_death_list", NetworkHandler.GUI_NPC_TOWN_HALL, GuiTownHallInventory.class, (l, p) -> place(l, p, "town_hall"),
				s -> new GuiTownHallDeathList((GuiTownHallInventory) s)));

		//automation
		add(list, "automation_quarry", NetworkHandler.GUI_WORKSITE_QUARRY, GuiWorksiteQuarry.class, (l, p) -> place(l, p, "quarry"));
		add(list, "automation_quarry_bounds", NetworkHandler.GUI_WORKSITE_QUARRY_BOUNDS, GuiWorksiteQuarryBounds.class, (l, p) -> place(l, p, "quarry"));
		add(list, "automation_tree_farm", NetworkHandler.GUI_WORKSITE_TREE_FARM, GuiWorksiteTreeFarm.class, (l, p) -> place(l, p, "tree_farm"));
		add(list, "automation_crop_farm", NetworkHandler.GUI_WORKSITE_CROP_FARM, GuiWorksiteCropFarm.class, (l, p) -> place(l, p, "crop_farm"));
		add(list, "automation_fruit_farm", NetworkHandler.GUI_WORKSITE_FRUIT_FARM, GuiWorksiteFruitFarm.class, (l, p) -> place(l, p, "fruit_farm"));
		add(list, "automation_animal_farm", NetworkHandler.GUI_WORKSITE_ANIMAL_FARM, GuiWorksiteAnimalFarm.class, (l, p) -> place(l, p, "animal_farm"));
		add(list, "automation_animal_control", NetworkHandler.GUI_WORKSITE_ANIMAL_CONTROL, GuiWorksiteAnimalControl.class, (l, p) -> place(l, p, "animal_farm"));
		add(list, "automation_fish_farm", NetworkHandler.GUI_WORKSITE_FISH_FARM, GuiWorksiteFishFarm.class, (l, p) -> place(l, p, "fish_farm"));
		add(list, "automation_fish_control", NetworkHandler.GUI_WORKSITE_FISH_CONTROL, GuiWorksiteFishControl.class, (l, p) -> place(l, p, "fish_farm"));
		add(list, "automation_inventory_side_adjust", NetworkHandler.GUI_WORKSITE_INVENTORY_SIDE_ADJUST, GuiWorksiteInventorySideSelection.class, (l, p) -> place(l, p, "crop_farm"));
		add(list, "automation_bounds_adjust", NetworkHandler.GUI_WORKSITE_BOUNDS, GuiWorksiteBoundsAdjust.class, (l, p) -> place(l, p, "crop_farm"));
		add(list, "automation_auto_crafting", NetworkHandler.GUI_WORKSITE_AUTO_CRAFT, GuiWorksiteAutoCrafting.class, (l, p) -> place(l, p, "auto_crafting"));
		add(list, "automation_warehouse_control", NetworkHandler.GUI_WAREHOUSE_CONTROL, GuiWarehouseControl.class, (l, p) -> place(l, p, "warehouse_control"));
		add(list, "automation_warehouse_storage", NetworkHandler.GUI_WAREHOUSE_STORAGE, GuiWarehouseStorage.class, (l, p) -> place(l, p, "warehouse_storage"));
		add(list, "automation_warehouse_interface", NetworkHandler.GUI_WAREHOUSE_OUTPUT, GuiWarehouseInterface.class, (l, p) -> place(l, p, "warehouse_interface"));
		add(list, "automation_warehouse_crafting", NetworkHandler.GUI_WAREHOUSE_CRAFTING, GuiWarehouseCraftingStation.class, (l, p) -> place(l, p, "warehouse_crafting"));
		add(list, "automation_warehouse_stock_viewer", NetworkHandler.GUI_WAREHOUSE_STOCK, GuiWarehouseStockViewer.class, (l, p) -> place(l, p, "warehouse_stock_viewer"));
		add(list, "automation_warehouse_stock_linker", NetworkHandler.GUI_WAREHOUSE_STOCK_LINKER, GuiWarehouseStockLinker.class, (l, p) -> place(l, p, "warehouse_stock_linker"));
		add(list, "automation_mailbox", NetworkHandler.GUI_MAILBOX_INVENTORY, GuiMailboxInventory.class, (l, p) -> place(l, p, "mailbox"));
		list.add(new Entry("automation_mailbox_name_select", NetworkHandler.GUI_MAILBOX_INVENTORY, GuiMailboxInventory.class, (l, p) -> place(l, p, "mailbox"),
				s -> {
					((GuiMailboxInventory) s).getContainer().removeSlots();
					return new GuiMailboxNameSelect((GuiMailboxInventory) s, true);
				}));
		list.add(new Entry("automation_mailbox_side_setup", NetworkHandler.GUI_MAILBOX_INVENTORY, GuiMailboxInventory.class, (l, p) -> place(l, p, "mailbox"),
				s -> {
					((GuiMailboxInventory) s).getContainer().removeSlots();
					return new GuiMailboxInventorySideSetup((GuiMailboxInventory) s);
				}));
		add(list, "automation_stirling_generator", NetworkHandler.GUI_STIRLING_GENERATOR, GuiStirlingGenerator.class, (l, p) -> place(l, p, "stirling_generator"));
		add(list, "automation_chunk_loader_deluxe", NetworkHandler.GUI_CHUNK_LOADER_DELUXE, GuiChunkLoaderDeluxe.class, (l, p) -> place(l, p, "chunk_loader_deluxe"));

		//structure
		Setup scannerItem = (l, p) -> {
			held(p, "structure_scanner");
			return new int[3];
		};
		add(list, "structure_scanner", NetworkHandler.GUI_SCANNER, GuiStructureScanner.class, scannerItem);
		add(list, "structure_scanner_block", NetworkHandler.GUI_SCANNER, GuiStructureScanner.class, (l, p) -> place(l, p, "structure_scanner_block"));
		list.add(new Entry("structure_scanner_validation", NetworkHandler.GUI_SCANNER, GuiStructureScanner.class, scannerItem, s -> new GuiStructureValidationSettings((GuiStructureScanner) s)));
		list.add(new Entry("structure_scanner_biomes", NetworkHandler.GUI_SCANNER, GuiStructureScanner.class, scannerItem, s -> new GuiStructureBiomeSelection((GuiStructureScanner) s)));
		list.add(new Entry("structure_scanner_dimensions", NetworkHandler.GUI_SCANNER, GuiStructureScanner.class, scannerItem, s -> new GuiDimensionSelection((GuiStructureScanner) s)));
		list.add(new Entry("structure_scanner_mods", NetworkHandler.GUI_SCANNER, GuiStructureScanner.class, scannerItem, s -> new GuiModSelection((GuiStructureScanner) s)));
		list.add(new Entry("structure_scanner_incorrect_name", NetworkHandler.GUI_SCANNER, GuiStructureScanner.class, scannerItem, s -> new GuiStructureIncorrectName((GuiStructureScanner) s)));
		add(list, "structure_builder_selection", NetworkHandler.GUI_BUILDER, GuiStructureSelection.class, (l, p) -> {
			held(p, "structure_builder");
			return new int[3];
		});
		add(list, "structure_drafting_station", NetworkHandler.GUI_DRAFTING_STATION, GuiDraftingStation.class, (l, p) -> place(l, p, "drafting_station"));
		list.add(new Entry("structure_drafting_station_selection", NetworkHandler.GUI_DRAFTING_STATION, GuiDraftingStation.class, (l, p) -> place(l, p, "drafting_station"),
				s -> new GuiStructureSelectionDraftingStation((GuiDraftingStation) s)));
		add(list, "structure_spawner_advanced_block", NetworkHandler.GUI_SPAWNER_ADVANCED_BLOCK, GuiSpawnerAdvanced.class, (l, p) -> spawner(l, p));
		add(list, "structure_spawner_advanced_item", NetworkHandler.GUI_SPAWNER_ADVANCED, GuiSpawnerAdvanced.class, (l, p) -> {
			ItemStack stack = held(p, "advanced_spawner");
			CompoundTag settings = new CompoundTag();
			SpawnerSettings.getDefaultSettings().writeToNBT(settings);
			net.shadowmage.ancientwarfare.core.util.ItemTools.setTagInfo(stack, "spawnerSettings", settings);
			return new int[3];
		});
		add(list, "structure_spawner_inventory", NetworkHandler.GUI_SPAWNER_ADVANCED_BLOCK_INVENTORY, GuiSpawnerAdvancedInventory.class, (l, p) -> spawner(l, p));
		list.add(new Entry("structure_spawner_add_entity", NetworkHandler.GUI_SPAWNER_ADVANCED_BLOCK, GuiSpawnerAdvanced.class, (l, p) -> spawner(l, p),
				s -> {
					SpawnerSettings.EntitySpawnGroup group = SpawnerSettings.getDefaultSettings().getSpawnGroups().get(0);
					return new GuiSpawnerAdvancedAddEntity((GuiSpawnerAdvanced) s, group, group.getEntitiesToSpawn().get(0));
				}));
		list.add(new Entry("structure_spawner_entity_selection", NetworkHandler.GUI_SPAWNER_ADVANCED_BLOCK, GuiSpawnerAdvanced.class, (l, p) -> spawner(l, p),
				s -> new GuiSpawnerAdvancedEntitySelection((GuiSpawnerAdvanced) s, SpawnerSettings.getDefaultSettings().getSpawnGroups().get(0).getEntitiesToSpawn().get(0))));
		Setup gate = (l, p) -> {
			CompoundTag nbt = new CompoundTag();
			nbt.putLong("pos1", POS.offset(-1, 0, 0).asLong());
			nbt.putLong("pos2", POS.offset(1, 2, 0).asLong());
			nbt.putInt("type", 0);
			nbt.putFloat("edgeMax", 3);
			nbt.putInt("health", 100);
			nbt.putByte("orient", (byte) net.minecraft.core.Direction.SOUTH.ordinal());
			return new int[] {spawn(l, p, "aw_gate", nbt).getId(), 0, 0};
		};
		add(list, "structure_gate_control", NetworkHandler.GUI_GATE_CONTROL, GuiGateControl.class, gate);
		add(list, "structure_gate_control_creative", NetworkHandler.GUI_GATE_CONTROL_CREATIVE, GuiGateControlCreative.class, gate);
		add(list, "structure_sound_block", NetworkHandler.GUI_SOUND_BLOCK, GuiSoundBlock.class, (l, p) -> place(l, p, "sound_block"));
		list.add(new Entry("structure_sound_select", NetworkHandler.GUI_SOUND_BLOCK, GuiSoundBlock.class, (l, p) -> place(l, p, "sound_block"),
				s -> {
					((GuiSoundBlock) s).getContainer().data.addNewEntry();
					return new GuiSoundSelect((GuiSoundBlock) s, ((GuiSoundBlock) s).getContainer().data.get(((GuiSoundBlock) s).getContainer().data.size() - 1));
				}));
		add(list, "structure_loot_chest_placer", NetworkHandler.GUI_LOOT_CHEST_PLACER, GuiLootChestPlacer.class, (l, p) -> {
			held(p, "loot_chest_placer");
			return new int[3];
		});
		add(list, "structure_loot_basket", NetworkHandler.GUI_LOOT_BASKET, GuiLootBasket.class, (l, p) -> place(l, p, "loot_basket"));
		add(list, "structure_stake", NetworkHandler.GUI_STAKE, GuiStake.class, (l, p) -> place(l, p, BuiltInRegistries.BLOCK.get(rl("stake")).defaultBlockState()
				.setValue(net.shadowmage.ancientwarfare.structure.util.StructureProperties.TOP_BOTTOM_PART, net.shadowmage.ancientwarfare.structure.util.TopBottomPart.BOTTOM)));
		add(list, "structure_statue", NetworkHandler.GUI_STATUE, GuiStatue.class, (l, p) -> place(l, p, "statue"));
	}

	private static int[] factionTrader(ServerLevel level, ServerPlayer player) {
		Entity e = spawn(level, player, "faction.trader", faction());
		if (e instanceof NpcFactionTrader trader) {
			trader.getTradeList().addNewTrade();
			FactionTrade trade = trader.getTradeList().get(0);
			trade.setInputStack(0, new ItemStack(Items.GOLD_INGOT, 3));
			trade.setOutputStack(0, new ItemStack(Items.IRON_SWORD));
			trade.setMaxAvailable(5);
		}
		return new int[] {e.getId(), 0, 0};
	}
}
