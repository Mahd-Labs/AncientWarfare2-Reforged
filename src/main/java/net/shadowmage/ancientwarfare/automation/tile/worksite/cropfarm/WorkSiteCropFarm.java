package net.shadowmage.ancientwarfare.automation.tile.worksite.cropfarm;

import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.item.BoneMealItem;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.automation.registry.CropFarmRegistry;
import net.shadowmage.ancientwarfare.automation.tile.worksite.IWorksiteAction;
import net.shadowmage.ancientwarfare.automation.tile.worksite.TileWorksiteFarm;
import net.shadowmage.ancientwarfare.core.entity.AWFakePlayer;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class WorkSiteCropFarm extends TileWorksiteFarm {
	private final Set<BlockPos> blocksToTill = new LinkedHashSet<>();
	private final Set<BlockPos> blocksToHarvest = new LinkedHashSet<>();
	private final Set<BlockPos> blocksToPlant = new LinkedHashSet<>();
	private final Set<BlockPos> blocksToFertilize = new LinkedHashSet<>();

	private final IItemHandler inventoryForDrops;

	public WorkSiteCropFarm(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.CROP_FARM_TILE.get(), pos, state);
		inventoryForDrops = new CombinedInvWrapper(plantableInventory, mainInventory);
	}

	@Override
	protected boolean isPlantable(ItemStack stack) {
		return CropFarmRegistry.getCrop(stack).isPlantable(stack);
	}

	@Override
	protected boolean isFarmable(Block block, BlockPos farmablePos) {
		//1.12: IPlantable with plant type Crop (crops and stems), or BlockCrops / BlockStem
		//port: also blocks in minecraft:crops (other mods' crops that do not extend the vanilla classes; 1.12 IPlantable)
		return block instanceof CropBlock || block instanceof StemBlock || block instanceof AttachedStemBlock
				|| block.defaultBlockState().is(net.minecraft.tags.BlockTags.CROPS);
	}

	@Override
	public void onBoundsAdjusted() {
		validateCollection(blocksToFertilize);
		validateCollection(blocksToHarvest);
		validateCollection(blocksToPlant);
		validateCollection(blocksToTill);
	}

	@Override
	protected void scanBlockPosition(BlockPos position) {
		BlockState state = level.getBlockState(position);
		Block block = level.getBlockState(position).getBlock();
		if (state.canBeReplaced()) {
			BlockState stateDown = level.getBlockState(position.below());
			if (CropFarmRegistry.isTillable(stateDown)) {
				blocksToTill.add(position.below());
			} else if (CropFarmRegistry.isSoil(stateDown)) {
				blocksToPlant.add(position);
			}
		}

		if (state.isAir()) {
			return;
		}

		ICrop crop = CropFarmRegistry.getCrop(state);
		blocksToHarvest.addAll(crop.getPositionsToHarvest(level, position, state));

		if (crop.canBeFertilized(state, level, position)) {
			blocksToFertilize.add(position);
		}
	}

	@Override
	public WorkType getWorkType() {
		return WorkType.FARMING;
	}

	@Override
	public boolean onBlockClicked(Player player, @Nullable InteractionHand hand) {
		if (!player.level().isClientSide) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_WORKSITE_CROP_FARM, worldPosition);
		}
		return true;
	}

	private boolean hasToPlant() {
		return (plantableCount > 0 && !blocksToPlant.isEmpty());
	}

	private boolean hasToFertilize() {
		return (bonemealCount > 0 && !blocksToFertilize.isEmpty());
	}

	private static final IWorksiteAction PLANT_ACTION = e -> WorksiteImplementation.getEnergyPerActivation(e) / 5D;
	private static final IWorksiteAction FERTILIZE_ACTION = e -> WorksiteImplementation.getEnergyPerActivation(e) / 5D;
	private static final IWorksiteAction TILL_ACTION = e -> WorksiteImplementation.getEnergyPerActivation(e) / 5D;
	private static final IWorksiteAction HARVEST_ACTION = e -> WorksiteImplementation.getEnergyPerActivation(e) / 5D;

	@Override
	protected Optional<IWorksiteAction> getNextAction() {
		if (!blocksToHarvest.isEmpty()) {
			return Optional.of(HARVEST_ACTION);
		} else if (hasToFertilize()) {
			return Optional.of(FERTILIZE_ACTION);
		} else if (hasToPlant()) {
			return Optional.of(PLANT_ACTION);
		} else if (!blocksToTill.isEmpty()) {
			return Optional.of(TILL_ACTION);
		}

		return Optional.empty();
	}

	@Override
	protected boolean processAction(IWorksiteAction action) {
		if (action == TILL_ACTION) {
			return tryTill();
		} else if (action == HARVEST_ACTION) {
			return tryHarvest();
		} else if (action == PLANT_ACTION) {
			return tryPlant();
		} else if (action == FERTILIZE_ACTION) {
			return tryFertilize();
		}
		return false;
	}

	private boolean tryFertilize() {
		Iterator<BlockPos> it = blocksToFertilize.iterator();
		BlockPos position;
		while (it.hasNext() && (position = it.next()) != null) {
			it.remove();
			BlockState state = level.getBlockState(position);
			Block block = state.getBlock();
			if (block instanceof BonemealableBlock) {
				for (int slot = 0; slot < miscInventory.getSlots(); slot++) {
					ItemStack stack = miscInventory.getStackInSlot(slot);
					if (stack.isEmpty()) {
						continue;
					}
					if (isBonemeal(stack)) {
						ItemStack clone = stack.copy();
						if (BoneMealItem.applyBonemeal(clone, level, position, AWFakePlayer.get(level))) {
							miscInventory.extractItem(slot, 1, false);
							level.levelEvent(1505, position, 15);
						}
						block = level.getBlockState(position).getBlock();
						if (block instanceof BonemealableBlock) {
							if (((BonemealableBlock) block).isValidBonemealTarget(level, position, state)) {
								blocksToFertilize.add(position);
							} else if (isFarmable(block, position)) {
								blocksToHarvest.add(position);
							}
						}
						return true;
					}
				}
				return false;
			}
		}
		return false;
	}

	private boolean tryPlant() {
		Iterator<BlockPos> it = blocksToPlant.iterator();
		BlockPos position;
		while (it.hasNext() && (position = it.next()) != null) {
			it.remove();
			if (canReplace(position)) {
				for (int slot = 0; slot < plantableInventory.getSlots(); slot++) {
					ItemStack stack = plantableInventory.getStackInSlot(slot);
					if (stack.isEmpty()) {
						continue;
					}
					if (isPlantable(stack)) {
						ItemStack clone = stack.copy();
						if (tryPlace(clone, position, Direction.UP)) {
							plantableInventory.extractItem(slot, 1, false);
							return true;
						}
					}
				}
				return false;
			}
		}
		return false;
	}

	private boolean tryHarvest() {
		Iterator<BlockPos> it = blocksToHarvest.iterator();
		BlockPos position;
		if (it.hasNext() && (position = it.next()) != null) {
			it.remove();
			BlockState state = level.getBlockState(position);
			ICrop crop = CropFarmRegistry.getCrop(state);
			return crop.harvest(level, state, position, getFortune(), inventoryForDrops);
		}
		return false;
	}

	private boolean tryTill() {
		Iterator<BlockPos> it = blocksToTill.iterator();
		BlockPos position;
		while (it.hasNext() && (position = it.next()) != null) {
			it.remove();
			BlockState state = level.getBlockState(position);
			if (CropFarmRegistry.isTillable(state) && canReplace(position.above())) {
				level.setBlockAndUpdate(position, CropFarmRegistry.getTilledState(state));
				level.playSound(null, worldPosition, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
				return true;
			}
		}
		return false;
	}
}
