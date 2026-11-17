package net.shadowmage.ancientwarfare.npc.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.shadowmage.ancientwarfare.core.block.BlockBase;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;

/*
 * Port notes: 1.12 registerClient (state mapper to blockstates/npc/<name>.json) is gone, the blockstate json lives at
 * blockstates/<name>.json. The creative tab content is listed by AWNPCTab.
 */
public class BlockBaseNPC extends BlockBase {
	public BlockBaseNPC(BlockBehaviour.Properties properties, String regName) {
		super(properties, AncientWarfareNPC.MOD_ID, regName);
	}
}
