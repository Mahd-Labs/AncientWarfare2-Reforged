package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.shadowmage.ancientwarfare.core.block.BlockBase;

/*
 * Port notes: stand-in for the 1.12 net.minecraft.block.material.Material constants the structure blocks were built
 * with. props() gives the 1.21 block properties that correspond to (material + the BlockBase default setHardness(2)):
 * map colour, note block instrument, "needs the right tool to drop" (ROCK / IRON) and lava ignition (WOOD).
 * The 1.12 step sound did not depend on the material (default stone unless setSoundType was called), so no sound here.
 * The mineable/<tool> tags are generated from the same table (tools/structure_gen_resources.py).
 */
public enum Material {
	WOOD, ROCK, IRON, CLAY, GRASS, GROUND;

	public BlockBehaviour.Properties props() {
		BlockBehaviour.Properties p = BlockBase.baseProperties();
		switch (this) {
			case WOOD:
				return p.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).ignitedByLava();
			case ROCK:
				return p.mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops();
			case IRON:
				return p.mapColor(MapColor.METAL).requiresCorrectToolForDrops();
			case CLAY:
				return p.mapColor(MapColor.CLAY);
			case GRASS:
				return p.mapColor(MapColor.GRASS);
			default:
				return p.mapColor(MapColor.DIRT);
		}
	}
}
