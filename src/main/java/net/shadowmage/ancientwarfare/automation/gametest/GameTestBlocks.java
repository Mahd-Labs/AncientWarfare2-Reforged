package net.shadowmage.ancientwarfare.automation.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.AncientWarfare;

import java.util.List;

/*
 * Blocks that stand in for another mod's broken crop in the GameTests (AutomationGameTests
 * cropFarmSurvivesThrowingCrop, cropFarmFertilizesCropThatTurnsIntoAnotherBlock). Registered only outside production
 * (AncientWarfareAutomation.init checks FMLEnvironment.production), so the released jar has no such blocks. No item,
 * model or loot table: the GameTest server is headless and places them with setBlock.
 */
public final class GameTestBlocks {
	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AncientWarfare.MOD_ID);

	/* throws from performBonemeal and from getDrops, as a broken block of another mod would */
	public static final DeferredBlock<Block> THROWING_CROP = BLOCKS.register("gametest_throwing_crop", () -> new ThrowingCrop(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
	/* Farmer's Delight's budding tomatoes in small: bone meal turns it into VINE_CROP, a different block */
	public static final DeferredBlock<Block> BUDDING_CROP = BLOCKS.register("gametest_budding_crop", () -> new BuddingCrop(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
	/* the tomato vine in small: its isValidBonemealTarget reads a property only it has (it throws for another state) */
	public static final DeferredBlock<Block> VINE_CROP = BLOCKS.register("gametest_vine_crop", () -> new VineCrop(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));

	/* a stone-like block whose drops throw, for the quarry */
	public static final DeferredBlock<Block> THROWING_BLOCK = BLOCKS.register("gametest_throwing_block", () -> new ThrowingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));

	private GameTestBlocks() {}

	public static void register(IEventBus modBus) {
		BLOCKS.register(modBus);
	}

	private static class ThrowingCrop extends CropBlock {
		ThrowingCrop(Properties properties) {
			super(properties);
		}

		@Override
		public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
			throw new IllegalArgumentException("GameTest: a broken crop of another mod threw while being fertilized");
		}

		@Override
		protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
			throw new IllegalArgumentException("GameTest: a broken crop of another mod threw while being harvested");
		}
	}

	private static class ThrowingBlock extends Block {
		ThrowingBlock(Properties properties) {
			super(properties);
		}

		@Override
		protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
			throw new IllegalArgumentException("GameTest: a broken block of another mod threw while being dug");
		}
	}

	private static class BuddingCrop extends CropBlock {
		BuddingCrop(Properties properties) {
			super(properties);
		}

		@Override
		public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
			return true;
		}

		@Override
		public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
			level.setBlock(pos, VINE_CROP.get().defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	private static class VineCrop extends CropBlock {
		static final BooleanProperty VINE = BooleanProperty.create("vine");

		VineCrop(Properties properties) {
			super(properties);
			registerDefaultState(defaultBlockState().setValue(VINE, true));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(VINE);
		}

		@Override
		public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
			//IllegalArgumentException "Cannot get property ... as it does not exist" when handed another block's state
			return state.getValue(VINE) && super.isValidBonemealTarget(level, pos, state);
		}
	}
}
