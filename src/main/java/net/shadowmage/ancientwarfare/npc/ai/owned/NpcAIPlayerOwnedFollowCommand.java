package net.shadowmage.ancientwarfare.npc.ai.owned;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.shadowmage.ancientwarfare.npc.ai.NpcAI;
import net.shadowmage.ancientwarfare.npc.entity.NpcPlayerOwned;
import net.shadowmage.ancientwarfare.npc.npc_command.NpcCommand.Command;
import net.shadowmage.ancientwarfare.npc.npc_command.NpcCommand.CommandType;

public class NpcAIPlayerOwnedFollowCommand extends NpcAI<NpcPlayerOwned> {

	private BlockPos moveTargetPos = null;

	public NpcAIPlayerOwnedFollowCommand(NpcPlayerOwned npc) {
		super(npc);
		setMutexBits(ATTACK | MOVE);
	}

	@Override
	public boolean canUse() {
		if (!super.canUse()) {
			return false;
		}
		Command cmd = npc.getCurrentCommand();
		return cmd != Command.NONE && (!(cmd.type == CommandType.GUARD || cmd.type == CommandType.ATTACK_AREA) || npc.getTarget() == null);
	}

	@Override
	public void stop() {
		Command cmd = npc.getCurrentCommand();
		if (cmd != Command.NONE && (npc.getTarget() == null || !cmd.type.isPersistent())) {
			npc.handlePlayerCommand(Command.NONE);
		}
	}

	@Override
	public void start() {
		//noop
	}

	@Override
	public void updateTask() {
		Command cmd = npc.getCurrentCommand();
		handleCommand(cmd);
		if (!cmd.type.isPersistent()) {
			npc.setPlayerCommand(Command.NONE);
		}
	}

	@SuppressWarnings("squid:S1199")
	private void handleCommand(Command cmd) {
		switch (cmd.type)//handle instant type commands
		{
			case CLEAR_HOME: {
				npc.clearRestriction();
				break;
			}
			case CLEAR_UPKEEP: {
				npc.setUpkeepAutoPosition(null);
				break;
			}
			case SET_HOME: {
				npc.restrictTo(cmd.pos, npc.getHomeRange());
				break;
			}
			case SET_UPKEEP: {
				npc.setUpkeepAutoPosition(cmd.pos);
				break;
			}
			case CLEAR_COMMAND:
			case ATTACK: {
				//should already be handled by npc 'handle command' functionality when command first received
				npc.setPlayerCommand(Command.NONE);
				break;
			}
			case ATTACK_AREA: {
				//TODO this likely needs an implementaion that will prioritize attacking targets over moving
				handleMoveCommand(cmd);
				break;
			}
			case GUARD: {
				handleGuardCommand(cmd);
				break;
			}
			case MOVE: {
				handleMoveCommand(cmd);
				break;
			}
			default:
		}
	}

	/*
	 * move towards the commanded guard-entity.
	 */
	private void handleGuardCommand(Command cmd) {
		Entity e = cmd.getEntityTarget(npc.level());
		if (e == null) {
			npc.setPlayerCommand(Command.NONE);//clear the command if the target entity cannot be found
			return;
		}
		double sqDist = npc.distanceToSqr(e);
		if (sqDist > MIN_RANGE) {
			moveToEntity(e, sqDist);//move to entity...
		} else {
			npc.getNavigation().stop();//clear path to stop moving
			if (e instanceof Horse) {
				if (!npc.isPassenger() && e.getPassengers().isEmpty()) {
					npc.startRiding(e);
					e.setYRot(npc.getYRot() % 360F);
		e.yRotO = e.getYRot();
					npc.setPlayerCommand(Command.NONE);//clear command if horse was mounted successfully..
				} else if (npc.isPassenger() && npc.getVehicle() == e) {
					npc.stopRiding();
					npc.setPlayerCommand(Command.NONE);
				}
			}
			//do not clear command, guard command is persistent
		}
	}

	private void handleMoveCommand(Command cmd) {
		if (moveTargetPos == null || moveTargetPos != cmd.pos) {
			moveTargetPos = cmd.pos;
		}
		double sqDist = npc.distanceToSqr(moveTargetPos);
		if (sqDist > MIN_RANGE) {
			moveToPosition(moveTargetPos, sqDist);//not finished moving...move along path (or at least try)
		} else {
			npc.setPlayerCommand(Command.NONE);//finished moving..clear the command...
		}
	}
}
