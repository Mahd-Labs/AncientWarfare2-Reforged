package net.shadowmage.ancientwarfare.automation.tile.worksite;

import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class WorkSiteFishFarm extends TileWorksiteBoundedInventory {
	private static final int MAX_WATER = 1280;
	private boolean harvestFish = true;
	private boolean harvestInk = true;

	private int waterBlockCount = 0;
	private int waterRescanDelay = 0;

	public WorkSiteFishFarm(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.FISH_FARM_TILE.get(), pos, state);
	}

	private static final IWorksiteAction FISH_ACTION = WorksiteImplementation::getEnergyPerActivation;

	@Override
	protected Optional<IWorksiteAction> getNextAction() {
		return waterBlockCount > 0 ? Optional.of(FISH_ACTION) : Optional.empty();
	}

	@Override
	protected boolean processAction(IWorksiteAction action) {
		float percentOfMax = ((float) waterBlockCount) / MAX_WATER;
		float check = level.random.nextFloat();
		if (check <= percentOfMax) {
			boolean fish = harvestFish;
			boolean ink = harvestInk;
			if (fish && ink) {
				fish = level.random.nextBoolean();
				ink = !fish;
			}
			if (fish) {
				LootParams params = new LootParams.Builder((ServerLevel) level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition)).withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withLuck(getFortune()).create(LootContextParamSets.FISHING);
				List<ItemStack> fishStacks = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING).getRandomItems(params);

				if (!InventoryTools.canInventoryHold(mainInventory, fishStacks)) {
					return false;
				}

				for (ItemStack fishStack : fishStacks) {
					InventoryTools.insertOrDropItem(mainInventory, fishStack, level, worldPosition);
				}
				return true;
			}
			if (ink) {
				ItemStack inkItem = new ItemStack(Items.INK_SAC, 1);
				int fortune = getFortune();
				if (fortune > 0) {
					inkItem.grow(level.random.nextInt(fortune + 1));
				}
				if (!InventoryTools.canInventoryHold(mainInventory, inkItem)) {
					return false;
				}

				InventoryTools.insertOrDropItem(mainInventory, inkItem, level, worldPosition);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean userAdjustableBlocks() {
		return false;
	}

	@Override
	protected void onBoundsSet() {
		super.onBoundsSet();
		BlockPos boundsMax = getWorkBoundsMax();
		setWorkBoundsMax(boundsMax.above(worldPosition.getY() - 1 - boundsMax.getY()));
		boundsMax = getWorkBoundsMin();
		setWorkBoundsMin(boundsMax.above(worldPosition.getY() - 5 - boundsMax.getY()));
		BlockTools.notifyBlockUpdate(this);
	}

	@Override
	public void onBoundsAdjusted() {
		super.onBoundsAdjusted();
		BlockPos boundsMax = getWorkBoundsMax();
		setWorkBoundsMax(boundsMax.above(worldPosition.getY() - 1 - boundsMax.getY()));
		boundsMax = getWorkBoundsMin();
		setWorkBoundsMin(boundsMax.above(worldPosition.getY() - 5 - boundsMax.getY()));
		BlockTools.notifyBlockUpdate(this);
	}

	@Override
	public int getBoundsMaxHeight() {
		return 4;
	}

	public boolean harvestFish() {
		return harvestFish;
	}

	public boolean harvestInk() {
		return harvestInk;
	}

	public void setHarvest(boolean fish, boolean ink) {
		if (harvestFish != fish || harvestInk != ink) {
			harvestFish = fish;
			harvestInk = ink;
			markDirty();
		}
	}

	private void countWater() {
		waterBlockCount = 0;
		BlockPos min = getWorkBoundsMin();
		BlockPos max = getWorkBoundsMax();
		for (int x = min.getX(); x <= max.getX(); x++) {
			for (int z = min.getZ(); z <= max.getZ(); z++) {
				for (int y = max.getY(); y >= min.getY(); y--) {
					BlockState state = level.getBlockState(new BlockPos(x, y, z));
					if (state.is(Blocks.WATER)) {
						waterBlockCount++;
					} else {
						break;
					}
				}
			}
		}
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		harvestFish = tag.getBoolean("fish");
		harvestInk = tag.getBoolean("ink");
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		tag.putBoolean("fish", harvestFish);
		tag.putBoolean("ink", harvestInk);
	}

	@Override
	public WorkType getWorkType() {
		return WorkType.FARMING;
	}

	@Override
	public boolean onBlockClicked(Player player, @Nullable InteractionHand hand) {
		if (!player.level().isClientSide) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_WORKSITE_FISH_FARM, worldPosition);
		}
		return true;
	}

	@Override
	public void openAltGui(Player player) {
		NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_WORKSITE_FISH_CONTROL, worldPosition);
	}

	@Override
	protected void updateWorksite() {
		level.getProfiler().push("WaterCount");
		if (waterRescanDelay-- <= 0) {
			countWater();
			waterRescanDelay = 200;
		}
		level.getProfiler().pop();
	}

}
