package net.shadowmage.ancientwarfare.structure.item;

import net.minecraft.nbt.ByteTag;
import net.minecraft.world.item.Item;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.input.InputHandler;
import net.shadowmage.ancientwarfare.core.input.IItemKeyInterface;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.shadowmage.ancientwarfare.structure.event.IBoxRenderer;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;
import net.shadowmage.ancientwarfare.structure.template.build.validation.StructureValidationType;
import net.shadowmage.ancientwarfare.structure.template.build.validation.StructureValidator;
import net.shadowmage.ancientwarfare.structure.template.load.TemplateLoader;
import net.shadowmage.ancientwarfare.structure.template.save.TemplateExporter;
import net.shadowmage.ancientwarfare.structure.template.scan.TemplateScanner;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.awt.Color.GREEN;

public class ItemStructureScanner extends ItemBaseStructure implements IItemKeyInterface, IBoxRenderer {
	private static final String VALIDATOR_TAG = "validator";
	private static final String STRUCTURE_NAME_TAG = "structureName";
	private static final String INCLUDE_TAG = "include";
	private static final String MOD_DEPENDENCIES_TAG = "mods";

	public ItemStructureScanner(String name) {
		super(name, new Item.Properties().stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		if (!stack.isEmpty()) {
			ItemStructureSettings viewSettings = ItemStructureSettings.getSettingsFor(stack);
			Component key = net.shadowmage.ancientwarfare.core.item.ItemHammer.altItemUse1KeyName.get();
			if (!viewSettings.hasPos1()) {
				tooltip.add(Component.translatable("guistrings.structure.scanner.select_first_pos", key));
				tooltip.add(Component.literal("(1/4)"));
			} else if (!viewSettings.hasPos2()) {
				tooltip.add(Component.translatable("guistrings.structure.scanner.select_second_pos", key));
				tooltip.add(Component.literal("(2/4)"));
			} else if (!viewSettings.hasBuildKey()) {
				tooltip.add(Component.translatable("guistrings.structure.scanner.select_offset", key));
				tooltip.add(Component.literal("(3/4)"));
			} else {
				tooltip.add(Component.literal(key + " : " + net.shadowmage.ancientwarfare.structure.util.TextHelper.translate("guistrings.structure.scanner.click_to_process")));
				tooltip.add(Component.literal("(4/4)"));
			}
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (world.isClientSide) {
			return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
		}
		ItemStructureSettings scanSettings = ItemStructureSettings.getSettingsFor(stack);
		if (player.isShiftKeyDown()) {
			scanSettings.clearSettings();
			ItemStructureSettings.setSettingsFor(stack, scanSettings);
		} else if (readyToExport(scanSettings)) {
			BlockPos key = scanSettings.key;
			if (Math.sqrt(player.distanceToSqr(key.getX() + 0.5d, key.getY(), key.getZ() + 0.5d)) > 10) {
				player.sendSystemMessage(Component.translatable("guistrings.structure.scanner.too_far"));
				return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
			}
			player.sendSystemMessage(Component.translatable("guistrings.structure.scanner.exporting"));
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_SCANNER, 0, 0, 0);
		}
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
	}

	public static boolean readyToExport(ItemStack scanner) {
		return readyToExport(ItemStructureSettings.getSettingsFor(scanner));
	}

	private static boolean readyToExport(ItemStructureSettings scanSettings) {
		return scanSettings.hasPos1() && scanSettings.hasPos2() && scanSettings.hasBuildKey();
	}

	public static void setStructureName(ItemStack stack, String name) {
		ItemTools.setTagInfo(stack,STRUCTURE_NAME_TAG, StringTag.valueOf(name));
	}

	public static String getStructureName(ItemStack stack) {
		//noinspection ConstantConditions
		return ItemTools.hasTag(stack) ? ItemTools.getTag(stack).getString(STRUCTURE_NAME_TAG) : "";
	}

	public static void setValidator(ItemStack stack, StructureValidator validator) {
		ItemTools.setTagInfo(stack,VALIDATOR_TAG, validator.serializeToNBT());
	}

	public static StructureValidator getValidator(ItemStack stack) {
		//noinspection ConstantConditions
		if (!ItemTools.hasTag(stack) || !ItemTools.getTag(stack).contains(VALIDATOR_TAG)) {
			return StructureValidationType.GROUND.getValidator();
		}

		CompoundTag validatorTag = ItemTools.getTag(stack).getCompound(VALIDATOR_TAG);

		Optional<StructureValidationType> type = StructureValidationType.getTypeFromName(validatorTag.getString("validationType"));
		if (!type.isPresent()) {
			return StructureValidationType.GROUND.getValidator();
		}

		StructureValidator validator = type.get().getValidator();
		validator.readFromNBT(validatorTag);
		return validator;
	}

	public static void setIncludeImmediately(ItemStack stack, boolean include) {
		ItemTools.setTagInfo(stack,INCLUDE_TAG, ByteTag.valueOf((byte) (include ? 1 : 0)));
	}

	public static boolean getIncludeImmediately(ItemStack stack) {
		//noinspection ConstantConditions
		return ItemTools.hasTag(stack) && ItemTools.getTag(stack).getBoolean(INCLUDE_TAG);
	}

	public static boolean scanStructure(Level world, ItemStack scanner) {
		ItemStructureSettings settings = ItemStructureSettings.getSettingsFor(scanner);

		int turns = (6 - settings.buildFace.get2DDataValue()) % 4;
		StructureTemplate template = TemplateScanner.scan(world, getModDependencies(scanner), settings.getMin(), settings.getMax(), settings.key, turns, getStructureName(scanner));

		StructureValidator validator = getValidator(scanner);
		template.setValidationSettings(validator);
		boolean include = getIncludeImmediately(scanner);
		if (include) {
			StructureTemplateManager.addTemplate(template);
		}
		return TemplateExporter.exportTo(template, new File(include ? TemplateLoader.INCLUDE_DIRECTORY : TemplateLoader.OUTPUT_DIRECTORY));
	}

	public static Set<String> getModDependencies(ItemStack scanner) {
		//noinspection ConstantConditions
		return ItemTools.hasTag(scanner) ? NBTHelper.getStringSet(ItemTools.getTag(scanner).getList(MOD_DEPENDENCIES_TAG, Tag.TAG_STRING)) : Collections.emptySet();
	}

	@Override
	public boolean onKeyActionClient(Player player, ItemStack stack, ItemAltFunction altFunction) {
		return altFunction == ItemAltFunction.ALT_FUNCTION_1;
	}

	@Override
	public void onKeyAction(Player player, ItemStack stack, ItemAltFunction altFunction) {
		BlockPos hit = BlockTools.getBlockClickedOn(player, player.level(), player.isShiftKeyDown());
		if (hit == null) {
			return;
		}
		ItemStructureSettings scanSettings = ItemStructureSettings.getSettingsFor(stack);
		if (!scanSettings.hasPos1()) {
			scanSettings.setPos1(hit);
			player.sendSystemMessage(Component.translatable("guistrings.structure.scanner.set_first_pos"));
		} else if (!scanSettings.hasPos2()) {
			scanSettings.setPos2(hit);
			player.sendSystemMessage(Component.translatable("guistrings.structure.scanner.set_second_pos"));
		} else if (!scanSettings.hasBuildKey()) {
			scanSettings.setBuildKey(hit, player.getDirection());
			player.sendSystemMessage(Component.translatable("guistrings.structure.scanner.set_offset_pos"));
		} else {
			player.sendSystemMessage(Component.translatable("guistrings.structure.scanner.click_to_process"));
		}
		ItemStructureSettings.setSettingsFor(stack, scanSettings);
	}


	@SuppressWarnings("ConstantConditions")
	public static void clearSettings(ItemStack scanner) {
		ItemStructureSettings settings = ItemStructureSettings.getSettingsFor(scanner);
		settings.clearSettings();
		ItemStructureSettings.setSettingsFor(scanner, settings);
		ItemTools.getTag(scanner).remove(STRUCTURE_NAME_TAG);
		ItemTools.getTag(scanner).remove(MOD_DEPENDENCIES_TAG);
		ItemTools.getTag(scanner).remove((INCLUDE_TAG));
		{
			CompoundTag stackTag = ItemTools.getTag(scanner);
			stackTag.remove(VALIDATOR_TAG);
			ItemTools.setTag(scanner, stackTag);
		}
	}

	public static void setModDependencies(ItemStack scanner, Set<String> mods) {
		ItemTools.setTagInfo(scanner,MOD_DEPENDENCIES_TAG, NBTHelper.getNBTStringList(mods));
	}
}
