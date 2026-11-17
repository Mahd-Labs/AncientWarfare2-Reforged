package net.shadowmage.ancientwarfare.core.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;

/*
 * CLIENT ONLY.
 * Port notes: resource locations must be lower case in 1.21.1, so the location used as texture id is the lower
 * cased path (the file on disk is still looked up with the path as given). The bundled fallback is read through
 * the resource manager (assets/ancientwarfare/<path>) instead of opening the mod jar by hand.
 */
public class TextureUtils {
	private TextureUtils() {}

	public static ResourceLocation getTextureLocation(String path) {
		String overridePath = AWCoreStatics.configPathForFiles + path;
		ResourceLocation locationOverride = ResourceLocation.tryBuild(AncientWarfareCore.MOD_ID, overridePath.toLowerCase(Locale.ROOT));
		if (locationOverride != null && textureLoaded(locationOverride)) {
			return locationOverride;
		}
		ResourceLocation locationMain = ResourceLocation.tryBuild(AncientWarfareCore.MOD_ID, path.toLowerCase(Locale.ROOT));
		if (locationMain != null && textureLoaded(locationMain)) {
			return locationMain;
		}

		if (locationOverride != null && loadTexture(locationOverride, overridePath)) {
			return locationOverride;
		}

		if (locationMain != null && loadTextureFromAssets(locationMain)) {
			return locationMain;
		}

		return MissingTextureAtlasSprite.getLocation();
	}

	private static boolean textureLoaded(ResourceLocation loc) {
		return Minecraft.getInstance().getTextureManager().getTexture(loc, null) != null;
	}

	private static boolean loadTexture(ResourceLocation loc, String path) {
		File file = new File(path);
		return file.exists() && loadTexture(loc, file);
	}

	private static boolean loadTexture(ResourceLocation loc, File file) {
		try (InputStream input = new FileInputStream(file)) {
			NativeImage image = NativeImage.read(input);
			Minecraft.getInstance().getTextureManager().register(loc, new TextureImageBased(loc, image));

			return true;
		}
		catch (IOException e) {
			//noop
		}
		return false;
	}

	private static boolean loadTextureFromAssets(ResourceLocation loc) {
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(loc);
		if (resource.isEmpty()) {
			return false;
		}
		try (InputStream input = resource.get().open()) {
			NativeImage image = NativeImage.read(input);
			Minecraft.getInstance().getTextureManager().register(loc, new TextureImageBased(loc, image));
			return true;
		}
		catch (IOException e) {
			//noop
		}
		return false;
	}
}
