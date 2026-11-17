package net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.entityrules;

import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

public class TemplateRuleEntityHanging extends TemplateRuleEntity<HangingEntity> {
	public static final String PLUGIN_NAME = "vanillaHangingEntity";
	private Direction direction;
	private BlockPos hangingOffset;

	public TemplateRuleEntityHanging(Level world, HangingEntity entity, int turns, int x, int y, int z) {
		super(world, entity, turns, x, y, z);
		BlockPos hangingOffset = entity.getPos().offset(-x, -y, -z);
		this.hangingOffset = BlockTools.rotateHorizontal(hangingOffset, turns);
		direction = Direction.from2DDataValue((Optional.ofNullable(entity.getDirection()).map(Direction::get2DDataValue).orElse(0) + turns) % 4);
	}

	public TemplateRuleEntityHanging() {
		super();
	}

	@Override
	protected CompoundTag getEntityNBT(BlockPos pos, int turns) {
		CompoundTag tag = super.getEntityNBT(pos, turns);
		Direction rotateDirection = Direction.from2DDataValue((direction.get2DDataValue() + turns) % 4);
		tag.putByte("Facing", (byte) rotateDirection.get2DDataValue());

		return tag;
	}

	@Override
	protected void updateEntityOnPlacement(int turns, BlockPos pos, HangingEntity e) {
		super.updateEntityOnPlacement(turns, pos, e);
		BlockPos hangingPos = pos.offset(BlockTools.rotateHorizontal(hangingOffset, turns));
		e.setPos(hangingPos.getX(), hangingPos.getY(), hangingPos.getZ()); //1.12 e.hangingPosition = ..
		updateBoundingBox(e);
	}

	@Override
	public void writeRuleData(CompoundTag tag) {
		super.writeRuleData(tag);
		tag.putByte("direction", (byte) direction.get2DDataValue());
		tag.putLong("hangingOffset", hangingOffset.asLong());
	}

	@Override
	public void parseRule(CompoundTag tag) {
		super.parseRule(tag);
		direction = Direction.from2DDataValue(tag.getByte("direction") % 4);
		hangingOffset = readPackedPos(tag.getLong("hangingOffset")); //1.12 layout in original templates
	}

	@Override
	public String getPluginName() {
		return PLUGIN_NAME;
	}


	private static void updateBoundingBox(HangingEntity entityHanging) {
		//Port notes: 1.12 called the protected updateBoundingBox by reflection; setting the position recalculates the box
		entityHanging.setPos(entityHanging.getX(), entityHanging.getY(), entityHanging.getZ());
	}
}
