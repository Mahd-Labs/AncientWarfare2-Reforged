package net.shadowmage.ancientwarfare.automation.render;

import net.shadowmage.ancientwarfare.automation.item.ItemBlockTorqueTile;
import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.automation.block.TorqueTier;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueBase;
import net.shadowmage.ancientwarfare.core.render.lib.IconTransformation;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;



public abstract class TorqueTieredRenderer<T extends TileTorqueBase> extends BaseTorqueRenderer<T> {
	public Map<TorqueTier, TextureAtlasSprite> sprites = Maps.newHashMap();
	public Map<TorqueTier, IconTransformation> iconTransforms = Maps.newHashMap();

	protected TorqueTieredRenderer(String modelPath) {
		super(modelPath);
	}

	public void setSprite(TorqueTier torqueTier, TextureAtlasSprite sprite) {
		sprites.put(torqueTier, sprite);
		iconTransforms.put(torqueTier, new IconTransformation(sprite));
	}

	@Override
	protected IconTransformation getIconTransform(IExtendedBlockState state) {
		return iconTransforms.get(state.getValue(AutomationProperties.TIER));
	}

	@Override
	protected IconTransformation getIconTransform(ItemStack stack) {
		return iconTransforms.get(stack.getItem() instanceof ItemBlockTorqueTile && ((ItemBlockTorqueTile) stack.getItem()).getTier() != null ? ((ItemBlockTorqueTile) stack.getItem()).getTier() : TorqueTier.LIGHT);
	}

	public TextureAtlasSprite getSprite(TorqueTier torqueTier) {
		return sprites.get(torqueTier);
	}
}
