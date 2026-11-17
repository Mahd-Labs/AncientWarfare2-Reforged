package net.shadowmage.ancientwarfare.core.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/*
 * CLIENT ONLY. Texture read from a file on disk when the texture manager loads it
 * (Minecraft.getInstance().getTextureManager().register(location, new TextureFileBased(location, file))).
 */
public class TextureFileBased extends AbstractTexture {

	private final ResourceLocation location;
	private File file;

	public TextureFileBased(ResourceLocation par1ResourceLocation, File file) {
		this.location = par1ResourceLocation;
		this.file = file;
	}

	public ResourceLocation getLocation() {
		return location;
	}

	@Override
	public void load(ResourceManager par1ResourceManager) {
		try (InputStream input = new FileInputStream(file); NativeImage image = NativeImage.read(input)) {
			TextureUtil.prepareImage(getId(), image.getWidth(), image.getHeight());
			image.upload(0, 0, 0, false);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}
