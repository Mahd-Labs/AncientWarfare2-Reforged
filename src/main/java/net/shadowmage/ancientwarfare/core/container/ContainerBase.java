package net.shadowmage.ancientwarfare.core.container;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import net.shadowmage.ancientwarfare.core.interfaces.IContainerGuiCallback;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.PacketGui;

/*
 * Port notes: extends AbstractContainerMenu. Subclasses keep the 1.12 constructor shape (player, x, y, z) and are
 * created only through NetworkHandler (which supplies MenuType + window id via a thread local).
 * 1.12 names kept as thin aliases so ported subclasses stay close to the original:
 *   addSlotToContainer -> addSlot, inventorySlots -> slots, canInteractWith -> stillValid,
 *   transferStackInSlot -> quickMoveStack, onContainerClosed -> removed, detectAndSendChanges -> broadcastChanges.
 * Slot x/y are final in 1.21.1, so removeSlots()/addSlots() toggle a flag read by AWSlot-aware screens
 * (see slotsHidden) instead of moving slots off screen.
 */
public class ContainerBase extends AbstractContainerMenu {

	public final Player player;
	private IContainerGuiCallback gui;
	public int playerSlots = 0;
	private boolean slotsHidden = false;

	public ContainerBase(Player player) {
		this(player, NetworkHandler.consumePendingMenu());
	}

	/*
	 * 1.12 "new ContainerBase(player)" inside a client sub screen (structure scanner validation / biome / dimension /
	 * mod selection): a slotless container that only gives the screen something to hold. It is never opened, has no
	 * menu type and is not synced; the sub screen talks to the server through its parent screen container.
	 */
	public static ContainerBase createDetached(Player player) {
		return new ContainerBase(player, new NetworkHandler.PendingMenu(null, 0));
	}

	private ContainerBase(Player player, NetworkHandler.PendingMenu pending) {
		super(pending.type(), pending.windowId());
		this.player = player;
	}

	/*
	 * stands in on the client when the real container could not be built there (the block or entity it needs
	 * is not known to the client yet, a stale held item). 1.12 then simply opened nothing; 1.21 disconnects a client
	 * whose menu factory returns nothing. The screen for a placeholder closes itself at once.
	 */
	private boolean placeholder = false;

	public static ContainerBase createPlaceholder(Player player, net.minecraft.world.inventory.MenuType<?> type, int windowId) {
		ContainerBase container = new ContainerBase(player, new NetworkHandler.PendingMenu(type, windowId));
		container.placeholder = true;
		return container;
	}

	public final boolean isPlaceholder() {
		return placeholder;
	}

	/*
	 * set the gui for this container for callback (refreshGui) purposes
	 */
	public final void setGui(IContainerGuiCallback gui) {
		this.gui = gui;
	}

	protected Slot addSlotToContainer(Slot slot) {
		return addSlot(slot);
	}

	/*
	 * @param tx  the upper-left X coordinate of the 9x3 inventory block
	 * @param ty  the upper-left Y coordinate of the 9x3 inventory block
	 * @param gap the gap size between upper (9x3) and lower(9x1) inventory blocks, in pixels
	 */
	protected int addPlayerSlots(int tx, int ty, int gap) {
		int y;
		int x;
		int slotNum;
		int xPos;
		int yPos;
		IItemHandler playerInventory = new PlayerMainInvWrapper(player.getInventory());
		for (x = 0; x < 9; ++x)//add player hotbar slots
		{
			xPos = tx + x * 18;
			yPos = ty + gap + 3 * 18;
			addSlot(new SlotItemHandler(playerInventory, x, xPos, yPos));
		}
		for (y = 0; y < 3; ++y) {
			for (x = 0; x < 9; ++x) {
				slotNum = y * 9 + x + 9;// +9 is to increment past hotbar slots
				xPos = tx + x * 18;
				yPos = ty + y * 18;
				addSlot(new SlotItemHandler(playerInventory, slotNum, xPos, yPos));
			}
		}
		playerSlots = 36;
		return ty + (4 * 18) + gap;
	}

	protected int addPlayerSlots(int ty) {
		return addPlayerSlots(8, ty, 4);
	}

	protected int addPlayerSlots() {
		return addPlayerSlots(8, 240 - 4 - 8 - 4 * 18, 4);
	}

	/*
	 * server side method to send a data-packet to the client-side GUI attached to the client-side version of this container
	 */
	protected final void sendDataToGui(CompoundTag data) {
		if (!player.level().isClientSide) {
			PacketGui pkt = new PacketGui();
			pkt.setTag("gui", data);
			NetworkHandler.sendToPlayer((ServerPlayer) player, pkt);
		}
	}

	/*
	 * send data from client-container to server container
	 */
	protected void sendDataToServer(String tagName, Tag tag) {
		CompoundTag compoundTag = new CompoundTag();
		compoundTag.put(tagName, tag);
		sendDataToServer(compoundTag);
	}

	protected void sendDataToServer(CompoundTag data) {
		if (player.level().isClientSide) {
			PacketGui pkt = new PacketGui();
			pkt.setData(data);
			NetworkHandler.sendToServer(pkt);
		}
	}

	/*
	 * send data from server container to client container
	 */
	protected void sendDataToClient(CompoundTag data) {
		if (!player.level().isClientSide) {
			PacketGui pkt = new PacketGui();
			pkt.setData(data);
			NetworkHandler.sendToPlayer((ServerPlayer) player, pkt);
		}
	}

	/*
	 * client/server side method to receive packet data from PacketGui
	 */
	public final void onPacketData(CompoundTag data) {
		if (data.contains("gui")) {
			if (gui != null) {
				gui.handlePacketData(data.getCompound("gui"));
			}
		} else {
			handlePacketData(data);
		}
	}

	/*
	 * subclasses should override this method to send any data from server to the client-side container.
	 * This method is called immediately after the container has been constructed and set as the active container.
	 * The data is received client-side immediately after the GUI has been constructed, initialized, and opened.
	 */
	public void sendInitData() {

	}

	/*
	 * sub-classes should override this method to handle any packet data they are expecting to receive.
	 * packets destined to the GUI or for slot-click have already been filtered out
	 */
	public void handlePacketData(CompoundTag tag) {

	}

	public boolean canInteractWith(Player var1) {
		return true;
	}

	@Override
	public final boolean stillValid(Player player) {
		return canInteractWith(player);
	}

	/*
	 * Causes the GUI to be re-setup on its next update tick
	 */
	public void refreshGui() {
		if (gui != null) {
			gui.refreshGui();
		}
	}

	/*
	 * remove the inventory slots from view on the screen, effectively disabling them
	 */
	public void removeSlots() {
		slotsHidden = true;
	}

	/*
	 * add any removed from screen slots back into view
	 */
	public void addSlots() {
		slotsHidden = false;
	}

	/*
	 * true while removeSlots() is in effect; GuiContainerBase must neither render nor hit-test slots then
	 */
	public boolean areSlotsHidden() {
		return slotsHidden;
	}

	public ItemStack transferStackInSlot(Player player, int slotClickedIndex) {
		return ItemStack.EMPTY; //by default there's no shift click support, child containers need to implement this
	}

	@Override
	public final ItemStack quickMoveStack(Player player, int index) {
		return transferStackInSlot(player, index);
	}

	public void onContainerClosed(Player player) {
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		onContainerClosed(player);
	}

	public void detectAndSendChanges() {
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		detectAndSendChanges();
	}
}
