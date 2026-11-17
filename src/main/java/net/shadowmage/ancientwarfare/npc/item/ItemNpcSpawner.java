package net.shadowmage.ancientwarfare.npc.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.NpcPlayerOwned;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFaction;
import net.shadowmage.ancientwarfare.npc.init.AWNPCEntities;
import net.shadowmage.ancientwarfare.npc.init.AWNPCItems;
import net.shadowmage.ancientwarfare.npc.registry.FactionRegistry;
import net.shadowmage.ancientwarfare.npc.registry.NpcDefaultsRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
 * Port notes: one item, the npc type / sub type / faction / stored npc data stay in the stack tag (custom data) with
 * the 1.12 keys. The 1.12 custom mesh definition (model variant per npc type) is the item property
 * "ancientwarfare:npc_variant" registered by the client (see getModelVariant and models/item/npc_spawner.json).
 */
public class ItemNpcSpawner extends ItemBaseNPC {

	private static final String NPC_TYPE_TAG = "npcType";
	private static final String NPC_SUBTYPE_TAG = "npcSubtype";
	private static final String FACTION_TAG = "faction";
	private static final String NPC_STORED_DATA_TAG = "npcStoredData";

	public ItemNpcSpawner() {
		super("npc_spawner", new Item.Properties().stacksTo(16));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		if (getNpcDisplayNameFromTag(stack).isPresent()) {
			tooltip.add(Component.translatable(getDescriptionId(stack)));
		}
		tooltip.add(Component.translatable("guistrings.npc.spawner.right_click_to_place"));
	}

	@Override
	public String getDescriptionId(ItemStack stack) {
		String npcName = getNpcType(stack);
		if (npcName != null) {
			npcName = npcName.replace("faction.", "");
			String npcSub = getNpcSubtype(stack);
			if (!npcSub.isEmpty()) {
				npcName = npcName + "." + npcSub;
			}
			return "entity.ancientwarfarenpc." + (getFaction(stack).map(s -> s + ".").orElse("")) + npcName + ".name";
		}
		return super.getDescriptionId(stack);
	}

	@Override
	public Component getName(ItemStack stack) {
		Optional<String> name = getNpcDisplayNameFromTag(stack);
		return name.isPresent() ? Component.literal(name.get()) : super.getName(stack);
	}

	private static Optional<String> getNpcDisplayNameFromTag(ItemStack stack) {
		if (ItemTools.hasTag(stack)) {
			CompoundTag tag = ItemTools.getTag(stack);
			if (tag.contains(NPC_STORED_DATA_TAG) && tag.getCompound(NPC_STORED_DATA_TAG).contains("name")) {
				return Optional.of(tag.getCompound(NPC_STORED_DATA_TAG).getString("name"));
			}
		}
		return Optional.empty();
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (world.isClientSide) {
			return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
		}
		BlockPos hit = BlockTools.getBlockClickedOn(player, player.level(), true);
		if (hit == null) {
			return new InteractionResultHolder<>(InteractionResult.PASS, stack);
		}
		NpcBase npc = createNpcFromItem(player.level(), stack);
		if (npc != null) {
			if (npc instanceof NpcPlayerOwned) {
				npc.setOwner(player);
			}
			npc.setPos(hit.getX() + 0.5d, hit.getY(), hit.getZ() + 0.5d);
			npc.setHomeAreaAtCurrentPosition();
			player.level().addFreshEntity(npc);
			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}
		}
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
	}

	/*
	 * create an NPC from the input item stack if valid, else return null<br>
	 * npc will have type, subtype, equipment, levels, health, food and owner set from item.
	 */
	public static NpcBase createNpcFromItem(Level world, ItemStack stack) {
		String type = getNpcType(stack);
		if (type == null) {
			return null;
		}
		String subType = getNpcSubtype(stack);
		Optional<String> faction = getFaction(stack);
		NpcBase npc = AWNPCEntities.createNpc(world, type, subType, faction.orElse(""));
		if (npc == null) {
			return null;
		}
		if (ItemTools.hasTag(stack) && ItemTools.getTag(stack).contains(NPC_STORED_DATA_TAG)) {
			for (int slot = 0; slot < NpcBase.ORDER_SLOT; slot++) {
				npc.setItemStackToSlot(slot, ItemStack.EMPTY);
			}
			npc.readAdditionalItemData(ItemTools.getTag(stack).getCompound(NPC_STORED_DATA_TAG));
		}
		return npc;
	}

	/*
	 * return an itemstack of npc spawner item that contains the data to spawn the input npc<br>
	 * npc type, subtype, equipment, levels health, food value, and owner will be stored.
	 */
	public static ItemStack getSpawnerItemForNpc(NpcBase npc) {
		String type = npc.getNpcType();
		String sub = npc.getNpcSubType();
		ItemStack stack = npc instanceof NpcFaction ? getStackForNpcType("faction." + type, sub, ((NpcFaction) npc).getFaction()) : getStackForNpcType(type, sub);
		CompoundTag tag = new CompoundTag();
		npc.writeAdditionalItemData(tag);
		ItemTools.setTagInfo(stack, NPC_STORED_DATA_TAG, tag);
		return stack;
	}

	/* 1.12 getSubItems */
	public static List<ItemStack> getSpawnerSubItems() {
		List<ItemStack> list = new ArrayList<>();
		List<ItemStack> playerOwned = new ArrayList<>();
		List<ItemStack> factionOwned = new ArrayList<>();
		for (AWNPCEntities.NpcDeclaration dec : AWNPCEntities.getNpcMap().values()) {
			if (dec.canSpawnBaseEntity()) {
				if (dec.getNpcType().startsWith("faction.")) {
					for (String factionName : FactionRegistry.getFactionNames()) {
						if (NpcDefaultsRegistry.getFactionNpcDefault(factionName, getShortNpcType(dec)).isEnabled()) {
							factionOwned.add(getStackForNpcType(dec.getNpcType(), "", factionName));
						}
					}
				} else {
					playerOwned.add(getStackForNpcType(dec.getNpcType(), "", ""));
				}
			}
			for (String sub : dec.getSubTypes()) {
				playerOwned.add(getStackForNpcType(dec.getNpcType(), sub));
			}
		}
		list.addAll(playerOwned);
		list.addAll(factionOwned);
		return list;
	}

	private static String getShortNpcType(AWNPCEntities.NpcDeclaration dec) {
		return dec.getNpcType().substring("faction.".length());
	}

	public static ItemStack getStackForNpcType(String type, String npcSubtype) {
		return getStackForNpcType(type, npcSubtype, "");
	}

	public static ItemStack getStackForNpcType(String type, String npcSubtype, String faction) {
		ItemStack stack = new ItemStack(AWNPCItems.NPC_SPAWNER.get());
		ItemTools.setTagInfo(stack, NPC_TYPE_TAG, StringTag.valueOf(type));
		ItemTools.setTagInfo(stack, NPC_SUBTYPE_TAG, StringTag.valueOf(npcSubtype));
		if (!faction.isEmpty()) {
			ItemTools.setTagInfo(stack, FACTION_TAG, StringTag.valueOf(faction));
		}
		return stack;
	}

	public static String getNpcType(ItemStack stack) {
		if (ItemTools.hasTag(stack) && ItemTools.getTag(stack).contains(NPC_TYPE_TAG)) {
			return ItemTools.getTag(stack).getString(NPC_TYPE_TAG);
		}
		return null;
	}

	private static String getNpcSubtype(ItemStack stack) {
		if (ItemTools.hasTag(stack) && ItemTools.getTag(stack).contains(NPC_SUBTYPE_TAG)) {
			return ItemTools.getTag(stack).getString(NPC_SUBTYPE_TAG);
		}
		return "";
	}

	public static Optional<String> getFaction(ItemStack stack) {
		if (ItemTools.hasTag(stack) && ItemTools.getTag(stack).contains(FACTION_TAG)) {
			return Optional.of(ItemTools.getTag(stack).getString(FACTION_TAG));
		}
		return Optional.empty();
	}

	/*
	 * 1.12 registerClient mesh definition: the model variant key of a stack (sub type if any, else the declaration's
	 * item model variant; "worker" declaration when the stack has no type); null when the type is unknown
	 */
	public static String getModelVariant(ItemStack stack) {
		String npcType = getNpcType(stack);
		if (npcType == null) {
			npcType = "worker";
		}
		AWNPCEntities.NpcDeclaration dec = AWNPCEntities.getNpcDeclaration(npcType);
		return dec == null ? null : dec.getItemModelVariant(getNpcSubtype(stack));
	}
}
