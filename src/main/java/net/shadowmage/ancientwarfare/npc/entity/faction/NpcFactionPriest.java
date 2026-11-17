package net.shadowmage.ancientwarfare.npc.entity.faction;

import net.minecraft.core.Holder;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.shadowmage.ancientwarfare.npc.ai.RestrictOpenDoorGoal;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIAttackNearest;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIDoor;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIFollowPlayer;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIMoveHome;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIWander;
import net.shadowmage.ancientwarfare.npc.ai.faction.NpcAIFactionHurt;
import net.shadowmage.ancientwarfare.npc.ai.faction.NpcAIFactionPriest;
import net.shadowmage.ancientwarfare.npc.ai.faction.NpcAIFactionRangedAttack;
import net.shadowmage.ancientwarfare.npc.entity.NoFriendlyFirePotion;

public class NpcFactionPriest extends NpcFaction implements RangedAttackMob {
	public NpcFactionPriest(EntityType<? extends NpcFactionPriest> type, Level world) {
		super(type, world);
		addAI();
	}

	public NpcFactionPriest(EntityType<? extends NpcFactionPriest> type, Level world, String factionName) {
		super(type, world, factionName);
		addAI();
	}

	private void addAI() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(0, new RestrictOpenDoorGoal(this));
		goalSelector.addGoal(0, new NpcAIDoor(this, true));
		goalSelector.addGoal(1, new NpcAIFollowPlayer(this));
		goalSelector.addGoal(2, new NpcAIMoveHome(this, 50F, 5F, 30F, 5F));
		goalSelector.addGoal(3, new NpcAIFactionPriest(this));
		goalSelector.addGoal(16, new NpcAIFactionRangedAttack(this, 0.8d, 20, 80));

		goalSelector.addGoal(101, new InteractGoal(this, Player.class, 3.0F, 1.0F));
		goalSelector.addGoal(102, new NpcAIWander(this));
		goalSelector.addGoal(103, new LookAtPlayerGoal(this, Mob.class, 8.0F));

		targetSelector.addGoal(1, new NpcAIFactionHurt(this, this::isHostileTowards));
		targetSelector.addGoal(15, new NpcAIAttackNearest(this,
				entity -> {
					//noinspection ConstantConditions
					if (!isHostileTowards(entity)) {
						return false;
					}
					if (hasRestriction()) {
						BlockPos home = getRestrictCenter();
						double dist = entity.distanceToSqr(home.getX() + 0.5d, home.getY(), home.getZ() + 0.5d);
						if (dist > 30 * 30) {
							return false;
						}
					}
					return true;
				}));
	}

	@Override
	public String getNpcType() {
		return "priest";
	}

	@Override
	public void performRangedAttack(LivingEntity target, float distanceFactor) {
		processWitchThrowPotionLogic(target);
	}

	//copy paste from EntityWitch with the exception of potion entity creation where getPotion is called to get no friendly fire potion
	private void processWitchThrowPotionLogic(LivingEntity target) {
		double d0 = target.getY() + (double) target.getEyeHeight() - 1.100000023841858D;
		double d1 = target.getX() + target.getDeltaMovement().x - getX();
		double d2 = d0 - getY();
		double d3 = target.getZ() + target.getDeltaMovement().z - getZ();
		float f = (float) Math.sqrt(d1 * d1 + d3 * d3);
		Holder<Potion> potiontype = Potions.HARMING;

		if (f >= 8.0F && !target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
			potiontype = Potions.SLOWNESS;
		} else if (target.getHealth() >= 8.0F && !target.hasEffect(MobEffects.POISON)) {
			potiontype = Potions.POISON;
		} else if (f <= 3.0F && !target.hasEffect(MobEffects.WEAKNESS) && random.nextFloat() < 0.25F) {
			potiontype = Potions.WEAKNESS;
		}

		ThrownPotion entitypotion = getPotion(potiontype);
		entitypotion.setXRot(entitypotion.getXRot() - -20.0F);
		entitypotion.shoot(d1, d2 + (double) (f * 0.2F), d3, 0.75F, 8.0F);
		level().playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_THROW, getSoundSource(), 1.0F, 0.8F + random.nextFloat() * 0.4F);
		level().addFreshEntity(entitypotion);
	}

	private ThrownPotion getPotion(Holder<Potion> potiontype) {
		return new NoFriendlyFirePotion(level(), this, net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.SPLASH_POTION, potiontype));
	}

	@Override
	protected boolean canAttackFlying() {
		return true;
	}

	@Override
	public boolean isPassive() {
		return false;
	}

	@Override
	public boolean canBeAffected(MobEffectInstance potionEffect) {
		//1.12: poison only applies when a PotionApplicableEvent listener explicitly allows it, i.e. priests are immune
		if (potionEffect.is(MobEffects.POISON)) {
			return false;
		}
		return super.canBeAffected(potionEffect);
	}
}
