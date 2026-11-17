package net.shadowmage.ancientwarfare.automation.registry;

import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;
import net.shadowmage.ancientwarfare.automation.tile.worksite.treefarm.ChorusFlowerDrop;
import net.shadowmage.ancientwarfare.automation.tile.worksite.treefarm.ChorusScanner;
import net.shadowmage.ancientwarfare.automation.tile.worksite.treefarm.DefaultTreeScanner;
import net.shadowmage.ancientwarfare.automation.tile.worksite.treefarm.IBlockExtraDrop;
import net.shadowmage.ancientwarfare.automation.tile.worksite.treefarm.ISapling;
import net.shadowmage.ancientwarfare.automation.tile.worksite.treefarm.ITreeScanner;
import net.shadowmage.ancientwarfare.automation.tile.worksite.treefarm.Sapling;
import net.shadowmage.ancientwarfare.core.registry.IRegistryDataParser;
import net.shadowmage.ancientwarfare.core.util.parsing.BlockStateMatcher;
import net.shadowmage.ancientwarfare.core.util.parsing.JsonHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class TreeFarmRegistry {
	private TreeFarmRegistry() {}

	private static final ITreeScanner DEFAULT_TREE_SCANNER =
			new DefaultTreeScanner(st -> st.instrument() == NoteBlockInstrument.BASS && st.getBlock() != AWAutomationBlocks.TREE_FARM.get()
					, sl -> sl.is(BlockTags.LEAVES));

	private static Set<ISapling> saplings = new HashSet<>();
	private static Set<BlockStateMatcher> soilBlocks = new HashSet<>();
	private static Set<IBlockExtraDrop> extraBlockDrops = new HashSet<>();
	private static List<ITreeScanner> treeScanners = new ArrayList<>();

	static {
		saplings.add(new Sapling(s -> s.getItem() instanceof BlockItem && ((BlockItem) s.getItem()).getBlock() instanceof SaplingBlock, false));
		saplings.add(new Sapling(s -> s.getItem() instanceof BlockItem && ((BlockItem) s.getItem()).getBlock() instanceof MushroomBlock, false));
		//port: saplings of other mods that do not extend SaplingBlock (1.12: IPlantable), by the item or block tag
		saplings.add(new Sapling(s -> s.getItem() instanceof BlockItem && (s.is(net.minecraft.tags.ItemTags.SAPLINGS)
				|| ((BlockItem) s.getItem()).getBlock().defaultBlockState().is(net.minecraft.tags.BlockTags.SAPLINGS)), false));

		extraBlockDrops.add(new ChorusFlowerDrop());
	}

	private static void registerTreeScanner(ITreeScanner treeScanner) {
		treeScanners.add(0, treeScanner);
	}

	static {
		registerTreeScanner(new ChorusScanner());
	}

	public static ITreeScanner getTreeScanner(BlockState state) {
		return getRegisteredTreeScanner(state).orElse(DEFAULT_TREE_SCANNER);
	}

	public static Optional<ITreeScanner> getRegisteredTreeScanner(BlockState state) {
		return treeScanners.stream().filter(ts -> ts.matches(state)).findFirst();
	}

	public static Optional<ISapling> getSapling(ItemStack stack) {
		return saplings.stream().filter(s -> s.matches(stack)).findFirst();
	}

	public static boolean isPlantable(ItemStack stack) {
		return saplings.stream().anyMatch(s -> s.matches(stack));
	}

	public static boolean isSoil(BlockState state) {
		return soilBlocks.stream().anyMatch(m -> m.test(state));
	}

	public static IBlockExtraDrop getBlockExtraDrop(BlockState state) {
		return extraBlockDrops.stream().filter(b -> b.matches(state)).findFirst().orElse(EMPTY_EXTRA_DROP);
	}

	public static class PlantableParser implements IRegistryDataParser {

		@Override
		public String getName() {
			return "saplings";
		}

		@Override
		public void parse(JsonObject json) {
			JsonArray saplings = GsonHelper.getAsJsonArray(json, "saplings");

			for (JsonElement e : saplings) {
				JsonObject saplingDefinition = GsonHelper.convertToJsonObject(e, "");
				TreeFarmRegistry.saplings.add(new Sapling(JsonHelper.getItemStackMatcher(GsonHelper.getAsJsonObject(saplingDefinition, "sapling")),
						saplingDefinition.has("right_click") && GsonHelper.getAsBoolean(saplingDefinition, "right_click")));
			}
		}
	}

	public static class SoilParser implements IRegistryDataParser {

		@Override
		public String getName() {
			return "tree_soil_blocks";
		}

		@Override
		public void parse(JsonObject json) {
			JsonArray soils = GsonHelper.getAsJsonArray(json, "soils");

			for (JsonElement t : soils) {
				soilBlocks.add(JsonHelper.getBlockStateMatcher(GsonHelper.convertToJsonObject(t, "")));
			}
		}
	}

	public static class TreeScannerParser implements IRegistryDataParser {
		@Override
		public String getName() {
			return "tree_scanners";
		}

		@Override
		public void parse(JsonObject json) {
			JsonArray treeScanners = GsonHelper.getAsJsonArray(json, "tree_scanners");

			for (JsonElement ts : treeScanners) {
				JsonObject treeScanner = GsonHelper.convertToJsonObject(ts, "");
				switch (GsonHelper.getAsString(treeScanner, "type")) {
					case "default":
					default:
						DefaultSearchParser.parse(treeScanner);
				}
			}
		}

		private static class DefaultSearchParser {
			private DefaultSearchParser() {}

			public static void parse(JsonObject treeScanner) {
				Predicate<BlockState> trunkMatcher = JsonHelper.getBlockStateMatcher(treeScanner, "trunks", "trunk");
				Predicate<BlockState> leafMatcher = JsonHelper.getBlockStateMatcher(treeScanner, "leaves", "leaf");

				int maxLeafDistance = GsonHelper.getAsInt(treeScanner, "max_leaf_distance");

				DefaultTreeScanner.INextPositionGetter nextPosGetter = parseNextPositionGetter(treeScanner);

				Optional<DefaultTreeScanner> currentScanner = treeScanners.stream().filter(m -> m instanceof DefaultTreeScanner && ((DefaultTreeScanner) m).getTrunkMatcher().hashCode() == trunkMatcher.hashCode()).map(m -> (DefaultTreeScanner) m).findFirst();

				if (currentScanner.isPresent()) {
					currentScanner.get().addLeafMatcher(leafMatcher);
				} else {
					registerTreeScanner(new DefaultTreeScanner(trunkMatcher, leafMatcher, nextPosGetter, maxLeafDistance));
				}
			}

			private static DefaultTreeScanner.INextPositionGetter parseNextPositionGetter(JsonObject treeScanner) {
				switch (GsonHelper.getAsString(treeScanner, "next_block_search")) {
					case "all_up_or_level":
						return DefaultTreeScanner.ALL_UP_OR_LEVEL;
					case "all_around":
						return DefaultTreeScanner.ALL_AROUND;
					case "connected_around":
						return DefaultTreeScanner.CONNECTED_AROUND;
					case "connected_down_or_level":
						return DefaultTreeScanner.CONNECTED_DOWN_OR_LEVEL;
					case "connected_up_or_level":
					default:
						return DefaultTreeScanner.CONNECTED_UP_OR_LEVEL;
				}
			}
		}
	}

	private static final IBlockExtraDrop EMPTY_EXTRA_DROP = new IBlockExtraDrop() {
		@Override
		public boolean matches(BlockState state) {
			return true;
		}

		@Override
		public NonNullList<ItemStack> getDrops(BlockGetter level, BlockPos pos, BlockState state, int fortune) {
			return NonNullList.create();
		}
	};
}
