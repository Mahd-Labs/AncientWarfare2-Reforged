package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shadowmage.ancientwarfare.core.block.BlockBase;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Port notes: besides being the common base (1.12: creative tab + client state mapper to blockstates/structure/<name>,
 * now blockstates/<name>.json and AWStructureTab), this class bridges the 1.12 block callbacks the structure blocks
 * were written against to their 1.21 counterparts, so the subclasses keep their original method bodies:
 *   getBoundingBox / getCollisionBoundingBox (AABB)  -> getShape / getCollisionShape (cached VoxelShape)
 *   getShapeBoxes (1.12 collisionRayTrace + addCollisionBoxToList over several AABBs) -> union VoxelShape
 *   onBlockActivated(.., hitX, hitY, hitZ) : boolean -> useItemOn (true = success, false = pass on to the item)
 *   onBlockPlacedBy -> setPlacedBy, getPickBlock -> getCloneItemStack
 *   getActualState (connection style properties computed from neighbours) -> stored in the state: applied after
 *     placement and in updateShape
 *   getDrops(drops, world, pos, state, fortune) -> getDrops(state, LootParams.Builder) when a subclass overrides it,
 *     otherwise the loot table
 *   random ticks call tick() like 1.12 updateTick
 */
public class BlockBaseStructure extends BlockBase {
	public static final AABB FULL_BLOCK_AABB = new AABB(0, 0, 0, 1, 1, 1);
	@Nullable
	public static final AABB NULL_AABB = null;
	private static final Map<AABB, VoxelShape> SHAPES = new ConcurrentHashMap<>();
	private static final Map<List<AABB>, VoxelShape> MULTI_SHAPES = new ConcurrentHashMap<>();

	public BlockBaseStructure(BlockBehaviour.Properties material, String regName) {
		super(material, AncientWarfareStructure.MOD_ID, regName);
		registerDefaultState(saneDefaultState(stateDefinition.any()));
	}

	/*
	 * Port notes: the 1.12 blocks never set a default state (the base state = first value of every property, e.g.
	 * "invisible=true", "on_fire=true"); what was placed came from getStateFromMeta(0) / onBlockPlacedBy. /setblock
	 * and the item now use the default state, so it is the state 1.12 meta 0 gave: the visible main block, not
	 * burning, not transparent, with legs.
	 * Two properties keep the 1.12 base state value instead, because that is what 1.12 getDefaultState() showed and
	 * every placement path sets them explicitly anyway: "lit" (true: brazier, ember brazier, fire pit; a stack
	 * without the lit tag still places the unlit block as 1.12 meta 0 did, see their onBlockPlacedBy) and the top /
	 * bottom "part" (top: iron cage, gibbet, wooden post, stake, thrones).
	 */
	private static final String[][] DEFAULTS = {{"visible", "true"}, {"invisible", "false"}, {"on_fire", "false"},
			{"transparent", "false"}, {"double", "false"}, {"bottom_part", "true"}, {"part", "south"}, {"legs", "both"},
			{"leg_northeast", "true"}, {"leg_northwest", "true"}, {"leg_southeast", "true"}, {"leg_southwest", "true"}};

	private BlockState saneDefaultState(BlockState state) {
		for (String[] def : DEFAULTS) {
			net.minecraft.world.level.block.state.properties.Property<?> property = stateDefinition.getProperty(def[0]);
			if (property != null) {
				state = withValue(state, property, def[1]);
			}
		}
		return state;
	}

	private static <T extends Comparable<T>> BlockState withValue(BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property, String value) {
		return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
	}

	public static VoxelShape shapeOf(@Nullable AABB aabb) {
		return aabb == null ? Shapes.empty() : SHAPES.computeIfAbsent(aabb, BlockBaseStructure::createShape);
	}

	/*
	 * 1.12 boxes may be flat (the gate proxy collision planes); a VoxelShape can not, so a flat side gets a minimal
	 * thickness. A box that is flat in all three directions stays empty.
	 */
	private static VoxelShape createShape(AABB aabb) {
		boolean flatX = aabb.getXsize() < 1.0E-7;
		boolean flatY = aabb.getYsize() < 1.0E-7;
		boolean flatZ = aabb.getZsize() < 1.0E-7;
		if ((flatX || flatY || flatZ) && !(flatX && flatY && flatZ)) {
			aabb = aabb.inflate(flatX ? 0.001 : 0, flatY ? 0.001 : 0, flatZ ? 0.001 : 0);
		}
		return Shapes.create(aabb);
	}

	public static VoxelShape shapeOf(List<AABB> aabbs) {
		VoxelShape cached = MULTI_SHAPES.get(aabbs);
		if (cached != null) {
			return cached;
		}
		VoxelShape shape = Shapes.empty();
		for (AABB aabb : aabbs) {
			shape = Shapes.or(shape, createShape(aabb));
		}
		MULTI_SHAPES.put(List.copyOf(aabbs), shape);
		return shape;
	}

	/* 1.12 Block.getBoundingBox */
	public AABB getBoundingBox(BlockState state, BlockGetter source, BlockPos pos) {
		return FULL_BLOCK_AABB;
	}

	/* 1.12 Block.getCollisionBoundingBox (NULL_AABB = no collision) */
	@Nullable
	public AABB getCollisionBoundingBox(BlockState state, BlockGetter world, BlockPos pos) {
		return getBoundingBox(state, world, pos);
	}

	/*
	 * 1.12 blocks that ray traced / collided against several boxes (collisionRayTrace + addCollisionBoxToList):
	 * return the boxes here, null = single box from getBoundingBox
	 */
	@Nullable
	protected List<AABB> getShapeBoxes(BlockState state, BlockGetter world, BlockPos pos) {
		return null;
	}

	/* 1.12 addCollisionBoxToList; null = single box from getCollisionBoundingBox (the 1.12 default) */
	@Nullable
	protected List<AABB> getCollisionBoxes(BlockState state, BlockGetter world, BlockPos pos) {
		return null;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		List<AABB> boxes = getShapeBoxes(state, level, pos);
		return boxes != null ? shapeOf(boxes) : shapeOf(getBoundingBox(state, level, pos));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (!hasCollision) {
			return Shapes.empty();
		}
		List<AABB> boxes = getCollisionBoxes(state, level, pos);
		return boxes != null ? shapeOf(boxes) : shapeOf(getCollisionBoundingBox(state, level, pos));
	}

	/* 1.12 Block.onBlockActivated */
	public boolean onBlockActivated(Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		return false;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		Vec3 loc = hit.getLocation();
		boolean result = onBlockActivated(level, pos, state, player, hand, hit.getDirection(), (float) (loc.x - pos.getX()), (float) (loc.y - pos.getY()), (float) (loc.z - pos.getZ()));
		return result ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	/* 1.12 Block.getStateForPlacement */
	public BlockState getStateForPlacement(Level world, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, int meta, LivingEntity placer) {
		return defaultBlockState();
	}

	/* 1.12 Block.canPlaceBlockAt */
	public boolean canPlaceBlockAt(Level world, BlockPos pos) {
		return world.getBlockState(pos).canBeReplaced();
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!canPlaceBlockAt(level, pos)) {
			return null;
		}
		Player placer = context.getPlayer();
		if (placer == null) {
			return defaultBlockState();
		}
		Vec3 loc = context.getClickLocation();
		return getStateForPlacement(level, pos, context.getClickedFace(), (float) (loc.x - pos.getX()), (float) (loc.y - pos.getY()), (float) (loc.z - pos.getZ()), 0, placer);
	}

	/* 1.12 Block.dropBlockAsItem */
	public void dropBlockAsItem(Level world, BlockPos pos, BlockState state, int fortune) {
		Block.dropResources(state, world, pos);
	}

	/* 1.12 activateOther(otherState.getBlock(), ..) on the other half of a two block structure */
	protected static boolean activateOther(Block block, Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		return block instanceof BlockBaseStructure && ((BlockBaseStructure) block).onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
	}

	/* 1.12 Block.onBlockPlacedBy */
	public void onBlockPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (placer != null) {
			onBlockPlacedBy(level, pos, state, placer, stack);
		}
		refreshActualState(level, pos);
	}

	/* 1.12 Block.getActualState: properties that are derived from the neighbours / the tile */
	public BlockState getActualState(BlockState state, BlockGetter world, BlockPos pos) {
		return state;
	}

	public void refreshActualState(Level level, BlockPos pos) {
		BlockState current = level.getBlockState(pos);
		if (current.getBlock() == this) {
			BlockState actual = getActualState(current, level, pos);
			if (actual != current) {
				level.setBlock(pos, actual, 3);
			}
		}
	}

	/* tiles call this after changing a value that getActualState shows in the block state */
	public static void refresh(BlockEntity te) {
		Level level = te.getLevel();
		if (level != null && !level.isClientSide && te.getBlockState().getBlock() instanceof BlockBaseStructure) {
			((BlockBaseStructure) te.getBlockState().getBlock()).refreshActualState(level, te.getBlockPos());
		}
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		return getActualState(state, level, pos);
	}

	/* 1.12 Block.getPickBlock */
	public ItemStack getPickBlock(BlockState state, HitResult target, Level world, BlockPos pos, Player player) {
		return new ItemStack(this);
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
		return level instanceof Level ? getPickBlock(state, target, (Level) level, pos, player) : new ItemStack(this);
	}

	/*
	 * 1.12 Block.getDrops, for the blocks that built their drop in code (variant / tile data in the stack NBT). Blocks
	 * that do not override it use their loot table. The world is the level the block was in; the block entity (already
	 * removed from the world when drops are collected) is available through getDropTile().
	 */
	public void getDrops(NonNullList<ItemStack> drops, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
	}

	private static final ThreadLocal<BlockEntity> DROP_TILE = new ThreadLocal<>();

	/* the block entity of the block whose drops are being collected (1.12 code read it from the world) */
	@Nullable
	protected static BlockEntity getDropTile() {
		return DROP_TILE.get();
	}

	protected static <T> java.util.Optional<T> getDropTile(BlockGetter world, BlockPos pos, Class<T> teClass) {
		BlockEntity te = DROP_TILE.get();
		if (te == null) {
			te = world.getBlockEntity(pos);
		}
		return teClass.isInstance(te) ? java.util.Optional.of(teClass.cast(te)) : java.util.Optional.empty();
	}

	private Boolean customDrops;

	private boolean hasCustomDrops() {
		if (customDrops == null) {
			boolean custom = false;
			try {
				custom = getClass().getMethod("getDrops", NonNullList.class, BlockGetter.class, BlockPos.class, BlockState.class, int.class).getDeclaringClass() != BlockBaseStructure.class;
			}
			catch (NoSuchMethodException e) {
				//not possible, declared above
			}
			customDrops = custom;
		}
		return customDrops;
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		if (!hasCustomDrops()) {
			return super.getDrops(state, params);
		}
		NonNullList<ItemStack> drops = NonNullList.create();
		Vec3 origin = params.getOptionalParameter(LootContextParams.ORIGIN);
		BlockPos pos = origin == null ? BlockPos.ZERO : BlockPos.containing(origin);
		DROP_TILE.set(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY));
		try {
			getDrops(drops, params.getLevel(), pos, state, 0);
		}
		finally {
			DROP_TILE.remove();
		}
		return drops;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		tick(state, level, pos, random);
	}
}
