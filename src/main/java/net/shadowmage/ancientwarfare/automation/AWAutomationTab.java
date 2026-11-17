package net.shadowmage.ancientwarfare.automation;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationItems;
import net.shadowmage.ancientwarfare.core.init.AWCoreItems;

import java.util.List;

/*
 * Port notes: 1.12 CreativeTabs subclass ("tabs.automation") -> registered CreativeModeTab "ancientwarfare:automation"
 * with the 1.12 title key. The content is listed here in the order the 1.12 SortItemsFirstComparator produced:
 * tree farm, crop farm, fruit farm, animal farm, fish farm, quarry, auto crafting, warehouse control, interface,
 * crafting, storage, stock viewer, stock linker, mailbox, torque blocks, flywheel storage, windmill blade, chunk
 * loaders, then the remaining items (worksite upgrades).
 */
public class AWAutomationTab {
	private AWAutomationTab() {}

	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AncientWarfareAutomation.MOD_ID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("automation", () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.tabs.automation"))
			.icon(() -> new ItemStack(AWCoreItems.IRON_HAMMER.get()))
			.displayItems((parameters, output) -> {
				output.accept(AWAutomationBlocks.TREE_FARM_ITEM.get());
				output.accept(AWAutomationBlocks.CROP_FARM_ITEM.get());
				output.accept(AWAutomationBlocks.FRUIT_FARM_ITEM.get());
				output.accept(AWAutomationBlocks.ANIMAL_FARM_ITEM.get());
				output.accept(AWAutomationBlocks.FISH_FARM_ITEM.get());
				output.accept(AWAutomationBlocks.QUARRY_ITEM.get());
				output.accept(AWAutomationBlocks.AUTO_CRAFTING_ITEM.get());
				output.accept(AWAutomationBlocks.WAREHOUSE_CONTROL_ITEM.get());
				output.accept(AWAutomationBlocks.WAREHOUSE_INTERFACE_ITEM.get());
				output.accept(AWAutomationBlocks.WAREHOUSE_CRAFTING_ITEM.get());
				acceptAll(output, AWAutomationBlocks.WAREHOUSE_STORAGE_ITEMS);
				output.accept(AWAutomationBlocks.WAREHOUSE_STOCK_VIEWER_ITEM.get());
				output.accept(AWAutomationBlocks.WAREHOUSE_STOCK_LINKER_ITEM.get());
				output.accept(AWAutomationBlocks.MAILBOX_ITEM.get());
				acceptAll(output, AWAutomationBlocks.FLYWHEEL_CONTROLLER_ITEMS);
				acceptAll(output, AWAutomationBlocks.TORQUE_JUNCTION_ITEMS);
				acceptAll(output, AWAutomationBlocks.TORQUE_SHAFT_ITEMS);
				acceptAll(output, AWAutomationBlocks.TORQUE_DISTRIBUTOR_ITEMS);
				output.accept(AWAutomationBlocks.STIRLING_GENERATOR_ITEM.get());
				output.accept(AWAutomationBlocks.WATERWHEEL_GENERATOR_ITEM.get());
				output.accept(AWAutomationBlocks.HAND_CRANKED_GENERATOR_ITEM.get());
				output.accept(AWAutomationBlocks.WINDMILL_GENERATOR_ITEM.get());
				acceptAll(output, AWAutomationBlocks.FLYWHEEL_STORAGE_ITEMS);
				output.accept(AWAutomationBlocks.WINDMILL_BLADE_ITEM.get());
				output.accept(AWAutomationBlocks.CHUNK_LOADER_SIMPLE_ITEM.get());
				output.accept(AWAutomationBlocks.CHUNK_LOADER_DELUXE_ITEM.get());
				AWAutomationItems.WORKSITE_UPGRADES.forEach(upgrade -> output.accept(upgrade.get()));
			})
			.build());

	private static void acceptAll(CreativeModeTab.Output output, List<DeferredItem<BlockItem>> items) {
		items.forEach(item -> output.accept(item.get()));
	}

	public static void register(IEventBus modBus) {
		TABS.register(modBus);
	}
}
