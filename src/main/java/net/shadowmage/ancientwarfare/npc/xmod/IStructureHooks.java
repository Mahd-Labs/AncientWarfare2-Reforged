package net.shadowmage.ancientwarfare.npc.xmod;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Optional;

/*
 * Port notes: everything the NPC module asks of the (unported) structure module. NONE behaves like "structure module
 * absent". Every method names the 1.12 code it stands for.
 */
public interface IStructureHooks {
	IStructureHooks NONE = new IStructureHooks() {};

	/*
	 * NpcFaction.despawnEntity: if the entity has CapabilityRespawnData.RESPAWN_DATA_CAPABILITY ->
	 * SpawnerHelper.createSpawner(respawnData, world)
	 */
	default void createRespawnSpawner(Entity entity, Level level) {
	}

	/*
	 * EventHandler.preventHostileSpawnsInStructures:
	 * AWGameData.getPerWorldData(world, StructureMap.class).shouldPreventSpawnAtPos(world, pos)
	 *  || AWGameData.getPerWorldData(world, TownMap.class).shouldPreventSpawnAtPos(world, pos)
	 */
	default boolean shouldPreventSpawnAtPos(Level level, BlockPos pos) {
		return false;
	}

	/*
	 * EventHandler.onChestClicked / playerDigSpeed:
	 * AWGameData.getPerWorldData(world, StructureMap.class).getStructureAt(world, pos)
	 */
	default Optional<IStructureEntry> getStructureAt(Level level, BlockPos pos) {
		return Optional.empty();
	}

	/* EventHandler.isLockedContainer: te instanceof ISpecialLootContainer */
	default boolean isSpecialLootContainer(BlockEntity te) {
		return false;
	}

	/* EventHandler.shouldBlockSlowDownDigging: block == AWStructureBlocks.ADVANCED_SPAWNER */
	default boolean isAdvancedSpawner(Block block) {
		return false;
	}

	/*
	 * ItemCoin.METALS_TO_BLOCKS: AWStructureBlocks.COIN_STACK_ANCIENT / _GOLD / _SILVER / _COPPER
	 * (metal = "ancient", "gold", "silver", "copper"); null = block not available
	 */
	@Nullable
	default Block getCoinStackBlock(String metal) {
		return null;
	}

	/* ItemCoin.onItemUse: AWStructureSounds.COIN_STACK_INTERACT */
	@Nullable
	default SoundEvent getCoinStackInteractSound() {
		return null;
	}

	/*
	 * AWNPCItems.addFactionBlocks: AWStructureBlocks.PROTECTION_FLAG.addFlagDefinition(new BlockFlag.FlagDefinition(name))
	 * and AWStructureBlocks.DECORATIVE_FLAG.addFlagDefinition(...)
	 */
	default void addFlagDefinition(String factionName) {
	}

	/*
	 * AWNPCItems.addFactionBlocks: if Item.getItemFromBlock(block) instanceof ItemBlockColored ->
	 * ((ItemBlockColored) itemBlock).addCustomItemTag(tag)   (tag already has "unlocalizedNamePart" = "faction")
	 */
	default void addThemedBlockItemTag(Block block, CompoundTag tag) {
	}

	/* StructureEntry as far as EventHandler needs it */
	interface IStructureEntry {
		/* StructureEntry.getBB().getAABB() */
		AABB getAABB();

		/*
		 * WorldTools.getTile(world, structure.getProtectionFlagPos(), TileProtectionFlag.class) is present
		 */
		boolean hasProtectionFlag(Level level);

		/*
		 * WorldTools.getTile(world, structure.getProtectionFlagPos(), TileProtectionFlag.class)
		 *   .map(t -> t.shouldProtectAgainst(player)) ; false when there is no flag tile
		 */
		boolean flagProtectsAgainst(Level level, Player player);
	}
}
