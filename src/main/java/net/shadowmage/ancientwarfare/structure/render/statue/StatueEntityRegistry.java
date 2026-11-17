package net.shadowmage.ancientwarfare.structure.render.statue;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/*
 * Port notes: client only (as in 1.12, where it lived next to the renderer). The entity and the statue model were
 * created again for every rendered frame in 1.12; they are kept per statue entity here (the entity only supplies
 * the texture and the renderer scale).
 */
public class StatueEntityRegistry {
	private StatueEntityRegistry() {}

	private static final Map<String, StatueEntity> STATUE_ENTITIES;

	static {
		ImmutableMap.Builder<String, StatueEntity> builder = new ImmutableMap.Builder<>();
		builder.put("Zombie", new StatueEntity("Zombie", EntityType.ZOMBIE::create, () -> new BipedStatueModel(ModelLayers.ZOMBIE)));
		builder.put("Enderman", new StatueEntity("Enderman", EntityType.ENDERMAN::create, () -> new BipedStatueModel(ModelLayers.ENDERMAN)));
		builder.put("Skeleton", new StatueEntity("Skeleton", EntityType.SKELETON::create, () -> new BipedStatueModel(ModelLayers.SKELETON)));
		builder.put("Vex", new StatueEntity("Vex", EntityType.VEX::create, VexStatueModel::new));
		builder.put("Zombie Villager", new StatueEntity("Zombie Villager", EntityType.ZOMBIE_VILLAGER::create, () -> new BipedStatueModel(ModelLayers.ZOMBIE_VILLAGER)));
		builder.put("Elder Guardian", new StatueEntity("Elder Guardian", EntityType.ELDER_GUARDIAN::create, () -> new GuardianStatueModel(ModelLayers.ELDER_GUARDIAN)));
		builder.put("Guardian", new StatueEntity("Guardian", EntityType.GUARDIAN::create, () -> new GuardianStatueModel(ModelLayers.GUARDIAN)));
		STATUE_ENTITIES = builder.build();
	}

	public static Set<String> getStatueEntityNames() {
		return STATUE_ENTITIES.keySet();
	}

	public static StatueEntity getStatueEntity(String name) {
		return STATUE_ENTITIES.get(name);
	}

	public static class StatueEntity {
		private String name;
		private Function<Level, LivingEntity> instantiateEntity;
		private Supplier<IStatueModel> getStatueModel;
		private LivingEntity entity;
		private IStatueModel statueModel;

		public StatueEntity(String name, Function<Level, LivingEntity> instantiateEntity, Supplier<IStatueModel> getStatueModel) {
			this.name = name;
			this.instantiateEntity = instantiateEntity;
			this.getStatueModel = getStatueModel;
		}

		public LivingEntity instantiateEntity(Level world) {
			if (entity == null || entity.level() != world) {
				entity = instantiateEntity.apply(world);
			}
			return entity;
		}

		public IStatueModel getStatueModel() {
			if (statueModel == null) {
				statueModel = getStatueModel.get();
			}
			return statueModel;
		}

		public String getName() {
			return name;
		}
	}
}
