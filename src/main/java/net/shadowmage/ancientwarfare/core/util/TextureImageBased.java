package net.shadowmage.ancientwarfare.core.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.awt.image.BufferedImage;

/*
 * CLIENT ONLY. 1.12: a SimpleTexture that uploaded a BufferedImage. 1.21.1: a DynamicTexture; the BufferedImage
 * constructor is kept and converts to a NativeImage, a NativeImage constructor is added.
 * Register with Minecraft.getInstance().getTextureManager().register(location, texture) (1.12 renderEngine.loadTexture).
 * The texture is uploaded in the constructor, which must run on the render thread.
 */
public class TextureImageBased extends DynamicTexture {

	BufferedImage image;
	private final ResourceLocation location;

	public TextureImageBased(ResourceLocation par1ResourceLocation, BufferedImage image) {
		super(toNativeImage(image));
		this.location = par1ResourceLocation;
		this.image = image;
	}

	public TextureImageBased(ResourceLocation par1ResourceLocation, NativeImage image) {
		super(image);
		this.location = par1ResourceLocation;
	}

	public ResourceLocation getLocation() {
		return location;
	}

	/*
	 * uploads the BufferedImage again (after it was drawn to), or the current NativeImage pixels if there is none
	 */
	public void reUploadImage() {
		if (image != null) {
			NativeImage pixels = getPixels();
			if (pixels != null && pixels.getWidth() == image.getWidth() && pixels.getHeight() == image.getHeight()) {
				copyPixels(image, pixels);
			} else {
				setPixels(toNativeImage(image));
			}
		}
		upload();
	}

	public static NativeImage toNativeImage(BufferedImage image) {
		NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);
		copyPixels(image, nativeImage);
		return nativeImage;
	}

	private static void copyPixels(BufferedImage image, NativeImage nativeImage) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getRGB(x, y);
				//NativeImage stores ABGR
				int abgr = (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
				nativeImage.setPixelRGBA(x, y, abgr);
			}
		}
	}

}
