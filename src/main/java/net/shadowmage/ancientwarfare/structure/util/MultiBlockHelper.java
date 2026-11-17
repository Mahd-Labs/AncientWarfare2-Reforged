package net.shadowmage.ancientwarfare.structure.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

/*
 * Port notes: 1.12 onMultiBlockItemUse re-implemented ItemBlock.onItemUse with an extra place check. The vanilla
 * 1.21 BlockItem.place flow does the rest (replaceable target, placement state, place sound, shrinking the stack),
 * so only the checker is left: ItemBlockBaseStructure.canPlace calls it.
 */
public class MultiBlockHelper {
	private MultiBlockHelper() {}

	/*
	 * 1.12 mayPlaceAt of the multi block items (a copy of World.mayPlace for the extra positions): nothing in the
	 * way and the block there is replaceable
	 */
	public static boolean mayPlaceAt(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.canBeReplaced() && world.isUnobstructed(state, pos, CollisionContext.empty()) && world.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(pos)).isEmpty();
	}

	public interface IPlaceChecker {
		boolean mayPlace(Level world, BlockPos pos, Direction sidePlacedOn, Player placer);
	}
}
