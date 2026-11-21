package com.galacticpurpose.network;

import com.galacticpurpose.GalacticPurpose;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * C2S lightweight 3D flight control packet while piloting InterstellarShipEntity in Free Flight mode.
 */
public record StarshipInputPayload(float forward, float strafe, float vertical, float yawDeg) implements CustomPacketPayload {
	public static final Type<StarshipInputPayload> TYPE =
		new Type<>(GalacticPurpose.id("starship_input"));

	public static final StreamCodec<FriendlyByteBuf, StarshipInputPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.FLOAT, StarshipInputPayload::forward,
		ByteBufCodecs.FLOAT, StarshipInputPayload::strafe,
		ByteBufCodecs.FLOAT, StarshipInputPayload::vertical,
		ByteBufCodecs.FLOAT, StarshipInputPayload::yawDeg,
		StarshipInputPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
