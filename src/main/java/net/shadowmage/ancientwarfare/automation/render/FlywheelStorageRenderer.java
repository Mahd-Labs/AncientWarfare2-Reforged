package net.shadowmage.ancientwarfare.automation.render;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.shadowmage.ancientwarfare.automation.block.TorqueTier;
import net.shadowmage.ancientwarfare.automation.item.ItemBlockFlywheelStorage;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.tile.torque.multiblock.TileFlywheelStorage;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.render.BaseBakery;
import net.shadowmage.ancientwarfare.core.render.lib.CCModel;
import net.shadowmage.ancientwarfare.core.render.lib.CCRenderState;
import net.shadowmage.ancientwarfare.core.render.lib.IconTransformation;
import net.shadowmage.ancientwarfare.core.render.lib.LightModel;
import net.shadowmage.ancientwarfare.core.render.lib.OBJParser;
import net.shadowmage.ancientwarfare.core.render.lib.RedundantTransformation;
import net.shadowmage.ancientwarfare.core.render.lib.Rotation;
import net.shadowmage.ancientwarfare.core.render.lib.Transformation;
import net.shadowmage.ancientwarfare.core.render.lib.Translation;
import net.shadowmage.ancientwarfare.core.render.lib.Vector3;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties.IS_CONTROL;

/*
 * Port notes: 1.12 ILayeredBlockBakery (solid layer: case bars and shrouds, translucent layer: windows with 25% alpha).
 * The port's BakeryModel has no layers: the static parts are baked in one go and the block uses the translucent
 * render type (set in AncientWarfareAutomationClient); the windows get their alpha through CCRenderState.alpha
 * (1.12: CCModel.setColour). The spinning parts are drawn by FlywheelStorageAnimationRenderer as before.
 */
public class FlywheelStorageRenderer extends BaseBakery implements ITESRRenderer {
	public static final FlywheelStorageRenderer INSTANCE = new FlywheelStorageRenderer();
	private static final float WINDOW_ALPHA = 0.25f;

	private Collection<CCModel> spindleSmall;
	private Collection<CCModel> upperShroudSmall;
	private Collection<CCModel> lowerShroudSmall;
	private Collection<CCModel> flywheelExtensionSmall;
	private Collection<CCModel> lowerWindowSmall;
	private Collection<CCModel> upperWindowSmall;
	private Collection<CCModel> caseBarsSmall;

	private Collection<CCModel> spindleLarge;
	private Collection<CCModel> upperShroudLarge;
	private Collection<CCModel> lowerShroudLarge;
	private Collection<CCModel> flywheelExtensionLarge;
	private Collection<CCModel> lowerWindowLarge;
	private Collection<CCModel> upperWindowLarge;
	private Collection<CCModel> caseBarsLarge;

	private Map<Pair<Boolean, TorqueTier>, IconTransformation> iconTransformations = Maps.newHashMap();
	private Map<Pair<Boolean, TorqueTier>, TextureAtlasSprite> sprites = Maps.newHashMap();

	private enum Layer {
		SOLID, TRANSLUCENT
	}

	public void setSprite(boolean large, TorqueTier tier, TextureAtlasSprite sprite) {
		sprites.put(new ImmutablePair<>(large, tier), sprite);
		iconTransformations.put(new ImmutablePair<>(large, tier), new IconTransformation(sprite));
	}

	public TextureAtlasSprite getSprite(TorqueTier tier) {
		return sprites.get(new ImmutablePair<>(false, tier));
	}

	private IconTransformation getIconTransformation(boolean large, TorqueTier tier) {
		return iconTransformations.get(new ImmutablePair<>(large, tier));
	}

	private FlywheelStorageRenderer() {
		super("automation/flywheel_small.obj");
		Map<String, CCModel> smallModel = groups;
		flywheelExtensionSmall = removeGroups(smallModel, s -> s.startsWith("spindle.flywheelExtension."));
		spindleSmall = removeGroups(smallModel, s -> s.startsWith("spindle."));
		upperShroudSmall = removeGroups(smallModel, s -> s.startsWith("shroudUpper."));
		lowerShroudSmall = removeGroups(smallModel, s -> s.startsWith("shroudLower."));
		lowerWindowSmall = removeGroups(smallModel, s -> s.startsWith("windowLower."));
		upperWindowSmall = removeGroups(smallModel, s -> s.startsWith("windowUpper."));
		caseBarsSmall = removeGroups(smallModel, s -> s.startsWith("caseBars."));

		Map<String, CCModel> largeModel = loadModel("flywheel_large.obj");
		flywheelExtensionLarge = removeGroups(largeModel, s -> s.startsWith("spindle.flywheelExtension."));
		spindleLarge = removeGroups(largeModel, s -> s.startsWith("spindle."));
		upperShroudLarge = removeGroups(largeModel, s -> s.startsWith("shroudUpper."));
		lowerShroudLarge = removeGroups(largeModel, s -> s.startsWith("shroudLower."));
		lowerWindowLarge = removeGroups(largeModel, s -> s.startsWith("windowLower."));
		upperWindowLarge = removeGroups(largeModel, s -> s.startsWith("windowUpper."));
		caseBarsLarge = removeGroups(largeModel, s -> s.startsWith("caseBars."));
	}

	private Map<String, CCModel> loadModel(String modelName) {
		String path = "/assets/" + AncientWarfareCore.MOD_ID + "/models/block/automation/" + modelName;
		Map<String, CCModel> ret;
		try (InputStream input = FlywheelStorageRenderer.class.getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalStateException("model file not found: " + path);
			}
			ret = OBJParser.parseModels(input, new RedundantTransformation());
		}
		catch (Exception e) {
			throw new RuntimeException("failed to load model: " + path, e);
		}
		for (Map.Entry<String, CCModel> group : ret.entrySet()) {
			group.setValue(group.getValue().backfacedCopy().computeNormals());
		}
		return ret;
	}

	private Collection<CCModel> removeGroups(Map<String, CCModel> objGroups, Function<String, Boolean> filter) {
		Set<CCModel> ret = Sets.newHashSet();
		Iterator<Map.Entry<String, CCModel>> iterator = objGroups.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, CCModel> entry = iterator.next();
			if (filter.apply(entry.getKey())) {
				ret.add(entry.getValue());
				iterator.remove();
			}
		}
		return ret;
	}

	private Collection<CCModel> transformModels(Collection<CCModel> groups, Transformation transform) {
		return groups.stream().map(e -> e.copy().apply(transform)).collect(Collectors.toSet());
	}

	@Override
	public List<BakedQuad> bakeItemQuads(@Nullable Direction face, ItemStack stack) {
		CCRenderState ccrs = CCRenderState.instance();
		ccrs.reset();
		ccrs.startBaking(false);
		TorqueTier tier = stack.getItem() instanceof ItemBlockFlywheelStorage ? ((ItemBlockFlywheelStorage) stack.getItem()).getTier() : TorqueTier.LIGHT;
		renderModels(getTransformedModels(Layer.SOLID, false, true, 0f, 1), ccrs, false, tier);
		renderModels(getTransformedModels(Layer.SOLID, false, false, 0f, 1), ccrs, false, tier);
		ccrs.alpha = WINDOW_ALPHA;
		renderModels(getTransformedModels(Layer.TRANSLUCENT, false, false, 0f, 1), ccrs, false, tier);
		ccrs.alpha = 1.f;
		return ccrs.bake();
	}

	@Override
	public List<BakedQuad> bakeQuads(@Nullable Direction face, IExtendedBlockState state) {
		if (face != null || !state.getValue(AutomationProperties.IS_CONTROL)) {
			return Collections.emptyList();
		}
		CCRenderState ccrs = CCRenderState.instance();
		ccrs.reset();
		ccrs.startBaking(true);
		boolean largeModel = state.getValue(AutomationProperties.WIDTH) > 1;
		TorqueTier tier = state.getValue(AutomationProperties.TIER);
		renderModels(getTransformedModels(Layer.SOLID, state), ccrs, largeModel, tier);
		ccrs.alpha = WINDOW_ALPHA;
		renderModels(getTransformedModels(Layer.TRANSLUCENT, state), ccrs, largeModel, tier);
		ccrs.alpha = 1.f;
		return ccrs.bake();
	}

	private Set<CCModel> getTransformedModels(Layer layer, IExtendedBlockState state) {
		if (!state.getValue(IS_CONTROL)) {
			return Collections.emptySet();
		}
		boolean largeModel = state.getValue(AutomationProperties.WIDTH) > 1;
		boolean displayDynamicParts = state.getValue(AutomationProperties.DYNAMIC);
		float rotation = state.getValue(AutomationProperties.ROTATION);
		int height = state.getValue(AutomationProperties.HEIGHT);

		return getTransformedModels(layer, largeModel, displayDynamicParts, rotation, height);
	}

	private Set<CCModel> getTransformedModels(Layer layer, boolean largeModel, boolean displayDynamicParts, float rotation, int height) {
		Set<CCModel> transformedGroups = Sets.newHashSet();
		Collection<CCModel> spindle = largeModel ? spindleLarge : spindleSmall;
		Collection<CCModel> flywheelExtension = largeModel ? flywheelExtensionLarge : flywheelExtensionSmall;
		Collection<CCModel> caseBars = largeModel ? caseBarsLarge : caseBarsSmall;
		Collection<CCModel> lowerWindow = largeModel ? lowerWindowLarge : lowerWindowSmall;
		Collection<CCModel> upperShroud = largeModel ? upperShroudLarge : upperShroudSmall;
		Collection<CCModel> lowerShroud = largeModel ? lowerShroudLarge : lowerShroudSmall;
		Collection<CCModel> upperWindow = largeModel ? upperWindowLarge : upperWindowSmall;

		if (displayDynamicParts) {
			if (layer == Layer.SOLID) {
				Transformation rotationTransform = new Rotation(rotation, 0, 1, 0).at(Vector3.center);
				for (int i = 0; i < height; i++) {
					Translation translation = new Translation(0, i, 0);
					transformedGroups.addAll(transformModels(spindle, translation.with(rotationTransform)));
					if (i < height - 1) {
						transformedGroups.addAll(transformModels(flywheelExtension, translation.with(rotationTransform)));//at every level less than highest
					}
				}
			}
		} else {
			for (int i = 0; i < height; i++) {
				Translation translation = new Translation(0, i, 0);
				if (layer == Layer.SOLID) {
					transformedGroups.addAll(transformModels(caseBars, translation));
					if (i == height - 1) {
						transformedGroups.addAll(transformModels(upperShroud, translation));//at highest level
					}
					if (i == 0) {
						transformedGroups.addAll(transformModels(lowerShroud, translation));//at ground level
					}
				} else {
					transformedGroups.addAll(transformModels(lowerWindow, translation));
					if (i < height - 1) {
						transformedGroups.addAll(transformModels(upperWindow, translation));
					}
				}
			}
		}
		return transformedGroups;
	}

	@Override
	public IExtendedBlockState handleState(IExtendedBlockState state, BlockGetter access, BlockPos pos) {
		Optional<TileFlywheelStorage> te = WorldTools.getTile(access, pos, TileFlywheelStorage.class);

		boolean isControl = true;
		int height = 1;
		int width = 1;
		if (te.isPresent()) {
			TileFlywheelStorage storage = te.get();
			isControl = storage.isControl;
			width = storage.setWidth;
			height = storage.setHeight;
		}

		IExtendedBlockState updatedState = state.withProperty(AutomationProperties.DYNAMIC, false);
		updatedState = updatedState.withProperty(AutomationProperties.IS_CONTROL, isControl);
		updatedState = updatedState.withProperty(AutomationProperties.HEIGHT, height);
		updatedState = updatedState.withProperty(AutomationProperties.WIDTH, width);
		updatedState = updatedState.withProperty(AutomationProperties.ROTATION, 0f);

		return updatedState;
	}

	private void renderModels(Collection<CCModel> modelGroups, CCRenderState ccrs, boolean large, TorqueTier tier) {
		for (CCModel group : modelGroups) {
			group.render(ccrs, getIconTransformation(large, tier));
		}
	}

	@Override
	public void renderTransformedBlockModels(CCRenderState ccrs, IExtendedBlockState state) {
		boolean largeModel = state.getValue(AutomationProperties.WIDTH) > 1;
		TorqueTier tier = state.getValue(AutomationProperties.TIER);
		Set<CCModel> modelGroups = getTransformedModels(Layer.SOLID, state);
		for (CCModel group : modelGroups) {
			group.render(ccrs, LightModel.standardLightModel, getIconTransformation(largeModel, tier));
		}
	}
}
