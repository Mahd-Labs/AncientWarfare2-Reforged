package net.shadowmage.ancientwarfare.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.core.init.AWCoreBlocks;
import net.shadowmage.ancientwarfare.core.init.AWCoreItems;

/*
 * Port notes: 1.12 CreativeTabs subclass ("tabs.awcore") -> registered CreativeModeTab "ancientwarfare:awcore" with
 * the 1.12 title key. 1.12 items put themselves into the tab (setCreativeTab / getSubItems); the tab lists its
 * content here, in the 1.12 registration order.
 */
public class AWCoreTab {
	private AWCoreTab() {}

	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AncientWarfareCore.MOD_ID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("awcore", () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.tabs.awcore"))
			.icon(() -> new ItemStack(AWCoreItems.RESEARCH_BOOK.get()))
			.displayItems((parameters, output) -> {
				output.accept(AWCoreBlocks.ENGINEERING_STATION_ITEM.get());
				output.accept(AWCoreBlocks.RESEARCH_STATION_ITEM.get());
				output.accept(AWCoreItems.INFO_TOOL.get());
				output.accept(AWCoreItems.RESEARCH_BOOK.get());
				AWCoreItems.RESEARCH_NOTE.get().getSubItems().forEach(output::accept);
				AWCoreItems.BACKPACKS.forEach(backpack -> output.accept(backpack.get()));
				output.accept(AWCoreItems.WOODEN_QUILL.get());
				output.accept(AWCoreItems.STONE_QUILL.get());
				output.accept(AWCoreItems.IRON_QUILL.get());
				output.accept(AWCoreItems.GOLD_QUILL.get());
				output.accept(AWCoreItems.DIAMOND_QUILL.get());
				output.accept(AWCoreItems.WOODEN_HAMMER.get());
				output.accept(AWCoreItems.STONE_HAMMER.get());
				output.accept(AWCoreItems.IRON_HAMMER.get());
				output.accept(AWCoreItems.GOLD_HAMMER.get());
				output.accept(AWCoreItems.DIAMOND_HAMMER.get());
				output.accept(AWCoreItems.MANUAL.get());
				AWCoreItems.COMPONENTS.forEach(component -> output.accept(component.get()));
				output.accept(AWCoreItems.STEEL_INGOT.get());
			})
			.build());

	public static void register(IEventBus modBus) {
		TABS.register(modBus);
	}
}
