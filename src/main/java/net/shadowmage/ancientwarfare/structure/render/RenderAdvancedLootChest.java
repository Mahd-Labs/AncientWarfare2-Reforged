package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.shadowmage.ancientwarfare.structure.tile.TileAdvancedLootChest;

public class RenderAdvancedLootChest extends RenderLootInfo<TileAdvancedLootChest> {
	private final ChestRenderer<TileAdvancedLootChest> chestRenderer;

	public RenderAdvancedLootChest(BlockEntityRendererProvider.Context context) {
		super(context);
		chestRenderer = new ChestRenderer<>(context);
	}

	@Override
	public void render(TileAdvancedLootChest te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		super.render(te, partialTicks, pose, buffers, packedLight, packedOverlay);
		if (te.getClass() == TileAdvancedLootChest.class) {
			chestRenderer.render(te, partialTicks, pose, buffers, packedLight, packedOverlay);
		}
	}
}
