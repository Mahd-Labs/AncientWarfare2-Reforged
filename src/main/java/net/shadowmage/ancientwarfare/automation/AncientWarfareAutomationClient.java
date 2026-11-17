package net.shadowmage.ancientwarfare.automation;

import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.NetworkHandlerClient;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteInventorySideSelection;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteAnimalControl;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteFishControl;
import net.shadowmage.ancientwarfare.automation.gui.GuiWarehouseControl;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteQuarry;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteQuarryBounds;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteTreeFarm;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteCropFarm;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteFruitFarm;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteAnimalFarm;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteFishFarm;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteBoundsAdjust;
import net.shadowmage.ancientwarfare.automation.gui.GuiWarehouseInterface;
import net.shadowmage.ancientwarfare.automation.gui.GuiChunkLoaderDeluxe;
import net.shadowmage.ancientwarfare.automation.gui.GuiMailboxInventory;
import net.shadowmage.ancientwarfare.automation.gui.GuiWarehouseStockLinker;
import net.shadowmage.ancientwarfare.automation.gui.GuiWorksiteAutoCrafting;
import net.shadowmage.ancientwarfare.automation.gui.GuiStirlingGenerator;
import net.shadowmage.ancientwarfare.automation.gui.GuiWarehouseStorage;
import net.shadowmage.ancientwarfare.automation.gui.GuiWarehouseStockViewer;
import net.shadowmage.ancientwarfare.automation.gui.GuiWarehouseCraftingStation;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.registries.DeferredItem;
import net.shadowmage.ancientwarfare.automation.block.BlockWaterwheelGenerator;
import net.shadowmage.ancientwarfare.automation.block.TorqueTier;
import net.shadowmage.ancientwarfare.automation.config.AWAutomationStatics;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;
import net.shadowmage.ancientwarfare.automation.render.FlywheelControllerAnimationRenderer;
import net.shadowmage.ancientwarfare.automation.render.FlywheelControllerRenderer;
import net.shadowmage.ancientwarfare.automation.render.FlywheelStorageAnimationRenderer;
import net.shadowmage.ancientwarfare.automation.render.FlywheelStorageRenderer;
import net.shadowmage.ancientwarfare.automation.render.HandCrankedGeneratorRenderer;
import net.shadowmage.ancientwarfare.automation.render.StirlingGeneratorRenderer;
import net.shadowmage.ancientwarfare.automation.render.TorqueAnimationRenderer;
import net.shadowmage.ancientwarfare.automation.render.TorqueDistributorRenderer;
import net.shadowmage.ancientwarfare.automation.render.TorqueJunctionRenderer;
import net.shadowmage.ancientwarfare.automation.render.TorqueShaftAnimationRenderer;
import net.shadowmage.ancientwarfare.automation.render.TorqueShaftRenderer;
import net.shadowmage.ancientwarfare.automation.render.TorqueTransportAnimationRenderer;
import net.shadowmage.ancientwarfare.automation.render.WarehouseStockLinkerRenderer;
import net.shadowmage.ancientwarfare.automation.render.WarehouseStockViewerRenderer;
import net.shadowmage.ancientwarfare.automation.render.WaterwheelGeneratorRenderer;
import net.shadowmage.ancientwarfare.automation.render.WindmillBladeAnimationRenderer;
import net.shadowmage.ancientwarfare.automation.render.WindmillBladeRenderer;
import net.shadowmage.ancientwarfare.automation.render.WindmillGeneratorRenderer;
import net.shadowmage.ancientwarfare.automation.render.WorksiteRenderer;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueBase;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileWaterwheelGenerator;
import net.shadowmage.ancientwarfare.core.input.InputHandler;
import net.shadowmage.ancientwarfare.core.render.BakeryModel;
import net.shadowmage.ancientwarfare.core.render.BaseBakery;
import net.shadowmage.ancientwarfare.core.render.BlockStateKeyGenerator;
import net.shadowmage.ancientwarfare.core.render.property.CoreProperties;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/*
 * Client-only module entry point (replaces the 1.12 ClientProxyAutomation and AutomationInputHandler). Called from
 * AncientWarfareClient, which only loads on the physical client. Never reference this class from common code.
 *
 * - block entity renderers: 1.12 ClientRegistry.bindTileEntitySpecialRenderer per tile class -> per block entity type
 * - TextureStitchEvent.Pre registerSprite -> TextureAtlasStitchedEvent (textures/model/** is part of the block atlas)
 * - the bakeries the 1.12 blocks registered in registerClient (CCBakeryModel + key generator) -> BakeryModel put into
 *   the model registry for every state of the block and for its item(s)
 * - GUI screens (automation.gui) are registered at the top of init
 */
public final class AncientWarfareAutomationClient {
	private AncientWarfareAutomationClient() {}

	private static final String KEYBIND_CATEGORY = "keybind.category.awAutomation";
	private static final String KEY_TOGGLE_WORKBOUNDS_RENDER = "keybind.awAutomation.toggle_workbounds_render";
	public static final KeyMapping TOGGLE_WORKBOUNDS_RENDER = new KeyMapping(KEY_TOGGLE_WORKBOUNDS_RENDER, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, KEYBIND_CATEGORY);

	private static final Function<Object, String> FLOAT_FORMAT = o -> String.format("%.6f", o);

	public static void init(IEventBus modBus, ModContainer container) {
		//1.12 ClientProxyAutomation and the block classes' @SideOnly registerClient registerGui calls
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WORKSITE_INVENTORY_SIDE_ADJUST, GuiWorksiteInventorySideSelection::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WORKSITE_ANIMAL_CONTROL, GuiWorksiteAnimalControl::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WORKSITE_FISH_CONTROL, GuiWorksiteFishControl::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WAREHOUSE_CONTROL, GuiWarehouseControl::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WORKSITE_QUARRY, GuiWorksiteQuarry::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WORKSITE_QUARRY_BOUNDS, GuiWorksiteQuarryBounds::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WORKSITE_TREE_FARM, GuiWorksiteTreeFarm::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WORKSITE_CROP_FARM, GuiWorksiteCropFarm::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WORKSITE_FRUIT_FARM, GuiWorksiteFruitFarm::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WORKSITE_ANIMAL_FARM, GuiWorksiteAnimalFarm::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WORKSITE_FISH_FARM, GuiWorksiteFishFarm::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WORKSITE_BOUNDS, GuiWorksiteBoundsAdjust::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WAREHOUSE_OUTPUT, GuiWarehouseInterface::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_CHUNK_LOADER_DELUXE, GuiChunkLoaderDeluxe::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_MAILBOX_INVENTORY, GuiMailboxInventory::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WAREHOUSE_STOCK_LINKER, GuiWarehouseStockLinker::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WORKSITE_AUTO_CRAFT, GuiWorksiteAutoCrafting::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_STIRLING_GENERATOR, GuiStirlingGenerator::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WAREHOUSE_STORAGE, GuiWarehouseStorage::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WAREHOUSE_STOCK, GuiWarehouseStockViewer::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_WAREHOUSE_CRAFTING, GuiWarehouseCraftingStation::new);

		modBus.addListener(AncientWarfareAutomationClient::initKeyBindings);
		modBus.addListener(AncientWarfareAutomationClient::registerRenderers);
		modBus.addListener(AncientWarfareAutomationClient::onTextureStitched);
		modBus.addListener(AncientWarfareAutomationClient::onModifyBakingResult);
	}

	private static void initKeyBindings(RegisterKeyMappingsEvent event) {
		event.register(TOGGLE_WORKBOUNDS_RENDER);
		InputHandler.registerCallBack(TOGGLE_WORKBOUNDS_RENDER, AncientWarfareAutomationClient::toggleWorkboundsConfigValue);
	}

	private static void toggleWorkboundsConfigValue() {
		AWAutomationStatics.setRenderWorkBounds(!AWAutomationStatics.renderWorkBounds);
	}

	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(AWAutomationBlocks.QUARRY_TILE.get(), ctx -> new WorksiteRenderer<>());
		event.registerBlockEntityRenderer(AWAutomationBlocks.TREE_FARM_TILE.get(), ctx -> new WorksiteRenderer<>());
		event.registerBlockEntityRenderer(AWAutomationBlocks.CROP_FARM_TILE.get(), ctx -> new WorksiteRenderer<>());
		event.registerBlockEntityRenderer(AWAutomationBlocks.FRUIT_FARM_TILE.get(), ctx -> new WorksiteRenderer<>());
		event.registerBlockEntityRenderer(AWAutomationBlocks.ANIMAL_FARM_TILE.get(), ctx -> new WorksiteRenderer<>());
		event.registerBlockEntityRenderer(AWAutomationBlocks.FISH_FARM_TILE.get(), ctx -> new WorksiteRenderer<>());
		event.registerBlockEntityRenderer(AWAutomationBlocks.WAREHOUSE_CONTROL_TILE.get(), ctx -> new WorksiteRenderer<>());
		event.registerBlockEntityRenderer(AWAutomationBlocks.WAREHOUSE_STOCK_VIEWER_TILE.get(), ctx -> new WarehouseStockViewerRenderer());
		event.registerBlockEntityRenderer(AWAutomationBlocks.WAREHOUSE_STOCK_LINKER_TILE.get(), ctx -> new WarehouseStockLinkerRenderer());

		//********************************************CONDUIT / TRANSPORT RENDERS***************************************************************//
		event.registerBlockEntityRenderer(AWAutomationBlocks.TORQUE_JUNCTION_LIGHT_TILE.get(), ctx -> new TorqueTransportAnimationRenderer(TorqueJunctionRenderer.INSTANCE));
		event.registerBlockEntityRenderer(AWAutomationBlocks.TORQUE_JUNCTION_MEDIUM_TILE.get(), ctx -> new TorqueTransportAnimationRenderer(TorqueJunctionRenderer.INSTANCE));
		event.registerBlockEntityRenderer(AWAutomationBlocks.TORQUE_JUNCTION_HEAVY_TILE.get(), ctx -> new TorqueTransportAnimationRenderer(TorqueJunctionRenderer.INSTANCE));
		event.registerBlockEntityRenderer(AWAutomationBlocks.TORQUE_DISTRIBUTOR_LIGHT_TILE.get(), ctx -> new TorqueTransportAnimationRenderer(TorqueDistributorRenderer.INSTANCE));
		event.registerBlockEntityRenderer(AWAutomationBlocks.TORQUE_DISTRIBUTOR_MEDIUM_TILE.get(), ctx -> new TorqueTransportAnimationRenderer(TorqueDistributorRenderer.INSTANCE));
		event.registerBlockEntityRenderer(AWAutomationBlocks.TORQUE_DISTRIBUTOR_HEAVY_TILE.get(), ctx -> new TorqueTransportAnimationRenderer(TorqueDistributorRenderer.INSTANCE));
		event.registerBlockEntityRenderer(AWAutomationBlocks.TORQUE_SHAFT_LIGHT_TILE.get(), ctx -> new TorqueShaftAnimationRenderer());
		event.registerBlockEntityRenderer(AWAutomationBlocks.TORQUE_SHAFT_MEDIUM_TILE.get(), ctx -> new TorqueShaftAnimationRenderer());
		event.registerBlockEntityRenderer(AWAutomationBlocks.TORQUE_SHAFT_HEAVY_TILE.get(), ctx -> new TorqueShaftAnimationRenderer());

		//********************************************STORAGE RENDERS***************************************************************//
		event.registerBlockEntityRenderer(AWAutomationBlocks.FLYWHEEL_LIGHT_TILE.get(), ctx -> new FlywheelControllerAnimationRenderer());
		event.registerBlockEntityRenderer(AWAutomationBlocks.FLYWHEEL_MEDIUM_TILE.get(), ctx -> new FlywheelControllerAnimationRenderer());
		event.registerBlockEntityRenderer(AWAutomationBlocks.FLYWHEEL_LARGE_TILE.get(), ctx -> new FlywheelControllerAnimationRenderer());
		event.registerBlockEntityRenderer(AWAutomationBlocks.FLYWHEEL_STORAGE_TILE.get(), ctx -> new FlywheelStorageAnimationRenderer());

		//********************************************GENERATOR RENDERS***************************************************************//
		event.registerBlockEntityRenderer(AWAutomationBlocks.STIRLING_GENERATOR_TILE.get(), ctx -> new TorqueAnimationRenderer<>(StirlingGeneratorRenderer.INSTANCE));
		event.registerBlockEntityRenderer(AWAutomationBlocks.WATERWHEEL_GENERATOR_TILE.get(), ctx -> new TorqueAnimationRenderer<TileWaterwheelGenerator>(WaterwheelGeneratorRenderer.INSTANCE) {
			@Override
			protected IExtendedBlockState updateAdditionalProperties(IExtendedBlockState state, TileTorqueBase te) {
				if (te instanceof TileWaterwheelGenerator) {
					return state.withProperty(BlockWaterwheelGenerator.VALID_SETUP, ((TileWaterwheelGenerator) te).validSetup);
				}
				return state;
			}
		});
		event.registerBlockEntityRenderer(AWAutomationBlocks.HAND_CRANKED_GENERATOR_TILE.get(), ctx -> new TorqueAnimationRenderer<>(HandCrankedGeneratorRenderer.INSTANCE));
		event.registerBlockEntityRenderer(AWAutomationBlocks.WINDMILL_GENERATOR_TILE.get(), ctx -> new TorqueAnimationRenderer<>(WindmillGeneratorRenderer.INSTANCE));
		event.registerBlockEntityRenderer(AWAutomationBlocks.WINDMILL_BLADE_TILE.get(), ctx -> new WindmillBladeAnimationRenderer());
	}

	private static TextureAtlasSprite sprite(TextureAtlas atlas, String name) {
		return atlas.getSprite(ResourceLocation.fromNamespaceAndPath(AncientWarfareAutomation.MOD_ID, "model/automation/" + name));
	}

	/*
	 * 1.12 onPreTextureStitch
	 */
	private static void onTextureStitched(TextureAtlasStitchedEvent evt) {
		TextureAtlas atlas = evt.getAtlas();
		if (!atlas.location().equals(InventoryMenu.BLOCK_ATLAS)) {
			return;
		}
		StirlingGeneratorRenderer.INSTANCE.setSprite(sprite(atlas, "stirling_generator"));
		HandCrankedGeneratorRenderer.INSTANCE.setSprite(sprite(atlas, "hand_cranked_generator"));
		WaterwheelGeneratorRenderer.INSTANCE.setSprite(sprite(atlas, "waterwheel_generator"));
		WindmillGeneratorRenderer.INSTANCE.setSprite(sprite(atlas, "windmill_generator"));
		WindmillBladeRenderer.INSTANCE.setSprite(sprite(atlas, "windmill_blade"));
		WindmillBladeRenderer.INSTANCE.setCubeSprite(sprite(atlas, "windmill_blade_cube"));
		for (TorqueTier tier : TorqueTier.values()) {
			String name = tier.getSerializedName();
			TorqueShaftRenderer.INSTANCE.setSprite(tier, sprite(atlas, "torque_shaft_" + name));
			TorqueJunctionRenderer.INSTANCE.setSprite(tier, sprite(atlas, "torque_junction_" + name));
			TorqueDistributorRenderer.INSTANCE.setSprite(tier, sprite(atlas, "torque_distributor_" + name));
			FlywheelControllerRenderer.INSTANCE.setSprite(tier, sprite(atlas, "flywheel_controller_" + name));
			FlywheelStorageRenderer.INSTANCE.setSprite(false, tier, sprite(atlas, "flywheel_small_" + name));
			FlywheelStorageRenderer.INSTANCE.setSprite(true, tier, sprite(atlas, "flywheel_large_" + name));
		}
	}

	private static BlockStateKeyGenerator.Builder torqueKey() {
		return new BlockStateKeyGenerator.Builder().addKeyProperties(CoreProperties.UNLISTED_FACING, AutomationProperties.DYNAMIC);
	}

	/*
	 * 1.12 registerClient of the torque blocks: ModelBakery.registerBlockKeyGenerator + ModelRegistryHelper.register
	 */
	@SuppressWarnings("deprecation")
	private static void onModifyBakingResult(ModelEvent.ModifyBakingResult evt) {
		BlockStateKeyGenerator plain = torqueKey().addKeyProperties(FLOAT_FORMAT, AutomationProperties.ROTATIONS).build();

		register(evt, AWAutomationBlocks.TORQUE_SHAFT.get(), AWAutomationBlocks.TORQUE_SHAFT_ITEMS, TorqueShaftRenderer.INSTANCE,
				new BlockStateKeyGenerator.Builder().addKeyProperties(AutomationProperties.TIER).addKeyProperties(CoreProperties.UNLISTED_FACING, AutomationProperties.DYNAMIC, AutomationProperties.HAS_PREVIOUS, AutomationProperties.HAS_NEXT)
						.addKeyProperties(FLOAT_FORMAT, AutomationProperties.INPUT_ROTATION).addKeyProperties(FLOAT_FORMAT, AutomationProperties.ROTATIONS).build(),
				() -> TorqueShaftRenderer.INSTANCE.getSprite(TorqueTier.LIGHT));
		register(evt, AWAutomationBlocks.TORQUE_JUNCTION.get(), AWAutomationBlocks.TORQUE_JUNCTION_ITEMS, TorqueJunctionRenderer.INSTANCE,
				new BlockStateKeyGenerator.Builder().addKeyProperties(AutomationProperties.TIER).addKeyProperties(CoreProperties.UNLISTED_FACING, AutomationProperties.DYNAMIC).addKeyProperties(AutomationProperties.CONNECTIONS)
						.addKeyProperties(FLOAT_FORMAT, AutomationProperties.ROTATIONS).build(),
				() -> TorqueJunctionRenderer.INSTANCE.getSprite(TorqueTier.LIGHT));
		register(evt, AWAutomationBlocks.TORQUE_DISTRIBUTOR.get(), AWAutomationBlocks.TORQUE_DISTRIBUTOR_ITEMS, TorqueDistributorRenderer.INSTANCE,
				new BlockStateKeyGenerator.Builder().addKeyProperties(AutomationProperties.TIER).addKeyProperties(CoreProperties.UNLISTED_FACING, AutomationProperties.DYNAMIC).addKeyProperties(AutomationProperties.CONNECTIONS)
						.addKeyProperties(FLOAT_FORMAT, AutomationProperties.ROTATIONS).build(),
				() -> TorqueDistributorRenderer.INSTANCE.getSprite(TorqueTier.LIGHT));
		register(evt, AWAutomationBlocks.FLYWHEEL_CONTROLLER.get(), AWAutomationBlocks.FLYWHEEL_CONTROLLER_ITEMS, FlywheelControllerRenderer.INSTANCE,
				new BlockStateKeyGenerator.Builder().addKeyProperties(AutomationProperties.TIER)
						.addKeyProperties(CoreProperties.UNLISTED_FACING, AutomationProperties.DYNAMIC, AutomationProperties.FLYWHEEL_ROTATION, AutomationProperties.USE_INPUT)
						.addKeyProperties(FLOAT_FORMAT, AutomationProperties.ROTATIONS).addKeyProperties(FLOAT_FORMAT, AutomationProperties.INPUT_ROTATION).build(),
				() -> FlywheelControllerRenderer.INSTANCE.getSprite(TorqueTier.LIGHT));
		register(evt, AWAutomationBlocks.FLYWHEEL_STORAGE.get(), AWAutomationBlocks.FLYWHEEL_STORAGE_ITEMS, FlywheelStorageRenderer.INSTANCE,
				new BlockStateKeyGenerator.Builder().addKeyProperties(AutomationProperties.TIER)
						.addKeyProperties(AutomationProperties.DYNAMIC, AutomationProperties.IS_CONTROL, AutomationProperties.WIDTH, AutomationProperties.HEIGHT)
						.addKeyProperties(FLOAT_FORMAT, AutomationProperties.ROTATION).build(),
				() -> FlywheelStorageRenderer.INSTANCE.getSprite(TorqueTier.LIGHT));
		ItemBlockRenderTypes.setRenderLayer(AWAutomationBlocks.FLYWHEEL_STORAGE.get(), RenderType.translucent());

		register(evt, AWAutomationBlocks.STIRLING_GENERATOR.get(), List.of(AWAutomationBlocks.STIRLING_GENERATOR_ITEM), StirlingGeneratorRenderer.INSTANCE, plain, () -> StirlingGeneratorRenderer.INSTANCE.sprite);
		register(evt, AWAutomationBlocks.HAND_CRANKED_GENERATOR.get(), List.of(AWAutomationBlocks.HAND_CRANKED_GENERATOR_ITEM), HandCrankedGeneratorRenderer.INSTANCE, plain, () -> HandCrankedGeneratorRenderer.INSTANCE.sprite);
		register(evt, AWAutomationBlocks.WINDMILL_GENERATOR.get(), List.of(AWAutomationBlocks.WINDMILL_GENERATOR_ITEM), WindmillGeneratorRenderer.INSTANCE, plain, () -> WindmillGeneratorRenderer.INSTANCE.sprite);
		register(evt, AWAutomationBlocks.WATERWHEEL_GENERATOR.get(), List.of(AWAutomationBlocks.WATERWHEEL_GENERATOR_ITEM), WaterwheelGeneratorRenderer.INSTANCE,
				new BlockStateKeyGenerator.Builder().addKeyProperties(BlockWaterwheelGenerator.VALID_SETUP).addKeyProperties(CoreProperties.UNLISTED_FACING, AutomationProperties.DYNAMIC)
						.addKeyProperties(FLOAT_FORMAT, AutomationProperties.ROTATIONS).build(),
				() -> WaterwheelGeneratorRenderer.INSTANCE.sprite);
		register(evt, AWAutomationBlocks.WINDMILL_BLADE.get(), List.of(AWAutomationBlocks.WINDMILL_BLADE_ITEM), WindmillBladeRenderer.INSTANCE,
				new BlockStateKeyGenerator.Builder()
						.addKeyProperties(AutomationProperties.FORMED, AutomationProperties.IS_CONTROL, AutomationProperties.HEIGHT, CoreProperties.UNLISTED_HORIZONTAL_FACING, AutomationProperties.DYNAMIC)
						.addKeyProperties(FLOAT_FORMAT, AutomationProperties.ROTATION).build(),
				() -> WindmillBladeRenderer.INSTANCE.cubeSprite);
	}

	private static void register(ModelEvent.ModifyBakingResult evt, Block block, List<DeferredItem<BlockItem>> items, BaseBakery bakery, BlockStateKeyGenerator keyGenerator, Supplier<TextureAtlasSprite> particle) {
		BakeryModel model = new BakeryModel(bakery, keyGenerator, particle);
		for (BlockState state : block.getStateDefinition().getPossibleStates()) {
			evt.getModels().put(BlockModelShaper.stateToModelLocation(state), model);
		}
		for (DeferredItem<BlockItem> item : items) {
			evt.getModels().put(ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item.get())), model);
		}
	}
}
