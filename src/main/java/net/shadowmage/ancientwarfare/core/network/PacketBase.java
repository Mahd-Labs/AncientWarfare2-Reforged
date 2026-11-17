package net.shadowmage.ancientwarfare.core.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.AncientWarfare;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/*
 * Port note: in 1.12 every AW packet shared one FML event channel and was prefixed with a byte id.
 * In 1.21.1 every packet class is its own CustomPacketPayload type ("ancientwarfare:packet_<id>"),
 * but subclasses keep the original writeToStream / readFromStream / execute shape.
 */
public abstract class PacketBase implements CustomPacketPayload {

	/* who may send a packet type; the other side is not even offered a decoder for it */
	public enum Direction {
		TO_CLIENT, TO_SERVER, BOTH
	}

	static final class Registration<T extends PacketBase> {
		Direction direction = Direction.BOTH;
		final int id;
		final Class<T> clz;
		final Supplier<T> factory;
		final Type<T> type;
		final StreamCodec<RegistryFriendlyByteBuf, T> codec;

		Registration(int id, Class<T> clz, Supplier<T> factory) {
			this.id = id;
			this.clz = clz;
			this.factory = factory;
			this.type = new Type<>(ResourceLocation.fromNamespaceAndPath(AncientWarfare.MOD_ID, "packet_" + id));
			this.codec = StreamCodec.of((buf, pkt) -> pkt.writeToStream(buf), buf -> {
				T pkt = factory.get();
				try {
					pkt.readFromStream(buf);
				}
				catch (IOException e) {
					throw new IllegalStateException("Error reading packet " + clz.getName(), e);
				}
				if (buf.isReadable()) {
					//1.21 disconnects on a payload that was not read completely; say which packet it was
					throw new io.netty.handler.codec.DecoderException("Packet " + clz.getName() + " left " + buf.readableBytes() + " bytes unread");
				}
				return pkt;
			});
		}
	}

	static final Map<Integer, Registration<?>> packetTypes = new HashMap<>();
	private static final Map<Class<? extends PacketBase>, Registration<?>> packetIDs = new HashMap<>();
	static boolean frozen = false;

	/*
	 * Must be called during mod construction (before RegisterPayloadHandlersEvent fires).
	 */
	public static <T extends PacketBase> void registerPacketType(int typeNum, Class<T> packetClz, Supplier<T> instantiate) {
		if (frozen) {
			throw new IllegalStateException("Packet " + packetClz.getName() + " registered after payload registration");
		}
		if (packetTypes.containsKey(typeNum)) {
			throw new IllegalStateException("Duplicate packet id " + typeNum + " for " + packetClz.getName());
		}
		Registration<T> reg = new Registration<>(typeNum, packetClz, instantiate);
		packetTypes.put(typeNum, reg);
		packetIDs.put(packetClz, reg);
	}

	/* test support: every registered packet class */
	public static java.util.Set<Class<? extends PacketBase>> registeredClasses() {
		return java.util.Collections.unmodifiableSet(packetIDs.keySet());
	}

	/*
	 * test support: encodes the packet, decodes it and encodes the result again. Returns null when the decoder read
	 * exactly what the encoder wrote and the second encoding has the same bytes, else a description of the problem.
	 */
	@SuppressWarnings("unchecked")
	public static String roundTripProblem(PacketBase sample, net.minecraft.core.RegistryAccess registries) {
		Registration<PacketBase> reg = (Registration<PacketBase>) packetIDs.get(sample.getClass());
		if (reg == null) {
			return "not registered";
		}
		RegistryFriendlyByteBuf first = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), registries);
		RegistryFriendlyByteBuf second = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), registries);
		try {
			reg.codec.encode(first, sample);
			byte[] written = new byte[first.readableBytes()];
			first.getBytes(first.readerIndex(), written);
			PacketBase decoded = reg.codec.decode(first);
			if (first.isReadable()) {
				return first.readableBytes() + " of " + written.length + " bytes left unread";
			}
			reg.codec.encode(second, decoded);
			byte[] again = new byte[second.readableBytes()];
			second.getBytes(second.readerIndex(), again);
			return java.util.Arrays.equals(written, again) ? null : "encoding the decoded packet gives different bytes (" + written.length + " -> " + again.length + ")";
		}
		catch (RuntimeException e) {
			return e.toString();
		}
		finally {
			first.release();
			second.release();
		}
	}

	/*
	 * Port addition: 1.12 had one channel for both directions, so a client could send the server's
	 * notification packets. Packets that only ever travel one way are registered that way.
	 */
	public static <T extends PacketBase> void registerPacketType(int typeNum, Class<T> packetClz, Supplier<T> instantiate, Direction direction) {
		registerPacketType(typeNum, packetClz, instantiate);
		packetTypes.get(typeNum).direction = direction;
	}

	public PacketBase() {
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		Registration<?> reg = packetIDs.get(getClass());
		if (reg == null) {
			throw new IllegalStateException("Unregistered packet class " + getClass().getName());
		}
		return reg.type;
	}

	protected abstract void writeToStream(RegistryFriendlyByteBuf data);

	protected abstract void readFromStream(RegistryFriendlyByteBuf data) throws IOException;

	protected void execute() {
	}

	/*
	 * Runs on the main thread of the receiving side. On the client the player is the local player.
	 */
	protected void execute(Player player) {
		execute();
	}

	/* test support: what the receiving side does with a decoded packet */
	public final void handleAsReceived(Player player) {
		handle(player);
	}

	final void handle(Player player) {
		execute(player);
	}
}
