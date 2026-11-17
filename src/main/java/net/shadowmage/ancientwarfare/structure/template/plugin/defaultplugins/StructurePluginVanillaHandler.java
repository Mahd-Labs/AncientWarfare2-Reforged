package net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.shadowmage.ancientwarfare.core.init.AWCoreBlocks;
import net.shadowmage.ancientwarfare.structure.api.StructureContentPlugin;
import net.shadowmage.ancientwarfare.structure.entity.EntityGate;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.structure.template.StructurePluginManager;
import net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules.TemplateRuleBlockDoors;
import net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules.TemplateRuleBlockInventory;
import net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules.TemplateRuleBlockTile;
import net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules.TemplateRuleCoffin;
import net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules.TemplateRuleFlag;
import net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules.TemplateRuleFluid;
import net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules.TemplateRuleStructureBuilder;
import net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules.TemplateRuleTotemPart;
import net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules.TemplateRuleVanillaSpawner;
import net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules.TemplateRuleVine;
import net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.entityrules.TemplateRuleGates;

public class StructurePluginVanillaHandler implements StructureContentPlugin {
	@Override
	public void addHandledBlocks(StructurePluginManager manager) {
		//Port notes: 1.12 listed the vanilla door blocks one by one
		manager.registerBlockHandler(TemplateRuleBlockDoors.PLUGIN_NAME, (world, pos, state) -> state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock, TemplateRuleBlockDoors::new, TemplateRuleBlockDoors::new);
		manager.registerBlockHandler(TemplateRuleVine.PLUGIN_NAME, Blocks.VINE, TemplateRuleVine::new, TemplateRuleVine::new);
		manager.registerBlockHandler(TemplateRuleVanillaSpawner.PLUGIN_NAME, Blocks.SPAWNER, TemplateRuleVanillaSpawner::new, TemplateRuleVanillaSpawner::new);
		manager.registerBlockHandler(TemplateRuleBlockTile.PLUGIN_NAME, Blocks.COMMAND_BLOCK, TemplateRuleBlockTile::new, TemplateRuleBlockTile::new);
		manager.registerBlockHandler(TemplateRuleBlockTile.PLUGIN_NAME, Blocks.BEACON, TemplateRuleBlockTile::new, TemplateRuleBlockTile::new);
		manager.registerBlockHandler(TemplateRuleBlockTile.PLUGIN_NAME, Blocks.COMMAND_BLOCK, TemplateRuleBlockTile::new, TemplateRuleBlockTile::new);
		manager.registerBlockHandler(TemplateRuleBlockTile.PLUGIN_NAME, Blocks.CHAIN_COMMAND_BLOCK, TemplateRuleBlockTile::new, TemplateRuleBlockTile::new);
		manager.registerBlockHandler(TemplateRuleBlockTile.PLUGIN_NAME, Blocks.REPEATING_COMMAND_BLOCK, TemplateRuleBlockTile::new, TemplateRuleBlockTile::new);
		manager.registerBlockHandler(TemplateRuleBlockInventory.PLUGIN_NAME, Blocks.DISPENSER,
				(world, pos, state, turns) -> new TemplateRuleBlockInventory(world, pos, state, turns, new Direction[] {null}, true),
				TemplateRuleBlockInventory::new);
		manager.registerBlockHandler(TemplateRuleBlockInventory.PLUGIN_NAME, Blocks.CHEST,
				(world, pos, state, turns) -> new TemplateRuleBlockInventory(world, pos, state, turns, new Direction[] {null}, true),
				TemplateRuleBlockInventory::new);
		manager.registerBlockHandler(TemplateRuleBlockInventory.PLUGIN_NAME, Blocks.DROPPER,
				(world, pos, state, turns) -> new TemplateRuleBlockInventory(world, pos, state, turns, new Direction[] {null}, true),
				TemplateRuleBlockInventory::new);
		manager.registerBlockHandler(TemplateRuleBlockInventory.PLUGIN_NAME, Blocks.HOPPER,
				(world, pos, state, turns) -> new TemplateRuleBlockInventory(world, pos, state, turns, new Direction[] {null}, true),
				TemplateRuleBlockInventory::new);
		manager.registerBlockHandler(TemplateRuleBlockInventory.PLUGIN_NAME, Blocks.TRAPPED_CHEST,
				(world, pos, state, turns) -> new TemplateRuleBlockInventory(world, pos, state, turns, new Direction[] {null}, true),
				TemplateRuleBlockInventory::new);
		manager.registerBlockHandler(TemplateRuleFluid.PLUGIN_NAME, Blocks.WATER, TemplateRuleFluid::new, TemplateRuleFluid::new);
		manager.registerBlockHandler(TemplateRuleFluid.PLUGIN_NAME, Blocks.LAVA, TemplateRuleFluid::new, TemplateRuleFluid::new);

		manager.registerBlockHandler(TemplateRuleBlockTile.PLUGIN_NAME, AWStructureBlocks.ADVANCED_SPAWNER.get(), TemplateRuleBlockTile::new, TemplateRuleBlockTile::new);
		manager.registerBlockHandler(TemplateRuleTotemPart.PLUGIN_NAME, AWStructureBlocks.TOTEM_PART.get(), TemplateRuleTotemPart::new, TemplateRuleTotemPart::new);
		manager.registerBlockHandler(TemplateRuleCoffin.PLUGIN_NAME, AWStructureBlocks.WOODEN_COFFIN.get(), TemplateRuleCoffin::new, TemplateRuleCoffin::new);
		manager.registerBlockHandler(TemplateRuleCoffin.PLUGIN_NAME, AWStructureBlocks.STONE_COFFIN.get(), TemplateRuleCoffin::new, TemplateRuleCoffin::new);
		manager.registerBlockHandler(TemplateRuleBlockTile.PLUGIN_NAME, AWCoreBlocks.ENGINEERING_STATION.get(), TemplateRuleBlockTile::new, TemplateRuleBlockTile::new);
		manager.registerBlockHandler(TemplateRuleBlockTile.PLUGIN_NAME, AWCoreBlocks.RESEARCH_STATION.get(), TemplateRuleBlockTile::new, TemplateRuleBlockTile::new);
		manager.registerBlockHandler(TemplateRuleBlockTile.PLUGIN_NAME, AWStructureBlocks.DRAFTING_STATION.get(), TemplateRuleBlockTile::new, TemplateRuleBlockTile::new);
		manager.registerBlockHandler(TemplateRuleStructureBuilder.PLUGIN_NAME, AWStructureBlocks.STRUCTURE_BUILDER_TICKED.get(), TemplateRuleStructureBuilder::new, TemplateRuleStructureBuilder::new);
		manager.registerBlockHandler(TemplateRuleBlockTile.PLUGIN_NAME, AWStructureBlocks.SOUND_BLOCK.get(), TemplateRuleBlockTile::new, TemplateRuleBlockTile::new);
		manager.registerBlockHandler(TemplateRuleBlockTile.PLUGIN_NAME, AWStructureBlocks.ADVANCED_LOOT_CHEST.get(), TemplateRuleBlockTile::new, TemplateRuleBlockTile::new);
		manager.registerBlockHandler(TemplateRuleFlag.PLUGIN_NAME, AWStructureBlocks.PROTECTION_FLAG.get(), TemplateRuleFlag::new, TemplateRuleFlag::new);
		manager.registerBlockHandler(TemplateRuleFlag.PLUGIN_NAME, AWStructureBlocks.DECORATIVE_FLAG.get(), TemplateRuleFlag::new, TemplateRuleFlag::new);
	}

	@Override
	public void addHandledEntities(StructurePluginManager manager) {
		manager.registerEntityHandler(TemplateRuleGates.PLUGIN_NAME, EntityGate.class, TemplateRuleGates::new, TemplateRuleGates::new);
	}
}
