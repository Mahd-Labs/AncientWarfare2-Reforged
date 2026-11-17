package net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.entityrules;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.api.IStructureBuilder;
import net.shadowmage.ancientwarfare.structure.api.TemplateRuleEntityBase;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static net.shadowmage.ancientwarfare.npc.event.EventHandler.NO_SPAWN_PREVENTION_TAG;

public class TemplateRuleEntity<T extends Entity> extends TemplateRuleEntityBase {
	public static final String PLUGIN_NAME = "entity";
	private CompoundTag tag;

	public ResourceLocation registryName;
	public float xOffset;
	public float zOffset;
	public float yOffset;
	public float rotation;

	public TemplateRuleEntity() {
		super();
	}

	public TemplateRuleEntity(Level world, T entity, int turns, int x, int y, int z) {
		super();

		registryName = net.shadowmage.ancientwarfare.structure.util.EntityHelper.getId(entity);

		rotation = (entity.getYRot() + 90.f * turns) % 360.f;
		float x1 = (float) (entity.getX() % 1.d);
		float z1 = (float) (entity.getZ() % 1.d);
		if (x1 < 0) {
			x1++;
		}
		if (z1 < 0) {
			z1++;
		}
		xOffset = BlockTools.rotateFloatX(x1, z1, turns);
		zOffset = BlockTools.rotateFloatZ(x1, z1, turns);
		yOffset = (float) (entity.getY() % 1d);

		tag = entity.saveWithoutId(new CompoundTag());
		tag.remove("UUIDMost");
		tag.remove("UUIDLeast");
	}

	@Override
	public void handlePlacement(Level world, int turns, BlockPos pos, IStructureBuilder builder) {
		createEntity(world, turns, pos).ifPresent(world::addFreshEntity);
	}

	//Port addition: 1.12 item stacks / uuids / custom names of original templates are upgraded on first use (see TemplateRuleBlockTile)
	private boolean legacyDataUpgraded = true;

	private Optional<T> createEntity(Level world, int turns, BlockPos pos) {
		if (!legacyDataUpgraded && tag != null) {
			net.shadowmage.ancientwarfare.structure.util.LegacyItemStacks.upgradeNbt(tag);
		}
		legacyDataUpgraded = true;
		//noinspection unchecked
		T e = (T) net.shadowmage.ancientwarfare.structure.util.EntityHelper.createEntity(registryName, world);
		if (e == null) {
			AncientWarfareStructure.LOG.debug("Could not create entity for name: {} Entity skipped during structure creation.", registryName);
			return Optional.empty();
		}
		CompoundTag entityNBT = getEntityNBT(pos, turns);
		removeNonExistentAttributes(e, entityNBT);

		e.load(entityNBT);
		updateEntityOnPlacement(turns, pos, e);
		addNoSpawnPreventionTag(e);
		return Optional.of(e);
	}

	private void removeNonExistentAttributes(T e, CompoundTag entityNBT) {
		if (e instanceof LivingEntity) {
			LivingEntity living = (LivingEntity) e;
			ListTag attributes = entityNBT.getList("Attributes", Tag.TAG_COMPOUND);
			Iterator<Tag> it = attributes.iterator();
			while(it.hasNext()) {
				Tag nbt = it.next();
				if (nbt.getId() == Tag.TAG_COMPOUND) {
					CompoundTag attribute = (CompoundTag) nbt;
					if (!net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(net.minecraft.resources.ResourceLocation.tryParse(attribute.getString("Name") == null ? "" : attribute.getString("Name").toLowerCase(java.util.Locale.ROOT))).map(h -> living.getAttributes().hasAttribute(h)).orElse(false)) {
						it.remove();
					}
				}
			}
		}
	}

	private void addNoSpawnPreventionTag(T e) {
		if (e instanceof Enemy) {
			e.getTags().add(NO_SPAWN_PREVENTION_TAG);
		}
	}

	@SuppressWarnings("unused") //parameters used in overrides
	protected CompoundTag getEntityNBT(BlockPos pos, int turns) {
		return tag;
	}

	protected void updateEntityOnPlacement(int turns, BlockPos pos, T e) {
		e.absMoveTo(pos.getX() + BlockTools.rotateFloatX(xOffset, zOffset, turns), pos.getY() + yOffset,
				pos.getZ() + BlockTools.rotateFloatZ(xOffset, zOffset, turns), (rotation + 90.f * turns) % 360.f, 0);
	}

	@Override
	public void writeRuleData(CompoundTag tag) {
		super.writeRuleData(tag);
		tag.putString("mobID", registryName.toString());
		tag.putFloat("xOffset", xOffset);
		tag.putFloat("yOffset", yOffset);
		tag.putFloat("zOffset", zOffset);
		tag.putFloat("rotation", rotation);
		tag.put("entityData", this.tag);
	}

	@Override
	public void parseRule(CompoundTag tag) {
		super.parseRule(tag);
		registryName = ResourceLocation.parse(tag.getString("mobID"));
		xOffset = tag.getFloat("xOffset");
		yOffset = tag.getFloat("yOffset");
		zOffset = tag.getFloat("zOffset");
		rotation = tag.getFloat("rotation");
		this.tag = tag.getCompound("entityData");
		legacyDataUpgraded = !isLegacyData();
	}

	@Override
	public String getPluginName() {
		return PLUGIN_NAME;
	}

	@Override
	public boolean shouldPlaceOnBuildPass(Level world, int turns, BlockPos pos, int buildPass) {
		return buildPass == 3;
	}

	@Override
	public List<ItemStack> getResources() {
		return Collections.emptyList();
	}
}
