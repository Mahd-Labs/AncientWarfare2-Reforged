package net.shadowmage.ancientwarfare.automation.block;

/*
 * Port note: the 1.12 unlisted CONNECTIONS properties are gone, renderers ask TileTorqueSidedCell.getConnections().
 */
public abstract class BlockTorqueTransportSided extends BlockTorqueTransport {

	protected BlockTorqueTransportSided(String regName) {
		super(regName);
	}
}
