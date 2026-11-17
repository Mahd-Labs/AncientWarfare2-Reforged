package net.shadowmage.ancientwarfare.structure.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.shadowmage.ancientwarfare.core.input.IItemKeyInterface;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.town.TownBoundingArea;
import net.shadowmage.ancientwarfare.structure.town.TownTemplate;
import net.shadowmage.ancientwarfare.structure.town.TownTemplateManager;
import net.shadowmage.ancientwarfare.structure.town.WorldTownGenerator;

import java.util.Optional;

/*
 * Port notes: the gui registration of registerClient lives in AncientWarfareStructureClient.registerGuis.
 * 1.12 Item.rayTrace(world, player, false) -> Item.getPlayerPOVHitResult (no fluids).
 */
public class ItemTownBuilder extends ItemBaseStructure implements IItemKeyInterface {

	public ItemTownBuilder(String name) {
		super(name, new Item.Properties().stacksTo(1));
		//TODO make texture (uses structure builder one currently)
	}

	@Override
	public boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.level.LevelReader world, BlockPos pos, Player player) {
		return false;
	}

	@Override
	public boolean onKeyActionClient(Player player, ItemStack stack, ItemAltFunction altFunction) {
		return altFunction == ItemAltFunction.ALT_FUNCTION_1;
	}

	@Override
	public void onKeyAction(Player player, ItemStack stack, ItemAltFunction altFunction) {
		if (player.level().isClientSide) {
			return;
		}

		BlockHitResult rayTraceResult = getPlayerPOVHitResult(player.level(), player, ClipContext.Fluid.NONE);
		if (rayTraceResult.getType() != HitResult.Type.BLOCK) {
			return;
		}

		Optional<TownTemplate> template = TownTemplateManager.INSTANCE.getTemplate(getTownName(stack));
		if (!template.isPresent()) {
			player.sendSystemMessage(Component.literal("No town template set"));
			return;
		}

		long t1 = System.nanoTime();
		WorldTownGenerator.INSTANCE.generate(player.level(), getTownArea(player.level(), rayTraceResult.getBlockPos(), player.getDirection(), getLength(stack), getWidth(stack)), template.get());
		long t2 = System.nanoTime();
		AncientWarfareStructure.LOG.debug("Total Town gen nanos (incl. validation): {}", t2 - t1);
	}

	/*
	 * public static for the GameTests (1.12: private)
	 */
	public static TownBoundingArea getTownArea(Level world, BlockPos pos, Direction horizontalFacing, int chunkLength, int chunkWidth) {
		int minY = Math.max(pos.getY() - 3, world.getMinBuildHeight());
		int maxY = Math.min(minY + 40, world.getMaxBuildHeight() - 1);

		if (horizontalFacing.getAxis() == Direction.Axis.X) {
			int chunkMinX = (pos.getX() >> 4) - (horizontalFacing.getStepX() < 0 ? chunkLength : 0);
			int chunkMaxX = (pos.getX() >> 4) + (horizontalFacing.getStepX() > 0 ? chunkLength : 0);
			int chunkMinZ = (pos.getZ() >> 4) - (chunkWidth / 2);
			int chunkMaxZ = chunkMinZ + chunkWidth;
			return new TownBoundingArea(chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ, minY, maxY);
		}

		int chunkMinX = (pos.getX() >> 4) - (chunkWidth / 2);
		int chunkMaxX = chunkMinX + chunkWidth;
		int chunkMinZ = (pos.getZ() >> 4) - (horizontalFacing.getStepZ() < 0 ? chunkLength : 0);
		int chunkMaxZ = (pos.getZ() >> 4) + (horizontalFacing.getStepZ() > 0 ? chunkLength : 0);
		return new TownBoundingArea(chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ, minY, maxY);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		if (!player.level().isClientSide && !player.isShiftKeyDown()) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_TOWN_BUILDER, 0, 0, 0);
		}
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
	}

	public static String getTownName(ItemStack townBuilder) {
		return !ItemTools.hasTag(townBuilder) ? "" : ItemTools.getTag(townBuilder).getString("townName");
	}

	public static void setTownName(ItemStack townBuilder, String structName) {
		CompoundTag tag = ItemTools.getTag(townBuilder);
		tag.putString("townName", structName);
		ItemTools.setTag(townBuilder, tag);
	}

	public static void setWidth(ItemStack townBuilder, int width) {
		CompoundTag tag = ItemTools.getTag(townBuilder);
		tag.putInt("width", width);
		ItemTools.setTag(townBuilder, tag);
	}

	public static void setLength(ItemStack townBuilder, int length) {
		CompoundTag tag = ItemTools.getTag(townBuilder);
		tag.putInt("length", length);
		ItemTools.setTag(townBuilder, tag);
	}

	public static int getWidth(ItemStack townBuilder) {
		return ItemTools.hasTag(townBuilder) ? ItemTools.getTag(townBuilder).getInt("width") : 0;
	}

	public static int getLength(ItemStack townBuilder) {
		return ItemTools.hasTag(townBuilder) ? ItemTools.getTag(townBuilder).getInt("length") : 0;
	}
}
