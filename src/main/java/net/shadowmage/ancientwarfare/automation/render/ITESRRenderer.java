package net.shadowmage.ancientwarfare.automation.render;

import net.shadowmage.ancientwarfare.core.render.lib.CCRenderState;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;


public interface ITESRRenderer {
	void renderTransformedBlockModels(CCRenderState ccrs, IExtendedBlockState state);
}
