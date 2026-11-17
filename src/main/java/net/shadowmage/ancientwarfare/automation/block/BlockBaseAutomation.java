package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.shadowmage.ancientwarfare.automation.AncientWarfareAutomation;
import net.shadowmage.ancientwarfare.core.block.BlockBase;

/*
 * Port notes: 1.12 (Material, regName) -> (Properties, regName); use rock() / wood() for the two 1.12 materials.
 * registerClient (state mapper to blockstates/automation/<name>.json) is gone: blockstates/<name>.json.
 * The creative tab content is listed by AWAutomationTab.
 * 1.12 eventReceived forwarding (world.addBlockEvent -> tile.receiveClientEvent) is done here for every automation
 * block: level.blockEvent(pos, block, id, param) -> BlockEntity.triggerEvent(id, param).
 */
public abstract class BlockBaseAutomation extends BlockBase {

	public BlockBaseAutomation(BlockBehaviour.Properties properties, String regName) {
		super(properties, AncientWarfareAutomation.MOD_ID, regName);
	}

	/*
	 * 1.12 Material.ROCK + setHardness(2): needs a pickaxe to drop
	 */
	public static BlockBehaviour.Properties rock() {
		return baseProperties().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops();
	}

	/*
	 * 1.12 Material.WOOD + setHardness(2)
	 */
	public static BlockBehaviour.Properties wood() {
		return baseProperties().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).ignitedByLava();
	}

	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		super.triggerEvent(state, level, pos, id, param);
		BlockEntity tile = level.getBlockEntity(pos);
		return tile != null && tile.triggerEvent(id, param);
	}
}
