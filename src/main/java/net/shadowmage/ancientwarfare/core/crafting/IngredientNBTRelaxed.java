package net.shadowmage.ancientwarfare.core.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.shadowmage.ancientwarfare.core.util.ItemTools;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/*
 * Port notes: 1.12 matched item + damage and required the ingredient's nbt to be contained in the input's nbt.
 * Here the ingredient's component patch has to be contained in the input: custom data is compared as a sub tag,
 * every other listed component has to be equal ("type": "ancientwarfare:item_nbt_relaxed", "item": "<id>",
 * "components": {...}).
 */
public class IngredientNBTRelaxed implements ICustomIngredient {
	public static final MapCodec<IngredientNBTRelaxed> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ItemStack.ITEM_NON_AIR_CODEC.fieldOf("item").forGetter(i -> i.stack.getItemHolder()),
			DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(i -> i.stack.getComponentsPatch())
	).apply(instance, (item, patch) -> new IngredientNBTRelaxed(new ItemStack(item, 1, patch))));

	private final ItemStack stack;

	protected IngredientNBTRelaxed(ItemStack stack) {
		this.stack = stack;
	}

	@Override
	public boolean test(ItemStack input) {
		if (input == null)
			return false;
		return this.stack.getItem() == input.getItem() && this.stack.getDamageValue() == input.getDamageValue() && nbtTagsMatch(input);
	}

	private boolean nbtTagsMatch(ItemStack input) {
		for (Map.Entry<DataComponentType<?>, Optional<?>> entry : stack.getComponentsPatch().entrySet()) {
			DataComponentType<?> type = entry.getKey();
			if (type == DataComponents.DAMAGE) {
				continue;
			}
			if (type == DataComponents.CUSTOM_DATA) {
				if (!ItemTools.hasTag(input)) {
					return false;
				}
				CompoundTag original = ItemTools.getTag(input);
				CompoundTag merged = original.copy();
				merged.merge(ItemTools.getTag(stack));
				if (!original.equals(merged)) {
					return false;
				}
			} else if (!Objects.equals(input.get(type), entry.getValue().orElse(null))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Stream<ItemStack> getItems() {
		return Stream.of(stack);
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	@Override
	public IngredientType<?> getType() {
		return AWCraftingManager.ITEM_NBT_RELAXED.get();
	}

}
