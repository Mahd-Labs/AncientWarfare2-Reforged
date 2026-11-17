package net.shadowmage.ancientwarfare.core.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.OptionalDouble;

/*
 * CLIENT ONLY. Port notes (1.12 -> 1.21.1):
 *  - There is no bound-texture / GL matrix state any more. Gui helpers take the GuiGraphics (for its pose) and the
 *    texture as leading parameters, the remaining parameters are unchanged:
 *      mc.renderEngine.bindTexture(tex); RenderTools.renderQuarteredTexture(a...)  ->  RenderTools.renderQuarteredTexture(graphics, tex, a...)
 *      RenderTools.renderTexturedQuad(x1...)  ->  RenderTools.renderTexturedQuad(graphics, tex, x1...)
 *      RenderTools.renderColoredQuad(x...)    ->  RenderTools.renderColoredQuad(graphics, x...)
 *  - World space helpers take (PoseStack, MultiBufferSource) as leading parameters. The pose stack must already be
 *    camera relative (in a RenderLevelStageEvent: poseStack.translate(-camera.x, -camera.y, -camera.z) and pass the
 *    world space box, or pass a box moved with adjustBBForPlayerPos like 1.12 did and an untranslated pose).
 *  - setFullColorLightmap() cannot set state any more; it returns the packed light value (block 0, sky 15 - the same
 *    coordinates 1.12 set) that the caller passes as packedLight.
 *  - 1.12 passed java.awt.Color 0-255 ints into float colour parameters, which OpenGL clamped to 1.0, so every
 *    non-zero channel rendered at full intensity. The Color overloads keep that result (see clamp()).
 */
public class RenderTools {
	private RenderTools() {}

	public static int setFullColorLightmap() {
		return LightTexture.pack(0, 15);
	}

	/*
	 * @param textureWidth  texture width
	 * @param textureHeight texture height
	 * @param texStartX     pixel start U
	 * @param texStartY     pixel start V
	 * @param texUsedWidth  pixel U width (width of used tex in pixels)
	 * @param texUsedHeight pixel V height (height of used tex in pixels)
	 * @param renderStartX  render position x
	 * @param renderStartY  render position y
	 * @param renderHeight  render height
	 * @param renderWidth   render width
	 */
	public static void renderQuarteredTexture(GuiGraphics graphics, ResourceLocation texture, int textureWidth, int textureHeight, int texStartX, int texStartY, int texUsedWidth, int texUsedHeight, int renderStartX, int renderStartY, int renderWidth, int renderHeight) {
		//perspective percent x, y
		float perX = 1.f / ((float) textureWidth);
		float perY = 1.f / ((float) textureHeight);
		float texMinX = ((float) texStartX) * perX;
		float texMinY = ((float) texStartY) * perY;
		float texMaxX = (float) (texStartX + texUsedWidth) * perX;
		float texMaxY = (float) (texStartY + texUsedHeight) * perY;
		float halfWidth = (((float) renderWidth) / 2.f) * perX;
		float halfHeight = (((float) renderHeight) / 2.f) * perY;
		float halfRenderWidth = ((float) renderWidth) * 0.5f;
		float halfRenderHeight = ((float) renderHeight) * 0.5f;

		//draw top-left quadrant
		renderTexturedQuad(graphics, texture, renderStartX, renderStartY, renderStartX + halfRenderWidth, renderStartY + halfRenderHeight, texMinX, texMinY, texMinX + halfWidth, texMinY + halfHeight);

		//draw top-right quadrant
		renderTexturedQuad(graphics, texture, renderStartX + halfRenderWidth, renderStartY, renderStartX + halfRenderWidth * 2, renderStartY + halfRenderHeight, texMaxX - halfWidth, texMinY, texMaxX, texMinY + halfHeight);

		//draw bottom-left quadrant
		renderTexturedQuad(graphics, texture, renderStartX, renderStartY + halfRenderHeight, renderStartX + halfRenderWidth, renderStartY + halfRenderHeight * 2, texMinX, texMaxY - halfHeight, texMinX + halfWidth, texMaxY);

		//draw bottom-right quadrant
		renderTexturedQuad(graphics, texture, renderStartX + halfRenderWidth, renderStartY + halfRenderHeight, renderStartX + halfRenderWidth * 2, renderStartY + halfRenderHeight * 2, texMaxX - halfWidth, texMaxY - halfHeight, texMaxX, texMaxY);
	}

	/*
	 * Same quad GuiGraphics.blit draws, but with float positions (the quartered renderer needs half pixels).
	 * Tinted by the current shader colour (graphics.setColor), like 1.12 GlStateManager.color did.
	 */
	public static void renderTexturedQuad(GuiGraphics graphics, ResourceLocation texture, float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2) {
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.enableBlend();
		Matrix4f matrix = graphics.pose().last().pose();
		BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferBuilder.addVertex(matrix, x1, y1, 0).setUv(u1, v1);
		bufferBuilder.addVertex(matrix, x1, y2, 0).setUv(u1, v2);
		bufferBuilder.addVertex(matrix, x2, y2, 0).setUv(u2, v2);
		bufferBuilder.addVertex(matrix, x2, y1, 0).setUv(u2, v1);
		BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
	}

	public static void renderColoredQuad(GuiGraphics graphics, int renderStartX, int renderStartY, int renderWidth, int renderHeight, float colorRed, float colorGreen, float colorBlue) {
		renderColoredQuad(graphics, renderStartX, renderStartY, (float) renderStartX + renderWidth, (float) renderStartY + renderHeight, colorRed, colorGreen, colorBlue, 1.f);
	}

	/*
	 * untextured gui quad with float corners and alpha (replaces the 1.12 inline glBegin(GL_QUADS) blocks of the widgets)
	 */
	public static void renderColoredQuad(GuiGraphics graphics, float x1, float y1, float x2, float y2, float colorRed, float colorGreen, float colorBlue, float alpha) {
		renderColoredQuad(graphics, RenderType.gui(), x1, y1, x2, y2, colorRed, colorGreen, colorBlue, alpha);
	}

	public static void renderColoredQuad(GuiGraphics graphics, RenderType renderType, float x1, float y1, float x2, float y2, float colorRed, float colorGreen, float colorBlue, float alpha) {
		Matrix4f matrix = graphics.pose().last().pose();
		VertexConsumer consumer = graphics.bufferSource().getBuffer(renderType);
		float r = clamp(colorRed);
		float g = clamp(colorGreen);
		float b = clamp(colorBlue);
		float a = clamp(alpha);
		consumer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, a);
		consumer.addVertex(matrix, x1, y2, 0).setColor(r, g, b, a);
		consumer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, a);
		consumer.addVertex(matrix, x2, y1, 0).setColor(r, g, b, a);
		graphics.flush();
	}

	/*
	 * OpenGL fixed function colour clamping, see class comment
	 */
	public static float clamp(float colorComponent) {
		return Mth.clamp(colorComponent, 0.f, 1.f);
	}

	/*
	 * render a BB as a set of enlarged cuboids.
	 */
	public static void drawOutlinedBoundingBox2(PoseStack poseStack, MultiBufferSource buffers, AABB bb, float r, float g, float b, float width) {
		VertexConsumer consumer = buffers.getBuffer(Types.QUADS);
		Matrix4f matrix = poseStack.last().pose();
		r = clamp(r);
		g = clamp(g);
		b = clamp(b);
		float hw = width / 2;
		drawCuboid(consumer, matrix, r, g, b, (float) bb.minX, (float) bb.minY - hw, (float) bb.minZ - hw, (float) bb.maxX, (float) bb.minY + hw, (float) bb.minZ + hw);
		drawCuboid(consumer, matrix, r, g, b, (float) bb.minX, (float) bb.maxY - hw, (float) bb.minZ - hw, (float) bb.maxX, (float) bb.maxY + hw, (float) bb.minZ + hw);
		drawCuboid(consumer, matrix, r, g, b, (float) bb.minX, (float) bb.minY - hw, (float) bb.maxZ - hw, (float) bb.maxX, (float) bb.minY + hw, (float) bb.maxZ + hw);
		drawCuboid(consumer, matrix, r, g, b, (float) bb.minX, (float) bb.maxY - hw, (float) bb.maxZ - hw, (float) bb.maxX, (float) bb.maxY + hw, (float) bb.maxZ + hw);

		drawCuboid(consumer, matrix, r, g, b, (float) bb.minX - hw, (float) bb.minY, (float) bb.minZ - hw, (float) bb.minX + hw, (float) bb.maxY, (float) bb.minZ + hw);
		drawCuboid(consumer, matrix, r, g, b, (float) bb.maxX - hw, (float) bb.minY, (float) bb.minZ - hw, (float) bb.maxX + hw, (float) bb.maxY, (float) bb.minZ + hw);
		drawCuboid(consumer, matrix, r, g, b, (float) bb.minX - hw, (float) bb.minY, (float) bb.maxZ - hw, (float) bb.minX + hw, (float) bb.maxY, (float) bb.maxZ + hw);
		drawCuboid(consumer, matrix, r, g, b, (float) bb.maxX - hw, (float) bb.minY, (float) bb.maxZ - hw, (float) bb.maxX + hw, (float) bb.maxY, (float) bb.maxZ + hw);

		drawCuboid(consumer, matrix, r, g, b, (float) bb.minX - hw, (float) bb.minY - hw, (float) bb.minZ, (float) bb.minX + hw, (float) bb.minY + hw, (float) bb.maxZ);
		drawCuboid(consumer, matrix, r, g, b, (float) bb.minX - hw, (float) bb.maxY - hw, (float) bb.minZ, (float) bb.minX + hw, (float) bb.maxY + hw, (float) bb.maxZ);
		drawCuboid(consumer, matrix, r, g, b, (float) bb.maxX - hw, (float) bb.minY - hw, (float) bb.minZ, (float) bb.maxX + hw, (float) bb.minY + hw, (float) bb.maxZ);
		drawCuboid(consumer, matrix, r, g, b, (float) bb.maxX - hw, (float) bb.maxY - hw, (float) bb.minZ, (float) bb.maxX + hw, (float) bb.maxY + hw, (float) bb.maxZ);
	}

	private static void drawCuboid(VertexConsumer consumer, Matrix4f matrix, float r, float g, float b, float x, float y, float z, float mx, float my, float mz) {
		float a = 0.4F;
		//z+ side
		consumer.addVertex(matrix, x, my, mz).setColor(r, g, b, a);
		consumer.addVertex(matrix, x, y, mz).setColor(r, g, b, a);
		consumer.addVertex(matrix, mx, y, mz).setColor(r, g, b, a);
		consumer.addVertex(matrix, mx, my, mz).setColor(r, g, b, a);

		//x+ side
		consumer.addVertex(matrix, mx, my, mz).setColor(r, g, b, a);
		consumer.addVertex(matrix, mx, y, mz).setColor(r, g, b, a);
		consumer.addVertex(matrix, mx, y, z).setColor(r, g, b, a);
		consumer.addVertex(matrix, mx, my, z).setColor(r, g, b, a);

		//y+ side
		consumer.addVertex(matrix, x, my, z).setColor(r, g, b, a);
		consumer.addVertex(matrix, x, my, mz).setColor(r, g, b, a);
		consumer.addVertex(matrix, mx, my, mz).setColor(r, g, b, a);
		consumer.addVertex(matrix, mx, my, z).setColor(r, g, b, a);

		//z- side
		consumer.addVertex(matrix, x, my, z).setColor(r, g, b, a);
		consumer.addVertex(matrix, mx, my, z).setColor(r, g, b, a);
		consumer.addVertex(matrix, mx, y, z).setColor(r, g, b, a);
		consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a);

		//x-side
		consumer.addVertex(matrix, x, y, mz).setColor(r, g, b, a);
		consumer.addVertex(matrix, x, my, mz).setColor(r, g, b, a);
		consumer.addVertex(matrix, x, my, z).setColor(r, g, b, a);
		consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a);

		//y- side
		consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a);
		consumer.addVertex(matrix, mx, y, z).setColor(r, g, b, a);
		consumer.addVertex(matrix, mx, y, mz).setColor(r, g, b, a);
		consumer.addVertex(matrix, x, y, mz).setColor(r, g, b, a);
	}

	/*
	 * Renders RGB lines for X,Y,Z axis'
	 * (1.12 also drew GL_POINTS at the origin and the axis ends; there is no point primitive in 1.21.1)
	 */
	public static void renderOrientationPoints(PoseStack poseStack, MultiBufferSource buffers, float colorMult) {
		VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
		PoseStack.Pose pose = poseStack.last();
		float c = clamp(colorMult);
		line(consumer, pose, 0, 0, 0, 1, 0, 0, c, 0, 0, 1.f);//red for x axis
		line(consumer, pose, 0, 0, 0, 0, 1, 0, 0, c, 0, 1.f);//green for y axis
		line(consumer, pose, 0, 0, 0, 0, 0, 1, 0, 0, c, 1.f);//blue for z axis
	}

	/*
	 * draw a player-position-normalized bounding box (can only be called during worldRender)
	 */
	public static void drawTopSideOverlay(PoseStack poseStack, MultiBufferSource buffers, AABB bb, Color color) {
		VertexConsumer consumer = buffers.getBuffer(Types.QUADS);
		Matrix4f matrix = poseStack.last().pose();
		float r = clamp(color.getRed());
		float g = clamp(color.getGreen());
		float b = clamp(color.getBlue());
		float a = color.getAlpha() / 255f;
		float yOffset = 0.01f; //offset to prevent z fighting
		consumer.addVertex(matrix, (float) bb.minX, (float) bb.maxY + yOffset, (float) bb.maxZ).setColor(r, g, b, a);
		consumer.addVertex(matrix, (float) bb.maxX, (float) bb.maxY + yOffset, (float) bb.maxZ).setColor(r, g, b, a);
		consumer.addVertex(matrix, (float) bb.maxX, (float) bb.maxY + yOffset, (float) bb.minZ).setColor(r, g, b, a);
		consumer.addVertex(matrix, (float) bb.minX, (float) bb.maxY + yOffset, (float) bb.minZ).setColor(r, g, b, a);
	}

	public static void drawOutlinedBoundingBox(PoseStack poseStack, MultiBufferSource buffers, AABB bb, Color color) {
		drawOutlinedBoundingBox(poseStack, buffers, bb, color, false);
	}

	public static void drawOutlinedBoundingBox(PoseStack poseStack, MultiBufferSource buffers, AABB bb, Color color, boolean disableDepth) {
		drawOutlinedBoundingBox(poseStack, buffers, bb, color.getRed(), color.getGreen(), color.getBlue(), disableDepth);
	}

	public static void drawOutlinedBoundingBox(PoseStack poseStack, MultiBufferSource buffers, AABB bb, float r, float g, float b) {
		drawOutlinedBoundingBox(poseStack, buffers, bb, r, g, b, false);
	}

	private static void drawOutlinedBoundingBox(PoseStack poseStack, MultiBufferSource buffers, AABB bb, float r, float g, float b, boolean disabledDepth) {
		VertexConsumer consumer = buffers.getBuffer(disabledDepth ? Types.LINES_NO_DEPTH : Types.LINES);
		PoseStack.Pose pose = poseStack.last();
		r = clamp(r);
		g = clamp(g);
		b = clamp(b);
		float a = 0.4F;
		float minX = (float) bb.minX, minY = (float) bb.minY, minZ = (float) bb.minZ;
		float maxX = (float) bb.maxX, maxY = (float) bb.maxY, maxZ = (float) bb.maxZ;
		//bottom outline
		line(consumer, pose, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
		line(consumer, pose, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
		line(consumer, pose, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
		line(consumer, pose, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
		//top outline
		line(consumer, pose, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
		line(consumer, pose, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
		line(consumer, pose, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
		line(consumer, pose, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
		//verticals
		line(consumer, pose, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
		line(consumer, pose, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
		line(consumer, pose, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
		line(consumer, pose, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
	}

	private static void line(VertexConsumer consumer, PoseStack.Pose pose, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
		float nx = x2 - x1;
		float ny = y2 - y1;
		float nz = z2 - z1;
		float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
		if (len > 0) {
			nx /= len;
			ny /= len;
			nz /= len;
		}
		consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
		consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
	}

	/*
	 * @param bb
	 * @param player
	 * @param partialTick
	 * @return
	 */
	public static AABB adjustBBForPlayerPos(AABB bb, Player player, float partialTick) {
		double x = getRenderOffsetX(player, partialTick);
		double y = getRenderOffsetY(player, partialTick);
		double z = getRenderOffsetZ(player, partialTick);
		return bb.move(-x, -y, -z);
	}

	public static double getRenderOffsetX(Player player, float partialTick) {
		return player.xOld + (player.getX() - player.xOld) * partialTick;
	}

	public static double getRenderOffsetY(Player player, float partialTick) {
		return player.yOld + (player.getY() - player.yOld) * partialTick;
	}

	public static double getRenderOffsetZ(Player player, float partialTick) {
		return player.zOld + (player.getZ() - player.zOld) * partialTick;
	}

	/*
	 * renders the block entity with its BlockEntityRenderer at pos (relative to the current pose), 1.12 renderTESR
	 */
	public static void renderTESR(@Nullable BlockEntity te, BlockPos pos, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
		if (te == null) {
			return;
		}
		BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(te);
		if (renderer != null) {
			poseStack.pushPose();
			poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
			renderer.render(te, 0, poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY);
			poseStack.popPose();
		}
	}

	/*
	 * Render types for the world space helpers. 1.12 state: blend on, texture off, depth mask off, line width 8.
	 */
	private static final class Types extends RenderStateShard {
		private Types() {
			super("aw_render_types", () -> {}, () -> {});
		}

		private static final RenderType LINES = RenderType.create("ancientwarfare_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1536,
				RenderType.CompositeState.builder()
						.setShaderState(RENDERTYPE_LINES_SHADER)
						.setLineState(new LineStateShard(OptionalDouble.of(8.0D)))
						.setLayeringState(VIEW_OFFSET_Z_LAYERING)
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setOutputState(ITEM_ENTITY_TARGET)
						.setWriteMaskState(COLOR_WRITE)
						.setCullState(NO_CULL)
						.createCompositeState(false));

		private static final RenderType LINES_NO_DEPTH = RenderType.create("ancientwarfare_lines_no_depth", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1536,
				RenderType.CompositeState.builder()
						.setShaderState(RENDERTYPE_LINES_SHADER)
						.setLineState(new LineStateShard(OptionalDouble.of(8.0D)))
						.setLayeringState(VIEW_OFFSET_Z_LAYERING)
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setOutputState(ITEM_ENTITY_TARGET)
						.setWriteMaskState(COLOR_WRITE)
						.setDepthTestState(NO_DEPTH_TEST)
						.setCullState(NO_CULL)
						.createCompositeState(false));

		private static final RenderType QUADS = RenderType.create("ancientwarfare_quads", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1536, false, true,
				RenderType.CompositeState.builder()
						.setShaderState(POSITION_COLOR_SHADER)
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setCullState(NO_CULL)
						.createCompositeState(false));
	}
}
