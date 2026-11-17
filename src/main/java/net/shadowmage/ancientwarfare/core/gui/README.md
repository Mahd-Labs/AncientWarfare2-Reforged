# core.gui - porting a 1.12 AW screen

All screens extend `GuiContainerBase<T extends ContainerBase>` (now an `AbstractContainerScreen<ContainerBase>`) and
are built from the element classes in `core.gui.elements`. Constructors, element classes, listener types and the
`initElements()` / `setupElements()` / `refreshGui()` / `handlePacketData(CompoundTag)` / `onGuiCloseRequested()` /
`closeGui()` life cycle are unchanged. A screen ports line by line with these substitutions.

## Screen (GuiContainerBase)

| 1.12 | port |
|---|---|
| `xSize`, `ySize` | `imageWidth`, `imageHeight` |
| `guiLeft`, `guiTop` | `leftPos`, `topPos` |
| `mc`, `fontRenderer` | `minecraft`, `font` (or `Minecraft.getInstance()`) |
| `inventorySlots` | `menu` / `getContainer()` |
| `Minecraft.getMinecraft().displayGuiScreen(gui)` | `Minecraft.getInstance().setScreen(gui)` |
| `initGui()` | `init()` (protected) |
| `updateScreen()` | `containerTick()` (call `super.containerTick()`; it runs the pending `refreshGui`) |
| `drawScreen(mouseX, mouseY, partialTicks)` | `render(GuiGraphics, mouseX, mouseY, partialTicks)` |
| `drawGuiContainerBackgroundLayer(f, x, y)` | `renderBg(GuiGraphics, partialTick, mouseX, mouseY)` |
| `drawGuiContainerForegroundLayer(x, y)` | `renderLabels(GuiGraphics, mouseX, mouseY)` (pose is translated to `leftPos/topPos`; the base class draws the elements here) |
| `onGuiClosed()` | `removed()` |
| `setTexture("ancientwarfare:textures/gui/x.png")` | same, lower case path |
| `NetworkHandler.registerGui(GUI_X, GuiX.class)` | `NetworkHandlerClient.registerGui(NetworkHandler.GUI_X, GuiX::new)` in the module client init |
| `pushViewport(x, y, w, h)` / `popViewport()` | `pushViewport(graphics, x, y, w, h)` / `popViewport(graphics)` (GuiGraphics scissor stack) |

Raw screens (not typed to a container class) such as `GuiSelectFromList` / `GuiFileSelect` still take the parent
`GuiContainerBase` and reuse its container.

Slots: 1.12 `container.removeSlots()` moved slots off screen. Slot positions are final now; while
`ContainerBase.areSlotsHidden()` is true the base screen draws no slot, no slot background and treats no slot as
hovered or clickable. Nothing to change in a ported screen.

## Elements

The only signature change that touches every element:

```java
public void render(int mouseX, int mouseY, float partialTick)              // 1.12
public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)   // port
```

Also: `Tooltip.renderTooltip(graphics, mouseX, mouseY, partialTick)`, `ItemSlot.renderStackSize(graphics, x, y, count, font)`,
`Composite.setViewport(graphics)` / `popViewport(graphics)`. Everything else (constructors, `addNewListener`,
`onPressed`, `onToggled`, `onTextUpdated`, `onValueUpdated`, `onSlotClicked`, `setTooltipIfFound`, ...) is as in 1.12.

Drawing inside a custom element / screen:

| 1.12 | port |
|---|---|
| `mc.renderEngine.bindTexture(tex); RenderTools.renderQuarteredTexture(a...)` | `RenderTools.renderQuarteredTexture(graphics, tex, a...)` |
| `bindTexture(tex); RenderTools.renderTexturedQuad(x1, y1, x2, y2, u1, v1, u2, v2)` | `RenderTools.renderTexturedQuad(graphics, tex, x1, y1, x2, y2, u1, v1, u2, v2)` |
| `bindTexture(tex); drawTexturedModalRect(x, y, u, v, w, h)` | `graphics.blit(tex, x, y, u, v, w, h)` (256x256 texture; other sizes: the blit overload with texture size) |
| `RenderTools.renderColoredQuad(x, y, w, h, r, g, b)` / inline `glBegin(GL_QUADS)` colour quads | `RenderTools.renderColoredQuad(graphics, ...)` (also a float corner + alpha overload) or `graphics.fill(x1, y1, x2, y2, argb)` |
| `GlStateManager.color(r, g, b, a)` before a textured draw | `graphics.setColor(r, g, b, a)` (reset to 1,1,1,1 afterwards) |
| `GlStateManager.pushMatrix / translate / scale / popMatrix` | `graphics.pose().pushPose() / translate / scale / popPose()` |
| `fontRenderer.drawString(s, x, y, c)` / `drawStringWithShadow` | `graphics.drawString(font, s, x, y, c, false)` / `(..., true)` |
| `fontRenderer.drawSplitString(s, x, y, w, c)` | `graphics.drawWordWrap(font, FormattedText.of(s), x, y, w, c)` |
| `fontRenderer.getStringWidth(s)` | `font.width(s)` |
| `I18n.format(key, args)` | `net.minecraft.client.resources.language.I18n.get(key, args)` |
| `itemRender.renderItemAndEffectIntoGUI(stack, x, y)` | `graphics.renderItem(stack, x, y)` |
| `itemRender.renderItemOverlayIntoGUI(font, stack, x, y, text)` | `graphics.renderItemDecorations(font, stack, x, y, text)` |
| `ClientProxy.getUnicodeFontRenderer().xxx(...)` | `UnicodeFont.xxx(graphics, ...)` (`drawString`, `drawSplitString`, `getStringWidth`, `getWordWrappedHeight`, `FONT_HEIGHT`) |
| `player.inventory.getItemStack()` (cursor stack) | `player.containerMenu.getCarried()` |
| button click sound | `GuiElement.playClickSound()` (protected static) |
| `RenderHelper.*`, `GlStateManager.enable/disable*`, `GL11.*` | delete; GuiGraphics manages state. Things that must be above item models draw at a higher z (`pose().translate(0, 0, 200)`) or with `RenderType.guiOverlay()` |

Depth: item models are drawn at z 150 and write depth. 1.12 code that disabled the depth test to draw over items
(slot highlights, tooltips, stack counts) uses z offsets here: `Tooltip` renders at z 400, stack counts at z 200,
slot highlights use the overlay render type.

Colours: text colours are passed through unchanged (`0xffffffff`, the hover grey `0xa0a0a0ff`, ...). `Line` and
`Rectangle` take RGBA like 1.12. 1.12 quirk kept: `ImageButton` / the `java.awt.Color` overloads of `RenderTools`
passed 0-255 ints as float colour components, which OpenGL clamped, so every non zero channel is full intensity.

## Input

`handleMouseInput` / `handleKeyboardInput` are replaced by the 1.21.1 callbacks in the base class; elements still
receive one `ActivationEvent` per input event through their `Listener`s (`Listener.MOUSE_DOWN`, `MOUSE_UP`,
`MOUSE_MOVED`, `MOUSE_WHEEL`, `KEY_DOWN`, `KEY_UP`), vanilla slot handling runs first, as before.

- `evt.mx`, `evt.my`, `evt.mButton` (0 left, 1 right, -1 none), `evt.state`: unchanged.
- `evt.mw`: wheel delta in the LWJGL2 scale the widgets were written for (120 per notch, sign = direction).
- `evt.key`: **GLFW key code** (`GLFW.GLFW_KEY_ENTER`...), not an LWJGL2 `Keyboard.KEY_*` code.
- Characters: 1.12 delivered key and character in one event. Now a key press is a `KEY_DOWN` event with `evt.ch == 0`
  and a typed character is its own `KEY_DOWN` event with `evt.key == GLFW.GLFW_KEY_UNKNOWN` and `evt.ch` set.
  `Text.handleKeyInput(keyCode, ch)` is called once for each. Code that compares `keyCode` against constants only
  needs the constants renamed; code that reads `ch` keeps working.
- A screen that overrode `keyTyped(char, int)` overrides `keyPressed(int keyCode, int scanCode, int modifiers)` /
  `charTyped(char, int)` and calls `super`; `mouseClicked(int, int, int)` becomes
  `mouseClicked(double mouseX, double mouseY, int button)` returning boolean.
- Escape / the inventory key call `closeGui()` when `shouldCloseOnVanillaKeys` is true and no text widget is
  selected (1.12 behaviour, including that Escape in a selected text field deselects it and closes the screen).

## Not verifiable on the build VM

Nothing here was looked at in a running game: layout, colours, z ordering, text field editing, scrolling and
tooltip placement need a pass with a real client.
