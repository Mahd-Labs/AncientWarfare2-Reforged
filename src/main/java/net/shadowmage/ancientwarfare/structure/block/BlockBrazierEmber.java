package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.EquipmentSlot;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.util.NBTBuilder;

import java.util.Random;

public class BlockBrazierEmber extends BlockBaseStructure {
	public static final String LIT_TAG = "lit";
	private static final BooleanProperty LIT = BooleanProperty.create(LIT_TAG);

	private static final AABB AABB = new AABB(0D, 0D, 0D, 1D, 0.88D, 1D);

	public BlockBrazierEmber() {
		super(Material.ROCK.props().noOcclusion().strength(2).randomTicks(), "brazier_ember");
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public AABB getBoundingBox(BlockState state, BlockGetter source, BlockPos pos) { return AABB; }

	@Override
	public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
		return state.getValue(LIT) ? 6 : 0;
	}

	@Override
	public boolean canPlaceBlockAt(Level worldIn, BlockPos pos) {
		return worldIn.getBlockState(pos.below()).isFaceSturdy(worldIn, pos.below(), Direction.UP);
	}

	@Override
	public void onBlockPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (!ItemTools.hasTag(stack)) {
			worldIn.setBlockAndUpdate(pos, state.setValue(LIT, false)); // 1.12: getStateFromMeta(0)
			return;
		}
		//noinspection ConstantConditions
		state = state.setValue(LIT, ItemTools.getTag(stack).getBoolean(LIT_TAG));
		worldIn.setBlockAndUpdate(pos, state);
	}

	@Override
	public ItemStack getPickBlock(BlockState state, HitResult target, Level world, BlockPos pos, Player player) {
		ItemStack pickStack = new ItemStack(this.asItem());
		ItemTools.setTag(pickStack, new NBTBuilder().setBoolean(LIT_TAG, state.getValue(LIT)).build());
		return pickStack;
	}
	@Override
	public boolean onBlockActivated(Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getItemInHand(hand);
		if (!state.getValue(LIT) && isFireStarter(stack)) {
			if (world.isClientSide) {
				return true;
			}

			world.setBlockAndUpdate(pos, state.setValue(LIT, true));
			if (stack.isDamageableItem()) {
				stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
			}
			world.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1, 1);

			return true;
		} else if (state.getValue(LIT) && stack.getItem() == Items.WATER_BUCKET) {
			if (world.isClientSide) {
				return true;
			}

			world.setBlockAndUpdate(pos, state.setValue(LIT, false));
			if (!player.getAbilities().instabuild) {
				player.setItemInHand(hand, new ItemStack(Items.BUCKET));
			}
			world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1);
		}

		return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
	}

	private boolean isFireStarter(ItemStack heldItem) {
		return heldItem.getItem() == Items.FLINT_AND_STEEL || heldItem.getItem() == Blocks.TORCH.asItem();
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
		ItemStack BlockBrazierEmber = new ItemStack(this);
		ItemTools.setTag(BlockBrazierEmber, new NBTBuilder().setBoolean(LIT_TAG, false).build());
		drops.add(BlockBrazierEmber);
	}

	@Override
	public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, RandomSource random) {
		if (!stateIn.getValue(LIT)) {
			return;
		}

		if (random.nextInt(10) == 0) {
			worldIn.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
		}

		for (int i = 0; i < 3; ++i) {
			double x = pos.getX() + 0.25D + random.nextDouble() * 0.5D;
			double y = pos.getY() + random.nextDouble() * 0.5D + 0.5D;
			double z = pos.getZ() + 0.25D + random.nextDouble() * 0.5D;
			worldIn.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
		}
	}

}
