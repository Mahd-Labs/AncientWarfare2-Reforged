package net.shadowmage.ancientwarfare.structure.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

/*
 * Port notes: 1.12 NBTUtil.readGameProfileFromNBT / writeGameProfile (removed from vanilla). Same tag layout for the
 * parts the mod uses: "Name" and "Id" (uuid string); skin properties are not stored.
 */
public class ProfileHelper {
	private ProfileHelper() {}

	@Nullable
	public static GameProfile readGameProfile(CompoundTag compound) {
		String name = compound.contains("Name", 8) ? compound.getString("Name") : null;
		UUID id = null;
		if (compound.contains("Id", 8)) {
			try {
				id = UUID.fromString(compound.getString("Id"));
			}
			catch (IllegalArgumentException e) {
				id = null;
			}
		}
		if (name == null || id == null) {
			return null;
		}
		return new GameProfile(id, name);
	}

	public static CompoundTag writeGameProfile(@Nullable GameProfile profile) {
		CompoundTag tag = new CompoundTag();
		if (profile == null) {
			return tag;
		}
		if (profile.getName() != null && !profile.getName().isEmpty()) {
			tag.putString("Name", profile.getName());
		}
		if (profile.getId() != null) {
			tag.putString("Id", profile.getId().toString());
		}
		return tag;
	}
}
