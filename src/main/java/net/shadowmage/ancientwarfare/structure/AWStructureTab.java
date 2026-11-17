package net.shadowmage.ancientwarfare.structure;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.core.util.NBTBuilder;
import net.shadowmage.ancientwarfare.structure.block.BlockAdvancedSpawner;
import net.shadowmage.ancientwarfare.structure.block.BlockBench;
import net.shadowmage.ancientwarfare.structure.block.BlockBrazierEmber;
import net.shadowmage.ancientwarfare.structure.block.BlockBrazierFlame;
import net.shadowmage.ancientwarfare.structure.block.BlockChair;
import net.shadowmage.ancientwarfare.structure.block.BlockCoinStack;
import net.shadowmage.ancientwarfare.structure.block.BlockFirePit;
import net.shadowmage.ancientwarfare.structure.block.BlockFlag;
import net.shadowmage.ancientwarfare.structure.block.BlockGravestone;
import net.shadowmage.ancientwarfare.structure.block.BlockStoneCoffin;
import net.shadowmage.ancientwarfare.structure.block.BlockStool;
import net.shadowmage.ancientwarfare.structure.block.BlockStructureBuilder;
import net.shadowmage.ancientwarfare.structure.block.BlockTable;
import net.shadowmage.ancientwarfare.structure.block.BlockTotemPart;
import net.shadowmage.ancientwarfare.structure.block.BlockWoodenCoffin;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.structure.init.AWStructureItems;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockColored;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockGravestone;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockStoneCoffin;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockWoodenCoffin;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;
import net.shadowmage.ancientwarfare.structure.tile.SpawnerSettings;
import net.shadowmage.ancientwarfare.structure.util.WoodVariantHelper;

/*
 * Port notes: 1.12 CreativeTabs subclass ("tabs.structures", icon: structure scanner) -> registered CreativeModeTab
 * "ancientwarfare:structures" with the 1.12 title key. 1.12 filled the tab from Block.getSubBlocks / Item.getSubItems
 * of everything that had this tab; those methods are gone, so their bodies live here in getSubBlocks(block, list)
 * (also used by AWStructureBlocks.registerLootContainers, like 1.12). Order: items (1.12 registration order), then
 * the item blocks (1.12 registration order). The gate proxy had no creative tab.
 */
public class AWStructureTab {
	private AWStructureTab() {}

	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AncientWarfareStructure.MOD_ID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("structures", () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.tabs.structures"))
			.icon(() -> new ItemStack(AWStructureItems.STRUCTURE_SCANNER.get()))
			.displayItems((parameters, output) -> {
				output.accept(AWStructureItems.STRUCTURE_SCANNER.get());
				output.accept(AWStructureItems.STRUCTURE_BUILDER.get());
				output.accept(AWStructureItems.STRUCTURE_BUILDER_WORLD_GEN.get());
				output.accept(AWStructureItems.TOWN_BUILDER.get());
				output.accept(AWStructureItems.SPAWNER_PLACER.get());
				//1.12 ItemGateSpawner.getSubItems
				AWStructureItems.GATE_SPAWNERS.values().forEach(gate -> output.accept(gate.get()));
				output.accept(AWStructureItems.CONSTRUCTION_TOOL.get());
				output.accept(AWStructureItems.CONSTRUCTION_TOOL_LAKES.get());
				output.accept(AWStructureItems.BLOCK_INFO_CLICKER.get());
				output.accept(AWStructureItems.LOOT_CHEST_PLACER.get());

				NonNullList<ItemStack> items = NonNullList.create();
				getSubBlocks(AWStructureBlocks.ADVANCED_SPAWNER.get(), items);
				getSubBlocks(AWStructureBlocks.DRAFTING_STATION.get(), items);
				getSubBlocks(AWStructureBlocks.STRUCTURE_BUILDER_TICKED.get(), items);
				getSubBlocks(AWStructureBlocks.SOUND_BLOCK.get(), items);
				getSubBlocks(AWStructureBlocks.STRUCTURE_SCANNER_BLOCK.get(), items);
				getSubBlocks(AWStructureBlocks.ADVANCED_LOOT_CHEST.get(), items);
				getSubBlocks(AWStructureBlocks.FIRE_PIT.get(), items);
				getSubBlocks(AWStructureBlocks.TOTEM_PART.get(), items);
				getSubBlocks(AWStructureBlocks.BRAZIER_EMBER.get(), items);
				getSubBlocks(AWStructureBlocks.BRAZIER_FLAME.get(), items);
				getSubBlocks(AWStructureBlocks.ALTAR_SHORT_CLOTH.get(), items);
				getSubBlocks(AWStructureBlocks.ALTAR_LONG_CLOTH.get(), items);
				getSubBlocks(AWStructureBlocks.ALTAR_CANDLE.get(), items);
				getSubBlocks(AWStructureBlocks.ALTAR_LECTERN.get(), items);
				getSubBlocks(AWStructureBlocks.ALTAR_SUN.get(), items);
				getSubBlocks(AWStructureBlocks.PROTECTION_FLAG.get(), items);
				getSubBlocks(AWStructureBlocks.DECORATIVE_FLAG.get(), items);
				getSubBlocks(AWStructureBlocks.GOLDEN_IDOL.get(), items);
				getSubBlocks(AWStructureBlocks.ORC_TOTEM_1.get(), items);
				getSubBlocks(AWStructureBlocks.ORC_TOTEM_2.get(), items);
				getSubBlocks(AWStructureBlocks.ORC_TOTEM_2_LIT.get(), items);
				getSubBlocks(AWStructureBlocks.GOBLIN_TOTEM_1.get(), items);
				getSubBlocks(AWStructureBlocks.GOBLIN_TOTEM_2.get(), items);
				getSubBlocks(AWStructureBlocks.GOBLIN_TOTEM_2_LIT.get(), items);
				getSubBlocks(AWStructureBlocks.LOOT_BASKET.get(), items);
				getSubBlocks(AWStructureBlocks.WOODEN_COFFIN.get(), items);
				getSubBlocks(AWStructureBlocks.STONE_COFFIN.get(), items);
				getSubBlocks(AWStructureBlocks.STOOL.get(), items);
				getSubBlocks(AWStructureBlocks.URN.get(), items);
				getSubBlocks(AWStructureBlocks.TABLE.get(), items);
				getSubBlocks(AWStructureBlocks.CHAIR.get(), items);
				getSubBlocks(AWStructureBlocks.TRIBAL_STOOL.get(), items);
				getSubBlocks(AWStructureBlocks.WOODEN_THRONE.get(), items);
				getSubBlocks(AWStructureBlocks.GOLDEN_THRONE.get(), items);
				getSubBlocks(AWStructureBlocks.WOODEN_POST.get(), items);
				getSubBlocks(AWStructureBlocks.IRON_CAGE.get(), items);
				getSubBlocks(AWStructureBlocks.GIBBET.get(), items);
				getSubBlocks(AWStructureBlocks.STRETCHING_RACK.get(), items);
				getSubBlocks(AWStructureBlocks.STAKE.get(), items);
				getSubBlocks(AWStructureBlocks.BENCH.get(), items);
				getSubBlocks(AWStructureBlocks.TRIBAL_CHAIR.get(), items);
				getSubBlocks(AWStructureBlocks.SCISSOR_SEAT.get(), items);
				getSubBlocks(AWStructureBlocks.STATUE.get(), items);
				getSubBlocks(AWStructureBlocks.COIN_STACK_COPPER.get(), items);
				getSubBlocks(AWStructureBlocks.COIN_STACK_SILVER.get(), items);
				getSubBlocks(AWStructureBlocks.COIN_STACK_GOLD.get(), items);
				getSubBlocks(AWStructureBlocks.COIN_STACK_ANCIENT.get(), items);
				getSubBlocks(AWStructureBlocks.GRAVESTONE.get(), items);
				items.forEach(stack -> output.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
			})
			.build());

	/*
	 * 1.12 Block.getSubBlocks of the structure blocks / Item.getSubItems of their item blocks
	 */
	public static void getSubBlocks(Block block, NonNullList<ItemStack> items) {
		if (block instanceof BlockAdvancedSpawner) {
			ItemStack stack = new ItemStack(block);
			SpawnerSettings settings = SpawnerSettings.getDefaultSettings();
			CompoundTag defaultTag = new CompoundTag();
			settings.writeToNBT(defaultTag);
			ItemTools.setTagInfo(stack, "spawnerSettings", defaultTag);
			items.add(stack);
		} else if (block instanceof BlockStool || block instanceof BlockTable || block instanceof BlockChair || block instanceof BlockBench) {
			WoodVariantHelper.getSubBlocks(block, items);
		} else if (block instanceof BlockBrazierEmber || block instanceof BlockBrazierFlame) {
			ItemStack stack = new ItemStack(block);
			ItemTools.setTag(stack, new NBTBuilder().setBoolean(BlockBrazierEmber.LIT_TAG, false).build());
			items.add(stack);
			stack = new ItemStack(block);
			ItemTools.setTag(stack, new NBTBuilder().setBoolean(BlockBrazierEmber.LIT_TAG, true).build());
			items.add(stack);
		} else if (block instanceof BlockFirePit) {
			for (BlockFirePit.Variant variant : BlockFirePit.Variant.values()) {
				ItemStack stack = new ItemStack(block);
				ItemTools.setTag(stack, new NBTBuilder().setString(BlockFirePit.VARIANT_TAG, variant.getSerializedName()).setBoolean(BlockFirePit.LIT_TAG, false).build());
				items.add(stack);
				stack = new ItemStack(block);
				ItemTools.setTag(stack, new NBTBuilder().setString(BlockFirePit.VARIANT_TAG, variant.getSerializedName()).setBoolean(BlockFirePit.LIT_TAG, true).build());
				items.add(stack);
			}
		} else if (block instanceof BlockCoinStack) {
			for (int stackSize = 8; stackSize < 65; stackSize = stackSize + 8) {
				ItemStack stack = new ItemStack(block);
				ItemTools.setTag(stack, new NBTBuilder().setInteger("size", stackSize).build());
				items.add(stack);
			}
		} else if (block instanceof BlockFlag) {
			for (BlockFlag.FlagDefinition flagDefinition : ((BlockFlag) block).getFlagDefinitions()) {
				ItemStack stack = new ItemStack(block);
				ItemTools.setTag(stack, new NBTBuilder()
						.setString("name", flagDefinition.getName())
						.build());
				items.add(stack);
			}
		} else if (block instanceof BlockGravestone) {
			for (int variant = 1; variant <= 8; variant++) {
				items.add(ItemBlockGravestone.getVariantStack(variant));
			}
		} else if (block instanceof BlockStoneCoffin) {
			for (BlockStoneCoffin.Variant variant : BlockStoneCoffin.Variant.values()) {
				items.add(ItemBlockStoneCoffin.getVariantStack(variant));
			}
		} else if (block instanceof BlockWoodenCoffin) {
			for (BlockWoodenCoffin.Variant variant : BlockWoodenCoffin.Variant.values()) {
				items.add(ItemBlockWoodenCoffin.getVariantStack(variant));
			}
		} else if (block instanceof BlockStructureBuilder) {
			for (String templateName : StructureTemplateManager.getSurvivalStructures().keySet()) {
				ItemStack item = new ItemStack(block);
				ItemTools.setTagInfo(item, "structureName", StringTag.valueOf(templateName));
				items.add(item);
			}
		} else if (block instanceof BlockTotemPart) {
			for (BlockTotemPart.Variant variant : BlockTotemPart.Variant.values()) {
				if (!variant.includeInSubBlocks()) {
					continue;
				}
				items.add(((BlockTotemPart) block).getVariantStack(variant));
			}
		} else if (block.asItem() instanceof ItemBlockColored) {
			//1.12 ItemBlockColored.getSubItems
			for (int dyeDamage = 0; dyeDamage < 16; ++dyeDamage) {
				ItemStack item = new ItemStack(block, 1);
				ItemTools.setTagInfo(item, "dyeColor", IntTag.valueOf(dyeDamage));
				items.add(item);
			}
			for (CompoundTag tag : ((ItemBlockColored) block.asItem()).getCustomItemTags()) {
				ItemStack item = new ItemStack(block, 1);
				ItemTools.setTag(item, tag.copy());
				items.add(item);
			}
		} else {
			items.add(new ItemStack(block));
		}
	}

	public static void register(IEventBus modBus) {
		TABS.register(modBus);
	}
}
