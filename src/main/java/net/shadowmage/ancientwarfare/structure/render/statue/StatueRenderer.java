package net.shadowmage.ancientwarfare.structure.render.statue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.util.MathUtils;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.tile.EntityStatueInfo;
import net.shadowmage.ancientwarfare.structure.tile.TileStatue;

import javax.annotation.Nullable;
import java.util.Map;

/*
 * Port notes: 1.12 TESR.
 * - ModelRenderer.offsetX/Y/Z (blocks, applied before the rotation point) -> added to the part position (* 16);
 *   rotateAngle = base + transform -> initial pose rotation + transform (the initial pose is the 1.12 base transform,
 *   see GuardianStatueModel; zero for the bipeds)
 * - RenderLivingBase.prepareScale -> scale(-1, -1, 1), the renderer's own scale (protected in 1.21; only the elder
 *   guardian of the statue entities has one), translate(0, -1.501, 0); the returned 0.0625 is built into ModelPart
 * - setLivingAnimations did nothing for these models and is not called
 * - disableCull / enableAlpha / bindTexture -> RenderType.entityCutoutNoCull(texture)
 */
public class StatueRenderer implements BlockEntityRenderer<TileStatue> {

	public StatueRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(TileStatue te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		EntityStatueInfo statueInfo = te.getEntityStatueInfo();

		if (statueInfo.getRenderType() == EntityStatueInfo.RenderType.MODEL) {
			StatueEntityRegistry.StatueEntity statueEntity = StatueEntityRegistry.getStatueEntity(statueInfo.getStatueEntityName());
			if (statueEntity == null || te.getLevel() == null) {
				return;
			}
			LivingEntity entity = statueEntity.instantiateEntity(te.getLevel());
			if (entity == null) {
				return;
			}
			EntityRenderDispatcher rendermanager = Minecraft.getInstance().getEntityRenderDispatcher();
			EntityRenderer<? super LivingEntity> render = rendermanager.getRenderer(entity);
			IStatueModel statueModel = statueEntity.getStatueModel();
			applyPartTransforms(statueModel, statueInfo.getPartTransforms());
			doRender(statueModel, statueInfo.getOverallTransform(), entity, 0.5f, 0, 0.5f, render.getTextureLocation(entity), te.getPrimaryFacing(), pose, buffers, packedLight);
		}
	}

	private void applyPartTransforms(IStatueModel statueModel, Map<String, EntityStatueInfo.Transform> partTransforms) {
		for (String partName : statueModel.getModelPartNames()) {
			ModelPart part = statueModel.getModelPart(partName);
			EntityStatueInfo.Transform transform = partTransforms.getOrDefault(partName, new EntityStatueInfo.Transform());
			EntityStatueInfo.Transform baseTransform = statueModel.getBaseTransforms().getOrDefault(partName, new EntityStatueInfo.Transform());
			part.resetPose();
			part.x += (baseTransform.getOffsetX() + transform.getOffsetX()) * 16;
			part.y += (baseTransform.getOffsetY() + transform.getOffsetY()) * 16;
			part.z += (baseTransform.getOffsetZ() + transform.getOffsetZ()) * 16;
			part.xRot += baseTransform.getRotationX() + transform.getRotationX();
			part.yRot += baseTransform.getRotationY() + transform.getRotationY();
			part.zRot += baseTransform.getRotationZ() + transform.getRotationZ();
		}
	}

	private void doRender(IStatueModel statueModel, EntityStatueInfo.Transform overallTransform, LivingEntity entity, float x, float y, float z, @Nullable ResourceLocation entityTexture, Direction primaryFacing, PoseStack pose, MultiBufferSource buffers, int packedLight) {
		pose.pushPose();

		try {
			float f = entity.yBodyRot + overallTransform.getRotationY() + primaryFacing.toYRot();
			pose.translate(x + overallTransform.getOffsetX(), y + overallTransform.getOffsetY(), z + overallTransform.getOffsetZ());
			pose.mulPose(Axis.YP.rotationDegrees(180.0F - f));
			if (!MathUtils.epsilonEquals(overallTransform.getRotationX(), 0)) {
				pose.mulPose(Axis.XP.rotationDegrees(overallTransform.getRotationX()));
			}
			if (!MathUtils.epsilonEquals(overallTransform.getRotationZ(), 0)) {
				pose.mulPose(Axis.ZP.rotationDegrees(overallTransform.getRotationZ()));
			}
			float scale = overallTransform.getScale();
			if (!MathUtils.epsilonEquals(scale, 1)) {
				pose.scale(scale, scale, scale);
			}
			//1.12 RenderLivingBase.prepareScale
			pose.scale(-1.0F, -1.0F, 1.0F);
			if (entity instanceof ElderGuardian) {
				pose.scale(ElderGuardian.ELDER_SIZE_SCALE, ElderGuardian.ELDER_SIZE_SCALE, ElderGuardian.ELDER_SIZE_SCALE);
			}
			pose.translate(0.0F, -1.501F, 0.0F);

			renderModel(statueModel, entityTexture, pose, buffers, packedLight);
		}
		catch (Exception exception) {
			AncientWarfareStructure.LOG.error("Couldn't render entity", (Throwable) exception);
		}
		pose.popPose();
	}

	private void renderModel(IStatueModel model, @Nullable ResourceLocation entityTexture, PoseStack pose, MultiBufferSource buffers, int packedLight) {
		if (entityTexture == null) {
			return;
		}
		VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(entityTexture));
		pose.pushPose();
		model.render(pose, buffer, packedLight, OverlayTexture.NO_OVERLAY);
		pose.popPose();
	}

	@Override
	public AABB getRenderBoundingBox(TileStatue te) {
		return te.getRenderBoundingBox();
	}
}
