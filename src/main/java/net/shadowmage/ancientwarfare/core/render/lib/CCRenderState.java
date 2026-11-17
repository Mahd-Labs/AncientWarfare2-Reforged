/*
 * Derived from CodeChickenLib 3.2.3 (TheCBProject/CodeChickenLib, branch 1.12.x), Copyright ChickenBones and
 * covers1624, licensed under the GNU LGPL 2.1; distributed here under the GNU GPL v3 or later as permitted by
 * section 3 of the LGPL 2.1.
 * Source file: codechicken/lib/render/CCRenderState.java. Changed 2026-11-14 for the port of Ancient Warfare 2 to
 * NeoForge 1.21.1: mostly rewritten against the NeoForge 1.21.1 vertex consumer and BakedQuad API; only the class
 * role and a few method names remain.
 */
package net.shadowmage.ancientwarfare.core.render.lib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/*
 * Stand in for codechicken.lib.render.CCRenderState + BakingVertexBuffer, reduced to the two things AW does with it:
 *
 *  baking (1.12: BakingVertexBuffer.create(); buffer.begin(7, ITEM); ccrs.reset(); ccrs.bind(buffer); ...; buffer.bake()):
 *      ccrs.reset(); ccrs.startBaking(blockShading); model.render(ccrs, icon)...; List<BakedQuad> quads = ccrs.bake();
 *
 *  immediate rendering from a BlockEntityRenderer (1.12: FastTESR, ccrs.bind(bufferBuilder); ccrs.setBrightness(world, pos)):
 *      ccrs.reset(); ccrs.bind(vertexConsumer, poseStack.last(), packedLight, packedOverlay); model.render(ccrs, lightModel, icon)...
 *
 * One instance per thread (chunk meshing bakes on worker threads).
 */
public class CCRenderState {
	private static final ThreadLocal<CCRenderState> instances = ThreadLocal.withInitial(CCRenderState::new);

	public static CCRenderState instance() {
		return instances.get();
	}

	private VertexConsumer consumer;
	private PoseStack.Pose pose;
	private int packedLight;
	private int packedOverlay;

	private List<BakedQuad> bakedQuads;
	private boolean blockShading;

	/*
	 * Port addition (automation flywheel windows; CCL: CCModel.setColour): alpha of the vertex colour, reset() sets 1
	 */
	public float alpha = 1.f;

	private CCRenderState() {
	}

	public void reset() {
		consumer = null;
		pose = null;
		bakedQuads = null;
		alpha = 1.f;
	}

	public void bind(VertexConsumer consumer, PoseStack.Pose pose, int packedLight, int packedOverlay) {
		this.consumer = consumer;
		this.pose = pose;
		this.packedLight = packedLight;
		this.packedOverlay = packedOverlay;
		this.bakedQuads = null;
	}

	/*
	 * @param blockShading true for quads used in the world (block models): the Forge 1.12 diffuse term of the vertex
	 *                     normal is baked into the vertex colour and the quad is flagged unshaded, because 1.21.1
	 *                     shades block quads by their Direction only. false for item quads (lit through their normals).
	 */
	public void startBaking(boolean blockShading) {
		this.consumer = null;
		this.pose = null;
		this.bakedQuads = new ArrayList<>();
		this.blockShading = blockShading;
	}

	public List<BakedQuad> bake() {
		List<BakedQuad> ret = bakedQuads == null ? new ArrayList<>() : bakedQuads;
		bakedQuads = null;
		return ret;
	}

	void render(CCModel model, @Nullable LightModel lightModel, IconTransformation icon) {
		if (bakedQuads != null) {
			bakeModel(model, lightModel, icon);
		} else if (consumer != null) {
			drawModel(model, lightModel, icon);
		} else {
			throw new IllegalStateException("CCRenderState: neither bind() nor startBaking() was called");
		}
	}

	private void drawModel(CCModel model, @Nullable LightModel lightModel, IconTransformation icon) {
		for (int i = 0; i < model.verts.length; i++) {
			Vector3 vert = model.verts[i];
			Vector3 normal = model.normals != null && model.normals[i] != null ? model.normals[i] : UP;
			float c = lightModel != null ? lightModel.apply(normal) : 1.f;
			consumer.addVertex(pose, (float) vert.x, (float) vert.y, (float) vert.z)
					.setColor(c, c, c, alpha)
					.setUv(icon.getU(model.us[i]), icon.getV(model.vs[i]))
					.setOverlay(packedOverlay)
					.setLight(packedLight)
					.setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
		}
	}

	private static final Vector3 UP = new Vector3(0, 1, 0);

	private void bakeModel(CCModel model, @Nullable LightModel lightModel, IconTransformation icon) {
		for (int q = 0; q + CCModel.vp <= model.verts.length; q += CCModel.vp) {
			int[] data = new int[32];//4 vertices of DefaultVertexFormat.BLOCK: xyz, colour, uv, light, normal
			Vector3 firstNormal = model.normals != null && model.normals[q] != null ? model.normals[q] : calculateNormal(model.verts[q], model.verts[q + 1], model.verts[q + 3]);
			for (int k = 0; k < CCModel.vp; k++) {
				int i = q + k;
				Vector3 vert = model.verts[i];
				Vector3 normal = model.normals != null && model.normals[i] != null ? model.normals[i] : firstNormal;
				float c = 1.f;
				if (lightModel != null) {
					c = lightModel.apply(normal);
				} else if (blockShading) {
					c = diffuseLight((float) normal.x, (float) normal.y, (float) normal.z);
				}
				int ci = (int) (c * 255.f) & 0xFF;
				int o = k * 8;
				data[o] = Float.floatToRawIntBits((float) vert.x);
				data[o + 1] = Float.floatToRawIntBits((float) vert.y);
				data[o + 2] = Float.floatToRawIntBits((float) vert.z);
				data[o + 3] = ((int) (alpha * 255.f) & 0xFF) << 24 | ci << 16 | ci << 8 | ci;//ABGR
				data[o + 4] = Float.floatToRawIntBits(icon.getU(model.us[i]));
				data[o + 5] = Float.floatToRawIntBits(icon.getV(model.vs[i]));
				data[o + 6] = 0;
				data[o + 7] = packNormal(normal);
			}
			//Use normal to calculate facing.
			Direction facing = calcNormalSide(firstNormal);
			if (facing == null) {
				facing = Direction.UP;
			}
			bakedQuads.add(new BakedQuad(data, -1, facing, icon.icon, !blockShading && lightModel == null, true));
		}
	}

	private static int packNormal(Vector3 normal) {
		int x = (byte) Math.round(normal.x * 127.0) & 0xFF;
		int y = (byte) Math.round(normal.y * 127.0) & 0xFF;
		int z = (byte) Math.round(normal.z * 127.0) & 0xFF;
		return x | y << 8 | z << 16;
	}

	/*
	 * net.minecraftforge.client.model.pipeline.LightUtil.diffuseLight (Forge 1.12)
	 */
	public static float diffuseLight(float x, float y, float z) {
		return Math.min(x * x * 0.6f + y * y * ((3f + y) / 4f) + z * z * 0.8f, 1f);
	}

	private static Vector3 calculateNormal(Vector3 p1, Vector3 p2, Vector3 p3) {
		Vector3 diff1 = p2.copy().subtract(p1);
		Vector3 diff2 = p3.copy().subtract(p1);
		return diff1.crossProduct(diff2).normalize();
	}

	/*
	 * codechicken.lib.util.VectorUtils.calcNormalSide
	 */
	@Nullable
	public static Direction calcNormalSide(Vector3 normal) {
		if (normal.y <= -0.99) {
			return Direction.DOWN;
		}
		if (normal.y >= 0.99) {
			return Direction.UP;
		}
		if (normal.z <= -0.99) {
			return Direction.NORTH;
		}
		if (normal.z >= 0.99) {
			return Direction.SOUTH;
		}
		if (normal.x <= -0.99) {
			return Direction.WEST;
		}
		if (normal.x >= 0.99) {
			return Direction.EAST;
		}
		return null;
	}
}
