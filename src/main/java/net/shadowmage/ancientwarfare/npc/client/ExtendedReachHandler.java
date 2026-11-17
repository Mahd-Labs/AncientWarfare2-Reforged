package net.shadowmage.ancientwarfare.npc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.npc.item.IExtendedReachWeapon;
import net.shadowmage.ancientwarfare.npc.network.PacketExtendedReachAttack;

import java.util.Optional;

/*
 * Port notes: 1.12 IExtendedReachWeapon.MouseClickHandler (MouseEvent, left button pressed) ->
 * InputEvent.InteractionKeyMappingTriggered for the attack key. getMouseOverExtended was a copy of
 * EntityRenderer.getMouseOver with the weapon's reach; the 1.21.1 equivalent is a block clip up to the reach followed
 * by ProjectileUtil.getEntityHitResult along the same ray. As in 1.12 the packet is sent in addition to the normal
 * attack handling (the server validates and attacks).
 */
public class ExtendedReachHandler {
	@SubscribeEvent
	public void onMouseClick(InputEvent.InteractionKeyMappingTriggered event) {
		if (!event.isAttack()) {
			return;
		}
		Player player = Minecraft.getInstance().player;
		if (player == null || !(player.getMainHandItem().getItem() instanceof IExtendedReachWeapon)) {
			return;
		}
		IExtendedReachWeapon weapon = (IExtendedReachWeapon) player.getMainHandItem().getItem();
		Optional<Entity> result = getMouseOverExtended(weapon.getReach());
		result.ifPresent(r -> {
			if (r.invulnerableTime == 0 && r != player) {
				NetworkHandler.sendToServer(new PacketExtendedReachAttack(r.getId()));
			}
		});
	}

	private Optional<Entity> getMouseOverExtended(float reach) {
		Minecraft mc = Minecraft.getInstance();
		Entity renderViewEntity = mc.getCameraEntity();
		if (renderViewEntity == null || mc.level == null) {
			return Optional.empty();
		}
		double d0 = reach;
		Vec3 positionEyes = renderViewEntity.getEyePosition(0);
		Vec3 look = renderViewEntity.getViewVector(1.0F);
		Vec3 end = positionEyes.add(look.x * d0, look.y * d0, look.z * d0);
		HitResult blockHit = mc.level.clip(new ClipContext(positionEyes, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, renderViewEntity));
		double calcDist = d0 * d0;
		if (blockHit.getType() != HitResult.Type.MISS) {
			calcDist = blockHit.getLocation().distanceToSqr(positionEyes);
		}
		AABB box = renderViewEntity.getBoundingBox().expandTowards(look.scale(d0)).inflate(1.0D, 1.0D, 1.0D);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(renderViewEntity, positionEyes, end, box, e -> !e.isSpectator() && e.isPickable(), calcDist);
		return entityHit == null ? Optional.empty() : Optional.of(entityHit.getEntity());
	}
}
