package net.shadowmage.ancientwarfare.core.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.shadowmage.ancientwarfare.core.gui.GuiContainerBase.ActivationEvent;
import net.shadowmage.ancientwarfare.core.gui.Listener;
import net.shadowmage.ancientwarfare.core.interfaces.IWidgetSelection;
import net.shadowmage.ancientwarfare.core.util.RenderTools;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

/*
 * Text input field
 * height = 12px
 *
 * Port note: handleKeyInput(keyCode, ch) receives GLFW key codes. A key press arrives with ch == 0 and a typed
 * character arrives as its own call with keyCode == GLFW_KEY_UNKNOWN (1.12 delivered both in one call).
 *
 * @author Shadowmage
 */
public class Text extends GuiElement {

	TextValidator textValidator = new TextValidator();
	IWidgetSelection selector;
	String text;
	int cursorIndex;

	public Text(int topLeftX, int topLeftY, int width, String defaultText, IWidgetSelection selector) {
		super(topLeftX, topLeftY, width, 12);
		if (defaultText == null) {
			defaultText = "";
		}
		this.text = defaultText;
		this.cursorIndex = text.length();
		this.selector = selector;
		this.addDefaultListeners();
		addAllowedChars();
	}

	protected void addAllowedChars() {
		this.textValidator.addValidChars(allowedChars);
		this.textValidator.addValidChars(allowedCharSymbols);
		this.textValidator.addValidChars(allowedNums);
		this.textValidator.addValidChars(allowedNumSymbols);
	}

	protected void addDefaultListeners() {
		this.addNewListener(new Listener(Listener.MOUSE_UP) {
			@Override
			public boolean onEvent(GuiElement widget, ActivationEvent evt) {
				if (enabled && visible && isMouseOverElement(evt.mx, evt.my)) {
					if (selected && evt.mButton == 1) {
						String oldText = text;
						setText("");
						onTextUpdated(oldText, text);
						return false;
					}
					setSelected(true);
					selector.onWidgetSelected(Text.this);
					cursorIndex = text.length();
				} else {
					if (selected) {
						selector.onWidgetDeselected(Text.this);
					}
					setSelected(false);
				}
				return true;
			}
		});

		this.addNewListener(new Listener(Listener.KEY_DOWN) {
			@Override
			public boolean onEvent(GuiElement widget, ActivationEvent evt) {
				if (enabled && visible && selected) {
					handleKeyInput(evt.key, evt.ch);
				}
				return true;
			}
		});
	}

	/*
	 * Called directly after character input updates or text changes from
	 * delete/backspace the new text has already been set.<br>
	 * Any references to this.text will return the newtext old test is passed in
	 * as a param so that it may be validated / reset to the previous text (cursor
	 * position will need manual updating)<br>
	 * Anonymous/Subclasses can override/implement this method for specialized
	 * callback-type functionality.
	 */
	public void onTextUpdated(String oldText, String newText) {

	}

	public void removeAllowedChars(char... chars) {
		for (char ch : chars) {
			this.textValidator.removeValidChar(ch);
		}
	}

	public void setAllowedChars(Set<Character> allowedChars) {
		this.textValidator = new TextValidator(allowedChars);
	}

	public void setAllowedChars(char[] chars) {
		this.textValidator = new TextValidator(chars);
	}

	protected void handleKeyInput(int keyCode, char ch) {
		boolean handled = false;
		switch (keyCode) {
			case GLFW.GLFW_KEY_LEFT: {
				handled = true;
				cursorIndex--;
				if (cursorIndex < 0) {
					cursorIndex = 0;
				}
			}
			break;
			case GLFW.GLFW_KEY_RIGHT: {
				handled = true;
				cursorIndex++;
				if (cursorIndex > text.length()) {
					cursorIndex = text.length();
				}
			}
			break;
			case GLFW.GLFW_KEY_ENTER: {
				handled = true;
				onEnterPressed();
			}
			break;
			case GLFW.GLFW_KEY_BACKSPACE: {
				handled = true;
				handleBackspaceAction();
			}
			break;
			case GLFW.GLFW_KEY_DELETE: {
				handled = true;
				handleDeleteAction();
			}
			break;
			case GLFW.GLFW_KEY_HOME: {
				handled = true;
				cursorIndex = 0;
			}
			break;
			case GLFW.GLFW_KEY_END: {
				handled = true;
				cursorIndex = text.length();
			}
			break;
			case GLFW.GLFW_KEY_ESCAPE: {
				this.selected = false;
				this.selector.onWidgetDeselected(this);
			}
			break;
		}
		if (!handled) {
			handleCharacter(ch);
		}
	}

	/*
	 * Anonymous classes should override this to handle enter-key pressed
	 * inner classes can also override this for default enter-key pressed action
	 */
	protected void onEnterPressed() {

	}

	protected void handleDeleteAction() {
		if (cursorIndex < text.length()) {
			String newText = "";
			for (int i = 0; i < text.length(); i++) {
				if (i == cursorIndex) {
					continue;
				}
				newText = newText + text.charAt(i);
			}
			String oldText = text;
			text = newText;
			onTextUpdated(oldText, newText);
		}
	}

	protected void handleBackspaceAction() {
		if (cursorIndex > 0) {
			String newText = "";
			for (int i = 0; i < text.length(); i++) {
				if (i == cursorIndex - 1) {
					continue;
				}
				newText = newText + text.charAt(i);
			}
			cursorIndex--;
			String oldText = text;
			setText(newText);
			onTextUpdated(oldText, newText);

		}
	}

	protected void handleCharacter(char ch) {
		if (isAllowedCharacter(ch))//is allowed character
		{
			String newText = "";
			for (int i = 0; i <= text.length(); i++) {
				if (i == cursorIndex) {
					newText = newText + ch;
				}
				if (i < text.length()) {
					newText = newText + text.charAt(i);
				}
			}
			String oldText = text;
			text = newText;
			onTextUpdated(oldText, newText);
			cursorIndex++;
		}
	}

	protected static char[] allowedChars = new char[] {'a',
			'b',
			'c',
			'd',
			'e',
			'f',
			'g',
			'h',
			'i',
			'j',
			'k',
			'l',
			'm',
			'n',
			'o',
			'p',
			'q',
			'r',
			's',
			't',
			'u',
			'v',
			'w',
			'x',
			'y',
			'z',
			'A',
			'B',
			'C',
			'D',
			'E',
			'F',
			'G',
			'H',
			'I',
			'J',
			'K',
			'L',
			'M',
			'N',
			'O',
			'P',
			'Q',
			'R',
			'S',
			'T',
			'U',
			'V',
			'W',
			'X',
			'Y',
			'Z',};

	protected static char[] allowedCharSymbols = new char[] {' ',
			'!',
			'#',
			'$',
			'%',
			'^',
			'&',
			'*',
			'(',
			')',
			'_',
			'-',
			'+',
			'=',
			'{',
			'}',
			'[',
			']',
			':',
			';',
			'"',
			'\'',
			',',
			'<',
			'.',
			'>',
			'/',
			'?',
			'@'};

	protected static char[] allowedNums = new char[] {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};
	protected static char[] allowedNumSymbols = new char[] {'.', '-'};

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		//lt-grey, for outline box
		RenderTools.renderColoredQuad(graphics, renderX, renderY, renderX + width, renderY + height, 0.9f, 0.9f, 0.9f, 1.f);

		//black, for the input box
		RenderTools.renderColoredQuad(graphics, renderX + 1, renderY + 1, renderX + width - 1, renderY + height - 1, 0.0f, 0.0f, 0.0f, 1.f);

		graphics.drawString(Minecraft.getInstance().font, text, renderX + 2, renderY + 2, enabled ? 0xffffffff : 0xA9A9A9ff, false);

		if (selected) {
			int w = 0;
			for (int i = 0; i < cursorIndex && i < text.length(); i++) {
				w += Minecraft.getInstance().font.width(String.valueOf(text.charAt(i)));
			}
			graphics.drawString(Minecraft.getInstance().font, "_", renderX + 2 + w, renderY + 3, 0xffff0000, false);
		}

		graphics.setColor(1.0f, 1.0f, 1.0f, 1.f);
	}

	public void setText(String text) {
		if (text == null) {
			text = "";
		}
		String in = text;
		text = "";
		for (int i = 0; i < in.length(); i++) {
			if (isAllowedCharacter(in.charAt(i))) {
				text = text + in.charAt(i);
			}
		}
		this.text = text;
		if (cursorIndex > text.length()) {
			cursorIndex = text.length();
		}
	}

	public void addAllowedChar(char ch) {
		this.textValidator.addValidChar(ch);
	}

	protected boolean isAllowedCharacter(char ch) {
		return this.textValidator.isCharValid(ch);
	}

	public String getText() {
		return text;
	}

	public static class TextValidator {

		Set<Character> validChars = new HashSet<>();

		public TextValidator() {
		}

		public TextValidator(char[] chars) {
			for (char ch : chars) {
				validChars.add(ch);
			}
		}

		public TextValidator(Set<Character> chars) {
			validChars.addAll(chars);
		}

		public void addValidChar(char ch) {
			validChars.add(ch);
		}

		public void addValidChars(char[] chars) {
			for (char ch : chars) {
				validChars.add(ch);
			}
		}

		public boolean isCharValid(char ch) {
			return validChars.contains(ch);
		}

		public void removeValidChar(char ch) {
			validChars.remove(ch);
		}

		public void clearAllowedChars() {
			validChars.clear();
		}
	}

}
