package net.shadowmage.ancientwarfare.core.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;
import net.shadowmage.ancientwarfare.core.crafting.wrappers.NoRecipeWrapper;
import net.shadowmage.ancientwarfare.core.crafting.wrappers.RegularCraftingWrapper;
import net.shadowmage.ancientwarfare.core.crafting.wrappers.ResearchCraftingWrapper;
import net.shadowmage.ancientwarfare.core.research.ResearchTracker;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.TriFunction;
import org.apache.logging.log4j.util.TriConsumer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/*
 * Port notes (1.12 -> 1.21.1):
 * - 1.12 kept research recipes in its own Forge registry filled from assets/<mod>/research_recipes/*.json at init.
 *   Recipes are data now: data/<ns>/recipe/**.json with "type": "ancientwarfare:research_shaped" /
 *   "ancientwarfare:research_shapeless" (RecipeType "ancientwarfare:research"), loaded and synced by the vanilla
 *   RecipeManager. Datapacks replace the 1.12 config/ancientwarfare/research_recipes override folder.
 * - 1.12 decided at load time whether a recipe became a research recipe or (research system off / item configured as
 *   not researched / recipe disabled) a plain Forge recipe. Here every recipe stays in the research type and the
 *   decision is taken when recipes are looked up (isResearchRecipe / isRegularRecipe); the single special crafting
 *   recipe "ancientwarfare:research_fallback" makes the un-gated ones craftable in any normal crafting grid.
 * - The ingredient factories "item_count", "ore_dict_count" (now "tag_count") and "item_nbt_relaxed" are NeoForge
 *   custom ingredient types with the same ids.
 */
public class AWCraftingManager {
	private AWCraftingManager() {}

	private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, AncientWarfare.MOD_ID);
	private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, AncientWarfare.MOD_ID);
	private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, AncientWarfare.MOD_ID);

	public static final DeferredHolder<RecipeType<?>, RecipeType<ResearchRecipeBase>> RESEARCH_RECIPE_TYPE = RECIPE_TYPES.register("research",
			() -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(AncientWarfare.MOD_ID, "research")));

	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapedResearchRecipe>> RESEARCH_SHAPED_SERIALIZER = RECIPE_SERIALIZERS.register("research_shaped",
			ShapedResearchRecipe.Serializer::new);
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapelessResearchRecipe>> RESEARCH_SHAPELESS_SERIALIZER = RECIPE_SERIALIZERS.register("research_shapeless",
			ShapelessResearchRecipe.Serializer::new);
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ResearchFallbackRecipe>> RESEARCH_FALLBACK_SERIALIZER = RECIPE_SERIALIZERS.register("research_fallback",
			() -> new SimpleCraftingRecipeSerializer<>(ResearchFallbackRecipe::new));

	public static final DeferredHolder<IngredientType<?>, IngredientType<IngredientCount>> ITEM_COUNT = INGREDIENT_TYPES.register("item_count",
			() -> new IngredientType<>(IngredientCount.CODEC));
	public static final DeferredHolder<IngredientType<?>, IngredientType<IngredientOreCount>> TAG_COUNT = INGREDIENT_TYPES.register("tag_count",
			() -> new IngredientType<>(IngredientOreCount.CODEC));
	public static final DeferredHolder<IngredientType<?>, IngredientType<IngredientNBTRelaxed>> ITEM_NBT_RELAXED = INGREDIENT_TYPES.register("item_nbt_relaxed",
			() -> new IngredientType<>(IngredientNBTRelaxed.CODEC));

	/*
	 * 1.12 init() + registerIngredients(): registers the recipe type, the serializers and the ingredient types.
	 * Must be called during mod construction.
	 */
	public static void init(IEventBus modBus) {
		RECIPE_TYPES.register(modBus);
		RECIPE_SERIALIZERS.register(modBus);
		INGREDIENT_TYPES.register(modBus);
	}

	/*
	 * Port addition: the recipe manager of the running server, else the one the client side last saw (remembered
	 * from the client level passed to the lookup methods, or supplied by the optional client hook).
	 */
	private static RecipeManager lastClientRecipeManager = null;
	private static Supplier<RecipeManager> clientRecipeManager = () -> null;

	public static void setClientRecipeManager(Supplier<RecipeManager> supplier) {
		clientRecipeManager = supplier;
	}

	@Nullable
	private static RecipeManager getRecipeManager(@Nullable Level world) {
		if (world != null) {
			if (world.isClientSide) {
				lastClientRecipeManager = world.getRecipeManager();
			}
			return world.getRecipeManager();
		}
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			return server.getRecipeManager();
		}
		RecipeManager manager = clientRecipeManager.get();
		return manager != null ? manager : lastClientRecipeManager;
	}

	private static List<ResearchRecipeBase> getAllResearchRecipes(@Nullable RecipeManager manager) {
		if (manager == null) {
			return Collections.emptyList();
		}
		List<ResearchRecipeBase> ret = new ArrayList<>();
		for (RecipeHolder<ResearchRecipeBase> holder : manager.getAllRecipesFor(RESEARCH_RECIPE_TYPE.get())) {
			holder.value().setRegistryName(holder.id());
			ret.add(holder.value());
		}
		return ret;
	}

	/*
	 * the 1.12 addRecipe conditions under which a recipe was put into the research recipe registry
	 */
	static boolean isResearchRecipe(ResearchRecipeBase recipe) {
		Item item = recipe.getRecipeOutput().getItem();
		return AWCoreStatics.isItemCraftable(item) && ((AWCoreStatics.isItemResearcheable(item) && AWCoreStatics.useResearchSystem) || hasCountIngredient(recipe));
	}

	/*
	 * the 1.12 addRecipe conditions under which a recipe was registered as a normal crafting recipe instead
	 */
	static boolean isRegularRecipe(ResearchRecipeBase recipe) {
		return AWCoreStatics.isItemCraftable(recipe.getRecipeOutput().getItem()) && !isResearchRecipe(recipe);
	}

	/*
	 * used by ResearchFallbackRecipe: the first recipe that needs no research and matches the crafting input
	 */
	@Nullable
	static ResearchRecipeBase findRegularResearchRecipe(CraftingInput input, @Nullable Level world) {
		for (ResearchRecipeBase recipe : getAllResearchRecipes(getRecipeManager(world))) {
			if (isRegularRecipe(recipe) && recipe.matchesGrid(input.width(), input.height(), input::getItem)) {
				return recipe;
			}
		}
		return null;
	}

	private static List<ICraftingRecipe> findMatchingResearchRecipes(CraftingContainer inventory, Level world, String playerName) {
		return findMatchingResearchRecipes(inventory, world, playerName, true);
	}

	private static List<ICraftingRecipe> findMatchingResearchRecipes(CraftingContainer inventory,
			@Nullable Level world, String playerName, boolean checkPlayerResearch) {
		List<ICraftingRecipe> ret = new ArrayList<>();
		if (world == null)
			return ret;
		for (ResearchRecipeBase recipe : getAllResearchRecipes(getRecipeManager(world))) {
			if (isResearchRecipe(recipe) && recipe.matches(inventory, world) && (!checkPlayerResearch || canPlayerCraft(recipe, world, playerName))) {
				ret.add(new ResearchCraftingWrapper(recipe));
			}
		}
		return ret;
	}

	private static List<ICraftingRecipe> findMatchingRegularRecipes(CraftingContainer inventory, @Nullable Level world) {
		List<ICraftingRecipe> ret = new ArrayList<>();
		if (world == null) {
			return ret;
		}
		CraftingInput input = inventory.asCraftInput();
		for (RecipeHolder<CraftingRecipe> recipe : world.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
			if (!(recipe.value() instanceof ResearchFallbackRecipe) && recipe.value().matches(input, world)) {
				ret.add(new RegularCraftingWrapper(recipe));
			}
		}
		for (RecipeHolder<ResearchRecipeBase> recipe : world.getRecipeManager().getAllRecipesFor(RESEARCH_RECIPE_TYPE.get())) {
			if (isRegularRecipe(recipe.value()) && recipe.value().matches(inventory, world)) {
				ret.add(new RegularCraftingWrapper(recipe));
			}
		}
		return ret;
	}

	private static List<ICraftingRecipe> findMatchingRecipesNoResearchCheck(CraftingContainer inventory, Level world) {
		List<ICraftingRecipe> recipes = findMatchingResearchRecipes(inventory, world, "", false);
		recipes.addAll(findMatchingRegularRecipes(inventory, world));
		return recipes;
	}

	public static List<ICraftingRecipe> findMatchingRecipes(CraftingContainer inventory, Level world, String playerName) {
		List<ICraftingRecipe> recipes = findMatchingResearchRecipes(inventory, world, playerName);
		recipes.addAll(findMatchingRegularRecipes(inventory, world));
		return recipes;
	}

	public static boolean canPlayerCraft(Level world, String playerName, String research) {
		return ResearchTracker.INSTANCE.hasPlayerCompleted(world, playerName, research);
	}

	private static boolean canPlayerCraft(ResearchRecipeBase recipe, Level world, String playerName) {
		return !AWCoreStatics.useResearchSystem || canPlayerCraft(world, playerName, recipe.getNeededResearch());
	}

	private static boolean hasCountIngredient(ResearchRecipeBase recipe) {
		return recipe.getIngredients().stream().anyMatch(i -> i.getCustomIngredient() instanceof IIngredientCount);
	}

	/*
	 * all recipes that currently are research recipes (see isResearchRecipe)
	 */
	public static Collection<ResearchRecipeBase> getRecipes() {
		return getRecipes(null);
	}

	public static Collection<ResearchRecipeBase> getRecipes(@Nullable Level world) {
		return getAllResearchRecipes(getRecipeManager(world)).stream().filter(AWCraftingManager::isResearchRecipe).collect(Collectors.toList());
	}

	/*
	 * Port addition (for the JEI plugin): the research-type recipes that currently are plain crafting recipes (see
	 * isRegularRecipe; 1.12 registered those as normal Forge recipes)
	 */
	public static Collection<ResearchRecipeBase> getRegularRecipes(@Nullable Level world) {
		return getAllResearchRecipes(getRecipeManager(world)).stream().filter(AWCraftingManager::isRegularRecipe).collect(Collectors.toList());
	}

	public static ICraftingRecipe getRecipe(RecipeResourceLocation recipe) {
		return getRecipe(null, recipe);
	}

	@SuppressWarnings("unchecked")
	public static ICraftingRecipe getRecipe(@Nullable Level world, RecipeResourceLocation recipe) {
		RecipeManager manager = getRecipeManager(world);
		if (manager == null) {
			return NoRecipeWrapper.INSTANCE;
		}
		Optional<RecipeHolder<?>> holder;
		switch (recipe.getRecipeType()) {
			case REGULAR:
				holder = manager.byKey(recipe.getResourceLocation());
				if (holder.isPresent() && holder.get().value() instanceof ResearchRecipeBase) {
					return isRegularRecipe((ResearchRecipeBase) holder.get().value()) ? new RegularCraftingWrapper((RecipeHolder<? extends Recipe<CraftingInput>>) holder.get()) : NoRecipeWrapper.INSTANCE;
				}
				return holder.isPresent() && holder.get().value() instanceof CraftingRecipe ? new RegularCraftingWrapper((RecipeHolder<? extends Recipe<CraftingInput>>) holder.get()) : NoRecipeWrapper.INSTANCE;
			case RESEARCH:
				holder = manager.byKey(recipe.getResourceLocation());
				if (holder.isPresent() && holder.get().value() instanceof ResearchRecipeBase && isResearchRecipe((ResearchRecipeBase) holder.get().value())) {
					ResearchRecipeBase resRecipe = (ResearchRecipeBase) holder.get().value();
					resRecipe.setRegistryName(holder.get().id());
					return new ResearchCraftingWrapper(resRecipe);
				}
				return NoRecipeWrapper.INSTANCE;
			default:
				return NoRecipeWrapper.INSTANCE;
		}
	}

	public static CraftingContainer fillCraftingMatrixFromInventory(List<ItemStack> resources) {
		CraftingContainer invCrafting = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
			@Override
			public ItemStack quickMoveStack(Player player, int index) {
				return ItemStack.EMPTY;
			}

			@Override
			public boolean stillValid(Player playerIn) {
				return true;
			}
		}, 3, 3);

		for (int i = 0; i < resources.size(); i++) {
			invCrafting.setItem(i, resources.get(i));
		}

		return invCrafting;
	}

	public static ICraftingRecipe findMatchingRecipe(Level world, NonNullList<ItemStack> inputs, ItemStack result) {
		CraftingContainer inv = fillCraftingMatrixFromInventory(inputs);

		List<ICraftingRecipe> recipes = findMatchingRecipesNoResearchCheck(inv, world);
		if (recipes.isEmpty()) {
			return NoRecipeWrapper.INSTANCE;
		}

		if (recipes.size() == 1) {
			return recipes.get(0);
		}

		return recipes.stream().filter(r -> recipeResultsEqual(result, r, inv)).findFirst().orElse(recipes.get(0));
	}

	/*
	 * Port note: 1.12 compared the ore dictionary ids of both results (when the wanted result had any), else the
	 * items; item tags take the place of the ore dictionary.
	 */
	private static boolean recipeResultsEqual(ItemStack result, ICraftingRecipe r, CraftingContainer inv) {
		ItemStack otherResult = r.getCraftingResult(inv);
		if (otherResult.isEmpty()) {
			return false;
		}

		Set<TagKey<Item>> tags = result.getTags().collect(Collectors.toSet());
		if (!tags.isEmpty()) {
			return tags.equals(otherResult.getTags().collect(Collectors.toSet()));
		}
		return ItemStack.isSameItem(otherResult, result);
	}

	public static NonNullList<ItemStack> getRecipeInventoryMatch(ICraftingRecipe recipe, IItemHandler inventory) {
		return getRecipeInventoryMatch(recipe, NonNullList.create(), inventory);
	}

	public static NonNullList<ItemStack> getRecipeInventoryMatch(ICraftingRecipe recipe, List<ItemStack> exactStacks, IItemHandler inventory) {
		return getRecipeInventoryMatch(recipe, exactStacks, s -> InventoryTools.hasCountOrMore(inventory, s), inventory);
	}

	public static NonNullList<ItemStack> getRecipeInventoryMatch(ICraftingRecipe recipe, List<ItemStack> exactStacks, Function<ItemStack, Boolean> hasCountOrMore, IItemHandler inventory) {
		return getRecipeInventoryMatch(recipe, exactStacks, hasCountOrMore, inventory, () -> NonNullList.withSize(9, ItemStack.EMPTY), (list, slot, stack) -> {
			list.set(slot, stack);
			return list;
		}, (a, in) -> NonNullList.create());
	}

	public static <T> T getRecipeInventoryMatch(ICraftingRecipe recipe, IItemHandler inventory, Supplier<T> initialize, TriConsumer<T, Integer, ItemStack> onMatch, BiConsumer<T, Ingredient> onFail) {
		return getRecipeInventoryMatch(recipe, NonNullList.create(), s -> InventoryTools.hasCountOrMore(inventory, s), inventory, initialize, onMatch, onFail);
	}

	public static <T> T getRecipeInventoryMatch(ICraftingRecipe recipe, List<ItemStack> exactStacks, Function<ItemStack, Boolean> hasCountOrMore, IItemHandler inventory, Supplier<T> initialize, TriConsumer<T, Integer, ItemStack> onMatch, BiConsumer<T, Ingredient> onFail) {
		return getRecipeInventoryMatch(recipe, exactStacks, hasCountOrMore, inventory, initialize, (t, i, s) -> {
			onMatch.accept(t, i, s);
			return t;
		}, (t, in) -> {
			onFail.accept(t, in);
			return t;
		});
	}

	@SuppressWarnings("squid:UnusedPrivateMethod")
	private static <T> T getRecipeInventoryMatch(ICraftingRecipe recipe, List<ItemStack> exactStacks, Function<ItemStack, Boolean> hasCountOrMore, IItemHandler inventory, Supplier<T> initialize, TriFunction<T, Integer, ItemStack, T> onMatch, BiFunction<T, Ingredient, T> onFail) {
		T ret = initialize.get();

		if (!recipe.isValid()) {
			ret = onFail.apply(ret, Ingredient.EMPTY);
			return ret;
		}

		Map<Integer, IngredientMatchData> ingredientsData = getIngredientData(recipe.getIngredients());
		Map<Integer, Ingredient> remainingIngredients = getSlotIngredients(ingredientsData);

		ret = matchExactStacks(exactStacks, hasCountOrMore, onMatch, ret, ingredientsData, remainingIngredients);

		if (remainingIngredients.isEmpty()) {
			return ret;
		}

		ret = matchUsingRecipeIngredients(inventory, onMatch, ret, ingredientsData, remainingIngredients);

		ret = processFailedIngredients(onFail, ret, remainingIngredients);

		return ret;
	}

	private static <T> T matchUsingRecipeIngredients(IItemHandler inventory, TriFunction<T, Integer, ItemStack, T> onMatch, T ret, Map<Integer, IngredientMatchData> ingredientsData, Map<Integer, Ingredient> remainingIngredients) {
		for (int slot = 0; slot < inventory.getSlots() && !remainingIngredients.isEmpty(); slot++) {
			ItemStack resourceStack = inventory.getStackInSlot(slot);
			if (resourceStack.isEmpty()) {
				continue;
			}
			ret = matchIngredientsToStack(onMatch, ret, ingredientsData, remainingIngredients, resourceStack);
		}
		return ret;
	}

	private static <T> T matchIngredientsToStack(TriFunction<T, Integer, ItemStack, T> onMatch, T ret, Map<Integer, IngredientMatchData> ingredientsData, Map<Integer, Ingredient> remainingIngredients, ItemStack resourceStack) {
		int currentStackCount = resourceStack.getCount();

		Iterator<Map.Entry<Integer, Ingredient>> it = remainingIngredients.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Ingredient> entry = it.next();
			Ingredient ingredient = entry.getValue();

			if (ingredient.test(resourceStack)) {
				IngredientMatchData ingredientData = ingredientsData.get(entry.getKey());
				int countToRemove = Math.min(ingredientData.remainingCount, currentStackCount);
				currentStackCount -= countToRemove;
				ingredientData.remainingCount -= countToRemove;
				if (ingredientData.remainingCount < 1) {
					ret = addSuccessfulMatch(ret, onMatch, resourceStack, ingredientData.count, ingredientData.index);
					it.remove();
				}
				if (currentStackCount <= 0) {
					break;
				}
			}
		}
		return ret;
	}

	private static <T> T matchExactStacks(List<ItemStack> exactStacks, Function<ItemStack, Boolean> hasCountOrMore, TriFunction<T, Integer, ItemStack, T> onMatch, T ret, Map<Integer, IngredientMatchData> ingredientsData, Map<Integer, Ingredient> remainingIngredients) {
		Map<ItemStack, Set<Integer>> consolidatedStacks = consolidateStacks(exactStacks, ingredientsData);
		for (Map.Entry<ItemStack, Set<Integer>> consolidatedStack : consolidatedStacks.entrySet()) {
			ItemStack stack = consolidatedStack.getKey();
			if (hasCountOrMore.apply(stack)) {
				for (Integer ingredientIndex : consolidatedStack.getValue()) {
					remainingIngredients.remove(ingredientIndex);
					if (ingredientsData.containsKey(ingredientIndex)) {
						IngredientMatchData ingredientData = ingredientsData.get(ingredientIndex);
						ret = addSuccessfulMatch(ret, onMatch, stack, ingredientData.count, ingredientData.index);
					} else {
						ret = addSuccessfulMatch(ret, onMatch, stack, 1, ingredientIndex);
					}
				}
			} else {
				for (int ingredientIndex : consolidatedStack.getValue()) {
					if (!remainingIngredients.containsKey(ingredientIndex)) {
						Ingredient ingredient = new IngredientCount(consolidatedStack.getKey(), 1).toVanilla();
						remainingIngredients.put(ingredientIndex, ingredient);
						ingredientsData.put(ingredientIndex, getSlotIngredient(ingredientIndex, ingredient));
					}
				}
			}
		}
		return ret;
	}

	private static <T> T addSuccessfulMatch(T ret, TriFunction<T, Integer, ItemStack, T> onMatch, ItemStack resourceStack, int count, int index) {
		ItemStack stackFound = resourceStack.copy();
		stackFound.setCount(count);
		ret = onMatch.apply(ret, index, stackFound);
		return ret;
	}

	private static Map<ItemStack, Set<Integer>> consolidateStacks(List<ItemStack> exactStacks, Map<Integer, IngredientMatchData> ingredientsData) {
		Map<ItemStack, Set<Integer>> ret = new HashMap<>();
		Set<Integer> matchedIngredientIndexes = new HashSet<>();
		int slot = 0;
		for (ItemStack stack : exactStacks) {
			consolidateStack(ingredientsData, ret, matchedIngredientIndexes, stack, slot);
			slot++;
		}

		return ret;
	}

	private static void consolidateStack(Map<Integer, IngredientMatchData> ingredientsData, Map<ItemStack, Set<Integer>> stackIngredientIndexes,
			Set<Integer> matchedIngredientIndexes, ItemStack stack, int slot) {
		if (stack.isEmpty()) {
			return;
		}
		int index = getIngredientIndexForStack(ingredientsData, matchedIngredientIndexes, stack);
		int count = 1;
		if (index == -1) {
			index = slot;
		} else {
			count = ingredientsData.get(index).count;
		}
		matchedIngredientIndexes.add(index);
		Optional<Map.Entry<ItemStack, Set<Integer>>> matching = getMatchingStackEntry(stackIngredientIndexes, stack);
		if (matching.isPresent()) {
			Map.Entry<ItemStack, Set<Integer>> entry = matching.get();
			entry.getKey().grow(count);
			entry.getValue().add(index);
		} else {
			Set<Integer> indexes = new HashSet<>();
			indexes.add(index);
			ItemStack stackCopy = stack.copy();
			stackCopy.setCount(count);
			stackIngredientIndexes.put(stackCopy, indexes);
		}
	}

	private static int getIngredientIndexForStack(Map<Integer, IngredientMatchData> ingredientsData, Set<Integer> matchedIngredientIndexes, ItemStack stack) {
		for (Map.Entry<Integer, IngredientMatchData> ingredientData : ingredientsData.entrySet()) {
			IngredientMatchData data = ingredientData.getValue();
			if (!matchedIngredientIndexes.contains(data.index) && data.ingredient.test(stack)) {
				return data.index;
			}
		}
		return -1;
	}

	private static Optional<Map.Entry<ItemStack, Set<Integer>>> getMatchingStackEntry(Map<ItemStack, Set<Integer>> stacks, ItemStack stackToFind) {
		for (Map.Entry<ItemStack, Set<Integer>> entry : stacks.entrySet()) {
			if (InventoryTools.doItemStacksMatch(entry.getKey(), stackToFind)) {
				return Optional.of(entry);
			}
		}
		return Optional.empty();
	}

	private static Map<Integer, Ingredient> getSlotIngredients(Map<Integer, IngredientMatchData> ingredientsData) {
		Map<Integer, Ingredient> ret = new HashMap<>();
		ingredientsData.forEach((slot, ingredientData) -> ret.put(slot, ingredientData.ingredient));
		return ret;
	}

	private static <T> T processFailedIngredients(BiFunction<T, Ingredient, T> onFail, T ret, Map<Integer, Ingredient> remainingIngredients) {
		for (Ingredient ingredient : remainingIngredients.values()) {
			ret = onFail.apply(ret, ingredient);
		}
		return ret;
	}

	private static class IngredientMatchData {
		public Ingredient ingredient;
		public int index;
		public int count;
		public int remainingCount;

		private IngredientMatchData(Ingredient ingredient, int index, int count) {
			this.ingredient = ingredient;
			this.index = index;
			this.count = count;
			remainingCount = count;
		}
	}

	private static Map<Integer, IngredientMatchData> getIngredientData(NonNullList<Ingredient> ingredients) {
		Map<Integer, IngredientMatchData> ret = new LinkedHashMap<>();
		for (int slot = 0; slot < ingredients.size(); slot++) {
			Ingredient ingredient = ingredients.get(slot);

			if (ingredient.isEmpty()) { //skip empty ingredients
				continue;
			}

			ret.put(slot, getSlotIngredient(slot, ingredient));
		}

		return ret;
	}

	private static IngredientMatchData getSlotIngredient(int slot, Ingredient ingredient) {
		return new IngredientMatchData(ingredient, slot, IngredientCount.getCount(ingredient));
	}

	public static ItemStack getIngredientInventoryMatch(IItemHandler clonedResourceInventory, Ingredient ingredient) {
		ItemStack stackFound = ItemStack.EMPTY;
		int count = IngredientCount.getCount(ingredient);
		for (int slot = 0; slot < clonedResourceInventory.getSlots(); slot++) {
			ItemStack resourceStack = clonedResourceInventory.getStackInSlot(slot);

			if (!resourceStack.isEmpty()) {
				//required for ingredient to actually see proper count and say it's a good item
				//e.g. ingredient requires stack of 3 but inventory only has 3 stacks of 1 of the item - that's still a match for the recipe
				ItemStack properCountStack = resourceStack.copy();
				properCountStack.setCount(count);

				if (ingredient.test(properCountStack)) {
					ItemStack removedStack = InventoryTools.removeItems(clonedResourceInventory, resourceStack, count, true);

					if (removedStack.getCount() == count) {
						InventoryTools.removeItems(clonedResourceInventory, resourceStack, count, false);
						stackFound = removedStack;
						break;
					}
				}
			}
		}
		return stackFound;
	}

	public static NonNullList<ItemStack> getReusableStacks(ICraftingRecipe recipe, CraftingContainer craftMatrix) {
		return recipe.getRemainingItems(craftMatrix).stream().filter(s -> !s.isEmpty() && recipe.getIngredients().stream().anyMatch(i -> i.test(s)))
				.collect(Collectors.toCollection(NonNullList::create));
	}
}
