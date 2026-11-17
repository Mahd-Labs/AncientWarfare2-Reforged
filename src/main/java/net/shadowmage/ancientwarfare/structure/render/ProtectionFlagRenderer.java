package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.structure.block.BlockFlag;
import net.shadowmage.ancientwarfare.structure.tile.TileFlag;

/*
 * Port notes: 1.12 TESR + IItemRenderer. ModelBanner -> the vanilla banner layer (same boxes: flag = bannerSlate,
 * pole = bannerStand, bar = bannerTop), ModelHumanoidHead -> the vanilla player head layer. The item half
 * (renderItem) is called by StructureItemRenderer; the 1.12 TRANSFORMS are the "display" block of the item model.
 * Licensing (release candidate): the faction banner textures carried a copy of vanilla's banner pole and crossbar
 * (the pixels of minecraft:textures/entity/banner_base.png outside the cloth). Those pixels are now transparent in the
 * mod's textures; the pole and bar are drawn first with the vanilla texture, then the faction texture on top, so
 * banners with their own pole art (demon, evil, icelord, vampire, part of orc) still show it. Same look as before.
 */
public class ProtectionFlagRenderer implements BlockEntityRenderer<TileFlag> {
	private static final ResourceLocation VANILLA_BANNER_BASE = ResourceLocation.withDefaultNamespace("textures/entity/banner_base.png");
	private final ModelPart flag;
	private final ModelPart pole;
	private final ModelPart bar;
	private final SkullModel humanoidHead;

	public ProtectionFlagRenderer(BlockEntityRendererProvider.Context context) {
		this(context.getModelSet());
	}

	public ProtectionFlagRenderer(EntityModelSet models) {
		ModelPart modelpart = models.bakeLayer(ModelLayers.BANNER);
		flag = modelpart.getChild("flag");
		pole = modelpart.getChild("pole");
		bar = modelpart.getChild("bar");
		humanoidHead = new SkullModel(models.bakeLayer(ModelLayers.PLAYER_HEAD));
	}

	@Override
	public void render(TileFlag te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		boolean flag = te.getLevel() != null;
		int angle = flag && te.getBlockState().hasProperty(BlockFlag.ROTATION) ? te.getBlockState().getValue(BlockFlag.ROTATION) : 0;
		long worldTime = flag ? te.getLevel().getGameTime() : 0L;
		String faction = te.getName();
		BlockPos pos = te.getBlockPos();
		render(pose, buffers, packedLight, partialTicks, angle, worldTime, faction, pos);
		if (te.isPlayerOwned()) {
			renderPlayerHead(te.getPlayerProfile(), pose, buffers, packedLight, partialTicks, angle);
		}
	}

	private void renderPlayerHead(GameProfile profile, PoseStack pose, MultiBufferSource buffers, int packedLight, float animateTicks, int rotation) {
		if (profile == null) {
			return;
		}
		ResourceLocation resourcelocation = Minecraft.getInstance().getSkinManager().getInsecureSkin(profile).texture();
		pose.pushPose();
		pose.translate(0.5F, 2F, 0.5F);
		pose.scale(-1.0F, -1.0F, 1.0F);
		humanoidHead.setupAnim(animateTicks, 360 * (rotation / 16.0F) - 180, 0.0F);
		humanoidHead.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(resourcelocation)), packedLight, OverlayTexture.NO_OVERLAY);
		pose.popPose();
	}

	private void render(PoseStack pose, MultiBufferSource buffers, int packedLight, float partialTicks, int rotation, float worldTime, String faction, BlockPos pos) {
		pose.pushPose();
		pose.translate(0.5F, 0.5F, 0.5F);
		float f1 = (float) (rotation * 360) / 16.0F;
		pose.mulPose(Axis.YP.rotationDegrees(-f1));
		pole.visible = true;

		float f3 = (float) (pos.getX() * 7 + pos.getY() * 9 + pos.getZ() * 13) + worldTime + partialTicks;
		flag.xRot = (-0.0125F + 0.01F * Mth.cos(f3 * (float) Math.PI * 0.02F)) * (float) Math.PI;
		ResourceLocation resourcelocation = getBannerResourceLocation(faction);

		pose.pushPose();
		pose.scale(0.6666667F, -0.6666667F, -0.6666667F);
		//1.12 ModelBanner.renderBanner
		flag.y = -32.0F;
		VertexConsumer vanillaBuffer = buffers.getBuffer(RenderType.entityCutoutNoCull(VANILLA_BANNER_BASE));
		pole.render(pose, vanillaBuffer, packedLight, OverlayTexture.NO_OVERLAY);
		bar.render(pose, vanillaBuffer, packedLight, OverlayTexture.NO_OVERLAY);
		VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(resourcelocation));
		flag.render(pose, buffer, packedLight, OverlayTexture.NO_OVERLAY);
		pole.render(pose, buffer, packedLight, OverlayTexture.NO_OVERLAY);
		bar.render(pose, buffer, packedLight, OverlayTexture.NO_OVERLAY);
		pose.popPose();

		pose.popPose();
	}

	private ResourceLocation getBannerResourceLocation(String faction) {
		ResourceLocation textureEntry = ResourceLocation.tryBuild(AncientWarfareCore.MOD_ID, "textures/entity/structure/banner/" + faction + ".png");
		return textureEntry == null ? ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/entity/structure/banner/.png") : textureEntry;
	}

	public void renderItem(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int packedLight) {
		if (!ItemTools.hasTag(stack)) {
			return;
		}
		pose.pushPose();
		CompoundTag tag = ItemTools.getTag(stack);
		render(pose, buffers, packedLight, 0, 0, 0, tag.getString("name"), BlockPos.ZERO);
		pose.popPose();
	}

	@Override
	public AABB getRenderBoundingBox(TileFlag te) {
		return te.getRenderBoundingBox();
	}
}
