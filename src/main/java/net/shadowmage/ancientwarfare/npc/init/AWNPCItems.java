package net.shadowmage.ancientwarfare.npc.init;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;
import net.shadowmage.ancientwarfare.npc.item.ItemBardInstrument;
import net.shadowmage.ancientwarfare.npc.item.ItemClub;
import net.shadowmage.ancientwarfare.npc.item.ItemCoin;
import net.shadowmage.ancientwarfare.npc.item.ItemCombatOrder;
import net.shadowmage.ancientwarfare.npc.item.ItemCommandBaton;
import net.shadowmage.ancientwarfare.npc.item.ItemExtendedReachWeapon;
import net.shadowmage.ancientwarfare.npc.item.ItemFoodBundle;
import net.shadowmage.ancientwarfare.npc.item.ItemIceSpear;
import net.shadowmage.ancientwarfare.npc.item.ItemMacuahuitl;
import net.shadowmage.ancientwarfare.npc.item.ItemNpcSpawner;
import net.shadowmage.ancientwarfare.npc.item.ItemPitchfork;
import net.shadowmage.ancientwarfare.npc.item.ItemRoutingOrder;
import net.shadowmage.ancientwarfare.npc.item.ItemScythe;
import net.shadowmage.ancientwarfare.npc.item.ItemShield;
import net.shadowmage.ancientwarfare.npc.item.ItemSickle;
import net.shadowmage.ancientwarfare.npc.item.ItemTradeOrder;
import net.shadowmage.ancientwarfare.npc.item.ItemUpkeepOrder;
import net.shadowmage.ancientwarfare.npc.item.ItemWorkOrder;
import net.shadowmage.ancientwarfare.npc.registry.FactionDefinition;
import net.shadowmage.ancientwarfare.npc.registry.FactionRegistry;
import net.shadowmage.ancientwarfare.npc.xmod.NpcXmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/*
 * Port notes: @ObjectHolder fields -> DeferredItem holders (call .get() at use sites); every item keeps its 1.12
 * registry path. Flattened: bard_instrument meta 0-3 -> bard_instrument_lute / _flute / _harp / _drum
 * (BARD_INSTRUMENTS, index = 1.12 meta; BARD_INSTRUMENT = lute). The ore dictionary name "foodBundle" is the item
 * tag ancientwarfare:food_bundle. allItems() lists every item in the 1.12 registration order (creative tab).
 */
@SuppressWarnings("WeakerAccess")
public class AWNPCItems {
	private AWNPCItems() {}

	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AncientWarfareNPC.MOD_ID);
	private static final List<DeferredItem<? extends Item>> ALL_ITEMS = new ArrayList<>();
	public static List<DeferredItem<? extends Item>> allItems() {
		return Collections.unmodifiableList(ALL_ITEMS);
	}

	public static final DeferredItem<ItemCommandBaton> WOODEN_COMMAND_BATON = register("wooden_command_baton", () -> new ItemCommandBaton("wooden_command_baton", Tiers.WOOD));
	public static final DeferredItem<ItemCommandBaton> STONE_COMMAND_BATON = register("stone_command_baton", () -> new ItemCommandBaton("stone_command_baton", Tiers.STONE));
	public static final DeferredItem<ItemCommandBaton> IRON_COMMAND_BATON = register("iron_command_baton", () -> new ItemCommandBaton("iron_command_baton", Tiers.IRON));
	public static final DeferredItem<ItemCommandBaton> GOLD_COMMAND_BATON = register("gold_command_baton", () -> new ItemCommandBaton("gold_command_baton", Tiers.GOLD));
	public static final DeferredItem<ItemCommandBaton> DIAMOND_COMMAND_BATON = register("diamond_command_baton", () -> new ItemCommandBaton("diamond_command_baton", Tiers.DIAMOND));

	public static final List<DeferredItem<ItemBardInstrument>> BARD_INSTRUMENTS;
	public static final DeferredItem<ItemBardInstrument> BARD_INSTRUMENT;

	static {
		List<DeferredItem<ItemBardInstrument>> instruments = new ArrayList<>();
		for (int i = 0; i < ItemBardInstrument.instrumentNames.length; i++) {
			final int meta = i;
			final String name = "bard_instrument_" + ItemBardInstrument.instrumentNames[i];
			instruments.add(register(name, () -> new ItemBardInstrument(name, meta)));
		}
		BARD_INSTRUMENTS = Collections.unmodifiableList(instruments);
		BARD_INSTRUMENT = BARD_INSTRUMENTS.get(0);
	}

	public static final DeferredItem<ItemShield> WOODEN_SHIELD = shield("wooden_shield", Tiers.WOOD, 336);
	public static final DeferredItem<ItemShield> STONE_SHIELD = shield("stone_shield", Tiers.STONE, 506);
	public static final DeferredItem<ItemShield> IRON_SHIELD = shield("iron_shield", Tiers.IRON, 759);
	public static final DeferredItem<ItemShield> GOLD_SHIELD = shield("gold_shield", Tiers.GOLD, 64);
	public static final DeferredItem<ItemShield> DIAMOND_SHIELD = shield("diamond_shield", Tiers.DIAMOND, 1138);
	public static final DeferredItem<ItemShield> SHIELD_TRIBAL_1 = shield("shield_tribal_1", Tiers.WOOD, 336);
	public static final DeferredItem<ItemShield> SHIELD_TRIBAL_2 = shield("shield_tribal_2", Tiers.WOOD, 336);
	public static final DeferredItem<ItemShield> SHIELD_ROUND_1 = shield("shield_round_1", Tiers.WOOD, 336);
	public static final DeferredItem<ItemShield> SHIELD_ROUND_2 = shield("shield_round_2", Tiers.WOOD, 336);
	public static final DeferredItem<ItemShield> SHIELD_ROUND_3 = shield("shield_round_3", Tiers.WOOD, 336);
	public static final DeferredItem<ItemShield> SHIELD_ROUND_4 = shield("shield_round_4", Tiers.WOOD, 336);
	public static final DeferredItem<ItemShield> SHIELD_ROUND_5 = shield("shield_round_5", Tiers.WOOD, 336);
	public static final DeferredItem<ItemShield> SHIELD_ROUND_6 = shield("shield_round_6", Tiers.WOOD, 336);
	public static final DeferredItem<ItemShield> SHIELD_WITCHBANE_1 = shield("shield_witchbane_1", Tiers.IRON, 759);
	public static final DeferredItem<ItemShield> SHIELD_WITCHBANE_2 = shield("shield_witchbane_2", Tiers.IRON, 759);
	public static final DeferredItem<ItemShield> SHIELD_BUFFLOKA = shield("shield_buffloka", Tiers.WOOD, 336);

	public static final DeferredItem<ItemWorkOrder> WORK_ORDER = register("work_order", ItemWorkOrder::new);
	public static final DeferredItem<ItemUpkeepOrder> UPKEEP_ORDER = register("upkeep_order", ItemUpkeepOrder::new);
	public static final DeferredItem<ItemCombatOrder> COMBAT_ORDER = register("combat_order", ItemCombatOrder::new);
	public static final DeferredItem<ItemRoutingOrder> ROUTING_ORDER = register("routing_order", ItemRoutingOrder::new);
	public static final DeferredItem<ItemTradeOrder> TRADE_ORDER = register("trade_order", ItemTradeOrder::new);
	public static final DeferredItem<ItemNpcSpawner> NPC_SPAWNER = register("npc_spawner", ItemNpcSpawner::new);

	public static final DeferredItem<ItemCoin> COIN = register("coin", ItemCoin::new);
	public static final DeferredItem<ItemMacuahuitl> MACUAHUITL = register("macuahuitl", () -> new ItemMacuahuitl(Tiers.IRON, "macuahuitl"));
	public static final DeferredItem<ItemSickle> SICKLE = register("sickle", () -> new ItemSickle(Tiers.IRON, -2.3F));
	public static final DeferredItem<ItemPitchfork> PITCHFORK = register("pitchfork", () -> new ItemPitchfork(Tiers.IRON, -2.3F));
	public static final DeferredItem<ItemScythe> SCYTHE = register("scythe", () -> new ItemScythe(Tiers.IRON, "scythe", 0.0F, -2.3F));
	public static final DeferredItem<ItemScythe> DEATH_SCYTHE = register("death_scythe", () -> new ItemScythe(Tiers.DIAMOND, "death_scythe", 0.0F, -2.3F) {
		@Override
		protected void applyPotionEffect(LivingEntity target) {
			target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100));
		}
	});
	public static final DeferredItem<ItemClub> GIANT_CLUB = register("giant_club", () -> new ItemClub(Tiers.DIAMOND, "giant_club", 3.5, -3.6D, 4.2F));
	public static final DeferredItem<ItemIceSpear> ICE_SPEAR = register("ice_spear", () -> new ItemIceSpear(Tiers.DIAMOND, "ice_spear", 2, -3, 4.2F));

	public static final DeferredItem<ItemFoodBundle> FOOD_BUNDLE = register("food_bundle", ItemFoodBundle::new);

	static {
		registerExtendedReachWeapons("spear", 2, -3, 4.2F);
		registerExtendedReachWeapons("halberd", 3, -3.2D, 4.5F);
		registerExtendedReachWeapons("lance", 2.5D, -3.2D, 5.5F);
		registerExtendedReachWeapons("cleaver", 3.5D, -2.8D, 3.0F);

		registerUniqueExtendedReachWeapon("obsidian_spear", 2, -3, 4.2F);
	}

	private static <T extends Item> DeferredItem<T> register(String name, Supplier<T> supplier) {
		DeferredItem<T> item = ITEMS.register(name, supplier);
		ALL_ITEMS.add(item);
		return item;
	}

	private static DeferredItem<ItemShield> shield(String name, Tier material, int durability) {
		return register(name, () -> new ItemShield(name, material, durability));
	}

	private static void registerExtendedReachWeapons(String name, double attackOffset, double attackSpeed, float reach) {
		registerUniqueExtendedReachWeapon(Tiers.WOOD, "wooden_" + name, attackOffset, attackSpeed, reach);
		registerUniqueExtendedReachWeapon(Tiers.STONE, "stone_" + name, attackOffset, attackSpeed, reach);
		registerUniqueExtendedReachWeapon(Tiers.IRON, "iron_" + name, attackOffset, attackSpeed, reach);
		registerUniqueExtendedReachWeapon(Tiers.GOLD, "golden_" + name, attackOffset, attackSpeed, reach);
		registerUniqueExtendedReachWeapon(Tiers.DIAMOND, "diamond_" + name, attackOffset, attackSpeed, reach);
	}

	private static void registerUniqueExtendedReachWeapon(String name, double attackOffset, double attackSpeed, float reach) {
		registerUniqueExtendedReachWeapon(Tiers.DIAMOND, name, attackOffset, attackSpeed, reach);
	}

	private static void registerUniqueExtendedReachWeapon(Tier material, String name, double attackOffset, double attackSpeed, float reach) {
		register(name, () -> new ItemExtendedReachWeapon(material, name, attackOffset, attackSpeed, reach));
	}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
	}

	public static void addFactionBlocks() {
		for (FactionDefinition definition : FactionRegistry.getFactionDefinitions()) {
			// TODO-CROSS(structure): AWStructureBlocks.PROTECTION_FLAG / DECORATIVE_FLAG .addFlagDefinition(new BlockFlag.FlagDefinition(name))
			NpcXmod.structure().addFlagDefinition(definition.getName());

			for (Map.Entry<String, CompoundTag> blockData : definition.getThemedBlocksTags().entrySet()) {
				ResourceLocation id = ResourceLocation.tryParse(blockData.getKey());
				if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
					AncientWarfareNPC.LOG.debug("Can't find block with registry name {} in block registry, skipping...", blockData.getKey());
					continue;
				}
				Block block = BuiltInRegistries.BLOCK.get(id);
				blockData.getValue().putString("unlocalizedNamePart", "faction");
				// TODO-CROSS(structure): ItemBlockColored.addCustomItemTag(tag)
				NpcXmod.structure().addThemedBlockItemTag(block, blockData.getValue());
			}
		}
	}
}
