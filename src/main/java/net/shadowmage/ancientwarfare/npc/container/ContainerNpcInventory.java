package net.shadowmage.ancientwarfare.npc.container;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.inventory.NpcEquipmentHandler;
import net.shadowmage.ancientwarfare.npc.skin.NpcSkinSettings;

public class ContainerNpcInventory extends ContainerNpcBase<NpcBase> implements ISkinSettingsContainer {
	public boolean doNotPursue; //if the npc should not pursue targets away from its position/route
	public boolean isArcher = entity.getNpcSubType().equals("archer");
	public final int guiHeight;
	private String name;
	public NpcSkinSettings skinSettings;
	//improvement (not baseline, config improvements.npc_level_editor): shown read-only; -1 = not shown (1.12 screen)
	public int level = -1;
	public int maxLevel;

	public ContainerNpcInventory(Player player, int x, int y, int z) {
		super(player, x);
		NpcEquipmentHandler inventory = new NpcEquipmentHandler(entity);
		addSlotToContainer(new SlotItemHandler(inventory, 0, 8, 8)); //weapon slot
		addSlotToContainer(new SlotItemHandler(inventory, 1, 8, 8 + 18));//shield slot
		addSlotToContainer(new SlotItemHandler(inventory, 2, 8, 8 + 18 * 5));//boots
		addSlotToContainer(new SlotItemHandler(inventory, 3, 8, 8 + 18 * 4));//legs
		addSlotToContainer(new SlotItemHandler(inventory, 4, 8, 8 + 18 * 3));//chest
		addSlotToContainer(new SlotItemHandler(inventory, 5, 8, 8 + 18 * 2));//helm
		addSlotToContainer(new SlotItemHandler(inventory, 6, 8 + 18 * 2, 8 + 18 * 3));//work/combat/route orders slot
		addSlotToContainer(new SlotItemHandler(inventory, 7, 8 + 18 * 2, 8 + 18 * 2));//upkeep orders slot

		guiHeight = addPlayerSlots(8 + 5 * 18 + 8 + 18) + 8;

		name = entity.hasCustomName() ? entity.getCustomName().getString() : ""; //1.12 getCustomNameTag() returned "" for unnamed npcs
		doNotPursue = entity.getDoNotPursue();
		skinSettings = entity.getSkinSettings();
	}

	public void sendChangesToServer() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("donotpursue", doNotPursue);
		sendDataToServer(tag);
	}

	public void sendInitData() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("donotpursue", doNotPursue);
		if (net.shadowmage.ancientwarfare.npc.config.AWNPCStatics.improvedNpcLevelEditor) {
			tag.putInt("level", entity.getLevelingStats().getLevel());
			tag.putInt("maxLevel", entity.getLevelingStats().getMaxLevel());
		}
		sendDataToClient(tag);
	}

	@Override
	public void handlePacketData(CompoundTag tag) {
		if (tag.contains("customName")) {
			this.name = tag.getString("customName");
		}
		if (player.level().isClientSide && tag.contains("level")) {
			level = tag.getInt("level");
			maxLevel = tag.getInt("maxLevel");
		}
		if (entity != null && !entity.isRemoved()) {
			if (tag.contains("repack")) {
				entity.repackEntity(player);
			} else if (tag.contains("setHome")) {
				entity.setHomeAreaAtCurrentPosition();
			} else if (tag.contains("clearHome")) {
				entity.clearRestriction();
			} else if (tag.contains("togglefollow")) {
				if (entity.getFollowingEntity() != null && entity.getFollowingEntity().getName().equals(player.getName())) {
					entity.clearFollowingEntity();
				} else { entity.setFollowingEntity(player); }
			}
			if (tag.contains("donotpursue")) {
				doNotPursue = tag.getBoolean("donotpursue");
			}
			if (tag.contains("skinSettings")) {
				skinSettings = NpcSkinSettings.deserializeNBT(tag.getCompound("skinSettings")).minimizeData();
				entity.setSkinSettings(skinSettings);
			}
		}
		refreshGui();
	}

	public void handleNpcNameUpdate(String newName) {
		name = newName;
	}

	@Override
	public void handleNpcSkinUpdate() {
		CompoundTag tag = new CompoundTag();
		tag.put("skinSettings", skinSettings.serializeNBT());
		sendDataToServer(tag);
	}

	@Override
	public void onContainerClosed(Player p_75134_1_) {
		super.onContainerClosed(p_75134_1_);
		entity.setCustomName(Component.literal(name));
		entity.setSkinSettings(skinSettings);
		entity.setDoNotPursue(doNotPursue);
	}

	public void setName() {
		CompoundTag tag = new CompoundTag();
		tag.putString("customName", name);
		sendDataToServer(tag);
	}

	@Override
	public NpcSkinSettings getSkinSettings() {
		return skinSettings;
	}

	@Override
	public void setSkinSettings(NpcSkinSettings skinSettings) {
		this.skinSettings = skinSettings;
	}

	@Override
	public ItemStack transferStackInSlot(Player player, int slotClickedIndex) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = slots.get(slotClickedIndex);

		if (slot != null && slot.hasItem()) {
			ItemStack slotStack = slot.getItem();
			itemstack = slotStack.copy();
			EquipmentSlot entityequipmentslot = player.getEquipmentSlotForItem(itemstack);

			if (slotClickedIndex < 8) {
				if (!moveItemStackTo(slotStack, 8, 44, true)) {
					return ItemStack.EMPTY;
				}
			} else if (entityequipmentslot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !slots.get(2 + entityequipmentslot.getIndex()).hasItem()) {
				int i = 2 + entityequipmentslot.getIndex();

				if (!moveItemStackTo(slotStack, i, i + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (entityequipmentslot == EquipmentSlot.MAINHAND && !slots.get(0).hasItem() && !slots.get(1).hasItem()) {
				if (!moveItemStackTo(slotStack, 0, 2, false)) {
					return ItemStack.EMPTY;
				}
			} else if (entityequipmentslot == EquipmentSlot.OFFHAND && !slots.get(1).hasItem()) {
				if (!moveItemStackTo(slotStack, 1, 2, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!getSlot(6).hasItem() && getSlot(6).mayPlace(slotStack)) {
				if (!moveItemStackTo(slotStack, 6, 7, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!getSlot(7).hasItem() && getSlot(7).mayPlace(slotStack)) {
				if (!moveItemStackTo(slotStack, 7, 8, false)) {
					return ItemStack.EMPTY;
				}
			} else if (slotClickedIndex < 17) {
				if (!moveItemStackTo(slotStack, 17, 44, false)) {
					return ItemStack.EMPTY;
				}
			} else if (slotClickedIndex < 44) {
				if (!moveItemStackTo(slotStack, 8, 17, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!moveItemStackTo(slotStack, 8, 44, false)) {
				return ItemStack.EMPTY;
			}

			if (slotStack.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			if (slotStack.getCount() == itemstack.getCount()) {
				return ItemStack.EMPTY;
			}

			slot.onTake(player, slotStack);
				ItemStack itemstack2 = slotStack;

			if (slotClickedIndex == 0) {
				player.drop(itemstack2, false);
			}
		}
		return itemstack;
	}
}
