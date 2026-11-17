package net.shadowmage.ancientwarfare.npc.event;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.NpcPlayerOwned;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFaction;
import net.shadowmage.ancientwarfare.npc.registry.FactionRegistry;
import net.shadowmage.ancientwarfare.npc.registry.NpcDefaultsRegistry;
import net.shadowmage.ancientwarfare.npc.xmod.IStructureHooks;
import net.shadowmage.ancientwarfare.npc.xmod.NpcXmod;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/*
 * Port notes:
 * - EntityJoinWorldEvent -> EntityJoinLevelEvent; the hostile AI injection looks through the target selector's
 *   WrappedGoals. NearestAttackableTargetGoal.targetType is protected, it is read through reflection (same field
 *   name in development and production, NeoForge runs on official names).
 * - everything that asked the structure module (structure / town maps, protection flags, special loot containers,
 *   the advanced spawner block) goes through NpcXmod.structure(); with that module absent none of it triggers.
 * - TileEntity.getTileData() -> BlockEntity.getPersistentData().
 */
public class EventHandler {
	public static final String NO_SPAWN_PREVENTION_TAG = "noSpawnPrevention";
	private Set<Predicate<Goal>> additionalHostileAIChecks = new HashSet<>();
	public static final EventHandler INSTANCE = new EventHandler();

	private static Field targetTypeField;

	private EventHandler() {
	}

	public void registerAdditionalHostileAICheck(Predicate<Goal> aiCheck) {
		additionalHostileAIChecks.add(aiCheck);
	}

	private static Class<?> getTargetClass(NearestAttackableTargetGoal<?> goal) {
		try {
			if (targetTypeField == null) {
				targetTypeField = NearestAttackableTargetGoal.class.getDeclaredField("targetType");
				targetTypeField.setAccessible(true);
			}
			return (Class<?>) targetTypeField.get(goal);
		}
		catch (ReflectiveOperationException | RuntimeException e) {
			AncientWarfare.LOG.debug("Unable to read target type of {}", goal, e);
			return null;
		}
	}

	private int getHostileAIPriority(PathfinderMob entity) {
		// Again search for a task to use for the same priority as our attack NPC task, but
		// look for "NearestAttackableTargetGoal" instead which is for ranged units
		for (WrappedGoal taskEntry : entity.targetSelector.getAvailableGoals()) {
			if (((taskEntry.getGoal() instanceof NearestAttackableTargetGoal && getTargetClass((NearestAttackableTargetGoal<?>) taskEntry.getGoal()) == Player.class)
					|| anyAdditionalHostileAIChecksMatch(taskEntry)) && taskEntry.getPriority() != -1) {
				return taskEntry.getPriority();
			}
		}
		return -1;
	}

	private boolean anyAdditionalHostileAIChecksMatch(WrappedGoal taskEntry) {
		for (Predicate<Goal> additionalCheck : additionalHostileAIChecks) {
			if (additionalCheck.test(taskEntry.getGoal())) {
				return true;
			}
		}
		return false;
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onEntityJoinWorld(EntityJoinLevelEvent event) {
		injectAi(event);
		preventHostileSpawnsInStructures(event);
	}

	private void preventHostileSpawnsInStructures(EntityJoinLevelEvent event) {
		Entity entity = event.getEntity();
		if (entity.level().isClientSide || !(entity instanceof Enemy) || isMarkedWithNoPreventionTag(entity)) {
			return;
		}

		Level world = event.getLevel();

		BlockPos pos = event.getEntity().blockPosition();

		// TODO-CROSS(structure): StructureMap.shouldPreventSpawnAtPos(world, pos) || TownMap.shouldPreventSpawnAtPos(world, pos)
		if (NpcXmod.structure().shouldPreventSpawnAtPos(world, pos)) {
			event.setCanceled(true);
		}
	}

	private boolean isMarkedWithNoPreventionTag(Entity entity) {
		return entity.getTags().contains(NO_SPAWN_PREVENTION_TAG);
	}

	private void injectAi(EntityJoinLevelEvent event) {
		if (event.getEntity() instanceof NpcBase) {
			return;
		}
		if (!(event.getEntity() instanceof PathfinderMob)) {
			return;
		}
		// Use new "auto injection"
		PathfinderMob entity = (PathfinderMob) event.getEntity();

		// all mobs that can attack somebody should have targetTasks
		int targetTaskPriority = getHostileAIPriority(entity);
		if (targetTaskPriority != -1) {
			entity.targetSelector.addGoal(targetTaskPriority, new NearestAttackableTargetGoal<>(entity, NpcBase.class, 0, true, false,
					e -> entityShouldTarget(entity, e)
			));
		}
	}

	private boolean entityShouldTarget(PathfinderMob entity, @Nullable LivingEntity target) {
		if (!(target instanceof NpcBase)) {
			return false;
		}
		NpcBase e = (NpcBase) target;

		if ((e instanceof NpcFaction) && !e.isPassive() && FactionRegistry.getFaction(((NpcFaction) e).getFaction()).isTarget(entity)) {
			return true;
		}

		return (e instanceof NpcPlayerOwned) && NpcDefaultsRegistry.getOwnedNpcDefault((NpcPlayerOwned) e).isTarget(entity);

	}

	@SubscribeEvent
	public void onChestClicked(PlayerInteractEvent.RightClickBlock evt) {
		Level world = evt.getLevel();
		BlockPos pos = evt.getPos();
		Player player = evt.getEntity();
		if (!player.getAbilities().instabuild && isLockedContainer(world, pos)) {
			// TODO-CROSS(structure): StructureMap.getStructureAt(world, pos) + TileProtectionFlag.shouldProtectAgainst(player)
			NpcXmod.structure().getStructureAt(world, pos).ifPresent(structure -> {
				if (structure.hasProtectionFlag(world) && structure.flagProtectsAgainst(world, player)) {
					evt.setCanceled(true);
					evt.setCancellationResult(InteractionResult.FAIL);
					if (world.isClientSide) {
						player.displayClientMessage(Component.translatable("gui.ancientwarfarenpc.no_chest_access_flag_not_claimed"), true);
					}
				} else {
					for (NpcFaction factionNpc : world.getEntitiesOfClass(NpcFaction.class, structure.getAABB())) {
						if (!factionNpc.isPassive()) {
							evt.setCanceled(true);
							evt.setCancellationResult(InteractionResult.FAIL);
							factionNpc.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200));
							if (world.isClientSide) {
								player.displayClientMessage(Component.translatable("gui.ancientwarfarenpc.no_chest_access",
										StringUtils.capitalize(factionNpc.getFaction())), true);
							}
							return;
						}
					}
				}
			});
		}
	}

	@SubscribeEvent
	public void playerDigSpeed(PlayerEvent.BreakSpeed evt) {
		if (evt.getEntity().isCreative() || evt.getEntity().isSpectator() || evt.getPosition().isEmpty()) {
			return;
		}

		Level world = evt.getEntity().level();
		BlockPos pos = evt.getPosition().get();
		// TODO-CROSS(structure): StructureMap.getStructureAt(world, pos) + TileProtectionFlag.shouldProtectAgainst(player)
		NpcXmod.structure().getStructureAt(world, pos).filter(structureEntry -> structureEntry.hasProtectionFlag(world)).ifPresent((IStructureHooks.IStructureEntry structureEntry) -> {
			if (structureEntry.flagProtectsAgainst(world, evt.getEntity()) && shouldBlockSlowDownDigging(world, pos)) {
				evt.setNewSpeed(evt.getOriginalSpeed() * 0.01f);
			}
		});
	}

	private boolean shouldBlockSlowDownDigging(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (!state.isCollisionShapeFullBlock(world, pos)) {
			return isLockedContainer(world, pos);
		}

		Block block = state.getBlock();
		// TODO-CROSS(structure): AWStructureBlocks.ADVANCED_SPAWNER
		return !(block == Blocks.SPAWNER || NpcXmod.structure().isAdvancedSpawner(block));
	}

	public static final String GENERATED_INVENTORY_TAG = "generatedInventory";

	private boolean isLockedContainer(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (!state.hasBlockEntity()) {
			return false;
		}

		// TODO-CROSS(structure): te instanceof ISpecialLootContainer
		return WorldTools.getTile(world, pos).map(te -> te.getPersistentData().getBoolean(GENERATED_INVENTORY_TAG) || NpcXmod.structure().isSpecialLootContainer(te)).orElse(false);
	}
}
