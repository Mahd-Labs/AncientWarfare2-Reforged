package net.shadowmage.ancientwarfare.automation.render;

import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import org.joml.Vector3f;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.tile.torque.multiblock.TileWindmillBlade;
import net.shadowmage.ancientwarfare.core.render.lib.CCModel;
import net.shadowmage.ancientwarfare.core.render.lib.CCRenderState;
import net.shadowmage.ancientwarfare.core.render.lib.IconTransformation;
import net.shadowmage.ancientwarfare.core.render.lib.OBJParser;
import net.shadowmage.ancientwarfare.core.render.lib.RedundantTransformation;
import net.shadowmage.ancientwarfare.core.render.lib.Rotation;
import net.shadowmage.ancientwarfare.core.render.lib.Transformation;
import net.shadowmage.ancientwarfare.core.render.lib.Translation;
import net.shadowmage.ancientwarfare.core.render.lib.Vector3;
import net.shadowmage.ancientwarfare.core.render.property.CoreProperties;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;
import net.shadowmage.ancientwarfare.core.util.Trig;
import net.shadowmage.ancientwarfare.core.util.WorldTools;



public class WindmillBladeRenderer extends AnimatedBlockRenderer {

	public static final WindmillBladeRenderer INSTANCE = new WindmillBladeRenderer();

	private static final ItemTransforms MODEL_PROPERTIES;

	static {
		//same conventions as core BaseBakery (1.12 TransformUtils.create values; third person left pre-mirrored)
		ItemTransform thirdPerson = create(0, 2.5f, 0, 75, 225, 0, 0.375f);
		ItemTransform thirdPersonLeft = create(0, 2.5f, 0, 75, -225, 0, 0.375f);
		ItemTransform gui = create(0, 0, 0, 30, 225, 90, 0.320f);
		ItemTransform ground = create(0, 3, 0, 0, 0, 0, 0.125f);
		ItemTransform fixed = create(0, 0, 0, 0, 0, 0, 0.25f);
		ItemTransform firstPerson = create(0, 0, 0, 0, 135, 0, 0.2f);
		MODEL_PROPERTIES = new ItemTransforms(thirdPersonLeft, thirdPerson, firstPerson, firstPerson, ItemTransform.NO_TRANSFORM, gui, ground, fixed);
	}

	private static ItemTransform create(float tx, float ty, float tz, float rx, float ry, float rz, float s) {
		return new ItemTransform(new Vector3f(rx, ry, rz), new Vector3f(tx / 16, ty / 16, tz / 16), new Vector3f(s, s, s));
	}

	private final Collection<CCModel> bladeShaft;
	private final Collection<CCModel> windmillShaft;
	private final Collection<CCModel> blade;
	private final Collection<CCModel> bladeJoint;
	private final CCModel cube;
	public TextureAtlasSprite cubeSprite;
	private IconTransformation cubeIconTransform;

	private WindmillBladeRenderer() {
		super("automation/windmill_blade.obj");
		cube = parseModel("automation/windmill_blade_cube.obj").values().iterator().next().backfacedCopy();
		bladeShaft = removeGroups(s -> s.startsWith("bladeShaft."));
		windmillShaft = transformModels(removeGroups(s -> s.startsWith("windmillShaft.")), new Translation(0d, 0.5d, 0d));
		blade = removeGroups(s -> s.startsWith("blade."));
		bladeJoint = removeGroups(s -> s.startsWith("bladeJoint."));
	}

	private Collection<CCModel> transformModels(Collection<CCModel> groups, Transformation transform) {
		return groups.stream().map(e -> e.copy().apply(transform)).collect(Collectors.toSet());
	}

	public void setCubeSprite(TextureAtlasSprite sprite) {
		this.cubeSprite = sprite;
		cubeIconTransform = new IconTransformation(this.cubeSprite);
	}

	@Override
	public List<BakedQuad> bakeQuads(@Nullable Direction face, IExtendedBlockState state) {
		if (state.getValue(AutomationProperties.FORMED) && (!state.getValue(AutomationProperties.DYNAMIC) || !state.getValue(AutomationProperties.IS_CONTROL))) {
			return Collections.emptyList(); //formed blade doesn't have static rendering and only control gets rendered
		}

		return super.bakeQuads(face, state);
	}

	@Override
	protected void renderBlockModels(Collection<CCModel> modelGroups, CCRenderState ccrs, Direction face, IExtendedBlockState state) {
		if (!state.getValue(AutomationProperties.FORMED)) {
			cube.render(ccrs, cubeIconTransform);
		} else {
			super.renderBlockModels(modelGroups, ccrs, face, state);
		}
	}

	@Override
	protected Collection<CCModel> applyModelTransforms(Collection<CCModel> modelGroups, Direction face, IExtendedBlockState state) {
		if (!state.getValue(AutomationProperties.FORMED)) {
			return Collections.singleton(cube);
		}

		if (!state.getValue(AutomationProperties.DYNAMIC) || !state.getValue(AutomationProperties.IS_CONTROL)) {
			return Collections.emptySet();
		}

		Direction frontFacing = state.getValue(CoreProperties.UNLISTED_HORIZONTAL_FACING);
		int height = (state.getValue(AutomationProperties.HEIGHT) - 1) / 2;
		float rotation = state.getValue(AutomationProperties.ROTATION);

		return transformBlades(frontFacing, height, rotation);
	}

	@Override
	protected IconTransformation getIconTransform(IExtendedBlockState state) {
		return !state.getValue(AutomationProperties.FORMED) ? cubeIconTransform : iconTransform;
	}

	private Set<CCModel> transformBlades(Direction frontFacing, int height, float rotation) {
		Set<CCModel> transformedGroups = Sets.newHashSet();
		Vector3 center = new Vector3(8d / 16d, 8d / 16d, 8d / 16d);
		Transformation baseRotation = new Rotation(rotation, 0, 0, 1).at(center);
		transformedGroups.addAll(rotateModels(windmillShaft, frontFacing, baseRotation));

		for (int i = 0; i < 4; i++) {
			Transformation bladeTransform = new Rotation(((float) i) * Trig.PI / 2f, 0, 0, 1).at(center).with(baseRotation);
			if (i < 2) {
				transformedGroups.addAll(rotateModels(bladeShaft, frontFacing, bladeTransform));
			}
			for (int k = 1; k < height; k++) {
				transformedGroups.addAll(rotateModels(blade, frontFacing, bladeTransform));
				if (k == height - 1) {
					transformedGroups.addAll(rotateModels(bladeJoint, frontFacing, bladeTransform));
				} else {
					bladeTransform = new Translation(0, 1, 0).with(bladeTransform);
				}
			}
		}
		return transformedGroups;
	}

	@Override
	public IExtendedBlockState handleState(IExtendedBlockState state, BlockGetter access, BlockPos pos) {
		return WorldTools.getTile(access, pos, TileWindmillBlade.class).map(t -> getWindmillState(state, t.isFormed())).orElseGet(() -> getWindmillState(state, false));
	}

	private IExtendedBlockState getWindmillState(IExtendedBlockState state, boolean formed) {
		return state.withProperty(AutomationProperties.FORMED, formed)
				.withProperty(AutomationProperties.IS_CONTROL, false)
				.withProperty(AutomationProperties.HEIGHT, 0)
				.withProperty(AutomationProperties.ROTATION, 0f)
				.withProperty(CoreProperties.UNLISTED_HORIZONTAL_FACING, Direction.NORTH)
				.withProperty(AutomationProperties.DYNAMIC, false);
	}

	@Override
	public ItemTransforms getModelProperties(ItemStack stack) {
		return MODEL_PROPERTIES;
	}

	@Override
	protected void renderItemModels(CCRenderState ccrs, ItemStack stack) {
		transformBlades(Direction.NORTH, 2, 0).forEach(m -> m.render(ccrs, iconTransform));
	}
}