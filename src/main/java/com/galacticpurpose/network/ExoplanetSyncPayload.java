package com.galacticpurpose.network;

import com.galacticpurpose.GalacticPurpose;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server-to-client snapshot syncing active Erebus exoplanet environment and dynamic gravity telemetry.
 */
public record ExoplanetSyncPayload(
	boolean activeInDimension,
	int routeOrdinal,
	int ambientCelsius,
	int pressureAtm,
	boolean hazardous,
	String statusKey,
	float effectiveG,
	float blackHoleElevation
) implements CustomPacketPayload {

	public static final Type<ExoplanetSyncPayload> TYPE =
		new Type<>(GalacticPurpose.id("exoplanet_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ExoplanetSyncPayload> CODEC =
		StreamCodec.ofMember(ExoplanetSyncPayload::write, ExoplanetSyncPayload::read);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeBoolean(this.activeInDimension);
		buf.writeVarInt(this.routeOrdinal);
		buf.writeVarInt(this.ambientCelsius);
		buf.writeVarInt(this.pressureAtm);
		buf.writeBoolean(this.hazardous);
		ByteBufCodecs.STRING_UTF8.encode(buf, this.statusKey);
		buf.writeFloat(this.effectiveG);
		buf.writeFloat(this.blackHoleElevation);
	}

	private static ExoplanetSyncPayload read(RegistryFriendlyByteBuf buf) {
		boolean active = buf.readBoolean();
		int routeOrd = buf.readVarInt();
		int celsius = buf.readVarInt();
		int atm = buf.readVarInt();
		boolean hazard = buf.readBoolean();
		String status = ByteBufCodecs.STRING_UTF8.decode(buf);
		float effG = buf.readFloat();
		float bhElev = buf.readFloat();
		return new ExoplanetSyncPayload(active, routeOrd, celsius, atm, hazard, status, effG, bhElev);
	}

	public static ExoplanetSyncPayload inactive() {
		return new ExoplanetSyncPayload(false, 0, 20, 1, false, "hazard.galacticpurpose.safe", 1.0f, 0.0f);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
