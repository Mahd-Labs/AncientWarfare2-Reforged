package net.shadowmage.ancientwarfare.npc;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.npc.init.AWNPCBlocks;
import net.shadowmage.ancientwarfare.npc.init.AWNPCItems;
import net.shadowmage.ancientwarfare.npc.item.ItemBardInstrument;
import net.shadowmage.ancientwarfare.npc.item.ItemCoin;
import net.shadowmage.ancientwarfare.npc.item.ItemCommandBaton;
import net.shadowmage.ancientwarfare.npc.item.ItemNpcSpawner;
import net.shadowmage.ancientwarfare.npc.item.ItemOrders;

import java.util.ArrayList;
import java.util.List;

/*
 * Port notes: 1.12 collected every item with this tab (registry order, sub items expanded) and sorted the town hall,
 * order items, command batons, bard instruments and npc spawners to the front (SortItemsFirstComparator, stable for
 * the rest). The same list is built here by hand: the five groups in that order, then everything else in
 * registration order.
 */
public class AWNPCTab {
	private AWNPCTab() {}

	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AncientWarfareNPC.MOD_ID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("npc", () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.tabs.npc"))
			.icon(() -> new ItemStack(AWNPCItems.NPC_SPAWNER.get()))
			.displayItems((parameters, output) -> getTabItems().forEach(output::accept))
			.build());

	public static List<ItemStack> getTabItems() {
		List<ItemStack> all = new ArrayList<>();
		for (DeferredItem<? extends Item> holder : AWNPCItems.allItems()) {
			Item item = holder.get();
			if (item instanceof ItemNpcSpawner) {
				all.addAll(ItemNpcSpawner.getSpawnerSubItems());
			} else if (item instanceof ItemCoin) {
				all.addAll(((ItemCoin) item).getSubItems());
			} else {
				all.add(new ItemStack(item));
			}
		}
		List<ItemStack> sorted = new ArrayList<>();
		sorted.add(new ItemStack(AWNPCBlocks.TOWN_HALL_ITEM.get()));
		moveMatching(all, sorted, ItemOrders.class);
		moveMatching(all, sorted, ItemCommandBaton.class);
		moveMatching(all, sorted, ItemBardInstrument.class);
		moveMatching(all, sorted, ItemNpcSpawner.class);
		sorted.addAll(all);
		return sorted;
	}

	private static void moveMatching(List<ItemStack> from, List<ItemStack> to, Class<?> itemClass) {
		for (ItemStack stack : new ArrayList<>(from)) {
			if (itemClass.isInstance(stack.getItem())) {
				to.add(stack);
				from.remove(stack);
			}
		}
	}

	public static void register(IEventBus modBus) {
		TABS.register(modBus);
	}
}
