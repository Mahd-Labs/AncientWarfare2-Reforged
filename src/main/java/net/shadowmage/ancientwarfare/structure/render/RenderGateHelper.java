package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.structure.entity.EntityGate;
import net.shadowmage.ancientwarfare.structure.render.gate.IGateRender;
import net.shadowmage.ancientwarfare.structure.render.gate.RenderGateBasic;
import net.shadowmage.ancientwarfare.structure.render.gate.RenderGateDouble;
import net.shadowmage.ancientwarfare.structure.render.gate.RenderGateRotatingBridge;
import net.shadowmage.ancientwarfare.structure.render.gate.RenderGateSingle;

import java.util.HashMap;
import java.util.function.Function;

/*
 * Port notes: as in 1.12 this is not the entity renderer of the gate (that is RenderGateInvisible); the gate is drawn
 * by GateProxyRenderer, the renderer of the one gate proxy tile flagged "render", which calls doRender here. One
 * render per gate type as 1.12.
 * - the pose handed in is already at the gate position (1.12 translate(d0, d1, d2))
 * - the hurt tint (1.12 glColor(1, 1 - percent, 1 - percent)) is the vertex colour
 * - the light is taken at EntityGate.getLightPos() (1.12 getBrightnessForRender)
 * - 1.12 drew only the bounding boxes when F3+B was on; the vanilla hitbox overlay does that now
 */
public final class RenderGateHelper {

	private HashMap<Integer, IGateRender> gateRenders = new HashMap<>();

	public RenderGateHelper(Function<ModelLayerLocation, ModelPart> baker) {
		this.addGateRender(0, new RenderGateBasic(baker));
		this.addGateRender(1, new RenderGateBasic(baker));
		this.addGateRender(4, new RenderGateSingle(baker));
		this.addGateRender(5, new RenderGateSingle(baker));
		this.addGateRender(8, new RenderGateDouble(baker));
		this.addGateRender(9, new RenderGateDouble(baker));
		this.addGateRender(12, new RenderGateRotatingBridge(baker));
	}

	protected void addGateRender(int type, IGateRender rend) {
		this.gateRenders.put(type, rend);
	}

	public void doRender(EntityGate gate, PoseStack pose, MultiBufferSource buffers, float f, float f1) {
		if (gate.pos1 == null || gate.pos2 == null) {
			return;
		}
		pose.pushPose();
		int color = -1;
		if (gate.hurtAnimationTicks > 0) {
			float percent = ((float) gate.hurtAnimationTicks / 20.f);
			int gb = (int) (255 * (1.f - percent));
			color = 0xFFFF0000 | gb << 8 | gb;
		}
		int light = LevelRenderer.getLightColor(gate.level(), gate.getLightPos());
		VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(getEntityTexture(gate)));
		pose.mulPose(Axis.YP.rotationDegrees(f));
		pose.scale(-1, -1, 1);
		IGateRender render = this.gateRenders.get(gate.getGateType().getGlobalID());
		if (render != null) {
			render.doRender(gate, f, f1, pose, buffer, light, color);
		}
		pose.popPose();
	}

	protected ResourceLocation getEntityTexture(EntityGate entity) {
		return entity.getGateType().getTexture();
	}
}
