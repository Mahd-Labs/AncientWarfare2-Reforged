package net.shadowmage.ancientwarfare.structure.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Optional;

/*
 * Port notes: same tag layout as 1.12 (RegistryName, Amplifier, Duration, Ambient, ShowParticles). The Forge 1.12
 * "CurativeItems" list is not written or read: 1.21 effects have no curative item list.
 */
public class PotionHelper {
	private static final String SHOW_PARTICLES_TAG = "ShowParticles";

	private PotionHelper() {}

	public static CompoundTag writeCustomPotionEffectToNBT(MobEffectInstance potionEffect) {
		CompoundTag nbt = new CompoundTag();
		ResourceLocation key = BuiltInRegistries.MOB_EFFECT.getKey(potionEffect.getEffect().value());
		nbt.putString("RegistryName", key == null ? "" : key.toString());
		nbt.putByte("Amplifier", (byte) potionEffect.getAmplifier());
		nbt.putInt("Duration", potionEffect.getDuration());
		nbt.putBoolean("Ambient", potionEffect.isAmbient());
		nbt.putBoolean(SHOW_PARTICLES_TAG, potionEffect.isVisible());
		return nbt;
	}

	public static Optional<MobEffectInstance> readCustomPotionEffectFromNBT(CompoundTag nbt) {
		ResourceLocation registryName = ResourceLocation.tryParse(nbt.getString("RegistryName"));
		Optional<Holder.Reference<MobEffect>> potion = registryName == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.getHolder(registryName);
		if (potion.isEmpty()) {
			return Optional.empty();
		} else {
			int j = nbt.getByte("Amplifier");
			int k = nbt.getInt("Duration");
			boolean flag = nbt.getBoolean("Ambient");
			boolean flag1 = true;
			if (nbt.contains(SHOW_PARTICLES_TAG, 1)) {
				flag1 = nbt.getBoolean(SHOW_PARTICLES_TAG);
			}
			return Optional.of(new MobEffectInstance(potion.get(), k, j < 0 ? 0 : j, flag, flag1));
		}
	}
}
