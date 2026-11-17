package net.shadowmage.ancientwarfare.structure.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.RenderTools;
import net.shadowmage.ancientwarfare.structure.api.TemplateRuleBlock;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/*
 * Port notes: 1.12 kept the tessellated preview as a BufferBuilder.State and drew it with the fixed function block
 * state at RenderWorldLast. Here the blocks are tessellated once into a VertexBuffer (block vertex format, positions
 * relative to the box minimum) and drawn with the translucent chunk render state.
 * - TemplateRuleBlock.renderRule / renderRuleDynamic (client code inside the common rule classes) are done here:
 *   BlockRenderDispatcher.renderBatched for the rule state, RenderTools.renderTESR for rules with a tile
 * - 1.12 ran the tessellation loop three times ("pass" was never used), drawing every block three times; once here
 * - the cache key uses the stack content hash (1.12: identity hash of the client stack object, same intent)
 * - IBlockAccess.getCombinedLight returned a constant 8; the fake level hands out the light of the real world at the
 *   preview position
 * - clearCache: 1.12 called Cache.cleanUp() (which only drops expired entries); the cache is emptied here so a
 *   reloaded template is tessellated again and its vertex buffer is freed
 */
public class PreviewRenderer {
	private static final RemovalListener<Integer, CachedState> ON_REMOVE = notification -> {
		CachedState state = notification.getValue();
		if (state != null && state.vertexBuffer != null) {
			RenderSystem.recordRenderCall(state.vertexBuffer::close);
		}
	};
	private static Cache<Integer, CachedState> previewCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).removalListener(ON_REMOVE).build();

	private PreviewRenderer() {}

	public static void clearCache() {
		previewCache.invalidateAll();
	}

	public static void renderTemplatePreview(PoseStack pose, MultiBufferSource buffers, Player player, InteractionHand hand, ItemStack stack, float delta, StructureTemplate structure, StructureBB bb, int turns) {
		int cacheKey = getKey(stack, turns, hand);

		Optional<CachedState> state = Optional.ofNullable(previewCache.getIfPresent(cacheKey));
		if (!state.isPresent()) {
			Map<BlockPos, TemplateRuleBlock> dynamicRenderRules = new HashMap<>();
			VertexBuffer vertexBuffer = null;
			try (ByteBufferBuilder bytes = new ByteBufferBuilder(RenderType.translucent().bufferSize())) {
				BufferBuilder buffer = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
				renderPreview(structure, bb, turns, buffer, dynamicRenderRules);
				MeshData mesh = buffer.build();
				if (mesh != null) {
					vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
					vertexBuffer.bind();
					vertexBuffer.upload(mesh);
					VertexBuffer.unbind();
				}
			}
			CachedState stateToCache = new CachedState(bb.min, vertexBuffer, dynamicRenderRules);
			previewCache.put(cacheKey, stateToCache);
			state = Optional.of(stateToCache);
		}
		BlockPos bbMin = state.get().getBBMin();
		Vec3i offset = bb.min.offset(-bbMin.getX(), -bbMin.getY(), -bbMin.getZ());

		pose.pushPose();
		pose.translate(0.005F + offset.getX(), 0.005F + offset.getY(), 0.005F + offset.getZ());
		VertexBuffer vertexBuffer = state.get().getVertexBuffer();
		if (vertexBuffer != null) {
			pose.pushPose();
			pose.translate(bbMin.getX(), bbMin.getY(), bbMin.getZ());
			RenderType type = RenderType.translucent();
			type.setupRenderState();
			Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
			vertexBuffer.bind();
			vertexBuffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
			VertexBuffer.unbind();
			type.clearRenderState();
			pose.popPose();
		}
		renderDynamicRules(pose, buffers, turns, state.get());
		pose.popPose();
	}

	private static void renderDynamicRules(PoseStack pose, MultiBufferSource buffers, int turns, CachedState state) {
		Iterator<Map.Entry<BlockPos, TemplateRuleBlock>> it = state.getDynamicRenderRules().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<BlockPos, TemplateRuleBlock> entry = it.next();
			try {
				//1.12 TemplateRuleBlock.renderRuleDynamic
				RenderTools.renderTESR(entry.getValue().getTileEntity(turns), entry.getKey(), pose, buffers, LightTexture.FULL_BRIGHT);
			}
			catch (Exception e) {
				it.remove(); // remove erroring tile rendering
			}
		}
	}

	private static int getKey(ItemStack stack, int turns, InteractionHand hand) {
		int hash = ItemStack.hashItemAndComponents(stack);
		hash = hash * 31 + turns;
		hash = hash * 31 + hand.hashCode();
		return hash;
	}

	private static void renderPreview(StructureTemplate structure, StructureBB bb, int turns, BufferBuilder buffer, Map<BlockPos, TemplateRuleBlock> dynamicRenderRules) {
		TemplateBlockAccess blockAccess = new TemplateBlockAccess(structure, bb, turns);
		PoseStack pose = new PoseStack();
		RandomSource random = RandomSource.create();
		for (int y = 0; y < structure.getSize().getY(); y++) {
			for (int x = 0; x < structure.getSize().getX(); x++) {
				for (int z = 0; z < structure.getSize().getZ(); z++) {
					BlockPos pos = new BlockPos(x, y, z);
					renderPreviewAt(structure, bb, turns, buffer, dynamicRenderRules, blockAccess, pos, pose, random);
				}
			}
		}
	}

	private static void renderPreviewAt(StructureTemplate structure, StructureBB bb, int turns, BufferBuilder buffer, Map<BlockPos, TemplateRuleBlock> dynamicRenderRules, TemplateBlockAccess blockAccess, BlockPos pos, PoseStack pose, RandomSource random) {
		BlockPos translateTo = BlockTools.rotateInArea(pos, structure.getSize().getX(), structure.getSize().getZ(), turns).offset(bb.min);
		structure.getRuleAt(pos).ifPresent(r -> {
			//1.12 TemplateRuleBlock.renderRule
			BlockState state = r.getState(turns);
			if (state != null && state.getRenderShape() == RenderShape.MODEL) {
				pose.pushPose();
				pose.translate(translateTo.getX() - bb.min.getX(), translateTo.getY() - bb.min.getY(), translateTo.getZ() - bb.min.getZ());
				try {
					Minecraft.getInstance().getBlockRenderer().renderBatched(state, translateTo, blockAccess, pose, buffer, true, random);
				}
				catch (Exception e) {
					// a block that can not be tessellated outside a real level is left out
				}
				pose.popPose();
			}
			if (r.isDynamicallyRendered(turns)) {
				dynamicRenderRules.put(translateTo, r);
			}
		});
	}

	private static class TemplateBlockAccess implements BlockAndTintGetter {
		private final int templateXSize;
		private final int templateZSize;
		private StructureTemplate template;
		private StructureBB bb;
		private int turns;
		private Map<Long, BlockState> positionStates = new HashMap<>();
		private Map<Long, BlockEntity> positionTiles = new HashMap<>();
		private final ClientLevel level = Minecraft.getInstance().level;

		private TemplateBlockAccess(StructureTemplate template, StructureBB bb, int turns) {
			this.template = template;
			this.bb = bb;
			this.turns = turns;
			int xSize = template.getSize().getX();
			int zSize = template.getSize().getZ();

			if (turns % 2D != 0) {
				this.templateXSize = zSize;
				this.templateZSize = xSize;
			} else {
				this.templateXSize = xSize;
				this.templateZSize = zSize;
			}
		}

		@Nullable
		@Override
		public BlockEntity getBlockEntity(BlockPos pos) {
			long posSerialized = pos.asLong();
			if (!positionTiles.containsKey(posSerialized)) {
				if (!bb.contains(pos)) {
					positionTiles.put(posSerialized, null);
				} else {
					getBlockRuleAt(pos).ifPresent(templateRuleBlock -> positionTiles.put(posSerialized, templateRuleBlock.getTileEntity(turns)));
				}
			}
			return positionTiles.get(posSerialized);
		}

		@Override
		public BlockState getBlockState(BlockPos pos) {
			long posSerialized = pos.asLong();
			BlockState state = positionStates.get(posSerialized);
			if (state == null) {
				if (!bb.contains(pos)) {
					positionStates.put(posSerialized, Blocks.AIR.defaultBlockState());
				} else {
					positionStates.put(posSerialized, getBlockRuleAt(pos).map(templateRuleBlock -> templateRuleBlock.getState(turns)).orElse(Blocks.AIR.defaultBlockState()));
				}
				state = positionStates.get(posSerialized);
			}
			return state;
		}

		private Optional<TemplateRuleBlock> getBlockRuleAt(BlockPos pos) {
			Vec3i tempPos = BlockTools.rotateInArea(pos.offset(-bb.min.getX(), -bb.min.getY(), -bb.min.getZ()), templateXSize, templateZSize, -turns);
			return template.getRuleAt(tempPos);
		}

		@Override
		public FluidState getFluidState(BlockPos pos) {
			return getBlockState(pos).getFluidState();
		}

		@Override
		public float getShade(Direction direction, boolean shade) {
			return level.getShade(direction, shade);
		}

		@Override
		public LevelLightEngine getLightEngine() {
			return level.getLightEngine();
		}

		@Override
		public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
			return level.getBlockTint(pos, colorResolver);
		}

		@Override
		public int getHeight() {
			return level.getHeight();
		}

		@Override
		public int getMinBuildHeight() {
			return level.getMinBuildHeight();
		}
	}

	private static class CachedState {
		private final BlockPos bbMin;
		@Nullable
		private final VertexBuffer vertexBuffer;
		private final Map<BlockPos, TemplateRuleBlock> dynamicRenderRules;

		private CachedState(BlockPos bbMin, @Nullable VertexBuffer vertexBuffer, Map<BlockPos, TemplateRuleBlock> dynamicRenderRules) {
			this.bbMin = bbMin;
			this.vertexBuffer = vertexBuffer;
			this.dynamicRenderRules = dynamicRenderRules;
		}

		@Nullable
		private VertexBuffer getVertexBuffer() {
			return vertexBuffer;
		}

		private BlockPos getBBMin() {
			return bbMin;
		}

		private Map<BlockPos, TemplateRuleBlock> getDynamicRenderRules() {
			return dynamicRenderRules;
		}
	}
}
