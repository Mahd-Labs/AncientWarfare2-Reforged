package net.shadowmage.ancientwarfare.core.container;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/*
 * Created by Olivier on 05/02/2015.
 */
public class ContainerEntityBase<T extends Entity> extends ContainerBase {

	public final T entity;

	@SuppressWarnings("unchecked")
	public ContainerEntityBase(Player player, int id) {
		super(player);
		entity = (T) player.level().getEntity(id);
		if (entity == null) {
			throw new IllegalArgumentException("Id wasn't a valid entity");
		}
	}

	@Override
	public boolean canInteractWith(Player var1) {
		return entity.isAlive();
	}

	public T getEntity() {
		return entity;
	}
}
