package net.shadowmage.ancientwarfare.npc.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.npc.init.AWNPCItems;
import net.shadowmage.ancientwarfare.npc.xmod.NpcXmod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Port notes: the metal stays in the stack tag ("metal", custom data) as in 1.12. The coin stack blocks belong to the
 * structure module and are reached through NpcXmod (placing fails while that module is absent).
 */
public class ItemCoin extends ItemBaseNPC {

	public ItemCoin() {
		super("coin");
	}

	/* 1.12 getSubItems */
	public List<ItemStack> getSubItems() {
		List<ItemStack> items = new ArrayList<>();
		Arrays.stream(CoinMetal.values()).forEach(metal -> {
					ItemStack subItem = getCoinStack(metal);
					items.add(subItem);
				}
		);
		return items;
	}

	private static ItemStack getCoinStack(CoinMetal metal) {
		return getCoinStack(metal, 1);
	}

	public static ItemStack getCoinStack(CoinMetal metal, int stackSize) {
		ItemStack coinStack = new ItemStack(AWNPCItems.COIN.get());
		ItemTools.setTagInfo(coinStack, "metal", StringTag.valueOf(metal.getName()));
		coinStack.setCount(stackSize);
		return coinStack;
	}

	public static boolean isSpecificCoin(ItemStack stack, CoinMetal coinMetal) {
		return stack.getItem() == AWNPCItems.COIN.get() && getMetal(stack) == coinMetal;
	}

	@Override
	public String getDescriptionId(ItemStack stack) {
		return "item.coin." + getMetalName(stack) + ".name";
	}

	public static CoinMetal getMetal(ItemStack stack) {
		if (!ItemTools.hasTag(stack)) {
			return CoinMetal.COPPER;
		}
		return CoinMetal.byName(getMetalName(stack));
	}

	private static String getMetalName(ItemStack stack) {
		return ItemTools.hasTag(stack) ? ItemTools.getTag(stack).getString("metal") : "";
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		ItemStack stack = context.getItemInHand();
		CoinMetal metal = getMetal(stack);

		if ((stack.getCount() < 8)) {
			return InteractionResult.FAIL;
		}

		BlockState state = world.getBlockState(pos);

		if (!state.canBeReplaced()) {
			pos = pos.relative(context.getClickedFace());
		}

		// TODO-CROSS(structure): ItemCoin.METALS_TO_BLOCKS (AWStructureBlocks.COIN_STACK_<metal>)
		Block coinBlock = metal == null ? null : NpcXmod.structure().getCoinStackBlock(metal.getName());
		if (coinBlock == null) {
			return InteractionResult.FAIL;
		}

		BlockState coinState = coinBlock.defaultBlockState();
		if (!world.getBlockState(pos).canBeReplaced() || !coinState.canSurvive(world, pos) || (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), stack))) {
			return InteractionResult.FAIL;
		}
		world.setBlockAndUpdate(pos, coinState);
		stack.shrink(8);
		// TODO-CROSS(structure): AWStructureSounds.COIN_STACK_INTERACT
		SoundEvent sound = NpcXmod.structure().getCoinStackInteractSound();
		if (sound != null) {
			world.playSound(null, pos, sound, SoundSource.PLAYERS, 0.5f, 1);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		if (getMetal(stack) == CoinMetal.ANCIENT) {
			tooltip.add(Component.translatable("item.coin.ancient.tooltip", ChatFormatting.DARK_AQUA.toString() + ChatFormatting.ITALIC.toString()));
		}
	}

	public enum CoinMetal {
		ANCIENT("ancient", 0x445948),
		GOLD("gold", 0xFFD700),
		SILVER("silver", 0xC0C0C0),
		COPPER("copper", 0xB87333);

		private String name;
		private int color;

		CoinMetal(String name, int color) {
			this.name = name;
			this.color = color;
		}

		public String getName() {
			return name;
		}

		private static Map<String, CoinMetal> values = new HashMap<>();

		static {
			Arrays.stream(values()).forEach(m -> values.put(m.getName(), m));
		}

		public static CoinMetal byName(String name) {
			return values.get(name);
		}

		public int getColor() {
			return color;
		}
	}
}
