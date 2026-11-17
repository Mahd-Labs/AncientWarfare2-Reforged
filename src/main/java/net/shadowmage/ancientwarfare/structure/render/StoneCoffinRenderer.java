package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.structure.block.BlockCoffin;
import net.shadowmage.ancientwarfare.structure.block.BlockMulti;
import net.shadowmage.ancientwarfare.structure.block.BlockStoneCoffin;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.structure.model.ModelStoneCoffin;
import net.shadowmage.ancientwarfare.structure.tile.TileStoneCoffin;

import java.util.HashMap;
import java.util.Map;

/*
 * Port notes: see WoodenCoffinRenderer (same scope: block entity render only).
 */
public class StoneCoffinRenderer extends RenderLootInfo<TileStoneCoffin> {
	private static final Map<BlockCoffin.IVariant, ResourceLocation> TEXTURES = new HashMap<>();

	static {
		for (BlockStoneCoffin.Variant variant : BlockStoneCoffin.Variant.values()) {
			TEXTURES.put(variant, ResourceLocation.fromNamespaceAndPath("ancientwarfare", "textures/model/structure/stone_coffin_" + variant.getSerializedName() + ".png"));
		}
	}

	private final ModelStoneCoffin stoneCoffinModel;

	public StoneCoffinRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
		stoneCoffinModel = new ModelStoneCoffin(context.bakeLayer(ModelStoneCoffin.LAYER));
	}

	@Override
	public void render(TileStoneCoffin te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		BlockState state = te.getBlockState();
		super.render(te, partialTicks, pose, buffers, packedLight, packedOverlay);
		if (state.getBlock() != AWStructureBlocks.STONE_COFFIN.get() || Boolean.TRUE.equals(state.getValue(BlockMulti.INVISIBLE))) {
			return;
		}
		float rotation = te.getDirection().getRotationAngle();

		pose.pushPose();
		pose.translate(0, 1.78F, 0);
		pose.mulPose(Axis.YP.rotationDegrees(-rotation)); //passing in negative value because of the flipping of the model below
		pose.pushPose();

		switch ((int) rotation) {
			case 0: // north
				pose.translate(0, 0, -1F);
				break;
			case 90: // east
				pose.translate(0, 0, -2F);
				break;
			case 180: //south
				pose.translate(-1F, 0, -2F);
				break;
			case 270: // west
			default:
				pose.translate(-1F, 0, -1F);
				break;
		}

		pose.mulPose(Axis.XP.rotationDegrees(180));
		pose.scale(0.074f, 0.074f, 0.074f);
		VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURES.get(te.getVariant())));
		float lidAngle = te.getPrevLidAngle() + (te.getLidAngle() - te.getPrevLidAngle()) * partialTicks;
		stoneCoffinModel.renderAll(pose, buffer, packedLight, packedOverlay, -1, (float) (-lidAngle / 180F * Math.PI));
		pose.popPose();
		pose.popPose();
	}

	@Override
	public AABB getRenderBoundingBox(TileStoneCoffin te) {
		return te.getRenderBoundingBox();
	}
}
