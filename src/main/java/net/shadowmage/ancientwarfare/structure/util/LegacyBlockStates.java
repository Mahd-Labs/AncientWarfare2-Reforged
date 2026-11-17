package net.shadowmage.ancientwarfare.structure.util;

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;

/*
 * Port addition: the block rules of the original (1.12) template files name 1.12 block states
 * ({blockName:"minecraft:planks",properties:{variant:"oak"}}). Rules written by the port carry a "dataVersion"; a
 * block state tag without it is run through the vanilla data fixers (the flattening and every later rename) from
 * the 1.12.2 data version, and the five 1.12 Ancient Warfare mod ids are folded into "ancientwarfare".
 * Tile entity data and item stacks stored in the templates are NOT converted.
 */
public class LegacyBlockStates {
	private LegacyBlockStates() {}

	public static final String DATA_VERSION_TAG = "dataVersion";
	private static final int DATA_VERSION_1_12_2 = 1343;
	private static final String BLOCK_NAME_TAG = "blockName";
	private static final String PROPERTIES_TAG = "properties";

	public static int currentDataVersion() {
		return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
	}

	/*
	 * blockStateTag: the core NBTHelper block state tag (blockName + properties) of a rule without a data version
	 */
	public static CompoundTag upgrade(CompoundTag blockStateTag) {
		String name = blockStateTag.getString(BLOCK_NAME_TAG);
		if (name.startsWith("ancientwarfare")) {
			CompoundTag ret = blockStateTag.copy();
			int colon = name.indexOf(':');
			if (colon > 0) {
				ret.putString(BLOCK_NAME_TAG, "ancientwarfare" + name.substring(colon));
			}
			return ret;
		}
		try {
			CompoundTag vanilla = new CompoundTag();
			vanilla.putString("Name", name);
			if (blockStateTag.contains(PROPERTIES_TAG)) {
				vanilla.put("Properties", blockStateTag.getCompound(PROPERTIES_TAG).copy());
			}
			Dynamic<Tag> fixed = DataFixers.getDataFixer().update(References.BLOCK_STATE, new Dynamic<>(NbtOps.INSTANCE, vanilla), DATA_VERSION_1_12_2, currentDataVersion());
			if (fixed.getValue() instanceof CompoundTag) {
				CompoundTag result = (CompoundTag) mutableCopy(fixed.getValue());
				CompoundTag ret = new CompoundTag();
				ret.putString(BLOCK_NAME_TAG, result.getString("Name"));
				if (result.contains("Properties")) {
					ret.put(PROPERTIES_TAG, result.getCompound("Properties"));
				}
				upgradeLeaves(ret);
				return ret;
			}
		}
		catch (RuntimeException e) {
			AncientWarfareStructure.LOG.warn("Could not upgrade the 1.12 block state {}", blockStateTag, e);
		}
		return blockStateTag;
	}

	/*
	 * A copy of a data fixer result whose compound and list tags can be changed. In some packs (seen in ATM10 8.6) the
	 * fixers return compound tags backed by immutable maps, so changing them threw UnsupportedOperationException (every
	 * 1.12 leaves rule of beta.8 there: 994 warnings at start and leaves built as air).
	 */
	public static Tag mutableCopy(Tag tag) {
		if (tag instanceof CompoundTag compound) {
			CompoundTag copy = new CompoundTag();
			for (String key : compound.getAllKeys()) {
				copy.put(key, mutableCopy(compound.get(key)));
			}
			return copy;
		}
		if (tag instanceof net.minecraft.nbt.ListTag list) {
			net.minecraft.nbt.ListTag copy = new net.minecraft.nbt.ListTag();
			for (Tag element : list) {
				copy.add(mutableCopy(element));
			}
			return copy;
		}
		return tag.copy();
	}

	/*
	 * The flattening table keeps the 1.12 leaves properties (decayable, check_decay) on the new leaves blocks; the
	 * vanilla chunk fixer (LeavesFix) replaces them, the block state fixer does not, so every 1.12 leaves rule failed
	 * to parse and was built as air. 1.12 leaves decayed only with decayable and check_decay both set (check_decay was
	 * set when a log or leaves block nearby was broken); the leaves of a template, which had check_decay false, stayed
	 * until then. Closest 1.21 state: persistent unless both were set. Deviation: such leaves no longer start to decay
	 * when the player breaks a log next to them.
	 */
	private static void upgradeLeaves(CompoundTag blockStateTag) {
		if (!blockStateTag.getString(BLOCK_NAME_TAG).endsWith("_leaves")) {
			return;
		}
		CompoundTag properties = blockStateTag.getCompound(PROPERTIES_TAG);
		if (!properties.contains("decayable") && !properties.contains("check_decay")) {
			return;
		}
		boolean decays = "true".equals(properties.getString("decayable")) && "true".equals(properties.getString("check_decay"));
		properties.remove("decayable");
		properties.remove("check_decay");
		properties.putString("persistent", decays ? "false" : "true");
		blockStateTag.put(PROPERTIES_TAG, properties);
	}

	/*
	 * Port addition: copies the named properties of a 1.12 block state tag (blockName + properties, as saved in the
	 * template) onto the upgraded state, where the upgraded block has the property and the saved value is valid for
	 * it. Used where the flattening table loses a value that 1.12 kept outside the metadata (the facing of an upper
	 * door half).
	 */
	public static BlockState withLegacyProperties(BlockState state, CompoundTag blockStateTag, String... names) {
		CompoundTag properties = blockStateTag.getCompound(PROPERTIES_TAG);
		for (String name : names) {
			Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
			if (property != null && properties.contains(name, Tag.TAG_STRING)) {
				state = withValue(state, property, properties.getString(name));
			}
		}
		return state;
	}

	private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String value) {
		return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
	}
}
