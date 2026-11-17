package net.shadowmage.ancientwarfare.npc.item;

/*
 * Port notes: the 1.12 nested client class MouseClickHandler (left click -> extended ray trace ->
 * PacketExtendedReachAttack) is client code and lives in npc.client.ExtendedReachHandler (installed by
 * AncientWarfareNPCClient); the server side (PacketExtendedReachAttack) is unchanged.
 */
public interface IExtendedReachWeapon {
	float getReach();
}
