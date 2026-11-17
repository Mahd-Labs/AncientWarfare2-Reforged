package net.shadowmage.ancientwarfare.automation.render;

import java.io.InputStream;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.render.lib.OBJParser;
import net.shadowmage.ancientwarfare.core.render.lib.RedundantTransformation;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.core.Direction;
import net.shadowmage.ancientwarfare.core.render.RotatableBlockRenderer;
import net.shadowmage.ancientwarfare.core.render.lib.CCModel;
import net.shadowmage.ancientwarfare.core.render.lib.CCRenderState;
import net.shadowmage.ancientwarfare.core.render.lib.LightModel;
import net.shadowmage.ancientwarfare.core.render.lib.Transformation;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;



public abstract class AnimatedBlockRenderer extends RotatableBlockRenderer implements ITESRRenderer {
	protected AnimatedBlockRenderer(String modelPath) {
		super(modelPath);
	}

	/*
	 * Port addition: 1.12 OBJParser.parseModels(resourceLocation, 7, new RedundantTransformation()) for extra model files
	 */
	protected static Map<String, CCModel> parseModel(String modelPath) {
		String path = "/assets/" + AncientWarfareCore.MOD_ID + "/models/block/" + modelPath;
		try (InputStream input = AnimatedBlockRenderer.class.getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalStateException("model file not found: " + path);
			}
			return OBJParser.parseModels(input, new RedundantTransformation());
		}
		catch (Exception e) {
			throw new RuntimeException("failed to load model: " + path, e);
		}
	}

	protected Collection<CCModel> removeGroups(Function<String, Boolean> filter) {
		return removeGroups(groups, filter);
	}

	protected Collection<CCModel> removeGroups(Map<String, CCModel> objGroups, Function<String, Boolean> filter) {
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

	protected Collection<CCModel> rotateModels(Collection<CCModel> groups, Direction frontFacing, Transformation transform) {
		return groups.stream().map(e -> rotateFacing(e.copy().apply(transform), frontFacing)).collect(Collectors.toSet());
	}

	@Override
	public void renderTransformedBlockModels(CCRenderState ccrs, IExtendedBlockState state) {
		Collection<CCModel> transformedGroups = applyModelTransforms(groups.values(), null, state);


		for (CCModel group : transformedGroups) {
			group.render(ccrs, LightModel.standardLightModel, getIconTransform(state));
		}
	}
}
