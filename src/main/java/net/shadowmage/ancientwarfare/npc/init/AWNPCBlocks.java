package net.shadowmage.ancientwarfare.npc.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.core.item.ItemBlockOwned;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;
import net.shadowmage.ancientwarfare.npc.block.BlockTownHall;
import net.shadowmage.ancientwarfare.npc.tile.TileTownHall;

/*
 * Port notes: @ObjectHolder fields -> deferred holders (call .get() at use sites). 1.12 TileRecruitingStation was
 * never registered (dead code) and is not ported.
 */
public class AWNPCBlocks {
	private AWNPCBlocks() {}

	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AncientWarfareNPC.MOD_ID);
	private static final DeferredRegister.Items ITEM_BLOCKS = DeferredRegister.createItems(AncientWarfareNPC.MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AncientWarfareNPC.MOD_ID);

	public static final DeferredBlock<BlockTownHall> TOWN_HALL = BLOCKS.register("town_hall", BlockTownHall::new);

	public static final DeferredItem<BlockItem> TOWN_HALL_ITEM = ITEM_BLOCKS.register("town_hall", () -> new ItemBlockOwned(TOWN_HALL.get()));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTownHall>> TOWN_HALL_TILE = TILES.register("town_hall_tile",
			() -> BlockEntityType.Builder.of(TileTownHall::new, TOWN_HALL.get()).build(null));

	public static void register(IEventBus modBus) {
		BLOCKS.register(modBus);
		ITEM_BLOCKS.register(modBus);
		TILES.register(modBus);
		modBus.addListener(AWNPCBlocks::registerCapabilities);
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		//1.12 TileTownHall.getCapability: the inventory on every side
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TOWN_HALL_TILE.get(), (tile, side) -> tile.getInventory());
	}
}
