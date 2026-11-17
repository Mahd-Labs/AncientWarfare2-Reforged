package net.shadowmage.ancientwarfare.core.network;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.container.ContainerBase;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/*
 * Client-only half of the 1.12 NetworkHandler gui registry. Only reference this class from client-only code.
 * Call registerGui during client mod construction (before RegisterMenuScreensEvent fires).
 * The 1.12 code registered gui classes that had a (ContainerBase) constructor; here a factory does the same job.
 */
public final class NetworkHandlerClient {
	private NetworkHandlerClient() {}

	private static final Map<Integer, Function<ContainerBase, ? extends AbstractContainerScreen<ContainerBase>>> guiFactories = new HashMap<>();

	public static void registerGui(int id, Function<ContainerBase, ? extends AbstractContainerScreen<ContainerBase>> guiFactory) {
		guiFactories.put(id, guiFactory);
	}

	public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
		for (Map.Entry<Integer, Function<ContainerBase, ? extends AbstractContainerScreen<ContainerBase>>> entry : guiFactories.entrySet()) {
			MenuType<ContainerBase> type = NetworkHandler.getMenuType(entry.getKey());
			if (type == null) {
				AncientWarfare.LOG.error("Gui {} registered without a container", entry.getKey());
				continue;
			}
			Function<ContainerBase, ? extends AbstractContainerScreen<ContainerBase>> factory = entry.getValue();
			event.<ContainerBase, AbstractContainerScreen<ContainerBase>>register(type, (menu, inv, title) -> menu.isPlaceholder() ? new ClosingScreen(menu, inv, title) : factory.apply(menu));
		}
	}

	/* the screen of a placeholder container; it shows nothing and closes on its first tick */
	private static final class ClosingScreen extends AbstractContainerScreen<ContainerBase> {
		private ClosingScreen(ContainerBase menu, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.network.chat.Component title) {
			super(menu, inventory, title);
		}

		@Override
		protected void renderBg(net.minecraft.client.gui.GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		}

		@Override
		protected void containerTick() {
			onClose();
		}
	}
}
