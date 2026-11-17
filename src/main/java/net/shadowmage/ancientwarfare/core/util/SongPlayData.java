package net.shadowmage.ancientwarfare.core.util;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.shadowmage.ancientwarfare.AncientWarfare;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SongPlayData {
	private boolean random = false;
	private boolean playOnPlayerEntry = false;
	private int minDelay;
	private int maxDelay;
	private List<SongEntry> tunes = new ArrayList<>();

	public int size() {
		return tunes.size();
	}

	public SongEntry get(int index) {
		return tunes.get(index);
	}

	public void addNewEntry() {
		SongEntry e = new SongEntry();
		tunes.add(e);
	}

	public void decrementEntry(int index) {
		if (index <= 0 || index >= tunes.size()) {
			return;
		}
		SongEntry e = tunes.remove(index);
		index--;
		tunes.add(index, e);
	}

	public void incrementEntry(int index) {
		if (index < 0 || index >= tunes.size() - 1) {
			return;
		}
		SongEntry e = tunes.remove(index);
		index++;
		tunes.add(index, e);
	}

	public void deleteEntry(int index) {
		if (index < 0 || index >= tunes.size()) {
			return;
		}
		tunes.remove(index);
	}

	public int getMinDelay() {
		return minDelay;
	}

	public int getMaxDelay() {
		return maxDelay;
	}

	public boolean getPlayOnPlayerEntry() {
		return playOnPlayerEntry;
	}

	public boolean getIsRandom() {
		return random;
	}

	public void setMinDelay(int val) {
		minDelay = val;
	}

	public void setMaxDelay(int val) {
		maxDelay = val;
	}

	public void setPlayOnPlayerEntry(boolean val) {
		playOnPlayerEntry = val;
	}

	public void setRandom(boolean val) {
		random = val;
	}

	public void readFromNBT(CompoundTag tag) {
		minDelay = tag.getInt("minDelay");
		maxDelay = tag.getInt("maxDelay");
		if (maxDelay < minDelay) {
			maxDelay = minDelay;
		}
		random = tag.getBoolean("random");
		playOnPlayerEntry = tag.getBoolean("playerEntry");
		tunes.clear();
		SongEntry d;
		ListTag l = tag.getList("entries", Tag.TAG_COMPOUND);
		for (int i = 0; i < l.size(); i++) {
			d = new SongEntry();
			d.readFromNBT(l.getCompound(i));
			tunes.add(d);
		}
	}

	public CompoundTag writeToNBT(CompoundTag tag) {
		tag.putInt("minDelay", minDelay);
		tag.putInt("maxDelay", maxDelay);
		tag.putBoolean("random", random);
		tag.putBoolean("playerEntry", playOnPlayerEntry);
		ListTag l = new ListTag();
		for (SongEntry tune : tunes) {
			l.add(tune.writeToNBT(new CompoundTag()));
		}
		tag.put("entries", l);
		return tag;
	}

	public static final class SongEntry {
		private static final String AUTO_LOAD_PREFIX = "auto_load/";
		private ResourceLocation soundRegistryName;
		@Nullable
		private SoundEvent sound;
		private float length;//length in seconds, used to determine when count down for next tune should start
		private int volume;// percentage, as integer 0 = 0%, 100=100%, 150=150%

		private SongEntry() {
			soundRegistryName = null;
			sound = null;
			length = 5;
			volume = 100;
		}

		public void setLength(float length) {
			this.length = length;
		}

		public void setSound(@Nullable SoundEvent sound) {
			this.sound = sound;
			soundRegistryName = sound == null ? null : BuiltInRegistries.SOUND_EVENT.getKey(sound);
			if (sound != null) {
				boolean isRecord = sound.getLocation().getPath().startsWith("music_disc.");
				if (isRecord && length() < 120) {
					setLength(120);
				}
			}
		}

		public void setVolume(int volume) {
			this.volume = volume;
		}

		public int volume() {
			return volume;
		}

		public String name() {
			return sound != null ? sound.getLocation().toString() : "";
		}

		public Optional<SoundEvent> getSound() {
			return Optional.ofNullable(sound);
		}

		public float length() {
			return length;
		}

		public void readFromNBT(CompoundTag tag) {
			if (tag.contains("name") && RegistryTools.parseOrNull(tag.getString("name")) != null) {
				soundRegistryName = RegistryTools.parseOrNull(tag.getString("name"));
				sound = BuiltInRegistries.SOUND_EVENT.getOptional(soundRegistryName).orElse(null);
				if (sound == null) {
					trySoundLookup();
				}
			}
			length = tag.getFloat("length");
			volume = tag.getInt("volume");
		}

		private void trySoundLookup() {
			if (!soundRegistryName.getNamespace().startsWith(AncientWarfare.MOD_ID) || !soundRegistryName.getPath().startsWith(AUTO_LOAD_PREFIX)) {
				return;
			}

			String soundDomain = soundRegistryName.getNamespace();
			String soundName = soundRegistryName.getPath().substring(AUTO_LOAD_PREFIX.length());

			SoundEvent found = null;
			for (SoundEvent soundEvent : BuiltInRegistries.SOUND_EVENT) {
				ResourceLocation soundEventRegistryName = BuiltInRegistries.SOUND_EVENT.getKey(soundEvent);
				//noinspection ConstantConditions
				String resPath = soundEventRegistryName.getPath();
				if (soundEventRegistryName.getNamespace().equals(soundDomain)
						&& (resPath.equals(soundName) || resPath.endsWith("/" + soundName))) {
					found = soundEvent;
					if (!resPath.startsWith(AUTO_LOAD_PREFIX)) {
						break;
					}
				}
			}

			if (found != null) {
				AncientWarfare.LOG.info("Sound {} replaced with automatically found {}", soundRegistryName, BuiltInRegistries.SOUND_EVENT.getKey(found));
				sound = found;
				soundRegistryName = BuiltInRegistries.SOUND_EVENT.getKey(found);
			} else {
				AncientWarfare.LOG.error("Sound {} no longer exists in the sound registry and no replacement was automatically found.\n"
						+ "The sound name will be saved in case the sound is reregistered.", soundRegistryName);
			}
		}

		public CompoundTag writeToNBT(CompoundTag tag) {
			if (soundRegistryName != null) {
				tag.putString("name", soundRegistryName.toString());
			}
			tag.putFloat("length", length);
			tag.putInt("volume", volume);
			return tag;
		}
	}
}