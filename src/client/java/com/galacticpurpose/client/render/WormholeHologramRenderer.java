package com.galacticpurpose.client.render;

import com.galacticpurpose.registry.ModBlocks;
import com.galacticpurpose.wormhole.WormholeMultiblockValidator;
import com.galacticpurpose.wormhole.WormholeTier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * Projects a live, color-coded 3D ghost blueprint hologram into the world for both Personal (7x5x7)
 * and Colossal Observatory (45x41x45) Wormhole Stabilizer multiblocks. Each ghost block vanishes automatically
 * once the player places the correct block!
 */
public final class WormholeHologramRenderer {
	private static BlockPos anchorCorePos;
	private static WormholeTier activeTier = WormholeTier.PERSONAL;
	private static ResourceLocation anchorDimension;

	private WormholeHologramRenderer() {
	}

	public static boolean isActive() {
		return anchorCorePos != null;
	}

	public static void clear() {
		anchorCorePos = null;
		anchorDimension = null;
	}

	public static void toggleAt(BlockPos corePos, WormholeTier tier) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			return;
		}
		if (corePos.equals(anchorCorePos) && activeTier == tier) {
			clear();
			mc.player.displayClientMessage(
				Component.translatable("message.galacticpurpose.hologram_cleared").withStyle(ChatFormatting.GRAY),
				true
			);
			return;
		}
		anchorCorePos = corePos.immutable();
		activeTier = tier;
		anchorDimension = mc.level.dimension().location();
		mc.player.displayClientMessage(
			Component.translatable(
				"message.galacticpurpose.hologram_projected",
				Component.translatable(tier.translationKey()),
				anchorCorePos.toShortString()
			).withStyle(ChatFormatting.AQUA),
			true
		);
	}

	public static void projectInFrontOfPlayer(WormholeTier tier) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			return;
		}
		Direction facing = mc.player.getDirection();
		int forward = tier == WormholeTier.COLOSSAL ? 28 : 6;
		int elev = tier == WormholeTier.COLOSSAL ? tier.injectorDrop() : 3;
		BlockPos corePos = mc.player.blockPosition().relative(facing, forward).above(elev);
		anchorCorePos = corePos.immutable();
		activeTier = tier;
		anchorDimension = mc.level.dimension().location();
		mc.player.displayClientMessage(
			Component.translatable(
				"message.galacticpurpose.hologram_projected",
				Component.translatable(tier.translationKey()),
				anchorCorePos.toShortString()
			).withStyle(ChatFormatting.AQUA),
			true
		);
	}

	public static void render(WorldRenderContext context) {
		if (anchorCorePos == null || context.world() == null || context.consumers() == null) {
			return;
		}
		Level level = context.world();
		if (!level.dimension().location().equals(anchorDimension)) {
			return;
		}

		// Auto-complete celebration once player finishes placing all required blocks!
		if (level.getBlockState(anchorCorePos).is(ModBlocks.WORMHOLE_CORE)
			&& WormholeMultiblockValidator.validate(level, anchorCorePos).valid()) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				mc.player.displayClientMessage(
					Component.translatable("message.galacticpurpose.hologram_complete")
						.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
					true
				);
			}
			clear();
			return;
		}

		PoseStack poseStack = context.matrixStack();
		MultiBufferSource buffers = context.consumers();
		if (poseStack == null) {
			return;
		}
		Vec3 cam = context.camera().getPosition();
		float pulse = 0.65f + 0.35f * Mth.sin((level.getGameTime() + context.tickCounter().getGameTimeDeltaPartialTick(false)) * 0.14f);

		poseStack.pushPose();
		poseStack.translate(-cam.x, -cam.y, -cam.z);
		VertexConsumer lines = buffers.getBuffer(RenderType.lines());

		// Core Center (Purple/Magenta)
		drawMissingGhost(level, poseStack, lines, anchorCorePos, ModBlocks.WORMHOLE_CORE, 0.85f, 0.32f, 1.0f, pulse);

		if (activeTier == WormholeTier.PERSONAL) {
			drawMissingGhost(level, poseStack, lines, anchorCorePos.offset(WormholeMultiblockValidator.INJECTOR_OFFSET),
				ModBlocks.WORMHOLE_INJECTOR, 1.0f, 0.78f, 0.18f, pulse);
			for (BlockPos off : WormholeMultiblockValidator.PYLON_OFFSETS) {
				drawMissingGhost(level, poseStack, lines, anchorCorePos.offset(off),
					ModBlocks.WORMHOLE_STABILIZER_PYLON, 0.22f, 0.88f, 1.0f, pulse);
			}
			for (BlockPos off : WormholeMultiblockValidator.CONCRETE_OFFSETS) {
				drawMissingGhost(level, poseStack, lines, anchorCorePos.offset(off),
					ModBlocks.LEAD_CONCRETE, 0.55f, 0.68f, 0.82f, pulse * 0.85f);
			}
		} else {
			int d = WormholeTier.COLOSSAL.stabilizerDistance();
			int drop = WormholeTier.COLOSSAL.injectorDrop();
			drawMissingGhost(level, poseStack, lines, anchorCorePos.below(drop),
				ModBlocks.WORMHOLE_INJECTOR, 1.0f, 0.78f, 0.18f, pulse);

			// Four 3x3 (9-block) Stabilizer Pylon arrays (Cyan)
			for (int side : new int[]{d, -d}) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int t = -1; t <= 1; t++) {
						drawMissingGhost(level, poseStack, lines, anchorCorePos.offset(side, dy, t),
							ModBlocks.WORMHOLE_STABILIZER_PYLON, 0.22f, 0.88f, 1.0f, pulse);
						drawMissingGhost(level, poseStack, lines, anchorCorePos.offset(t, dy, side),
							ModBlocks.WORMHOLE_STABILIZER_PYLON, 0.22f, 0.88f, 1.0f, pulse);
					}
				}
			}
			// Four support spines + 4 octagonal diagonal deck anchors (Slate Blue)
			for (int dy = -2; dy >= -drop; dy--) {
				drawMissingGhost(level, poseStack, lines, anchorCorePos.offset(d, dy, 0), ModBlocks.LEAD_CONCRETE, 0.52f, 0.66f, 0.82f, pulse * 0.78f);
				drawMissingGhost(level, poseStack, lines, anchorCorePos.offset(-d, dy, 0), ModBlocks.LEAD_CONCRETE, 0.52f, 0.66f, 0.82f, pulse * 0.78f);
				drawMissingGhost(level, poseStack, lines, anchorCorePos.offset(0, dy, d), ModBlocks.LEAD_CONCRETE, 0.52f, 0.66f, 0.82f, pulse * 0.78f);
				drawMissingGhost(level, poseStack, lines, anchorCorePos.offset(0, dy, -d), ModBlocks.LEAD_CONCRETE, 0.52f, 0.66f, 0.82f, pulse * 0.78f);
			}
			for (BlockPos off : new BlockPos[]{
				new BlockPos(15, -drop, 15), new BlockPos(15, -drop, -15),
				new BlockPos(-15, -drop, 15), new BlockPos(-15, -drop, -15)
			}) {
				drawMissingGhost(level, poseStack, lines, anchorCorePos.offset(off), ModBlocks.LEAD_CONCRETE, 0.52f, 0.66f, 0.82f, pulse * 0.85f);
			}
		}

		poseStack.popPose();
	}

	private static void drawMissingGhost(
		Level level, PoseStack poseStack, VertexConsumer lines,
		BlockPos pos, Block expected, float r, float g, float b, float a
	) {
		if (level.getBlockState(pos).is(expected)) {
			return; // Already correctly built!
		}
		boolean blocked = !level.getBlockState(pos).isAir();
		float rr = blocked ? 1.0f : r;
		float gg = blocked ? 0.22f : g;
		float bb = blocked ? 0.22f : b;
		LevelRenderer.renderLineBox(
			poseStack, lines,
			pos.getX() + 0.04, pos.getY() + 0.04, pos.getZ() + 0.04,
			pos.getX() + 0.96, pos.getY() + 0.96, pos.getZ() + 0.96,
			rr, gg, bb, a
		);
	}
}
