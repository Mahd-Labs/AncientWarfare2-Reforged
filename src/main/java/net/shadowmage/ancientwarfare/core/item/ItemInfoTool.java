package net.shadowmage.ancientwarfare.core.item;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.ItemTools;

import java.util.List;
import java.util.Map;

/*
 * Port notes:
 * - 1.12 copied the json to the system clipboard with AWT from the server thread (single player only). The json line
 *   sent to the player now carries a "copy to clipboard" click event instead.
 * - The loot entry json is written in the 1.21.1 loot table format (set_count / set_custom_data / set_damage).
 * - "Meta" in the info output is the damage value.
 */
public class ItemInfoTool extends ItemBaseCore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public ItemInfoTool() {
		super("info_tool", new Item.Properties().stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		tooltip.add(Component.literal(getMode(stack).getDisplayName() + " mode"));
	}

	private void printSimpleMessage(Player player, BlockState state) {
		player.sendSystemMessage(Component.literal("Block name: " + BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()));
		if (!state.getValues().isEmpty()) {
			player.sendSystemMessage(Component.literal("Properties:"));
			for (Map.Entry<Property<?>, Comparable<?>> prop : state.getValues().entrySet()) {
				player.sendSystemMessage(Component.literal(prop.getKey().getName() + " : " + prop.getValue().toString()));
			}
		}
	}

	private void printJSON(Player player, BlockState state) {
		String json = BlockTools.serializeToJson(state).toString();
		printAndCopyToClipboard(player, json);
	}

	public void printItemInfo(Player player, ItemStack infoTool, ItemStack stack) {
		Mode mode = getMode(infoTool);
		switch (mode) {
			case INFO:
				printSimpleMessage(player, stack);
				break;
			case JSON:
				printJSON(player, stack);
				break;
			case LOOT_ENTRY:
				printLootEntryJSON(player, stack);
				break;
		}
	}

	private void printSimpleMessage(Player player, ItemStack stack) {
		player.sendSystemMessage(Component.literal("Item name: " + BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
		player.sendSystemMessage(Component.literal("Meta: " + stack.getDamageValue()));
		if (ItemTools.hasTag(stack)) {
			player.sendSystemMessage(Component.literal("NBT: " + ItemTools.getTag(stack).toString()));
		}
	}

	private void printJSON(Player player, ItemStack stack) {
		String json = ItemTools.serializeToJson(stack).toString();
		printAndCopyToClipboard(player, json);
	}

	private void printAndCopyToClipboard(Player player, String json) {
		player.sendSystemMessage(Component.literal(json).withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, json))));
		player.sendSystemMessage(Component.literal("Click the text to copy it to the clipboard"));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		if (world.isClientSide) {
			return super.use(world, player, hand);
		}

		BlockHitResult hit = getPlayerPOVHitResult(world, player, ClipContext.Fluid.SOURCE_ONLY);
		if (hit.getType() == HitResult.Type.BLOCK) {
			BlockState state = world.getBlockState(hit.getBlockPos());
			Mode mode = getMode(player.getItemInHand(hand));
			switch (mode) {
				case INFO:
					printSimpleMessage(player, state);
					break;
				case JSON:
					printJSON(player, state);
					break;
				case LOOT_ENTRY:
					printLootEntryJSON(player, state, hit, world);
					break;
			}
			return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
		}

		if (player.isShiftKeyDown()) {
			return new InteractionResultHolder<>(InteractionResult.SUCCESS, cycleMode(player.getItemInHand(hand)));
		} else {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_INFO_TOOL);
			return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
		}
	}

	private void printLootEntryJSON(Player player, BlockState state, BlockHitResult hit, Level world) {
		printLootEntryJSON(player, state.getCloneItemStack(hit, world, hit.getBlockPos(), player));
	}

	private void printLootEntryJSON(Player player, ItemStack stack) {
		JsonArray functions = new JsonArray();
		JsonObject setCount = new JsonObject();
		setCount.addProperty("function", "minecraft:set_count");
		setCount.addProperty("count", 1);
		functions.add(setCount);
		if (ItemTools.hasTag(stack)) {
			JsonObject setNbt = new JsonObject();
			setNbt.addProperty("function", "minecraft:set_custom_data");
			setNbt.addProperty("tag", ItemTools.getTag(stack).toString());
			functions.add(setNbt);
		}
		if (stack.isDamageableItem()) {
			JsonObject setDamage = new JsonObject();
			setDamage.addProperty("function", "minecraft:set_damage");
			setDamage.addProperty("damage", 1F - (float) stack.getDamageValue() / (float) stack.getMaxDamage());
			functions.add(setDamage);
		}

		JsonObject json = new JsonObject();
		json.addProperty("type", "minecraft:item");
		json.addProperty("name", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		json.addProperty("weight", 1);
		json.add("functions", functions);
		printAndCopyToClipboard(player, GSON.toJson(json));
	}

	private ItemStack cycleMode(ItemStack stack) {
		CompoundTag tag = ItemTools.getTag(stack);
		tag.putString("mode", getMode(stack).cycle().name().toLowerCase());
		ItemTools.setTag(stack, tag);
		return stack;
	}

	private Mode getMode(ItemStack stack) {
		if (ItemTools.hasTag(stack)) {
			return Mode.fromString(ItemTools.getTag(stack).getString("mode"));
		}
		return Mode.INFO;
	}

	enum Mode {
		INFO("Info"),
		JSON("JSON"),
		LOOT_ENTRY("Loot Entry");

		private String displayName;

		Mode(String displayName) {
			this.displayName = displayName;
		}

		private static final Map<String, Mode> NAME_MODE_MAP;

		static {
			ImmutableMap.Builder<String, Mode> builder = new ImmutableMap.Builder<>();
			for (Mode mode : Mode.values()) {
				builder.put(mode.name().toLowerCase(), mode);
			}
			NAME_MODE_MAP = builder.build();
		}

		public Mode cycle() {
			switch (this) {
				case INFO:
					return JSON;
				case JSON:
					return LOOT_ENTRY;
				case LOOT_ENTRY:
					return INFO;
			}
			return INFO;
		}

		public static Mode fromString(String mode) {
			return NAME_MODE_MAP.getOrDefault(mode.toLowerCase(), INFO);
		}

		public String getDisplayName() {
			return displayName;
		}
	}
}
