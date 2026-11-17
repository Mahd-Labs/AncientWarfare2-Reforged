package net.shadowmage.ancientwarfare.core.owner;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Utf8String;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.UUID;

@Immutable
public class Owner {
	public static final Owner EMPTY = new Owner();
	private static final String OWNER_NAME_TAG = "ownerName";
	private static final String OWNER_ID_TAG = "ownerId";
	private final UUID uuid;
	private final String name;

	private Owner() {
		this(new UUID(0, 0), "");
	}

	private Owner(UUID uuid, String name) {
		this.uuid = uuid;
		this.name = name;
	}

	public Owner(Player player) {
		this(player.getUUID(), player.getGameProfile().getName());
	}

	public Owner(ByteBuf buffer) {
		this(new UUID(buffer.readLong(), buffer.readLong()), Utf8String.read(buffer, 32767));
	}

	public Owner(Level world, String name) {
		Player player = null;
		for (Player p : world.players()) {
			if (name.equals(p.getGameProfile().getName())) {
				player = p;
				break;
			}
		}
		uuid = player != null ? player.getUUID() : new UUID(0, 0);
		this.name = name;
	}

	public boolean isOwnerOrSameTeamOrFriend(@Nullable Entity entity) {
		if (entity instanceof IOwnable) {
			Owner owner = ((IOwnable) entity).getOwner();
			return isOwnerOrSameTeamOrFriend(entity.level(), owner.getUUID(), owner.getName());
		}
		if (entity instanceof OwnableEntity && ((OwnableEntity) entity).getOwner() != null) {
			Entity owner = ((OwnableEntity) entity).getOwner();
			return isOwnerOrSameTeamOrFriend(entity.level(), owner.getUUID(), getEntityName(owner));
		}
		return entity != null && isOwnerOrSameTeamOrFriend(entity.level(), entity.getUUID(), getEntityName(entity));
	}

	/* 1.12 Entity.getName(): the profile name for players, the display name otherwise */
	private static String getEntityName(Entity entity) {
		return entity instanceof Player ? ((Player) entity).getGameProfile().getName() : entity.getName().getString();
	}

	public boolean isOwnerOrSameTeamOrFriend(Level world, @Nullable UUID playerId, String playerName) {
		return TeamViewerRegistry.areFriendly(world, uuid, playerId, name, playerName);
	}

	public String getName() {
		return name;
	}

	public UUID getUUID() {
		return uuid;
	}

	public void serializeToBuffer(ByteBuf buffer) {
		buffer.writeLong(uuid.getMostSignificantBits());
		buffer.writeLong(uuid.getLeastSignificantBits());
		Utf8String.write(buffer, name, 32767);
	}

	public CompoundTag serializeToNBT(CompoundTag tag) {
		if (this == EMPTY) {
			return tag;
		}
		tag.putString(OWNER_NAME_TAG, name);
		tag.putUUID(OWNER_ID_TAG, uuid);
		return tag;
	}

	public static Owner deserializeFromNBT(CompoundTag tag) {
		if (tag.contains(OWNER_NAME_TAG)) {
			return new Owner(tag.hasUUID(OWNER_ID_TAG) ? tag.getUUID(OWNER_ID_TAG) : new UUID(0, 0), tag.getString(OWNER_NAME_TAG));
		}
		return Owner.EMPTY;
	}

	public boolean playerHasCommandPermissions(Level world, UUID playerId, String playerName) {
		return this != Owner.EMPTY && TeamViewerRegistry.areTeamMates(world, uuid, playerId, name, playerName);
	}
}
