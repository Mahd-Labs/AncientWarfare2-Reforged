package net.shadowmage.ancientwarfare.structure.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;

import java.util.Optional;
import java.util.function.Supplier;

/*
 * Port notes: 1.12 entity capability (attached to every EntityLiving in AttachCapabilitiesEvent, saved with the
 * entity) -> NeoForge data attachment "ancientwarfare:respawn_data" (serializable, created on first access).
 * 1.12 entity.hasCapability(RESPAWN_DATA_CAPABILITY) / getCapability(..) -> get(entity).
 */
public class CapabilityRespawnData {
	private CapabilityRespawnData() {}

	private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, AncientWarfareStructure.MOD_ID);

	public static final Supplier<AttachmentType<RespawnData>> RESPAWN_DATA = ATTACHMENTS.register("respawn_data",
			() -> AttachmentType.serializable(RespawnData::new).build());

	public static void register(IEventBus modBus) {
		ATTACHMENTS.register(modBus);
	}

	/* the respawn data of an entity that can carry it (1.12: every EntityLiving) */
	public static Optional<IRespawnData> get(Entity entity) {
		return entity instanceof LivingEntity ? Optional.of(entity.getData(RESPAWN_DATA)) : Optional.empty();
	}

	/* true if the entity already has respawn data (does not create it) */
	public static boolean has(Entity entity) {
		return entity instanceof LivingEntity && entity.hasData(RESPAWN_DATA);
	}
}
