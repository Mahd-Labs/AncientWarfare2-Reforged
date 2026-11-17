package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.shadowmage.ancientwarfare.core.block.BlockBase;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;
import net.shadowmage.ancientwarfare.core.input.IItemKeyInterface;
import net.shadowmage.ancientwarfare.core.interfaces.IWorkSite;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/*
 * Port notes: ToolMaterial -> Tier. Attack damage / speed are default item attribute modifiers. The block rotation
 * calls BlockBase.rotateBlock for AW blocks and rotates the block state of other blocks (1.12 Forge Block.rotateBlock).
 */
public class ItemHammer extends ItemBaseCore implements IItemKeyInterface {
	public static final String WORK_MODE_TAG = "workMode";

	/*
	 * client hook: display name of the "alt item use 1" key (1.12: InputHandler.ALT_ITEM_USE_1.getDisplayName()).
	 * The client key handler installs the real key name.
	 */
	public static Supplier<Component> altItemUse1KeyName = () -> Component.translatable(AWCoreStatics.KEY_ALT_ITEM_USE_1);

	private double attackDamage = 5.d;

	private Tier material;

	public ItemHammer(String regName, Tier material) {
		super(regName, new Item.Properties().stacksTo(1).durability(material.getUses()).attributes(createAttributes(4.f + material.getAttackDamageBonus())));
		attackDamage = 4.f + material.getAttackDamageBonus();
		this.material = material;
	}

	private static ItemAttributeModifiers createAttributes(double attackDamage) {
		return ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, attackDamage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.1D, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.build();
	}

	public Tier getMaterial() {
		return material;
	}

	public double getAttackDamage() {
		return attackDamage;
	}

	@Override
	public int getEnchantmentValue() {
		return material.getEnchantmentValue();
	}

	@Override
	public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
		if (material.getRepairIngredient().test(repair))
			return true;
		return super.isValidRepairItem(toRepair, repair);
	}

	@Override
	public boolean hurtEnemy(ItemStack par1ItemStack, LivingEntity par2EntityLivingBase, LivingEntity par3EntityLivingBase) {
		return true;
	}

	@Override
	public void postHurtEnemy(ItemStack par1ItemStack, LivingEntity par2EntityLivingBase, LivingEntity par3EntityLivingBase) {
		par1ItemStack.hurtAndBreak(1, par3EntityLivingBase, EquipmentSlot.MAINHAND);
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity entityLiving) {
		if (state.getDestroySpeed(world, pos) != 0) {
			stack.hurtAndBreak(2, entityLiving, EquipmentSlot.MAINHAND);
		}
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		tooltip.add(Component.translatable("guistrings.core.hammer.use_primary_item_key", altItemUse1KeyName.get()));
		if (ItemTools.getTag(stack).getBoolean(WORK_MODE_TAG)) {
			tooltip.add(Component.translatable("guistrings.core.hammer.work_mode"));
		} else {
			tooltip.add(Component.translatable("guistrings.core.hammer.rotate_mode"));
		}
	}

	@Override
	public boolean onKeyActionClient(Player player, ItemStack stack, ItemAltFunction altFunction) {
		return altFunction == ItemAltFunction.ALT_FUNCTION_1;
	}

	@Override
	public void onKeyAction(Player player, ItemStack stack, ItemAltFunction altFunction) {
		if (player.level().isClientSide) {
			return;
		}
		boolean mode = ItemTools.getTag(stack).getBoolean(WORK_MODE_TAG);
		mode = !mode;
		ItemTools.setTagInfo(stack, WORK_MODE_TAG, net.minecraft.nbt.ByteTag.valueOf(mode));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.3F, 0.6F);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (world.isClientSide) {
			return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
		}
		BlockHitResult hit = getPlayerPOVHitResult(world, player, ClipContext.Fluid.NONE);
		if (hit.getType() == HitResult.Type.MISS) {
			return new InteractionResultHolder<>(InteractionResult.PASS, stack);
		}
		boolean mode = ItemTools.getTag(stack).getBoolean(WORK_MODE_TAG);
		if (!ItemTools.hasTag(stack)) {
			ItemTools.setTag(stack, new net.minecraft.nbt.CompoundTag());
		}
		if (mode) {
			Optional<IWorkSite> te = WorldTools.getTile(world, hit.getBlockPos(), IWorkSite.class).filter(IWorkSite::hasWork);
			if (te.isPresent()) {
				te.get().addEnergyFromPlayer(player);
				playSound(world, hit, SoundEvents.PISTON_CONTRACT);
			} else if (!world.isEmptyBlock(hit.getBlockPos())) {
				playBlockSound(world, hit, world.getBlockState(hit.getBlockPos()));
			}
		} else {
			if (!world.isEmptyBlock(hit.getBlockPos())) {
				BlockState state = world.getBlockState(hit.getBlockPos());
				boolean rotated = state.getBlock() instanceof BlockBase ? ((BlockBase) state.getBlock()).rotateBlock(world, hit.getBlockPos(), hit.getDirection()) :
						BlockBase.rotateVanillaBlock(world, hit.getBlockPos());
				if (rotated)
					playSound(world, hit, SoundEvents.PISTON_EXTEND);
				else
					playBlockSound(world, hit, state);
			}
		}
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
	}

	private void playSound(Level world, BlockHitResult hit, SoundEvent sound) {
		world.playSound(null, hit.getBlockPos(), sound, SoundSource.BLOCKS, 0.2F, world.random.nextFloat() * 0.15F + 0.6F);
	}

	private void playBlockSound(Level world, BlockHitResult hit, BlockState state) {
		SoundType sound = state.getSoundType(world, hit.getBlockPos(), null);
		if (sound != null) {
			world.playSound(null, hit.getBlockPos(), sound.getPlaceSound(), SoundSource.BLOCKS, sound.getVolume() * 0.5F, sound.getPitch() * 0.8F);
		}
	}
}
