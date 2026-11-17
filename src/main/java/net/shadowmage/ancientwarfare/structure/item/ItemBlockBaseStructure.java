package net.shadowmage.ancientwarfare.structure.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;
import net.shadowmage.ancientwarfare.structure.util.MultiBlockHelper;

import javax.annotation.Nullable;

/*
 * Port notes: bridges the 1.12 ItemBlock callbacks the structure item blocks were written against:
 *   placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState) -> BlockItem.placeBlock(context, state)
 *     (the subclass code after super.placeBlockAt runs right after the block is set; Block.onBlockPlacedBy runs
 *     after it in 1.21, before it in 1.12)
 *   canPlaceBlockOnSide(world, pos, side, player, stack) and the MultiBlockHelper.IPlaceChecker the multi block
 *     items passed to MultiBlockHelper.onMultiBlockItemUse -> BlockItem.canPlace(context, state)
 *   getUnlocalizedName(stack) ("tile.<name>[...]", without the ".name" suffix 1.12 appended) -> getDescriptionId(stack)
 *   getItemStackDisplayName(stack) : String -> getName(stack) (null = the normal translated name)
 */
public class ItemBlockBaseStructure extends ItemBlockBase {
	private BlockPlaceContext currentContext;

	public ItemBlockBaseStructure(Block block) {
		super(block);
	}

	public ItemBlockBaseStructure(Block block, Item.Properties properties) {
		super(block, properties);
	}

	/* 1.12 ItemBlock.placeBlockAt */
	public boolean placeBlockAt(ItemStack stack, Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, BlockState newState) {
		return currentContext != null && super.placeBlock(currentContext, newState);
	}

	@Override
	protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
		BlockPlaceContext previous = currentContext;
		currentContext = context;
		try {
			BlockPos pos = context.getClickedPos();
			Vec3 loc = context.getClickLocation();
			return placeBlockAt(context.getItemInHand(), context.getPlayer(), context.getLevel(), pos, context.getClickedFace(),
					(float) (loc.x - pos.getX()), (float) (loc.y - pos.getY()), (float) (loc.z - pos.getZ()), state);
		}
		finally {
			currentContext = previous;
		}
	}

	/* 1.12 ItemBlock.canPlaceBlockOnSide */
	public boolean canPlaceBlockOnSide(Level world, BlockPos pos, Direction side, @Nullable Player player, ItemStack stack) {
		return true;
	}

	/* the checker the 1.12 item passed to MultiBlockHelper.onMultiBlockItemUse */
	@Nullable
	protected MultiBlockHelper.IPlaceChecker getPlaceChecker() {
		return null;
	}

	@Override
	protected boolean canPlace(BlockPlaceContext context, BlockState state) {
		if (!super.canPlace(context, state)) {
			return false;
		}
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();
		if (!canPlaceBlockOnSide(world, pos, context.getClickedFace(), player, context.getItemInHand())) {
			return false;
		}
		MultiBlockHelper.IPlaceChecker checker = getPlaceChecker();
		return checker == null || player == null || checker.mayPlace(world, pos, context.getClickedFace(), player);
	}

	/* 1.12 Item.getUnlocalizedName(stack): the translation key without the ".name" suffix */
	public String getUnlocalizedName(ItemStack stack) {
		return getLegacyName(stack);
	}

	/* 1.12 Item.getUnlocalizedNameInefficiently(stack) */
	protected final String getLegacyName(ItemStack stack) {
		String key = super.getDescriptionId(stack);
		return key.endsWith(".name") ? key.substring(0, key.length() - 5) : key;
	}

	@Override
	public String getDescriptionId(ItemStack stack) {
		return getUnlocalizedName(stack) + ".name";
	}

	/* 1.12 Item.getItemStackDisplayName; null = translate getDescriptionId(stack) */
	@Nullable
	public String getItemStackDisplayName(ItemStack stack) {
		return null;
	}

	@Override
	public Component getName(ItemStack stack) {
		String name = getItemStackDisplayName(stack);
		return name == null ? super.getName(stack) : Component.literal(name);
	}
}
