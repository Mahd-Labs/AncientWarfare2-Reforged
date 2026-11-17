package net.shadowmage.ancientwarfare.npc.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;

/*
 * Port notes: 1.12 registered four OrderCopyingRecipe instances in code (upkeep / routing / combat / work order).
 * Recipes are data in 1.21.1: the recipe class is the custom type "ancientwarfare:order_copy" with the order item as
 * its only parameter, and the four recipes are json files (data/ancientwarfare/recipe/npc/<x>_order_copy.json).
 */
public class AWNpcCrafting {
	private AWNpcCrafting() {}

	private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, AncientWarfareNPC.MOD_ID);

	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<OrderCopyingRecipe>> ORDER_COPY_SERIALIZER = RECIPE_SERIALIZERS.register("order_copy", OrderCopyingSerializer::new);

	public static void register(IEventBus modBus) {
		RECIPE_SERIALIZERS.register(modBus);
	}

	public static class OrderCopyingRecipe extends CustomRecipe {
		private final Item item;

		public OrderCopyingRecipe(Item item) {
			super(CraftingBookCategory.MISC);
			this.item = item;
		}

		public Item getItem() {
			return item;
		}

		@Override
		public boolean matches(CraftingInput var1, Level var2) {
			ItemStack order1 = ItemStack.EMPTY;
			ItemStack order2 = ItemStack.EMPTY;
			boolean foundOtherStuff = false;
			ItemStack stack;
			for (int i = 0; i < var1.size(); i++) {
				stack = var1.getItem(i);
				if (stack.isEmpty()) {
					continue;
				}
				if (stack.getItem() == item) {
					if (order1.isEmpty()) {
						order1 = stack;
					} else if (order2.isEmpty()) {
						order2 = stack;
					} else {
						foundOtherStuff = true;
						break;
					}
				} else {
					foundOtherStuff = true;
					break;
				}
			}
			return !foundOtherStuff && !order1.isEmpty() && !order2.isEmpty();
		}

		@Override
		public ItemStack assemble(CraftingInput var1, HolderLookup.Provider registries) {
			ItemStack order1 = ItemStack.EMPTY;
			ItemStack order2 = ItemStack.EMPTY;
			boolean foundOtherStuff = false;
			ItemStack stack;
			for (int i = 0; i < var1.size(); i++) {
				stack = var1.getItem(i);
				if (stack.isEmpty()) {
					continue;
				}
				if (stack.getItem() == item) {
					if (order1.isEmpty()) {
						order1 = stack;
					} else if (order2.isEmpty()) {
						order2 = stack;
					} else {
						foundOtherStuff = true;
						break;
					}
				} else {
					foundOtherStuff = true;
					break;
				}
			}
			if (foundOtherStuff || order1.isEmpty() || order2.isEmpty()) {
				return ItemStack.EMPTY;
			}
			ItemStack retStack = order2.copy();
			if (ItemTools.hasTag(order1)) {
				ItemTools.setTag(retStack, ItemTools.getTag(order1).copy());
			} else {
				ItemTools.setTag(retStack, null);
			}
			retStack.setCount(2);
			return retStack;
		}

		@Override
		public boolean canCraftInDimensions(int width, int height) {
			return width * height >= 2;
		}

		@Override
		public ItemStack getResultItem(HolderLookup.Provider registries) {
			return new ItemStack(item);
		}

		@Override
		public RecipeSerializer<?> getSerializer() {
			return ORDER_COPY_SERIALIZER.get();
		}
	}

	public static class OrderCopyingSerializer implements RecipeSerializer<OrderCopyingRecipe> {
		private static final MapCodec<OrderCopyingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(OrderCopyingRecipe::getItem)
		).apply(instance, OrderCopyingRecipe::new));
		private static final StreamCodec<RegistryFriendlyByteBuf, OrderCopyingRecipe> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.registry(Registries.ITEM), OrderCopyingRecipe::getItem, OrderCopyingRecipe::new);

		@Override
		public MapCodec<OrderCopyingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, OrderCopyingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
