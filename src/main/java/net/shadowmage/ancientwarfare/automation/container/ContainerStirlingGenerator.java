package net.shadowmage.ancientwarfare.automation.container;

import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileStirlingGenerator;
import net.shadowmage.ancientwarfare.core.container.ContainerTileBase;

import javax.annotation.Nonnull;

public class ContainerStirlingGenerator extends ContainerTileBase<TileStirlingGenerator> {

	public int guiHeight;
	public double energy;
	public int burnTime;
	public int burnTimeBase;

	public ContainerStirlingGenerator(Player player, int x, int y, int z) {
		super(player, x, y, z);
		addSlotToContainer(new SlotItemHandler(tileEntity.getItemHandler(null), 0, 8 + 4 * 18, 8 + 12) {
			@Override
			public boolean mayPlace(ItemStack par1ItemStack) {
				return par1ItemStack.getBurnTime(null) > 0;
			}
		});
		guiHeight = addPlayerSlots(8 + 18 + 8 + 12 + 12) + 8;
	}

	/*
	 * Port note: 1.12 IContainerListener.sendWindowProperty / updateProgressBar -> ClientboundContainerSetDataPacket / setData
	 */
	private void sendWindowProperty(int id, int value) {
		if (player instanceof ServerPlayer) {
			((ServerPlayer) player).connection.send(new ClientboundContainerSetDataPacket(containerId, id, value));
		}
	}

	@Override
	public void setData(int par1, int par2) {
		if (par1 == 0) {
			energy = par2 * 0.001d * tileEntity.getMaxTorque(tileEntity.getPrimaryFacing());
			refreshGui();
		} else if (par1 == 1) {
			burnTime = par2;
			refreshGui();
		} else if (par1 == 2) {
			burnTimeBase = par2;
			refreshGui();
		}
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		double g = tileEntity.getTorqueStored(tileEntity.getPrimaryFacing());
		if (g != energy) {
			energy = g;
			int e = (int) (g * 1000.d / tileEntity.getMaxTorque(tileEntity.getPrimaryFacing()));
			sendWindowProperty(0, e);
		}
		int b = tileEntity.getBurnTime();
		if (b != burnTime) {
			burnTime = b;
			sendWindowProperty(1, b);
		}
		b = tileEntity.getBurnTimeBase();
		if (b != burnTimeBase) {
			burnTimeBase = b;
			sendWindowProperty(2, b);
		}
	}

	/*
	 * @return should always return null for normal implementation, not sure wtf the rest of the code is about
	 */
	@Override
	public ItemStack transferStackInSlot(Player par1EntityPlayer, int slotClickedIndex) {
		Slot slot = this.getSlot(slotClickedIndex);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		int slots = tileEntity.getItemHandler(null).getSlots();
		ItemStack stackFromSlot = slot.getItem();
		if (slotClickedIndex < slots)//click on input slot, merge into player inventory
		{
			this.moveItemStackTo(stackFromSlot, slots, slots + playerSlots, false);
		} else//click on player slot, attempt merge into te inventory
		{
			this.moveItemStackTo(stackFromSlot, 0, slots, false);
		}
		if (stackFromSlot.getCount() == 0) {
			slot.set(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return ItemStack.EMPTY;
	}

}
