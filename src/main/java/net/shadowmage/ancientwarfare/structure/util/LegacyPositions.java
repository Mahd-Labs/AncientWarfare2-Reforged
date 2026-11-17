package net.shadowmage.ancientwarfare.structure.util;

import net.minecraft.core.BlockPos;

/*
 * Port addition. BlockPos.toLong() changed its bit layout in 1.13:
 *   1.12: x << 38 | y << 26 | z   (x 26 bits, y 12 bits, z 26 bits)
 *   1.21: x << 38 | z << 12 | y
 * Every packed position inside the original (1.12) template files (the "position" of entity rules, "hangingOffset",
 * positions inside tile / entity data) has the 1.12 layout and must be decoded with fromLegacyLong. Data written by
 * the port keeps the 1.21 layout (BlockPos.asLong / BlockPos.of); which one a rule has is decided by its origin
 * (rules written by the port carry LegacyBlockStates.DATA_VERSION_TAG), never by looking at the value.
 */
public class LegacyPositions {
	private LegacyPositions() {}

	private static final int NUM_X_BITS = 26;
	private static final int NUM_Z_BITS = 26;
	private static final int NUM_Y_BITS = 12;
	private static final int Y_SHIFT = NUM_Z_BITS;
	private static final int X_SHIFT = Y_SHIFT + NUM_Y_BITS;

	public static BlockPos fromLegacyLong(long serialized) {
		int x = (int) (serialized << 64 - X_SHIFT - NUM_X_BITS >> 64 - NUM_X_BITS);
		int y = (int) (serialized << 64 - Y_SHIFT - NUM_Y_BITS >> 64 - NUM_Y_BITS);
		int z = (int) (serialized << 64 - NUM_Z_BITS >> 64 - NUM_Z_BITS);
		return new BlockPos(x, y, z);
	}

	/*
	 * a 1.12 packed position re-packed with the 1.21 layout
	 */
	public static long upgradeLong(long legacy) {
		return fromLegacyLong(legacy).asLong();
	}
}
