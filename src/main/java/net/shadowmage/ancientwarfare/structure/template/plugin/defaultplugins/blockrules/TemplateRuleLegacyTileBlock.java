package net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules;

import com.google.gson.JsonPrimitive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.api.IStructureBuilder;

import java.util.Map;
import java.util.UUID;

/*
 * Port addition: the 1.12 rules "bed", "vanillaSkull", "banner", "vanillaSign" and "vanillaFlowerPot" of the original
 * templates. 1.12 kept what these blocks look like in the tile entity (bed colour, skull type / rotation / owner,
 * banner colour and patterns, sign text, the flower in the pot); since 1.13 the colour, type and flower are the block
 * and the rest is block entity data in a new format, and the vanilla block state fixer cannot see the tile. The rule
 * reads the 1.12 rule data, builds the 1.21 block state and block entity data, and places them as the 1.12 rules did
 * (skull rotation and standing sign / banner rotation turned with the structure). It is written back as a block tile
 * rule, so templates saved by the port keep it.
 */
public class TemplateRuleLegacyTileBlock extends TemplateRuleBlockTile<BlockEntity> {
	private final String legacyPlugin;

	public TemplateRuleLegacyTileBlock(String legacyPlugin) {
		super();
		this.legacyPlugin = legacyPlugin;
	}

	private static final String[] SKULLS = {"skeleton_skull", "wither_skeleton_skull", "zombie_head", "player_head", "creeper_head", "dragon_head"};
	private static final String[] WALL_SKULLS = {"skeleton_wall_skull", "wither_skeleton_wall_skull", "zombie_wall_head", "player_wall_head", "creeper_wall_head", "dragon_wall_head"};
	private static final Map<String, String> BANNER_PATTERNS = Map.ofEntries(Map.entry("b", "base"), Map.entry("bl", "square_bottom_left"),
			Map.entry("br", "square_bottom_right"), Map.entry("tl", "square_top_left"), Map.entry("tr", "square_top_right"), Map.entry("bs", "stripe_bottom"),
			Map.entry("ts", "stripe_top"), Map.entry("ls", "stripe_left"), Map.entry("rs", "stripe_right"), Map.entry("cs", "stripe_center"),
			Map.entry("ms", "stripe_middle"), Map.entry("drs", "stripe_downright"), Map.entry("dls", "stripe_downleft"), Map.entry("ss", "small_stripes"),
			Map.entry("cr", "cross"), Map.entry("sc", "straight_cross"), Map.entry("bt", "triangle_bottom"), Map.entry("tt", "triangle_top"),
			Map.entry("bts", "triangles_bottom"), Map.entry("tts", "triangles_top"), Map.entry("ld", "diagonal_left"), Map.entry("rd", "diagonal_up_right"),
			Map.entry("lud", "diagonal_up_left"), Map.entry("rud", "diagonal_right"), Map.entry("mc", "circle"), Map.entry("mr", "rhombus"),
			Map.entry("vh", "half_vertical"), Map.entry("hh", "half_horizontal"), Map.entry("vhr", "half_vertical_right"), Map.entry("hhb", "half_horizontal_bottom"),
			Map.entry("bo", "border"), Map.entry("cbo", "curly_border"), Map.entry("gra", "gradient"), Map.entry("gru", "gradient_up"), Map.entry("bri", "bricks"),
			Map.entry("cre", "creeper"), Map.entry("sku", "skull"), Map.entry("flo", "flower"), Map.entry("moj", "mojang"), Map.entry("glb", "globe"),
			Map.entry("pig", "piglin"));

	@Override
	public void parseRule(CompoundTag ruleTag) {
		super.parseRule(ruleTag);
		if (!legacyData) {
			return;
		}
		CompoundTag legacyTile = this.tag == null ? new CompoundTag() : this.tag;
		CompoundTag blockState = ruleTag.getCompound("blockState");
		String legacyName = blockState.getString("blockName");
		CompoundTag legacyProperties = blockState.getCompound("properties");
		CompoundTag tile = new CompoundTag();
		try {
			switch (legacyPlugin) {
				case "bed" -> state = recolour(state, DyeColor.byId(ruleTag.getInt("bedColor") & 15).getName() + "_bed");
				case "banner" -> {
					//1.12 dye damage: 15 = white ... 0 = black
					DyeColor base = DyeColor.byId(15 - (legacyTile.getInt("Base") & 15));
					state = recolour(state, base.getName() + (legacyName.endsWith("wall_banner") ? "_wall_banner" : "_banner"));
					tile.put("patterns", bannerPatterns(legacyTile.getList("Patterns", Tag.TAG_COMPOUND)));
				}
				case "vanillaSign" -> {
					if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
						state = state.setValue(BlockStateProperties.ROTATION_16, ruleTag.getInt("rotation") & 15); //1.12 placed the rule's rotation
					}
					tile.put("front_text", signText(ruleTag));
				}
				case "vanillaSkull" -> {
					int type = Math.max(0, Math.min(SKULLS.length - 1, legacyTile.getByte("SkullType")));
					Direction facing = Direction.byName(legacyProperties.getString("facing"));
					if (facing == null || facing.getAxis() == Direction.Axis.Y) {
						state = block(SKULLS[type]).defaultBlockState().setValue(BlockStateProperties.ROTATION_16, ruleTag.getInt("skullRotation") & 15);
					} else {
						state = block(WALL_SKULLS[type]).defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
					}
					if (legacyTile.contains("Owner", Tag.TAG_COMPOUND)) {
						tile.put("profile", profile(legacyTile.getCompound("Owner")));
					}
				}
				case "vanillaFlowerPot" -> state = flowerPot(ruleTag);
				default -> {
				}
			}
		}
		catch (RuntimeException e) {
			AncientWarfareStructure.LOG.debug("1.12 {} rule data not converted: {}", legacyPlugin, e.toString());
		}
		this.tag = tile;
		this.facing = null;
		markTileDataUpgraded();
		this.buildPass = net.shadowmage.ancientwarfare.structure.registry.StructureBlockRegistry.getBuildPass(state);
	}

	private static Block block(String path) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(path));
	}

	private static BlockState recolour(BlockState state, String path) {
		Block block = block(path);
		return block == Blocks.AIR ? state : block.withPropertiesOf(state);
	}

	private static ListTag bannerPatterns(ListTag legacy) {
		ListTag ret = new ListTag();
		for (int i = 0; i < legacy.size(); i++) {
			CompoundTag entry = legacy.getCompound(i);
			String pattern = BANNER_PATTERNS.get(entry.getString("Pattern"));
			if (pattern != null) {
				CompoundTag layer = new CompoundTag();
				layer.putString("pattern", "minecraft:" + pattern);
				layer.putString("color", DyeColor.byId(15 - (entry.getInt("Color") & 15)).getName());
				ret.add(layer);
			}
		}
		return ret;
	}

	/* 1.12 wrote getFormattedText(): the line with a "§r" after each part; the codes still render the same in 1.21 */
	private static CompoundTag signText(CompoundTag ruleTag) {
		ListTag messages = new ListTag();
		for (int i = 0; i < 4; i++) {
			String line = ruleTag.getString("signContents" + i);
			while (line.endsWith("§r")) {
				line = line.substring(0, line.length() - 2);
			}
			messages.add(StringTag.valueOf(new JsonPrimitive(line).toString()));
		}
		CompoundTag text = new CompoundTag();
		text.put("messages", messages);
		text.putString("color", "black");
		text.putBoolean("has_glowing_text", false);
		return text;
	}

	private static CompoundTag profile(CompoundTag owner) {
		CompoundTag profile = new CompoundTag();
		if (owner.contains("Name", Tag.TAG_STRING) && !owner.getString("Name").isEmpty()) {
			profile.putString("name", owner.getString("Name"));
		}
		if (owner.contains("Id", Tag.TAG_STRING)) {
			UUID id = UUID.fromString(owner.getString("Id"));
			profile.put("id", new IntArrayTag(net.minecraft.core.UUIDUtil.uuidToIntArray(id)));
		}
		ListTag properties = new ListTag();
		CompoundTag legacyProperties = owner.getCompound("Properties");
		for (String name : legacyProperties.getAllKeys()) {
			ListTag values = legacyProperties.getList(name, Tag.TAG_COMPOUND);
			for (int i = 0; i < values.size(); i++) {
				CompoundTag property = new CompoundTag();
				property.putString("name", name);
				property.putString("value", values.getCompound(i).getString("Value"));
				if (values.getCompound(i).contains("Signature", Tag.TAG_STRING)) {
					property.putString("signature", values.getCompound(i).getString("Signature"));
				}
				properties.add(property);
			}
		}
		if (!properties.isEmpty()) {
			profile.put("properties", properties);
		}
		return profile;
	}

	/* 1.12 TemplateRuleFlowerPot: the plant is the rule's item (itemName, itemMeta); 1.21 has a potted block per plant */
	private static BlockState flowerPot(CompoundTag ruleTag) {
		if (!ruleTag.contains("itemName", Tag.TAG_STRING) || ruleTag.getString("itemName").equals("minecraft:air")) {
			return Blocks.FLOWER_POT.defaultBlockState();
		}
		CompoundTag stack = new CompoundTag();
		stack.putString("id", ruleTag.getString("itemName"));
		stack.putByte("Count", (byte) 1);
		stack.putShort("Damage", (short) ruleTag.getInt("itemMeta"));
		ItemStack plant = net.shadowmage.ancientwarfare.structure.util.LegacyItemStacks.read(stack);
		if (plant.isEmpty()) {
			return Blocks.FLOWER_POT.defaultBlockState();
		}
		Block potted = block("potted_" + BuiltInRegistries.ITEM.getKey(plant.getItem()).getPath());
		return potted == Blocks.AIR ? Blocks.FLOWER_POT.defaultBlockState() : potted.defaultBlockState();
	}

	/* 1.12 turned the skull rotation and the standing sign / banner rotation with the structure (a quarter turn = 4) */
	@Override
	public BlockState getState(int turns) {
		BlockState rotated = BlockTools.rotateFacing(state, turns);
		if (rotated.hasProperty(BlockStateProperties.ROTATION_16)) {
			rotated = rotated.setValue(BlockStateProperties.ROTATION_16, (rotated.getValue(BlockStateProperties.ROTATION_16) + 4 * turns) & 15);
		}
		return rotated;
	}

	@Override
	public void handlePlacement(Level world, int turns, BlockPos pos, IStructureBuilder builder) {
		if (!builder.placeBlock(pos, getState(turns), buildPass) || tag == null || tag.isEmpty()) {
			return;
		}
		WorldTools.getTile(world, pos).ifPresent(te -> {
			CompoundTag data = te.saveWithFullMetadata(world.registryAccess());
			data.merge(tag);
			try {
				te.loadWithComponents(data, world.registryAccess());
				te.setChanged();
				world.sendBlockUpdated(pos, getState(turns), getState(turns), 2);
			}
			catch (Exception e) {
				AncientWarfareStructure.LOG.error("Error loading tile entity data from template for {}: ", te.getClass(), e);
			}
		});
	}

	/* 1.12: a bed is paid for by its foot half; a pot with a plant needs the pot and the plant */
	@Override
	public java.util.List<ItemStack> getResources() {
		if (state.hasProperty(BlockStateProperties.BED_PART) && state.getValue(BlockStateProperties.BED_PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD) {
			return java.util.Collections.emptyList();
		}
		if (state.getBlock() instanceof net.minecraft.world.level.block.FlowerPotBlock pot && pot.getPotted() != Blocks.AIR) {
			return java.util.List.of(new ItemStack(Blocks.FLOWER_POT), new ItemStack(pot.getPotted()));
		}
		return super.getResources();
	}

	@Override
	public boolean shouldReuseRule(Level world, BlockState state, int turns, BlockPos pos) {
		return false;
	}
}
