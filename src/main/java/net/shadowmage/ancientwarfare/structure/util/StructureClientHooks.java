package net.shadowmage.ancientwarfare.structure.util;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;

/*
 * Port notes: replaces the 1.12 CommonProxyStructure / ClientProxyStructure pair and the places where common code
 * called client classes directly (guarded by world.isRemote / @SideOnly). The client entry point
 * (AncientWarfareStructureClient) installs the real implementation; on a dedicated server the no-op defaults stay
 * (PORTING.md rule 4).
 */
public class StructureClientHooks {
	private static StructureClientHooks instance = new StructureClientHooks();

	public static StructureClientHooks get() {
		return instance;
	}

	public static void set(StructureClientHooks hooks) {
		instance = hooks;
	}

	/* 1.12 Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleSun(world, x, y, z)) */
	public void addSunParticle(Level world, double x, double y, double z) {
	}

	/* 1.12 effectRenderer.addEffect(new ParticleColoredFlame(world, x, y, z, xSpeed, ySpeed, zSpeed, color)) */
	public void addColoredFlameParticle(Level world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int color) {
	}

	/* 1.12 BlockHighlightRenderer / PacketHighlightBlock target */
	public void setBlockHighlightInfo(BlockHighlightInfo info) {
	}

	/* --- 1.12 CommonProxyStructure --- */

	public void clearTemplatePreviewCache() {
		//noop here, overriden in the client proxy
	}

	public void resetSoundAt(BlockPos pos) {
		//noop here, overriden in the client proxy
	}

	public void setSoundAt(BlockPos pos, SoundEvent currentTune, float volume) {
		//noop here, overriden in the client proxy
	}

	public void stopSoundAt(BlockPos pos) {
		//noop here, overriden in the client proxy
	}

	public boolean hasSoundAt(BlockPos pos) {
		return false;
	}

	public boolean isSoundPlayingAt(BlockPos pos) {
		return false;
	}

	public void playSoundAt(BlockPos pos) {
		//noop here, overriden in the client proxy
	}

	public double getClientPlayerDistanceTo(BlockPos pos) {
		return 0;
	}

	/* 1.12 InputHandler.ALT_ITEM_USE_<n>.getDisplayName() (item tooltips); empty on a dedicated server */
	public String getKeyName(int altItemUseKey) {
		return "";
	}

	public Optional<Player> getPlayer() {
		return Optional.empty();
	}
}
