package net.shadowmage.ancientwarfare.structure;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.shadowmage.ancientwarfare.core.input.InputHandler;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.NetworkHandlerClient;
import net.shadowmage.ancientwarfare.structure.gui.GuiDraftingStation;
import net.shadowmage.ancientwarfare.structure.gui.GuiGateControl;
import net.shadowmage.ancientwarfare.structure.gui.GuiGateControlCreative;
import net.shadowmage.ancientwarfare.structure.gui.GuiLootBasket;
import net.shadowmage.ancientwarfare.structure.gui.GuiLootChestPlacer;
import net.shadowmage.ancientwarfare.structure.gui.GuiSoundBlock;
import net.shadowmage.ancientwarfare.structure.gui.GuiSpawnerAdvanced;
import net.shadowmage.ancientwarfare.structure.gui.GuiSpawnerAdvancedInventory;
import net.shadowmage.ancientwarfare.structure.gui.GuiStake;
import net.shadowmage.ancientwarfare.structure.gui.GuiStatue;
import net.shadowmage.ancientwarfare.structure.gui.GuiStructureScanner;
import net.shadowmage.ancientwarfare.structure.gui.GuiStructureSelection;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.block.BlockBrazierEmber;
import net.shadowmage.ancientwarfare.structure.block.BlockFirePit;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.structure.init.AWStructureEntities;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockGravestone;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockTotemPart;
import net.shadowmage.ancientwarfare.structure.model.StructureModelLayers;
import net.shadowmage.ancientwarfare.structure.tile.TileColored;
import net.shadowmage.ancientwarfare.structure.tile.TileSoundBlock;
import net.shadowmage.ancientwarfare.structure.util.StructureClientHooks;
import net.shadowmage.ancientwarfare.structure.util.WoodVariantHelper;

import java.util.Optional;
import java.util.function.ToIntFunction;

/*
 * Client-only module entry point (replaces the 1.12 ClientProxyStructure). Called from AncientWarfareClient,
 * which only loads on the physical client. Never reference this class from common code.
 *
 * - the 1.12 custom mesh definitions (item model picked from the stack NBT: wood variant, lit, coin stack size,
 *   gravestone / totem variant) -> item property "ancientwarfare:variant" + overrides in the item models
 *   (tools/structure_gen_resources.py writes the models in the same order as the functions below)
 * - AWStructureBlockColors / AWStructureItemColors -> RegisterColorHandlersEvent
 * - StructureClientHooks: the client half of the 1.12 proxy (key names, client player, particles)
 * - renderers (structure/render/**): block entity renderers, RenderLevelStageEvent overlays (item bounding boxes +
 *   template preview, block highlight, structure entry boxes), the item renderer (StructureItemRenderer), the sound
 *   block disguise model, sun / coloured flame particles, the sound block client sounds
 * - GUIs: structure/gui/**, registered in registerGuis()
 */
public final class AncientWarfareStructureClient {
	private AncientWarfareStructureClient() {}

	private static final ResourceLocation VARIANT = ResourceLocation.fromNamespaceAndPath(AncientWarfareStructure.MOD_ID, "variant");

	public static void init(IEventBus modBus, ModContainer container) {
		StructureClientHooks.set(new ClientHooks());
		registerGuis();
		modBus.addListener(AncientWarfareStructureClient::clientSetup);
		modBus.addListener(AncientWarfareStructureClient::registerRenderers);
		modBus.addListener(AncientWarfareStructureClient::registerLayerDefinitions);
		modBus.addListener(AncientWarfareStructureClient::registerBlockColors);
		modBus.addListener(AncientWarfareStructureClient::registerItemColors);
		modBus.addListener(AncientWarfareStructureClient::modifyBakingResult);
		modBus.addListener(AncientWarfareStructureClient::registerClientExtensions);
		//1.12 ClientProxyStructure.preInit: MinecraftForge.EVENT_BUS.register(new ...Renderer())
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(new net.shadowmage.ancientwarfare.structure.render.StructureBoundingBoxRenderer()::handleRenderLastEvent);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(new net.shadowmage.ancientwarfare.structure.render.BlockHighlightRenderer()::handleRenderLastEvent);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(new net.shadowmage.ancientwarfare.structure.render.StructureEntryBBRenderer()::handleRenderLastEvent);
	}

	/* 1.12 BlockSoundBlock.registerClient: the baked sound block model is the disguise bakery (SoundBlockRenderer) */
	private static void modifyBakingResult(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult event) {
		ResourceLocation soundBlock = ResourceLocation.fromNamespaceAndPath(AncientWarfareStructure.MOD_ID, "sound_block");
		event.getModels().replaceAll((location, model) -> location.id().equals(soundBlock) && !"inventory".equals(location.getVariant())
				? new net.shadowmage.ancientwarfare.structure.render.SoundBlockRenderer(model) : model);
	}

	/* 1.12 ModelRegistryHelper.registerItemRenderer(item, new ...Renderer()) */
	private static void registerClientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
		event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
			@Override
			public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return net.shadowmage.ancientwarfare.structure.render.StructureItemRenderer.INSTANCE;
			}
		}, AWStructureBlocks.WOODEN_COFFIN_ITEM.get(), AWStructureBlocks.STONE_COFFIN_ITEM.get(), AWStructureBlocks.PROTECTION_FLAG_ITEM.get(),
				AWStructureBlocks.DECORATIVE_FLAG_ITEM.get(), AWStructureBlocks.ADVANCED_LOOT_CHEST_ITEM.get());
	}

	/*
	 * Dev showcase: one picture per gate type, named like the 1.12 pictures (entity__aw_gate@single_wood ...): a 3 wide,
	 * 3 high gate along the x axis centred on the stage (x -1..1, y 100..102, z 0), as the 1.12 harness built with
	 * Gate.constructGate. The gate places its proxy blocks itself; the proxy flagged "render" draws it.
	 */
	private static void registerShowcaseVariants() {
		ResourceLocation gateId = ResourceLocation.fromNamespaceAndPath(AncientWarfareStructure.MOD_ID, "aw_gate");
		Object[][] gates = {{0, "vertical_wooden", 3f}, {1, "vertical_iron", 3f}, {4, "single_wood", 3f}, {5, "single_iron", 3f}, {8, "double_wood", 1.5f},
				{9, "double_iron", 1.5f}, {12, "drawbridge", 90f}};
		for (Object[] gate : gates) {
			CompoundTag nbt = new CompoundTag();
			nbt.putLong("pos1", new BlockPos(-1, 100, 0).asLong());
			nbt.putLong("pos2", new BlockPos(1, 102, 0).asLong());
			nbt.putInt("type", (Integer) gate[0]);
			nbt.putFloat("edge", 0);
			nbt.putFloat("edgeMax", (Float) gate[2]);
			nbt.putInt("health", net.shadowmage.ancientwarfare.structure.gates.types.Gate.getGateByID((Integer) gate[0]).getMaxHealth());
			nbt.putByte("status", (byte) 0);
			nbt.putByte("orient", (byte) net.minecraft.core.Direction.SOUTH.ordinal());
			net.shadowmage.ancientwarfare.core.util.ShowcaseHooks.registerEntityVariant(gateId, (String) gate[1], nbt);
		}
	}

	/*
	 * 1.12 NetworkHandler.registerGui calls of ClientProxyStructure.preInit and the registerClient() methods of the
	 * items / blocks.
	 */
	private static void registerGuis() {
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_GATE_CONTROL, GuiGateControl::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_GATE_CONTROL_CREATIVE, GuiGateControlCreative::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_BUILDER, GuiStructureSelection::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_TOWN_BUILDER, net.shadowmage.ancientwarfare.structure.gui.GuiTownSelection::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_SCANNER, GuiStructureScanner::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_LOOT_CHEST_PLACER, GuiLootChestPlacer::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_LOOT_BASKET, GuiLootBasket::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_DRAFTING_STATION, GuiDraftingStation::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_SOUND_BLOCK, GuiSoundBlock::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_SPAWNER_ADVANCED, GuiSpawnerAdvanced::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_SPAWNER_ADVANCED_BLOCK, GuiSpawnerAdvanced::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_SPAWNER_ADVANCED_INVENTORY, GuiSpawnerAdvancedInventory::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_SPAWNER_ADVANCED_BLOCK_INVENTORY, GuiSpawnerAdvancedInventory::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_STATUE, GuiStatue::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_STAKE, GuiStake::new);
	}

	private static void variant(Item item, ToIntFunction<ItemStack> index) {
		ItemProperties.register(item, VARIANT, (stack, level, entity, seed) -> index.applyAsInt(stack));
	}

	private static void clientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			if (net.shadowmage.ancientwarfare.core.util.ShowcaseHooks.isEnabled()) {
				registerShowcaseVariants();
			}
			ToIntFunction<ItemStack> wood = stack -> WoodVariantHelper.getVariant(stack).ordinal();
			variant(AWStructureBlocks.STOOL_ITEM.get(), wood);
			variant(AWStructureBlocks.TABLE_ITEM.get(), wood);
			variant(AWStructureBlocks.CHAIR_ITEM.get(), wood);
			variant(AWStructureBlocks.BENCH_ITEM.get(), wood);
			variant(AWStructureBlocks.FIRE_PIT_ITEM.get(), stack -> {
				CompoundTag tag = ItemTools.getTag(stack);
				return BlockFirePit.Variant.byValue(tag.getString(BlockFirePit.VARIANT_TAG)).ordinal() * 2 + (tag.getBoolean(BlockFirePit.LIT_TAG) ? 1 : 0);
			});
			ToIntFunction<ItemStack> lit = stack -> ItemTools.getTag(stack).getBoolean(BlockBrazierEmber.LIT_TAG) ? 1 : 0;
			variant(AWStructureBlocks.BRAZIER_EMBER_ITEM.get(), lit);
			variant(AWStructureBlocks.BRAZIER_FLAME_ITEM.get(), lit);
			ToIntFunction<ItemStack> coins = stack -> Math.max(0, Math.min(8, ItemTools.getTag(stack).getInt("size") / 8));
			variant(AWStructureBlocks.COIN_STACK_COPPER_ITEM.get(), coins);
			variant(AWStructureBlocks.COIN_STACK_SILVER_ITEM.get(), coins);
			variant(AWStructureBlocks.COIN_STACK_GOLD_ITEM.get(), coins);
			variant(AWStructureBlocks.COIN_STACK_ANCIENT_ITEM.get(), coins);
			variant(AWStructureBlocks.GRAVESTONE_ITEM.get(), stack -> ItemTools.hasTag(stack) ? Math.max(1, Math.min(8, ItemBlockGravestone.getVariant(stack))) : 0);
			variant(AWStructureBlocks.TOTEM_PART_ITEM.get(), stack -> ItemBlockTotemPart.getVariant(stack).getId());
		});
	}

	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(AWStructureBlocks.ADVANCED_LOOT_CHEST_TILE.get(), net.shadowmage.ancientwarfare.structure.render.RenderAdvancedLootChest::new);
		event.registerBlockEntityRenderer(AWStructureBlocks.LOOT_BASKET_TILE.get(), net.shadowmage.ancientwarfare.structure.render.RenderAdvancedLootChest::new);
		event.registerBlockEntityRenderer(AWStructureBlocks.GRAVESTONE_TILE.get(), net.shadowmage.ancientwarfare.structure.render.RenderLootInfo::new);
		event.registerBlockEntityRenderer(AWStructureBlocks.URN_TILE.get(), net.shadowmage.ancientwarfare.structure.render.RenderLootInfo::new);
		event.registerBlockEntityRenderer(AWStructureBlocks.PROTECTION_FLAG_TILE.get(), net.shadowmage.ancientwarfare.structure.render.ProtectionFlagRenderer::new);
		event.registerBlockEntityRenderer(AWStructureBlocks.DECORATIVE_FLAG_TILE.get(), net.shadowmage.ancientwarfare.structure.render.ProtectionFlagRenderer::new);
		event.registerBlockEntityRenderer(AWStructureBlocks.STATUE_TILE.get(), net.shadowmage.ancientwarfare.structure.render.statue.StatueRenderer::new);
		event.registerBlockEntityRenderer(AWStructureBlocks.STAKE_TILE.get(), net.shadowmage.ancientwarfare.structure.render.StakeRenderer::new);
		event.registerBlockEntityRenderer(AWStructureBlocks.ADVANCED_SPAWNER_TILE.get(), net.shadowmage.ancientwarfare.structure.render.RenderAdvancedSpawner::new);
		event.registerBlockEntityRenderer(AWStructureBlocks.GATE_PROXY_TILE.get(), net.shadowmage.ancientwarfare.structure.render.GateProxyRenderer::new);
		event.registerBlockEntityRenderer(AWStructureBlocks.STRUCTURE_BUILDER_TICKED_TILE.get(), net.shadowmage.ancientwarfare.structure.render.RenderStructureBuilder::new);
		event.registerBlockEntityRenderer(AWStructureBlocks.STRUCTURE_SCANNER_BLOCK_TILE.get(), net.shadowmage.ancientwarfare.structure.render.StructureScannerRenderer::new);
		event.registerBlockEntityRenderer(AWStructureBlocks.WOODEN_COFFIN_TILE.get(), net.shadowmage.ancientwarfare.structure.render.WoodenCoffinRenderer::new);
		event.registerBlockEntityRenderer(AWStructureBlocks.STONE_COFFIN_TILE.get(), net.shadowmage.ancientwarfare.structure.render.StoneCoffinRenderer::new);
		event.registerEntityRenderer(AWStructureEntities.GATE.get(), net.shadowmage.ancientwarfare.structure.render.RenderGateInvisible::new);
		event.registerEntityRenderer(AWStructureEntities.SEAT.get(), NoopRenderer::new);
	}

	private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		StructureModelLayers.register(event);
	}

	/* 1.12 AWStructureBlockColors.init */
	private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
		event.register((state, world, pos, tintIndex) -> WorldTools.getTile(world, pos, TileColored.class).map(TileColored::getColor).orElse(-1),
				AWStructureBlocks.ALTAR_CANDLE.get(), AWStructureBlocks.ALTAR_LONG_CLOTH.get(), AWStructureBlocks.ALTAR_SHORT_CLOTH.get());
		event.register((state, world, pos, tintIndex) -> {
			BlockState disguiseState = WorldTools.getTile(world, pos, TileSoundBlock.class).filter(t -> t.getDisguiseState() != null)
					.map(TileSoundBlock::getDisguiseState).orElse(Blocks.JUKEBOX.defaultBlockState());
			return event.getBlockColors().getColor(disguiseState, world, pos, 0);
		}, AWStructureBlocks.SOUND_BLOCK.get());
	}

	/* 1.12 AWStructureItemColors.init (1.21 item colours are ARGB) */
	private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register((stack, tintIndex) -> 0xFF000000 | AWStructureBlocks.ALTAR_CANDLE_ITEM.get().getColor(stack),
				AWStructureBlocks.ALTAR_CANDLE_ITEM.get(), AWStructureBlocks.ALTAR_LONG_CLOTH_ITEM.get(), AWStructureBlocks.ALTAR_SHORT_CLOTH_ITEM.get());
	}

	private static final class ClientHooks extends StructureClientHooks {
		@Override
		public void addSunParticle(Level world, double x, double y, double z) {
			if (world instanceof ClientLevel clientWorld) {
				Minecraft.getInstance().particleEngine.add(new net.shadowmage.ancientwarfare.structure.render.ParticleSun(clientWorld, x, y, z));
			}
		}

		@Override
		public void addColoredFlameParticle(Level world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int color) {
			//1.12 ParticleColoredFlame: a ParticleFlame whose colour (white) is multiplied by the given rgb
			Particle flame = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.FLAME, x, y, z, xSpeed, ySpeed, zSpeed);
			if (flame != null) {
				flame.setColor((float) (color >> 16 & 255) / 255.0F, (float) (color >> 8 & 255) / 255.0F, (float) (color & 255) / 255.0F);
			}
		}

		@Override
		public void setBlockHighlightInfo(net.shadowmage.ancientwarfare.structure.util.BlockHighlightInfo info) {
			net.shadowmage.ancientwarfare.structure.render.BlockHighlightRenderer.setBlockHighlightInfo(info);
		}

		/* --- 1.12 ClientProxyStructure --- */
		private final Map<BlockPos, SimpleSoundInstance> currentSounds = new HashMap<>();

		@Override
		public void clearTemplatePreviewCache() {
			net.shadowmage.ancientwarfare.structure.render.PreviewRenderer.clearCache();
		}

		@Override
		public void resetSoundAt(BlockPos pos) {
			currentSounds.remove(pos);
		}

		@Override
		public void setSoundAt(BlockPos pos, SoundEvent soundEvent, float volume) {
			currentSounds.put(pos, getPositionedSoundRecord(soundEvent, (float) pos.getX(), (float) pos.getY(), (float) pos.getZ(), volume));
		}

		private SimpleSoundInstance getPositionedSoundRecord(SoundEvent soundEvent, float x, float y, float z, float volume) {
			return new SimpleSoundInstance(soundEvent.getLocation(), SoundSource.RECORDS, volume, 1.0F, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.LINEAR, x, y, z, false);
		}

		@Override
		public void stopSoundAt(BlockPos pos) {
			if (currentSounds.containsKey(pos)) {
				Minecraft.getInstance().getSoundManager().stop(currentSounds.get(pos));
			}
		}

		@Override
		public boolean hasSoundAt(BlockPos pos) {
			return currentSounds.containsKey(pos);
		}

		@Override
		public boolean isSoundPlayingAt(BlockPos pos) {
			if (!hasSoundAt(pos)) {
				return false;
			}
			SoundInstance positionedsoundrecord = currentSounds.get(pos);
			return Minecraft.getInstance().getSoundManager().isActive(positionedsoundrecord);
		}

		@Override
		public void playSoundAt(BlockPos pos) {
			if (hasSoundAt(pos)) {
				Minecraft.getInstance().getSoundManager().play(currentSounds.get(pos));
			}
		}

		@Override
		public String getKeyName(int altItemUseKey) {
			switch (altItemUseKey) {
				case 1:
					return InputHandler.ALT_ITEM_USE_1.getTranslatedKeyMessage().getString();
				case 2:
					return InputHandler.ALT_ITEM_USE_2.getTranslatedKeyMessage().getString();
				case 3:
					return InputHandler.ALT_ITEM_USE_3.getTranslatedKeyMessage().getString();
				case 4:
					return InputHandler.ALT_ITEM_USE_4.getTranslatedKeyMessage().getString();
				default:
					return InputHandler.ALT_ITEM_USE_5.getTranslatedKeyMessage().getString();
			}
		}

		@Override
		public double getClientPlayerDistanceTo(BlockPos pos) {
			Player player = Minecraft.getInstance().player;
			return player == null ? 0 : Math.sqrt(player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
		}

		@Override
		public Optional<Player> getPlayer() {
			return Optional.ofNullable(Minecraft.getInstance().player);
		}
	}
}
