package net.shadowmage.ancientwarfare.core.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import java.util.function.IntFunction;

/*
 * Port notes: json is { "type": "ancientwarfare:research_shaped", "research": "<goal>", "pattern": [...],
 * "key": {...}, "result": {"id": ..., "count": n} } (1.12: "ancientwarfare:research_recipe"). The vanilla
 * ShapedRecipePattern replaces the Forge ShapedPrimer; matching still is the 1.12 non mirrored grid scan.
 */
public class ShapedResearchRecipe extends ResearchRecipeBase {
	protected int width = 0;
	protected int height = 0;
	private final ShapedRecipePattern pattern;

	public ShapedResearchRecipe(String research, ItemStack output, ShapedRecipePattern pattern) {
		super(research, pattern.ingredients(), output);
		this.width = pattern.width();
		this.height = pattern.height();
		this.pattern = pattern;
	}

	@Override
	boolean matchesGrid(int gridWidth, int gridHeight, IntFunction<ItemStack> stacks) {
		for (int x = 0; x <= gridWidth - width; x++) {
			for (int y = 0; y <= gridHeight - height; ++y) {
				if (checkMatch(gridWidth, gridHeight, stacks, x, y, false)) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean checkMatch(int gridWidth, int gridHeight, IntFunction<ItemStack> stacks, int startX, int startY, boolean mirror) {
		for (int x = 0; x < gridWidth; x++) {
			for (int y = 0; y < gridHeight; y++) {
				int subX = x - startX;
				int subY = y - startY;
				Ingredient target = Ingredient.EMPTY;

				if (subX >= 0 && subY >= 0 && subX < width && subY < height) {
					if (mirror) {
						target = getIngredients().get(width - subX - 1 + subY * width);
					} else {
						target = getIngredients().get(subX + subY * width);
					}
				}

				if (!target.test(stacks.apply(x + y * gridWidth))) {
					return false;
				}
			}
		}

		return true;
	}

	public int getRecipeWidth() {
		return width;
	}

	public int getRecipeHeight() {
		return height;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width >= this.width && height >= this.height;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AWCraftingManager.RESEARCH_SHAPED_SERIALIZER.get();
	}

	public static class Serializer implements RecipeSerializer<ShapedResearchRecipe> {
		public static final MapCodec<ShapedResearchRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.STRING.fieldOf("research").forGetter(ResearchRecipeBase::getNeededResearch),
				ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ResearchRecipeBase::getRecipeOutput),
				ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern)
		).apply(instance, ShapedResearchRecipe::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, ShapedResearchRecipe> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, ResearchRecipeBase::getNeededResearch,
				ItemStack.STREAM_CODEC, ResearchRecipeBase::getRecipeOutput,
				ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
				ShapedResearchRecipe::new);

		@Override
		public MapCodec<ShapedResearchRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, ShapedResearchRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
