package net.shadowmage.ancientwarfare.core.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.shadowmage.ancientwarfare.core.gui.GuiContainerBase.ActivationEvent;
import net.shadowmage.ancientwarfare.core.gui.Listener;
import net.shadowmage.ancientwarfare.core.interfaces.ITooltipRenderer;
import net.shadowmage.ancientwarfare.core.util.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * base GUI Element class
 * subclasses should add their own actionListeners during constructor
 * to handle default implementation details such as playing sound on button
 * click, toggling states or changing highlighting.
 *
 * Port note: render(mouseX, mouseY, partialTick) is now render(GuiGraphics, mouseX, mouseY, partialTick)
 * for every element; everything else keeps its 1.12 signature.
 *
 * @author Shadowmage
 */
public abstract class GuiElement {

	private List<Listener> actionListeners = new ArrayList<>();

	protected boolean mouseInterface;
	protected boolean keyboardInterface;

	protected boolean enabled;

	protected boolean visible;

	protected boolean selected;//isFocused -- for text-input lines / etc
	protected boolean scrollInput = false;//if should intercept scroll input, mostly used inside compositescrolled containers, really should only be true for number input widget
	protected boolean renderTooltip = true;
	protected Tooltip tooltip;

	protected long hoverStart;
	protected int topLeftX;

	protected int topLeftY;
	protected int renderX;

	protected int renderY;
	protected int width;

	protected int height;
	//resource file names are lower case in the port (1.12: guiBackgroundLarge.png, guiButtons1.png, guiButtons2.png)
	public static final ResourceLocation backgroundTextureLocation = ResourceLocation.fromNamespaceAndPath("ancientwarfare", "textures/gui/guibackgroundlarge.png");

	public static final ResourceLocation widgetTexture1 = ResourceLocation.fromNamespaceAndPath("ancientwarfare", "textures/gui/guibuttons1.png");
	public static final ResourceLocation widgetTexture2 = ResourceLocation.fromNamespaceAndPath("ancientwarfare", "textures/gui/guibuttons2.png");

	public GuiElement(int topLeftX, int topLeftY) {
		this.topLeftX = topLeftX;
		this.topLeftY = topLeftY;
		this.enabled = true;
		this.visible = true;
	}

	public GuiElement(int topLeftX, int topLeftY, int width, int height) {
		this(topLeftX, topLeftY);
		this.width = width;
		this.height = height;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setScrollable(boolean handleScroll) {
		this.scrollInput = handleScroll;
	}

	/*
	 * called to update the internal positioning of this element.
	 * needs to be called anytime the parent gui layout is changed (resized / etc)
	 */
	public void updateGuiPosition(int guiLeft, int guiTop) {
		renderX = topLeftX + guiLeft;
		renderY = topLeftY + guiTop;
	}

	public void setRenderPosition(int topLeftX, int topLeftY) {
		this.topLeftX = topLeftX;
		this.topLeftY = topLeftY;
	}

	/*
	 * called from GUI to process mouse interface.
	 * all functionality should be implemented via ActionListeners
	 * even default functionality (e.g. play sound on click, toggle state, etc)
	 */
	public final void handleMouseInput(ActivationEvent evt) {
		if (mouseInterface && visible && enabled && !actionListeners.isEmpty()) {
			for (Listener o : this.actionListeners) {
				//   bitwise check of types, if it returns !=0 at least 1 type bit was shared, so should execute
				if ((o.type & evt.type) != 0) {
					if (!o.onEvent(this, evt)) {
						break;
					}
				}
			}
		}
	}

	/*
	 * called from GUI to process keyboard interface
	 * all functionality should be implemented via ActionListeners
	 * including default functionality.
	 * <p/>
	 * will only fire events if this element is currently selected.
	 * selection is currently handled manually --
	 * must call element.setSelected() and element.clearSelected()
	 * currently multiple elements may be selected concurrently and all
	 * will receive the keyboard input events
	 */
	public final void handleKeyboardInput(ActivationEvent evt) {
		if (keyboardInterface && visible && enabled && !actionListeners.isEmpty()) {
			for (Listener o : this.actionListeners) {
				//   bitwise check of types, if it returns !=0 at least 1 type bit was shared, so should execute
				if ((o.type & evt.type) != 0) {
					if (!o.onEvent(this, evt)) {
						break;
					}
				}
			}
		}
	}

	/*
	 * add a new event listener to this element
	 * if the element is not set to receive those event types
	 * --auto-flag the element to receive those events
	 *
	 * @param listener the new listener to add
	 */
	public final void addNewListener(Listener listener) {
		this.actionListeners.add(listener);
		if ((listener.type & Listener.MOUSE_TYPES) != 0) {
			this.mouseInterface = true;
		}
		if ((listener.type & Listener.KEY_TYPES) != 0) {
			this.keyboardInterface = true;
		}
	}

	public final boolean isMouseOverElement(int mouseX, int mouseY) {
		return mouseX >= renderX && mouseX < renderX + width && mouseY >= renderY && mouseY < renderY + height;
	}

	public abstract void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);//called from gui to draw this element

	/*
	 * checks for tooltip-rendering for this gui-element.
	 */
	public void postRender(int mouseX, int mouseY, float partialTick, long tick, ITooltipRenderer rend) {
		if (tooltip == null) {
			return;
		}
		if (isMouseOverElement(mouseX, mouseY)) {
			if (hoverStart == -1) {
				hoverStart = tick;
			} else if (tick - hoverStart > 500)//0.5 seconds
			{
				rend.handleElementTooltipRender(tooltip, mouseX, mouseY);
			}
		} else {
			hoverStart = -1;
		}
	}

	public void setTooltip(Tooltip tip) {
		this.tooltip = tip;
	}

	public void addTooltip(String text) {
		addTooltip(text, 40);
	}

	public void addTooltip(String text, int maxWidthInChars) {
		// wrap around \n characters
		List<String> textLines = Arrays.asList(I18n.get(text).split("\\\\n"));

		// perform additional word-wrapping to maxWidthInChars, if required
		List<String> textLinesWrapped = new ArrayList<>();
		for (String textLine : textLines) {
			if (textLine.length() > maxWidthInChars) {
				// line is too long, perform additional splitting
				List<String> lineSplit = TextUtils.split(textLine, maxWidthInChars);
				textLinesWrapped.addAll(lineSplit);
			} else
				textLinesWrapped.add(textLine);
		}

		int width = Minecraft.getInstance().font.width(textLinesWrapped.get(0));
		int height = 0;

		// pre-calculate the width and height (necessary for multiline tooltips)
		for (int i = 0; i < textLinesWrapped.size(); i++) {
			int lineWidth = Minecraft.getInstance().font.width(textLinesWrapped.get(i));
			if (lineWidth > width)
				width = lineWidth;
			height = addLineSpacing(height, i, textLinesWrapped.size());
		}

		if (tooltip == null) {
			tooltip = new Tooltip(width, height);
		}

		height = 0;
		for (int i = 0; i < textLinesWrapped.size(); i++) {
			tooltip.addTooltipElement(new Label(0, height, textLinesWrapped.get(i)));
			height = addLineSpacing(height, i, textLinesWrapped.size());
		}
		this.renderTooltip = true;
	}

	// for multi-line tooltip formatting
	private int addLineSpacing(int height, int lineNumber, int totalLines) {
		if (lineNumber + 1 == totalLines)
			// no line spacing for the last line
			height += 8;
		else
			height += 10;
		return height;
	}

	public void setTooltipIfFound(String name) {
		String tooltipFound = null;
		String tooltipCheck;
		int endIndex = name.lastIndexOf(".");
		if (endIndex > 0) {
			// replace the last .segment of this key with ".all" and see if it has a tooltip
			tooltipCheck = name.substring(0, endIndex) + ".*.tooltip";
			if (isTooltipAvailable(tooltipCheck)) {
				tooltipFound = tooltipCheck;
			}
		}
		// even if there is a wildcard tooltip found, we still want to check for a more specific one
		tooltipCheck = name + ".tooltip";
		if (isTooltipAvailable(tooltipCheck))
			tooltipFound = tooltipCheck;

		if (tooltipFound != null)
			addTooltip(I18n.get(tooltipFound));
	}

	private boolean isTooltipAvailable(String key) {
		String translated = I18n.get(key);
		if (!translated.equals(key))
			return true;
		return false;
	}

	public boolean selected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/*
	 * port helper: 1.12 mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F))
	 */
	protected static void playClickSound() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

}
