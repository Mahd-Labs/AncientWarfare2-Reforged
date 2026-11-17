package net.shadowmage.ancientwarfare.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.npc.xmod.IStructureHooks;
import net.shadowmage.ancientwarfare.npc.xmod.NpcXmod;
import net.shadowmage.ancientwarfare.structure.block.BlockFlag;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureEntry;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureMap;
import net.shadowmage.ancientwarfare.structure.gamedata.TownMap;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.structure.init.AWStructureSounds;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockColored;
import net.shadowmage.ancientwarfare.structure.tile.ISpecialLootContainer;
import net.shadowmage.ancientwarfare.structure.tile.TileProtectionFlag;
import net.shadowmage.ancientwarfare.structure.util.CapabilityRespawnData;
import net.shadowmage.ancientwarfare.structure.util.SpawnerHelper;

import javax.annotation.Nullable;
import java.util.Optional;

/*
 * Port notes: the structure side of the cross module hooks (PORTING.md "Priorities"): what the NPC module asks of the
 * structure module (npc.xmod.IStructureHooks; every method is the 1.12 code named in the interface javadoc) and the
 * two hooks core left for this module (BlockTools skippable blocks, CommandUtils block export). Installed from
 * AncientWarfareStructure.init.
 */
public final class StructureHooks implements IStructureHooks {
	private StructureHooks() {}

	static void install() {
		NpcXmod.setStructureHooks(new StructureHooks());
		StructureCoreHooks.install();
	}

	@Override
	public void createRespawnSpawner(Entity entity, Level level) {
		if (CapabilityRespawnData.has(entity)) {
			CapabilityRespawnData.get(entity).ifPresent(respawnData -> SpawnerHelper.createSpawner(respawnData, level));
		}
	}

	@Override
	public boolean shouldPreventSpawnAtPos(Level level, BlockPos pos) {
		if (level.isClientSide) {
			return false;
		}
		return StructureMap.get(level).shouldPreventSpawnAtPos(level, pos)
				|| AWGameData.INSTANCE.getPerWorldData(level, TownMap.class).shouldPreventSpawnAtPos(level, pos);
	}

	@Override
	public Optional<IStructureEntry> getStructureAt(Level level, BlockPos pos) {
		return StructureMap.get(level).getStructureAt(level, pos).map(Entry::new);
	}

	@Override
	public boolean isSpecialLootContainer(BlockEntity te) {
		return te instanceof ISpecialLootContainer;
	}

	@Override
	public boolean isAdvancedSpawner(Block block) {
		return block == AWStructureBlocks.ADVANCED_SPAWNER.get();
	}

	@Nullable
	@Override
	public Block getCoinStackBlock(String metal) {
		switch (metal) {
			case "ancient":
				return AWStructureBlocks.COIN_STACK_ANCIENT.get();
			case "gold":
				return AWStructureBlocks.COIN_STACK_GOLD.get();
			case "silver":
				return AWStructureBlocks.COIN_STACK_SILVER.get();
			case "copper":
				return AWStructureBlocks.COIN_STACK_COPPER.get();
			default:
				return null;
		}
	}

	@Nullable
	@Override
	public SoundEvent getCoinStackInteractSound() {
		return AWStructureSounds.COIN_STACK_INTERACT.get();
	}

	@Override
	public void addFlagDefinition(String factionName) {
		AWStructureBlocks.PROTECTION_FLAG.get().addFlagDefinition(new BlockFlag.FlagDefinition(factionName));
		AWStructureBlocks.DECORATIVE_FLAG.get().addFlagDefinition(new BlockFlag.FlagDefinition(factionName));
	}

	@Override
	public void addThemedBlockItemTag(Block block, CompoundTag tag) {
		if (block.asItem() instanceof ItemBlockColored) {
			((ItemBlockColored) block.asItem()).addCustomItemTag(tag);
		}
	}

	private static final class Entry implements IStructureEntry {
		private final StructureEntry structure;

		private Entry(StructureEntry structure) {
			this.structure = structure;
		}

		@Override
		public AABB getAABB() {
			return structure.getBB().getAABB();
		}

		@Override
		public boolean hasProtectionFlag(Level level) {
			return WorldTools.getTile(level, structure.getProtectionFlagPos(), TileProtectionFlag.class).isPresent();
		}

		@Override
		public boolean flagProtectsAgainst(Level level, Player player) {
			return WorldTools.getTile(level, structure.getProtectionFlagPos(), TileProtectionFlag.class).map(t -> t.shouldProtectAgainst(player)).orElse(false);
		}
	}
}
