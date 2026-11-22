package com.galacticpurpose.network;

import com.galacticpurpose.GalacticPurpose;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server-to-client snapshot syncing Mars terraforming values, Red Giant phase, Solar Storm telemetry,
 * and live player bunker shelter leakage risk (0 = sealed, 100 = exposed).
 */
public record MarsSyncPayload(
	boolean activeInDimension,
	float heatPoints,
	float pressurePoints,
	float oxygenPoints,
	boolean redGiantActive,
	int stormPhaseOrdinal,
	int stormTimerSeconds,
	float shelterLeakPercent
) implements CustomPacketPayload {

	public static final Type<MarsSyncPayload> TYPE =
		new Type<>(GalacticPurpose.id("mars_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MarsSyncPayload> CODEC =
		CustomPacketPayload.codec(MarsSyncPayload::write, MarsSyncPayload::read);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeBoolean(this.activeInDimension);
		buf.writeFloat(this.heatPoints);
		buf.writeFloat(this.pressurePoints);
		buf.writeFloat(this.oxygenPoints);
		buf.writeBoolean(this.redGiantActive);
		buf.writeVarInt(this.stormPhaseOrdinal);
		buf.writeVarInt(this.stormTimerSeconds);
		buf.writeFloat(this.shelterLeakPercent);
	}

	private static MarsSyncPayload read(RegistryFriendlyByteBuf buf) {
		return new MarsSyncPayload(
			buf.readBoolean(),
			buf.readFloat(),
			buf.readFloat(),
			buf.readFloat(),
			buf.readBoolean(),
			buf.readVarInt(),
			buf.readVarInt(),
			buf.readFloat()
		);
	}

	public static MarsSyncPayload inactive() {
		return new MarsSyncPayload(false, 0.0f, 0.0f, 0.0f, false, 0, 0, 100.0f);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
