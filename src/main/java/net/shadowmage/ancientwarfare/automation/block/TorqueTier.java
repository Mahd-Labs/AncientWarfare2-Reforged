package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum TorqueTier implements StringRepresentable {
	LIGHT(0), MEDIUM(1), HEAVY(2);

	private int meta;

	TorqueTier(int meta) {
		this.meta = meta;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ENGLISH);
	}

	/* 1.12 name */
	public String getName() {
		return getSerializedName();
	}

	public int getMeta() {
		return meta;
	}

	public static TorqueTier byMetadata(int meta) {
		return values()[meta];
	}
}
