package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.NBTBuilder;
import net.shadowmage.ancientwarfare.npc.item.ItemCoin;
import net.shadowmage.ancientwarfare.structure.init.AWStructureSoundTypes;
import net.shadowmage.ancientwarfare.structure.init.AWStructureSounds;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BlockCoinStack extends BlockBaseStructure {
	private ItemCoin.CoinMetal coinMetal;

	public BlockCoinStack(String regName, ItemCoin.CoinMetal coinMetal) {
		//1.12 setHardness(0.3) + setResistance(0.4): explosion resistance 0.4 * 3 / 5
		super(Material.GROUND.props().noOcclusion().strength(0.3F, 0.24F), regName);
		this.coinMetal = coinMetal;
	}

	private static final Map<Integer, AABB> STACK_SIZE_AABBs = new ImmutableMap.Builder<Integer, AABB>()
			.put(8, new AABB(0D, 0D, 0D, 1D, 0.10D, 1D))
			.put(16, new AABB(0D, 0D, 0D, 1D, 0.25D, 1D))
			.put(24, new AABB(0D, 0D, 0D, 1D, 0.37D, 1D))
			.put(32, new AABB(0D, 0D, 0D, 1D, 0.57D, 1D))
			.put(40, new AABB(0D, 0D, 0D, 1D, 0.81D, 1D))
			.put(48, new AABB(0D, 0D, 0D, 1D, 0.81D, 1D))
			.put(56, new AABB(0D, 0D, 0D, 1D, 0.95D, 1D))
			.put(64, new AABB(0D, 0D, 0D, 1D, 1.00D, 1D))
			.build();

	private static final String STACK_SIZE_TAG = "size";

	private static final PropertyStackSize STACK_SIZE = new PropertyStackSize();

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(STACK_SIZE);
	}

	@Override
	public void onBlockPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (!ItemTools.hasTag(stack)) {
			return;
		}
		worldIn.setBlockAndUpdate(pos, state.setValue(STACK_SIZE, getCoinStackSize(stack)));
	}

	private int getCoinStackSize(ItemStack stack) {
		//noinspection ConstantConditions
		return ItemTools.hasTag(stack) ? ItemTools.getTag(stack).getInt(STACK_SIZE_TAG) : 0;
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
		drops.add(ItemCoin.getCoinStack(coinMetal, state.getValue(STACK_SIZE)));
	}

	@Override
	public boolean onBlockActivated(Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getItemInHand(hand);
		if (world.isClientSide) {
			return true;
		}
		if (ItemCoin.isSpecificCoin(stack, coinMetal)) {
			addToStack(world, pos, state, player, stack);
		} else {
			removeFromStack(world, pos, state, player);
		}
		return true;
	}

	private void removeFromStack(Level world, BlockPos pos, BlockState state, Player player) {
		int blockstacksize = state.getValue(STACK_SIZE);

		//noinspection ConstantConditions
		InventoryTools.insertOrDropItem(player.getCapability(Capabilities.ItemHandler.ENTITY), ItemCoin.getCoinStack(coinMetal, 8), world, pos);
		if (blockstacksize > 8) {
			world.setBlockAndUpdate(pos, state.setValue(STACK_SIZE, blockstacksize - 8));
		} else {
			world.removeBlock(pos, false);
		}
		world.playSound(null, pos, AWStructureSounds.COIN_STACK_INTERACT.get(), SoundSource.PLAYERS, 0.5f, 1);
	}

	private void addToStack(Level world, BlockPos pos, BlockState state, Player player, ItemStack stack) {
		if (stack.getCount() >= 8) {
			int blockStackSize = state.getValue(STACK_SIZE);
			if (blockStackSize < 64) {
				world.setBlockAndUpdate(pos, state.setValue(STACK_SIZE, (blockStackSize + 8)));
				if (!player.getAbilities().instabuild) {
					stack.shrink(8);
				}
			}
		}
		world.playSound(null, pos, AWStructureSounds.COIN_STACK_INTERACT.get(), SoundSource.PLAYERS, 0.5f, 1);
	}

	@Override
	public SoundType getSoundType(BlockState state, LevelReader world, BlockPos pos, Entity entity) {
		return AWStructureSoundTypes.COINSTACK;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		tooltip.add(Component.literal(getCoinStackSize(stack) + " ").append(Component.translatable("guistrings.structure.coins")));
	}

	@Override
	public AABB getBoundingBox(BlockState state, BlockGetter source, BlockPos pos) {
		return STACK_SIZE_AABBs.get(state.getValue(STACK_SIZE));
	}

	/*
	 * Port notes: 1.12 PropertyHelper<Integer> subclass with the values 8, 16, .. 64 -> Property<Integer> subclass
	 */
	public static class PropertyStackSize extends Property<Integer> {
		private final ImmutableSet<Integer> allowedValues;

		private PropertyStackSize() {
			super(STACK_SIZE_TAG, Integer.class);
			this.allowedValues = ImmutableSet.of(8, 16, 24, 32, 40, 48, 56, 64);
		}

		@Override
	public Collection<Integer> getPossibleValues() {
			return this.allowedValues;
		}

		@Override
	public java.util.Optional<Integer> getValue(String value) {
			try {
				Integer integer = Integer.valueOf(value);
				return this.allowedValues.contains(integer) ? java.util.Optional.of(integer) : java.util.Optional.empty();
			}
			catch (NumberFormatException var3) {
				return java.util.Optional.empty();
			}
		}

		@Override
	public String getName(Integer value) {
			return value.toString();
		}
	}
}
