package net.shadowmage.ancientwarfare.structure.init;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;
import net.shadowmage.ancientwarfare.npc.item.ItemCoin;
import net.shadowmage.ancientwarfare.structure.AWStructureTab;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.block.BlockAdvancedLootChest;
import net.shadowmage.ancientwarfare.structure.block.BlockAdvancedSpawner;
import net.shadowmage.ancientwarfare.structure.block.BlockBench;
import net.shadowmage.ancientwarfare.structure.block.BlockBrazierEmber;
import net.shadowmage.ancientwarfare.structure.block.BlockBrazierFlame;
import net.shadowmage.ancientwarfare.structure.block.BlockChair;
import net.shadowmage.ancientwarfare.structure.block.BlockCoinStack;
import net.shadowmage.ancientwarfare.structure.block.BlockDecorativeFlag;
import net.shadowmage.ancientwarfare.structure.block.BlockDraftingStation;
import net.shadowmage.ancientwarfare.structure.block.BlockFirePit;
import net.shadowmage.ancientwarfare.structure.block.BlockGateProxy;
import net.shadowmage.ancientwarfare.structure.block.BlockGibbet;
import net.shadowmage.ancientwarfare.structure.block.BlockGoldenIdol;
import net.shadowmage.ancientwarfare.structure.block.BlockGoldenThrone;
import net.shadowmage.ancientwarfare.structure.block.BlockGravestone;
import net.shadowmage.ancientwarfare.structure.block.BlockIronCage;
import net.shadowmage.ancientwarfare.structure.block.BlockLootBasket;
import net.shadowmage.ancientwarfare.structure.block.BlockProtectionFlag;
import net.shadowmage.ancientwarfare.structure.block.BlockScissorSeat;
import net.shadowmage.ancientwarfare.structure.block.BlockSoundBlock;
import net.shadowmage.ancientwarfare.structure.block.BlockStake;
import net.shadowmage.ancientwarfare.structure.block.BlockStatue;
import net.shadowmage.ancientwarfare.structure.block.BlockStoneCoffin;
import net.shadowmage.ancientwarfare.structure.block.BlockStool;
import net.shadowmage.ancientwarfare.structure.block.BlockStretchingRack;
import net.shadowmage.ancientwarfare.structure.block.BlockStructureBuilder;
import net.shadowmage.ancientwarfare.structure.block.BlockStructureScanner;
import net.shadowmage.ancientwarfare.structure.block.BlockTable;
import net.shadowmage.ancientwarfare.structure.block.BlockTotemCube;
import net.shadowmage.ancientwarfare.structure.block.BlockTotemPart;
import net.shadowmage.ancientwarfare.structure.block.BlockTribalChair;
import net.shadowmage.ancientwarfare.structure.block.BlockTribalStool;
import net.shadowmage.ancientwarfare.structure.block.BlockUrn;
import net.shadowmage.ancientwarfare.structure.block.BlockWoodenCoffin;
import net.shadowmage.ancientwarfare.structure.block.BlockWoodenPost;
import net.shadowmage.ancientwarfare.structure.block.BlockWoodenThrone;
import net.shadowmage.ancientwarfare.structure.block.altar.BlockAltarCandle;
import net.shadowmage.ancientwarfare.structure.block.altar.BlockAltarLectern;
import net.shadowmage.ancientwarfare.structure.block.altar.BlockAltarLongCloth;
import net.shadowmage.ancientwarfare.structure.block.altar.BlockAltarShortCloth;
import net.shadowmage.ancientwarfare.structure.block.altar.BlockAltarSun;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockAdvancedSpawner;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockBrazierEmber;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockBrazierFlame;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockChair;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockColored;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockFirePit;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockFlag;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockGravestone;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockPosts;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockStoneCoffin;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockStretchingRack;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockStructureBuilder;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockTotemPart;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockWoodenCoffin;
import net.shadowmage.ancientwarfare.structure.item.ItemLootChestPlacer;
import net.shadowmage.ancientwarfare.structure.item.ItemLootChestPlacer.LootContainerInfo;
import net.shadowmage.ancientwarfare.structure.item.ItemMultiBlock;
import net.shadowmage.ancientwarfare.structure.item.WoodItemBlock;
import net.shadowmage.ancientwarfare.structure.tile.TEGateProxy;
import net.shadowmage.ancientwarfare.structure.tile.TileAdvancedLootChest;
import net.shadowmage.ancientwarfare.structure.tile.TileAdvancedSpawner;
import net.shadowmage.ancientwarfare.structure.tile.TileAltarCandle;
import net.shadowmage.ancientwarfare.structure.tile.TileChair;
import net.shadowmage.ancientwarfare.structure.tile.TileColored;
import net.shadowmage.ancientwarfare.structure.tile.TileDecorativeFlag;
import net.shadowmage.ancientwarfare.structure.tile.TileDraftingStation;
import net.shadowmage.ancientwarfare.structure.tile.TileGravestone;
import net.shadowmage.ancientwarfare.structure.tile.TileLootBasket;
import net.shadowmage.ancientwarfare.structure.tile.TileProtectionFlag;
import net.shadowmage.ancientwarfare.structure.tile.TileSoundBlock;
import net.shadowmage.ancientwarfare.structure.tile.TileStake;
import net.shadowmage.ancientwarfare.structure.tile.TileStatue;
import net.shadowmage.ancientwarfare.structure.tile.TileStoneCoffin;
import net.shadowmage.ancientwarfare.structure.tile.TileStructureBuilder;
import net.shadowmage.ancientwarfare.structure.tile.TileStructureScanner;
import net.shadowmage.ancientwarfare.structure.tile.TileTotemPart;
import net.shadowmage.ancientwarfare.structure.tile.TileUrn;
import net.shadowmage.ancientwarfare.structure.tile.TileWoodenCoffin;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/*
 * Port notes: @ObjectHolder fields -> DeferredBlock holders (call .get() at use sites). Item blocks and block entity
 * types are registered here as well (1.12 registerItemBlocks / registerTile); block entity type ids are the 1.12 tile
 * ids. No item was flattened: the 1.12 structure blocks kept their variants in the stack NBT (wood variant, coffin
 * variant, colour, lit, ...) and the port keeps them in the stack's custom data; block meta became block state
 * properties.
 */
@SuppressWarnings("WeakerAccess")
public class AWStructureBlocks {
	private AWStructureBlocks() {}

	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AncientWarfareStructure.MOD_ID);
	private static final DeferredRegister.Items ITEM_BLOCKS = DeferredRegister.createItems(AncientWarfareStructure.MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AncientWarfareStructure.MOD_ID);

	/* blocks, 1.12 registration order */
	public static final DeferredBlock<BlockAdvancedSpawner> ADVANCED_SPAWNER = BLOCKS.register("advanced_spawner", BlockAdvancedSpawner::new);
	public static final DeferredBlock<BlockGateProxy> GATE_PROXY = BLOCKS.register("gate_proxy", BlockGateProxy::new);
	public static final DeferredBlock<BlockDraftingStation> DRAFTING_STATION = BLOCKS.register("drafting_station", BlockDraftingStation::new);
	public static final DeferredBlock<BlockStructureBuilder> STRUCTURE_BUILDER_TICKED = BLOCKS.register("structure_builder_ticked", BlockStructureBuilder::new);
	public static final DeferredBlock<BlockSoundBlock> SOUND_BLOCK = BLOCKS.register("sound_block", BlockSoundBlock::new);
	public static final DeferredBlock<BlockStructureScanner> STRUCTURE_SCANNER_BLOCK = BLOCKS.register("structure_scanner_block", BlockStructureScanner::new);
	public static final DeferredBlock<BlockAdvancedLootChest> ADVANCED_LOOT_CHEST = BLOCKS.register("advanced_loot_chest", BlockAdvancedLootChest::new);
	public static final DeferredBlock<BlockTotemPart> TOTEM_PART = BLOCKS.register("totem_part", BlockTotemPart::new);
	public static final DeferredBlock<BlockFirePit> FIRE_PIT = BLOCKS.register("fire_pit", BlockFirePit::new);
	public static final DeferredBlock<BlockBrazierFlame> BRAZIER_FLAME = BLOCKS.register("brazier_flame", BlockBrazierFlame::new);
	public static final DeferredBlock<BlockBrazierEmber> BRAZIER_EMBER = BLOCKS.register("brazier_ember", BlockBrazierEmber::new);
	public static final DeferredBlock<BlockAltarShortCloth> ALTAR_SHORT_CLOTH = BLOCKS.register("altar_short_cloth", BlockAltarShortCloth::new);
	public static final DeferredBlock<BlockAltarLongCloth> ALTAR_LONG_CLOTH = BLOCKS.register("altar_long_cloth", BlockAltarLongCloth::new);
	public static final DeferredBlock<BlockAltarCandle> ALTAR_CANDLE = BLOCKS.register("altar_candle", BlockAltarCandle::new);
	public static final DeferredBlock<BlockAltarLectern> ALTAR_LECTERN = BLOCKS.register("altar_lectern", BlockAltarLectern::new);
	public static final DeferredBlock<BlockAltarSun> ALTAR_SUN = BLOCKS.register("altar_sun", BlockAltarSun::new);
	public static final DeferredBlock<BlockProtectionFlag> PROTECTION_FLAG = BLOCKS.register("protection_flag", BlockProtectionFlag::new);
	public static final DeferredBlock<BlockDecorativeFlag> DECORATIVE_FLAG = BLOCKS.register("decorative_flag", BlockDecorativeFlag::new);
	public static final DeferredBlock<BlockGoldenIdol> GOLDEN_IDOL = BLOCKS.register("golden_idol", BlockGoldenIdol::new);
	public static final DeferredBlock<BlockTotemCube> GOBLIN_TOTEM_1 = BLOCKS.register("goblin_totem_1", () -> new BlockTotemCube("goblin_totem_1"));
	public static final DeferredBlock<BlockTotemCube> GOBLIN_TOTEM_2 = BLOCKS.register("goblin_totem_2", () -> new BlockTotemCube("goblin_totem_2"));
	public static final DeferredBlock<BlockTotemCube> GOBLIN_TOTEM_2_LIT = BLOCKS.register("goblin_totem_2_lit", () -> new BlockTotemCube("goblin_totem_2_lit", true));
	public static final DeferredBlock<BlockTotemCube> ORC_TOTEM_1 = BLOCKS.register("orc_totem_1", () -> new BlockTotemCube("orc_totem_1"));
	public static final DeferredBlock<BlockTotemCube> ORC_TOTEM_2 = BLOCKS.register("orc_totem_2", () -> new BlockTotemCube("orc_totem_2"));
	public static final DeferredBlock<BlockTotemCube> ORC_TOTEM_2_LIT = BLOCKS.register("orc_totem_2_lit", () -> new BlockTotemCube("orc_totem_2_lit", true));
	public static final DeferredBlock<BlockLootBasket> LOOT_BASKET = BLOCKS.register("loot_basket", BlockLootBasket::new);
	public static final DeferredBlock<BlockWoodenCoffin> WOODEN_COFFIN = BLOCKS.register("wooden_coffin", BlockWoodenCoffin::new);
	public static final DeferredBlock<BlockStoneCoffin> STONE_COFFIN = BLOCKS.register("stone_coffin", BlockStoneCoffin::new);
	public static final DeferredBlock<BlockStool> STOOL = BLOCKS.register("stool", BlockStool::new);
	public static final DeferredBlock<BlockUrn> URN = BLOCKS.register("urn", BlockUrn::new);
	public static final DeferredBlock<BlockTable> TABLE = BLOCKS.register("table", BlockTable::new);
	public static final DeferredBlock<BlockChair> CHAIR = BLOCKS.register("chair", BlockChair::new);
	public static final DeferredBlock<BlockTribalStool> TRIBAL_STOOL = BLOCKS.register("tribal_stool", BlockTribalStool::new);
	public static final DeferredBlock<BlockWoodenThrone> WOODEN_THRONE = BLOCKS.register("wooden_throne", BlockWoodenThrone::new);
	public static final DeferredBlock<BlockGoldenThrone> GOLDEN_THRONE = BLOCKS.register("golden_throne", BlockGoldenThrone::new);
	public static final DeferredBlock<BlockWoodenPost> WOODEN_POST = BLOCKS.register("wooden_post", BlockWoodenPost::new);
	public static final DeferredBlock<BlockIronCage> IRON_CAGE = BLOCKS.register("iron_cage", BlockIronCage::new);
	public static final DeferredBlock<BlockGibbet> GIBBET = BLOCKS.register("gibbet", BlockGibbet::new);
	public static final DeferredBlock<BlockStretchingRack> STRETCHING_RACK = BLOCKS.register("stretching_rack", BlockStretchingRack::new);
	public static final DeferredBlock<BlockStake> STAKE = BLOCKS.register("stake", BlockStake::new);
	public static final DeferredBlock<BlockBench> BENCH = BLOCKS.register("bench", BlockBench::new);
	public static final DeferredBlock<BlockTribalChair> TRIBAL_CHAIR = BLOCKS.register("tribal_chair", BlockTribalChair::new);
	public static final DeferredBlock<BlockScissorSeat> SCISSOR_SEAT = BLOCKS.register("scissor_seat", BlockScissorSeat::new);
	public static final DeferredBlock<BlockStatue> STATUE = BLOCKS.register("statue", BlockStatue::new);
	public static final DeferredBlock<BlockCoinStack> COIN_STACK_COPPER = BLOCKS.register("coin_stack_copper", () -> new BlockCoinStack("coin_stack_copper", ItemCoin.CoinMetal.COPPER));
	public static final DeferredBlock<BlockCoinStack> COIN_STACK_SILVER = BLOCKS.register("coin_stack_silver", () -> new BlockCoinStack("coin_stack_silver", ItemCoin.CoinMetal.SILVER));
	public static final DeferredBlock<BlockCoinStack> COIN_STACK_GOLD = BLOCKS.register("coin_stack_gold", () -> new BlockCoinStack("coin_stack_gold", ItemCoin.CoinMetal.GOLD));
	public static final DeferredBlock<BlockCoinStack> COIN_STACK_ANCIENT = BLOCKS.register("coin_stack_ancient", () -> new BlockCoinStack("coin_stack_ancient", ItemCoin.CoinMetal.ANCIENT));
	public static final DeferredBlock<BlockGravestone> GRAVESTONE = BLOCKS.register("gravestone", BlockGravestone::new);

	/* item blocks, 1.12 registration order */
	public static final DeferredItem<ItemBlockAdvancedSpawner> ADVANCED_SPAWNER_ITEM = ITEM_BLOCKS.register("advanced_spawner", () -> new ItemBlockAdvancedSpawner(ADVANCED_SPAWNER.get()));
	public static final DeferredItem<BlockItem> GATE_PROXY_ITEM = ITEM_BLOCKS.register("gate_proxy", () -> new ItemBlockBase(GATE_PROXY.get()));
	public static final DeferredItem<BlockItem> DRAFTING_STATION_ITEM = ITEM_BLOCKS.register("drafting_station", () -> new ItemBlockBase(DRAFTING_STATION.get()));
	public static final DeferredItem<ItemBlockStructureBuilder> STRUCTURE_BUILDER_TICKED_ITEM = ITEM_BLOCKS.register("structure_builder_ticked", () -> new ItemBlockStructureBuilder(STRUCTURE_BUILDER_TICKED.get()));
	public static final DeferredItem<BlockItem> SOUND_BLOCK_ITEM = ITEM_BLOCKS.register("sound_block", () -> new ItemBlockBase(SOUND_BLOCK.get()));
	public static final DeferredItem<BlockItem> STRUCTURE_SCANNER_BLOCK_ITEM = ITEM_BLOCKS.register("structure_scanner_block", () -> new ItemBlockBase(STRUCTURE_SCANNER_BLOCK.get()));
	public static final DeferredItem<BlockItem> ADVANCED_LOOT_CHEST_ITEM = ITEM_BLOCKS.register("advanced_loot_chest", () -> new ItemBlockBase(ADVANCED_LOOT_CHEST.get()) {
		@Override
		public String getDescriptionId() {
			return "tile.advanced_loot_chest.name";
		}
	});
	public static final DeferredItem<ItemBlockFirePit> FIRE_PIT_ITEM = ITEM_BLOCKS.register("fire_pit", () -> new ItemBlockFirePit(FIRE_PIT.get()));
	public static final DeferredItem<ItemBlockTotemPart> TOTEM_PART_ITEM = ITEM_BLOCKS.register("totem_part", () -> new ItemBlockTotemPart(TOTEM_PART.get()));
	public static final DeferredItem<ItemBlockBrazierEmber> BRAZIER_EMBER_ITEM = ITEM_BLOCKS.register("brazier_ember", () -> new ItemBlockBrazierEmber(BRAZIER_EMBER.get()));
	public static final DeferredItem<ItemBlockBrazierFlame> BRAZIER_FLAME_ITEM = ITEM_BLOCKS.register("brazier_flame", () -> new ItemBlockBrazierFlame(BRAZIER_FLAME.get()));
	public static final DeferredItem<ItemBlockColored> ALTAR_SHORT_CLOTH_ITEM = ITEM_BLOCKS.register("altar_short_cloth", () -> new ItemBlockColored(ALTAR_SHORT_CLOTH.get()));
	public static final DeferredItem<ItemBlockColored> ALTAR_LONG_CLOTH_ITEM = ITEM_BLOCKS.register("altar_long_cloth", () -> new ItemBlockColored(ALTAR_LONG_CLOTH.get()));
	public static final DeferredItem<ItemBlockColored> ALTAR_CANDLE_ITEM = ITEM_BLOCKS.register("altar_candle", () -> new ItemBlockColored(ALTAR_CANDLE.get()));
	public static final DeferredItem<BlockItem> ALTAR_LECTERN_ITEM = ITEM_BLOCKS.register("altar_lectern", () -> new ItemBlockBase(ALTAR_LECTERN.get()));
	public static final DeferredItem<BlockItem> ALTAR_SUN_ITEM = ITEM_BLOCKS.register("altar_sun", () -> new ItemBlockBase(ALTAR_SUN.get()));
	public static final DeferredItem<ItemBlockFlag> PROTECTION_FLAG_ITEM = ITEM_BLOCKS.register("protection_flag", () -> new ItemBlockFlag(PROTECTION_FLAG.get()));
	public static final DeferredItem<ItemBlockFlag> DECORATIVE_FLAG_ITEM = ITEM_BLOCKS.register("decorative_flag", () -> new ItemBlockFlag(DECORATIVE_FLAG.get()));
	public static final DeferredItem<BlockItem> GOLDEN_IDOL_ITEM = ITEM_BLOCKS.register("golden_idol", () -> new ItemBlockBase(GOLDEN_IDOL.get()));
	public static final DeferredItem<BlockItem> ORC_TOTEM_1_ITEM = ITEM_BLOCKS.register("orc_totem_1", () -> new ItemBlockBase(ORC_TOTEM_1.get()));
	public static final DeferredItem<BlockItem> ORC_TOTEM_2_ITEM = ITEM_BLOCKS.register("orc_totem_2", () -> new ItemBlockBase(ORC_TOTEM_2.get()));
	public static final DeferredItem<BlockItem> ORC_TOTEM_2_LIT_ITEM = ITEM_BLOCKS.register("orc_totem_2_lit", () -> new ItemBlockBase(ORC_TOTEM_2_LIT.get()));
	public static final DeferredItem<BlockItem> GOBLIN_TOTEM_1_ITEM = ITEM_BLOCKS.register("goblin_totem_1", () -> new ItemBlockBase(GOBLIN_TOTEM_1.get()));
	public static final DeferredItem<BlockItem> GOBLIN_TOTEM_2_ITEM = ITEM_BLOCKS.register("goblin_totem_2", () -> new ItemBlockBase(GOBLIN_TOTEM_2.get()));
	public static final DeferredItem<BlockItem> GOBLIN_TOTEM_2_LIT_ITEM = ITEM_BLOCKS.register("goblin_totem_2_lit", () -> new ItemBlockBase(GOBLIN_TOTEM_2_LIT.get()));
	public static final DeferredItem<BlockItem> LOOT_BASKET_ITEM = ITEM_BLOCKS.register("loot_basket", () -> new ItemBlockBase(LOOT_BASKET.get()));
	public static final DeferredItem<ItemBlockWoodenCoffin> WOODEN_COFFIN_ITEM = ITEM_BLOCKS.register("wooden_coffin", () -> new ItemBlockWoodenCoffin(WOODEN_COFFIN.get()));
	public static final DeferredItem<ItemBlockStoneCoffin> STONE_COFFIN_ITEM = ITEM_BLOCKS.register("stone_coffin", () -> new ItemBlockStoneCoffin(STONE_COFFIN.get()));
	public static final DeferredItem<WoodItemBlock> STOOL_ITEM = ITEM_BLOCKS.register("stool", () -> new WoodItemBlock(STOOL.get()));
	public static final DeferredItem<BlockItem> URN_ITEM = ITEM_BLOCKS.register("urn", () -> new ItemBlockBase(URN.get()));
	public static final DeferredItem<WoodItemBlock> TABLE_ITEM = ITEM_BLOCKS.register("table", () -> new WoodItemBlock(TABLE.get()));
	public static final DeferredItem<ItemBlockChair> CHAIR_ITEM = ITEM_BLOCKS.register("chair", () -> new ItemBlockChair(CHAIR.get()));
	public static final DeferredItem<BlockItem> TRIBAL_STOOL_ITEM = ITEM_BLOCKS.register("tribal_stool", () -> new ItemBlockBase(TRIBAL_STOOL.get()));
	public static final DeferredItem<BlockItem> WOODEN_THRONE_ITEM = ITEM_BLOCKS.register("wooden_throne", () -> new ItemBlockBase(WOODEN_THRONE.get()));
	public static final DeferredItem<BlockItem> GOLDEN_THRONE_ITEM = ITEM_BLOCKS.register("golden_throne", () -> new ItemBlockBase(GOLDEN_THRONE.get()));
	public static final DeferredItem<ItemBlockPosts> WOODEN_POST_ITEM = ITEM_BLOCKS.register("wooden_post", () -> new ItemBlockPosts(WOODEN_POST.get()));
	public static final DeferredItem<ItemBlockPosts> IRON_CAGE_ITEM = ITEM_BLOCKS.register("iron_cage", () -> new ItemBlockPosts(IRON_CAGE.get()));
	public static final DeferredItem<ItemBlockPosts> GIBBET_ITEM = ITEM_BLOCKS.register("gibbet", () -> new ItemBlockPosts(GIBBET.get()));
	public static final DeferredItem<ItemBlockStretchingRack> STRETCHING_RACK_ITEM = ITEM_BLOCKS.register("stretching_rack", () -> new ItemBlockStretchingRack(STRETCHING_RACK.get()));
	public static final DeferredItem<ItemMultiBlock> STAKE_ITEM = ITEM_BLOCKS.register("stake", () -> new ItemMultiBlock(STAKE.get(), new Vec3i(0, 0, 0), new Vec3i(0, 2, 0)));
	public static final DeferredItem<WoodItemBlock> BENCH_ITEM = ITEM_BLOCKS.register("bench", () -> new WoodItemBlock(BENCH.get()));
	public static final DeferredItem<ItemMultiBlock> TRIBAL_CHAIR_ITEM = ITEM_BLOCKS.register("tribal_chair", () -> new ItemMultiBlock(TRIBAL_CHAIR.get(), new Vec3i(0, 0, 0), new Vec3i(0, 1, 0)));
	public static final DeferredItem<ItemMultiBlock> SCISSOR_SEAT_ITEM = ITEM_BLOCKS.register("scissor_seat", () -> new ItemMultiBlock(SCISSOR_SEAT.get(), new Vec3i(0, 0, 0), new Vec3i(0, 1, 0)));
	public static final DeferredItem<BlockItem> STATUE_ITEM = ITEM_BLOCKS.register("statue", () -> new ItemBlockBase(STATUE.get()));
	public static final DeferredItem<BlockItem> COIN_STACK_COPPER_ITEM = ITEM_BLOCKS.register("coin_stack_copper", () -> new ItemBlockBase(COIN_STACK_COPPER.get()));
	public static final DeferredItem<BlockItem> COIN_STACK_SILVER_ITEM = ITEM_BLOCKS.register("coin_stack_silver", () -> new ItemBlockBase(COIN_STACK_SILVER.get()));
	public static final DeferredItem<BlockItem> COIN_STACK_GOLD_ITEM = ITEM_BLOCKS.register("coin_stack_gold", () -> new ItemBlockBase(COIN_STACK_GOLD.get()));
	public static final DeferredItem<BlockItem> COIN_STACK_ANCIENT_ITEM = ITEM_BLOCKS.register("coin_stack_ancient", () -> new ItemBlockBase(COIN_STACK_ANCIENT.get()));
	public static final DeferredItem<ItemBlockGravestone> GRAVESTONE_ITEM = ITEM_BLOCKS.register("gravestone", () -> new ItemBlockGravestone(GRAVESTONE.get()));

	/* block entity types, 1.12 registerTile ids */
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileAdvancedSpawner>> ADVANCED_SPAWNER_TILE = tile("advanced_spawner_tile", TileAdvancedSpawner::new, ADVANCED_SPAWNER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TEGateProxy>> GATE_PROXY_TILE = tile("gate_proxy_tile", TEGateProxy::new, GATE_PROXY);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileDraftingStation>> DRAFTING_STATION_TILE = tile("drafting_station_tile", TileDraftingStation::new, DRAFTING_STATION);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileStructureBuilder>> STRUCTURE_BUILDER_TICKED_TILE = tile("structure_builder_ticked_tile", TileStructureBuilder::new, STRUCTURE_BUILDER_TICKED);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileSoundBlock>> SOUND_BLOCK_TILE = tile("sound_block_tile", TileSoundBlock::new, SOUND_BLOCK);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileStructureScanner>> STRUCTURE_SCANNER_BLOCK_TILE = tile("structure_scanner_block_tile", TileStructureScanner::new, STRUCTURE_SCANNER_BLOCK);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileAdvancedLootChest>> ADVANCED_LOOT_CHEST_TILE = tile("advanced_loot_chest_tile", TileAdvancedLootChest::new, ADVANCED_LOOT_CHEST);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTotemPart>> TOTEM_PART_TILE = tile("totem_part_tile", TileTotemPart::new, TOTEM_PART);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileColored>> COLORED_TILE = tile("colored_tile", TileColored::new, ALTAR_SHORT_CLOTH, ALTAR_LONG_CLOTH);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileAltarCandle>> ALTAR_CANDLE_TILE = tile("altar_candle_tile", TileAltarCandle::new, ALTAR_CANDLE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileProtectionFlag>> PROTECTION_FLAG_TILE = tile("protection_flag_tile", TileProtectionFlag::new, PROTECTION_FLAG);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileDecorativeFlag>> DECORATIVE_FLAG_TILE = tile("decorative_flag_tile", TileDecorativeFlag::new, DECORATIVE_FLAG);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileLootBasket>> LOOT_BASKET_TILE = tile("loot_basket", TileLootBasket::new, LOOT_BASKET);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWoodenCoffin>> WOODEN_COFFIN_TILE = tile("wooden_coffin", TileWoodenCoffin::new, WOODEN_COFFIN);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileStoneCoffin>> STONE_COFFIN_TILE = tile("stone_coffin", TileStoneCoffin::new, STONE_COFFIN);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileUrn>> URN_TILE = tile("urn_tile", TileUrn::new, URN);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileChair>> CHAIR_TILE = tile("chair_tile", TileChair::new, CHAIR);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileStake>> STAKE_TILE = tile("stake_tile", TileStake::new, STAKE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileStatue>> STATUE_TILE = tile("statue_tile", TileStatue::new, STATUE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileGravestone>> GRAVESTONE_TILE = tile("gravestone_tile", TileGravestone::new, GRAVESTONE);

	@SafeVarargs
	private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> tile(String teId, BiFunction<BlockPos, BlockState, T> factory, Supplier<? extends Block>... blocks) {
		return TILES.register(teId, () -> {
			Block[] valid = new Block[blocks.length];
			for (int i = 0; i < blocks.length; i++) {
				valid[i] = blocks[i].get();
			}
			return BlockEntityType.Builder.of(factory::apply, valid).build(null);
		});
	}

	public static void register(IEventBus modBus) {
		BLOCKS.register(modBus);
		ITEM_BLOCKS.register(modBus);
		TILES.register(modBus);
	}

	/*
	 * 1.12 registerLootContainers (ran at the end of the item registry event): call from common setup
	 */
	public static void registerLootContainers() {
		ItemLootChestPlacer.registerLootContainer(registryName(ADVANCED_LOOT_CHEST.get()), new ItemStack(ADVANCED_LOOT_CHEST.get()), LootContainerInfo.SINGLE_BLOCK_PLACEMENT_CHECKER);
		ItemLootChestPlacer.registerLootContainer(registryName(LOOT_BASKET.get()), new ItemStack(LOOT_BASKET.get()), LootContainerInfo.SINGLE_BLOCK_PLACEMENT_CHECKER);

		NonNullList<ItemStack> subBlocks = NonNullList.create();
		AWStructureTab.getSubBlocks(WOODEN_COFFIN.get(), subBlocks);
		subBlocks.forEach(subBlock -> ItemLootChestPlacer.registerLootContainer(
				registryName(WOODEN_COFFIN.get()) + "_" + ItemBlockWoodenCoffin.getVariant(subBlock), subBlock,
				(block, world, pos, sidePlacedOn, placer) -> ItemBlockWoodenCoffin.canPlace(world, pos, sidePlacedOn, placer)));

		NonNullList<ItemStack> stoneSubBlocks = NonNullList.create();
		AWStructureTab.getSubBlocks(STONE_COFFIN.get(), stoneSubBlocks);
		stoneSubBlocks.forEach(subBlock -> ItemLootChestPlacer.registerLootContainer(
				registryName(STONE_COFFIN.get()) + "_" + ItemBlockStoneCoffin.getVariant(subBlock), subBlock,
				(block, world, pos, sidePlacedOn, placer) -> ItemBlockStoneCoffin.canPlace(world, pos, sidePlacedOn, placer)));

		NonNullList<ItemStack> graveSubBlocks = NonNullList.create();
		AWStructureTab.getSubBlocks(GRAVESTONE.get(), graveSubBlocks);
		graveSubBlocks.forEach(subBlock -> ItemLootChestPlacer.registerLootContainer(
				registryName(GRAVESTONE.get()) + "_" + ItemBlockGravestone.getVariant(subBlock), subBlock,
				LootContainerInfo.SINGLE_BLOCK_PLACEMENT_CHECKER));

		ItemLootChestPlacer.registerLootContainer(registryName(URN.get()), new ItemStack(URN.get()), LootContainerInfo.SINGLE_BLOCK_PLACEMENT_CHECKER);
	}

	private static String registryName(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block).toString();
	}
}
