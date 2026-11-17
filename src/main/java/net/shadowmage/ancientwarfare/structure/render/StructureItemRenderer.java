package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockStoneCoffin;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockWoodenCoffin;
import net.shadowmage.ancientwarfare.structure.model.ModelCoffin;
import net.shadowmage.ancientwarfare.structure.model.ModelStoneCoffin;

/*
 * Port notes: the 1.12 CodeChickenLib IItemRenderer halves of WoodenCoffinRenderer, StoneCoffinRenderer,
 * ProtectionFlagRenderer and RenderItemAdvancedLootChest (renderItem), as one BlockEntityWithoutLevelRenderer set on
 * those items through IClientItemExtensions. Their TRANSFORMS maps are the "display" blocks of the item models
 * (parent builtin/entity); as in 1.12 the item transform and the translate(-0.5, -0.5, -0.5) are already applied
 * when renderByItem is called.
 */
public class StructureItemRenderer extends BlockEntityWithoutLevelRenderer {
	public static final StructureItemRenderer INSTANCE = new StructureItemRenderer();

	private static final ChestBlockEntity CHEST_TE = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());

	private ModelCoffin coffinModel;
	private ModelStoneCoffin stoneCoffinModel;
	private ProtectionFlagRenderer flagRenderer;

	private StructureItemRenderer() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		coffinModel = null;
		stoneCoffinModel = null;
		flagRenderer = null;
	}

	private void initModels() {
		if (coffinModel == null) {
			EntityModelSet models = Minecraft.getInstance().getEntityModels();
			coffinModel = new ModelCoffin(models.bakeLayer(ModelCoffin.LAYER));
			stoneCoffinModel = new ModelStoneCoffin(models.bakeLayer(ModelStoneCoffin.LAYER));
			flagRenderer = new ProtectionFlagRenderer(models);
		}
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		initModels();
		Item item = stack.getItem();
		if (item == AWStructureBlocks.WOODEN_COFFIN_ITEM.get()) {
			//1.12 WoodenCoffinRenderer.renderItem
			ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("ancientwarfare", "textures/model/structure/coffin_" + ItemBlockWoodenCoffin.getVariant(stack).getSerializedName() + ".png");
			pose.pushPose();
			VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
			coffinModel.renderAll(pose, buffer, packedLight, packedOverlay, -1);
			pose.popPose();
		} else if (item == AWStructureBlocks.STONE_COFFIN_ITEM.get()) {
			//1.12 StoneCoffinRenderer.renderItem
			ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("ancientwarfare", "textures/model/structure/stone_coffin_" + ItemBlockStoneCoffin.getVariant(stack).getSerializedName() + ".png");
			pose.pushPose();
			VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
			stoneCoffinModel.renderAll(pose, buffer, packedLight, packedOverlay, -1);
			pose.popPose();
		} else if (item == AWStructureBlocks.PROTECTION_FLAG_ITEM.get() || item == AWStructureBlocks.DECORATIVE_FLAG_ITEM.get()) {
			flagRenderer.renderItem(stack, pose, buffers, packedLight);
		} else if (item == AWStructureBlocks.ADVANCED_LOOT_CHEST_ITEM.get()) {
			//1.12 RenderItemAdvancedLootChest.renderItem
			pose.pushPose();
			Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(CHEST_TE, pose, buffers, packedLight, packedOverlay);
			pose.popPose();
		}
	}
}
