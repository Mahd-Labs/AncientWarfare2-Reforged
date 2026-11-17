package net.shadowmage.ancientwarfare.structure.tile;

import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.mojang.authlib.GameProfile;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.owner.Owner;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureEntry;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureMap;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.structure.init.AWStructureSounds;
import net.shadowmage.ancientwarfare.structure.network.PacketStructureEntry;
import net.shadowmage.ancientwarfare.structure.util.ConquerHelper;

import javax.annotation.Nullable;
import java.util.Optional;

public class TileProtectionFlag extends TileFlag {
	private static final String PLAYER_PROFILE_TAG = "playerProfile";
	private static final String OWNER_TAG = "owner";
	private static final float UNBREAKABLE = -1F;
	private Owner owner = Owner.EMPTY;
	private GameProfile playerProfile;

	public TileProtectionFlag(BlockPos pos, BlockState state) {
		this(AWStructureBlocks.PROTECTION_FLAG_TILE.get(), pos, state);
	}

	protected TileProtectionFlag(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected void readNBT(CompoundTag tag) {
		super.readNBT(tag);
		if (tag.contains(OWNER_TAG)) {
			owner = Owner.deserializeFromNBT(tag.getCompound(OWNER_TAG));
			playerProfile = net.shadowmage.ancientwarfare.structure.util.ProfileHelper.readGameProfile(tag.getCompound(PLAYER_PROFILE_TAG));
		}
	}

	@Override
	protected CompoundTag writeNBT(CompoundTag tag) {
		super.writeNBT(tag);
		if (owner != Owner.EMPTY) {
			tag.put(OWNER_TAG, owner.serializeToNBT(new CompoundTag()));
			tag.put(PLAYER_PROFILE_TAG, net.shadowmage.ancientwarfare.structure.util.ProfileHelper.writeGameProfile(playerProfile));
		}
		return tag;
	}

	@Override
	public void clearRemoved() {
		super.clearRemoved();
		StructureMap.get(level).getStructureAt(level, worldPosition).ifPresent(structure -> {
			if (!structure.getProtectionFlagPos().equals(worldPosition)) {
				structure.setProtectionFlagPos(worldPosition);
				if (!level.isClientSide) {
					NetworkHandler.sendToAllPlayers(new PacketStructureEntry(StructureMap.dim(level), structure.getChunkX(), structure.getChunkZ(), structure));
				}
			}
		});
	}

	@Override
	public ItemStack getItemStack() {
		ItemStack stack = new ItemStack(AWStructureBlocks.PROTECTION_FLAG.get());
		CompoundTag tag = new CompoundTag();
		writeNBT(tag);
		ItemTools.setTag(stack, tag);
		return stack;
	}

	public void onActivatedBy(Player player) {
		if (isPlayerOwned() || level.isClientSide) {
			return;
		}

		Optional<StructureEntry> structure = StructureMap.get(level).getStructureAt(level, worldPosition);
		if (!structure.isPresent()) {
			return;
		}

		StructureEntry st = structure.get();
		if (ConquerHelper.checkBBConquered(player, st.getBB())) {
			turnOffSoundBlocks(st);
			setOwner(player, player.getGameProfile());
			st.setConquered();
			player.displayClientMessage(Component.translatable("gui.ancientwarfarestructure.structure_conquered", st.getName()), true);
			level.playSound(null, worldPosition, AWStructureSounds.PROTECTION_FLAG_CLAIM.get(), SoundSource.BLOCKS, 1, 1);
		}
		setChanged();
		BlockTools.notifyBlockUpdate(this);
	}

	private void turnOffSoundBlocks(StructureEntry structure) {
		for (BlockPos blockPos : BlockPos.betweenClosed(structure.getBB().min, structure.getBB().max)) {
			if (level.getBlockState(blockPos).getBlock() == AWStructureBlocks.SOUND_BLOCK.get()) {
				WorldTools.getTile(level, blockPos, TileSoundBlock.class).ifPresent(TileSoundBlock::turnOffByProtectionFlag);
			}
		}
	}

	private void setOwner(Player player, GameProfile playerProfile) {
		owner = new Owner(player);
		this.playerProfile = playerProfile;
	}

	public boolean isPlayerOwned() {
		return owner != Owner.EMPTY;
	}

	public GameProfile getPlayerProfile() {
		return playerProfile;
	}

	public float getPlayerRelativeBlockHardness(Player player, float original) {
		return owner.isOwnerOrSameTeamOrFriend(player) ? original : UNBREAKABLE;
	}

	public boolean shouldProtectAgainst(Player player) {
		return !owner.isOwnerOrSameTeamOrFriend(player);
	}
}
