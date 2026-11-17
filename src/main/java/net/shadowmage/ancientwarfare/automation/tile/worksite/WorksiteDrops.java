package net.shadowmage.ancientwarfare.automation.tile.worksite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/*
 * Port addition: replaces the 1.12 calls Block.getDrops(drops, world, pos, state, fortune). In 1.21.1 drops come from
 * loot tables that read the fortune level from the tool in the loot context (same approach as core BlockTools).
 */
public final class WorksiteDrops {
	private WorksiteDrops() {}

	public static NonNullList<ItemStack> getDrops(Level level, BlockPos pos, BlockState state, int fortune) {
		NonNullList<ItemStack> drops = NonNullList.create();
		getDrops(drops, level, pos, state, fortune);
		return drops;
	}

	public static void getDrops(NonNullList<ItemStack> drops, Level level, BlockPos pos, BlockState state, int fortune) {
		if (level instanceof ServerLevel) {
			drops.addAll(Block.getDrops(state, (ServerLevel) level, pos, level.getBlockEntity(pos), null, getFortuneTool(level, fortune)));
		}
	}

	private static ItemStack getFortuneTool(Level world, int fortune) {
		if (fortune <= 0) {
			return ItemStack.EMPTY;
		}
		ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
		world.registryAccess().lookup(Registries.ENCHANTMENT).flatMap(reg -> reg.get(Enchantments.FORTUNE)).ifPresent(e -> tool.enchant(e, fortune));
		return tool;
	}
}
