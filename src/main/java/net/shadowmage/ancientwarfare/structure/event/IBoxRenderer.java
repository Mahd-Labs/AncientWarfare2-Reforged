package net.shadowmage.ancientwarfare.structure.event;

/*
 * Port notes: in 1.12 the items implemented renderBox(player, hand, stack, delta) themselves with direct GL calls
 * (the interface also carried the Util drawing helpers). Client drawing can not live in common item classes any more:
 * this stays as the marker the client side box renderer looks for, the drawing itself is client code
 * (see AncientWarfareStructureClient).
 */
public interface IBoxRenderer {
}
