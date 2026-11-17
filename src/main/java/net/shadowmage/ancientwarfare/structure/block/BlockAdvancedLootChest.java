package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.structure.tile.TileAdvancedLootChest;

/*
 * Port notes: 1.12 BlockChest(Type.BASIC) -> ChestBlock with the mod's own block entity type (vanilla chest
 * properties: wood, hardness 2.5, wood sound). getItemDropped (a vanilla chest) is the loot table
 * loot_table/blocks/advanced_loot_chest.json; the contents drop through the vanilla ChestBlock.onRemove.
 * The chest renderer / item renderer registration lives in AncientWarfareStructureClient.
 */
public class BlockAdvancedLootChest extends ChestBlock {
	public BlockAdvancedLootChest() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava(),
				() -> AWStructureBlocks.ADVANCED_LOOT_CHEST_TILE.get());
	}

	@Override
	public String getDescriptionId() {
		return "tile.advanced_loot_chest.name";
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileAdvancedLootChest(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (!world.isClientSide && WorldTools.getTile(world, pos, TileAdvancedLootChest.class).map(te -> te.fillWithLootAndCheckIfGoodToOpen(player)).orElse(false)) {
			MenuProvider ilockablecontainer = getMenuProvider(state, world, pos);
			if (ilockablecontainer != null) {
				player.openMenu(ilockablecontainer);
				player.awardStat(Stats.CUSTOM.get(Stats.OPEN_CHEST));
			}
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
