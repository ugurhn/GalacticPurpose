package com.galacticpurpose.network;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.climate.CollapsePhase;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server-to-client snapshot syncing Overworld solar instability state for HUD and sky visuals.
 */
public record ClimateSyncPayload(
	boolean activeInDimension,
	int phaseOrdinal,
	long day,
	float severity,
	float ambientCelsius,
	boolean exposedToSky
) implements CustomPacketPayload {

	public static final Type<ClimateSyncPayload> TYPE =
		new Type<>(GalacticPurpose.id("climate_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ClimateSyncPayload> CODEC =
		StreamCodec.composite(
			ByteBufCodecs.BOOL, ClimateSyncPayload::activeInDimension,
			ByteBufCodecs.VAR_INT, ClimateSyncPayload::phaseOrdinal,
			ByteBufCodecs.VAR_LONG, ClimateSyncPayload::day,
			ByteBufCodecs.FLOAT, ClimateSyncPayload::severity,
			ByteBufCodecs.FLOAT, ClimateSyncPayload::ambientCelsius,
			ByteBufCodecs.BOOL, ClimateSyncPayload::exposedToSky,
			ClimateSyncPayload::new
		);

	public static ClimateSyncPayload inactive() {
		return new ClimateSyncPayload(false, CollapsePhase.STABLE.ordinal(), 0L, 0.0f, 20.0f, false);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
