package com.galacticpurpose.wormhole;

import com.galacticpurpose.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure scanner and batch step selector for the Wormhole Structure Assembler wrench.
 */
public final class WormholeAssemblerCalculator {
	public record Obstruction(BlockPos worldPos, Block existingBlock, boolean inClearZone) {
	}

	public record ScanResult(
		WormholeTier tier,
		int totalRequired,
		int alreadySatisfied,
		List<WormholeMultiblockValidator.StructureBlockSpec> buildableSlots,
		List<Obstruction> obstructions,
		Map<Block, Integer> remainingMaterialCounts
	) {
		public boolean isComplete() {
			return this.alreadySatisfied >= this.totalRequired && this.obstructions.isEmpty();
		}
	}

	private WormholeAssemblerCalculator() {
	}

	public static int batchSizeFor(WormholeTier tier) {
		return tier == WormholeTier.COLOSSAL ? 4 : 1;
	}

	public static ScanResult scan(Level level, BlockPos corePos, WormholeTier tier) {
		List<WormholeMultiblockValidator.StructureBlockSpec> blueprint =
			WormholeMultiblockValidator.getRequiredBlueprint(tier);
		List<BlockPos> clearOffsets = WormholeMultiblockValidator.getRequiredClearOffsets(tier);

		int satisfied = 0;
		List<WormholeMultiblockValidator.StructureBlockSpec> buildable = new ArrayList<>();
		List<Obstruction> obstructions = new ArrayList<>();
		Map<Block, Integer> remaining = new LinkedHashMap<>();

		for (BlockPos clearOff : clearOffsets) {
			BlockPos worldPos = corePos.offset(clearOff);
			BlockState state = level.getBlockState(worldPos);
			if (!state.isAir() && !state.is(ModBlocks.WORMHOLE_PORTAL) && !state.canBeReplaced()) {
				obstructions.add(new Obstruction(worldPos, state.getBlock(), true));
			}
		}

		for (WormholeMultiblockValidator.StructureBlockSpec spec : blueprint) {
			BlockPos worldPos = corePos.offset(spec.offset());
			BlockState current = level.getBlockState(worldPos);
			Block expected = spec.targetState().getBlock();

			if (current.is(expected)) {
				satisfied++;
			} else if (current.isAir() || current.canBeReplaced()) {
				buildable.add(spec);
				remaining.merge(expected, 1, Integer::sum);
			} else {
				obstructions.add(new Obstruction(worldPos, current.getBlock(), false));
				remaining.merge(expected, 1, Integer::sum);
			}
		}

		return new ScanResult(
			tier,
			blueprint.size(),
			satisfied,
			List.copyOf(buildable),
			List.copyOf(obstructions),
			Map.copyOf(remaining)
		);
	}
}
