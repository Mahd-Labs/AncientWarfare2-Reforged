package net.shadowmage.ancientwarfare.automation.tile.torque;

import net.minecraft.core.BlockPos;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.automation.tile.ISidedItemHandlerTile;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.shadowmage.ancientwarfare.automation.config.AWAutomationStatics;
import net.shadowmage.ancientwarfare.core.interfaces.ITorque.TorqueCell;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.tile.IBlockBreakHandler;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import javax.annotation.Nullable;

public class TileStirlingGenerator extends TileTorqueSingleCell implements IBlockBreakHandler, ISidedItemHandlerTile {

	private final ItemStackHandler fuelHandler = new ItemStackHandler(1) {
		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return stack.getBurnTime(null) > 0 ? super.insertItem(slot, stack, simulate) : stack;
		}
	};

	private int burnTime = 0;
	private int burnTimeBase = 0;

	public TileStirlingGenerator(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.STIRLING_GENERATOR_TILE.get(), pos, state);
		torqueCell = new TorqueCell(0, 4, 1600, AWAutomationStatics.med_efficiency_factor);
	}

	@Override
	public void update() {
		super.update();
		if (!level.isClientSide) {
			if (burnTime <= 0 && torqueCell.getEnergy() < torqueCell.getMaxEnergy()) {
				//if fueled, consume one, set burn-ticks to fuel value
				int ticks = fuelHandler.getStackInSlot(0).getBurnTime(null);
				if (ticks > 0) {
					fuelHandler.extractItem(0, 1, false);
					burnTime = ticks;
					burnTimeBase = ticks;
				}
			} else if (burnTime > 0) {
				torqueCell.setEnergy(torqueCell.getEnergy() + AWAutomationStatics.stirling_generator_output);
				burnTime--;
			}
		}
	}

	@Override
	public boolean onBlockClicked(Player player, @Nullable InteractionHand hand) {
		if (!player.level().isClientSide) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_STIRLING_GENERATOR, worldPosition);
		}
		return true;
	}

	public int getBurnTime() {
		return burnTime;
	}

	public int getBurnTimeBase() {
		return burnTimeBase;
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		burnTime = tag.getInt("burnTicks");
		burnTimeBase = tag.getInt("burnTicksBase");
		if (tag.contains("inventory")) {
			NBTHelper.deserializeHandler(fuelHandler, tag.getCompound("inventory"));
		}
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		tag.putInt("burnTicks", burnTime);
		tag.putInt("burnTicksBase", burnTimeBase);
		tag.put("inventory", NBTHelper.serializeHandler(fuelHandler));
	}

	@Override
	public boolean canInputTorque(Direction from) {
		return false;
	}

	@Nullable
	@Override
	public IItemHandler getItemHandler(@Nullable Direction facing) {
		return fuelHandler;
	}

	@Override
	public void onBlockBroken(BlockState state) {
		InventoryTools.dropItemsInWorld(level, fuelHandler, worldPosition);
	}
}
