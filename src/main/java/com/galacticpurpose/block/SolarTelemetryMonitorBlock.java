package com.galacticpurpose.block;

import com.galacticpurpose.compat.SpaceCompatManager;
import com.galacticpurpose.terraforming.MarsTerraformingState;
import com.galacticpurpose.terraforming.RadiationShelterChecker;
import com.galacticpurpose.terraforming.SolarStormCalculator;
import com.galacticpurpose.terraforming.SolarStormPhase;
import com.galacticpurpose.terraforming.StormSnapshot;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Early-warning radar console reporting Red Giant solar storm countdowns and bunker radiation shielding.
 */
public class SolarTelemetryMonitorBlock extends Block {
	public static final MapCodec<SolarTelemetryMonitorBlock> CODEC = simpleCodec(SolarTelemetryMonitorBlock::new);

	public SolarTelemetryMonitorBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}

		boolean shielded = RadiationShelterChecker.isPositionShielded(
			serverLevel,
			BlockPos.containing(player.getX(), player.getEyeY(), player.getZ())
		);
		Component shelterText = Component.translatable(
			shielded
				? "message.galacticpurpose.telemetry_shielded"
				: "message.galacticpurpose.telemetry_unshielded"
		).withStyle(shielded ? ChatFormatting.GREEN : ChatFormatting.RED);

		if (!SpaceCompatManager.get().isMars(serverLevel)) {
			player.displayClientMessage(
				Component.translatable("message.galacticpurpose.telemetry_off_mars", shelterText),
				false
			);
			return InteractionResult.CONSUME;
		}

		MarsTerraformingState mars = MarsTerraformingState.get(serverLevel);
		StormSnapshot storm = SolarStormCalculator.evaluate(
			mars.isRedGiantActive(),
			mars.getStormCycleTicks(),
			true,
			false,
			false
		);

		Component stormStatus = switch (storm.phase()) {
			case DORMANT -> Component.translatable("message.galacticpurpose.telemetry_dormant")
				.withStyle(ChatFormatting.AQUA);
			case COUNTDOWN, WARNING -> Component.translatable(
				"message.galacticpurpose.telemetry_countdown",
				formatSeconds(storm.secondsUntilImpact())
			).withStyle(storm.phase() == SolarStormPhase.WARNING ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
			case ACTIVE -> Component.translatable(
				"message.galacticpurpose.telemetry_active",
				formatSeconds(storm.secondsRemaining())
			).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
		};

		player.displayClientMessage(
			Component.translatable("message.galacticpurpose.telemetry_report", stormStatus, shelterText),
			false
		);
		return InteractionResult.CONSUME;
	}

	private static String formatSeconds(int totalSeconds) {
		int mins = totalSeconds / 60;
		int secs = totalSeconds % 60;
		return String.format("%02d:%02d", mins, secs);
	}
}
