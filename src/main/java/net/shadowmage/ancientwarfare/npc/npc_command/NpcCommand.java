package net.shadowmage.ancientwarfare.npc.npc_command;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.EntityTools;
import net.shadowmage.ancientwarfare.npc.entity.NpcPlayerOwned;
import net.shadowmage.ancientwarfare.npc.item.ItemCommandBaton;
import net.shadowmage.ancientwarfare.npc.network.PacketNpcCommand;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class NpcCommand {
	//TODO refactor to proper individual instances of commands that will not only hold the type, but their logic as well (move that from NPCAIPlayerOwnedFollowCommand)

	public enum CommandType {
		MOVE, ATTACK, //attack click on entity
		ATTACK_AREA, //attack click on block
		GUARD, //attack click on friendly player or npc
		SET_HOME, SET_UPKEEP, CLEAR_HOME, CLEAR_UPKEEP, CLEAR_COMMAND, NONE;

		public boolean isPersistent() {
			return (this == ATTACK || this == GUARD || this == ATTACK_AREA);
		}
	}

	/*
	 * client-side handle command. called from command baton key handler
	 */
	public static void handleCommandClient(CommandType type, HitResult hit) {
		if (hit != null && hit.getType() != HitResult.Type.MISS) {
			if (hit.getType() == HitResult.Type.ENTITY && ((EntityHitResult) hit).getEntity() != null) {
				NetworkHandler.sendToServer(new PacketNpcCommand(type, ((EntityHitResult) hit).getEntity()));
			} else if (hit.getType() == HitResult.Type.BLOCK) {
				NetworkHandler.sendToServer(new PacketNpcCommand(type, ((BlockHitResult) hit).getBlockPos()));
			}
		}
	}

	/*
	 * server side handle command. called from packet triggered from client key input while baton is equipped
	 */
	public static void handleServerCommand(Player player, CommandType type, boolean block, BlockPos pos, int entityID) {
		Command cmd;
		if (block) {
			cmd = new Command(type, pos);
		} else {
			cmd = new Command(type, entityID);
		}
		List<Entity> targets = ItemCommandBaton.getCommandedEntities(player.level(), EntityTools.getItemFromEitherHand(player, ItemCommandBaton.class));
		for (Entity e : targets) {
			if (e instanceof NpcPlayerOwned) {
				((NpcPlayerOwned) e).handlePlayerCommand(cmd);
			}
		}
	}

	public static class Command {
		public static final Command NONE = new Command(CommandType.NONE, new BlockPos(0, 0, 0));
		private static final String IDMSB_TAG = "idmsb";
		private static final String IDLSB_TAG = "idlsb";

		public CommandType type;
		public BlockPos pos = BlockPos.ZERO;
		private boolean blockTarget;
		private UUID entityUUID;
		private int entityID;
		private Entity entity;

		public Command() {
		}

		public Command(CompoundTag tag) {
			readFromNBT(tag);
		}

		public Command(CommandType type, BlockPos pos) {
			this.type = type;
			this.pos = pos;
			blockTarget = true;
		}

		public Command(CommandType type, int entityID) {
			this.type = type;
			this.entityID = entityID;
			blockTarget = false;
		}

		public Command copy() {
			Command cmd = new Command();
			cmd.type = type;
			cmd.pos = pos;
			cmd.entity = entity;
			cmd.entityID = entityID;
			cmd.entityUUID = entityUUID;
			cmd.blockTarget = blockTarget;
			return cmd;
		}

		public void readFromNBT(CompoundTag tag) {
			type = CommandType.values()[tag.getInt("type")];
			blockTarget = tag.getBoolean("block");
			pos = BlockPos.of(tag.getLong("pos"));
			if (tag.contains(IDMSB_TAG) && tag.contains(IDLSB_TAG)) {
				entityUUID = new UUID(tag.getLong(IDMSB_TAG), tag.getLong(IDLSB_TAG));
			}
			entityID = tag.getInt("entityid");
		}

		public CompoundTag writeToNBT(CompoundTag tag) {
			tag.putInt("type", type.ordinal());
			tag.putBoolean("block", blockTarget);
			tag.putLong("pos", pos.asLong());
			if (entityUUID != null) {
				tag.putLong(IDMSB_TAG, entityUUID.getMostSignificantBits());
				tag.putLong(IDLSB_TAG, entityUUID.getLeastSignificantBits());
			}
			tag.putInt("entityid", entityID);
			return tag;
		}

		/*
		 * should be called by packet prior to passing command into npc processing
		 */
		private void findEntity(Level world) {
			if (blockTarget) {
				return;
			}
			if (entity != null) {
				return;
			}
			if (entityUUID == null) {
				entity = world.getEntity(entityID);
				if (entity != null) {
					entityUUID = entity.getUUID();
				}
			} else {
				entity = world.getPlayerByUUID(entityUUID);
			}
		}

		public Entity getEntityTarget(Level world) {
			if (blockTarget) {
				return null;
			}
			if (entity != null) {
				return entity;
			} else {
				findEntity(world);
			}
			return entity;
		}
	}
}
