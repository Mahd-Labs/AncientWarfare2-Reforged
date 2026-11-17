package net.shadowmage.ancientwarfare.structure.api;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public abstract class TemplateRuleEntityBase extends TemplateRule {

	private BlockPos pos;
	/*
	 * Port addition: true for a rule parsed from an original 1.12 template (no data version). Its packed
	 * positions have the 1.12 bit layout, its entity data 1.12 item stacks / uuids (see util.LegacyPositions).
	 */
	private boolean legacyData;

	protected final boolean isLegacyData() {
		return legacyData;
	}

	protected final BlockPos readPackedPos(long packed) {
		return legacyData ? net.shadowmage.ancientwarfare.structure.util.LegacyPositions.fromLegacyLong(packed) : BlockPos.of(packed);
	}

	/*
	 * Called by reflection
	 */
	public TemplateRuleEntityBase() {}

	@Override
	public void writeRuleData(CompoundTag tag) {
		tag.putLong("position", pos.asLong());
		tag.putInt(net.shadowmage.ancientwarfare.structure.util.LegacyBlockStates.DATA_VERSION_TAG, net.shadowmage.ancientwarfare.structure.util.LegacyBlockStates.currentDataVersion());
	}

	@Override
	public void parseRule(CompoundTag tag) {
		legacyData = !tag.contains(net.shadowmage.ancientwarfare.structure.util.LegacyBlockStates.DATA_VERSION_TAG);
		pos = readPackedPos(tag.getLong("position"));
	}

	@Override
	protected String getRuleType() {
		return "entity";
	}

	public final void setPos(BlockPos pos) {
		this.pos = pos;
	}

	public final BlockPos blockPosition() {
		return pos;
	}

	@Override
	public ItemStack getRemainingStack() {
		return ItemStack.EMPTY;
	}
}
