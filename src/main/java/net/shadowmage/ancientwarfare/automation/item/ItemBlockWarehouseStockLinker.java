package net.shadowmage.ancientwarfare.automation.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.shadowmage.ancientwarfare.automation.block.BlockWarehouseStockLinker;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;
import net.shadowmage.ancientwarfare.core.item.ItemBlockOwnedRotatable;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.core.util.RayTraceUtils;

import java.util.List;

/*
 * Port notes: the linked position is stored in the stack's custom data under WAREHOUSE_POS_TAG as a compound {X,Y,Z}
 * (the 1.12 NBTUtil.createPosTag layout); readPos / writePos replace NBTUtil.
 */
public class ItemBlockWarehouseStockLinker extends ItemBlockOwnedRotatable {
	public static final String WAREHOUSE_POS_TAG = "warehousePosTag";

	public ItemBlockWarehouseStockLinker(BlockWarehouseStockLinker block) {
		super(block);
	}

	public static BlockPos readPos(CompoundTag tag) {
		return new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
	}

	public static CompoundTag writePos(BlockPos pos) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("X", pos.getX());
		tag.putInt("Y", pos.getY());
		tag.putInt("Z", pos.getZ());
		return tag;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		if (ItemTools.hasTag(stack)) {
			tooltip.add(Component.translatable("tile.warehouse_stock_linker.tooltip", formatPos(readPos(ItemTools.getTag(stack).getCompound(WAREHOUSE_POS_TAG)))));
		}
	}

	private void addMessage(Player player, BlockPos pos) {
		player.sendSystemMessage(Component.translatable("guistrings.automation.warehouse_set", formatPos(pos)));
	}

	private String formatPos(BlockPos pos) {
		return String.format("x: %d y: %d z: %d", pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Level world = context.getLevel();
		if (player != null) {
			if (player.isShiftKeyDown() && world.isClientSide) {
				return InteractionResult.PASS;
			}
			if (player.isShiftKeyDown() && !world.isClientSide) {
				HitResult hit = RayTraceUtils.getPlayerTarget(player, 5, 0);
				if (hit != null && hit.getType() == HitResult.Type.BLOCK && world.getBlockState(((BlockHitResult) hit).getBlockPos()).getBlock() == AWAutomationBlocks.WAREHOUSE_CONTROL.get()) {
					ItemStack stack = context.getItemInHand();
					BlockPos hitPos = ((BlockHitResult) hit).getBlockPos();
					ItemTools.setTagInfo(stack, WAREHOUSE_POS_TAG, writePos(hitPos));
					addMessage(player, hitPos);
					return InteractionResult.SUCCESS;
				}
			}
		}
		return super.useOn(context);
	}
}
