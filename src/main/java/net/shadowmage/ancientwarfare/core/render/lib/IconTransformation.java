/*
 * Derived from CodeChickenLib 3.2.3 (TheCBProject/CodeChickenLib, branch 1.12.x), Copyright ChickenBones and
 * covers1624, licensed under the GNU LGPL 2.1; distributed here under the GNU GPL v3 or later as permitted by
 * section 3 of the LGPL 2.1.
 * Source file: codechicken/lib/vec/uv/IconTransformation.java. Changed 2026-11-14 for the port of Ancient Warfare
 * 2 to NeoForge 1.21.1: rewritten for TextureAtlasSprite of Minecraft 1.21.1.
 */
package net.shadowmage.ancientwarfare.core.render.lib;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/*
 * Stand in for codechicken.lib.vec.uv.IconTransformation: maps model texture coordinates (0..1) onto an atlas sprite.
 */
public class IconTransformation {
	public TextureAtlasSprite icon;

	public IconTransformation(TextureAtlasSprite icon) {
		this.icon = icon;
	}

	public float getU(double u) {
		return icon.getU((float) u);
	}

	public float getV(double v) {
		return icon.getV((float) v);
	}
}
