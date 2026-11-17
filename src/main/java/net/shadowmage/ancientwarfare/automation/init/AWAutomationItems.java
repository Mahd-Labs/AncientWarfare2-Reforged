package net.shadowmage.ancientwarfare.automation.init;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.automation.AncientWarfareAutomation;
import net.shadowmage.ancientwarfare.automation.item.ItemWorksiteUpgrade;
import net.shadowmage.ancientwarfare.core.upgrade.WorksiteUpgrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Port notes: the 1.12 meta item worksite_upgrade is flattened into worksite_upgrade_<variant>
 * (WORKSITE_UPGRADES, index = WorksiteUpgrade ordinal = 1.12 meta; WORKSITE_UPGRADE = meta 0).
 */
@SuppressWarnings("WeakerAccess")
public class AWAutomationItems {
	private AWAutomationItems() {}

	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AncientWarfareAutomation.MOD_ID);

	public static final List<DeferredItem<ItemWorksiteUpgrade>> WORKSITE_UPGRADES;
	public static final DeferredItem<ItemWorksiteUpgrade> WORKSITE_UPGRADE;

	static {
		List<DeferredItem<ItemWorksiteUpgrade>> upgrades = new ArrayList<>();
		for (WorksiteUpgrade upgrade : WorksiteUpgrade.values()) {
			upgrades.add(ITEMS.register("worksite_upgrade_" + ItemWorksiteUpgrade.SUB_NAMES[upgrade.ordinal()], () -> new ItemWorksiteUpgrade(upgrade)));
		}
		WORKSITE_UPGRADES = Collections.unmodifiableList(upgrades);
		WORKSITE_UPGRADE = WORKSITE_UPGRADES.get(0);
	}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
	}
}
