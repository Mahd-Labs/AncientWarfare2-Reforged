package net.shadowmage.ancientwarfare.automation.tile;

import net.neoforged.neoforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.automation.block.BlockMailbox;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.minecraft.core.BlockPos;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;

import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import com.google.common.collect.Lists;
import net.shadowmage.ancientwarfare.automation.gamedata.MailboxData;
import net.shadowmage.ancientwarfare.automation.gamedata.MailboxData.DeliverableItem;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableTile;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.render.property.CoreProperties;
import net.shadowmage.ancientwarfare.core.tile.IBlockBreakHandler;
import net.shadowmage.ancientwarfare.core.tile.TileOwned;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TileMailbox extends TileOwned implements ISidedItemHandlerTile, IRotatableTile, ITickableTile, IBlockBreakHandler {

	private boolean autoExport;//TODO : should automatically try and export from output side
	private boolean privateBox;

	public ItemStackHandler sendInventory = new ItemStackHandler(18);
	public List<Direction> sendSides = Lists.newArrayList();
	public ItemStackHandler receivedInventory = new ItemStackHandler(18);
	public List<Direction> receivedSides = Lists.newArrayList();

	private String mailboxName;
	private String destinationName;

	public TileMailbox(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.MAILBOX_TILE.get(), pos, state);
		sendSides.add(Direction.UP);
		receivedSides.add(Direction.DOWN);
	}

	@Override
	public void update() {
		if (!(level != null) || level.isClientSide) {
			return;
		}
		if (mailboxName != null)//try to receive mail
		{
			MailboxData data = AWGameData.INSTANCE.getData(level, MailboxData.class);

			List<DeliverableItem> items = new ArrayList<>();
			data.getDeliverableItems(privateBox ? getOwner().getName() : null, mailboxName, items, level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
			data.addMailboxReceiver(privateBox ? getOwner().getName() : null, mailboxName, this);

			if (destinationName != null)//try to send mail
			{
				trySendItems(data);
			}
		}
	}

	private void trySendItems(MailboxData data) {
		ItemStack item;
		String owner = privateBox ? getOwner().getName() : null;
		String dim = level.dimension().location().toString();
		for (int slot = 0; slot < sendInventory.getSlots(); slot++) {
			item = sendInventory.getStackInSlot(slot);
			if (!item.isEmpty()) {
				data.addDeliverableItem(owner, destinationName, sendInventory.extractItem(slot, item.getCount(), false), dim, worldPosition);
				break;
			}
		}
	}

	/*
	 * Port addition: the exposed item handler depends on the configured sides
	 */
	public void onSidesChanged() {
		if (level != null) {
			level.invalidateCapabilities(worldPosition);
		}
	}

	public String getMailboxName() {
		return mailboxName;
	}

	public String getTargetName() {
		return destinationName;
	}

	public void setMailboxName(String name) {
		if (level.isClientSide) {
			return;
		}
		mailboxName = name;
		markDirty();
	}

	public void setTargetName(String name) {
		if (level.isClientSide) {
			return;
		}
		destinationName = name;
		markDirty();
	}

	public boolean isAutoExport() {
		return autoExport;
	}

	public boolean isPrivateBox() {
		return privateBox;
	}

	public void setAutoExport(boolean val) {
		autoExport = val;
	}

	public void setPrivateBox(boolean val) {
		if (level.isClientSide) {
			return;
		}
		if (val != privateBox) {
			mailboxName = null;
			destinationName = null;
			markDirty();
		}
		privateBox = val;
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		if (tag.contains("targetName")) {
			destinationName = tag.getString("targetName");
		}
		if (tag.contains("mailboxName")) {
			mailboxName = tag.getString("mailboxName");
		}
		if (tag.contains("autoExport")) {
			autoExport = tag.getBoolean("autoExport");
		}
		if (tag.contains("privateBox")) {
			privateBox = tag.getBoolean("privateBox");
		}
		if (tag.contains("sendInventory")) {
			NBTHelper.deserializeHandler(sendInventory, tag.getCompound("sendInventory"));
		}
		if (tag.contains("receivedInventory")) {
			NBTHelper.deserializeHandler(receivedInventory, tag.getCompound("receivedInventory"));
		}
		//port: out-of-range side numbers (edited data) are skipped instead of throwing
		if (tag.contains("sendSides")) {
			int[] sides = tag.getIntArray("sendSides");
			sendSides = Arrays.stream(sides).filter(o -> o >= 0 && o < Direction.values().length).mapToObj(o -> Direction.values()[o]).collect(Collectors.toList());
		}
		if (tag.contains("receivedSides")) {
			int[] sides = tag.getIntArray("receivedSides");
			receivedSides = Arrays.stream(sides).filter(o -> o >= 0 && o < Direction.values().length).mapToObj(o -> Direction.values()[o]).collect(Collectors.toList());
		}
		//port: sides loaded into a block entity that is already in the world (/data merge, a structure placing its data)
		//change what pipes see, so the exposed handlers are refreshed as for a change in the screen
		if (tag.contains("sendSides") || tag.contains("receivedSides")) {
			onSidesChanged();
		}
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		if (destinationName != null) {
			tag.putString("targetName", destinationName);
		}
		if (mailboxName != null) {
			tag.putString("mailboxName", mailboxName);
		}
		tag.put("sendInventory", NBTHelper.serializeHandler(sendInventory));
		tag.put("receivedInventory", NBTHelper.serializeHandler(receivedInventory));
		tag.putIntArray("sendSides", sendSides.stream().mapToInt(Enum::ordinal).toArray());
		tag.putIntArray("receivedSides", receivedSides.stream().mapToInt(Enum::ordinal).toArray());
		tag.putBoolean("privateBox", privateBox);

	}

	@Override
	public Direction getPrimaryFacing() {
		return level.getBlockState(worldPosition).getValue(BlockMailbox.FACING);
	}

	@Override
	public void setPrimaryFacing(Direction face) {
		level.setBlockAndUpdate(worldPosition, level.getBlockState(worldPosition).setValue(BlockMailbox.FACING, face));
	}

	@Nullable
	@Override
	public IItemHandler getItemHandler(@Nullable Direction facing) {
			if (receivedSides.contains(facing)) {
				return receivedInventory;
			} else if (sendSides.contains(facing)) {
				return sendInventory;
			}
		return null;
	}

	@Override
	public void onBlockBroken(BlockState state) {
		InventoryTools.dropItemsInWorld(level, sendInventory, worldPosition);
		InventoryTools.dropItemsInWorld(level, receivedInventory, worldPosition);
	}
}
