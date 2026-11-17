package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.structure.entity.EntityGate;

public class RenderGateInvisible extends EntityRenderer<EntityGate> {
	public RenderGateInvisible(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(EntityGate entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight) {
		//invisible
	}

	@Override
	public ResourceLocation getTextureLocation(EntityGate entity) {
		return entity.getGateType().getTexture(); // 1.12 returned null; never bound
	}
}
