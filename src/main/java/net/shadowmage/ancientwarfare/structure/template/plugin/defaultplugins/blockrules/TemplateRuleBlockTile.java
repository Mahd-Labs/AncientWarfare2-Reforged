package net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.api.IStructureBuilder;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;

public class TemplateRuleBlockTile<T extends BlockEntity> extends TemplateRuleVanillaBlocks {
	private static final String FACING_TAG = "facing";
	public static final String PLUGIN_NAME = "blockTile";
	public CompoundTag tag;
	public Direction facing = null;


	public TemplateRuleBlockTile(Level world, BlockPos pos, BlockState state, int turns) {
		super(world, pos, state, turns);
		WorldTools.getTile(world, pos).ifPresent(t -> {
			tag = t.saveWithFullMetadata(world.registryAccess());
			tag.remove("x");
			tag.remove("y");
			tag.remove("z");
			if (t instanceof BlockRotationHandler.IRotatableTile) {
				facing = rotateFacing(turns, ((BlockRotationHandler.IRotatableTile) t).getPrimaryFacing());
			}
		});

	}

	private Direction rotateFacing(int turns, Direction o) {
		if (o.getAxis() != Direction.Axis.Y) {
			for (int i = 0; i < turns; i++) {
				o = o.getClockWise();
			}
		}
		return o;
	}

	public TemplateRuleBlockTile() {
		super();
	}

	/*
	 * Port addition: 1.12 item stacks / uuids / custom names in the tile data of original templates are upgraded the
	 * first time the rule is placed (not while the templates are parsed: the data fixers are slow)
	 */
	private boolean legacyDataUpgraded = true;

	/* Port addition: for subclasses that convert the 1.12 tile data themselves */
	protected void markTileDataUpgraded() {
		legacyDataUpgraded = true;
	}

	protected void upgradeLegacyData() {
		if (!legacyDataUpgraded && tag != null) {
			net.shadowmage.ancientwarfare.structure.util.LegacyItemStacks.upgradeNbt(tag);
		}
		legacyDataUpgraded = true;
	}

	@Override
	public void handlePlacement(Level world, int turns, BlockPos pos, IStructureBuilder builder) {
		upgradeLegacyData();
		super.handlePlacement(world, turns, pos, builder);
		WorldTools.getTile(world, pos).ifPresent(te -> {
			@SuppressWarnings("unchecked") T t = (T) te;
			tag.putInt("x", pos.getX());
			tag.putInt("y", pos.getY());
			tag.putInt("z", pos.getZ());
			try {
				t.loadWithComponents(tag, world.registryAccess());
				rotateTe(t, turns);
				//Port addition: 1.12 showed values the tile holds (variant, burning, transparent) through getActualState;
				//the block state is brought in step with the tile data just loaded
				BlockState current = world.getBlockState(pos);
				if (current.getBlock() instanceof net.shadowmage.ancientwarfare.structure.block.BlockBaseStructure structureBlock) {
					BlockState actual = structureBlock.getActualState(current, world, pos);
					if (actual != current) {
						world.setBlock(pos, actual, 2);
					}
				}
			}
			catch (Exception e) {
				AncientWarfareStructure.LOG.error("Error loading tile entity data from template for {}: ", t.getClass(), e);
			}
		});
	}

	@Override
	public boolean shouldReuseRule(Level world, BlockState state, int turns, BlockPos pos) {
		return false;
	}

	@Override
	public void writeRuleData(CompoundTag tag) {
		super.writeRuleData(tag);
		tag.put("teData", this.tag);
		if (facing != null) {
			tag.putString(FACING_TAG, facing.getName());
		}
	}

	@Override
	public void parseRule(CompoundTag tag) {
		super.parseRule(tag);
		this.tag = tag.getCompound("teData");
		legacyDataUpgraded = !legacyData;
		if (tag.contains(FACING_TAG)) {
			facing = Direction.byName(tag.getString(FACING_TAG));
		}
		//Port addition: 1.12 BlockChair took its facing from the tile (getActualState), the block state saved in
		//1.12 templates always says north. The facing is a block state property now: take it from the rule's facing
		//(the tile's primary facing). Other 1.12 tiles with a primary facing either had it in the metadata too or keep
		//the block state in step themselves (worksites).
		if (legacyData && facing != null && facing.getAxis().isHorizontal() && state.getBlock() instanceof net.shadowmage.ancientwarfare.structure.block.BlockChair
				&& state.hasProperty(net.shadowmage.ancientwarfare.core.render.property.CoreProperties.FACING)) {
			state = state.setValue(net.shadowmage.ancientwarfare.core.render.property.CoreProperties.FACING, facing);
		}
	}

	@Override
	public String getPluginName() {
		return PLUGIN_NAME;
	}





	@SuppressWarnings("squid:S1172") // parameters supposed to be used by overriding methods
	protected void rotateTe(T te, int turns) {
		if (facing != null && te instanceof BlockRotationHandler.IRotatableTile) {
			((BlockRotationHandler.IRotatableTile) te).setPrimaryFacing(rotateFacing(turns, facing));
		}
	}

}
