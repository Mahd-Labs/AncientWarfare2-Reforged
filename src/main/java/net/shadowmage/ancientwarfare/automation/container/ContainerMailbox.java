package net.shadowmage.ancientwarfare.automation.container;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import com.google.common.collect.Lists;
import net.shadowmage.ancientwarfare.automation.gamedata.MailboxData;
import net.shadowmage.ancientwarfare.automation.tile.TileMailbox;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.RelativeSide;
import net.shadowmage.ancientwarfare.core.container.ContainerTileBase;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ContainerMailbox extends ContainerTileBase<TileMailbox> {

	public int guiHeight;
	/*
	 * synched stats
	 */
	public String targetName;
	public String mailboxName;
	public boolean autoExport;
	public boolean privateBox;
	public List<String> publicBoxNames = Lists.newArrayList();
	public List<String> privateBoxNames = Lists.newArrayList();
	public List<Direction> sendSides = Lists.newArrayList();
	public List<Direction> receivedSides = Lists.newArrayList();

	public ContainerMailbox(Player player, int x, int y, int z) {
		super(player, x, y, z);

		int xPos, yPos, x1, y1;
		for (int i = 0; i < 18; i++) {
			x1 = i % 9;
			y1 = i / 9;
			xPos = x1 * 18 + 8;
			yPos = y1 * 18 + 8 + 12;
			addSlotToContainer(new SlotItemHandler(tileEntity.receivedInventory, i, xPos, yPos) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}
			});
		}

		for (int i = 0; i < 18; i++) {
			x1 = i % 9;
			y1 = i / 9;
			xPos = x1 * 18 + 8;
			yPos = y1 * 18 + 8 + 12 + 2 * 18 + 12;
			addSlotToContainer(new SlotItemHandler(tileEntity.sendInventory, i, xPos, yPos));
		}

		y1 = 8 + 12 + 12 + 4 * 18;
		guiHeight = addPlayerSlots(y1 + 12) + 8 + 24;

		if (!player.level().isClientSide) {
			MailboxData data = AWGameData.INSTANCE.getData(player.level(), MailboxData.class);
			publicBoxNames.addAll(data.getPublicBoxNames());
			privateBoxNames.addAll(data.getPrivateBoxNames(tileEntity.getOwner().getName()));
			privateBox = tileEntity.isPrivateBox();
			autoExport = tileEntity.isAutoExport();
			mailboxName = tileEntity.getMailboxName();
			targetName = tileEntity.getTargetName();
			sendSides = tileEntity.sendSides;
			receivedSides = tileEntity.receivedSides;
		}
	}

	@Override
	public void sendInitData() {
		CompoundTag tag = new CompoundTag();
		tag.putIntArray("sendSides", tileEntity.sendSides.stream().mapToInt(Enum::ordinal).toArray());
		tag.putIntArray("receivedSides", tileEntity.receivedSides.stream().mapToInt(Enum::ordinal).toArray());
		if (mailboxName != null) {
			tag.putString("mailboxName", mailboxName);
		}
		if (targetName != null) {
			tag.putString("targetName", targetName);
		}

		tag.putBoolean("privateBox", privateBox);
		tag.putBoolean("autoExport", autoExport);

		ListTag nameList = new ListTag();
		for (String boxName : publicBoxNames) {
			nameList.add(StringTag.valueOf(boxName));
		}
		tag.put("publicBoxNames", nameList);

		nameList = new ListTag();
		for (String boxName : privateBoxNames) {
			nameList.add(StringTag.valueOf(boxName));
		}
		tag.put("privateBoxNames", nameList);
		sendDataToClient(tag);
	}

	@Override
	public void handlePacketData(CompoundTag tag) {
		handleAccessChange(tag);
		if (tag.contains("sendSides")) {
			int[] sides = tag.getIntArray("sendSides");
			tileEntity.sendSides = Arrays.stream(sides).mapToObj(o -> Direction.values()[o]).collect(Collectors.toList());
			sendSides = tileEntity.sendSides;
			tileEntity.onSidesChanged();
		}
		if (tag.contains("receivedSides")) {
			int[] sides = tag.getIntArray("receivedSides");
			tileEntity.receivedSides = Arrays.stream(sides).mapToObj(o -> Direction.values()[o]).collect(Collectors.toList());
			receivedSides = tileEntity.receivedSides;
			tileEntity.onSidesChanged();
		}
		if (tag.contains("autoExport")) {
			autoExport = tag.getBoolean("autoExport");
			tileEntity.setAutoExport(autoExport);
		}
		if (tag.contains("privateBox")) {
			privateBox = tag.getBoolean("privateBox");
			tileEntity.setPrivateBox(privateBox);
		}
		if (tag.contains("clearMailbox")) {
			mailboxName = null;
			tileEntity.setMailboxName(null);
		} else if (tag.contains("mailboxName")) {
			mailboxName = tag.getString("mailboxName");
			tileEntity.setMailboxName(mailboxName);
		}
		if (tag.contains("clearTarget")) {
			targetName = null;
			tileEntity.setTargetName(null);
		} else if (tag.contains("targetName")) {
			targetName = tag.getString("targetName");
			tileEntity.setTargetName(targetName);
		}
		if (tag.contains("publicBoxNames")) {
			publicBoxNames.clear();
			ListTag nameList = tag.getList("publicBoxNames", Tag.TAG_STRING);
			for (int i = 0; i < nameList.size(); i++) {
				publicBoxNames.add(nameList.getString(i));
			}
		}
		if (tag.contains("privateBoxNames")) {
			privateBoxNames.clear();
			ListTag nameList = tag.getList("privateBoxNames", Tag.TAG_STRING);
			for (int i = 0; i < nameList.size(); i++) {
				privateBoxNames.add(nameList.getString(i));
			}
		}
		if (tag.contains("addMailbox") || tag.contains("deleteMailbox")) {
			MailboxData data = AWGameData.INSTANCE.getData(player.level(), MailboxData.class);
			String name = tag.getString("addMailbox");
			if (!name.isEmpty())
				data.addMailbox(tileEntity.isPrivateBox() ? tileEntity.getOwner().getName() : null, name);
			name = tag.getString("deleteMailbox");
			if (!name.isEmpty())
				data.deleteMailbox(tileEntity.isPrivateBox() ? tileEntity.getOwner().getName() : null, name);
		}
		refreshGui();
	}

	private void handleAccessChange(CompoundTag tag) {
		if (tag.contains("accessChange")) {
			CompoundTag slotTag = tag.getCompound("accessChange");
			RelativeSide base = RelativeSide.values()[slotTag.getInt("baseSide")];
			RelativeSide access = RelativeSide.values()[slotTag.getInt("accessSide")];

			updateSides(base, access);
		}
	}

	public void updateSides(RelativeSide base, RelativeSide access) {
		Direction facing = RelativeSide.getMCSideToAccess(BlockRotationHandler.RotationType.FOUR_WAY, tileEntity.getPrimaryFacing(), base);
		List<Direction> removeFrom;
		List<Direction> addTo;
		if (access == RelativeSide.TOP || access == RelativeSide.BOTTOM) {
			if (access == RelativeSide.TOP) {
				removeFrom = tileEntity.sendSides;
				addTo = tileEntity.receivedSides;
			} else {
				removeFrom = tileEntity.receivedSides;
				addTo = tileEntity.sendSides;
			}
			removeFrom.remove(facing);
			addTo.add(facing);

		} else {
			tileEntity.sendSides.remove(facing);
			tileEntity.receivedSides.remove(facing);
		}
		tileEntity.onSidesChanged();
		sendSides = tileEntity.sendSides;
		receivedSides = tileEntity.receivedSides;
	}

	public void sendSlotChange(RelativeSide base, RelativeSide access) {
		CompoundTag tag;
		CompoundTag slotTag;
		tag = new CompoundTag();
		slotTag = new CompoundTag();
		slotTag.putInt("baseSide", base.ordinal());
		slotTag.putInt("accessSide", access.ordinal());
		tag.put("accessChange", slotTag);
		sendDataToServer(tag);
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		CompoundTag tag = null;
		/*
		 * DETECT CHANGES TO NAME AND TARGET AND SEND TO CLIENT
         */
		if (!sendSides.equals(tileEntity.sendSides)) {
			if (tag == null) {
				tag = new CompoundTag();
			}
			sendSides = tileEntity.sendSides;
			tag.putIntArray("sendSides", sendSides.stream().mapToInt(Enum::ordinal).toArray());
		}
		if (!receivedSides.equals(tileEntity.receivedSides)) {
			receivedSides = tileEntity.receivedSides;
			tag.putIntArray("receivedSides", receivedSides.stream().mapToInt(Enum::ordinal).toArray());
		}
		String name = tileEntity.getMailboxName();
		if (!StringUtils.equals(name, mailboxName)) {
			if (tag == null) {
				tag = new CompoundTag();
			}
			mailboxName = tileEntity.getMailboxName();
			if (mailboxName == null) {
				tag.putBoolean("clearMailbox", true);
			} else {
				tag.putString("mailboxName", mailboxName);
			}
		}
		name = tileEntity.getTargetName();
		if (!StringUtils.equals(name, targetName)) {
			if (tag == null) {
				tag = new CompoundTag();
			}
			targetName = tileEntity.getTargetName();
			if (targetName == null) {
				tag.putBoolean("clearTarget", true);
			} else {
				tag.putString("targetName", targetName);
			}
		}
		/*
		 * detect changes to auto export and private box setting
         */
		if (autoExport != tileEntity.isAutoExport()) {
			if (tag == null) {
				tag = new CompoundTag();
			}
			autoExport = tileEntity.isAutoExport();
			tag.putBoolean("autoExport", autoExport);
		}
		if (privateBox != tileEntity.isPrivateBox()) {
			if (tag == null) {
				tag = new CompoundTag();
			}
			privateBox = tileEntity.isPrivateBox();
			tag.putBoolean("privateBox", privateBox);
		}
		/*
		 * detect changes to public or private names list
         */
		MailboxData data = AWGameData.INSTANCE.getData(player.level(), MailboxData.class);
		if (!publicBoxNames.equals(data.getPublicBoxNames())) {
			if (tag == null) {
				tag = new CompoundTag();
			}
			publicBoxNames.clear();
			publicBoxNames.addAll(data.getPublicBoxNames());
			ListTag nameList = new ListTag();
			for (String boxName : publicBoxNames) {
				nameList.add(StringTag.valueOf(boxName));
			}
			tag.put("publicBoxNames", nameList);
		}
		if (!privateBoxNames.equals(data.getPrivateBoxNames(tileEntity.getOwner().getName()))) {
			if (tag == null) {
				tag = new CompoundTag();
			}
			privateBoxNames.clear();
			privateBoxNames.addAll(data.getPrivateBoxNames(tileEntity.getOwner().getName()));
			ListTag nameList = new ListTag();
			for (String boxName : privateBoxNames) {
				nameList.add(StringTag.valueOf(boxName));
			}
			tag.put("privateBoxNames", nameList);
		}
		/*
		 * if tag is not null (something has changed), send it to client
         */
		if (tag != null) {
			sendDataToClient(tag);
		}
	}

	/*
	 * client-side input method
	 */
	public void handleNameAdd(String name) {
		CompoundTag tag = new CompoundTag();
		tag.putString("addMailbox", name);
		sendDataToServer(tag);
	}

	/*
	 * client-side input method
	 */
	public void handleNameDelete(String name) {
		CompoundTag tag = new CompoundTag();
		tag.putString("deleteMailbox", name);
		sendDataToServer(tag);
	}

	/*
	 * client-side input method
	 */
	public void handleNameSelection(String name) {
		CompoundTag tag = new CompoundTag();
		if (name == null) {
			tag.putBoolean("clearMailbox", true);
		} else {
			tag.putString("mailboxName", name);
		}
		mailboxName = name;
		sendDataToServer(tag);
	}

	/*
	 * client-side input method
	 */
	public void handleTargetSelection(String name) {
		CompoundTag tag = new CompoundTag();
		if (name == null) {
			tag.putBoolean("clearTarget", true);
		} else {
			tag.putString("targetName", name);
		}
		targetName = name;
		sendDataToServer(tag);
	}

	/*
	 * client-side input method
	 */
	public void handlePrivateBoxToggle(boolean newVal) {
		targetName = null;
		mailboxName = null;
		privateBox = newVal;
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("privateBox", privateBox);
		sendDataToServer(tag);
	}

	public void handleAutoExportToggle(boolean newVal) {
		autoExport = newVal;
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("autoExport", newVal);
		sendDataToServer(tag);
	}

	@Override
	public ItemStack transferStackInSlot(Player par1EntityPlayer, int slotClickedIndex) {
		ItemStack slotStackCopy = ItemStack.EMPTY;
		Slot theSlot = this.getSlot(slotClickedIndex);
		if (theSlot != null && theSlot.hasItem()) {
			ItemStack slotStack = theSlot.getItem();
			slotStackCopy = slotStack.copy();
			if (slotStack.getCount() == 0) {
				theSlot.set(ItemStack.EMPTY);
			} else {
				theSlot.setChanged();
			}
			if (slotStack.getCount() == slotStackCopy.getCount()) {
				return ItemStack.EMPTY;
			}
			theSlot.onTake(par1EntityPlayer, slotStack);
		}
		return slotStackCopy;
	}

}
