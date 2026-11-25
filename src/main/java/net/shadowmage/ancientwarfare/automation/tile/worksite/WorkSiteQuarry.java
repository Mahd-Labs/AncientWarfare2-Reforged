package net.shadowmage.ancientwarfare.automation.tile.worksite;

import net.minecraft.tags.BlockTags;
import net.shadowmage.ancientwarfare.automation.chunkloader.AWChunkLoader;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.upgrade.WorksiteUpgrade;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public final class WorkSiteQuarry extends TileWorksiteBoundedInventory {
	private boolean finished;
	private boolean hasDoneInit = false;
	public int height;

	/*
	 * Current position within work bounds.
	 * Incremented when work is processed.
	 */
	private BlockPos current = BlockPos.ZERO;//position within bounds that is the 'active' position
	private BlockPos validate = BlockPos.ZERO;

	public WorkSiteQuarry(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.QUARRY_TILE.get(), pos, state);
	}

	@Override
	public boolean userAdjustableBlocks() {
		return false;
	}

	@Override
	protected void onBoundsSet() {
		super.onBoundsSet();
		offsetBounds();
	}

	private void offsetBounds() {
		BlockPos boundsMax = getWorkBoundsMax();
		setWorkBoundsMax(boundsMax.above(worldPosition.getY() - 1 - boundsMax.getY()));
		boundsMax = getWorkBoundsMin();
		setWorkBoundsMin(boundsMax.above(worldPosition.getY() - height - boundsMax.getY()));
		BlockTools.notifyBlockUpdate(this);
	}

	@Override
	public void onBoundsAdjusted() {
		offsetBounds();
		current = new BlockPos(getWorkBoundsMin().getX(), getWorkBoundsMax().getY(), getWorkBoundsMin().getZ());
		validate = current;
	}

	@Override
	public Set<WorksiteUpgrade> getValidUpgrades() {
		return EnumSet.of(WorksiteUpgrade.ENCHANTED_TOOLS_1, WorksiteUpgrade.ENCHANTED_TOOLS_2, WorksiteUpgrade.QUARRY_MEDIUM, WorksiteUpgrade.QUARRY_LARGE, WorksiteUpgrade.TOOL_QUALITY_1, WorksiteUpgrade.TOOL_QUALITY_2, WorksiteUpgrade.TOOL_QUALITY_3, WorksiteUpgrade.QUARRY_CHUNK_LOADER);
	}

	@Override
	public int getBoundsMaxWidth() {
		if (getUpgrades().contains(WorksiteUpgrade.QUARRY_LARGE)) {
			return 64;
		}
		return getUpgrades().contains(WorksiteUpgrade.QUARRY_MEDIUM) ? 32 : 16;
	}

	@Override
	protected void updateWorksite() {
		if (!hasDoneInit) {
			initWorkSite();
			hasDoneInit = true;
		}
	}

	@Override
	public void addUpgrade(WorksiteUpgrade upgrade) {
		super.addUpgrade(upgrade);
		current = new BlockPos(getWorkBoundsMin().getX(), getWorkBoundsMax().getY(), getWorkBoundsMin().getZ());
		validate = current;
		this.finished = false;
	}

	private static final IWorksiteAction DIG_ACTION = WorksiteImplementation::getEnergyPerActivation;

	@Override
	protected Optional<IWorksiteAction> getNextAction() {
		return !finished ? Optional.of(DIG_ACTION) : Optional.empty();
	}

	@Override
	protected boolean processAction(IWorksiteAction action) {
		if (!hasDoneInit) {
			initWorkSite();
			hasDoneInit = true;
		}
		/*
		 * while the current position is invalid, increment to a valid one. generally the incremental scan
		 * should have take care of this prior to processWork being called, but just in case...
		 */
		//canHarvest and harvestBlock call the block's own code; a block that throws is skipped (the guard answers
		//false for it until its skip time is over), so the quarry moves past it instead of taking the server down
		while (!foreignBlocks.run(level, current, "checking whether it can be dug", () -> canHarvest(current))) {
			if (!incrementPosition()) {
				/*
				 * if no valid position was found, set finished, exit
				 */
				finished = true;
				return false;
			}
		}
		/*
		 * if made it this far, a valid position was found, break it and add blocks to inventory
		 */
		BlockPos digPos = current;
		return foreignBlocks.run(level, digPos, "digging", () -> harvestBlock(digPos));
	}

	private boolean harvestBlock(BlockPos current) {
		BlockState state = level.getBlockState(current);
		Block block = state.getBlock();
		NonNullList<ItemStack> stacks = NonNullList.create();

		WorksiteDrops.getDrops(stacks, level, current, state, getFortune());

		if (!InventoryTools.insertItems(mainInventory, stacks, true).isEmpty()) {
			return false;
		}

		if (!BlockTools.breakBlockNoDrops(level, current, state)) {
			return false;
		}

		InventoryTools.insertOrDropItems(mainInventory, stacks, level, current);

		return true;
	}

	private boolean incrementPosition() {
		if (finished) {
			return false;
		}
		if (isMaxInChunk(current.getX()) || current.getX() >= getWorkBoundsMax().getX()) {
			int startX = Math.max(getWorkBoundsMin().getX(), getMinChunkX());
			if (isMaxInChunk(current.getZ()) || current.getZ() >= getWorkBoundsMax().getZ()) {
				if (current.getY() <= (worldPosition.getY() - (height))) {
					return moveToStartOfNextChunk();
				} else {
					int startZ = Math.max(getWorkBoundsMin().getZ(), getMinChunkZ());
					current = new BlockPos(startX, current.getY() - 1, startZ);
				}
			} else {
				current = new BlockPos(startX, current.getY(), current.getZ() + 1);
			}
		} else {
			current = current.east();
		}
		return true;
	}

	private int getMinChunkZ() {
		return (current.getZ() >> 4) * 16;
	}

	private boolean moveToStartOfNextChunk() {
		unforceNonMachineChunk();

		int x = getMinChunkX() + 16;
		int z = Math.max(getWorkBoundsMin().getZ(), getMinChunkZ());
		if (x > getWorkBoundsMax().getX()) {
			x = getWorkBoundsMin().getX();
			z = getMinChunkZ() + 16;
			if (z > getWorkBoundsMax().getZ()) {
				return false;
			}
		}
		current = new BlockPos(x, getWorkBoundsMax().getY(), z);

		chunkLoadWorkBounds();

		return true;
	}

	private void unforceNonMachineChunk() {
		if(!hasChunkLoaderUpgrade()) {
			return;
		}

		ChunkPos currentChunk = new ChunkPos(current);
		//unload chunk only if not same as machine chunk
		if (!currentChunk.equals(new ChunkPos(worldPosition))) {
			AWChunkLoader.unforceChunk(chunkTicket, new ChunkPos(current));
		}
	}

	@Override
	protected void chunkLoadWorkBounds() {
		if(!hasChunkLoaderUpgrade()) {
			return;
		}

		AWChunkLoader.forceChunk(chunkTicket, new ChunkPos(current));
	}

	private int getMinChunkX() {
		return (current.getX() >> 4) * 16;
	}

	private boolean isMaxInChunk(int coord) {
		return (coord & 15) == 15;
	}

	private boolean canHarvest(BlockPos harvestPos) {
		BlockState state = level.getBlockState(harvestPos);
		Block block = state.getBlock();
		if (level.isEmptyBlock(harvestPos) || state.liquid()) {
			return false;
		}
		//1.12 Block.getHarvestLevel: harvest levels are block tags now
		int harvestLevel = state.is(BlockTags.NEEDS_DIAMOND_TOOL) ? 3 : state.is(BlockTags.NEEDS_IRON_TOOL) ? 2 : state.is(BlockTags.NEEDS_STONE_TOOL) ? 1 : 0;
		if (harvestLevel >= 2) {
			int toolLevel = 1;
			if (getUpgrades().contains(WorksiteUpgrade.TOOL_QUALITY_3)) {
				toolLevel = Integer.MAX_VALUE;
			} else if (getUpgrades().contains(WorksiteUpgrade.TOOL_QUALITY_2)) {
				toolLevel = 3;
			} else if (getUpgrades().contains(WorksiteUpgrade.TOOL_QUALITY_1)) {
				toolLevel = 2;
			}
			if (toolLevel < harvestLevel) {
				return false;
			}//else is harvestable, check the rest of the checks
		}
		return state.getDestroySpeed(level, harvestPos) >= 0;
	}

	private void initWorkSite() {
		//Port note: 1.12 "height = y - 1" (dig down to y = 1); the world bottom is not 0 any more
		height = worldPosition.getY() - 1 - (level == null ? 0 : level.getMinBuildHeight());
		BlockPos boundsMin = getWorkBoundsMin();
		setWorkBoundsMin(boundsMin.above(worldPosition.getY() - height - boundsMin.getY()));
		current = new BlockPos(getWorkBoundsMin().getX(), getWorkBoundsMax().getY(), getWorkBoundsMin().getZ());
		validate = current;
		BlockTools.notifyBlockUpdate(this);//resend work-bounds change
	}

	@Override
	public WorkType getWorkType() {
		return WorkType.MINING;
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		current = BlockPos.of(tag.getLong("current"));
		validate = BlockPos.of(tag.getLong("validate"));
		finished = tag.getBoolean("finished");
		hasDoneInit = tag.getBoolean("init");
		height = tag.getInt("height");
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		tag.putLong("current", current.asLong());
		tag.putLong("validate", validate.asLong());
		tag.putBoolean("finished", finished);
		tag.putBoolean("init", hasDoneInit);
		tag.putInt("height", height);
	}

	@Override
	public boolean onBlockClicked(Player player, @Nullable InteractionHand hand) {
		if (!player.level().isClientSide) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_WORKSITE_QUARRY, worldPosition);
		}
		return true;
	}
}
