package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.GameType;
import net.shadowmage.ancientwarfare.structure.tile.SpawnerSettings;
import net.shadowmage.ancientwarfare.structure.tile.TileAdvancedSpawner;

import java.util.List;

/*
 * Port notes: 1.12 TESR: the name of the first entity of the first spawn group as a name plate above the spawner, only
 * for creative / spectator players. The hand made plate (quad + two drawString passes) is the shared name plate of
 * RenderLootInfo at the 1.12 scale (0.016666668 * 1.6).
 */
public class RenderAdvancedSpawner implements BlockEntityRenderer<TileAdvancedSpawner> {
	private final Font fontrenderer;

	public RenderAdvancedSpawner(BlockEntityRendererProvider.Context context) {
		fontrenderer = context.getFont();
	}

	@Override
	public void render(TileAdvancedSpawner te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gameMode == null || (!mc.player.isCreative() && mc.gameMode.getPlayerMode() != GameType.SPECTATOR)) {
			return;
		}

		List<SpawnerSettings.EntitySpawnGroup> spawnGroups = te.getSettings().getSpawnGroups();
		if (spawnGroups.isEmpty()) {
			return;
		}
		List<SpawnerSettings.EntitySpawnSettings> spawnSettings = spawnGroups.get(0).getEntitiesToSpawn();
		if (spawnSettings.isEmpty()) {
			return;
		}
		String string = spawnSettings.get(0).getCustomName().orElse(I18n.get(spawnSettings.get(0).getEntityName()));

		float f = 1.6F;
		float f1 = 0.016666668F * f;
		RenderLootInfo.drawNameplate(fontrenderer, string, pose, buffers, 0.5F, 1 + 0.5F, 0.5F, f1);
	}
}
