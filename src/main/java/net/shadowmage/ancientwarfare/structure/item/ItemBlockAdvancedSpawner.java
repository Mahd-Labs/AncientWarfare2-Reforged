package net.shadowmage.ancientwarfare.structure.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.input.IItemKeyInterface;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.tile.SpawnerSettings;
import net.shadowmage.ancientwarfare.structure.tile.SpawnerSettings.EntitySpawnGroup;
import net.shadowmage.ancientwarfare.structure.tile.SpawnerSettings.EntitySpawnSettings;
import net.shadowmage.ancientwarfare.structure.tile.TileAdvancedSpawner;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBlockAdvancedSpawner extends ItemBlockBaseStructure implements IItemKeyInterface {
	private static final String SPAWNER_SETTINGS_TAG = "spawnerSettings";

	public ItemBlockAdvancedSpawner(Block block) {
		super(block);
	}

	@Override
	public boolean placeBlockAt(ItemStack stack, Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, BlockState newState) {
		//noinspection ConstantConditions
		if (!ItemTools.hasTag(stack) || !ItemTools.getTag(stack).contains(SPAWNER_SETTINGS_TAG)) {
			SpawnerSettings settings = SpawnerSettings.getDefaultSettings();
			CompoundTag defaultTag = new CompoundTag();
			settings.writeToNBT(defaultTag);
			ItemTools.setTagInfo(stack,SPAWNER_SETTINGS_TAG, defaultTag);
		}
		boolean val = super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
		if (!world.isClientSide && val) {
			WorldTools.getTile(world, pos, TileAdvancedSpawner.class).ifPresent(t -> {
				SpawnerSettings settings = new SpawnerSettings();
				settings.readFromNBT(ItemTools.getTag(stack).getCompound(SPAWNER_SETTINGS_TAG));
				t.setSettings(settings);
			});
		}
		return val;
	}

	@Override
	public boolean onKeyActionClient(Player player, ItemStack stack, ItemAltFunction altFunction) {
		return altFunction == ItemAltFunction.ALT_FUNCTION_1;
	}

	@Override
	public void onKeyAction(Player player, ItemStack stack, ItemAltFunction altFunction) {
		if (player.isShiftKeyDown()) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_SPAWNER_ADVANCED_INVENTORY, 0, 0, 0);
		} else {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_SPAWNER_ADVANCED, 0, 0, 0);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		//noinspection ConstantConditions
		if (!ItemTools.hasTag(stack) || !ItemTools.getTag(stack).contains(SPAWNER_SETTINGS_TAG)) {
			tooltip.add(Component.translatable("guistrings.corrupt_item"));
			return;
		}
		SpawnerSettings tooltipSettings = new SpawnerSettings();
		tooltipSettings.readFromNBT(ItemTools.getTag(stack).getCompound(SPAWNER_SETTINGS_TAG));
		List<EntitySpawnGroup> groups = tooltipSettings.getSpawnGroups();
		tooltip.add(Component.literal(net.shadowmage.ancientwarfare.structure.util.TextHelper.translate("guistrings.spawner.group_count") + ": " + groups.size()));
		EntitySpawnGroup group;
		for (int i = 0; i < groups.size(); i++) {
			group = groups.get(i);
			tooltip.add(Component.literal(net.shadowmage.ancientwarfare.structure.util.TextHelper.translate("guistrings.spawner.group_number") + ": " + (i + 1) + " " + net.shadowmage.ancientwarfare.structure.util.TextHelper.translate("guistrings.spawner.group_weight") + ": " + group.getWeight()));
			for (EntitySpawnSettings set : group.getEntitiesToSpawn()) {
				tooltip.add(Component.literal("  " + net.shadowmage.ancientwarfare.structure.util.TextHelper.translate("guistrings.spawner.entity_type") + ": " + net.shadowmage.ancientwarfare.structure.util.TextHelper.translate(set.getEntityName()) + " " + set.getSpawnMin() + " to " + set.getSpawnMax() + " (" + (set.getSpawnTotal() < 0 ? "infinite" : set.getSpawnTotal()) + " total)"));
			}
		}
	}
}
