package net.shadowmage.ancientwarfare.npc.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.phys.EntityHitResult;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import net.shadowmage.ancientwarfare.core.input.IItemKeyInterface;
import net.shadowmage.ancientwarfare.core.input.IScrollableItem;
import net.shadowmage.ancientwarfare.core.input.InputHandler;
import net.shadowmage.ancientwarfare.core.util.NBTBuilder;
import net.shadowmage.ancientwarfare.core.util.RayTraceUtils;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.npc_command.NpcCommand;
import net.shadowmage.ancientwarfare.npc.npc_command.NpcCommand.CommandType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ItemCommandBaton extends ItemBaseNPC implements IItemKeyInterface, IScrollableItem {

	private final double attackDamage;
	int range = 120;//TODO set range from config
	private final net.minecraft.world.item.Tier material;

	/*
	 * Port notes: the 1.12 tooltip also wrote a default "mode" tag into the stack (a side effect that is not possible
	 * on the immutable-by-convention 1.21 tooltip stacks); getMode falls back to CLEAR_COMMAND without the tag, which
	 * is what the 1.12 tag resolved to as well. Key names come through the client hook of core's ItemHammer.
	 */
	public ItemCommandBaton(String name, net.minecraft.world.item.Tier material) {
		super(name, new Item.Properties().stacksTo(1).durability(material.getUses()).attributes(weaponAttributes(4 + material.getAttackDamageBonus(), -2.3D)));
		attackDamage = 4 + material.getAttackDamageBonus();
		this.material = material;
	}

	@Override
		public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {

		tooltip.add(Component.literal("RMB = ").append(Component.translatable("guistrings.npc.baton.add_remove")));

		Component keyText = net.shadowmage.ancientwarfare.core.item.ItemHammer.altItemUse1KeyName.get();
		tooltip.add(Component.empty().append(keyText).append(" = Execute Command: ").append(Component.translatable(getMode(stack).getTranslationKey())));

		tooltip.add(Component.literal("Use scroll wheel to change mode when sneaking"));
	}

	/*
	 * Return the enchantability factor of the item.
	 */
	@Override
	public int getEnchantmentValue() {
		return material.getEnchantmentValue();
	}

	/*
	 * Return whether this item is repairable in an anvil.
	 */
	@Override
	public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
		if (material.getRepairIngredient().test(repair)) { return true; }
		return super.isValidRepairItem(toRepair, repair);
	}

	/*
	 * Raise the damage on the stack.
	 */
	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity attacked, LivingEntity wielder) {
		stack.hurtAndBreak(1, wielder, EquipmentSlot.MAINHAND);
		return true;
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity entityLiving) {
		if (state.getDestroySpeed(world, pos) != 0) {
			stack.hurtAndBreak(2, entityLiving, EquipmentSlot.MAINHAND);
		}
		return true;
	}

	public double getAttackDamage() {
		return attackDamage;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (world.isClientSide) {
			return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
		}
		if (player.isShiftKeyDown()) {
			//TODO openGUI
		} else {
			HitResult pos = RayTraceUtils.getPlayerTarget(player, range, 0);
			if (pos != null && pos.getType() == HitResult.Type.ENTITY && ((EntityHitResult) pos).getEntity() instanceof NpcBase) {
				NpcBase npc = (NpcBase) ((EntityHitResult) pos).getEntity();
				if (npc.hasCommandPermissions(player.getUUID(), player.getName().getString())) {
					onNpcClicked(npc, stack);
				}
			}
		}
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
	}

	@Override
	public void onKeyAction(Player player, ItemStack stack, ItemAltFunction altFunction) {
		//noop ...or...??
	}

	@Override
	public boolean onKeyActionClient(Player player, ItemStack stack, ItemAltFunction altFunction) {
		if (altFunction == ItemAltFunction.ALT_FUNCTION_1) {
			switch (getMode(stack)) {
				case CLEAR_COMMAND: {
					HitResult hit = new EntityHitResult(player);
					CommandType c = CommandType.CLEAR_COMMAND;
					NpcCommand.handleCommandClient(c, hit);
				}
				break;
				case ATTACK: {
					HitResult hit = RayTraceUtils.getPlayerTarget(player, range, 0);
					if (hit != null) {
						CommandType c = hit.getType() == HitResult.Type.ENTITY ? CommandType.ATTACK : CommandType.ATTACK_AREA;
						NpcCommand.handleCommandClient(c, hit);
					}
				}
				break;
				case MOVE: {
					HitResult hit = RayTraceUtils.getPlayerTarget(player, range, 0);
					if (hit != null) {
						CommandType c = hit.getType() == HitResult.Type.ENTITY ? CommandType.GUARD : CommandType.MOVE;
						NpcCommand.handleCommandClient(c, hit);
					}
				}
				break;
				case SET_HOME: {
					HitResult hit = RayTraceUtils.getPlayerTarget(player, range, 0);
					if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
						CommandType c = CommandType.SET_HOME;
						NpcCommand.handleCommandClient(c, hit);
					}
				}
				break;
				case CLEAR_HOME: {
					HitResult hit = RayTraceUtils.getPlayerTarget(player, range, 0);
					CommandType c = CommandType.CLEAR_HOME;
					NpcCommand.handleCommandClient(c, hit);
				}
				break;
				case SET_UPKEEP: {
					HitResult hit = RayTraceUtils.getPlayerTarget(player, range, 0);
					if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
						CommandType c = CommandType.SET_UPKEEP;
						NpcCommand.handleCommandClient(c, hit);
					}
				}
				break;
				case CLEAR_UPKEEP: {
					HitResult hit = RayTraceUtils.getPlayerTarget(player, range, 0);
					CommandType c = CommandType.CLEAR_UPKEEP;
					NpcCommand.handleCommandClient(c, hit);
				}
				break;
			}
		}
		return false;
	}

	private void onNpcClicked(NpcBase npc, ItemStack stack) {
		CommandSet.loadFromStack(stack).onNpcClicked(npc, stack);
	}

	/*
	 * client hook (installed by AncientWarfareNPCClient): every entity the client level knows, 1.12
	 * WorldClient.loadedEntityList. Common code must not name ClientLevel.
	 */
	public static java.util.function.Function<Level, Iterable<Entity>> clientEntities = level -> java.util.Collections.emptyList();

	public static List<Entity> getCommandedEntities(Level world, ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof ItemCommandBaton)) {
			return new ArrayList<>();
		}
		return CommandSet.loadFromStack(stack).getEntities(world);
	}

	@Override
	public boolean onScrollUp(Level world, Player player, ItemStack stack) {
		if (!world.isClientSide) {
			changeMode(getMode(stack).next(), player, stack);
		}
		return true;
	}

	@Override
	public boolean onScrollDown(Level world, Player player, ItemStack stack) {
		if (!world.isClientSide) {
			changeMode(getMode(stack).previous(), player, stack);
		}
		return true;
	}

	/*
	 * relies on NPCs transmitting their unique entity-id to client-side<br>
	 *
	 * @author Shadowmage
	 */
	private static class CommandSet {

		private Set<UUID> ids = new HashSet<>();

		private CommandSet() {
		}

		static CommandSet loadFromStack(ItemStack stack) {
			CommandSet set = new CommandSet();
			//noinspection ConstantConditions
			if (ItemTools.hasTag(stack) && ItemTools.getTag(stack).contains("entityList")) {
				set.readFromNBT(ItemTools.getTag(stack).getCompound("entityList"));
			}
			return set;
		}

		private void writeToStack(ItemStack stack) {
			ItemTools.setTagInfo(stack,"entityList", writeToNBT());
		}

		private void readFromNBT(CompoundTag tag) {
			ListTag entryList = tag.getList("entryList", Tag.TAG_COMPOUND);
			CompoundTag idTag;
			for (int i = 0; i < entryList.size(); i++) {
				idTag = entryList.getCompound(i);
				ids.add(idTag.getUUID("uuid"));
			}
		}

		private CompoundTag writeToNBT() {
			CompoundTag tag = new CompoundTag();
			ListTag entryList = new ListTag();
			CompoundTag idTag;
			for (UUID id : ids) {
				idTag = new CompoundTag();
				idTag.putUUID("uuid", id);
				entryList.add(idTag);
			}
			tag.put("entryList", entryList);

			return tag;
		}

		void onNpcClicked(NpcBase npc, ItemStack stack) {
			if (ids.contains(npc.getUUID())) {
				ids.remove(npc.getUUID());
			} else {
				ids.add(npc.getUUID());
			}
			validateEntities(npc.level());
			writeToStack(stack);
		}

		List<Entity> getEntities(Level world) {
			List<Entity> in = Lists.newArrayList();
			if (world instanceof ServerLevel) {
				ServerLevel worldServer = (ServerLevel) world;
				for (UUID id : ids) {
					Entity e = worldServer.getEntity(id);
					if (e != null) {
						in.add(e);
					}
				}
			} else if (world.isClientSide && !ids.isEmpty()) {
				//1.12 iterated WorldClient.loadedEntityList. The common Level only offers area queries, and an area query
				//walks every section column of its box (EntitySectionStorage.forEachAccessibleNonEmptySection), so a box
				//over the whole world costs millions of lookups per call; the client entry point supplies the entity list
				for (Entity entity : clientEntities.apply(world)) {
					if (ids.contains(entity.getUUID())) {
						in.add(entity);
					}
				}
			}
			return in;
		}

		/*
		 * should be called server side to clear out any old un-findable entity references.<br>
		 * should probably only be called on-right click, as operation may be costly
		 */
		private void validateEntities(Level world) {
			if (world instanceof ServerLevel) {
				ServerLevel worldServer = (ServerLevel) world;
				Iterator<UUID> it = ids.iterator();
				UUID id;
				while (it.hasNext()) {
					id = it.next();
					if (id == null || worldServer.getEntity(id) == null) {
						it.remove();
					}
				}
			}
		}
	}

	private void changeMode(BatonMode mode, Player player, ItemStack stack) {
		player.sendSystemMessage(Component.translatable(mode.getTranslationKey()));
		ItemTools.setTagInfo(stack,"mode", StringTag.valueOf(mode.getName()));
	}

	private BatonMode getMode(ItemStack stack) {
		assert ItemTools.getTag(stack) != null;
		return ItemTools.hasTag(stack) ? BatonMode.fromName(ItemTools.getTag(stack).getString("mode")) : BatonMode.CLEAR_COMMAND;
	}

	public enum BatonMode {
		CLEAR_COMMAND("clear", "guistrings.npc.baton.clear"),
		ATTACK("attack", "guistrings.npc.baton.attack"),
		MOVE("move", "guistrings.npc.baton.move"),
		SET_HOME("setHome", "guistrings.npc.baton.set_home"),
		CLEAR_HOME("clearHome", "guistrings.npc.baton.clear_home"),
		SET_UPKEEP("setUpkeep", "guistrings.npc.baton.set_upkeep"),
		CLEAR_UPKEEP("clearUpkeep", "guistrings.npc.baton.clear_upkeep");

		private String name;
		final String key;

		BatonMode(String name, String key) {
			this.name = name;
			this.key = key;
		}

		public String getTranslationKey() {
			return key;
		}

		public static BatonMode getDefault() {
			return CLEAR_COMMAND;
		}

		public BatonMode next() {
			int ordinal = ordinal() + 1;
			if (ordinal >= BatonMode.values().length) {
				ordinal = 0;
			}
			return BatonMode.values()[ordinal];
		}

		public BatonMode previous() {
			int ordinal = ordinal() - 1;
			if (ordinal < 0) {
				ordinal = BatonMode.values().length - 1;
			}
			return BatonMode.values()[ordinal];
		}

		private static final ImmutableMap<String, BatonMode> NAME_TO_MODE;

		static {
			ImmutableMap.Builder<String, BatonMode> builder = new ImmutableMap.Builder<>();
			for (BatonMode mode : values()) {
				builder.put(mode.name, mode);
			}
			NAME_TO_MODE = builder.build();
		}

		public static BatonMode fromName(String name) {
			return NAME_TO_MODE.getOrDefault(name, CLEAR_COMMAND);
		}

		public String getName() {
			return name;
		}
	}
}
