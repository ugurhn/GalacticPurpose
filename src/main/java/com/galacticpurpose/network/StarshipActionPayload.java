package com.galacticpurpose.network;

import com.galacticpurpose.GalacticPurpose;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * C2S pilot action packet for Interstellar Starship controls & Orbital Transit helm commands.
 */
public record StarshipActionPayload(int actionId) implements CustomPacketPayload {
	public static final int ACTION_IGNITE_ORBITAL_LAUNCH = 0;
	public static final int ACTION_OPEN_CARGO = 1;
	public static final int ACTION_TOGGLE_FREE_FLIGHT = 2;
	public static final int ACTION_ENGAGE_AUTO_LAND_OR_ENTER = 3;

	public static final Type<StarshipActionPayload> TYPE =
		new Type<>(GalacticPurpose.id("starship_action"));

	public static final StreamCodec<FriendlyByteBuf, StarshipActionPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, StarshipActionPayload::actionId,
		StarshipActionPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
