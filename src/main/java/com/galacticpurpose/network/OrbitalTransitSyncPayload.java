package com.galacticpurpose.network;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.ship.StarshipFlightPhysics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C orbital spaceflight & telemetry state synced to the client while inside orbital_transit.
 * Includes exact elapsed/duration ticks so the client interpolates planet & wormhole motion smoothly at 144 FPS.
 */
public record OrbitalTransitSyncPayload(
	boolean inTransit,
	boolean returning,
	int routeOrdinal,
	int elapsedTicks,
	int cruiseDurationTicks,
	boolean reentryActive,
	int reentryTicks,
	int reentryDurationTicks,
	int linkStatusOrdinal,
	int fuelUnits
) implements CustomPacketPayload {
	public static final Type<OrbitalTransitSyncPayload> TYPE =
		new Type<>(GalacticPurpose.id("orbital_transit_sync"));

	public static final StreamCodec<FriendlyByteBuf, OrbitalTransitSyncPayload> CODEC = new StreamCodec<>() {
		@Override
		public OrbitalTransitSyncPayload decode(FriendlyByteBuf buf) {
			return new OrbitalTransitSyncPayload(
				buf.readBoolean(),
				buf.readBoolean(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readBoolean(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt()
			);
		}

		@Override
		public void encode(FriendlyByteBuf buf, OrbitalTransitSyncPayload p) {
			buf.writeBoolean(p.inTransit);
			buf.writeBoolean(p.returning);
			buf.writeVarInt(p.routeOrdinal);
			buf.writeVarInt(p.elapsedTicks);
			buf.writeVarInt(p.cruiseDurationTicks);
			buf.writeBoolean(p.reentryActive);
			buf.writeVarInt(p.reentryTicks);
			buf.writeVarInt(p.reentryDurationTicks);
			buf.writeVarInt(p.linkStatusOrdinal);
			buf.writeVarInt(p.fuelUnits);
		}
	};

	public static OrbitalTransitSyncPayload inactive() {
		return new OrbitalTransitSyncPayload(
			false, false, 0, 0, StarshipFlightPhysics.CRUISE_DURATION_TICKS, false, 0, StarshipFlightPhysics.REENTRY_DURATION_TICKS, 0, 0
		);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
