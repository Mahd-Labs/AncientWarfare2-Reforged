package net.shadowmage.ancientwarfare.structure.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.item.ItemBase;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;

/*
 * Port notes: the 1.12 client registration (item model at models/item/structure/<name>) is plain resource naming
 * now (models/item/<name>.json); the creative tab content is listed by AWStructureTab.
 * Bridges 1.12 Item.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ) -> useOn(UseOnContext).
 */
public class ItemBaseStructure extends ItemBase {
	public ItemBaseStructure(String regName) {
		super(AncientWarfareStructure.MOD_ID, regName);
	}

	public ItemBaseStructure(String regName, Item.Properties properties) {
		super(AncientWarfareStructure.MOD_ID, regName, properties);
	}

	/* 1.12 Item.onItemUse */
	public InteractionResult onItemUse(Player player, Level world, BlockPos pos, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		return InteractionResult.PASS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		BlockPos pos = context.getClickedPos();
		Vec3 loc = context.getClickLocation();
		return onItemUse(player, context.getLevel(), pos, context.getHand(), context.getClickedFace(),
				(float) (loc.x - pos.getX()), (float) (loc.y - pos.getY()), (float) (loc.z - pos.getZ()));
	}
}
