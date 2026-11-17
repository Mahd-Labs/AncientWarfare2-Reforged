package net.shadowmage.ancientwarfare.core.util;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/*
 * CLIENT ONLY. 1.12: thin wrapper around ModelLoader.setCustomModelResourceLocation(item, meta, location).
 *
 * 1.21.1 resolves the model of an item from its registry name (models/item/<name>.json), there is no per item /
 * per metadata model location any more, and items with sub types are separate items in the port. What is left:
 *
 *  - registerItem(.., ModelResourceLocation) and the Function2 overload: the item is rendered with the baked model
 *    registered under that location. This is what the bakery blocks need ("item uses the model of block state X" /
 *    "item uses the BakeryModel"). The alias is applied in ModelEvent.ModifyBakingResult (see onModifyBakingResult,
 *    hooked up by AncientWarfareCoreClient), after the module's own listener put its BakeryModels in - register
 *    module listeners that add models with EventPriority.HIGH or simply before the alias is needed.
 *  - the prefix / variant overloads built "ancientwarfare:<prefix>/<registry path>[_meta]#<variant>" for the Forge
 *    blockstate json format. With variant "inventory" there is nothing to do in the port (the converted item model
 *    json is found by registry name). Any other variant is treated as a block state variant string of the block
 *    with the same registry name ("normal" = the property-less state) and aliased like above.
 *
 * Call these from the module client init (not from block / item classes: common code must not load this class).
 */
public class ModelLoaderHelper {

	private ModelLoaderHelper() {
	}

	private static final Map<Item, ModelResourceLocation> customLocations = new LinkedHashMap<>();

	public static void registerItem(Item item, String prefix) {
		registerItem(item, prefix, "inventory");
	}

	public static void registerItem(Item item, String prefix, boolean metaSuffix) {
		registerItem(item, prefix, metaSuffix, "inventory");
	}

	public static void registerItem(Block block, String prefix, String variant, boolean metaSuffix) {
		registerItem(block.asItem(), prefix, metaSuffix, variant);
	}

	public static void registerItem(Block block, String prefix, String variant) {
		registerItem(block.asItem(), prefix, variant);
	}

	public static void registerItem(Item item, String prefix, String variant) {
		registerItem(item, prefix, true, variant);
	}

	public static void registerItem(Item item, String prefix, boolean metaSuffix, String variant) {
		registerItem(item, prefix, metaSuffix, meta -> variant);
	}

	public static void registerItem(Item item, String prefix, boolean metaSuffix, Function<Integer, String> getVariant) {
		String variant = getVariant.apply(0);
		if (ModelResourceLocation.INVENTORY_VARIANT.equals(variant)) {
			return;//models/item/<registry name>.json is picked up by vanilla
		}
		ResourceLocation name = BuiltInRegistries.ITEM.getKey(item);
		registerItem(item, (it, meta) -> new ModelResourceLocation(name, "normal".equals(variant) ? "" : variant));
	}

	public static void registerItem(Block block, ModelResourceLocation modelLocation) {
		registerItem(block.asItem(), (i, m) -> modelLocation);
	}

	public static void registerItem(Item item, Function2<Item, Integer, ModelResourceLocation> getModelLocation) {
		customLocations.put(item, getModelLocation.apply(item, 0));
	}

	public static void registerItem(Item item, int meta, String modelVariantName) {
		//TODO again hardcoded to just ancientwarfare mod name
		customLocations.put(item, ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, modelVariantName)));
	}

	public static void registerItem(Item item, int meta, String modelName, String variant) {
		customLocations.put(item, new ModelResourceLocation(ResourceLocation.parse(modelName), variant));
	}

	/*
	 * points the inventory model of every registered item at its custom model
	 */
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
		Map<ModelResourceLocation, BakedModel> models = event.getModels();
		for (Map.Entry<Item, ModelResourceLocation> entry : customLocations.entrySet()) {
			ModelResourceLocation itemLocation = ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(entry.getKey()));
			if (itemLocation.equals(entry.getValue())) {
				continue;
			}
			BakedModel model = models.get(entry.getValue());
			if (model == null) {
				AncientWarfare.LOG.error("No baked model {} for item {}", entry.getValue(), itemLocation.id());
				continue;
			}
			models.put(itemLocation, model);
		}
	}

}
