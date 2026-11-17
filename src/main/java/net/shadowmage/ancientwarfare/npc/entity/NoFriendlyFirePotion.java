package net.shadowmage.ancientwarfare.npc.entity;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;

/*
 * Port notes: 1.12 overrode EntityPotion.applySplash; in 1.21.1 that method is private, so onHit is overridden and
 * handles the splash case itself (copy of ThrownPotion.applySplash with canApplyToEntity); everything else goes to
 * the vanilla implementation. The entity uses the vanilla potion entity type (1.12 rendered it with RenderPotion).
 */
public class NoFriendlyFirePotion extends ThrownPotion {
	public NoFriendlyFirePotion(Level worldIn, LivingEntity throwerIn, ItemStack potionDamageIn) {
		super(worldIn, throwerIn);
		setItem(potionDamageIn);
	}

	@Override
	protected void onHit(HitResult result) {
		PotionContents potioncontents = getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		if (level().isClientSide || !potioncontents.hasEffects() || isLingeringPotion()) {
			super.onHit(result);
			return;
		}
		applySplash(potioncontents.getAllEffects(), result.getType() == HitResult.Type.ENTITY ? ((EntityHitResult) result).getEntity() : null);
		int i = potioncontents.potion().isPresent() && potioncontents.potion().get().value().hasInstantEffects() ? 2007 : 2002;
		level().levelEvent(i, blockPosition(), potioncontents.getColor());
		discard();
	}

	private boolean isLingeringPotion() {
		return getItem().is(net.minecraft.world.item.Items.LINGERING_POTION);
	}

	//copy paste from ThrownPotion just with a change to call canApplyToEntity to allow for additional conditions there
	@SuppressWarnings("squid:S3776") //copy pasted so leaving as close to the original as possible
	private void applySplash(Iterable<MobEffectInstance> effects, @Nullable Entity entityHit) {
		AABB axisalignedbb = this.getBoundingBox().inflate(4.0D, 2.0D, 4.0D);
		List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, axisalignedbb);

		if (!list.isEmpty()) {
			Entity source = getEffectSource();
			for (LivingEntity entitylivingbase : list) {
				if (canApplyToEntity(entitylivingbase)) {
					double d0 = this.distanceToSqr(entitylivingbase);

					if (d0 < 16.0D) {
						double d1 = 1.0D - Math.sqrt(d0) / 4.0D;

						if (entitylivingbase == entityHit) {
							d1 = 1.0D;
						}

						for (MobEffectInstance potioneffect : effects) {
							Holder<MobEffect> potion = potioneffect.getEffect();

							if (potion.value().isInstantenous()) {
								potion.value().applyInstantenousEffect(this, this.getOwner(), entitylivingbase, potioneffect.getAmplifier(), d1);
							} else {
								int i = (int) (d1 * (double) potioneffect.getDuration() + 0.5D);

								if (i > 20) {
									entitylivingbase.addEffect(new MobEffectInstance(potion, i, potioneffect.getAmplifier(), potioneffect.isAmbient(), potioneffect.isVisible()), source);
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean canApplyToEntity(LivingEntity entitylivingbase) {
		Entity thrower = getOwner();
		return entitylivingbase.isAffectedByPotions() && (!(thrower instanceof NpcBase) || ((NpcBase) thrower).isHostileTowards(entitylivingbase));
	}
}
