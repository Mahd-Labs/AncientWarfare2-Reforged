package net.shadowmage.ancientwarfare.structure.template.build;

import net.neoforged.bus.api.Event;
import net.minecraft.world.level.biome.Biome;
import net.shadowmage.ancientwarfare.structure.block.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.api.IStructureBuilder;
import net.shadowmage.ancientwarfare.structure.api.TemplateRule;
import net.shadowmage.ancientwarfare.structure.api.TemplateRuleBlock;
import net.shadowmage.ancientwarfare.structure.api.TemplateRuleEntityBase;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.shadowmage.ancientwarfare.structure.template.build.validation.properties.StructureValidationProperties.BIOME_REPLACEMENT;

public class StructureBuilder implements IStructureBuilder {

	protected StructureTemplate template;
	protected Level world;
	BlockPos buildOrigin;
	Direction buildFace;
	protected int turns;
	int maxPriority = 3;
	int currentPriority;//current build priority...may not be needed anymore?
	Vec3i curTempPos;
	BlockPos destination;

	protected StructureBB bb;

	private boolean isFinished = false;
	private boolean budgetedStarted = false;
	private ResumableSteps budgetedBefore;
	private ResumableSteps budgetedAfter;
	private boolean budgetedBlocksDone = false;
	private int beforeCalls;
	private int afterCalls;
	private long beforeNanos;
	private long afterNanos;
	private long longestBeforeNanos;
	private long longestAfterNanos;
	private net.minecraft.core.Holder<Biome> biome;
	private Map<BlockPos, BlockState> statesToSetAgain = new HashMap<>();
	private Map<BlockPos, BlockState> positionsToUpdate = new HashMap<>();

	public StructureBuilder(Level world, StructureTemplate template, Direction face, BlockPos pos) {
		this(world, template, face, pos, new StructureBB(pos, face, template));
	}

	public StructureBuilder(Level world, StructureTemplate template, Direction face, BlockPos buildKey, StructureBB bb) {
		this.world = world;
		biome = world.getBiome(buildKey);
		this.template = template;
		this.buildFace = face;
		this.bb = bb;
		buildOrigin = buildKey;
		destination = BlockPos.ZERO;
		curTempPos = Vec3i.ZERO;
		currentPriority = 0;

		turns = ((face.get2DDataValue() + 2) % 4);
		/*
		 * initialize the first target destination so that the structure is ready to start building when called on to build
		 */
		incrementDestination();
	}

	public StructureTemplate getTemplate() {
		return template;
	}

	public StructureBB getBoundingBox() {
		return bb;
	}

	protected StructureBuilder() {
		destination = BlockPos.ZERO;
		buildOrigin = BlockPos.ZERO;
	}

	public void instantConstruction() {
		while (!this.isFinished()) {
			Optional<TemplateRuleBlock> rule = template.getRuleAt(curTempPos);
			if (rule.isPresent()) {
				placeCurrentPosition(rule.get());
			} else if (currentPriority == 0) {
				placeAir();
			}
			increment();
		}
		setStateAgainForSpecialBlocks();
		updateNeighbors();
		changeBiome();

		this.placeEntities();
	}

	/*
	 * Improvement (not baseline, budgeted_placement): the body of instantConstruction split over several calls. Places
	 * positions in exactly the order of instantConstruction (all passes) until the deadline (System.nanoTime) has
	 * passed, checked every 64 positions; returns true once the structure is complete, after the same final steps
	 * (rails set again, neighbour updates, biome, entities). unlimited = run to the end. Called by BudgetedBuild only;
	 * instantConstruction is unchanged and stays the 1.12 path.
	 * What a subclass does before / after the blocks (the world generation builder: the validator's pre- and
	 * postGeneration) runs as resumable steps under the same deadline: before -> blocks -> after, each complete before
	 * the next one starts.
	 */
	public boolean constructUntil(long deadlineNanos, boolean unlimited) {
		if (!budgetedStarted) {
			budgetedStarted = true;
			budgetedBefore = beforeConstructionSteps();
		}
		if (budgetedBefore != null) {
			long start = System.nanoTime();
			boolean done = budgetedBefore.run(deadlineNanos, unlimited);
			long time = System.nanoTime() - start;
			beforeCalls++;
			beforeNanos += time;
			longestBeforeNanos = Math.max(longestBeforeNanos, time);
			if (!done) {
				return false;
			}
			budgetedBefore = null;
		}
		if (!budgetedBlocksDone && !constructBlocksUntil(deadlineNanos, unlimited)) {
			return false;
		}
		if (!budgetedBlocksDone) {
			budgetedBlocksDone = true;
			budgetedAfter = afterConstructionSteps();
		}
		if (budgetedAfter != null) {
			long start = System.nanoTime();
			boolean done = budgetedAfter.run(deadlineNanos, unlimited);
			long time = System.nanoTime() - start;
			afterCalls++;
			afterNanos += time;
			longestAfterNanos = Math.max(longestAfterNanos, time);
			if (!done) {
				return false;
			}
			budgetedAfter = null;
		}
		return true;
	}

	/* diagnostics (debug log): one position or step of a budgeted build that took more than 50 ms */
	private void logSlowUnit(long start, int chunksBefore, String what) {
		long time = System.nanoTime() - start;
		if (time > 50000000L) {
			net.shadowmage.ancientwarfare.structure.AncientWarfareStructure.LOG.debug("Slow unit in budgeted build of {}: {} took {} ms, new chunks loaded since the unit started {}", template.name, what, time / 1000000,
					net.shadowmage.ancientwarfare.structure.worldgen.WorldGenTickHandler.INSTANCE.getNewChunkLoads() - chunksBefore);
		}
	}

	private boolean constructBlocksUntil(long deadlineNanos, boolean unlimited) {
		int count = 0;
		while (!this.isFinished()) {
			if (!unlimited && (count & 63) == 63 && System.nanoTime() - deadlineNanos >= 0) {
				return false;
			}
			count++;
			long unitStart = System.nanoTime();
			int chunksBefore = net.shadowmage.ancientwarfare.structure.worldgen.WorldGenTickHandler.INSTANCE.getNewChunkLoads();
			Optional<TemplateRuleBlock> rule = template.getRuleAt(curTempPos);
			if (rule.isPresent()) {
				placeCurrentPosition(rule.get());
			} else if (currentPriority == 0) {
				placeAir();
			}
			logSlowUnit(unitStart, chunksBefore, rule.map(r -> "rule " + r.getClass().getSimpleName()).orElse("air") + " at " + destination + " pass " + currentPriority);
			increment();
		}
		long unitStart = System.nanoTime();
		int chunksBefore = net.shadowmage.ancientwarfare.structure.worldgen.WorldGenTickHandler.INSTANCE.getNewChunkLoads();
		setStateAgainForSpecialBlocks();
		logSlowUnit(unitStart, chunksBefore, "setStateAgainForSpecialBlocks");
		unitStart = System.nanoTime();
		updateNeighbors();
		logSlowUnit(unitStart, chunksBefore, "updateNeighbors");
		unitStart = System.nanoTime();
		changeBiome();
		logSlowUnit(unitStart, chunksBefore, "changeBiome");
		unitStart = System.nanoTime();
		this.placeEntities();
		logSlowUnit(unitStart, chunksBefore, "placeEntities");
		return true;
	}

	/*
	 * budgeted placement: what a subclass's instantConstruction does before / after the blocks, as resumable steps
	 */
	protected ResumableSteps beforeConstructionSteps() {
		return new ResumableSteps();
	}

	protected ResumableSteps afterConstructionSteps() {
		return new ResumableSteps();
	}

	/* budgeted placement statistics: calls (ticks) that worked on the steps before / after the blocks, time spent */
	public int getBeforeCalls() {
		return beforeCalls;
	}

	public int getAfterCalls() {
		return afterCalls;
	}

	public long getBeforeNanos() {
		return beforeNanos;
	}

	public long getAfterNanos() {
		return afterNanos;
	}

	public long getLongestBeforeNanos() {
		return longestBeforeNanos;
	}

	public long getLongestAfterNanos() {
		return longestAfterNanos;
	}

	/* budgeted placement: blocks around the bounding box that the build's chunk ticket covers */
	public int getChunkMargin() {
		return Math.max(0, template.getValidationSettings().getBorderSize());
	}

	public Level getWorld() {
		return world;
	}

	private void changeBiome() {
		ResourceLocation biomeRegistryName = template.getValidationSettings().getPropertyValue(BIOME_REPLACEMENT);
		if (biomeRegistryName == null) {
			return;
		}
		//Port notes: per column WorldTools.changeBiome -> BiomeChanger (one refill per chunk); 1.12 biome names are translated
		net.shadowmage.ancientwarfare.structure.util.BiomeChanger.create(world, biomeRegistryName).ifPresent(changer -> {
			BlockPos minPos = bb.min;
			BlockPos maxPos = new BlockPos(bb.max.getX(), bb.min.getY(), bb.max.getZ());
			BlockPos.betweenClosed(minPos, maxPos).forEach(pos -> {
				if (isTopBlockSolid(world, pos)) {
					changer.addColumn(pos);
				}
			});
			changer.apply();
		});
	}

	private boolean isTopBlockSolid(Level world, BlockPos pos) {
		//Port notes: 1.12 walked down from chunk.getTopFilledSegment() + 16 and skipped air, leaves and foliage
		BlockPos posDown;
		int top = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) + 1;
		for (BlockPos currentPos = new BlockPos(pos.getX(), top, pos.getZ()); currentPos.getY() >= world.getMinBuildHeight(); currentPos = posDown) {
			posDown = currentPos.below();
			BlockState state = world.getBlockState(posDown);

			if (!state.isAir() && !state.is(net.minecraft.tags.BlockTags.LEAVES) && !(state.getBlock() instanceof net.minecraft.world.level.block.BushBlock)) {
				return !state.liquid();
			}
		}
		return false;
	}

	private void updateNeighbors() {
		for (Map.Entry<BlockPos, BlockState> entry : positionsToUpdate.entrySet()) {
			world.updateNeighborsAt(entry.getKey(), entry.getValue().getBlock());

			if (entry.getValue().hasAnalogOutputSignal()) {
				world.updateNeighbourForOutputSignal(entry.getKey(), entry.getValue().getBlock());
			}
			//schedule update spread in the next 40 ticks in case we have a lot of redstone somewhere
			world.scheduleTick(entry.getKey(), entry.getValue().getBlock(), world.random.nextInt(40));
		}
	}

	private void setStateAgainForSpecialBlocks() {
		for (Map.Entry<BlockPos, BlockState> entry : statesToSetAgain.entrySet()) {
			world.setBlock(entry.getKey(), entry.getValue(), 2);
		}
	}

	private void placeEntities() {
		for (TemplateRuleEntityBase rule : template.getEntityRules().values()) {
			destination = BlockTools.rotateInArea(rule.blockPosition(), template.getSize().getX(), template.getSize().getZ(), turns).offset(bb.min);
			rule.handlePlacement(world, turns, destination, this);
		}
	}

	/*
	 * should be called by template-rules to handle block-placement in the world.
	 * Handles village-block swapping during world-gen, and chunk-insert for blocks
	 * with priority > 0
	 */
	@Override
	public boolean placeBlock(BlockPos pos, BlockState state, int priority) {
		if (pos.getY() <= world.getMinBuildHeight() || pos.getY() >= world.getMaxBuildHeight()) { //1.12: y <= 0 || y >= world.getHeight()
			return false;
		}

		BlockState adjustedState = state;
		if (template.getValidationSettings().isBlockSwap()) {
			adjustedState = getBiomeSpecificBlockState(biome, state);
		}

		//Port addition: the upper half of a 1.12 double plant takes the plant of the lower half (1.12 getActualState)
		adjustedState = net.shadowmage.ancientwarfare.structure.util.LegacyActualStates.beforePlacement(world, pos, adjustedState);
		boolean result = world.setBlock(pos, adjustedState, 2);
		if (result) {
			if (DOUBLE_SET_BLOCKS.contains(adjustedState.getBlock())) {
				statesToSetAgain.put(pos, adjustedState);
			}
			if (state.isSignalSource()) {
				positionsToUpdate.put(pos, adjustedState);
			}
			//Port addition: the values 1.12 derived from the neighbours (connections, stair corners, double chests)
			net.shadowmage.ancientwarfare.structure.util.LegacyActualStates.afterPlacement(world, pos, adjustedState);
		}

		return result;
	}

	private void placeCurrentPosition(TemplateRule rule) {
		if (rule.shouldPlaceOnBuildPass(world, turns, destination, currentPriority)) {
			this.placeRule(rule);
		}
	}

	protected boolean increment() {
		if (isFinished) {
			return false;
		}
		if (incrementPosition()) {
			incrementDestination();
		} else {
			this.isFinished = true;
		}
		return !isFinished;
	}

	private void placeAir() {
		if (!template.getValidationSettings().isPreserveBlocks()) {
			world.removeBlock(destination, false);
		}
	}

	void placeRule(TemplateRule rule) {
		if (destination.getY() <= world.getMinBuildHeight()) { //1.12: y <= 0
			return;
		}
		rule.handlePlacement(world, turns, destination, this);
	}

	void incrementDestination() {
		destination = BlockTools.rotateInArea(new BlockPos(curTempPos), template.getSize().getX(), template.getSize().getZ(), turns).offset(bb.min);
	}

	/*
	 * return true if could increment position
	 * return false if template is finished
	 */
	private boolean incrementPosition() {
		int currentX = curTempPos.getX();
		int currentY = curTempPos.getY();
		int currentZ = curTempPos.getZ();
		currentX++;
		if (currentX >= template.getSize().getX()) {
			currentX = 0;
			currentZ++;
			if (currentZ >= template.getSize().getZ()) {
				currentZ = 0;
				currentY++;
				if (currentY >= template.getSize().getY()) {
					currentY = 0;
					currentPriority++;
					if (currentPriority > maxPriority) {
						currentPriority = 0;
						return false;
					}
				}
			}
		}
		curTempPos = new Vec3i(currentX, currentY, currentZ);
		return true;
	}

	public boolean isFinished() {
		return isFinished;
	}

	float getTotalBlocks() {
		return (float) template.getSize().getX() * template.getSize().getZ() * template.getSize().getY();
	}

	public float getPercentDoneWithPass() {
		float max = getTotalBlocks();
		float current = (float) curTempPos.getY() * (template.getSize().getX() * template.getSize().getZ());//add layers done
		current += curTempPos.getZ() * template.getSize().getX();//add rows done
		current += curTempPos.getX();//add blocks done
		return current / max;
	}

	public int getPass() {
		return currentPriority;
	}

	public int getMaxPasses() {
		return maxPriority;
	}

	/*
	 * Port notes: 1.12 first posted the Forge BiomeEvent.GetVillageBlockID (gone) and then swapped by biome class
	 * (BiomeDesert / BiomeTaiga / BiomeSavanna). Biome classes are gone too: desert = the desert biome, taiga and
	 * savanna = the vanilla biome tags. The swaps are the same: logs, planks, oak (desert: also cobblestone) stairs,
	 * savanna: cobblestone -> upright acacia log. Like 1.12 only the stair facing is carried over.
	 */
	private BlockState getBiomeSpecificBlockState(net.minecraft.core.Holder<Biome> biome, BlockState originalBlockState) {
		Set<IBlockSwapMapping> mappings = biome.is(net.minecraft.world.level.biome.Biomes.DESERT) ? DESERT_SWAPS :
				biome.is(net.minecraft.tags.BiomeTags.IS_TAIGA) ? TAIGA_SWAPS : biome.is(net.minecraft.tags.BiomeTags.IS_SAVANNA) ? SAVANNA_SWAPS : null;
		if (mappings != null) {
			for (IBlockSwapMapping mapping : mappings) {
				if (mapping.matches(originalBlockState)) {
					return mapping.swap(originalBlockState);
				}
			}
		}

		return originalBlockState;
	}

	private static final Set<Block> DOUBLE_SET_BLOCKS = ImmutableSet.of(Blocks.RAIL, Blocks.ACTIVATOR_RAIL, Blocks.DETECTOR_RAIL, Blocks.POWERED_RAIL);

	private static BlockState stairs(Block stairs, BlockState from) {
		return stairs.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, from.getValue(net.minecraft.world.level.block.StairBlock.FACING));
	}

	private static BlockState log(Block log, BlockState from) {
		net.minecraft.core.Direction.Axis axis = from.hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) ? from.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) : net.minecraft.core.Direction.Axis.Y;
		return log.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, axis);
	}

	private static final Predicate<BlockState> IS_LOG = s -> s.is(net.minecraft.tags.BlockTags.LOGS);
	private static final Predicate<BlockState> IS_PLANKS = s -> s.is(net.minecraft.tags.BlockTags.PLANKS);

	private static final Set<IBlockSwapMapping> DESERT_SWAPS = ImmutableSet.of(
			new BlockSwapMapping(IS_LOG, s -> Blocks.SANDSTONE.defaultBlockState()),
			new BlockSwapMapping(s -> s.is(Blocks.COBBLESTONE), s -> Blocks.SANDSTONE.defaultBlockState()),
			new BlockSwapMapping(IS_PLANKS, s -> Blocks.CUT_SANDSTONE.defaultBlockState()),
			new BlockSwapMapping(s -> s.is(Blocks.OAK_STAIRS), s -> stairs(Blocks.SANDSTONE_STAIRS, s)),
			new BlockSwapMapping(s -> s.is(Blocks.COBBLESTONE_STAIRS), s -> stairs(Blocks.SANDSTONE_STAIRS, s)));
	private static final Set<IBlockSwapMapping> TAIGA_SWAPS = ImmutableSet.of(
			new BlockSwapMapping(IS_LOG, s -> log(Blocks.SPRUCE_LOG, s)),
			new BlockSwapMapping(IS_PLANKS, s -> Blocks.SPRUCE_PLANKS.defaultBlockState()),
			new BlockSwapMapping(s -> s.is(Blocks.OAK_STAIRS), s -> stairs(Blocks.SPRUCE_STAIRS, s)));
	private static final Set<IBlockSwapMapping> SAVANNA_SWAPS = ImmutableSet.of(
			new BlockSwapMapping(IS_LOG, s -> log(Blocks.ACACIA_LOG, s)),
			new BlockSwapMapping(IS_PLANKS, s -> Blocks.ACACIA_PLANKS.defaultBlockState()),
			new BlockSwapMapping(s -> s.is(Blocks.OAK_STAIRS), s -> stairs(Blocks.ACACIA_STAIRS, s)),
			new BlockSwapMapping(s -> s.is(Blocks.COBBLESTONE), s -> Blocks.ACACIA_LOG.defaultBlockState()));

	public Direction getBuildFace() {
		return buildFace;
	}

	// @formatter:on

	private interface IBlockSwapMapping {
		boolean matches(BlockState state);

		BlockState swap(BlockState state);
	}

	private static class BlockSwapMapping implements IBlockSwapMapping {
		private final Predicate<BlockState> blockMatcher;
		private final Function<BlockState, BlockState> doSwap;

		private BlockSwapMapping(Predicate<BlockState> blockMatcher, Function<BlockState, BlockState> doSwap) {
			this.blockMatcher = blockMatcher;
			this.doSwap = doSwap;
		}

		@Override
	public boolean matches(BlockState state) {
			return blockMatcher.test(state);
		}

		@Override
	public BlockState swap(BlockState state) {
			return doSwap.apply(state);
		}
	}
}
