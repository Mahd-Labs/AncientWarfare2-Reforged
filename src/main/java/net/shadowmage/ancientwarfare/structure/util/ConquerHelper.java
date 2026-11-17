package net.shadowmage.ancientwarfare.structure.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.TextUtils;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFaction;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.structure.network.PacketHighlightBlock;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.tile.SpawnerSettings;
import net.shadowmage.ancientwarfare.structure.tile.TileAdvancedSpawner;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ConquerHelper {
	private static final Cache<StructureBB, Boolean> STRUCTURE_BB_CONQUERED = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

	private ConquerHelper() {}

	private static boolean checkBBConquered(Level world, StructureBB bb) {
		return checkBBConquered(world, bb, npc -> {}, pos -> {});
	}

	public static boolean checkBBConquered(Player player, StructureBB bb) {
		return checkBBConquered(player.level(), bb, npc -> markNpcAndMessagePlayer(player, npc), pos -> markSpawnerAndMessagePlayer(player, pos));
	}

	private static void markSpawnerAndMessagePlayer(Player player, BlockPos pos) {
		NetworkHandler.sendToPlayer((ServerPlayer) player, new PacketHighlightBlock(new BlockHighlightInfo(pos, player.level().getGameTime() + 200)));
		player.displayClientMessage(Component.translatable("gui.ancientwarfarestructure.structure_spawner_present"), true);
	}

	private static void markNpcAndMessagePlayer(Player player, NpcFaction npc) {
		npc.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200));
		player.displayClientMessage(Component.translatable("gui.ancientwarfarestructure.structure_hostile_alive",
				TextUtils.getSimpleBlockPosString(npc.blockPosition())), true);
	}

	private static boolean checkBBConquered(Level world, StructureBB bb, Consumer<NpcFaction> onHostileNpcFound, Consumer<BlockPos> onHostileSpawnerFound) {
		AABB boundingBox = bb.getAABB();
		for (NpcFaction factionNpc : world.getEntitiesOfClass(NpcFaction.class, boundingBox)) {
			if (!factionNpc.isPassive()) {
				onHostileNpcFound.accept(factionNpc);
				return false;
			}
		}

		for (BlockPos blockPos : BlockPos.betweenClosed(bb.min, bb.max)) {
			if (!world.isLoaded(blockPos)) {
				return false;
			}
			if (world.getBlockState(blockPos).getBlock() == AWStructureBlocks.ADVANCED_SPAWNER.get() &&
					WorldTools.getTile(world, blockPos, TileAdvancedSpawner.class).map(te -> SpawnerSettings.spawnsHostileNpcs(te.getSettings())).orElse(false)) {
				onHostileSpawnerFound.accept(blockPos);
				return false;
			}
		}
		return true;
	}

	public static boolean checkBBNotConquered(Level world, StructureBB bb) {
		try {
			return !STRUCTURE_BB_CONQUERED.get(bb, () -> checkBBConquered(world, bb));
		}
		catch (ExecutionException e) {
			AncientWarfareNPC.LOG.error("Error getting conquered structureBB info ", e);
			return false;
		}
	}
}
