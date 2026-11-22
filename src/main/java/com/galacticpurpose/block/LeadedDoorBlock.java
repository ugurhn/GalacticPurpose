package com.galacticpurpose.block;

import com.galacticpurpose.GalacticPurpose;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Heavy lead-reinforced airlock door with a clear leaded-glass viewport pane.
 * Can be opened directly by hand or Redstone while sealing 100% of solar storm radiation when closed.
 */
public class LeadedDoorBlock extends DoorBlock {
	public static final BlockSetType LEAD_AIRLOCK_SET = BlockSetTypeBuilder.copyOf(BlockSetType.IRON)
		.openableByHand(true)
		.register(GalacticPurpose.id("lead_airlock"));

	public LeadedDoorBlock(Properties properties) {
		super(LEAD_AIRLOCK_SET, properties);
	}
}
