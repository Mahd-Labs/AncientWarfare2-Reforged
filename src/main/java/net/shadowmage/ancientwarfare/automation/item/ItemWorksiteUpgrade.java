package net.shadowmage.ancientwarfare.automation.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.shadowmage.ancientwarfare.automation.AncientWarfareAutomation;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationItems;
import net.shadowmage.ancientwarfare.core.interfaces.IWorkSite;
import net.shadowmage.ancientwarfare.core.item.ItemMulti;
import net.shadowmage.ancientwarfare.core.upgrade.WorksiteUpgrade;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import javax.annotation.Nullable;
import java.util.HashSet;

/*
 * Port notes: flattened (see ItemMulti): one item per upgrade, registered as worksite_upgrade_<1.12 variant name>
 * (AWAutomationItems.WORKSITE_UPGRADES, index = WorksiteUpgrade ordinal = 1.12 meta).
 */
public class ItemWorksiteUpgrade extends ItemMulti {
	/* 1.12 model variant names, index = WorksiteUpgrade ordinal */
	public static final String[] SUB_NAMES = {"bounds_medium", "bounds_large", "quarry_medium", "quarry_large", "enchanted_tools_1", "enchanted_tools_2", "quality_tools_1", "quality_tools_2", "quality_tools_3", "chunkloader_basic", "chunkloader_quarry"};

	private final WorksiteUpgrade upgrade;

	public ItemWorksiteUpgrade(WorksiteUpgrade upgrade) {
		super(AncientWarfareAutomation.MOD_ID, "worksite_upgrade", upgrade.ordinal());
		this.upgrade = upgrade;
	}

	private static WorksiteUpgrade getUpgrade(ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof ItemWorksiteUpgrade)) {
			throw new RuntimeException("Cannot retrieve worksite upgrade type for: " + stack + ".  Null stack, or item, or mismatched item!");
		}
		return ((ItemWorksiteUpgrade) stack.getItem()).upgrade;
	}

	public static ItemStack getStack(@Nullable WorksiteUpgrade upgrade) {
		return upgrade == null ? ItemStack.EMPTY : new ItemStack(AWAutomationItems.WORKSITE_UPGRADES.get(upgrade.ordinal()).get());
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (world.isClientSide) {
			return InteractionResultHolder.success(stack);
		}
		BlockPos pos = BlockTools.getBlockClickedOn(player, world, false);
		if (pos != null) {
			BlockEntity te = world.getBlockEntity(pos);
			if (te instanceof IWorkSite) {
				IWorkSite ws = (IWorkSite) te;
				WorksiteUpgrade upgrade = getUpgrade(stack);
				if (!ws.getValidUpgrades().contains(upgrade)) {
					return InteractionResultHolder.fail(stack);
				}
				HashSet<WorksiteUpgrade> wsug = new HashSet<>(ws.getUpgrades());
				if (wsug.contains(upgrade)) {
					return InteractionResultHolder.fail(stack);
				}
				for (WorksiteUpgrade ug : wsug) {
					if (ug.exclusive(upgrade)) {
						return InteractionResultHolder.fail(stack);//exclusive upgrade present, exit early
					}
				}
				for (WorksiteUpgrade ug : wsug) {
					if (upgrade.overrides(ug)) {
						InventoryTools.dropItemInWorld(player.level(), getStack(ug), te.getBlockPos());
						ws.removeUpgrade(ug);
					}
				}
				ws.addUpgrade(upgrade);
				stack.shrink(1);
			}
		}
		return InteractionResultHolder.success(stack);
	}

}
