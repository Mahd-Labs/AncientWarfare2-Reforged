package net.shadowmage.ancientwarfare.npc.client;

import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.shadowmage.ancientwarfare.npc.init.AWNPCItems;
import net.shadowmage.ancientwarfare.npc.item.ItemCoin;
import net.shadowmage.ancientwarfare.npc.item.ItemNpcSpawner;
import net.shadowmage.ancientwarfare.npc.registry.FactionRegistry;

/*
 * Port notes: ItemColors.registerItemColorHandler -> RegisterColorHandlersEvent.Item. 1.21.1 item colours are ARGB
 * (the 1.12 RGB values get a full alpha byte; -1 is still "no tint").
 */
public class NPCItemColors {
	private NPCItemColors() {}

	public static final int FACTION_TOP_COLOR = 0xEF5757;

	public static void init(RegisterColorHandlersEvent.Item event) {
		event.register(((stack, tintIndex) -> {
			if (tintIndex == 1 || tintIndex == 2) {
				String factionName = ItemNpcSpawner.getFaction(stack).orElse("");
				if (tintIndex == 2) {
					return FactionRegistry.getFactionNames().contains(factionName) ? opaque(FACTION_TOP_COLOR) : -1;
				} else {
					return FactionRegistry.getFactionNames().contains(factionName) ? opaque(FactionRegistry.getFaction(factionName).getColor()) : -1;
				}
			}
			return -1;
		}), AWNPCItems.NPC_SPAWNER.get());
		event.register(((stack, tintindex) -> {
			ItemCoin.CoinMetal metal = ItemCoin.getMetal(stack);
			return metal == null ? -1 : opaque(metal.getColor());
		}), AWNPCItems.COIN.get());
	}

	private static int opaque(int rgb) {
		return rgb == -1 ? -1 : 0xFF000000 | rgb;
	}
}
