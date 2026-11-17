package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

/*
 * Port notes: 1.12 registerClient (state mapper to blockstates/core/<name>.json) is gone, the blockstate json lives at
 * blockstates/<name>.json. The creative tab content is listed by AWCoreTab.
 */
public abstract class BlockBaseCore extends BlockBase implements BlockRotationHandler.IRotatableBlock {

	public BlockBaseCore(BlockBehaviour.Properties properties, String regName) {
		super(properties, AncientWarfareCore.MOD_ID, regName);
	}

	@Override
	public BlockRotationHandler.RotationType getRotationType() {
		return BlockRotationHandler.RotationType.FOUR_WAY;
	}

	@Override
	public boolean invertFacing() {
		return true;
	}
}
