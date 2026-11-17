package net.shadowmage.ancientwarfare.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.input.IItemKeyInterface.ItemAltFunction;
import net.shadowmage.ancientwarfare.core.network.PacketBase;
import net.shadowmage.ancientwarfare.core.network.PacketBlockEvent;
import net.shadowmage.ancientwarfare.core.network.PacketEntity;
import net.shadowmage.ancientwarfare.core.network.PacketGui;
import net.shadowmage.ancientwarfare.core.network.PacketItemInteraction;
import net.shadowmage.ancientwarfare.core.network.PacketItemMouseScroll;
import net.shadowmage.ancientwarfare.core.network.PacketManualReload;
import net.shadowmage.ancientwarfare.core.network.PacketResearchInit;
import net.shadowmage.ancientwarfare.core.network.PacketResearchStart;
import net.shadowmage.ancientwarfare.core.network.PacketResearchUpdate;
import net.shadowmage.ancientwarfare.core.research.ResearchData;
import net.shadowmage.ancientwarfare.npc.faction.FactionEntry;
import net.shadowmage.ancientwarfare.npc.network.PacketExtendedReachAttack;
import net.shadowmage.ancientwarfare.npc.network.PacketFactionUpdate;
import net.shadowmage.ancientwarfare.npc.network.PacketNpcCommand;
import net.shadowmage.ancientwarfare.npc.network.PacketTeamMembershipUpdate;
import net.shadowmage.ancientwarfare.npc.network.PacketTeamStandingUpdate;
import net.shadowmage.ancientwarfare.npc.network.PacketTeamStandingsUpdate;
import net.shadowmage.ancientwarfare.npc.npc_command.NpcCommand.CommandType;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureEntry;
import net.shadowmage.ancientwarfare.structure.network.PacketHighlightBlock;
import net.shadowmage.ancientwarfare.structure.network.PacketShowBoundingBoxes;
import net.shadowmage.ancientwarfare.structure.network.PacketSoundBlockPlayerSpecValues;
import net.shadowmage.ancientwarfare.structure.network.PacketStructure;
import net.shadowmage.ancientwarfare.structure.network.PacketStructureEntry;
import net.shadowmage.ancientwarfare.structure.network.PacketStructureRemove;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.util.BlockHighlightInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/*
 * Network checks that need no client. Single player never encodes packets, so a packet whose reader does not take
 * exactly what its writer wrote only fails when a real client joins a dedicated server (1.21 disconnects on it).
 */
@GameTestHolder(AncientWarfare.MOD_ID)
@PrefixGameTestTemplate(false)
public class PacketGameTests {

	/* NBT of the given size that does not compress well, like real template / research data of that size */
	private static CompoundTag tag(int longs) {
		CompoundTag tag = new CompoundTag();
		Random random = new Random(longs);
		for (int i = 0; i < longs; i++) {
			tag.putLong("k" + i, random.nextLong());
		}
		tag.putString("name", "sample");
		return tag;
	}

	private static void set(Object target, String field, Object value) {
		try {
			Field f = target.getClass().getDeclaredField(field);
			f.setAccessible(true);
			f.set(target, value);
		}
		catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	/*
	 * Every registered packet type: a populated instance is encoded, decoded and encoded again. The decoder has to
	 * read exactly what was written and the second encoding has to give the same bytes. A packet class without a
	 * sample here fails the test, so new packets cannot be forgotten.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = "core_empty", timeoutTicks = 100)
	public static void everyPacketRoundTrips(GameTestHelper helper) {
		Entity pig = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 2));
		List<PacketBase> samples = new ArrayList<>();
		BlockPos pos = new BlockPos(-12345, 70, 67890);
		ResourceLocation team = ResourceLocation.fromNamespaceAndPath("ancientwarfare", "test_team");

		samples.add(new PacketBlockEvent(pos, Blocks.STONE, (short) 3, (short) 250));
		PacketEntity entity = new PacketEntity(pig);
		entity.packetData = tag(4);
		samples.add(entity);
		PacketEntity bigEntity = new PacketEntity(pig);
		bigEntity.packetData = tag(400);
		samples.add(bigEntity);
		samples.add(new PacketGui(tag(4)));
		samples.add(new PacketGui(tag(400)));
		samples.add(new net.shadowmage.ancientwarfare.core.network.PacketGuiToClient(new PacketGui(tag(400))));
		//3 MiB of GUI data from the server (a big warehouse) reads on the client
		CompoundTag warehouse = new CompoundTag();
		warehouse.putByteArray("data", new byte[3 * 1024 * 1024]);
		samples.add(new net.shadowmage.ancientwarfare.core.network.PacketGuiToClient(new PacketGui(warehouse)));
		samples.add(new PacketItemInteraction(ItemAltFunction.values()[ItemAltFunction.values().length - 1]));
		samples.add(new PacketItemMouseScroll(true));
		samples.add(new PacketManualReload());
		samples.add(new net.shadowmage.ancientwarfare.core.network.PacketTransferRecipe(new net.shadowmage.ancientwarfare.core.crafting.RecipeResourceLocation(net.shadowmage.ancientwarfare.core.crafting.RecipeResourceLocation.RecipeType.RESEARCH, ResourceLocation.fromNamespaceAndPath(AncientWarfare.MOD_ID, "engineering_station"))));
		samples.add(new PacketResearchInit(new ResearchData("AWResearchData")));
		PacketResearchInit bigResearch = new PacketResearchInit();
		set(bigResearch, "researchDataTag", tag(2000));
		samples.add(bigResearch);
		samples.add(new PacketResearchStart("AwTestOwner", "engineering", true));
		samples.add(new PacketResearchUpdate("AwTestOwner", "engineering", true, false));

		samples.add(new PacketHighlightBlock(new BlockHighlightInfo(pos, 123456789L)));
		samples.add(new PacketShowBoundingBoxes(true));
		PacketSoundBlockPlayerSpecValues sound = new PacketSoundBlockPlayerSpecValues();
		set(sound, "tilePos", pos);
		set(sound, "stopped", true);
		set(sound, "lastTimePlayerNear", 987654321L);
		set(sound, "numberOfTimesRepeated", 7);
		samples.add(sound);
		for (int size : new int[] {0, 4, 60, 400, 5000}) {
			PacketStructure structure = new PacketStructure();
			structure.packetData = tag(size);
			samples.add(structure);
		}
		//the packet a joining client gets: the names of all loaded templates (the one that broke joining)
		PacketStructure templateList = new PacketStructure();
		java.util.Set<String> names = new java.util.TreeSet<>(net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager.getTemplates());
		templateList.packetData.put("structureList", net.shadowmage.ancientwarfare.core.util.NBTHelper.getNBTStringList(names));
		templateList.packetData.put("survivalStructures", net.shadowmage.ancientwarfare.core.util.NBTHelper.getNBTStringList(names));
		samples.add(templateList);
		samples.add(new PacketStructureEntry("minecraft:overworld", -3, 9, new StructureEntry(new StructureBB(pos, pos.offset(20, 10, 20)), "GameTestStructure", 5, -3, 9)));
		samples.add(new PacketStructureRemove("GameTestStructure"));
		samples.add(new net.shadowmage.ancientwarfare.structure.network.PacketStructureRequest("GameTestStructure"));

		net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase vehicle = new net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase(helper.getLevel());
		samples.add(new net.shadowmage.ancientwarfare.vehicle.network.PacketAimUpdate(vehicle, java.util.Optional.of(12.5F), java.util.Optional.empty(), java.util.Optional.of(31F)));
		samples.add(new net.shadowmage.ancientwarfare.vehicle.network.PacketAimUpdate(vehicle, java.util.Optional.empty(), java.util.Optional.of(-270.25F), java.util.Optional.empty()));
		samples.add(new net.shadowmage.ancientwarfare.vehicle.network.PacketAmmoSelect(vehicle, "ancientwarfare:ammo_stone_shot_10"));
		CompoundTag ammoTag = new CompoundTag();
		ammoTag.put("list", vehicle.ammoHelper.serializeNBT().getList("list", net.minecraft.nbt.Tag.TAG_COMPOUND));
		samples.add(new net.shadowmage.ancientwarfare.vehicle.network.PacketAmmoUpdate(vehicle, ammoTag));
		samples.add(new net.shadowmage.ancientwarfare.vehicle.network.PacketFireUpdate(vehicle));
		samples.add(new net.shadowmage.ancientwarfare.vehicle.network.PacketPackCommand(vehicle));
		samples.add(new net.shadowmage.ancientwarfare.vehicle.network.PacketSingleAmmoUpdate(vehicle, "ancientwarfare:ammo_stone_shot_10", 37));
		samples.add(new net.shadowmage.ancientwarfare.vehicle.network.PacketVehicleMove(vehicle, -123.456f));
		samples.add(new net.shadowmage.ancientwarfare.vehicle.network.PacketVehicleInput(vehicle, (byte) -1));
		samples.add(new net.shadowmage.ancientwarfare.vehicle.network.PacketOpenAmmoSelection(vehicle));

		samples.add(new PacketExtendedReachAttack(pig.getId()));
		samples.add(new PacketFactionUpdate(tag(4)));
		samples.add(new PacketFactionUpdate(tag(400)));
		for (CommandType type : CommandType.values()) {
			samples.add(new PacketNpcCommand(type, pos));
			samples.add(new PacketNpcCommand(type, pig));
		}
		samples.add(new PacketTeamMembershipUpdate(team, "AwTestOwner", PacketTeamMembershipUpdate.Action.ADD));
		samples.add(new PacketTeamMembershipUpdate(team, "AwTestOwner", PacketTeamMembershipUpdate.Action.REMOVE));
		samples.add(new PacketTeamStandingUpdate(team, "bandit", -25));
		samples.add(new PacketTeamStandingsUpdate(team, new FactionEntry()));

		Set<Class<?>> covered = new HashSet<>();
		List<String> problems = new ArrayList<>();
		for (PacketBase sample : samples) {
			covered.add(sample.getClass());
			String problem = PacketBase.roundTripProblem(sample, helper.getLevel().registryAccess());
			if (problem != null) {
				problems.add(sample.getClass().getSimpleName() + ": " + problem);
			}
		}
		for (Class<?> registered : PacketBase.registeredClasses()) {
			if (!covered.contains(registered)) {
				problems.add(registered.getSimpleName() + ": registered packet without a sample in this test");
			}
		}
		if (!problems.isEmpty()) {
			helper.fail(problems.size() + " packet problems: " + String.join("; ", problems));
		}
		helper.succeed();
	}

	/*
	 * a tiny packet that announces a 2 GiB byte array inside its compressed NBT is refused before anything is
	 * allocated (the NBT of a packet is read with a size limit), and a normal tag still reads.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = "core_empty", timeoutTicks = 100)
	public static void oversizedPacketNbtIsRefused(GameTestHelper helper) throws java.io.IOException {
		java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream();
		try (java.io.DataOutputStream out = new java.io.DataOutputStream(new java.util.zip.GZIPOutputStream(raw))) {
			out.writeByte(10); //root compound
			out.writeUTF("");
			out.writeByte(7); //byte array "a" with a length of Integer.MAX_VALUE and no data
			out.writeUTF("a");
			out.writeInt(Integer.MAX_VALUE);
		}
		io.netty.buffer.ByteBuf hostile = io.netty.buffer.Unpooled.buffer();
		net.minecraft.network.VarInt.write(hostile, raw.size());
		hostile.writeBytes(raw.toByteArray());
		long before = Runtime.getRuntime().totalMemory();
		boolean refused = false;
		try {
			net.shadowmage.ancientwarfare.core.util.PacketHelper.readNBTTag(hostile);
		}
		catch (RuntimeException e) {
			refused = true;
		}
		if (!refused) {
			helper.fail("a packet announcing a 2 GiB array was read (" + raw.size() + " bytes on the wire)");
		}
		if (Runtime.getRuntime().totalMemory() - before > 1L << 30) {
			helper.fail("reading the hostile packet grew the heap by more than 1 GiB");
		}
		//data the server sent is read without a limit (templates are far above any sensible client limit)
		CompoundTag big = new CompoundTag();
		big.putByteArray("data", new byte[80 * 1024 * 1024]);
		io.netty.buffer.ByteBuf fromServer = io.netty.buffer.Unpooled.buffer();
		net.shadowmage.ancientwarfare.core.util.PacketHelper.writeNBTTag(fromServer, big);
		if (net.shadowmage.ancientwarfare.core.util.PacketHelper.readServerNBTTag(fromServer).getByteArray("data").length != 80 * 1024 * 1024) {
			helper.fail("80 MiB of NBT sent by the server did not read back");
		}
		fromServer.release();
		io.netty.buffer.ByteBuf normal = io.netty.buffer.Unpooled.buffer();
		net.shadowmage.ancientwarfare.core.util.PacketHelper.writeNBTTag(normal, tag(400));
		if (!net.shadowmage.ancientwarfare.core.util.PacketHelper.readNBTTag(normal).equals(tag(400))) {
			helper.fail("a normal tag no longer reads back");
		}
		helper.succeed();
	}

	/*
	 * a GUI request sent by a client is refused unless it switches between the GUIs of the block or
	 * entity whose GUI the player has open. Here the player has nothing open and asks for the creative controls of
	 * an NPC next to it and for a spawner inventory where there is no spawner (which crashed the server).
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = "core_empty", timeoutTicks = 100)
	public static void clientCannotOpenArbitraryGui(GameTestHelper helper) {
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		net.shadowmage.ancientwarfare.npc.entity.NpcBase npc = net.shadowmage.ancientwarfare.npc.init.AWNPCEntities.createNpc(helper.getLevel(), "combat", "soldier", "");
		npc.setOwnerName("alice");
		npc.setPos(at.getX() + 1.5, at.getY(), at.getZ() + 0.5);
		helper.getLevel().addFreshEntity(npc);
		try {
			PacketGui creative = new PacketGui();
			creative.setOpenGui(net.shadowmage.ancientwarfare.core.network.NetworkHandler.GUI_NPC_CREATIVE, npc.getId(), 0, 0);
			creative.handleAsReceived(player);
			if (player.containerMenu != player.inventoryMenu) {
				helper.fail("a client request opened the creative NPC controls: " + player.containerMenu);
			}
			PacketGui spawner = new PacketGui();
			spawner.setOpenGui(net.shadowmage.ancientwarfare.core.network.NetworkHandler.GUI_SPAWNER_ADVANCED_BLOCK_INVENTORY, at.getX(), at.getY() + 3, at.getZ());
			spawner.handleAsReceived(player);
			if (player.containerMenu != player.inventoryMenu) {
				helper.fail("a client request opened a spawner inventory where no spawner is: " + player.containerMenu);
			}
		}
		finally {
			npc.discard();
			helper.getLevel().getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	/*
	 * the research notifications of the server do nothing when a client sends them: no research is granted
	 * and nothing is queued for a player who has no research station open.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = "core_empty", timeoutTicks = 100)
	public static void clientCannotGrantResearch(GameTestHelper helper) {
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		String name = "AwPacketTestMallory";
		net.shadowmage.ancientwarfare.core.research.ResearchTracker tracker = net.shadowmage.ancientwarfare.core.research.ResearchTracker.INSTANCE;
		try {
			new PacketResearchUpdate(name, "mathematics", true, true).handleAsReceived(player);
			if (tracker.hasPlayerCompleted(helper.getLevel(), name, "mathematics")) {
				helper.fail("a client packet granted research");
			}
			new PacketResearchUpdate(name, "mathematics", true, false).handleAsReceived(player);
			if (tracker.getResearchQueueFor(helper.getLevel(), name).contains("mathematics")) {
				helper.fail("a client packet queued research without a research station");
			}
		}
		finally {
			tracker.clearResearch(helper.getLevel(), name);
			helper.getLevel().getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	/*
	 * the largest template of the pack travels to a client (PacketStructure, as the builder selection sends
	 * it) and reads back completely; a 64 MiB limit on the client disconnected every player for six templates.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = "core_empty", timeoutTicks = 400)
	public static void largestTemplateReachesTheClient(GameTestHelper helper) {
		net.shadowmage.ancientwarfare.structure.template.StructureTemplate largest = null;
		long largestSize = -1;
		for (net.shadowmage.ancientwarfare.structure.template.StructureTemplate template : net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager.getLoadedTemplates()) {
			long size = (long) template.getSize().getX() * template.getSize().getY() * template.getSize().getZ();
			if (size > largestSize) {
				largestSize = size;
				largest = template;
			}
		}
		if (largest == null) {
			helper.fail("no template is loaded");
		}
		System.gc();
		long usedBefore = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20;
		//encoded and decoded as PacketStructure does it, one copy at a time (server and client are two JVMs in play)
		String name = largest.name;
		net.minecraft.core.Vec3i size = largest.getSize();
		io.netty.buffer.ByteBuf wire = io.netty.buffer.Unpooled.buffer();
		CompoundTag sent = new CompoundTag();
		sent.put("singleStructure", largest.serializeNBT());
		net.shadowmage.ancientwarfare.core.util.PacketHelper.writeNBTTag(wire, sent);
		sent = null;
		int wireBytes = wire.readableBytes();
		CompoundTag received = net.shadowmage.ancientwarfare.core.util.PacketHelper.readServerNBTTag(wire);
		boolean complete = !wire.isReadable();
		wire.release();
		net.shadowmage.ancientwarfare.structure.template.StructureTemplate copy = net.shadowmage.ancientwarfare.structure.template.StructureTemplate.deserializeNBT(received.getCompound("singleStructure"));
		if (!complete || copy == null || !name.equals(copy.name) || !size.equals(copy.getSize())) {
			helper.fail("template " + name + " (" + largestSize + " blocks, " + wireBytes + " bytes on the wire) did not arrive: " + (copy == null ? "null" : copy.name + " " + copy.getSize()));
		}
		net.shadowmage.ancientwarfare.AncientWarfare.LOG.info("largestTemplateReachesTheClient: {} with {} blocks, {} bytes on the wire; heap used before the test {} MB, max {} MB", name, largestSize, wireBytes, usedBefore, Runtime.getRuntime().maxMemory() >> 20);
		helper.succeed();
	}
}
