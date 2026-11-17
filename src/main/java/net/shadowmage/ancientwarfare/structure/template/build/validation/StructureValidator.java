package net.shadowmage.ancientwarfare.structure.template.build.validation;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.biome.Biome;
import net.shadowmage.ancientwarfare.structure.block.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.tile.worksite.treefarm.DefaultTreeScanner;
import net.shadowmage.ancientwarfare.automation.tile.worksite.treefarm.ITree;
import net.shadowmage.ancientwarfare.automation.tile.worksite.treefarm.ITreeScanner;
import net.shadowmage.ancientwarfare.structure.config.AWStructureStatics;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.template.build.validation.properties.IStructureValidationProperty;
import net.shadowmage.ancientwarfare.structure.worldgen.WorldGenDetailedLogHelper;
import net.shadowmage.ancientwarfare.structure.worldgen.WorldStructureGenerator;
import net.shadowmage.ancientwarfare.structure.worldgen.stats.PlacementRejectionReason;
import net.shadowmage.ancientwarfare.structure.worldgen.stats.WorldGenStatistics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.shadowmage.ancientwarfare.structure.template.build.validation.properties.StructureValidationProperties.*;

public abstract class StructureValidator {
	private static final int CLEAR_TREE_MAX_BORDER_DISTANCE = 10;
	public final StructureValidationType validationType;

	private boolean riverBiomeChecked = false;
	private boolean canSpawnInRiverBiome = false;

	private HashMap<IStructureValidationProperty<?>, Object> properties = new LinkedHashMap<>();

	protected StructureValidator(StructureValidationType validationType) {
		this.validationType = validationType;
		for (IStructureValidationProperty property : validationType.getValidationProperties()) {
			properties.put(property, property.getDefaultValue());
		}
	}

	/*
	 * should be called from validator setup GUI when swapping between validator types,
	 * to transfer any comparable settings from the old one to the new one.<br>
	 * This method should be called on the NEW StructureValidator.
	 */
	public void inheritPropertiesFrom(StructureValidator validator) {
		for (IStructureValidationProperty<?> property : properties.keySet()) {
			if (validator.properties.containsKey(property)) {
				properties.put(property, validator.properties.get(property));
			}
		}
	}

	/*
	 * helper method to read data from tag -- to be overriden by
	 * child-classes that have additional validation data set through gui
	 */
	public final void readFromNBT(CompoundTag tag) {
		for (Map.Entry<IStructureValidationProperty<?>, Object> entry : properties.entrySet()) {
			entry.setValue(entry.getKey().deserializeNBT(tag));
		}
	}

	public final CompoundTag serializeToNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString("validationType", validationType.getName());
		for (Map.Entry<IStructureValidationProperty<?>, Object> entry : properties.entrySet()) {
			serializePropertyNBT(tag, entry.getKey(), entry.getValue());
		}
		return tag;
	}

	private <T> void serializePropertyNBT(CompoundTag tag, IStructureValidationProperty<T> property, Object value) {
		property.serializeNBT(tag, property.getValueClass().cast(value));
	}

	protected void setDefaultSettings(StructureTemplate template) {
	}

	/*
	 * should this template be included for selection for generation? should only validate block placement, most other stuff has been checked (dimension/biome/cluster value/etc)
	 */
	public abstract boolean shouldIncludeForSelection(Level world, int x, int y, int z, Direction face, StructureTemplate template);

	/*
	 * if template should be included for selection, get the adjusted spawn Y level from the input block position.  this adjustedY will be used for validation and generation if template is selected and validated
	 */
	@SuppressWarnings("squid:S1172")
	public int getAdjustedSpawnY(Level world, int x, int y, int z, Direction face, StructureTemplate template, StructureBB bb) {
		return y;
	}

	/*
	 * if selected for placement, validate that placement. return false if placement is invalid
	 */
	public abstract boolean validatePlacement(Level world, int x, int y, int z, Direction face, StructureTemplate template, StructureBB bb);

	/*
	 * Improvement (not baseline, background_attempt_chunks): true when validatePlacement reads nothing but the columns of
	 * the border ring (validateBorderBlocks), so only the chunks of that ring are prepared for the attempt; false (the
	 * default) prepares the whole bounding box plus border
	 */
	public boolean readsBorderRingOnly() {
		return false;
	}

	/*
	 * after validation, do any necessary clearing or leveling/etc
	 */
	public abstract void preGeneration(Level world, BlockPos pos, Direction face, StructureTemplate template, StructureBB bb);

	public void postGeneration(Level world, BlockPos origin, StructureBB bb, StructureTemplate template) {

	}

	/*
	 * called from StructureBuilder when constructed with world-gen settings whenever a '0' rule is detected
	 * in the template
	 * implementations should fill the input x,y,z with whatever block is an appropriate 'fill' for that
	 * validation type -- e.g. air or water
	 */
	@SuppressWarnings("squid:S1172")
	public void handleClearAction(Level world, BlockPos pos, StructureTemplate template, StructureBB bb) {
		if (isPreserveBlocks()) {
			return;
		}

		BlockState state = world.getBlockState(pos);
		if (!state.isAir()) {
			if (state.is(net.minecraft.tags.BlockTags.LOGS)) {
				ITreeScanner treeScanner = new DefaultTreeScanner(st -> st.is(net.minecraft.tags.BlockTags.LOGS), sl -> sl.is(net.minecraft.tags.BlockTags.LEAVES), DefaultTreeScanner.ALL_AROUND, 6);
				ITree tree = treeScanner.scanTree(world, pos,
						bb.min.offset(-CLEAR_TREE_MAX_BORDER_DISTANCE, 0, -CLEAR_TREE_MAX_BORDER_DISTANCE),
						bb.max.offset(CLEAR_TREE_MAX_BORDER_DISTANCE, 0, CLEAR_TREE_MAX_BORDER_DISTANCE));
				tree.getLeafPositions().forEach(p -> world.removeBlock(p, false));
				tree.getTrunkPositions().forEach(p -> world.removeBlock(p, false));
				return;
			}
			world.removeBlock(pos, false);
		}
	}

	public static StructureValidator parseValidator(List<String> lines) {
		StructureValidationType type = StructureValidationType.GROUND;

		Iterator<String> it = lines.iterator();
		if (it.hasNext()) {
			type = parseType(it.next());
		}

		StructureValidator validator = type.getValidator();

		while (it.hasNext()) {
			parseLine(it.next(), validator::parsePropertyValue);
		}

		//defaulting templates to whitelist overworld dimension if no dimension list provided and set to blacklist
		if (validator.getAcceptedDimensions().length == 0) {
			validator.setPropertyValue(DIMENSION_WHITE_LIST, true);
			validator.setPropertyValue(DIMENSION_LIST, new int[] {0});
		}

		return validator;
	}

	private void parsePropertyValue(String name, String value) {
		for (IStructureValidationProperty<?> property : properties.keySet()) {
			if (property.getName().equalsIgnoreCase(name)) {
				parsePropertyValue(property, value);
			}
		}
	}

	private <T> void parsePropertyValue(IStructureValidationProperty<T> property, String stringValue) {
		T value = property.parseValue(stringValue);
		setPropertyValue(property, value);
	}

	@SuppressWarnings("squid:S4784")
	private static final Pattern NAME_VALUE_MATCHER = Pattern.compile("([^=]*)=([^=]*)");

	private static void parseLine(String line, BiConsumer<String, String> parseNameValue) {
		Matcher matcher = NAME_VALUE_MATCHER.matcher(line);
		if (matcher.matches()) {
			parseNameValue.accept(matcher.group(1), matcher.group(2));
		}
	}

	private static StructureValidationType parseType(String line) {
		Matcher matcher = NAME_VALUE_MATCHER.matcher(line);
		if (matcher.matches() && matcher.group(1).equalsIgnoreCase("type")) {
			return StructureValidationType.getTypeFromName(matcher.group(2)).orElse(StructureValidationType.GROUND);
		}
		return StructureValidationType.GROUND;
	}

	private static <T> String getStringValue(StructureValidator validator, IStructureValidationProperty<T> property) {
		T value = validator.getPropertyValue(property);
		return property.getStringValue(value);
	}

	public static void writeValidator(BufferedWriter out, StructureValidator validator) throws IOException {
		out.write("type=" + validator.validationType.getName());
		out.newLine();
		for (IStructureValidationProperty<?> property : validator.properties.keySet()) {
			out.write(property.getName() + "=" + getStringValue(validator, property));
			out.newLine();
		}
	}

	public <T> void setPropertyValue(IStructureValidationProperty<T> property, T value) {
		if (!properties.containsKey(property)) {
			throw new IllegalArgumentException("Unable to update property - validator doesn't have property: " + property.getName());
		}
		properties.put(property, value);
	}

	public <T> T getPropertyValue(IStructureValidationProperty<T> property) {
		if (!properties.containsKey(property)) {
			throw new IllegalArgumentException("Unable to get property value - validator doesn't have property: " + property.getName());
		}
		return property.getValueClass().cast(properties.get(property));
	}

	public final StructureValidator setDefaults(StructureTemplate template) {
		setDefaultSettings(template);
		return this;
	}

	public final void setBiomeWhiteList(boolean val) {
		setPropertyValue(BIOME_WHITE_LIST, val);
	}

	public final void setDimensionWhiteList(boolean val) {
		setPropertyValue(DIMENSION_WHITE_LIST, val);
	}

	public final boolean isBlockSwap() {
		return getPropertyValue(BLOCK_SWAP);
	}

	public final int getSelectionWeight() {
		return getPropertyValue(SELECTION_WEIGHT);
	}

	public final int getClusterValue() {
		return getPropertyValue(CLUSTER_VALUE);
	}

	public final boolean isWorldGenEnabled() {
		return getPropertyValue(WORLD_GEN);
	}

	public final boolean isPreserveBlocks() {
		return getPropertyValue(PRESERVE_BLOCKS);
	}

	public final boolean shouldPreventHostileSpawns() {
		return getPropertyValue(PREVENT_NATURAL_HOSTILE_SPAWNS);
	}

	public final boolean isBiomeWhiteList() {
		return getPropertyValue(BIOME_WHITE_LIST);
	}

	public final boolean isUnique() {
		return getPropertyValue(UNIQUE);
	}

	public final boolean isDimensionWhiteList() {
		return getPropertyValue(DIMENSION_WHITE_LIST);
	}

	public final int[] getAcceptedDimensions() {
		return getPropertyValue(DIMENSION_LIST);
	}

	public final void setValidDimension(Set<Integer> dims) {
		int[] dimsa = new int[dims.size()];
		int index = 0;
		for (Integer dim : dims) {
			dimsa[index] = dim;
			index++;
		}
		setPropertyValue(DIMENSION_LIST, dimsa);
	}

	public final int getMinDuplicateDistance() {
		return getPropertyValue(MIN_DUPLICATE_DISTANCE);
	}

	public final void setBiomeList(Set<String> biomes) {
		setPropertyValue(BIOME_LIST, biomes);
	}

	public Set<String> getBiomeList() {
		//noinspection unchecked
		return getPropertyValue(BIOME_LIST);
	}

	int getMaxFill() {
		return getPropertyValue(MAX_FILL);
	}

	int getMaxLeveling() {
		return getPropertyValue(MAX_LEVELING);
	}

	public int getBorderSize() {
		return getPropertyValue(BORDER_SIZE);
	}

	//*********************************************** UTILITY METHODS *************************************************//
	boolean validateBorderBlocks(Level world, StructureBB bb, int minY, int maxY, boolean skipWater) {
		int bx;
		int bz;
		int borderSize = getBorderSize();

		for (bx = bb.min.getX() - borderSize; bx <= bb.max.getX() + borderSize; bx++) {
			bz = bb.min.getZ() - borderSize;
			if (!validateBlockHeightTypeAndBiome(world, bx, bz, minY, maxY, skipWater)) {
				return false;
			}
			bz = bb.max.getZ() + borderSize;
			if (!validateBlockHeightTypeAndBiome(world, bx, bz, minY, maxY, skipWater)) {
				return false;
			}
		}
		for (bz = bb.min.getZ() - borderSize + 1; bz <= bb.max.getZ() + borderSize - 1; bz++) {
			bx = bb.min.getX() - borderSize;
			if (!validateBlockHeightTypeAndBiome(world, bx, bz, minY, maxY, skipWater)) {
				return false;
			}
			bx = bb.max.getX() + borderSize;
			if (!validateBlockHeightTypeAndBiome(world, bx, bz, minY, maxY, skipWater)) {
				return false;
			}
		}
		return true;
	}

	private boolean canSpawnInRiverBiome() {
		if (!riverBiomeChecked) {
			//noinspection unchecked
			canSpawnInRiverBiome = getPropertyValue(BIOME_WHITE_LIST) && net.shadowmage.ancientwarfare.structure.util.LegacyBiomes.translate(getBiomeList()).stream().anyMatch(name -> {
				//Port notes: BiomeDictionary.Type.RIVER -> the vanilla is_river biome tag (looked up in the builtin registry data of the server)
				net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
				return server != null && server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, ResourceLocation.parse((String) name))).map(b -> b.is(net.minecraft.tags.BiomeTags.IS_RIVER)).orElse(false);
			});
			riverBiomeChecked = true;
		}

		return canSpawnInRiverBiome;
	}

	/*
	 * validates both top block height and block type for the input position and settings
	 */
	boolean validateBlockHeightAndType(Level world, int x, int z, int min, int max, boolean skipWater, Predicate<BlockState> isValidState) {
		return validateBlockType(world, x, validateBlockHeight(world, x, z, min, max, skipWater), z, isValidState);
	}

	private boolean validateBlockHeightTypeAndBiome(Level world, int x, int z, int min, int max, boolean skipWater, Predicate<BlockState> isValidState) {
		BlockPos pos = new BlockPos(x, 1, z);
		if (!canSpawnInRiverBiome() && net.shadowmage.ancientwarfare.structure.util.LegacyBiomes.getSurfaceBiome(world, x, z).is(net.minecraft.tags.BiomeTags.IS_RIVER)) {
			WorldGenDetailedLogHelper.log("Rejected for placement into river biome at {}", () -> pos);
			return false;
		}

		return validateBlockHeightAndType(world, x, z, min, max, skipWater, isValidState);
	}

	boolean validateBlockHeightTypeAndBiome(Level world, int x, int z, int min, int max, boolean skipWater) {
		return validateBlockHeightTypeAndBiome(world, x, z, min, max, skipWater, AWStructureStatics::isValidTargetBlock);
	}

	/*
	 * validates top block height at X, Z is >=  min and <= max (inclusive)
	 * returns topFoundY or -1 if not within range
	 */
	private int validateBlockHeight(Level world, int x, int z, int minimumAcceptableY, int maximumAcceptableY, boolean skipWater) {
		int topFilledY = WorldStructureGenerator.getTargetY(world, x, z, skipWater);
		if (topFilledY < minimumAcceptableY || topFilledY > maximumAcceptableY) {
			WorldGenDetailedLogHelper.log("Rejected for leveling or depth test. foundY {} min {} max {} at: x {} y {} z {}", () -> topFilledY, () -> minimumAcceptableY, () -> maximumAcceptableY, () -> x, () -> topFilledY, () -> z);
			return -1;
		}
		return topFilledY;
	}

	/*
	 * validates the target block at x,y,z is one of the input valid blocks
	 */
	private boolean validateBlockType(Level world, int x, int y, int z, Predicate<BlockState> isValidState) {
		if (y <= world.getMinBuildHeight() || y >= world.getMaxBuildHeight()) { //1.12: y <= 0 || y >= world.getHeight()
			return false;
		}
		BlockState state = world.getBlockState(new BlockPos(x, y, z));
		Block block = state.getBlock();
		if (!isValidState.test(state)) {
			WorldGenDetailedLogHelper.log("Rejected because of invalid target block \"{}\" at: x {} y {} z {} ", () -> BuiltInRegistries.BLOCK.getKey(block), () -> x, () -> y, () -> z);
			return false;
		}
		return true;
	}

	/*
	 * return the lowest acceptable Y level for a filled block
	 * for the input template and BB
	 */
	int getMinY(StructureTemplate template, StructureBB bb) {
		int minY = bb.min.getY() - getMaxFill() - 1;
		if (getBorderSize() > 0) {
			minY += template.getOffset().getY();
		}
		return minY;
	}

	/*
	 * return the highest acceptable Y level for a filled block
	 * for the input template and BB
	 */
	int getMaxY(StructureTemplate template, StructureBB bb) {
		return bb.min.getY() + template.getOffset().getY() + getMaxLeveling();
	}

	private int getMaxFillY(StructureTemplate template, StructureBB bb) {
		return getMinY(template, bb) + getMaxFill();
	}

	private void borderLeveling(Level world, int x, int z, StructureTemplate template, StructureBB bb) {
		if (getMaxLeveling() <= 0) {
			return;
		}
		int topFilledY = WorldStructureGenerator.getTargetY(world, x, z, true);
		int step = WorldStructureGenerator.getStepNumber(x, z, bb.min.getX(), bb.max.getX(), bb.min.getZ(), bb.max.getZ());
		for (int y = bb.min.getY() + template.getOffset().getY() + step; y <= topFilledY; y++) {
			handleClearAction(world, new BlockPos(x, y, z), template, bb);
		}
		BlockState fillBlock = net.shadowmage.ancientwarfare.structure.util.BiomeBlocks.getTopBlock(world, new BlockPos(x, 1, z));
		int y = bb.min.getY() + template.getOffset().getY() + step - 1;
		BlockPos pos = new BlockPos(x, y, z);
		BlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if (block != Blocks.AIR && !state.getFluidState().is(net.minecraft.tags.FluidTags.WATER) && !AWStructureStatics.isSkippable(state)) {
			world.setBlockAndUpdate(pos, fillBlock);
		}
	}

	private void borderFill(Level world, int x, int z, StructureTemplate template, StructureBB bb) {
		if (getMaxFill() <= 0) {
			return;
		}
		int maxFillY = getMaxFillY(template, bb);
		int step = WorldStructureGenerator.getStepNumber(x, z, bb.min.getX(), bb.max.getX(), bb.min.getZ(), bb.max.getZ());
		maxFillY -= step;
		BlockState fillBlock = net.shadowmage.ancientwarfare.structure.util.BiomeBlocks.getTopBlock(world, new BlockPos(x, 1, z));
		for (int y = maxFillY; y > 1; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			BlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			if (AWStructureStatics.isSkippable(state) || block == Blocks.WATER) {
				world.setBlockAndUpdate(pos, fillBlock);
			}
		}
	}

	private void underFill(Level world, int x, int z, StructureBB bb) {
		int topFilledY = WorldStructureGenerator.getTargetY(world, x, z, true);
		BlockState fillBlock = net.shadowmage.ancientwarfare.structure.util.BiomeBlocks.getTopBlock(world, new BlockPos(x, 1, z));
		for (int y = topFilledY; y <= bb.min.getY() - 1; y++) {
			world.setBlockAndUpdate(new BlockPos(x, y, z), fillBlock);
		}
	}

	void prePlacementUnderfill(Level world, StructureBB bb) {
		int bx;
		int bz;
		for (bx = bb.min.getX(); bx <= bb.max.getX(); bx++) {
			for (bz = bb.min.getZ(); bz <= bb.max.getZ(); bz++) {
				underFill(world, bx, bz, bb);
			}
		}
	}

	void prePlacementBorder(Level world, StructureTemplate template, StructureBB bb) {
		int borderSize = getBorderSize();
		if (borderSize <= 0) {
			return;
		}
		int bx;
		int bz;
		for (bx = bb.min.getX() - borderSize; bx <= bb.max.getX() + borderSize; bx++) {
			for (bz = bb.max.getZ() + borderSize; bz > bb.max.getZ(); bz--) {
				borderLeveling(world, bx, bz, template, bb);
				borderFill(world, bx, bz, template, bb);
			}
			for (bz = bb.min.getZ() - borderSize; bz < bb.min.getZ(); bz++) {
				borderLeveling(world, bx, bz, template, bb);
				borderFill(world, bx, bz, template, bb);
			}
		}
		for (bz = bb.min.getZ(); bz <= bb.max.getZ(); bz++) {
			for (bx = bb.min.getX() - borderSize; bx < bb.min.getX(); bx++) {
				borderLeveling(world, bx, bz, template, bb);
				borderFill(world, bx, bz, template, bb);
			}
			for (bx = bb.max.getX() + borderSize; bx > bb.max.getX(); bx--) {
				borderLeveling(world, bx, bz, template, bb);
				borderFill(world, bx, bz, template, bb);
			}
		}
	}

	/*
	 * Improvement (not baseline, budgeted_placement): preGeneration / postGeneration as resumable steps, used by the
	 * budgeted build (StructureBuilderWorldGen) only. Each validator that does work there overrides these with the
	 * same loops in the same order cut into units (one column or one position each); the default runs the original
	 * method as a single unit. With the switch off, preGeneration / postGeneration are called unchanged.
	 */
	public net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps preGenerationSteps(Level world, BlockPos pos, Direction face, StructureTemplate template, StructureBB bb) {
		return net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps.of(() -> preGeneration(world, pos, face, template, bb));
	}

	public net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps postGenerationSteps(Level world, BlockPos origin, StructureBB bb, StructureTemplate template) {
		return net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps.of(() -> postGeneration(world, origin, bb, template));
	}

	/* prePlacementUnderfill, one unit per column, same order */
	java.util.List<Runnable> prePlacementUnderfillUnits(Level world, StructureBB bb) {
		java.util.List<Runnable> units = new java.util.ArrayList<>();
		int bx;
		int bz;
		for (bx = bb.min.getX(); bx <= bb.max.getX(); bx++) {
			for (bz = bb.min.getZ(); bz <= bb.max.getZ(); bz++) {
				int x = bx;
				int z = bz;
				units.add(() -> underFill(world, x, z, bb));
			}
		}
		return units;
	}

	/* prePlacementBorder, one unit per column (leveling + fill), same order */
	java.util.List<Runnable> prePlacementBorderUnits(Level world, StructureTemplate template, StructureBB bb) {
		java.util.List<Runnable> units = new java.util.ArrayList<>();
		int borderSize = getBorderSize();
		if (borderSize <= 0) {
			return units;
		}
		int bx;
		int bz;
		for (bx = bb.min.getX() - borderSize; bx <= bb.max.getX() + borderSize; bx++) {
			for (bz = bb.max.getZ() + borderSize; bz > bb.max.getZ(); bz--) {
				addBorderUnit(units, world, bx, bz, template, bb);
			}
			for (bz = bb.min.getZ() - borderSize; bz < bb.min.getZ(); bz++) {
				addBorderUnit(units, world, bx, bz, template, bb);
			}
		}
		for (bz = bb.min.getZ(); bz <= bb.max.getZ(); bz++) {
			for (bx = bb.min.getX() - borderSize; bx < bb.min.getX(); bx++) {
				addBorderUnit(units, world, bx, bz, template, bb);
			}
			for (bx = bb.max.getX() + borderSize; bx > bb.max.getX(); bx--) {
				addBorderUnit(units, world, bx, bz, template, bb);
			}
		}
		return units;
	}

	private void addBorderUnit(java.util.List<Runnable> units, Level world, int x, int z, StructureTemplate template, StructureBB bb) {
		units.add(() -> {
			borderLeveling(world, x, z, template, bb);
			borderFill(world, x, z, template, bb);
		});
	}

	public boolean isSurvival() {
		return getPropertyValue(SURVIVAL);
	}

	public Set<String> getBiomeGroupList() {
		//noinspection unchecked
		return getPropertyValue(BIOME_GROUP_LIST);
	}

	public void setBiomeGroupList(Set<String> biomeGroups) {
		setPropertyValue(BIOME_GROUP_LIST, biomeGroups);
	}
}
