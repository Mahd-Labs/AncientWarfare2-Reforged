package net.shadowmage.ancientwarfare.core.util;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;
import net.shadowmage.ancientwarfare.core.entity.AWFakePlayer;
import net.shadowmage.ancientwarfare.core.util.parsing.PropertyState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.minecraft.world.level.block.state.properties.RailShape.*;

public class BlockTools {

	/*
	 * Port addition: 1.12 called AWStructureStatics.isSkippable(state) directly. The structure module installs its
	 * predicate here (default: nothing is skippable) so that core does not depend on the structure module.
	 */
	private static Predicate<BlockState> skippableBlocks = state -> false;

	public static void setSkippableBlockPredicate(Predicate<BlockState> isSkippable) {
		skippableBlocks = isSkippable;
	}

	public static BlockPos rotateHorizontal(BlockPos offset, int turns) {
		BlockPos.MutableBlockPos ret = new BlockPos.MutableBlockPos(offset.getX(), offset.getY(), offset.getZ());
		for (int i = 0; i < turns; i++) {
			ret.set(-ret.getZ(), ret.getY(), ret.getX());
		}
		return ret;
	}

	/*
	 * rotate a float X offset (-1<=x<=1) within a block
	 */
	public static float rotateFloatX(float x, float z, int turns) {
		float x1;
		float z1;
		x1 = x;
		z1 = z;
		for (int i = 0; i < turns; i++) {
			z = x1;
			x = 1.f - z1;
			x1 = x;
			z1 = z;
		}
		return x;
	}

	public static float rotateFloatZ(float x, float z, int turns) {
		float x1;
		float z1;
		x1 = x;
		z1 = z;
		for (int i = 0; i < turns; i++) {
			z = x1;
			x = 1.f - z1;
			x1 = x;
			z1 = z;
		}
		return z;
	}

	public static BlockPos getAverageOf(BlockPos... positions) {
		float x = 0;
		float y = 0;
		float z = 0;
		int count = 0;
		for (BlockPos pos : positions) {
			x += pos.getX();
			y += pos.getY();
			z += pos.getZ();
			count++;
		}
		if (count > 0) {
			x /= count;
			y /= count;
			z /= count;
		}
		return BlockPos.containing(x, y, z);
	}

	/*
	 * will return null if nothing is in range
	 */
	@Nullable
	public static BlockPos getBlockClickedOn(Player player, Level world, boolean offset) {
		float rotPitch = player.getXRot();
		float rotYaw = player.getYRot();
		double testX = player.getX();
		double testY = player.getY() + player.getEyeHeight();
		double testZ = player.getZ();
		Vec3 testVector = new Vec3(testX, testY, testZ);
		float var14 = Mth.cos(-rotYaw * 0.017453292F - (float) Math.PI);
		float var15 = Mth.sin(-rotYaw * 0.017453292F - (float) Math.PI);
		float var16 = -Mth.cos(-rotPitch * 0.017453292F);
		float vectorY = Mth.sin(-rotPitch * 0.017453292F);
		float vectorX = var15 * var16;
		float vectorZ = var14 * var16;
		double reachLength = 5.0D;
		Vec3 testVectorFar = testVector.add(vectorX * reachLength, vectorY * reachLength, vectorZ * reachLength);
		BlockHitResult testHitPosition = world.clip(new ClipContext(testVector, testVectorFar, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));

		/*
		 * if nothing was hit, return null
		 */
		if (testHitPosition.getType() == HitResult.Type.MISS) {
			return null;
		}

		Vec3 var25 = player.getViewVector(1.0F);
		float var27 = 1.0F;
		List<Entity> entitiesPossiblyHitByVector = world.getEntities(player, player.getBoundingBox().expandTowards(var25.x * reachLength, var25.y * reachLength, var25.z * reachLength).expandTowards(var27, var27, var27));
		for (Entity testEntity : entitiesPossiblyHitByVector) {
			if (testEntity.isPickable()) {
				float bbExpansionSize = testEntity.getPickRadius();
				AABB entityBB = testEntity.getBoundingBox().expandTowards(bbExpansionSize, bbExpansionSize, bbExpansionSize);
				/*
				 * if an entity is hit, return its position
				 */
				if (entityBB.contains(testVector)) {
					return BlockPos.containing(testEntity.getX(), testEntity.getY(), testEntity.getZ());
				}
			}
		}
		/*
		 * if no entity was hit, return the position impacted.
		 */
		return offset ? testHitPosition.getBlockPos().relative(testHitPosition.getDirection()) : testHitPosition.getBlockPos();
	}

	public static BlockPos rotateAroundOrigin(BlockPos pos, int turns) {
		for (int i = 0; i < turns; i++) {
			pos = rotateAroundOrigin(pos);
		}
		return pos;
	}

	/*
	 * rotate a position around its origin (0,0,0), in 90 degree steps
	 */
	private static BlockPos rotateAroundOrigin(BlockPos pos) {
		return new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
	}

	/*
	 * checks to see if TEST lies somewhere in the cube bounded by pos1 and pos2
	 *
	 * @return true if it does
	 */
	public static boolean isPositionWithinBounds(BlockPos test, BlockPos pos1, BlockPos pos2) {
		return isPositionWithinHorizontalBounds(test, pos1, pos2)
				&& test.getY() >= pos1.getY() && test.getY() <= pos2.getY();
	}

	public static boolean isPositionWithinHorizontalBounds(BlockPos test, BlockPos minPos, BlockPos maxPos) {
		return test.getX() >= minPos.getX() && test.getX() <= maxPos.getX()
				&& test.getZ() >= minPos.getZ() && test.getZ() <= maxPos.getZ();
	}

	/*
	 * return a new BlockPos containing the minimum coordinates from the two passed in BlockPos
	 */
	public static BlockPos getMin(BlockPos pos1, BlockPos pos2) {
		return new BlockPos(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));
	}

	/*
	 * return a new BlockPos containing the maximum coordinates from the two passed in BlockPos
	 */
	public static BlockPos getMax(BlockPos pos1, BlockPos pos2) {
		return new BlockPos(Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));
	}

	/*
	 * rotates a given block-position in a given area by the number of turns.  Used by templates
	 * to get a rotated BlockPos.
	 */
	public static BlockPos rotateInArea(BlockPos pos, int xSize, int zSize, int turns) {
		int xSize1 = xSize;
		int zSize1 = zSize;
		int x = pos.getX();
		int z = pos.getZ();
		int x1 = x;
		int z1 = z;
		int positiveTurns = turns > 0 ? turns : 4 + turns;
		for (int i = 0; i < positiveTurns; i++) {
			x = zSize - 1 - z1;
			z = x1;
			x1 = x;
			z1 = z;
			xSize = zSize1;
			zSize = xSize1;
			xSize1 = xSize;
			zSize1 = zSize;
		}
		return new BlockPos(x, pos.getY(), z);
	}

	public static boolean breakBlockAndDrop(Level world, BlockPos pos) {
		return breakBlock(world, pos, 0, true);
	}

	public static boolean breakBlock(Level world, BlockPos pos, int fortune, boolean doDrop) {
		if (world.isClientSide) {
			return false;
		}
		BlockState state = world.getBlockState(pos);
		if (world.isEmptyBlock(pos) || state.getDestroySpeed(world, pos) < 0) {
			return false;
		}
		if (doDrop) {
			if (!canBreakBlock(world, pos, state)) {
				return false;
			}
			Block.dropResources(state, world, pos, world.getBlockEntity(pos), null, getFortuneTool(world, fortune));
		}
		return world.removeBlock(pos, false);
	}

	/*
	 * Port addition: 1.12 passed the fortune level to Block.dropBlockAsItem. In 1.21.1 drops come from loot tables
	 * that read the fortune level from the tool in the loot context.
	 */
	private static ItemStack getFortuneTool(Level world, int fortune) {
		if (fortune <= 0) {
			return ItemStack.EMPTY;
		}
		ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
		world.registryAccess().lookup(Registries.ENCHANTMENT).flatMap(reg -> reg.get(Enchantments.FORTUNE)).ifPresent(e -> tool.enchant(e, fortune));
		return tool;
	}

	private static boolean canBreakBlock(Level world, BlockPos pos, BlockState state) {
		return !AWCoreStatics.fireBlockBreakEvents || !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(world, pos, state, AWFakePlayer.get(world))).isCanceled();
	}

	public static boolean breakBlockNoDrops(Level world, BlockPos pos, BlockState state) {
		if (!BlockTools.canBreakBlock(world, pos, state) || !world.removeBlock(pos, false)) {
			return false;
		}
		world.levelEvent(2001, pos, Block.getId(state));

		return true;
	}

	public static boolean placeItemBlockRightClick(ItemStack stack, Level world, BlockPos pos) {
		Player owner = AWFakePlayer.get(world);
		owner.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
		owner.setXRot(90F % 360F);

		return stack.use(world, owner, InteractionHand.MAIN_HAND).getResult().consumesAction();
	}

	public static boolean placeItemBlock(ItemStack stack, Level world, BlockPos pos, Direction face) {
		Direction direction = face.getOpposite();

		Player owner = AWFakePlayer.get(world);
		owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockPos clickedPos = pos.relative(direction);
		BlockHitResult hit = new BlockHitResult(new Vec3(clickedPos.getX() + 0.25D, clickedPos.getY() + 0.25D, clickedPos.getZ() + 0.25D), face, clickedPos, false);
		return stack.useOn(new UseOnContext(world, owner, InteractionHand.MAIN_HAND, stack, hit)).consumesAction();
	}

	public static void notifyBlockUpdate(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		world.sendBlockUpdated(pos, state, state, 3);
	}

	public static void notifyBlockUpdate(BlockEntity tile) {
		if (tile.getLevel() != null) {
			notifyBlockUpdate(tile.getLevel(), tile.getBlockPos());
		}
	}

	public static void notifyNeighbors(BlockEntity tile) {
		Level world = tile.getLevel();
		BlockPos pos = tile.getBlockPos();
		world.updateNeighborsAt(pos, world.getBlockState(pos).getBlock());
	}

	public static JsonElement serializeToJson(BlockState state) {
		JsonObject serializedState = new JsonObject();
		serializedState.addProperty("name", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());

		JsonObject serializedProps = new JsonObject();
		for (Map.Entry<Property<?>, Comparable<?>> prop : state.getValues().entrySet()) {
			serializedProps.addProperty(prop.getKey().getName(), serializeValue(prop.getKey(), prop.getValue()));
		}

		if (!serializedProps.entrySet().isEmpty()) {
			serializedState.add("properties", serializedProps);
		}

		return serializedState;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> String serializeValue(Property<T> property, Comparable<?> valueString) {
		return property.getName((T) valueString);
	}

	public static Direction getHorizontalFacingFromMeta(int meta) {
		return Direction.values()[meta].getAxis() != Direction.Axis.Y ? Direction.values()[meta] : Direction.NORTH;
	}

	public static <T> T getBlockState(Tuple<String, Map<String, String>> blockProps, Function<Block, T> init, AddPropertyFunction<T> addProperty) {
		String registryName = blockProps.getA();
		Map<String, String> properties = blockProps.getB();

		Block block = RegistryTools.getBlock(registryName);
		T ret = init.apply(block);

		StateDefinition<Block, BlockState> stateContainer = block.getStateDefinition();

		for (Map.Entry<String, String> prop : properties.entrySet()) {
			Property<?> property = stateContainer.getProperty(prop.getKey());

			if (property == null) {
				throw new MissingResourceException("Block \"" + BuiltInRegistries.BLOCK.getKey(block) + "\" doesn't have \"" + prop.getKey() + "\" property",
						Property.class.getName(), prop.getKey());
			}

			ret = addProperty.apply(ret, property, getPropertyState(block, stateContainer, prop.getKey(), prop.getValue()).getValue());
		}

		return ret;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> BlockState updateProperty(BlockState state, Property<T> property, Comparable<?> value) {
		return state.setValue(property, (T) value);
	}

	private static <T extends Comparable<T>> Optional<T> getValueHelper(Property<T> property, String valueString) {
		return property.getValue(valueString);
	}

	public static BlockState rotateFacing(BlockState state, int turns) {
		for (Map.Entry<Property<?>, Comparable<?>> property : state.getValues().entrySet()) {
			Class<?> valueClass = property.getKey().getValueClass();
			if (ROTATORS.containsKey(valueClass)) {
				state = rotateY(state, property.getKey(), turns);
			}
		}
		return state;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> BlockState rotateY(BlockState state, Property<T> property, int turns) {
		T rotated = ((IRotator<T>) ROTATORS.get(property.getValueClass())).rotateY(state.getValue(property), turns);
		return property.getPossibleValues().contains(rotated) ? state.setValue(property, rotated) : state;
	}

	/*
	 * Port note: the 1.12 lever orientation, log axis and huge mushroom rotators are gone with their properties
	 * (lever = FACE + FACING, log = Direction.Axis, mushroom blocks have six boolean face properties).
	 */
	@SuppressWarnings({"Convert2Lambda", "squid:S1604"})
	private static final Map<Class<?>, IRotator<?>> ROTATORS = new ImmutableMap.Builder<Class<?>, IRotator<?>>()
			.put(Direction.class, new IRotator<Direction>() {
				@Override
				public Direction rotateY(Direction facing, int turns) {
					if (facing.getAxis() == Direction.Axis.Y) {
						return facing;
					}

					for (int i = 0; i < turns; i++) {
						facing = facing.getClockWise();
					}
					return facing;
				}
			})
			.put(Direction.Axis.class, new IRotator<Direction.Axis>() {
				@Override
				public Direction.Axis rotateY(Direction.Axis facing, int turns) {
					if (facing == Direction.Axis.Y || turns % 2 == 0) {
						return facing;
					}

					return facing == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
				}
			})
			.put(RailShape.class, new IRotator<RailShape>() {
				@Override
				public RailShape rotateY(RailShape facing, int turns) {
					RailShape rotatedFacing = facing;
					for (int i = 0; i < turns; i++) {
						rotatedFacing = rotateOnce(rotatedFacing);
					}
					return rotatedFacing;
				}

				private RailShape rotateOnce(RailShape facing) {
					switch (facing) {
						case NORTH_SOUTH:
							return EAST_WEST;
						case EAST_WEST:
							return NORTH_SOUTH;
						case ASCENDING_EAST:
							return ASCENDING_SOUTH;
						case ASCENDING_SOUTH:
							return ASCENDING_WEST;
						case ASCENDING_WEST:
							return ASCENDING_NORTH;
						case ASCENDING_NORTH:
							return ASCENDING_EAST;
						case SOUTH_EAST:
							return SOUTH_WEST;
						case SOUTH_WEST:
							return NORTH_WEST;
						case NORTH_WEST:
							return NORTH_EAST;
						case NORTH_EAST:
						default:
							return SOUTH_EAST;
					}
				}
			})
			.build();

	@SuppressWarnings({"java:S3740", "rawtypes"}) // this is a method that helps with turning raw property type into generic one
	public static PropertyState getPropertyState(Block block, StateDefinition<Block, BlockState> stateContainer, String propName, String propValue) {
		Property<?> property = stateContainer.getProperty(propName);
		if (property == null) {
			throw new MissingResourceException("Block \"" + BuiltInRegistries.BLOCK.getKey(block) + "\" doesn't have \"" + propName + "\" property",
					Property.class.getName(), propName);
		}
		return getPropertyState(property, propName, propValue);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>, V extends T> PropertyState<T, V> getPropertyState(Property<T> property, String propName, String propValue) {
		Optional<?> value = getValueHelper(property, propValue);
		if (!value.isPresent()) {
			throw new MissingResourceException("Invalid value \"" + propValue + "\" for property \"" + propName + "\"", Property.class.getName(), propName);
		}
		return new PropertyState<>(property, (V) value.get());
	}

	public static int getTopFilledHeight(Level world, int x, int z, boolean skippables, int maxY) {
		return getTopFilledHeight(world.getChunk(x >> 4, z >> 4), x, z, skippables, maxY);
	}

	public static int getTopFilledHeight(Level world, int x, int z, boolean skippables) {
		return getTopFilledHeight(world.getChunk(x >> 4, z >> 4), x, z, skippables);
	}

	public static int getTopFilledHeight(LevelChunk chunk, int x, int z, boolean skippables) {
		return getTopFilledHeight(chunk, x, z, skippables, chunk.getHighestSectionPosition() + 16);
	}

	/*
	 * Port note: scans down to the bottom of the world (1.12: down to y=1); still returns -1 when nothing is found.
	 */
	private static int getTopFilledHeight(LevelChunk chunk, int x, int z, boolean skippables, int maxY) {
		for (int y = maxY; y > chunk.getMinBuildHeight(); y--) {
			BlockState state = chunk.getBlockState(new BlockPos(x, y, z));
			if (state.isAir() || (skippables && skippableBlocks.test(state))) {
				continue;
			}
			return y;
		}
		return -1;
	}

	private interface IRotator<T extends Comparable<T>> {
		T rotateY(T facing, int turns);
	}

	public interface AddPropertyFunction<T> {
		T apply(T obj, Property<?> property, Comparable<?> value);
	}

	public static Iterable<BlockPos> getAllInBoxTopDown(BlockPos from, BlockPos to) {
		return getAllInBoxTopDown(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()), Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
	}

	public static Iterable<BlockPos> getAllInBoxTopDown(final int x1, final int y1, final int z1, final int x2, final int y2, final int z2) {
		return () -> new AbstractIterator<BlockPos>() {
			private boolean first = true;
			private int lastPosX;
			private int lastPosY;
			private int lastPosZ;

			protected BlockPos computeNext() {
				if (first) {
					first = false;
					lastPosX = x1;
					lastPosY = y2;
					lastPosZ = z1;
					return new BlockPos(x1, y2, z1);
				} else if (lastPosX == x2 && lastPosY == y1 && lastPosZ == z2) {
					return endOfData();
				} else {
					if (lastPosX < x2) {
						++lastPosX;
					} else if (lastPosZ < z2) {
						lastPosX = x1;
						++lastPosZ;
					} else if (lastPosY > y1) {
						lastPosX = x1;
						lastPosZ = z1;
						--lastPosY;
					}

					return new BlockPos(lastPosX, lastPosY, lastPosZ);
				}
			}
		};
	}
}
