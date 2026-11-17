package net.shadowmage.ancientwarfare.structure.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.util.EntityTools;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.structure.util.LootHelper;

import javax.annotation.Nullable;

/*
 * Port notes: 1.12 TileEntityChest subclass -> ChestBlockEntity subclass with the mod's block entity type.
 * readFromNBT / writeToNBT -> loadAdditional / saveAdditional. The update packet / update tag carry only the loot
 * settings like 1.12 (the client never needs the content: getItem returns the raw list on the client so the lazy
 * loot table fill of the vanilla chest is never triggered there).
 */
public class TileAdvancedLootChest extends ChestBlockEntity implements ISpecialLootContainer {
	private static final String LOOT_SETTINGS_TAG = "lootSettings";
	private LootSettings lootSettings = new LootSettings();

	public TileAdvancedLootChest(BlockPos pos, BlockState state) {
		this(AWStructureBlocks.ADVANCED_LOOT_CHEST_TILE.get(), pos, state);
	}

	protected TileAdvancedLootChest(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public boolean fillWithLootAndCheckIfGoodToOpen(@Nullable Player player) {
		return LootHelper.fillWithLootAndCheckIfGoodToOpen(this, player != null ? player : EntityTools.findClosestPlayer(level, worldPosition, 100));
	}

	@Override
	protected void loadAdditional(CompoundTag compound, HolderLookup.Provider registries) {
		super.loadAdditional(compound, registries);
		if (compound.contains(LOOT_SETTINGS_TAG)) {
			lootSettings = LootSettings.deserializeNBT(compound.getCompound(LOOT_SETTINGS_TAG));
		}
	}

	@Override
	protected void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {
		super.saveAdditional(compound, registries);
		compound.put(LOOT_SETTINGS_TAG, lootSettings.serializeNBT());
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		tag.put(LOOT_SETTINGS_TAG, lootSettings.serializeNBT());
		return tag;
	}

	@Override
	public ItemStack getItem(int index) {
		if (level == null || level.isClientSide) {
			return this.getItems().get(index);
		}
		return super.getItem(index);
	}

	/* 1.12 TileEntity.getRenderBoundingBox, used by the renderer */
	public AABB getRenderBoundingBox() {
		return new AABB(Vec3.atLowerCornerOf(worldPosition.offset(-1, 0, -1)), Vec3.atLowerCornerOf(worldPosition.offset(2, 2, 2)));
	}

	@Override
	public void setLootSettings(LootSettings settings) {
		this.lootSettings = settings;
	}

	@Override
	public LootSettings getLootSettings() {
		return lootSettings;
	}
}
