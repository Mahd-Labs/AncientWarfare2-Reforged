package net.shadowmage.ancientwarfare.npc.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/*
 * Port notes: the 1.12 item had four meta sub types; flattened into bard_instrument_lute / _flute / _harp / _drum
 * (meta 0-3 = getInstrument()). Translation keys stay item.bard_instrument.<name>.name.
 */
public class ItemBardInstrument extends ItemBaseNPC {

	public static final String[] instrumentNames = new String[] {"lute", "flute", "harp", "drum"};

	private final int meta;

	public ItemBardInstrument(String regName, int meta) {
		super(regName);
		this.meta = meta;
	}

	/* the 1.12 item damage (meta) */
	public int getInstrument() {
		return meta;
	}

	@Override
	public String getDescriptionId() {
		return "item.bard_instrument." + instrumentNames[meta] + ".name";
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!world.isClientSide) {
			SoundEvent s;
			s = SoundEvents.NOTE_BLOCK_BASEDRUM.value();
			if (meta == 0) {
				s = SoundEvents.NOTE_BLOCK_BASS.value();
			} else if (meta == 1) {
				s = SoundEvents.NOTE_BLOCK_FLUTE.value();
			} else if (meta == 2) {
				s = SoundEvents.NOTE_BLOCK_HARP.value();
			}
			world.playSound(null, player.getX() + 0.5, player.getY() + 0.5, player.getZ() + 0.5, s, SoundSource.PLAYERS, 2.0F, 1.0F);
		}
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
	}

	@Override
	public boolean onEntitySwing(ItemStack stack, LivingEntity living, InteractionHand hand) {
		return true;
	}
}
