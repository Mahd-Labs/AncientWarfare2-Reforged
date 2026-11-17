package net.shadowmage.ancientwarfare.npc.tile;

import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.shadowmage.ancientwarfare.core.interfaces.IInteractableTile;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.tile.IBlockBreakHandler;
import net.shadowmage.ancientwarfare.core.tile.TileOwned;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;
import net.shadowmage.ancientwarfare.npc.container.ContainerTownHall;
import net.shadowmage.ancientwarfare.npc.entity.NpcPlayerOwned;
import net.shadowmage.ancientwarfare.npc.item.ItemNpcSpawner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TileTownHall extends TileOwned implements IInteractableTile, ITickableTile, IBlockBreakHandler {

	public boolean alarmActive = false;

	public String name = "Unnamed"; //TODO see if naming town halls is needed for anything
	private int broadcastRange = AWNPCStatics.townMaxRange;
	private int updateDelayTicks = 0;
	private boolean isActive = true;
	private boolean isNeglected = false;

	private String oldOwner = null;

	private final List<NpcDeathEntry> deathNotices = new ArrayList<>();

	private final ItemStackHandler inventory = new ItemStackHandler(27) {
		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
		}
	};

	private final List<ContainerTownHall> viewers = new ArrayList<>();

	public TileTownHall(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		super(net.shadowmage.ancientwarfare.npc.init.AWNPCBlocks.TOWN_HALL_TILE.get(), pos, state);
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	@Override
	public void update() {
		if (level == null || level.isClientSide)
			return;

		updateDelayTicks--;
		if (updateDelayTicks <= 0 && isActive) {
			broadcast();
			updateDelayTicks = AWNPCStatics.townUpdateFreq;
		}
	}

	public void addViewer(ContainerTownHall viewer) {
		if (!viewers.contains(viewer)) {
			viewers.add(viewer);
		}
	}

	public void removeViewer(ContainerTownHall viewer) {
		while (viewers.contains(viewer)) {
			viewers.remove(viewer);
		}
	}

	private void broadcast() {
		AABB bb = new AABB(worldPosition.getX() - broadcastRange, worldPosition.getY() - broadcastRange / 2, worldPosition.getZ() - broadcastRange, worldPosition.getX() + broadcastRange + 1, worldPosition.getY() + broadcastRange / 2 + 1, worldPosition.getZ() + broadcastRange + 1);
		List<NpcPlayerOwned> entities = level.getEntitiesOfClass(NpcPlayerOwned.class, bb);
		if (entities.size() > 0) {
			for (Entity entity : entities) {
				if (((NpcPlayerOwned) entity).hasCommandPermissions(getOwner())) {
					((NpcPlayerOwned) entity).handleTownHallBroadcast(worldPosition);
				}
			}
		}
	}

	public void clearDeathNotices() {
		deathNotices.clear();
		informViewers();
	}

	public void informViewers() {
		for (ContainerTownHall cth : viewers) {
			cth.onTownHallDeathListUpdated();
		}
	}

	public void handleNpcDeath(NpcPlayerOwned npc, DamageSource source) {
		boolean canRes = true;//TODO set canRes  based on distance from town-hall?
		NpcDeathEntry entry = new NpcDeathEntry(npc, source, canRes);
		deathNotices.add(entry);
		informViewers();
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		NBTHelper.deserializeHandler(inventory, tag.getCompound("inventory"));
		ListTag entryList = tag.getList("deathNotices", Tag.TAG_COMPOUND);
		NpcDeathEntry entry;
		for (int i = 0; i < entryList.size(); i++) {
			entry = new NpcDeathEntry(entryList.getCompound(i));
			deathNotices.add(entry);
		}
		if (tag.contains("name"))
			name = tag.getString("name");
		if (tag.contains("range"))
			setRange(tag.getInt("range"));
		if (tag.contains("alarmActive"))
			alarmActive = (tag.getBoolean("alarmActive"));
		if (tag.contains("isActive"))
			isActive = (tag.getBoolean("isActive"));
		if (tag.contains("isNeglected"))
			isNeglected = (tag.getBoolean("isNeglected"));
		if (tag.contains("oldOwner"))
			oldOwner = (tag.getString("oldOwner"));
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		tag.put("inventory", NBTHelper.serializeHandler(inventory));
		ListTag entryList = new ListTag();
		for (NpcDeathEntry entry : deathNotices) {
			entryList.add(entry.writeToNBT(new CompoundTag()));
		}
		tag.put("deathNotices", entryList);
		tag.putString("name", name);
		tag.putInt("range", broadcastRange);
		tag.putBoolean("alarmActive", alarmActive);
		tag.putBoolean("isActive", isActive);
		tag.putBoolean("isNeglected", isNeglected);
		if (oldOwner != null)
			tag.putString("oldOwner", oldOwner);
	}

	@Override
	public void onBlockBroken(BlockState state) {
		InventoryTools.dropItemsInWorld(level, inventory, worldPosition);
	}

	public static class NpcDeathEntry {
		public ItemStack stackToSpawn;
		public String npcType;
		public String npcName;
		public String deathCause;
		public boolean resurrected;
		public boolean canRes;
		public boolean beingResurrected;
		public int[] pos;

		public NpcDeathEntry(CompoundTag tag) {
			readFromNBT(tag);
		}

		public NpcDeathEntry(NpcPlayerOwned npc, DamageSource source, boolean canRes) {
			this.stackToSpawn = ItemNpcSpawner.getSpawnerItemForNpc(npc);
			this.npcType = npc.getNpcFullType();
			this.npcName = npc.hasCustomName() ? npc.getCustomName().getString() : "";
			this.deathCause = source.getMsgId();
			this.canRes = canRes;
			this.pos = new int[] {Mth.floor(npc.getX()), Mth.floor(npc.getY()), Mth.floor(npc.getZ())};
		}

		public final void readFromNBT(CompoundTag tag) {
			stackToSpawn = NBTHelper.readItemStack(tag.getCompound("spawnerStack"));
			npcType = tag.getString("npcType");
			npcName = tag.getString("npcName");
			deathCause = tag.getString("deathCause");
			resurrected = tag.getBoolean("resurrected");
			canRes = tag.getBoolean("canRes");
			pos = tag.getIntArray("pos");
		}

		public CompoundTag writeToNBT(CompoundTag tag) {
			tag.put("spawnerStack", NBTHelper.writeItemStack(stackToSpawn, new CompoundTag()));
			tag.putString("npcType", npcType);
			tag.putString("npcName", npcName);
			tag.putString("deathCause", deathCause);
			tag.putBoolean("resurrected", resurrected);
			tag.putBoolean("canRes", canRes);
			tag.putIntArray("pos", pos);
			return tag;
		}
	}

	@Override
	public boolean onBlockClicked(Player player, InteractionHand hand) {
		if (!player.level().isClientSide && isOwner(player)) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_NPC_TOWN_HALL, worldPosition);
		}
		return true;
	}

	public List<NpcDeathEntry> getDeathList() {
		return deathNotices;
	}

	public int getRange() {
		return broadcastRange;
	}

	public void setRange(int val) {
		if (val < AWNPCStatics.townMaxRange) {
			broadcastRange = val;
		} else {
			broadcastRange = AWNPCStatics.townMaxRange;
		}
	}

}
