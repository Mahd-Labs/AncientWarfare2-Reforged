package net.shadowmage.ancientwarfare.showcase;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPresets;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.util.ShowcaseHooks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/*
 * Dev-only visual check harness (port addition, not part of the 1.12 mod). Client only.
 * Enabled with the JVM property -Daw.showcase=true (gradle run "showcase"). It creates a void world, then for every
 * ancientwarfare block, item and entity type: puts it on a small stage, moves the (spectator) camera to a fixed
 * vantage point, waits for the frame to settle and writes <gameDir>/showcase/<kind>__<name>.png. When done it writes
 * showcase/DONE and quits the client. Subjects that throw are recorded in showcase/failures.txt and skipped.
 *
 * Variants: blocks get a second angle ("~back") and one picture per non-default property value ("@prop=value");
 * items get every creative-tab stack that carries extra components ("@n"); entities get what modules registered
 * with ShowcaseHooks.registerEntityVariant ("@name"). -Daw.showcase.novariants=true turns variants off.
 * Properties: aw.showcase.filter=<regex on "kind/name">, aw.showcase.namespace (default ancientwarfare),
 * aw.showcase.settle=<ticks, default 12>, aw.showcase.frames=<frames, default 4>
 */
public final class ShowcaseHarness {
	private ShowcaseHarness() {}

	private enum Phase {WAIT_TITLE, WAIT_WORLD, WARMUP, STAGE, SETTLE, GUIS, DONE}

	/*
	 * variant: null for the default subject, else a short name ("facing=east", "3", "archer").
	 * payload: BlockState (block), ItemStack (item), CompoundTag merged into the entity (entity) or null.
	 * back: second camera angle from the opposite corner (blocks only), file suffix "~back".
	 */
	private record Subject(String kind, ResourceLocation id, String variant, Object payload, boolean back) {
		String fileName() {
			return kind + "__" + id.getPath().replace('/', '.') + (variant == null ? "" : "@" + variant.replaceAll("[^A-Za-z0-9_.=+-]", "_")) + (back ? "~back" : "") + ".png";
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> BlockState withValue(BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property, Comparable<?> value) {
		return state.setValue(property, (T) value);
	}

	private static final int STAGE_X = 0;
	private static final int STAGE_Y = 100;
	private static final int STAGE_Z = 0;

	private static Phase phase = Phase.WAIT_TITLE;
	private static final List<Subject> subjects = new ArrayList<>();
	private static int index = -1;
	private static int ticks = 0;
	private static int frames = 0;
	private static int titleTicks = 0;
	private static File outDir;
	private static final List<String> failures = new ArrayList<>();
	private static Entity stagedEntity;
	private static int settleTicks = 12;
	private static int settleFrames = 4;

	/*
	 * Called by name from core.util.ShowcaseHooks.startHarness(): no class outside this package may refer to this one,
	 * release jars are built without the package.
	 */
	public static void init() {
		settleTicks = Integer.getInteger("aw.showcase.settle", 12);
		settleFrames = Integer.getInteger("aw.showcase.frames", 4);
		NeoForge.EVENT_BUS.addListener(ShowcaseHarness::onClientTick);
		NeoForge.EVENT_BUS.addListener(ShowcaseHarness::onFrame);
		AncientWarfare.LOG.info("[showcase] harness enabled");
	}

	private static void onFrame(RenderFrameEvent.Post event) {
		frames++;
	}

	private static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		try {
			switch (phase) {
				case WAIT_TITLE -> waitTitle(mc);
				case WAIT_WORLD -> waitWorld(mc);
				case WARMUP -> {
					if (++ticks > 60) {
						next(mc);
					}
				}
				case STAGE -> {
					// the server thread stages the subject; give it a tick, then let the client settle
					// smoke and flames of the previous subject would drift through the next picture (as the 1.12 harness)
					mc.particleEngine.setLevel(mc.level);
					ticks = 0;
					frames = 0;
					phase = Phase.SETTLE;
				}
				case SETTLE -> {
					if (ticks == 3) {
						mc.particleEngine.setLevel(mc.level); // the old block may still have emitted some before the client saw the change
					}
					if (++ticks >= settleTicks && frames >= settleFrames) {
						capture(mc);
						next(mc);
					}
				}
				case GUIS -> {
					//gui pass (ShowcaseGuis), after the block / item / entity subjects
					if (ShowcaseGuis.tick(mc)) {
						finish(mc);
					}
				}
				case DONE -> {
				}
			}
		}
		catch (Exception e) {
			AncientWarfare.LOG.error("[showcase] harness error", e);
			failures.add("harness: " + e);
			finish(mc);
		}
	}

	private static void waitTitle(Minecraft mc) {
		Screen screen = mc.screen;
		if (mc.level != null || !(screen instanceof TitleScreen || screen instanceof AccessibilityOnboardingScreen)) {
			return;
		}
		if (++titleTicks < 20) {
			return;
		}
		mc.options.onboardAccessibility = false;
		mc.options.hideGui = true;
		mc.options.pauseOnLostFocus = false;
		mc.options.fov().set(70);
		mc.options.renderDistance().set(4);
		mc.options.save();
		outDir = new File(mc.gameDirectory, "showcase");
		outDir.mkdirs();

		String name = "aw_showcase_" + System.currentTimeMillis();
		GameRules rules = new GameRules();
		rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
		rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
		rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
		rules.getRule(GameRules.RULE_DOFIRETICK).set(false, null);
		rules.getRule(GameRules.RULE_RANDOMTICKING).set(0, null);
		LevelSettings settings = new LevelSettings(name, GameType.SPECTATOR, false, Difficulty.NORMAL, true, rules, WorldDataConfiguration.DEFAULT);
		Function<RegistryAccess, WorldDimensions> dims = ra -> {
			WorldDimensions base = ra.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions();
			FlatLevelGeneratorSettings voidSettings = ra.registryOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET).getHolderOrThrow(FlatLevelGeneratorPresets.THE_VOID).value().settings();
			return base.replaceOverworldGenerator(ra, new FlatLevelSource(voidSettings));
		};
		phase = Phase.WAIT_WORLD;
		mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(0L, false, false), dims, screen);
	}

	private static void waitWorld(Minecraft mc) {
		IntegratedServer server = mc.getSingleplayerServer();
		if (mc.level == null || mc.player == null || server == null || mc.screen != null && !(mc.screen.getClass().getSimpleName().contains("Chat"))) {
			if (mc.screen != null && mc.level != null && mc.player != null) {
				mc.setScreen(null);
			}
			return;
		}
		collectSubjects();
		AncientWarfare.LOG.info("[showcase] world ready, {} subjects", subjects.size());
		server.execute(() -> {
			ServerLevel level = server.overworld();
			level.setDayTime(6000);
			level.setWeatherParameters(100000, 0, false, false);
		});
		ticks = 0;
		phase = Phase.WARMUP;
	}

	private static void collectSubjects() {
		String ns = System.getProperty("aw.showcase.namespace", AncientWarfare.MOD_ID);
		String filter = System.getProperty("aw.showcase.filter", "");
		Pattern pattern = filter.isEmpty() ? null : Pattern.compile(filter);
		List<Subject> all = new ArrayList<>();
		boolean variants = !Boolean.getBoolean("aw.showcase.novariants");
		for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
			if (!id.getNamespace().equals(ns)) {
				continue;
			}
			Block block = BuiltInRegistries.BLOCK.get(id);
			BlockState def = block.defaultBlockState();
			all.add(new Subject("block", id, null, def, false));
			all.add(new Subject("block", id, null, def, true));
			if (variants) {
				// one picture per non-default value of each property (other properties stay default)
				int n = 0;
				for (net.minecraft.world.level.block.state.properties.Property<?> property : def.getProperties()) {
					for (Comparable<?> value : property.getPossibleValues()) {
						if (value.equals(def.getValue(property)) || n >= 40) {
							continue;
						}
						n++;
						all.add(new Subject("block", id, property.getName() + "=" + value.toString().toLowerCase(java.util.Locale.ROOT), withValue(def, property, value), false));
					}
				}
			}
		}
		java.util.Map<Item, Integer> stackCounts = new java.util.HashMap<>();
		for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
			if (id.getNamespace().equals(ns)) {
				all.add(new Subject("item", id, null, new ItemStack(BuiltInRegistries.ITEM.get(id)), false));
			}
		}
		if (variants) {
			// stacks with extra data shown in creative tabs (the 1.12 harness shot getSubItems variants)
			Minecraft mc = Minecraft.getInstance();
			try {
				net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(), true, mc.level.registryAccess());
				for (net.minecraft.world.item.CreativeModeTab tab : net.minecraft.world.item.CreativeModeTabs.allTabs()) {
					for (ItemStack stack : tab.getDisplayItems()) {
						ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
						if (!id.getNamespace().equals(ns) || stack.getComponentsPatch().isEmpty()) {
							continue;
						}
						boolean dup = false;
						for (Subject s : all) {
							if (s.kind().equals("item") && s.id().equals(id) && s.payload() instanceof ItemStack other && ItemStack.isSameItemSameComponents(other, stack)) {
								dup = true;
								break;
							}
						}
						if (!dup) {
							int n = stackCounts.merge(stack.getItem(), 1, Integer::sum);
							all.add(new Subject("item", id, String.valueOf(n), stack.copy(), false));
						}
					}
				}
			}
			catch (Exception e) {
				AncientWarfare.LOG.error("[showcase] could not read creative tab stacks", e);
				failures.add("creative tabs: " + e);
			}
		}
		for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
			if (id.getNamespace().equals(ns)) {
				all.add(new Subject("entity", id, null, null, false));
				if (variants) {
					for (ShowcaseHooks.EntityVariant v : ShowcaseHooks.getEntityVariants(id)) {
						all.add(new Subject("entity", id, v.name(), v.nbt(), false));
					}
				}
			}
		}
		all.sort(Comparator.comparing(Subject::kind).thenComparing((Subject s) -> s.id().getPath()).thenComparing(s -> s.variant() == null ? "" : s.variant()).thenComparing(Subject::back));
		for (Subject s : all) {
			if (pattern == null || pattern.matcher(s.kind() + "/" + s.id().getPath()).find()) {
				subjects.add(s);
			}
		}
	}

	private static void next(Minecraft mc) {
		index++;
		if (index >= subjects.size()) {
			if (ShowcaseGuis.start(mc, outDir, failures)) {
				phase = Phase.GUIS;
			} else {
				finish(mc);
			}
			return;
		}
		Subject subject = subjects.get(index);
		IntegratedServer server = mc.getSingleplayerServer();
		if (server == null) {
			failures.add("server gone");
			finish(mc);
			return;
		}
		phase = Phase.STAGE;
		server.execute(() -> {
			try {
				stage(server, subject);
			}
			catch (Throwable t) {
				AncientWarfare.LOG.error("[showcase] failed to stage {}", subject, t);
				synchronized (failures) {
					failures.add(subject.fileName() + ": " + t);
				}
			}
		});
	}

	private static void stage(IntegratedServer server, Subject subject) {
		ServerLevel level = server.overworld();
		// clear previous
		if (stagedEntity != null) {
			stagedEntity.discard();
			stagedEntity = null;
		}
		for (Entity e : level.getEntities((Entity) null, new AABB(STAGE_X - 12, STAGE_Y - 4, STAGE_Z - 12, STAGE_X + 13, STAGE_Y + 16, STAGE_Z + 13), e -> !(e instanceof Player))) {
			e.discard();
		}
		for (BlockPos pos : BlockPos.betweenClosed(STAGE_X - 6, STAGE_Y, STAGE_Z - 6, STAGE_X + 6, STAGE_Y + 8, STAGE_Z + 6)) {
			if (!level.getBlockState(pos).isAir()) {
				level.removeBlockEntity(pos);
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
			}
		}
		for (BlockPos pos : BlockPos.betweenClosed(STAGE_X - 6, STAGE_Y - 1, STAGE_Z - 6, STAGE_X + 6, STAGE_Y - 1, STAGE_Z + 6)) {
			boolean dark = ((pos.getX() + pos.getZ()) & 1) == 0;
			BlockState floor = dark ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState();
			if (level.getBlockState(pos) != floor) {
				level.setBlock(pos, floor, 2);
			}
		}
		BlockPos center = new BlockPos(STAGE_X, STAGE_Y, STAGE_Z);
		double size = 1.0;
		double height = 1.0;
		switch (subject.kind()) {
			case "block" -> {
				level.setBlock(center, (BlockState) subject.payload(), 3);
			}
			case "item" -> {
				ItemStack shown = (ItemStack) subject.payload();
				Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
				if (display != null) {
					display.moveTo(STAGE_X + 0.5, STAGE_Y + 0.75, STAGE_Z + 0.5, 0, 0);
					net.minecraft.nbt.CompoundTag tag = display.saveWithoutId(new net.minecraft.nbt.CompoundTag());
					tag.put("item", shown.save(level.registryAccess()));
					// face the camera: a billboarded display shows the stack's back (mirrored) unless turned 180 degrees about Y
					net.minecraft.nbt.CompoundTag tr = new net.minecraft.nbt.CompoundTag();
					tr.put("translation", floats(0F, 0F, 0F));
					tr.put("left_rotation", floats(0F, 1F, 0F, 0F));
					tr.put("scale", floats(1F, 1F, 1F));
					tr.put("right_rotation", floats(0F, 0F, 0F, 1F));
					tag.put("transformation", tr);
					tag.putString("billboard", "center");
					display.load(tag);
					level.addFreshEntity(display);
					stagedEntity = display;
				}
			}
			case "entity" -> {
				EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(subject.id());
				Entity entity = type.create(level);
				if (entity == null) {
					throw new IllegalStateException("EntityType.create returned null");
				}
				if (subject.payload() instanceof net.minecraft.nbt.CompoundTag extra) {
					net.minecraft.nbt.CompoundTag merged = entity.saveWithoutId(new net.minecraft.nbt.CompoundTag());
					merged.merge(extra);
					entity.load(merged);
				}
				entity.moveTo(STAGE_X + 0.5, STAGE_Y, STAGE_Z + 0.5, 0F, 0);
				entity.setYHeadRot(0F);
				entity.setYBodyRot(0F);
				if (entity instanceof Mob mob) {
					mob.setNoAi(true);
					mob.setPersistenceRequired();
				}
				entity.setSilent(true);
				level.addFreshEntity(entity);
				stagedEntity = entity;
				size = Math.max(1.0, Math.max(entity.getBbWidth(), entity.getBbHeight()));
				height = Math.max(1.0, entity.getBbHeight());
			}
			default -> {
			}
		}
		// camera: south-east of the stage looking north-west and down, distance scaled by subject size
		double dist = 1.0 + size * 1.5;
		double side = subject.back() ? -1 : 1;
		double cx = STAGE_X + 0.5 + side * dist * 0.7071;
		double cz = STAGE_Z + 0.5 + side * dist * 0.7071;
		double eyeY = STAGE_Y + height * 0.5 + dist * 0.45;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.setGameMode(GameType.SPECTATOR);
			float pitch = (float) Math.toDegrees(Math.atan2(eyeY - (STAGE_Y + height * 0.5), dist));
			player.teleportTo(level, cx, eyeY - player.getEyeHeight(), cz, subject.back() ? -45F : 135F, pitch);
		}
	}

	private static net.minecraft.nbt.ListTag floats(float... values) {
		net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
		for (float v : values) {
			list.add(net.minecraft.nbt.FloatTag.valueOf(v));
		}
		return list;
	}

	private static void capture(Minecraft mc) {
		Subject subject = subjects.get(index);
		File file = new File(outDir, subject.fileName());
		try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
			image.writeToFile(file);
		}
		catch (IOException e) {
			failures.add(subject.kind() + "/" + subject.id() + ": screenshot " + e);
		}
		if ((index + 1) % 25 == 0) {
			AncientWarfare.LOG.info("[showcase] {}/{}", index + 1, subjects.size());
		}
	}

	private static void finish(Minecraft mc) {
		phase = Phase.DONE;
		if (outDir == null) {
			outDir = new File(mc.gameDirectory, "showcase");
			outDir.mkdirs();
		}
		try (FileWriter fw = new FileWriter(new File(outDir, "failures.txt"))) {
			synchronized (failures) {
				for (String f : failures) {
					fw.write(f + "\n");
				}
			}
		}
		catch (IOException ignored) {
		}
		try (FileWriter fw = new FileWriter(new File(outDir, "DONE"))) {
			fw.write((subjects.size() + ShowcaseGuis.count()) + " subjects, " + failures.size() + " failures\n");
		}
		catch (IOException ignored) {
		}
		AncientWarfare.LOG.info("[showcase] finished: {} subjects, {} failures", subjects.size(), failures.size());
		mc.stop();
	}
}
