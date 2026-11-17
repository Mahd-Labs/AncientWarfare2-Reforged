package net.shadowmage.ancientwarfare.npc.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.shadowmage.ancientwarfare.core.gui.GuiContainerBase;
import net.shadowmage.ancientwarfare.core.gui.GuiSelectFromList;
import net.shadowmage.ancientwarfare.core.gui.elements.Button;
import net.shadowmage.ancientwarfare.core.gui.elements.Checkbox;
import net.shadowmage.ancientwarfare.core.gui.elements.Text;
import net.shadowmage.ancientwarfare.core.util.RenderTools;
import net.shadowmage.ancientwarfare.npc.container.ContainerNpcBase;
import net.shadowmage.ancientwarfare.npc.container.ISkinSettingsContainer;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.skin.NpcSkinManager;
import net.shadowmage.ancientwarfare.npc.skin.NpcSkinSettings;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.stream.Collectors;

public class GuiSkinSelection extends GuiContainerBase {
	private final GuiContainerBase<? extends ContainerNpcBase<NpcBase>> parent;
	private Checkbox defaultType;
	private Checkbox playerType;
	private Checkbox npcType;
	private NpcSkinSettings skinSettings;
	private ISkinSettingsContainer skinSettingsContainer;
	private int previewTop = 0;

	private static final int SUBCONTROLS_INDENT = 24;
	private int mouseX;
	private int mouseY;

	private int playerSkinUpdateCooldown = 0;
	private boolean updatePlayerSkin = false;

	public GuiSkinSelection(GuiContainerBase<? extends ContainerNpcBase<NpcBase>> parent, ISkinSettingsContainer skinSettingsContainer) {
		//1.12 ran this screen on a throw-away slot-less ContainerBase. A container cannot be created outside of the menu registry
		//in 1.21.1, so it runs on the parent container with its slots hidden (restored when returning to the parent).
		super(parent.getContainer(), 200, 240);
		parent.getContainer().removeSlots();
		this.parent = parent;
		this.skinSettings = skinSettingsContainer.getSkinSettings();
		this.skinSettingsContainer = skinSettingsContainer;
	}

	@Override
	public void setupElements() {
		clearElements();

		imageHeight = calculateYSize();
		topPos = (this.height - this.imageHeight) / 2;

		int startHeight = 8;
		defaultType = new Checkbox(8, startHeight, 12, 12, "gui.ancientwarfarenpc.skin_selection.default") {
			@Override
			public void onToggled() {
				super.onToggled();
				if (checked()) {
					skinSettings.setSkinType(NpcSkinSettings.SkinType.DEFAULT);
					playerType.setChecked(false);
					npcType.setChecked(false);
					refreshGui();
				} else {
					setChecked(true);
				}
			}
		};
		defaultType.setChecked(skinSettings.getSkinType() == NpcSkinSettings.SkinType.DEFAULT);
		addGuiElement(defaultType);
		startHeight += 16;

		playerType = new Checkbox(8, startHeight, 12, 12, "gui.ancientwarfarenpc.skin_selection.player") {
			@Override
			public void onToggled() {
				super.onToggled();
				if (checked()) {
					skinSettings.setSkinType(NpcSkinSettings.SkinType.PLAYER);
					defaultType.setChecked(false);
					npcType.setChecked(false);
					refreshGui();
				} else {
					setChecked(true);
				}
			}
		};
		playerType.setChecked(skinSettings.getSkinType() == NpcSkinSettings.SkinType.PLAYER);
		addGuiElement(playerType);
		startHeight += 16;

		if (playerType.checked()) {
			startHeight = addPlayerElements(startHeight);
		}

		npcType = new Checkbox(8, startHeight, 12, 12, "gui.ancientwarfarenpc.skin_selection.npc_type") {
			@Override
			public void onToggled() {
				super.onToggled();
				if (checked()) {
					skinSettings.setSkinType(NpcSkinSettings.SkinType.NPC_TYPE);
					playerType.setChecked(false);
					defaultType.setChecked(false);
					refreshGui();
				} else {
					setChecked(true);
				}
			}
		};
		npcType.setChecked(skinSettings.getSkinType() == NpcSkinSettings.SkinType.NPC_TYPE);
		addGuiElement(npcType);
		startHeight += 16;

		if (npcType.checked()) {
			startHeight = addNpcTypeElements(startHeight);
		}

		if (!defaultType.checked()) {
			Checkbox alex = new Checkbox(8, startHeight, 16, 16, "gui.ancientwarfarenpc.skin_selection.alex_model") {
				@Override
				public void onToggled() {
					super.onToggled();
					skinSettings.setAlexModel(checked());
					refreshGui();
				}
			};
			alex.setChecked(skinSettings.isAlexModel());
			addGuiElement(alex);

			startHeight += 20;
		}
		previewTop = startHeight;
	}

	private int calculateYSize() {
		switch (skinSettings.getSkinType()) {
			case NPC_TYPE:
				return skinSettings.getNpcTypeName().isEmpty() || skinSettings.isRandom() ? 250 : 266;
			case PLAYER:
				return 230;
			case DEFAULT:
			default:
				return 194;
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		super.render(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float var1, int var2, int var3) {
		super.renderBg(graphics, var1, var2, var3);
		int posX = leftPos + imageWidth / 2;
		int posY = topPos + previewTop + 8 + 100;

		int backgroundWidth = imageWidth - 16;
		int backgroundHeigth = 130;
		processPlayerSkinUpdate();
		RenderTools.renderColoredQuad(graphics, posX - backgroundWidth / 2, topPos + previewTop, backgroundWidth, backgroundHeigth, 0, 0, 0);
		drawEntityOnScreen(graphics, posX, posY, 35, posX - mouseX, posY - 50 - mouseY, parent.getContainer().entity);
	}

	private void processPlayerSkinUpdate() {
		if (playerSkinUpdateCooldown > 0) {
			playerSkinUpdateCooldown--;
			return;
		}
		if (!updatePlayerSkin) {
			return;
		}
		updatePlayerSkin = false;
		skinSettingsContainer.handleNpcSkinUpdate();
		playerSkinUpdateCooldown = 40;
	}

	private int addNpcTypeElements(int startHeight) {
		addGuiElement(new Button(SUBCONTROLS_INDENT, startHeight, 150, 12,
				skinSettings.getNpcTypeName().isEmpty() ? "gui.ancientwarfarenpc.skin_selection.select_npc_type" : skinSettings.getNpcTypeName()) {
			@Override
			protected void onPressed() {
				Minecraft.getInstance().setScreen(new GuiSelectFromList<>(GuiSkinSelection.this, skinSettings.getNpcTypeName(), s -> s,
						this::getNpcTypes, s -> {
					setText(s);
					skinSettings.setNpcTypeName(s);
					skinSettings.resetNpcTypeSkin();
					refreshGui();
				}));
			}

			private List<String> getNpcTypes() {
				return NpcSkinManager.getSkinTypes().stream().sorted().collect(Collectors.toList());
			}
		});

		startHeight += 16;

		Checkbox random = new Checkbox(SUBCONTROLS_INDENT, startHeight, 16, 16, "gui.ancientwarfarenpc.skin_selection.random") {
			@Override
			public void onToggled() {
				super.onToggled();
				skinSettings.setRandom(checked());
				refreshGui();
			}
		};
		random.setChecked(skinSettings.isRandom());
		addGuiElement(random);

		startHeight += 20;

		if (!skinSettings.getNpcTypeName().isEmpty() && !skinSettings.isRandom()) {
			addGuiElement(new Button(SUBCONTROLS_INDENT, startHeight, 150, 12,
					skinSettings.getNpcTypeSkin().map(rl -> rl.toString().replace("ancientwarfare:skin_pack/", "").replace(".png", "")).orElse("gui.ancientwarfarenpc.skin_selection.select_npc_skin")) {
				@Override
				protected void onPressed() {
					Minecraft.getInstance().setScreen(new GuiSelectFromList<>(GuiSkinSelection.this,
							skinSettings.getNpcTypeSkin().orElse(null), rl -> rl == null ? "" : rl.toString().replace("ancientwarfare:skin_pack/", "").replace(".png", ""),
							this::getNpcTypes, rl -> {
						setText(rl.toString());
						skinSettings.setNpcTypeSkin(rl);
					}));
				}

				private List<ResourceLocation> getNpcTypes() {
					return NpcSkinManager.getTypeSkins(skinSettings.getNpcTypeName()).stream().sorted().collect(Collectors.toList());
				}
			});
			startHeight += 16;
		}
		return startHeight;
	}

	private int addPlayerElements(int startHeight) {
		addGuiElement(new Text(SUBCONTROLS_INDENT, startHeight, 100, skinSettings.getPlayerName(), this) {
			@Override
			public void onTextUpdated(String oldText, String newText) {
				skinSettings.setPlayerName(newText);
				updatePlayerSkin = true;
			}
		});

		return startHeight + 16;
	}

	/*
	 * 1.12 copy of GuiInventory.drawEntityOnScreen (entity feet at posX / posY). The 1.21.1 equivalent of that vanilla method is
	 * InventoryScreen.renderEntityInInventory, called here with no extra translation so the feet stay at posX / posY.
	 */
	public static void drawEntityOnScreen(GuiGraphics graphics, int posX, int posY, int scale, float mouseX, float mouseY, LivingEntity ent) {
		float angleX = (float) Math.atan((double) (mouseX / 40.0F));
		float angleY = (float) Math.atan((double) (mouseY / 40.0F));
		Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf cameraOrientation = new Quaternionf().rotateX(angleY * 20.0F * (float) (Math.PI / 180.0));
		pose.mul(cameraOrientation);
		float f = ent.yBodyRot;
		float f1 = ent.getYRot();
		float f2 = ent.getXRot();
		float f3 = ent.yHeadRotO;
		float f4 = ent.yHeadRot;
		ent.yBodyRot = 180.0F + angleX * 20.0F;
		ent.setYRot(180.0F + angleX * 40.0F);
		ent.setXRot(-angleY * 20.0F);
		ent.yHeadRot = ent.getYRot();
		ent.yHeadRotO = ent.getYRot();
		InventoryScreen.renderEntityInInventory(graphics, posX, posY, scale, new Vector3f(), pose, cameraOrientation, ent);
		ent.yBodyRot = f;
		ent.setYRot(f1);
		ent.setXRot(f2);
		ent.yHeadRotO = f3;
		ent.yHeadRot = f4;
	}

	@Override
	protected boolean onGuiCloseRequested() {
		skinSettingsContainer.setSkinSettings(skinSettings);
		skinSettingsContainer.handleNpcSkinUpdate();
		parent.getContainer().addSlots();
		parent.refreshGui();
		Minecraft.getInstance().setScreen(parent);
		return false;
	}

	@Override
	public void initElements() {
		//noop
	}
}
