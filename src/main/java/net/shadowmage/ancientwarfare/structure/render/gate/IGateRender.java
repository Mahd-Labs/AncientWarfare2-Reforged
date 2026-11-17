package net.shadowmage.ancientwarfare.structure.render.gate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.shadowmage.ancientwarfare.structure.entity.EntityGate;

/* Port notes: stands for the raw 1.12 Render type RenderGateHelper kept its per gate type renders as. */
public interface IGateRender {
	void doRender(EntityGate gate, float f, float f1, PoseStack pose, VertexConsumer buffer, int light, int color);
}
