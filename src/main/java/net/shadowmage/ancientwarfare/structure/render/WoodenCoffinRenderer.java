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
import net.shadowmage.ancientwarfare.structure.block.BlockWoodenCoffin;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.structure.model.ModelCoffin;
import net.shadowmage.ancientwarfare.structure.tile.TileWoodenCoffin;

import java.util.HashMap;
import java.util.Map;

/*
 * Port notes: 1.12 TESR (GlStateManager transforms) -> BlockEntityRenderer with the same transforms on the PoseStack.
 * Not ported: the item renderer half (IItemRenderer / TRANSFORMS, the coffin item is a placeholder model) and the
 * RenderLootInfo base class (the loot info name plate shown to creative players).
 */
public class WoodenCoffinRenderer extends RenderLootInfo<TileWoodenCoffin> {
	private static final Map<BlockCoffin.IVariant, ResourceLocation> TEXTURES = new HashMap<>();

	static {
		for (BlockWoodenCoffin.Variant variant : BlockWoodenCoffin.Variant.values()) {
			TEXTURES.put(variant, ResourceLocation.fromNamespaceAndPath("ancientwarfare", "textures/model/structure/coffin_" + variant.getSerializedName() + ".png"));
		}
	}

	private final ModelCoffin coffinModel;

	public WoodenCoffinRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
		coffinModel = new ModelCoffin(context.bakeLayer(ModelCoffin.LAYER));
	}

	@Override
	public void render(TileWoodenCoffin te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		BlockState state = te.getBlockState();
		super.render(te, partialTicks, pose, buffers, packedLight, packedOverlay);
		if (state.getBlock() != AWStructureBlocks.WOODEN_COFFIN.get() || Boolean.TRUE.equals(state.getValue(BlockMulti.INVISIBLE))) {
			return;
		}
		float rotation = te.getDirection().getRotationAngle();
		boolean upright = te.getUpright();

		pose.pushPose();
		pose.translate(0.5F, 2.16F, 0.5F);
		pose.mulPose(Axis.YP.rotationDegrees(-rotation)); //passing in negative value because of the flipping of the model below
		pose.pushPose();
		if (upright) {
			if ((rotation % 90) == 0) {
				pose.translate(0, -1.25, 1.85);
			} else {
				pose.translate(0, -1.25, 2.1);
			}
		} else {
			pose.translate(0, 0, -0.22f);
		}
		pose.mulPose(Axis.XP.rotationDegrees(upright ? 265 : 180));
		pose.scale(0.09f, 0.09f, 0.09f);
		VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURES.get(te.getVariant())));
		float lidAngle = te.getPrevLidAngle() + (te.getLidAngle() - te.getPrevLidAngle()) * partialTicks;
		coffinModel.renderAll(pose, buffer, packedLight, packedOverlay, -1, (float) (-lidAngle / 180F * Math.PI));
		pose.popPose();
		pose.popPose();
	}

	@Override
	public AABB getRenderBoundingBox(TileWoodenCoffin te) {
		return te.getRenderBoundingBox();
	}

	@Override
	protected double getNameplateOffsetZ(TileWoodenCoffin te, double z) {
		if (!te.getUpright()) {
			return super.getNameplateOffsetZ(te, z);
		}
		double offSetZ = Math.max(Math.min(net.minecraft.client.Minecraft.getInstance().player.getZ() - te.getBlockPos().getZ(), 1), -1);
		return z + offSetZ;
	}

	@Override
	protected double getNameplateOffsetX(TileWoodenCoffin te, double x) {
		if (!te.getUpright()) {
			return super.getNameplateOffsetX(te, x);
		}
		double offSetX = Math.max(Math.min(net.minecraft.client.Minecraft.getInstance().player.getX() - te.getBlockPos().getX(), 1), -1);
		return x + offSetX;
	}
}
