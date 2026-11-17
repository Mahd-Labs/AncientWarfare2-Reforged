package net.shadowmage.ancientwarfare.core.render;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.render.lib.CCModel;
import net.shadowmage.ancientwarfare.core.render.lib.CCRenderState;
import net.shadowmage.ancientwarfare.core.render.lib.IconTransformation;
import net.shadowmage.ancientwarfare.core.render.lib.OBJParser;
import net.shadowmage.ancientwarfare.core.render.lib.RedundantTransformation;
import net.shadowmage.ancientwarfare.core.render.lib.Transformation;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/*
 * CLIENT ONLY. 1.12: a CodeChickenLib ISimpleBlockBakery. See core/render/README.md for how the pieces map.
 * Method names and the override points are the 1.12 ones; types changed as follows:
 *   IExtendedBlockState (Forge)  -> core.render.property.IExtendedBlockState
 *   IBlockAccess                 -> BlockGetter
 *   EnumFacing                   -> Direction
 *   CCModel, CCRenderState, IconTransformation, Transformation... -> core.render.lib.* (same names)
 *   PerspectiveProperties        -> ItemTransforms
 * The model file is read from the mod jar (assets/ancientwarfare/models/block/<modelPath>) when the bakery is
 * constructed, so the 1.12 pattern of static INSTANCE fields with group filtering in the constructor still works.
 */
public abstract class BaseBakery {

	public IExtendedBlockState handleState(IExtendedBlockState state, BlockGetter access, BlockPos pos) {
		return state;
	}

	protected Map<String, CCModel> groups;

	public TextureAtlasSprite sprite;
	protected IconTransformation iconTransform;

	protected BaseBakery(String modelPath) {
		String path = "/assets/" + AncientWarfareCore.MOD_ID + "/models/block/" + modelPath;
		try (InputStream input = BaseBakery.class.getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalStateException("model file not found: " + path);
			}
			groups = OBJParser.parseModels(input, getBaseTransformation());
		}
		catch (Exception e) {
			throw new RuntimeException("failed to load model: " + path, e);
		}
		for (Map.Entry<String, CCModel> group : groups.entrySet()) {
			group.setValue(group.getValue().backfacedCopy().computeNormals());
		}
	}

	protected Transformation getBaseTransformation() {
		return new RedundantTransformation();
	}

	public void setSprite(TextureAtlasSprite textureAtlasSprite) {
		sprite = textureAtlasSprite;
		iconTransform = new IconTransformation(sprite);
	}

	private static final ItemTransforms MODEL_PROPERTIES;

	static {
		//1.12: TransformUtils.create(tx, ty, tz, rx, ry, rz, scale) per TransformType (translation in 1/16 block, rotation in degrees).
		//1.12 registered flipLeft(thirdPerson) for the left hand and Forge mirrored left hand matrices once more; vanilla 1.21.1
		//mirrors the left hand transform itself, so third person left is entered pre-mirrored to end up with the same matrix.
		ItemTransform thirdPerson = create(0, 2.5f, 0, 75, 225, 0, 0.375f);
		ItemTransform thirdPersonLeft = create(0, 2.5f, 0, 75, -225, 0, 0.375f);
		ItemTransform gui = create(0, 0, 0, 30, 225, 0, 0.625f);
		ItemTransform ground = create(0, 3, 0, 0, 0, 0, 0.25f);
		ItemTransform fixed = create(0, 0, 0, 0, 0, 0, 0.5f);
		ItemTransform firstPersonRight = create(0, 0, 0, 0, 135, 0, 0.4f);
		ItemTransform firstPersonLeft = create(0, 0, 0, 0, 135, 0, 0.4f);
		MODEL_PROPERTIES = new ItemTransforms(thirdPersonLeft, thirdPerson, firstPersonLeft, firstPersonRight, ItemTransform.NO_TRANSFORM, gui, ground, fixed);
	}

	private static ItemTransform create(float tx, float ty, float tz, float rx, float ry, float rz, float s) {
		return new ItemTransform(new Vector3f(rx, ry, rz), new Vector3f(tx / 16, ty / 16, tz / 16), new Vector3f(s, s, s));
	}

	public List<BakedQuad> bakeQuads(@Nullable Direction face, IExtendedBlockState state) {
		if (face != null) {
			return Collections.emptyList();
		}

		CCRenderState ccrs = CCRenderState.instance();
		ccrs.reset();
		ccrs.startBaking(true);
		Collection<CCModel> transformedGroups = applyModelTransforms(groups.values(), face, state);
		renderBlockModels(transformedGroups, ccrs, face, state);

		return ccrs.bake();
	}

	protected Collection<CCModel> applyModelTransforms(Collection<CCModel> modelGroups, Direction face, IExtendedBlockState state) {
		return modelGroups;
	}

	protected void renderBlockModels(Collection<CCModel> modelGroups, CCRenderState ccrs, Direction face, IExtendedBlockState state) {
		renderAllModels(modelGroups, ccrs, state);
	}

	private void renderAllModels(Collection<CCModel> modelGroups, CCRenderState ccrs, IExtendedBlockState state) {
		for (CCModel group : modelGroups) {
			group.render(ccrs, getIconTransform(state));
		}
	}

	private void renderAllModels(CCRenderState ccrs, ItemStack stack) {
		for (CCModel group : groups.values()) {
			group.render(ccrs, getIconTransform(stack));
		}
	}

	protected IconTransformation getIconTransform(IExtendedBlockState state) {
		return iconTransform;
	}

	protected IconTransformation getIconTransform(ItemStack stack) {
		return iconTransform;
	}

	protected void renderItemModels(CCRenderState ccrs, ItemStack stack) {
		renderAllModels(ccrs, stack);
	}

	public List<BakedQuad> bakeItemQuads(@Nullable Direction face, ItemStack stack) {
		CCRenderState ccrs = CCRenderState.instance();
		ccrs.reset();
		ccrs.startBaking(false);
		renderItemModels(ccrs, stack);
		return ccrs.bake();
	}

	public ItemTransforms getModelProperties(ItemStack stack) {
		return MODEL_PROPERTIES;
	}
}
