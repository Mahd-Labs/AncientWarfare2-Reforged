package net.shadowmage.ancientwarfare.structure.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

/*
 * Port notes: 1.12 Particle on the vanilla particle sheet, texture index 151 - age * 8 / maxAge = the eight spell
 * ("effect") sprites from the largest to the smallest. The sheet is an atlas now: the same sprites are looked up by
 * name (minecraft:effect_7 .. effect_0). Added straight to the particle engine as 1.12 did (no particle type).
 */
public class ParticleSun extends TextureSheetParticle {
	public ParticleSun(ClientLevel world, double x, double y, double z) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D);
		this.xd = 0;
		this.zd = 0;
		yd = 0;
		float f = (float) (Math.random() * 0.1f + 0.9f);
		rCol = 1f * f;
		gCol = 213 / 255f * f;
		bCol = 74 / 255f * f;
		quadSize *= 0.5F;
		float scale = world.random.nextFloat() * 0.3f + 0.7f;
		quadSize *= scale;
		lifetime = (int) (16.0D / (Math.random() * 0.8D + 0.2D));
		lifetime = (int) ((float) lifetime * scale);
		setParticleTextureIndex(151);
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;

		if (age++ >= lifetime) {
			remove();
		}

		setParticleTextureIndex(151 - age * 8 / Math.max(1, lifetime));
		move(xd, yd, zd);

		xd *= 0.96D;
		yd *= 0.96D;
		zd *= 0.96D;
	}

	private void setParticleTextureIndex(int index) {
		AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
		if (texture instanceof TextureAtlas atlas) {
			setSprite(atlas.getSprite(ResourceLocation.withDefaultNamespace("effect_" + Math.max(0, Math.min(7, index - 144)))));
		}
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}
}
