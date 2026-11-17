package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.shadowmage.ancientwarfare.structure.tile.ISpecialLootContainer;
import net.shadowmage.ancientwarfare.structure.tile.LootSettings;
import org.joml.Matrix4f;

import java.util.Optional;

/*
 * Port notes: 1.12 TESR base class. EntityRenderer.drawNameplate -> the same plate drawn with Font.drawInBatch (dark
 * see through pass, then the normal pass, as the vanilla name tag); x / y / z are offsets from the tile position (the
 * pose handed in is already there). setLightmapDisabled(true) -> full bright.
 */
public class RenderLootInfo<T extends BlockEntity & ISpecialLootContainer> implements BlockEntityRenderer<T> {
	protected final BlockEntityRenderDispatcher rendererDispatcher;
	private final Font font;

	public RenderLootInfo(BlockEntityRendererProvider.Context context) {
		rendererDispatcher = context.getBlockEntityRenderDispatcher();
		font = context.getFont();
	}

	@Override
	public void render(T te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gameMode == null) {
			return;
		}
		if ((mc.player.getAbilities().instabuild || mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
				&& rendererDispatcher.cameraHitResult instanceof BlockHitResult hit && te.getBlockPos().equals(hit.getBlockPos())) {
			drawLootInfo(te, pose, buffers, getNameplateOffsetX(te, 0), 0, getNameplateOffsetZ(te, 0));
		}
	}

	private void drawLootInfo(T te, PoseStack pose, MultiBufferSource buffers, double x, double y, double z) {
		LootSettings lootSettings = te.getLootSettings();
		float verticalOffset = 0;
		if (lootSettings.getSpawnEntity()) {
			renderString(pose, buffers, (float) x, (float) y + verticalOffset, (float) z, lootSettings.getEntity().toString());
			verticalOffset += 0.3;
		}
		if (lootSettings.getSplashPotion()) {
			for (MobEffectInstance effect : lootSettings.getEffects()) {
				renderString(pose, buffers, (float) x, (float) y + verticalOffset, (float) z,
						BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()) + " " + effect.getAmplifier() + " "
								+ effect.getDuration() / 1200 + ":" + (effect.getDuration() % 1200) / 20);
				verticalOffset += 0.3;
			}
		}
		if (lootSettings.hasLoot()) {
			Optional<ResourceLocation> lt = lootSettings.getLootTableName();
			if (lt.isPresent()) {
				String str = te.getLootSettings().getLootRolls() + " x " + lt.get().toString();
				renderString(pose, buffers, (float) x, (float) y + verticalOffset, (float) z, str);
				verticalOffset += 0.3;
			}
		}
		if (lootSettings.hasMessage()) {
			renderString(pose, buffers, (float) x, (float) y + verticalOffset, (float) z, "Message: \"" + lootSettings.getPlayerMessage() + "\"");
		}
	}

	private void renderString(PoseStack pose, MultiBufferSource buffers, float x, float y, float z, String str) {
		drawNameplate(font, str, pose, buffers, x + 0.5F, y + 1.5F, z + 0.5F, 0.025F);
	}

	/* 1.12 EntityRenderer.drawNameplate (not sneaking, not third person frontal) */
	public static void drawNameplate(Font font, String str, PoseStack pose, MultiBufferSource buffers, float x, float y, float z, float scale) {
		pose.pushPose();
		pose.translate(x, y, z);
		pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
		pose.scale(scale, -scale, scale);
		Matrix4f matrix = pose.last().pose();
		int background = (int) (0.25F * 255.0F) << 24;
		float left = -font.width(str) / 2;
		font.drawInBatch(str, left, 0, 0x20ffffff, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, background, LightTexture.FULL_BRIGHT);
		font.drawInBatch(str, left, 0, 0xffffffff, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
		pose.popPose();
	}

	protected double getNameplateOffsetZ(T te, double z) {
		return z;
	}

	protected double getNameplateOffsetX(T te, double x) {
		return x;
	}
}
