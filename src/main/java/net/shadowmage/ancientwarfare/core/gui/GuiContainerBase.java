package net.shadowmage.ancientwarfare.core.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.container.ContainerBase;
import net.shadowmage.ancientwarfare.core.gui.elements.GuiElement;
import net.shadowmage.ancientwarfare.core.gui.elements.Tooltip;
import net.shadowmage.ancientwarfare.core.interfaces.IContainerGuiCallback;
import net.shadowmage.ancientwarfare.core.interfaces.ITooltipRenderer;
import net.shadowmage.ancientwarfare.core.interfaces.IWidgetSelection;
import net.shadowmage.ancientwarfare.core.util.RenderTools;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/*
 * Port notes (1.12 GuiContainer -> 1.21.1 AbstractContainerScreen). See core/gui/README.md for the full table.
 *  fields:   xSize/ySize -> imageWidth/imageHeight, guiLeft/guiTop -> leftPos/topPos, mc -> minecraft,
 *            fontRenderer -> font, inventorySlots -> menu (or getContainer())
 *  methods:  initGui -> init, updateScreen -> containerTick, drawScreen -> render,
 *            drawGuiContainerBackgroundLayer -> renderBg, drawGuiContainerForegroundLayer -> renderLabels,
 *            onGuiClosed -> removed, mc.displayGuiScreen(x) -> Minecraft.getInstance().setScreen(x)
 *  input:    handleMouseInput -> mouseClicked / mouseReleased / mouseMoved / mouseScrolled,
 *            handleKeyboardInput -> keyPressed / keyReleased / charTyped. Elements still get one ActivationEvent per
 *            input event. ActivationEvent.key is now a GLFW key code (GLFW.GLFW_KEY_*) instead of an LWJGL2
 *            Keyboard code. Typed characters arrive as a separate KEY_DOWN event with key == GLFW_KEY_UNKNOWN (-1)
 *            and ch set; key presses arrive with ch == 0. ActivationEvent.mw keeps the LWJGL2 scale (120 per notch).
 *  slots:    slot positions are final in 1.21.1. While ContainerBase.areSlotsHidden() is true the slots (and their
 *            backgrounds) are not drawn and cannot be hovered or clicked (1.12 moved them off screen).
 */
public abstract class GuiContainerBase<T extends ContainerBase> extends AbstractContainerScreen<ContainerBase> implements IContainerGuiCallback, ITooltipRenderer, IWidgetSelection {

	private GuiElement selectedWidget = null;
	protected boolean shouldCloseOnVanillaKeys = true;
	private float partialRenderTick = 0.f;
	private boolean initDone = false;
	private boolean shouldUpdate = false;
	private List<GuiElement> elements = new ArrayList<>();

	private Tooltip elementTooltip;
	private int elementTooltipX;
	private int elementTooltipY;

	private ItemStack tooltipStack = ItemStack.EMPTY;
	private int tooltipX;
	private int tooltipY;

	private ResourceLocation backgroundTexture;

	protected Player player;

	protected GuiContainerBase(ContainerBase container, int xSize, int ySize) {
		super(container, container.player.getInventory(), Component.empty());
		container.setGui(this);
		this.imageWidth = xSize;
		this.imageHeight = ySize;
		this.player = container.player;
		this.backgroundTexture = GuiElement.backgroundTextureLocation;
	}

	public GuiContainerBase(ContainerBase container) {
		this(container, 256, 240);
	}

	public final void setTexture(String name) {
		this.backgroundTexture = ResourceLocation.parse(name);
	}

	@SuppressWarnings("unchecked")
	public final T getContainer() {
		return (T) this.menu;
	}

	@Override
	public void handleItemStackTooltipRender(ItemStack itemStack, int mouseX, int mouseY) {
		this.tooltipStack = itemStack;
		this.tooltipX = mouseX;
		this.tooltipY = mouseY;
	}

	@Override
	public void handleElementTooltipRender(Tooltip tooltip, int mouseX, int mouseY) {
		this.elementTooltip = tooltip;
		elementTooltipX = mouseX;
		elementTooltipY = mouseY;
	}

	protected void clearElements() {
		if (this.selectedWidget != null && this.elements.contains(selectedWidget)) {
			this.selectedWidget = null;
		}
		this.elements.clear();
	}

	protected void addGuiElement(GuiElement element) {
		this.elements.add(element);
	}

	protected void removeGuiElement(GuiElement element) {
		this.elements.remove(element);
	}

	/*
	 * 1.12 handleMouseInput: vanilla handling first, then one event to every element
	 */
	private void dispatchMouseEvent(int type, int button, boolean state, double mouseX, double mouseY, int wheel) {
		ActivationEvent evt = new ActivationEvent(type, button, state, (int) mouseX, (int) mouseY, wheel);
		//iterate over a copy: a listener may add or remove elements while the event is being dispatched
		for (GuiElement element : new ArrayList<>(this.elements)) {
			element.handleMouseInput(evt);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		super.mouseClicked(mouseX, mouseY, button);
		dispatchMouseEvent(Listener.MOUSE_DOWN, button, true, mouseX, mouseY, 0);
		return true;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		super.mouseReleased(mouseX, mouseY, button);
		dispatchMouseEvent(Listener.MOUSE_UP, button, false, mouseX, mouseY, 0);
		return true;
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		super.mouseMoved(mouseX, mouseY);
		dispatchMouseEvent(Listener.MOUSE_MOVED, -1, false, mouseX, mouseY, 0);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		int wheel = (int) (scrollY * 120);//LWJGL2 reported 120 per wheel notch, the widgets are written against that
		if (wheel != 0) {
			dispatchMouseEvent(Listener.MOUSE_WHEEL, -1, false, mouseX, mouseY, wheel);
		}
		return true;
	}

	private void dispatchKeyEvent(int type, int key, char ch, boolean state) {
		ActivationEvent evt = new ActivationEvent(type, key, ch, state);
		for (GuiElement element : new ArrayList<>(this.elements)) {
			element.handleKeyboardInput(evt);
		}
	}

	/*
	 * 1.12 handleKeyboardInput + keyTyped
	 */
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		dispatchKeyEvent(Listener.KEY_DOWN, keyCode, (char) 0, true);

		if (selectedWidget == null) {
			boolean isExitCommand = keyCode == GLFW.GLFW_KEY_ESCAPE || this.minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode));
			if (isExitCommand) {
				if (shouldCloseOnVanillaKeys) {
					closeGui();
				}
			} else {
				super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
		return true;
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		dispatchKeyEvent(Listener.KEY_DOWN, GLFW.GLFW_KEY_UNKNOWN, codePoint, true);
		return true;
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		dispatchKeyEvent(Listener.KEY_UP, keyCode, (char) 0, false);
		if (selectedWidget == null) {
			super.keyReleased(keyCode, scanCode, modifiers);
		}
		return true;
	}

	protected final void closeGui() {
		if (onGuiCloseRequested()) {
			Minecraft.getInstance().player.closeContainer();
		}
	}

	/*
	 * over-rideable method for onGuiClosed w/ proper functionality -- (is called before the fucking container disappears)
	 * derived classes can override this to handle custom onClosed functionality (e.g. call container to send data to server/
	 * finalize settings)
	 *
	 * @return true if the GUI should closed -- false if another GUI has been opened/the GUI should not closed
	 */
	protected boolean onGuiCloseRequested() {
		return true;
	}

	@Override
	protected void init() {
		super.init();
		if (!initDone) {
			initElements();
			initDone = true;
		}
		this.setupElements();
		for (GuiElement element : this.elements) {
			element.updateGuiPosition(leftPos, topPos);
		}
	}

	@Override
	protected void containerTick() {
		if (this.shouldUpdate) {
			menu.setGui(this);
			this.init();
			this.shouldUpdate = false;
		}
		super.containerTick();
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		if (backgroundTexture != null) {
			RenderTools.renderQuarteredTexture(graphics, backgroundTexture, 256, 256, 0, 0, 256, 240, width / 2 - imageWidth / 2, (height / 2) - (imageHeight / 2), imageWidth, imageHeight);
		}
		if (!menu.areSlotsHidden()) {
			for (Slot slot : this.menu.slots) {
				graphics.blit(GuiElement.widgetTexture1, slot.x - 1 + leftPos, slot.y - 1 + topPos, 152, 120, 18, 18);
			}
		}
	}

	@Override
	protected void renderSlot(GuiGraphics graphics, Slot slot) {
		if (!menu.areSlotsHidden()) {
			super.renderSlot(graphics, slot);
		}
	}

	/*
	 * slot hit test used by the vanilla hover / click code; no slot can be hit while the container hides its slots
	 */
	@Override
	protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
		return !menu.areSlotsHidden() && super.isHovering(x, y, width, height, mouseX, mouseY);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		partialRenderTick = partialTicks;
		super.render(graphics, mouseX, mouseY, partialTicks);
		renderTooltip(graphics, mouseX, mouseY);
		if (!tooltipStack.isEmpty()) {
			graphics.renderTooltip(font, tooltipStack, tooltipX, tooltipY);
			tooltipStack = ItemStack.EMPTY;
		}
		if (elementTooltip != null) {
			elementTooltip.renderTooltip(graphics, elementTooltipX, elementTooltipY, partialRenderTick);
			elementTooltip = null;
		}
	}

	/*
	 * 1.12 drawGuiContainerForegroundLayer. The pose is translated to leftPos/topPos by the vanilla screen,
	 * elements render in absolute screen coordinates.
	 */
	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.pose().pushPose();
		graphics.pose().translate(-leftPos, -topPos, 0);
		long time = System.currentTimeMillis();
		graphics.setColor(1.f, 1.f, 1.f, 1.f);
		for (GuiElement element : elements) {
			element.render(graphics, mouseX, mouseY, partialRenderTick);
			element.postRender(mouseX, mouseY, partialRenderTick, time, this);
		}
		graphics.pose().popPose();
	}

	/*
	 * call this method to enforce a re-initialization of gui at the start of next game tick (not render tick)<br>
	 * setupElements() will then be called<br>
	 * and all elements will have their position updated
	 */
	@Override
	public void refreshGui() {
		this.shouldUpdate = true;
	}

	/*
	 * Sub-classes should implement this method to add initial gui elements.<br>
	 * Only called a single time, shortly after construction,
	 * but before any rendering or other update methods are called
	 */
	public abstract void initElements();

	/*
	 * sub-classes should implement this method to setup/change any elements that need adjusting when the gui is initialized<br>
	 * any elements that are positioned outside of the gui-window space will need their positions updated by calling element.setPosition(xPos, yPos)
	 * as they reference internal position relative to the guiLeft / guiTop values from this gui (which are passed in and updated directly after setupElements() is called)<br>
	 * Always called at least once, directly after {@link #initElements()}
	 */
	public abstract void setupElements();

	/*
	 * sub-classes should override this method to handle any expected packet data
	 */
	@Override
	public void handlePacketData(CompoundTag data) {

	}

	@Override
	public void onWidgetSelected(GuiElement element) {
		this.selectedWidget = element;
		element.setSelected(true);
	}

	@Override
	public void onWidgetDeselected(GuiElement element) {
		if (selectedWidget == element) {
			selectedWidget = null;
		}
		element.setSelected(false);
	}

	public void onCompositeCleared(List<GuiElement> compositeElements) {
		if (compositeElements.contains(selectedWidget)) {
			selectedWidget = null;
		}
	}

	/*
	 * Action event for gui-widgets.  A single event is sent to all element / listeners for each input event (key up/down, mouse up/down/move/wheel)
	 *
	 * @author Shadowmage
	 */
	public static class ActivationEvent {
		/*
		 * the type of event, one of the Listener constants (KEY_UP, KEY_DOWN, MOUSE_UP, MOUSE_DOWN, MOUSE_WHEEL, MOUSE_MOVED)
		 */
		public final int type;

		/*
		 * what key was pressed? (GLFW key code; GLFW_KEY_UNKNOWN for a typed-character event)
		 */
		public int key;

		/*
		 * the mouse button number.
		 * -1 = none
		 * 0 = LMB
		 * 1 = RMB
		 * 2+= ?
		 */
		public int mButton;
		/*
		 * the state of the button or key.  true for pressed, false for released
		 */
		public boolean state;
		/*
		 * the input char, for keyboard events (0 for a key press without character)
		 */
		public char ch;
		/*
		 * mouse x position
		 */
		public int mx;
		/*
		 * mouse y position
		 */
		public int my;
		/*
		 * mouse-wheel delta movement
		 */
		public int mw;//mousewheel delta movement

		private ActivationEvent(int type, int button, boolean state, int mx, int my, int mw) {
			this.type = type;
			this.mButton = button;
			this.state = state;
			this.mx = mx;
			this.my = my;
			this.mw = mw;
		}

		private ActivationEvent(int type, int key, char character, boolean state) {
			this.type = type;
			this.key = key;
			this.ch = character;
			this.state = state;
		}
	}

	/*
	 * Push a new scissors-test viewport onto the stack.<br>
	 * If this viewport would extend outside of the currently-set viewport, it
	 * will be truncated to fit inside of the existing viewport
	 * (the stack and the truncation are done by GuiGraphics in 1.21.1)
	 */
	public static void pushViewport(GuiGraphics graphics, int x, int y, int w, int h) {
		graphics.enableScissor(x, y, x + w, y + h);
	}

	/*
	 * pop a scissors-test viewport off of the stack
	 */
	public static void popViewport(GuiGraphics graphics) {
		graphics.disableScissor();
	}

}
