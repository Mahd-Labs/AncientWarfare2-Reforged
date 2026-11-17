package net.shadowmage.ancientwarfare.structure.api;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.shadowmage.ancientwarfare.core.util.RenderTools;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.registry.StructureBlockRegistry;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Optional;

public abstract class TemplateRuleBlock extends TemplateRule {
	protected BlockState state;
	private ItemStack cachedStack = null;
	private boolean placeInSurvival = false;

	public TemplateRuleBlock(BlockState state, int turns) {
		this.state = BlockTools.rotateFacing(state, turns);
	}

	public TemplateRuleBlock() {
	}

	public abstract boolean shouldReuseRule(Level world, BlockState state, int turns, BlockPos pos);

	@Override
	public List<ItemStack> getResources() {
		if (state.getBlock() == Blocks.AIR) {
			return Collections.emptyList();
		}

		ItemStack stack = getCachedStack();
		if (!stack.isEmpty()) {
			return Collections.singletonList(stack);
		}

		return Collections.emptyList();
	}

	private ItemStack getCachedStack() {
		cacheStack();
		return cachedStack;
	}

	private void cacheStack() {
		if (cachedStack == null) {
			Optional<ItemStack> stack = getStack();
			placeInSurvival = stack.isPresent();
			cachedStack = stack.orElse(ItemStack.EMPTY);
		}
	}

	protected Optional<ItemStack> getStack() {
		return StructureBlockRegistry.getItemStackFrom(state);
	}

	@Override
	public ItemStack getRemainingStack() {
		return StructureBlockRegistry.getRemainingStackFrom(state);
	}

	@Override
	public boolean placeInSurvival() {
		cacheStack();
		return state.getBlock() != Blocks.AIR && placeInSurvival;
	}

	@Override
	protected String getRuleType() {
		return "rule";
	}

	/*
	 * Port addition: true for a rule parsed from an original 1.12 template (rules written by the port carry a data version)
	 */
	protected boolean legacyData;

	@Override
	public void parseRule(CompoundTag tag) {
		legacyData = !tag.contains(net.shadowmage.ancientwarfare.structure.util.LegacyBlockStates.DATA_VERSION_TAG);
		try {
			//Port addition: rules of original (1.12) templates have no data version, their block state is upgraded
			CompoundTag blockStateTag = tag.getCompound("blockState");
			state = NBTHelper.getBlockState(tag.contains(net.shadowmage.ancientwarfare.structure.util.LegacyBlockStates.DATA_VERSION_TAG) ? blockStateTag : net.shadowmage.ancientwarfare.structure.util.LegacyBlockStates.upgrade(blockStateTag));
		}
		catch (RuntimeException e) {
			//Port notes: 1.12 caught MissingResourceException and warned. The shipped pack names many blocks of 1.12 mods and
			//block states the vanilla data fixers cannot map (invalid resource location): air, logged at debug.
			AncientWarfareStructure.LOG.debug("Unable to find blockstate while parsing structure template thus replacing it with air - {}.", e.getMessage());
			state = Blocks.AIR.defaultBlockState();
		}
	}

	@Override
	public void writeRuleData(CompoundTag tag) {
		tag.put("blockState", NBTHelper.getBlockStateTag(state));
		tag.putInt(net.shadowmage.ancientwarfare.structure.util.LegacyBlockStates.DATA_VERSION_TAG, net.shadowmage.ancientwarfare.structure.util.LegacyBlockStates.currentDataVersion());
	}


	public BlockState getState(int turns) {
		return BlockTools.rotateFacing(state, turns);
	}

	public BlockEntity getTileEntity(int turns) {
		return null;
	}

	@SuppressWarnings("squid:S1172")
	public boolean isDynamicallyRendered(int turns) {
		return false;
	}

}
