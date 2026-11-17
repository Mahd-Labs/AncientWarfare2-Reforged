package net.shadowmage.ancientwarfare.automation.tile.worksite.treefarm;

import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.block.BonemealableBlock;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.automation.registry.TreeFarmRegistry;
import net.shadowmage.ancientwarfare.automation.tile.worksite.IWorksiteAction;
import net.shadowmage.ancientwarfare.automation.tile.worksite.TileWorksiteFarm;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class WorkSiteTreeFarm extends TileWorksiteFarm {
	private static final int MAX_DISTANCE_TO_INITIAL_CUT_POSITION = 15;
	private static final String TARGET_LIST_TAG = "targetList";
	private static final String TARGET_LEAF_LIST_TAG = "targetLeafList";
	private boolean hasShears;
	private final Set<BlockPos> blocksToShear = new LinkedHashSet<>();
	private final Set<BlockPos> leafBlocksToChop = new LinkedHashSet<>();
	private final Set<BlockPos> trunkBlocksToChop = new LinkedHashSet<>();
	private final Set<BlockPos> blocksToPlant = new LinkedHashSet<>();
	private final Set<BlockPos> blocksToFertilize = new LinkedHashSet<>();

	private final IItemHandler inventoryForDrops;

	public WorkSiteTreeFarm(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.TREE_FARM_TILE.get(), pos, state);
		inventoryForDrops = new CombinedInvWrapper(plantableInventory, mainInventory);
	}

	@Override
	protected boolean isPlantable(ItemStack stack) {
		return TreeFarmRegistry.isPlantable(stack);
	}

	@Override
	protected boolean isMiscItem(ItemStack stack) {
		return stack.getItem() == Items.SHEARS || super.isMiscItem(stack);
	}

	@Override
	public void onBoundsAdjusted() {
		validateCollection(blocksToFertilize);
		validateCollection(trunkBlocksToChop);
		validateCollection(leafBlocksToChop);
		validateCollection(blocksToPlant);
		if (!hasShears) {
			blocksToShear.clear();
		}
		markDirty();
	}

	@Override
	protected void countResources() {
		super.countResources();
		hasShears = InventoryTools.getCountOf(miscInventory, s -> s.getItem() == Items.SHEARS) > 0;
	}

	private boolean bonemealBlock() {
		if (bonemealCount <= 0 || blocksToFertilize.isEmpty()) {
			return false;
		}

		Iterator<BlockPos> it = blocksToFertilize.iterator();
		BlockPos position = it.next();
		it.remove();

		BlockState state = level.getBlockState(position);

		return canFertilize(level, position, state) && fertilize(position);

	}

	private boolean plant() {
		if (plantableCount <= 0 || blocksToPlant.isEmpty()) {
			return false;
		}

		//noinspection ConstantConditions
		Optional<Tuple<ItemStack, ISapling>> plantable = InventoryTools.stream(plantableInventory).map(p -> new Tuple<>(p, TreeFarmRegistry.getSapling(p)))
				.filter(t -> t.getB().isPresent()).map(t -> new Tuple<>(t.getA(), t.getB().get())).findFirst();
		if (plantable.isPresent()) {
			Iterator<BlockPos> it = blocksToPlant.iterator();
			while (it.hasNext()) {
				BlockPos position = it.next();
				it.remove();

				ItemStack stack = plantable.get().getA();
				ISapling sapling = plantable.get().getB();
				if (canReplace(position) && tryPlantingSapling(position, stack, sapling)) {
					InventoryTools.removeItems(plantableInventory, stack, 1);
					return true;
				}
			}
		}

		return false;
	}

	private boolean tryPlantingSapling(BlockPos position, ItemStack stack, ISapling sapling) {
		return sapling.isRightClick() ? BlockTools.placeItemBlockRightClick(stack.copy(), level, position) :
				tryPlace(stack.copy(), position, Direction.UP) || tryPlace(stack.copy(), position, Direction.DOWN);
	}

	private boolean chopBlock(boolean wood) {
		if ((wood && trunkBlocksToChop.isEmpty()) || (!wood && leafBlocksToChop.isEmpty())) {
			return false;
		}

		Iterator<BlockPos> it = wood ? trunkBlocksToChop.iterator() : leafBlocksToChop.iterator();
		BlockPos position = it.next();
		BlockState state = level.getBlockState(position);
		if (state.isAir()) {
			it.remove();
			return false;
		}

		IBlockExtraDrop extraDrop = TreeFarmRegistry.getBlockExtraDrop(state);
		NonNullList<ItemStack> extraDrops = extraDrop.getDrops(level, position, state, getFortune());
		if (!harvestBlock(position)) {
			return false;
		}
		it.remove();
		InventoryTools.insertOrDropItems(inventoryForDrops, extraDrops, level, position);
		return true;
	}

	private boolean shearBlock() {
		if (!hasShears || blocksToShear.isEmpty()) {
			return false;
		}

		Iterator<BlockPos> it = blocksToShear.iterator();
		BlockPos position = it.next();
		it.remove();
		Block block = level.getBlockState(position).getBlock();
		if (block instanceof IShearable) {
			Optional<ItemStack> shears = InventoryTools.stream(miscInventory).filter(s -> s.getItem() instanceof ShearsItem).findFirst();

			if (shears.isPresent() && shear(position, (IShearable) block, shears.get())) {
				return true;
			}
		}

		return false;
	}

	private boolean shear(BlockPos position, IShearable block, ItemStack shears) {
		if (block.isShearable(null, shears, level, position)) {
			List<ItemStack> drops = block.onSheared(null, shears, level, position);
			drops = InventoryTools.insertItems(plantableInventory, drops, false);
			InventoryTools.insertOrDropItems(mainInventory, drops, level, worldPosition);
			level.removeBlock(position, false);
			return true;
		}
		return false;
	}

	private void addTreeBlocks(BlockState state, BlockPos basePos) {
		level.getProfiler().push("TreeFinder");

		ITree tree = TreeFarmRegistry.getTreeScanner(state).scanTree(level, basePos,
				basePos.offset(-MAX_DISTANCE_TO_INITIAL_CUT_POSITION, 0, -MAX_DISTANCE_TO_INITIAL_CUT_POSITION),
				basePos.offset(MAX_DISTANCE_TO_INITIAL_CUT_POSITION, 0, MAX_DISTANCE_TO_INITIAL_CUT_POSITION));
		List<BlockPos> leafBlocks = tree.getLeafPositions();
		if (hasShears) {
			blocksToShear.addAll(leafBlocks);
		} else {
			leafBlocksToChop.addAll(leafBlocks);
		}
		List<BlockPos> trunkBlocks = tree.getTrunkPositions();
		trunkBlocksToChop.addAll(trunkBlocks);

		if (!leafBlocks.isEmpty() || !trunkBlocks.isEmpty()) {
			markDirty();
		}
		level.getProfiler().pop();
	}

	@Override
	public WorkType getWorkType() {
		return WorkType.FORESTRY;
	}

	@Override
	public boolean onBlockClicked(Player player, @Nullable InteractionHand hand) {
		if (!player.level().isClientSide) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_WORKSITE_TREE_FARM, worldPosition);
		}
		return true;
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		if (!trunkBlocksToChop.isEmpty()) {
			ListTag chopList = new ListTag();
			for (BlockPos position : trunkBlocksToChop) {
				chopList.add(LongTag.valueOf(position.asLong()));
			}
			tag.put(TARGET_LIST_TAG, chopList);
		}
		if (!leafBlocksToChop.isEmpty()) {
			ListTag chopList = new ListTag();
			for (BlockPos position : leafBlocksToChop) {
				chopList.add(LongTag.valueOf(position.asLong()));
			}
			tag.put(TARGET_LEAF_LIST_TAG, chopList);
		}
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		trunkBlocksToChop.clear();
		if (tag.contains(TARGET_LIST_TAG)) {
			ListTag chopList = tag.getList(TARGET_LIST_TAG, Tag.TAG_LONG);
			for (int i = 0; i < chopList.size(); i++) {
				trunkBlocksToChop.add(BlockPos.of(((LongTag) chopList.get(i)).getAsLong()));
			}
		}
		if (tag.contains(TARGET_LEAF_LIST_TAG)) {
			ListTag chopList = tag.getList(TARGET_LEAF_LIST_TAG, Tag.TAG_LONG);
			for (int i = 0; i < chopList.size(); i++) {
				leafBlocksToChop.add(BlockPos.of(((LongTag) chopList.get(i)).getAsLong()));
			}
		}
	}

	@Override
	protected void scanBlockPosition(BlockPos scanPos) {
		if (canReplace(scanPos)) {
			BlockState state = level.getBlockState(scanPos.below());
			if (TreeFarmRegistry.isSoil(state) || (state.canBeReplaced() && TreeFarmRegistry
					.isSoil(level.getBlockState(scanPos.above())))) {
				blocksToPlant.add(scanPos);
			}
		} else {
			BlockState state = level.getBlockState(scanPos);
			if (canFertilize(level, scanPos, state)) {
				blocksToFertilize.add(scanPos);
			} else if (!state.isAir() && trunkBlocksToChop.isEmpty() && leafBlocksToChop.isEmpty()) {
				addTreeBlocks(state, scanPos);
			}
		}
	}

	private boolean canFertilize(Level level, BlockPos pos, BlockState state) {
		return state.getBlock() instanceof BonemealableBlock && ((BonemealableBlock) state.getBlock()).isValidBonemealTarget(level, pos, state);
	}

	private static final IWorksiteAction SHEAR_ACTION = WorksiteImplementation::getEnergyPerActivation;
	private static final IWorksiteAction CHOP_TRUNK_ACTION = e -> WorksiteImplementation.getEnergyPerActivation(e) / 2D;
	private static final IWorksiteAction CHOP_LEAF_ACTION = e -> WorksiteImplementation.getEnergyPerActivation(e) / 10D;
	private static final IWorksiteAction PLANT_ACTION = WorksiteImplementation::getEnergyPerActivation;
	private static final IWorksiteAction BONEMEAL_ACTION = WorksiteImplementation::getEnergyPerActivation;

	@Override
	protected Optional<IWorksiteAction> getNextAction() {
		if (hasShears && !blocksToShear.isEmpty()) {
			return Optional.of(SHEAR_ACTION);
		} else if (!leafBlocksToChop.isEmpty()) {
			return Optional.of(CHOP_LEAF_ACTION);
		} else if (!trunkBlocksToChop.isEmpty()) {
			return Optional.of(CHOP_TRUNK_ACTION);
		} else if (plantableCount > 0 && !blocksToPlant.isEmpty()) {
			return Optional.of(PLANT_ACTION);
		} else if (bonemealCount > 0 && !blocksToFertilize.isEmpty()) {
			return Optional.of(BONEMEAL_ACTION);
		}
		return Optional.empty();
	}

	@Override
	protected boolean processAction(IWorksiteAction action) {
		if (action == SHEAR_ACTION) {
			return shearBlock();
		} else if (action == CHOP_TRUNK_ACTION) {
			return chopBlock(true);
		} else if (action == CHOP_LEAF_ACTION) {
			return chopBlock(false);
		} else if (action == BONEMEAL_ACTION) {
			return bonemealBlock();
		} else if (action == PLANT_ACTION) {
			return plant();
		}
		return false;
	}
}
