package net.shadowmage.ancientwarfare.core.render.property;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/*
 * Replacement for Forge 1.12 IExtendedBlockState (the name is kept so that renderers port line by line; it is a
 * final class here): an immutable BlockState plus values for "unlisted" properties, which are NeoForge
 * ModelProperty keys in the port. It travels from getModelData to getQuads inside ModelData under STATE.
 *
 * Differences to 1.12:
 *  - unlisted properties are not declared by the block; getUnlistedProperties() only contains the ones that were set,
 *    getValue of an unset one returns null (as 1.12 did for a declared but unset one).
 *  - withProperty returns IExtendedBlockState directly (no cast needed).
 */
public final class IExtendedBlockState {
	public static final ModelProperty<IExtendedBlockState> STATE = new ModelProperty<>();

	private final BlockState state;
	private final Map<ModelProperty<?>, Object> unlisted;

	public IExtendedBlockState(BlockState state) {
		this(state, Collections.emptyMap());
	}

	private IExtendedBlockState(BlockState state, Map<ModelProperty<?>, Object> unlisted) {
		this.state = state;
		this.unlisted = unlisted;
	}

	/*
	 * the state carried by the model data, or a bare one for the given block state
	 */
	public static IExtendedBlockState from(BlockState state, ModelData data) {
		IExtendedBlockState extended = data.get(STATE);
		return extended != null ? extended : new IExtendedBlockState(state);
	}

	public ModelData toModelData() {
		return ModelData.of(STATE, this);
	}

	/*
	 * the vanilla block state (1.12 getClean())
	 */
	public BlockState getClean() {
		return state;
	}

	public Block getBlock() {
		return state.getBlock();
	}

	public <T extends Comparable<T>> T getValue(Property<T> property) {
		return state.getValue(property);
	}

	public boolean hasProperty(Property<?> property) {
		return state.hasProperty(property);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T getValue(ModelProperty<T> property) {
		return (T) unlisted.get(property);
	}

	public <T extends Comparable<T>, V extends T> IExtendedBlockState withProperty(Property<T> property, V value) {
		return new IExtendedBlockState(state.setValue(property, value), unlisted);
	}

	public <T> IExtendedBlockState withProperty(ModelProperty<T> property, T value) {
		Map<ModelProperty<?>, Object> map = new HashMap<>(unlisted);
		map.put(property, value);
		return new IExtendedBlockState(state, map);
	}

	/*
	 * the unlisted properties that have been set on this state
	 */
	public Map<ModelProperty<?>, Optional<?>> getUnlistedProperties() {
		Map<ModelProperty<?>, Optional<?>> ret = new HashMap<>();
		for (Map.Entry<ModelProperty<?>, Object> entry : unlisted.entrySet()) {
			ret.put(entry.getKey(), Optional.ofNullable(entry.getValue()));
		}
		return ret;
	}
}
