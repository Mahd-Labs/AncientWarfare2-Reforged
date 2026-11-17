package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.core.util.RenderTools;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockStructureBuilder;
import net.shadowmage.ancientwarfare.structure.item.ItemConstructionTool;
import net.shadowmage.ancientwarfare.structure.item.ItemGateSpawner;
import net.shadowmage.ancientwarfare.structure.item.ItemStructureBuilder;
import net.shadowmage.ancientwarfare.structure.item.ItemStructureScanner;
import net.shadowmage.ancientwarfare.structure.item.ItemStructureSettings;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;

import java.awt.*;
import java.util.Optional;

/*
 * Port notes: in 1.12 every IBoxRenderer item drew its own box in renderBox(player, hand, stack, delta) and
 * IBoxRenderer.Util held the drawing helpers. Items are common classes now (no client code), so the five renderBox
 * bodies and the Util helpers live here, unchanged apart from the drawing target: the pose stack handed in is at the
 * world origin (already moved by -camera), which replaces RenderTools.adjustBBForPlayerPos.
 */
public final class BoxRenderers {
	private BoxRenderers() {}

	private static final Color GREEN = new Color(0, 255, 0);

	public static void renderBox(PoseStack pose, MultiBufferSource buffers, Player player, InteractionHand hand, ItemStack stack, float delta) {
		Item item = stack.getItem();
		if (item instanceof ItemGateSpawner) {
			renderGateSpawner(pose, buffers, player, stack);
		} else if (item instanceof ItemStructureBuilder) {
			renderStructureBuilder(pose, buffers, player, hand, stack, delta);
		} else if (item instanceof ItemBlockStructureBuilder) {
			renderBlockStructureBuilder(pose, buffers, player, hand, stack, delta);
		} else if (item instanceof ItemStructureScanner) {
			renderStructureScanner(pose, buffers, player, stack);
		} else if (item instanceof ItemConstructionTool) {
			renderConstructionTool(pose, buffers, player, stack);
		}
	}

	/* 1.12 ItemGateSpawner.renderBox */
	private static void renderGateSpawner(PoseStack pose, MultiBufferSource buffers, Player player, ItemStack stack) {
		CompoundTag tag = ItemTools.hasTag(stack) ? ItemTools.getTag(stack) : null;
		BlockPos p1;
		BlockPos p2;
		if (tag != null && tag.contains(ItemGateSpawner.AW_GATE_INFO_TAG)) {
			tag = tag.getCompound(ItemGateSpawner.AW_GATE_INFO_TAG);
			if (tag.contains("pos1")) {
				p1 = BlockPos.of(tag.getLong("pos1"));
				if (tag.contains("pos2")) {
					p2 = BlockPos.of(tag.getLong("pos2"));
				} else {
					p2 = BlockTools.getBlockClickedOn(player, player.level(), true);
					if (p2 == null) {
						return;
					}
				}
			} else {
				p1 = BlockTools.getBlockClickedOn(player, player.level(), true);
				if (p1 == null) {
					return;
				}
				p2 = p1;
			}
		} else {
			p1 = BlockTools.getBlockClickedOn(player, player.level(), true);
			if (p1 == null) {
				return;
			}
			p2 = p1;
		}
		renderBoundingBox(pose, buffers, BlockTools.getMin(p1, p2), BlockTools.getMax(p1, p2));
	}

	/* 1.12 ItemStructureBuilder.renderBox */
	private static void renderStructureBuilder(PoseStack pose, MultiBufferSource buffers, Player player, InteractionHand hand, ItemStack stack, float delta) {
		ItemStructureSettings settings = ItemStructureSettings.getSettingsFor(stack);
		if (!settings.hasName()) {
			return;
		}
		String name = settings.name();
		Optional<StructureTemplate> template = StructureTemplateManager.getTemplate(name);
		if (!template.isPresent()) {
			return;
		}
		Optional<Tuple<BlockPos, Direction>> buildPos = ((ItemStructureBuilder) stack.getItem()).getBuildPosFromStackOrPlayer(player, stack);
		if (!buildPos.isPresent())
			return;

		BlockPos hit = buildPos.get().getA();
		Direction facing = buildPos.get().getB();

		StructureBB bb = new StructureBB(hit, facing, template.get().getSize(), template.get().getOffset());
		int turns = (facing.get2DDataValue() + 2) % 4;
		renderBoundingBox(pose, buffers, bb.min, bb.max);
		renderBoundingBox(pose, buffers, hit, hit);

		PreviewRenderer.renderTemplatePreview(pose, buffers, player, hand, stack, delta, template.get(), bb, turns);
	}

	/* 1.12 ItemBlockStructureBuilder.renderBox */
	private static void renderBlockStructureBuilder(PoseStack pose, MultiBufferSource buffers, Player player, InteractionHand hand, ItemStack stack, float delta) {
		if (!ItemTools.hasTag(stack) || !ItemTools.getTag(stack).contains(ItemBlockStructureBuilder.STRUCTURE_NAME_TAG)) {
			return;
		}
		String name = ItemTools.getTag(stack).getString(ItemBlockStructureBuilder.STRUCTURE_NAME_TAG);
		BlockPos hit = BlockTools.getBlockClickedOn(player, player.level(), true);
		if (hit == null) {
			return;
		}
		StructureTemplateManager.getTemplate(name).ifPresent(t -> {
			renderBoundingBox(pose, buffers, hit, hit);
			Direction face = player.getDirection();
			BlockPos p2 = hit.relative(face, t.getSize().getZ() - 1 - t.getOffset().getZ() + 1);
			StructureBB bb = new StructureBB(p2, face, t.getSize(), t.getOffset());
			renderBoundingBox(pose, buffers, bb.min, bb.max);
			PreviewRenderer.renderTemplatePreview(pose, buffers, player, hand, stack, delta, t, bb, (face.get2DDataValue() + 2) % 4);
		});
	}

	/* 1.12 ItemStructureScanner.renderBox */
	public static void renderStructureScanner(PoseStack pose, MultiBufferSource buffers, Player player, ItemStack stack) {
		ItemStructureSettings settings = ItemStructureSettings.getSettingsFor(stack);
		BlockPos firstCorner;
		BlockPos secondCorner;
		BlockPos min;
		BlockPos max;

		if (settings.hasPos1()) {
			firstCorner = settings.getPos1();
		} else {
			firstCorner = BlockTools.getBlockClickedOn(player, player.level(), player.isShiftKeyDown());
		}
		if (settings.hasPos2()) {
			secondCorner = settings.getPos2();
		} else {
			secondCorner = BlockTools.getBlockClickedOn(player, player.level(), player.isShiftKeyDown());
		}
		if (firstCorner == null || secondCorner == null) {
			return;
		}
		min = BlockTools.getMin(firstCorner, secondCorner);
		max = BlockTools.getMax(firstCorner, secondCorner);
		renderBoundingBox(pose, buffers, min, max);

		if (settings.hasPos2()) {
			BlockPos buildKey = settings.buildKey();
			if (!settings.hasBuildKey()) {
				buildKey = BlockTools.getBlockClickedOn(player, player.level(), player.isShiftKeyDown());
			}

			if (buildKey != null) {
				renderBoundingBox(pose, buffers, buildKey, buildKey, GREEN);
				BlockPos outerFirst = new BlockPos(min.getX() - 1, buildKey.getY(), min.getZ() - 1);
				BlockPos outerSecond = new BlockPos(max.getX() + 1, buildKey.getY(), max.getZ() + 1);
				renderBoundingBoxTopSide(pose, buffers, outerFirst, outerSecond, new Color(GREEN.getRed(), GREEN.getGreen(), GREEN.getBlue(), 80));
			}
		}
	}

	/* 1.12 ItemConstructionTool.renderBox */
	private static void renderConstructionTool(PoseStack pose, MultiBufferSource buffers, Player player, ItemStack stack) {
		ItemConstructionTool.ConstructionSettings settings = ItemConstructionTool.getSettings(stack);
		if (settings == null) {
			return;
		}
		BlockPos p1, p2;
		BlockPos p3 = BlockTools.getBlockClickedOn(player, player.level(), player.isShiftKeyDown());
		p1 = settings.hasPos1() ? settings.pos1() : p3;
		p2 = settings.hasPos2() ? settings.pos2() : p3;
		if (p1 != null && p2 != null) {
			renderBoundingBox(pose, buffers, BlockTools.getMin(p1, p2), BlockTools.getMax(p1, p2));
		}
		if (p3 != null) {
			renderBoundingBox(pose, buffers, p3, p3, 1, 0, 0);
		}
	}

	/* --- 1.12 IBoxRenderer.Util --- */

	private static AABB box(BlockPos min, BlockPos max) {
		return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
	}

	public static void renderBoundingBoxTopSide(PoseStack pose, MultiBufferSource buffers, BlockPos min, BlockPos max, Color color) {
		RenderTools.drawTopSideOverlay(pose, buffers, box(min, max), color);
	}

	public static void renderBoundingBox(PoseStack pose, MultiBufferSource buffers, BlockPos min, BlockPos max, Color color) {
		RenderTools.drawOutlinedBoundingBox(pose, buffers, box(min, max), color);
	}

	public static void renderBoundingBox(PoseStack pose, MultiBufferSource buffers, BlockPos min, BlockPos max) {
		RenderTools.drawOutlinedBoundingBox(pose, buffers, box(min, max), 1.f, 1.f, 1.f);
	}

	public static void renderBoundingBox(PoseStack pose, MultiBufferSource buffers, BlockPos min, BlockPos max, float r, float g, float b) {
		RenderTools.drawOutlinedBoundingBox(pose, buffers, box(min, max), r, g, b);
	}
}
