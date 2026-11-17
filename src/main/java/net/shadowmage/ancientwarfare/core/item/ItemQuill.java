package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.interfaces.IWorkSite;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

import java.util.List;

public class ItemQuill extends ItemBaseCore {
	private double attackDamage = 5.d;
	private Tier material;

	public ItemQuill(String regName, Tier material) {
		super(regName, new Item.Properties().stacksTo(1).durability(material.getUses()).attributes(createAttributes(1.f + material.getAttackDamageBonus())));
		this.material = material;
		attackDamage = 1.f + material.getAttackDamageBonus();
	}

	private static ItemAttributeModifiers createAttributes(double attackDamage) {
		return ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, attackDamage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.build();
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		tooltip.add(Component.translatable("guistrings.core.quill.work_mode"));
	}

	public Tier getMaterial() {
		return material;
	}

	public double getAttackDamage() {
		return attackDamage;
	}

	@Override
	public int getEnchantmentValue() {
		return this.material.getEnchantmentValue();
	}

	@Override
	public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
		if (this.material.getRepairIngredient().test(repair))
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
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (world.isClientSide) {
			return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
		}
		BlockPos pos = BlockTools.getBlockClickedOn(player, world, false);
		if (pos != null) {
			WorldTools.getTile(world, pos, IWorkSite.class).filter(t -> t.getWorkType() == IWorkSite.WorkType.RESEARCH).ifPresent(t -> addResearchEnergy(player, stack, t, hand));
		}
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
	}

	private void addResearchEnergy(Player player, ItemStack stack, IWorkSite teResearchStation, InteractionHand hand) {
		if (teResearchStation.hasWork()) {
			teResearchStation.addEnergyFromPlayer(player);
			stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
		}
	}
}
