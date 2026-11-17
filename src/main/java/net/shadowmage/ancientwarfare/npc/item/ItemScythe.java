package net.shadowmage.ancientwarfare.npc.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.util.BlockTools;

/*
 * Port notes: 1.12 isFarmable was "block instanceof IPlantable" (BlockBush and subclasses, cactus, reeds); IPlantable
 * is gone in 1.21.1, the same block classes are tested directly.
 */
public class ItemScythe extends ItemBaseNPC {
	private final Tier material;
	protected float attackSpeed;
	private static final int HARVEST_RADIUS = 2;

	public ItemScythe(Tier material, String registryName, float attackOffset, float attackSpeed) {
		super(registryName, new Item.Properties().stacksTo(1).durability(material.getUses()).attributes(weaponAttributes(material.getAttackDamageBonus() + attackOffset, attackSpeed)));
		this.material = material;
		this.attackSpeed = attackSpeed;
	}

	@Override
	public int getEnchantmentValue() {
		return this.material.getEnchantmentValue();
	}

	@SuppressWarnings("squid:S1186") //used in child class
	protected void applyPotionEffect(LivingEntity target) {
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		applyPotionEffect(target);
		return true;
	}

	/*
	 * Port notes: 1.12 Item.onBlockStartBreak (called before the block is broken, "true" cancelled the normal break)
	 * does not exist in 1.21.1. The area harvest runs from mineBlock, i.e. right after the clicked block was broken:
	 * same result when the clicked block is a plant; when it is not, 1.12 kept the clicked block and harvested only
	 * the plants around it, here the clicked block is broken as well.
	 */
	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
		if (!level.isClientSide && miningEntity instanceof Player) {
			onBlockStartBreak(stack, pos, (Player) miningEntity);
		}
		return true;
	}

	public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, Player player) {
		Level world = player.level();

		boolean harvestedSomething = false;
		for (BlockPos mutablePos : BlockPos.betweenClosed(pos.offset(-HARVEST_RADIUS, -HARVEST_RADIUS, -HARVEST_RADIUS), pos.offset(HARVEST_RADIUS, HARVEST_RADIUS, HARVEST_RADIUS))) {
			BlockPos currPos = mutablePos.immutable();
			BlockState currState = world.getBlockState(currPos);
			Block currBlock = currState.getBlock();
			if (isFarmable(currBlock)) {
				harvestedSomething = true;
				BlockTools.breakBlockAndDrop(world, currPos);
				world.levelEvent(player, 2001, currPos, Block.getId(currState));
			}
		}

		if (!harvestedSomething) {
			return false;
		}
		player.getMainHandItem().hurtAndBreak(2, player, EquipmentSlot.MAINHAND);
		return true;
	}

	private boolean isFarmable(Block block) {
		return block instanceof BushBlock || block instanceof CactusBlock || block instanceof SugarCaneBlock;
	}
}
