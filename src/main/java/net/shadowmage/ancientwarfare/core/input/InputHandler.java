package net.shadowmage.ancientwarfare.core.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.PacketItemMouseScroll;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;
import net.shadowmage.ancientwarfare.core.input.IItemKeyInterface.ItemAltFunction;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/*
 * CLIENT ONLY.
 * Port notes: key bindings are registered through RegisterKeyMappingsEvent (AncientWarfareCoreClient adds
 * InputHandler::initKeyBindings as mod bus listener). Other modules register their own KeyMappings in their own
 * RegisterKeyMappingsEvent listener and their callbacks with registerCallBack, like 1.12.
 * 1.12 KeyInputEvent -> InputEvent.Key (action GLFW_PRESS), MouseEvent wheel -> InputEvent.MouseScrollingEvent.
 */
public class InputHandler {

	private static final String CATEGORY = "keybind.category.awCore";
	public static final KeyMapping ALT_ITEM_USE_1 = new KeyMapping(AWCoreStatics.KEY_ALT_ITEM_USE_1, ItemKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);
	public static final KeyMapping ALT_ITEM_USE_2 = new KeyMapping(AWCoreStatics.KEY_ALT_ITEM_USE_2, ItemKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);
	public static final KeyMapping ALT_ITEM_USE_3 = new KeyMapping(AWCoreStatics.KEY_ALT_ITEM_USE_3, ItemKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY);
	public static final KeyMapping ALT_ITEM_USE_4 = new KeyMapping(AWCoreStatics.KEY_ALT_ITEM_USE_4, ItemKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
	public static final KeyMapping ALT_ITEM_USE_5 = new KeyMapping(AWCoreStatics.KEY_ALT_ITEM_USE_5, ItemKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);

	private static final Set<InputCallbackDispatcher> keybindingCallbacks = new HashSet<>();

	static {
		NeoForge.EVENT_BUS.register(new InputHandler());
	}

	private InputHandler() {
	}

	public static void initKeyBindings(RegisterKeyMappingsEvent event) {
		event.register(ALT_ITEM_USE_1);
		event.register(ALT_ITEM_USE_2);
		event.register(ALT_ITEM_USE_3);
		event.register(ALT_ITEM_USE_4);
		event.register(ALT_ITEM_USE_5);

		initCallbacks();
	}

	private static void initCallbacks() {
		registerCallBack(ALT_ITEM_USE_1, new ItemInputCallback(ItemAltFunction.ALT_FUNCTION_1));
		registerCallBack(ALT_ITEM_USE_2, new ItemInputCallback(ItemAltFunction.ALT_FUNCTION_2));
		registerCallBack(ALT_ITEM_USE_3, new ItemInputCallback(ItemAltFunction.ALT_FUNCTION_3));
		registerCallBack(ALT_ITEM_USE_4, new ItemInputCallback(ItemAltFunction.ALT_FUNCTION_4));
		registerCallBack(ALT_ITEM_USE_5, new ItemInputCallback(ItemAltFunction.ALT_FUNCTION_5));
	}

	public static void registerCallBack(KeyMapping keyBinding, IInputCallback callback) {
		Predicate<InputCallbackDispatcher> matchingKeyBinding = d -> d.getKeyBinding().equals(keyBinding);
		if (keybindingCallbacks.stream().anyMatch(matchingKeyBinding)) {
			keybindingCallbacks.stream().filter(matchingKeyBinding).findFirst().ifPresent(d -> d.addInputCallback(callback));
		} else {
			keybindingCallbacks.add(new InputCallbackDispatcher(keyBinding, callback));
		}
	}

	@SubscribeEvent
	public void onKeyInput(InputEvent.Key evt) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		if (player == null) {
			return;
		}

		boolean state = evt.getAction() == GLFW.GLFW_PRESS;

		if (state) {
			keybindingCallbacks.stream().filter(k -> k.getKeyBinding().isDown()).forEach(InputCallbackDispatcher::onKeyPressed);
		}
	}

	@SubscribeEvent
	public void onMouseEvent(InputEvent.MouseScrollingEvent event) {
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		if (player.isShiftKeyDown() && event.getScrollDeltaY() != 0) {
			ItemStack stack = player.getMainHandItem();
			Item item = stack.getItem();
			if (item instanceof IScrollableItem) {
				if (event.getScrollDeltaY() > 0) {
					if (((IScrollableItem) item).onScrollUp(player.level(), player, stack)) {
						NetworkHandler.sendToServer(new PacketItemMouseScroll(true));
					}

				} else {
					if (((IScrollableItem) item).onScrollDown(player.level(), player, stack)) {
						NetworkHandler.sendToServer(new PacketItemMouseScroll(false));
					}
				}
				event.setCanceled(true);
			}
		}
	}
}
