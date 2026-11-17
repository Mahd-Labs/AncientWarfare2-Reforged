package net.shadowmage.ancientwarfare.structure.item;

import net.minecraft.world.item.Item;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.input.InputHandler;
import net.shadowmage.ancientwarfare.core.input.IItemKeyInterface;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.event.IBoxRenderer;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBuilder;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class ItemStructureBuilder extends ItemBaseStructure implements IItemKeyInterface, IBoxRenderer {
	private static final String LOCK_POS_TAG = "lockPos";

	public ItemStructureBuilder(String name) {
		super(name, new Item.Properties().stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		String structure = "guistrings.structure.no_selection";
		ItemStructureSettings viewSettings = ItemStructureSettings.getSettingsFor(stack);
		if (viewSettings.hasName()) {
			structure = viewSettings.name;
		}
		tooltip.add(Component.literal(net.shadowmage.ancientwarfare.structure.util.TextHelper.translate("guistrings.current_structure") + " " + net.shadowmage.ancientwarfare.structure.util.TextHelper.translate(structure)));
		Component key = net.shadowmage.ancientwarfare.core.item.ItemHammer.altItemUse1KeyName.get();
		tooltip.add(Component.translatable("guistrings.structure.place_structure", key));
		tooltip.add(Component.translatable("guistrings.structure.lock_overlay"));
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
		if (player == null || player.level().isClientSide) {
			return;
		}
		ItemStructureSettings buildSettings = ItemStructureSettings.getSettingsFor(stack);
		if (buildSettings.hasName()) {
			Optional<StructureTemplate> template = StructureTemplateManager.getTemplate(buildSettings.name);
			if (!template.isPresent()) {
				player.sendSystemMessage(Component.translatable("guistrings.template.not_found"));
				return;
			}
			Optional<Tuple<BlockPos, Direction>> buildPos = getBuildPosFromStackOrPlayer(player, stack);
			if (!buildPos.isPresent()) {
				return;
			}
			BlockPos hit = buildPos.get().getA();
			Direction facing = buildPos.get().getB();

			StructureBuilder builder = new StructureBuilder(player.level(), template.get(), facing, hit);
			buildStructure(player, hit, facing, builder);
			removeLockPosition(stack);
			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}
		} else {
			player.sendSystemMessage(Component.translatable("guistrings.structure.no_selection"));
		}
	}

	protected void buildStructure(Player player, BlockPos hit, Direction facing, StructureBuilder builder) {
		//budgeted_placement (improvement, not baseline): queued; with the switch off builder.instantConstruction() at once
		net.shadowmage.ancientwarfare.structure.template.build.StructurePlacementQueue.INSTANCE.build(builder, null, null);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		if (!player.level().isClientSide && !player.isShiftKeyDown() && player.getAbilities().instabuild) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_BUILDER, 0, 0, 0);
		}
		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown() && getLockPosition(stack).isPresent()) {
			removeLockPosition(stack);
		}

		return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
	}

	private void removeLockPosition(ItemStack stack) {
		//noinspection ConstantConditions
		ItemTools.getTag(stack).remove(LOCK_POS_TAG);
	}

	@Override
	public InteractionResult onItemUse(Player player, Level worldIn, BlockPos pos, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getItemInHand(hand);
		ItemStructureSettings buildSettings = ItemStructureSettings.getSettingsFor(stack);
		if (player.isShiftKeyDown() && buildSettings.hasName()) {
			if (!worldIn.isClientSide) {
				lockPosition(stack, pos.relative(facing), player);
			}
			return InteractionResult.SUCCESS;
		}

		return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
	}

	private void lockPosition(ItemStack stack, BlockPos pos, Player player) {
		CompoundTag tag = ItemTools.getTag(stack);
		//noinspection ConstantConditions
		tag.putLong(LOCK_POS_TAG, pos.asLong());
		tag.putByte("lockFacing", (byte) player.getDirection().get2DDataValue());
	}

	private Optional<Tuple<BlockPos, Direction>> getLockPosition(ItemStack stack) {
		CompoundTag tag = ItemTools.getTag(stack);
		return tag != null && tag.contains(LOCK_POS_TAG) ? Optional.of(new Tuple<>(BlockPos.of(tag.getLong(LOCK_POS_TAG)), Direction.from2DDataValue(tag.getByte("lockFacing")))) : Optional.empty();
	}


	public Optional<Tuple<BlockPos, Direction>> getBuildPosFromStackOrPlayer(Player player, ItemStack stack) {
		Optional<Tuple<BlockPos, Direction>> lockPosition = getLockPosition(stack);
		Optional<Tuple<BlockPos, Direction>> buildPos;
		if (lockPosition.isPresent()) {
			buildPos = lockPosition;
		} else {
			BlockPos hit = BlockTools.getBlockClickedOn(player, player.level(), true);
			if (hit == null) {
				return Optional.empty();
			}
			buildPos = Optional.of(new Tuple<>(hit, player.getDirection()));
		}
		return buildPos;
	}

}
