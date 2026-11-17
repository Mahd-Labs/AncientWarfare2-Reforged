package net.shadowmage.ancientwarfare.automation.tile.worksite;

import net.minecraft.world.item.BlockItem;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.RelativeSide;
import net.shadowmage.ancientwarfare.core.entity.AWFakePlayer;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.EntityTools;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

public abstract class TileWorksiteFarm extends TileWorksiteBoundedInventory {
	private static final int PLANTABLE_INVENTORY_SIZE = 3;
	private static final int SIZE = 16;
	public static final int DEFAULT_MISC_INVENTORY_SIZE = 3;
	private byte[] targetMap = new byte[SIZE * SIZE];
	private final Queue<BlockPos> blocksToUpdate = new LinkedList<>();
	public final ItemStackHandler plantableInventory;
	public final ItemStackHandler miscInventory;
	protected int plantableCount;
	protected int bonemealCount;

	/*
	 * flag should be set to true whenever updating inventory internally (e.g. harvesting blocks) to prevent
	 * unnecessary inventory rescanning.  should be set back to false after blocks are added to inventory
	 */
	private boolean shouldCountResources;

	public TileWorksiteFarm(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		this.shouldCountResources = true;
		plantableInventory = new ItemStackHandler(PLANTABLE_INVENTORY_SIZE) {
			@Override
			protected void onContentsChanged(int slot) {
				shouldCountResources = true;
			}

			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				return isPlantable(stack) ? super.insertItem(slot, stack, simulate) : stack;
			}
		};
		miscInventory = new ItemStackHandler(getMiscInventorySize()) {
			@Override
			protected void onContentsChanged(int slot) {
				shouldCountResources = true;
			}

			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				return isMiscItem(stack) ? super.insertItem(slot, stack, simulate) : stack;
			}
		};
		setSideInventory(RelativeSide.FRONT, plantableInventory, RelativeSide.FRONT);
		setSideInventory(RelativeSide.BOTTOM, miscInventory, RelativeSide.TOP);
	}

	protected boolean isMiscItem(ItemStack stack) {
		return isBonemeal(stack);
	}

	protected int getMiscInventorySize() {
		return DEFAULT_MISC_INVENTORY_SIZE;
	}

	protected abstract boolean isPlantable(ItemStack stack);

	protected abstract void scanBlockPosition(BlockPos pos);

	@Override
	protected final void updateWorksite() {
		level.getProfiler().push("Incremental Scan");
		if (blocksToUpdate.isEmpty() && hasWorkBounds()) {
			fillBlocksToProcess(blocksToUpdate);
		}
		if (!blocksToUpdate.isEmpty()) {
			BlockPos pos = blocksToUpdate.poll();
			scanBlockPosition(pos);
		}
		level.getProfiler().pop();
		updateBlockWorksite();
	}

	@Override
	public void onBlockBroken(BlockState state) {
		super.onBlockBroken(state);
		InventoryTools.dropItemsInWorld(level, plantableInventory, worldPosition);
		InventoryTools.dropItemsInWorld(level, miscInventory, worldPosition);
	}

	protected boolean harvestBlock(BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		if (state.isAir()) {
			return false;
		}

		NonNullList<ItemStack> stacks = NonNullList.create();

		WorksiteDrops.getDrops(stacks, level, pos, state, getFortune());

		if (!inventoryCanHold(stacks)) {
			return false;
		}

		if (!BlockTools.breakBlockNoDrops(level, pos, state)) {
			return false;
		}

		insertOrDropCrops(pos, stacks);
		return true;
	}

	private boolean inventoryCanHold(NonNullList<ItemStack> stacks) {
		List<ItemStack> remainingStacks = InventoryTools.insertItems(plantableInventory, stacks, true);
		remainingStacks = InventoryTools.insertItems(mainInventory, remainingStacks, true);
		return remainingStacks.isEmpty();
	}

	private void insertOrDropCrops(BlockPos pos, NonNullList<ItemStack> stacks) {
		List<ItemStack> remainingItems = InventoryTools.insertItems(plantableInventory, stacks, false);
		InventoryTools.insertOrDropItems(mainInventory, remainingItems, level, pos);
	}

	@Override
	public final boolean userAdjustableBlocks() {
		return true;
	}

	boolean isTarget(BlockPos p) {
		return isTarget(p.getX(), p.getZ());
	}

	private boolean isTarget(int x1, int y1) {
		int z = (y1 - getWorkBoundsMin().getZ()) * SIZE + x1 - getWorkBoundsMin().getX();
		return z >= 0 && z < targetMap.length && targetMap[z] == 1;
	}

	protected boolean isBonemeal(ItemStack stack) {
		return stack.getItem() == Items.BONE_MEAL;
	}

	boolean isFarmable(Block block) {
		return isFarmable(block, new BlockPos(0, 0, 0));
	}

	protected boolean isFarmable(Block block, BlockPos farmablePos) {
		return isPlantableBlock(block);
	}

	/*
	 * Port addition: 1.12 "instanceof IPlantable" (BlockBush and sub classes, reeds, cactus). IPlantable is gone.
	 */
	public static boolean isPlantableBlock(Block block) {
		//1.21 has no IPlantable, which other mods' crops and saplings implemented in 1.12; a block that does not extend
		//one of the vanilla classes is recognised through the vanilla tags those mods put it in (version-caused fix)
		return block instanceof BushBlock || block instanceof SugarCaneBlock || block instanceof CactusBlock
				|| block.defaultBlockState().is(net.minecraft.tags.BlockTags.CROPS) || block.defaultBlockState().is(net.minecraft.tags.BlockTags.SAPLINGS);
	}

	/*
	 * Port addition: 1.12 "item instanceof IPlantable" (seed items); seeds are block items of their crop block now.
	 */
	public static boolean isPlantableItem(ItemStack stack) {
		return stack.getItem() instanceof BlockItem && (isPlantableBlock(((BlockItem) stack.getItem()).getBlock()) || isSeedItem(stack));
	}

	/* a block item tagged as seed or sapling (c:seeds, minecraft:villager_plantable_seeds, minecraft:saplings) */
	public static boolean isSeedItem(ItemStack stack) {
		return stack.is(net.neoforged.neoforge.common.Tags.Items.SEEDS) || stack.is(net.minecraft.tags.ItemTags.VILLAGER_PLANTABLE_SEEDS)
				|| stack.is(net.minecraft.tags.ItemTags.SAPLINGS);
	}

	protected boolean canReplace(BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.canBeReplaced();
	}

	protected boolean tryPlace(ItemStack stack, BlockPos pos, Direction face) {
		return BlockTools.placeItemBlock(stack, level, pos, face);
	}

	private void pickupItems() {
		List<ItemEntity> items = EntityTools.getEntitiesWithinBounds(level, ItemEntity.class, getWorkBoundsMin(), getWorkBoundsMax());
		if (items.isEmpty()) { return; }
		ItemStack stack;
		for (ItemEntity item : items) {
			if (item.isAlive()) {
				stack = item.getItem();
				if (!stack.isEmpty()) {
					stack = InventoryTools.mergeItemStack(plantableInventory, stack);
					stack = InventoryTools.mergeItemStack(mainInventory, stack);
					if (!stack.isEmpty()) {
						item.setItem(stack);
					} else {
						item.discard();
					}
				}
			}
		}
	}

	@Override
	protected void validateCollection(Collection<BlockPos> blocks) {
		if (!hasWorkBounds()) {
			blocks.clear();
			return;
		}
		Iterator<BlockPos> it = blocks.iterator();
		BlockPos pos;
		while (it.hasNext() && (pos = it.next()) != null) {
			if (!isInBounds(pos) || !isTarget(pos)) {
				it.remove();
			}
		}
	}

	private void fillBlocksToProcess(Collection<BlockPos> targets) {
		BlockPos min = getWorkBoundsMin();
		BlockPos max = getWorkBoundsMax();
		for (int x = min.getX(); x < max.getX() + 1; x++) {
			for (int z = min.getZ(); z < max.getZ() + 1; z++) {
				if (isTarget(x, z)) {
					targets.add(new BlockPos(x, min.getY(), z));
				}
			}
		}
	}

	//TODO implement to check target blocks, clear invalid ones
	public void onTargetsAdjusted() {
		onBoundsAdjusted();
	}

	@Override
	protected void onBoundsSet() {
		for (int x = 0; x < SIZE; x++) {
			for (int z = 0; z < SIZE; z++) {
				targetMap[z * SIZE + x] = (byte) 1;
			}
		}
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		tag.putByteArray("targetMap", targetMap);
		tag.put("plantableInventory", NBTHelper.serializeHandler(plantableInventory));
		tag.put("miscInventory", NBTHelper.serializeHandler(miscInventory));
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		if (tag.contains("targetMap")) {
			targetMap = tag.getByteArray("targetMap");
		}
		NBTHelper.deserializeHandler(plantableInventory, tag.getCompound("plantableInventory"));
		NBTHelper.deserializeHandler(miscInventory, tag.getCompound("miscInventory"));
	}

	public byte[] getTargetMap() {
		return targetMap;
	}

	public void setTargetBlocks(byte[] targets) {
		boolean change = !Objects.deepEquals(targetMap, targets);
		targetMap = targets;
		if (change) {
			onTargetsAdjusted();
			markDirty();
		}
	}

	private void updateBlockWorksite() {
		level.getProfiler().push("Items Pickup");
		if (level.getDayTime() % 20 == 0) {
			pickupItems();
		}
		level.getProfiler().popPush("Count Resources");
		if (shouldCountResources) {
			countResources();
			shouldCountResources = false;
		}
		level.getProfiler().pop();
	}

	protected void countResources() {
		plantableCount = InventoryTools.getCountOf(plantableInventory, this::isPlantable);
		bonemealCount = InventoryTools.getCountOf(miscInventory, this::isBonemeal);
	}

	protected boolean fertilize(BlockPos pos) {
		Optional<ItemStack> stack = InventoryTools.stream(miscInventory).filter(this::isBonemeal).findFirst();
		if (stack.isPresent() && BoneMealItem.applyBonemeal(stack.get(), level, pos, AWFakePlayer.get(level))) {
			level.levelEvent(1505, pos, 15);
			return true;
		}
		return false;
	}
}
