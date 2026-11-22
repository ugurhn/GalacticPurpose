package com.galacticpurpose.network;

import com.galacticpurpose.GalacticPurpose;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C trigger for mastered, ear-safe aerospace & cosmic client sound events.
 */
public record ClientSoundEventPayload(int eventId, float volume, float pitch) implements CustomPacketPayload {
	public static final int EVENT_COUNTDOWN_PING = 0;
	public static final int EVENT_IGNITION_GO = 1;
	public static final int EVENT_WORMHOLE_WARP = 2;
	public static final int EVENT_HYDRAULIC_TOUCHDOWN = 3;
	public static final int EVENT_ABORT_ALARM = 4;
	public static final int EVENT_AUTOPILOT_ENGAGE = 5;
	public static final int EVENT_STORM_WARNING = 6;
	public static final int EVENT_STORM_STRIKE = 7;

	public static final Type<ClientSoundEventPayload> TYPE =
		new Type<>(GalacticPurpose.id("client_sound_event"));

	public static final StreamCodec<FriendlyByteBuf, ClientSoundEventPayload> CODEC = new StreamCodec<>() {
		@Override
		public ClientSoundEventPayload decode(FriendlyByteBuf buf) {
			return new ClientSoundEventPayload(buf.readVarInt(), buf.readFloat(), buf.readFloat());
		}

		@Override
		public void encode(FriendlyByteBuf buf, ClientSoundEventPayload p) {
			buf.writeVarInt(p.eventId);
			buf.writeFloat(p.volume);
			buf.writeFloat(p.pitch);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
