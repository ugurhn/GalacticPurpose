package com.galacticpurpose.gametest;

import com.galacticpurpose.block.BioDistillerBlock;
import com.galacticpurpose.block.BioDistillerBlockEntity;
import com.galacticpurpose.block.DeepSpaceTelescopeBlockEntity;
import com.galacticpurpose.block.SolarFluxGeneratorBlockEntity;
import com.galacticpurpose.block.TerraformingMachineBlock;
import com.galacticpurpose.block.TerraformingMachineBlockEntity;
import com.galacticpurpose.block.WormholeCoreBlock;
import com.galacticpurpose.block.WormholeCoreBlockEntity;
import com.galacticpurpose.block.WormholeEnergyInjectorBlockEntity;
import com.galacticpurpose.climate.ClimateSnapshot;
import com.galacticpurpose.climate.CollapsePhase;
import com.galacticpurpose.climate.OverworldHazardTicker;
import com.galacticpurpose.climate.SolarInstabilityCalculator;
import com.galacticpurpose.codex.MissionCodexGrantState;
import com.galacticpurpose.command.GalacticPurposeCommands;
import com.galacticpurpose.compat.SpaceCompatManager;
import com.galacticpurpose.config.ModConfig;
import com.galacticpurpose.entity.InterstellarShipEntity;
import com.galacticpurpose.entity.SingularityCollapseEntity;
import com.galacticpurpose.exoplanet.ExoplanetGravityCalculator;
import com.galacticpurpose.exoplanet.ExoplanetHazardCalculator;
import com.galacticpurpose.exoplanet.ExoplanetLandmarkBuilder;
import com.galacticpurpose.ship.OrbitalShipCabinBuilder;
import com.galacticpurpose.ship.OrbitalTransitManager;
import com.galacticpurpose.ship.StarshipLandingCalculator;
import com.galacticpurpose.wormhole.WormholeLinkCalculator;
import com.galacticpurpose.wormhole.WormholeLinkRegistry;
import com.galacticpurpose.exoplanet.ExoplanetHazardSnapshot;
import com.galacticpurpose.exoplanet.ExoplanetHazardTicker;
import com.galacticpurpose.item.LeadArmorItem;
import com.galacticpurpose.item.LeadEnergyCellItem;
import com.galacticpurpose.item.PressureArmorItem;
import com.galacticpurpose.item.ThermalArmorItem;
import com.galacticpurpose.registry.ModBlocks;
import com.galacticpurpose.registry.ModItems;
import com.galacticpurpose.terraforming.MarsProgressSnapshot;
import com.galacticpurpose.terraforming.MarsTerraformingCalculator;
import com.galacticpurpose.terraforming.MarsTerraformingState;
import com.galacticpurpose.terraforming.MarsWorldTerraformer;
import com.galacticpurpose.terraforming.RadiationShelterChecker;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import com.galacticpurpose.wormhole.WormholeMultiblockValidator;
import com.galacticpurpose.wormhole.WormholeState;
import com.galacticpurpose.wormhole.WormholeTier;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import team.reborn.energy.api.EnergyStorage;

/**
 * In-engine Minecraft 1.21.1 server GameTests verifying machines, world terraforming, bunkers, armor, and sky checks.
 */
public class GalacticPurposeGameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE, batch = "mars_heater", timeoutTicks = 100)
	public void thermalHeaterConsumesCellAndRaisesMarsHeat(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ModConfig.get().setMarsTerraformingTestMode(true);
		MarsTerraformingState state = MarsTerraformingState.get(level);
		state.activateRedGiant(level, false);
		state.setPercents(0.0f, 0.0f, 0.0f);

		BlockPos pos = new BlockPos(1, 2, 1);
		helper.setBlock(pos, ModBlocks.THERMAL_HEATER);

		TerraformingMachineBlockEntity be = helper.getBlockEntity(pos);
		be.setItem(0, new ItemStack(ModItems.CREATIVE_ENERGY_CELL));

		helper.runAfterDelay(25, () -> {
			helper.assertBlockProperty(pos, TerraformingMachineBlock.LIT, true);
			helper.assertTrue(be.energyStorage().getAmount() > 0L, "Expected machine to draw energy from cell");
			helper.assertTrue(state.snapshot().heatPoints() > 0.0, "Expected Mars Heat to increase");
			ModConfig.get().setMarsTerraformingTestMode(false);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "mars_flora", timeoutTicks = 100)
	public void floraOxygenatorRespectsHeatAndPressurePrerequisites(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ModConfig.get().setMarsTerraformingTestMode(true);
		MarsTerraformingState state = MarsTerraformingState.get(level);
		state.activateRedGiant(level, false);
		state.setPercents(0.0f, 0.0f, 0.0f);

		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, ModBlocks.FLORA_OXYGENATOR);

		TerraformingMachineBlockEntity be = helper.getBlockEntity(pos);
		be.setItem(0, new ItemStack(ModItems.CREATIVE_ENERGY_CELL));

		helper.runAfterDelay(10, () -> {
			helper.assertBlockProperty(pos, TerraformingMachineBlock.LIT, false);
			helper.assertTrue(state.snapshot().oxygenPoints() == 0.0, "Oxygen should stay 0 when Mars is frozen");

			state.setPercents(20.0f, 15.0f, 0.0f);
			helper.runAfterDelay(25, () -> {
				helper.assertBlockProperty(pos, TerraformingMachineBlock.LIT, true);
				helper.assertTrue(state.snapshot().oxygenPoints() > 0.0, "Oxygen should increase once warm");
				ModConfig.get().setMarsTerraformingTestMode(false);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "bunker", timeoutTicks = 100)
	public void leadBunkerAndLeadSuitProvideRadiationShielding(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		BlockPos exposed = new BlockPos(1, 2, 1);
		helper.setBlock(exposed.below(), Blocks.STONE);
		helper.assertFalse(
			RadiationShelterChecker.isPositionShielded(level, helper.absolutePos(exposed)),
			"Open sky position should not be radiation-shielded"
		);

		for (int dx = 4; dx <= 6; dx++) {
			for (int dz = 4; dz <= 6; dz++) {
				helper.setBlock(dx, 1, dz, ModBlocks.LEAD_CONCRETE);
				helper.setBlock(dx, 3, dz, ModBlocks.LEAD_CONCRETE);
				if (dx == 4 || dx == 6 || dz == 4 || dz == 6) {
					helper.setBlock(dx, 2, dz, (dx == 5 && dz == 4) ? ModBlocks.LEADED_GLASS : ModBlocks.LEAD_CONCRETE);
				}
			}
		}

		BlockPos insideBunker = new BlockPos(5, 2, 5);
		helper.assertTrue(
			RadiationShelterChecker.isPositionShielded(level, helper.absolutePos(insideBunker)),
			"Enclosed Lead-Concrete + Leaded Glass bunker should shield radiation"
		);

		Player mockPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertFalse(LeadArmorItem.hasFullSuit(mockPlayer), "Bare player should not have full lead suit");
		mockPlayer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.LEAD_HELMET));
		mockPlayer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.LEAD_CHESTPLATE));
		mockPlayer.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.LEAD_LEGGINGS));
		mockPlayer.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.LEAD_BOOTS));
		helper.assertTrue(LeadArmorItem.hasFullSuit(mockPlayer), "Equipped player should have full lead suit");

		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "world_terraformer", skyAccess = true, timeoutTicks = 100)
	public void marsWorldTerraformerMeltsIceAndGrowsGrass(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos icePos = new BlockPos(2, 2, 2);
		BlockPos sandPos = new BlockPos(4, 2, 4);
		helper.setBlock(icePos.below(), Blocks.STONE);
		helper.setBlock(icePos, Blocks.ICE);
		helper.setBlock(sandPos.below(), Blocks.STONE);
		helper.setBlock(sandPos, Blocks.RED_SAND);
		helper.setBlock(sandPos.above(), Blocks.AIR);

		helper.runAfterDelay(2, () -> {
			MarsProgressSnapshot floraSnap = MarsTerraformingCalculator.snapshot(5000, 4500, 3000);
			helper.assertTrue(floraSnap.liquidWaterReady() && floraSnap.floraReady(), "Expected water and flora readiness");

			boolean melted = MarsWorldTerraformer.tryTransformAt(level, helper.absolutePos(icePos), floraSnap, level.random);
			boolean grassed = MarsWorldTerraformer.tryTransformAt(level, helper.absolutePos(sandPos.above()), floraSnap, level.random);

			helper.assertTrue(melted, "Expected ice block to melt");
			helper.assertTrue(grassed, "Expected red sand column to grass (topEmpty="
				+ level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, helper.absolutePos(sandPos.above()))
				+ ", sandAbs=" + helper.absolutePos(sandPos) + ")");
			helper.assertBlockPresent(Blocks.WATER, icePos);
			helper.assertBlockPresent(Blocks.GRASS_BLOCK, sandPos);

			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "overworld_collapse", skyAccess = true, timeoutTicks = 100)
	public void overworldSkyAccessDifferentiatesExposedVsShelteredClimate(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos openPos = new BlockPos(1, 2, 1);
		BlockPos shelteredPos = new BlockPos(5, 2, 5);

		helper.setBlock(openPos.below(), Blocks.STONE);
		helper.setBlock(shelteredPos.below(), Blocks.STONE);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				helper.setBlock(shelteredPos.offset(dx, 2, dz), Blocks.STONE);
			}
		}

		helper.runAfterDelay(2, () -> {
			boolean openExposed = OverworldHazardTicker.isExposedToSky(level, helper.absolutePos(openPos));
			boolean roofExposed = OverworldHazardTicker.isExposedToSky(level, helper.absolutePos(shelteredPos));

			helper.assertTrue(openExposed, "Open air block should see sky");
			helper.assertFalse(roofExposed, "Block beneath 3x3 stone roof should be sheltered");

			ClimateSnapshot exposedDay40 = SolarInstabilityCalculator.evaluate(
				40, 6000L, openExposed, false, false, ModConfig.get()
			);
			ClimateSnapshot shelteredDay40 = SolarInstabilityCalculator.evaluate(
				40, 6000L, roofExposed, false, false, ModConfig.get()
			);

			helper.assertTrue(exposedDay40.phase() == CollapsePhase.CRITICAL, "Day 40 should be CRITICAL");
			helper.assertTrue(exposedDay40.heatDamage() > 0.0f, "Exposed player should take solar heat damage");
			helper.assertTrue(shelteredDay40.heatDamage() == 0.0f, "Sheltered player should take zero damage");

			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "bio_distiller", timeoutTicks = 150)
	public void bioDistillerRefinesEssenceIntoExoticBioFuel(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 2, 1);
		helper.setBlock(pos, ModBlocks.BIO_DISTILLER);

		BioDistillerBlockEntity be = helper.getBlockEntity(pos);
		be.setItem(0, new ItemStack(ModItems.CREATIVE_ENERGY_CELL));
		be.setItem(1, new ItemStack(ModItems.MARS_BIO_ESSENCE, 2));

		helper.runAfterDelay(10, () -> {
			helper.assertBlockProperty(pos, BioDistillerBlock.LIT, true);
			helper.runAfterDelay(100, () -> {
				helper.assertTrue(be.getItem(1).isEmpty(), "Expected 2x Mars Bio-Essence to be consumed");
				ItemStack output = be.getItem(2);
				helper.assertTrue(
					output.is(ModItems.EXOTIC_BIO_FUEL) && output.getCount() == 1,
					"Expected 1x Exotic Bio-Fuel in output slot"
				);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "telescope", skyAccess = true, timeoutTicks = 160)
	public void deepSpaceTelescopeRequiresOpenMarsSkyAndScansCoordinateCard(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ModConfig.get().setMarsTerraformingTestMode(true);

		BlockPos blockedPos = new BlockPos(1, 2, 1);
		BlockPos openPos = new BlockPos(4, 2, 4);
		helper.setBlock(blockedPos.below(), Blocks.STONE);
		helper.setBlock(blockedPos, ModBlocks.DEEP_SPACE_TELESCOPE);
		helper.setBlock(blockedPos.above(2), Blocks.STONE);

		helper.setBlock(openPos.below(), Blocks.STONE);
		helper.setBlock(openPos, ModBlocks.DEEP_SPACE_TELESCOPE);

		Player mockPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
		DeepSpaceTelescopeBlockEntity blockedBe = helper.getBlockEntity(blockedPos);
		DeepSpaceTelescopeBlockEntity openBe = helper.getBlockEntity(openPos);

		helper.runAfterDelay(5, () -> {
			EnergyStorage blockedEnergy = EnergyStorage.SIDED.find(level, helper.absolutePos(blockedPos), null);
			EnergyStorage openEnergy = EnergyStorage.SIDED.find(level, helper.absolutePos(openPos), null);
			helper.assertTrue(blockedEnergy != null && openEnergy != null, "Expected telescope EnergyStorage");
			try (Transaction tx = Transaction.openOuter()) {
				blockedEnergy.insert(10_000L, tx);
				openEnergy.insert(10_000L, tx);
				tx.commit();
			}

			helper.useBlock(blockedPos, mockPlayer);
			helper.assertFalse(blockedBe.isScanning(), "Blocked telescope sky should prevent scan start");

			helper.assertTrue(
				OverworldHazardTicker.isExposedToSky(level, helper.absolutePos(openPos).above()),
				"Open telescope position should see sky (canSeeSkyAbove="
					+ level.canSeeSky(helper.absolutePos(openPos).above()) + ")"
			);
			helper.useBlock(openPos, mockPlayer);
			helper.assertTrue(openBe.isScanning(), "Expected telescope scan to start under open Mars sky");

			helper.runAfterDelay(85, () -> {
				helper.assertTrue(
					openBe.readyReward() == ExoplanetRoute.HEPHAESTUS_IV,
					"Expected Hephaestus-IV scan discovery"
				);
				helper.useBlock(openPos, mockPlayer);
				helper.assertTrue(
					mockPlayer.getInventory().countItem(ModItems.forCoordinateCard(ExoplanetRoute.HEPHAESTUS_IV)) == 1,
					"Expected player to receive Coordinate Card after interacting"
				);
				ModConfig.get().setMarsTerraformingTestMode(false);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "wormhole_core", skyAccess = true, timeoutTicks = 1200)
	public void wormholeMultiblockValidatesAndChargesToOpenPortal(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos corePos = new BlockPos(4, 5, 4);
		BlockPos injectorPos = corePos.offset(0, -3, 0);

		helper.setBlock(corePos, ModBlocks.WORMHOLE_CORE);
		helper.assertFalse(
			WormholeMultiblockValidator.validate(level, helper.absolutePos(corePos)).valid(),
			"Isolated Wormhole Core should fail multiblock validation"
		);

		buildWormholeFrame(helper, corePos);

		helper.assertTrue(
			WormholeMultiblockValidator.validate(level, helper.absolutePos(corePos)).valid(),
			"Completed 7x4x7 Wormhole Stabilizer should pass multiblock validation"
		);

		WormholeCoreBlockEntity core = helper.getBlockEntity(corePos);
		EnergyStorage injectorEnergy = EnergyStorage.SIDED.find(level, helper.absolutePos(injectorPos), null);
		helper.assertTrue(injectorEnergy != null, "Expected EnergyStorage proxy on Wormhole Injector");
		try (Transaction tx = Transaction.openOuter()) {
			long inserted = injectorEnergy.insert(20_000L, tx);
			tx.commit();
			helper.assertTrue(inserted == 20_000L, "Expected Injector proxy to insert energy into Core");
		}
		helper.assertTrue(
			core.energyStorage().getAmount() == 20_000L,
			"Expected Core energy storage to reflect Injector insertion"
		);

		core.setItem(0, new ItemStack(ModItems.forCoordinateCard(ExoplanetRoute.HEPHAESTUS_IV)));
		core.setItem(1, new ItemStack(ModItems.EXOTIC_BIO_FUEL, 8));
		core.setItem(2, new ItemStack(Items.BLUE_ICE, 8));
		core.setItem(3, new ItemStack(ModItems.CREATIVE_ENERGY_CELL));

		// Ignite Wormhole Core (button 0)
		core.handleMenuButton(null, 0);

		helper.runAfterDelay(25, () -> {
			helper.assertTrue(
				core.wormholeState() == WormholeState.CHARGING,
				"Expected Wormhole Core to enter CHARGING state"
			);
			helper.assertBlockProperty(corePos, WormholeCoreBlock.STAGE, 1);

			helper.runAfterDelay(1050, () -> {
				helper.assertTrue(
					core.wormholeState() == WormholeState.OPEN,
					"Expected Wormhole Core to finish charging to OPEN, was " + core.wormholeState()
				);
				helper.assertBlockProperty(corePos, WormholeCoreBlock.STAGE, 2);
				helper.assertBlockPresent(ModBlocks.WORMHOLE_PORTAL, corePos.below());
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "exoplanet_dims", timeoutTicks = 120)
	public void exoplanetDimensionsAndFeaturesLoadFromDatapack(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		for (ExoplanetRoute route : ExoplanetRoute.values()) {
			ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, route.dimensionId());
			ServerLevel exoLevel = server.getLevel(key);
			helper.assertTrue(exoLevel != null, "Expected exoplanet dimension loaded: " + route.dimensionId());
			exoLevel.getChunk(0, 0);
		}
		ServerLevel pelagia = server.getLevel(ResourceKey.create(Registries.DIMENSION, ExoplanetRoute.PELAGIA_9.dimensionId()));
		helper.assertTrue(pelagia != null && pelagia.getBlockState(new BlockPos(0, 100, 0)).is(Blocks.WATER),
			"Expected Pelagia-9 deep ocean water at y=100");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "wormhole_quantum", skyAccess = true, timeoutTicks = 120)
	public void quantumCoreResonanceUnlocksSanctuaryOnWormholeCore(GameTestHelper helper) {
		BlockPos corePos = new BlockPos(4, 5, 4);
		helper.setBlock(corePos, ModBlocks.WORMHOLE_CORE);
		buildWormholeFrame(helper, corePos);

		WormholeCoreBlockEntity core = helper.getBlockEntity(corePos);
		core.setItem(0, new ItemStack(ModItems.forCoordinateCard(ExoplanetRoute.SANCTUARY)));
		core.setItem(1, new ItemStack(ModItems.EXOTIC_BIO_FUEL, 4));
		core.setItem(3, new ItemStack(ModItems.CREATIVE_ENERGY_CELL));

		core.handleMenuButton(null, 0);
		helper.assertTrue(
			core.wormholeState() == WormholeState.IDLE && !core.isQuantumUpgraded(),
			"Sanctuary route must stay locked before Quantum Core resonance"
		);

		boolean upgraded = core.tryUpgradeWithQuantumCore(null, new ItemStack(ModItems.QUANTUM_CORE));
		helper.assertTrue(upgraded && core.isQuantumUpgraded(), "Expected Quantum Core upgrade to succeed");

		core.handleMenuButton(null, 0);
		helper.assertTrue(
			core.wormholeState() == WormholeState.CHARGING,
			"Expected Sanctuary ignition to begin charging after Quantum Core upgrade"
		);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "situational_suits", timeoutTicks = 80)
	public void situationalThermalAndPressureSuitsProtectRespectiveHazards(GameTestHelper helper) {
		Player mockPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertFalse(ThermalArmorItem.hasFullSuit(mockPlayer), "Bare player should not have thermal suit");
		helper.assertFalse(PressureArmorItem.hasFullSuit(mockPlayer), "Bare player should not have pressure suit");
		helper.assertFalse(SpaceCompatManager.get().hasThermalShield(mockPlayer), "Bare player should lack thermal shield");

		mockPlayer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.THERMAL_HELMET));
		mockPlayer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.THERMAL_CHESTPLATE));
		mockPlayer.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.THERMAL_LEGGINGS));
		mockPlayer.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.THERMAL_BOOTS));

		helper.assertTrue(ThermalArmorItem.hasFullSuit(mockPlayer), "Expected full Thermal Suit");
		helper.assertTrue(SpaceCompatManager.get().hasThermalShield(mockPlayer),
			"Expected SpaceCompat thermal shield integration");

		ExoplanetHazardSnapshot pelagiaInThermal = ExoplanetHazardCalculator.evaluate(
			ExoplanetRoute.PELAGIA_9.dimensionId(),
			40,
			false,
			true,
			ThermalArmorItem.hasFullSuit(mockPlayer),
			PressureArmorItem.hasFullSuit(mockPlayer)
		);
		helper.assertTrue(pelagiaInThermal != null && pelagiaInThermal.hazardous() && pelagiaInThermal.pressureDamage() > 0.0f,
			"Anti-God-Mode check: Thermal Suit must NOT block deep Pelagia pressure");

		mockPlayer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PRESSURE_HELMET));
		mockPlayer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PRESSURE_CHESTPLATE));
		mockPlayer.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PRESSURE_LEGGINGS));
		mockPlayer.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.PRESSURE_BOOTS));

		helper.assertTrue(PressureArmorItem.hasFullSuit(mockPlayer), "Expected full Pressure Suit");
		helper.assertFalse(ThermalArmorItem.hasFullSuit(mockPlayer), "Switching suits should unset thermal shield");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "exoplanet_gravity", timeoutTicks = 60)
	public void exoplanetGravityChangesWithErebusOrbitalPosition(GameTestHelper helper) {
		Player mockPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
		double baseGravity = mockPlayer.getAttributeValue(Attributes.GRAVITY);
		double baseSafeFall = mockPlayer.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);

		ExoplanetGravityCalculator.GravityReading zenith =
			ExoplanetGravityCalculator.evaluate(ExoplanetRoute.HEPHAESTUS_IV, 6000L);
		ExoplanetGravityCalculator.GravityReading nadir =
			ExoplanetGravityCalculator.evaluate(ExoplanetRoute.HEPHAESTUS_IV, 18000L);
		ExoplanetGravityCalculator.GravityReading nyx =
			ExoplanetGravityCalculator.evaluate(ExoplanetRoute.NYX_0, 6000L);

		ExoplanetHazardTicker.applyExoplanetGravity(mockPlayer, zenith.effectiveG());
		double zenithGravity = mockPlayer.getAttributeValue(Attributes.GRAVITY);

		ExoplanetHazardTicker.applyExoplanetGravity(mockPlayer, nadir.effectiveG());
		double nadirGravity = mockPlayer.getAttributeValue(Attributes.GRAVITY);

		helper.assertTrue(
			nadirGravity > zenithGravity && zenithGravity > baseGravity,
			"Expected Hephaestus nadir gravity > zenith gravity > Overworld 1.0g"
		);

		ExoplanetHazardTicker.applyExoplanetGravity(mockPlayer, nyx.effectiveG());
		double nyxGravity = mockPlayer.getAttributeValue(Attributes.GRAVITY);
		double nyxSafeFall = mockPlayer.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
		helper.assertTrue(nyxGravity < baseGravity * 0.6, "Expected low gravity on Nyx-0");
		helper.assertTrue(nyxSafeFall > baseSafeFall + 2.0, "Expected increased safe fall distance in low gravity");

		ExoplanetHazardTicker.clearExoplanetGravity(mockPlayer);
		helper.assertTrue(
			Math.abs(mockPlayer.getAttributeValue(Attributes.GRAVITY) - baseGravity) < 0.0001,
			"Expected gravity attribute to reset to Overworld baseline after leaving exoplanets"
		);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "singularity_collapse", skyAccess = true, timeoutTicks = 580)
	public void wormholePresetsAndSingularityCollapseAscendRipAndDetonate(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos corePos = new BlockPos(4, 5, 4);
		buildWormholeFrame(helper, corePos);

		WormholeCoreBlockEntity core = helper.getBlockEntity(corePos);
		core.configureStablePreset(level, ExoplanetRoute.PELAGIA_9, WormholeTier.PERSONAL);
		helper.assertTrue(core.wormholeState() == WormholeState.OPEN, "Stable preset must be OPEN");
		helper.assertBlockPresent(ModBlocks.WORMHOLE_PORTAL, corePos.below());

		core.configureUnstablePreset(level, WormholeTier.PERSONAL);
		helper.assertTrue(
			core.wormholeState() == WormholeState.UNSTABLE && core.tempCelsius() >= 8_600.0f,
			"Unstable preset must start critical UNSTABLE"
		);

		// Verify Quench (Kill Power, button 1) succeeds BEFORE Point of No Return (8650C < 9300C, structure intact)
		core.handleMenuButton(null, 1);
		helper.assertTrue(
			core.wormholeState() == WormholeState.SHUTTING_DOWN,
			"Quench button must switch UNSTABLE reactor to SHUTTING_DOWN before Point of No Return"
		);

		// Verify Point of No Return lockout: if a stabilizer is broken on a hot reactor, Quench is refused!
		core.configureUnstablePreset(level, WormholeTier.PERSONAL);
		helper.setBlock(corePos.offset(3, 0, 0), Blocks.AIR);
		helper.assertFalse(
			WormholeMultiblockValidator.isStabilizerSideIntact(level, helper.absolutePos(corePos), WormholeTier.PERSONAL, net.minecraft.core.Direction.EAST),
			"Broken East stabilizer must immediately cut East beam validation"
		);
		core.handleMenuButton(null, 1);
		helper.assertTrue(
			core.wormholeState() == WormholeState.UNSTABLE,
			"Point of No Return must lock out Quench when stabilizers are breached at high temperature"
		);
		helper.setBlock(corePos, Blocks.AIR);

		// Populate dense cobblestone ring around base so surface-peel vortex rips & progressive shockwave carves it
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -6; dz <= 6; dz++) {
				helper.setBlock(corePos.offset(dx, -4, dz), Blocks.COBBLESTONE);
			}
		}

		Vec3 startPos = Vec3.atCenterOf(helper.absolutePos(corePos));
		SingularityCollapseEntity collapse = SingularityCollapseEntity.spawnCustom(level, startPos, 1.5f, 10);

		helper.runAfterDelay(60, () -> {
			helper.assertTrue(collapse.isAlive(), "Collapse entity must stay alive during sky ascent");
			helper.assertTrue(
				collapse.getY() > startPos.y + 10.0,
				"Expected singularity to ascend smoothly into sky, deltaY=" + (collapse.getY() - startPos.y)
			);
			helper.assertTrue(
				collapse.visualCoreRadius(0.0f) > 2.5f,
				"Expected singularity core to grow dramatically (2.5x+) during ascent"
			);

			helper.runAfterDelay(100, () -> {
				AABB searchBox = collapse.getBoundingBox().inflate(55.0);
				int fallingCount = level.getEntitiesOfClass(FallingBlockEntity.class, searchBox).size();
				helper.assertTrue(
					fallingCount > 0 || collapse.absorbedBlocks() > 0,
					"Expected singularity surface-peel vortex to rip/absorb blocks (falling="
						+ fallingCount + ", absorbed=" + collapse.absorbedBlocks() + ")"
				);

				helper.runAfterDelay(175, () -> {
					helper.assertTrue(
						collapse.isDetonating() && collapse.shockwaveRadius(0.0f) > 4.0f,
						"Expected active expanding Draconic-style shockwave phase at tick ~335"
					);

					helper.runAfterDelay(175, () -> {
						helper.assertFalse(collapse.isAlive(), "Expected shockwave entity to complete & discard by tick 495");
						helper.succeed();
					});
				});
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "colossal_wormhole", skyAccess = true, timeoutTicks = 120)
	public void colossalShipScaleWormholeMultiblockAndOrbitCommandWork(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();

		// 1. Verify Colossal 45x41x45 Open-Sky Octagonal Observatory (4x 3x3 = 36 Stabilizers + Injector at -21)
		BlockPos coreAbs = helper.absolutePos(new BlockPos(4, 26, 4));
		WormholeMultiblockValidator.buildStructure(level, coreAbs, WormholeTier.COLOSSAL);

		WormholeMultiblockValidator.ValidationResult validation = WormholeMultiblockValidator.validate(level, coreAbs);
		helper.assertTrue(
			validation.valid() && validation.tier() == WormholeTier.COLOSSAL,
			"Expected valid COLOSSAL 45x41x45 Wormhole Observatory, missing="
				+ validation.expectedBlockKey() + " at " + validation.missingOffset()
		);

		WormholeCoreBlockEntity core = (WormholeCoreBlockEntity) level.getBlockEntity(coreAbs);
		WormholeEnergyInjectorBlockEntity injector =
			(WormholeEnergyInjectorBlockEntity) level.getBlockEntity(coreAbs.below(WormholeTier.COLOSSAL.injectorDrop()));
		helper.assertTrue(core != null && injector != null, "Expected Colossal Core and ground-floor Injector BEs");
		helper.assertTrue(injector.findConnectedCore() == core, "Floor Injector 21 blocks below must link to Colossal Core");

		core.configureStablePreset(level, ExoplanetRoute.HEPHAESTUS_IV, WormholeTier.COLOSSAL);
		helper.assertTrue(
			core.activeTier() == WormholeTier.COLOSSAL && core.wormholeState() == WormholeState.OPEN,
			"Expected open COLOSSAL preset"
		);
		helper.assertTrue(
			level.getBlockState(coreAbs.below(WormholeTier.COLOSSAL.portalDrop())).is(ModBlocks.WORMHOLE_PORTAL),
			"Expected Colossal 11x11 ship gateway portal at y = core - 13"
		);

		// Verify player standing at the Floor Energy Injector 21 blocks below can keep the Colossal GUI open!
		Player deckPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
		BlockPos injectorPos = coreAbs.below(WormholeTier.COLOSSAL.injectorDrop());
		deckPlayer.setPos(injectorPos.getX() + 0.5, injectorPos.getY() + 1.0, injectorPos.getZ() + 0.5);
		helper.assertTrue(
			core.stillValid(deckPlayer),
			"Player standing at Colossal Floor Injector (21 blocks below core) must pass stillValid() to open GUI"
		);

		// Verify breaking 1 pylon on the East 3x3 array immediately cuts the East beam status
		helper.assertTrue(
			WormholeMultiblockValidator.isStabilizerSideIntact(level, coreAbs, WormholeTier.COLOSSAL, net.minecraft.core.Direction.EAST),
			"Expected intact East 3x3 stabilizer wall"
		);
		level.setBlock(coreAbs.offset(WormholeTier.COLOSSAL.stabilizerDistance(), 0, 0), Blocks.AIR.defaultBlockState(), 3);
		helper.assertFalse(
			WormholeMultiblockValidator.isStabilizerSideIntact(level, coreAbs, WormholeTier.COLOSSAL, net.minecraft.core.Direction.EAST),
			"Expected broken East 3x3 stabilizer wall to cut beam immediately"
		);

		// 2. Verify /galacticpurpose orbit updates Overworld PrimaryLevelData so exoplanets actually change dayTime!
		ServerLevel hephaestus = server.getLevel(
			ResourceKey.create(Registries.DIMENSION, ExoplanetRoute.HEPHAESTUS_IV.dimensionId())
		);
		helper.assertTrue(hephaestus != null, "Expected Hephaestus-IV dimension loaded");

		GalacticPurposeCommands.setServerOrbitalTime(server, 6000L);
		helper.assertTrue(
			Math.floorMod(hephaestus.getDayTime(), 24000L) == 6000L,
			"Expected Hephaestus-IV dayTime to snap immediately to zenith (6000)"
		);

		GalacticPurposeCommands.setServerOrbitalTime(server, 18000L);
		helper.assertTrue(
			Math.floorMod(hephaestus.getDayTime(), 24000L) == 18000L,
			"Expected Hephaestus-IV dayTime to snap immediately to nadir (18000)"
		);

		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "solar_energy_compat", skyAccess = true, timeoutTicks = 100)
	public void solarFluxGeneratorPowersMachinesAndLeadEnergyCellAndItemStorageWork(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		level.getServer().overworld().setDayTime(6000L); // noon sunny sky

		BlockPos genPos = new BlockPos(2, 2, 2);
		BlockPos distPos = new BlockPos(3, 2, 2);
		helper.setBlock(genPos.below(), Blocks.STONE);
		helper.setBlock(distPos.below(), Blocks.STONE);
		helper.setBlock(genPos, ModBlocks.SOLAR_FLUX_GENERATOR);
		helper.setBlock(distPos, ModBlocks.BIO_DISTILLER);

		// Verify universal ItemStorage.SIDED pipe lookup on Bio-Distiller
		helper.assertTrue(
			ItemStorage.SIDED.find(level, helper.absolutePos(distPos), null) != null,
			"Expected universal ItemStorage.SIDED lookup on Bio-Distiller for pipe mods"
		);

		// Verify LeadEnergyCellItem SimpleEnergyItem charge/discharge
		ItemStack cell = new ItemStack(ModItems.LEAD_ENERGY_CELL);
		LeadEnergyCellItem cellItem = (LeadEnergyCellItem) ModItems.LEAD_ENERGY_CELL;
		cellItem.setStoredEnergy(cell, 50_000L);
		helper.assertTrue(cellItem.getStoredEnergy(cell) == 50_000L, "Expected Lead Energy Cell to store 50,000 E");

		// Verify MissionCodexGrantState grants once per player UUID
		MissionCodexGrantState grants = MissionCodexGrantState.get(level.getServer().overworld());
		java.util.UUID testUuid = java.util.UUID.randomUUID();
		helper.assertTrue(grants.tryMarkGranted(testUuid), "First join should grant Mission Codex");
		helper.assertFalse(grants.tryMarkGranted(testUuid), "Second join must not duplicate Mission Codex");

		BioDistillerBlockEntity distiller = helper.getBlockEntity(distPos);
		SolarFluxGeneratorBlockEntity gen = helper.getBlockEntity(genPos);

		helper.runAfterDelay(20, () -> {
			helper.assertTrue(gen.lastOutputPerTick() > 0L, "Expected Solar Flux Generator to produce solar energy");
			helper.assertTrue(
				distiller.energyStorage().getAmount() > 0L,
				"Expected Solar Flux Generator to push energy via universal EnergyStorage.SIDED into adjacent machine"
			);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "exoplanet_identity", skyAccess = true, timeoutTicks = 120)
	public void exoplanetCustomTerrainBlocksAndLandmarksAndNativeFuelRecipesWork(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();

		// Verify all 4 exoplanet major ruins build cleanly with custom planet blocks & relic centerpieces
		ServerLevel heph = server.getLevel(ResourceKey.create(Registries.DIMENSION, ExoplanetRoute.HEPHAESTUS_IV.dimensionId()));
		ServerLevel pelagia = server.getLevel(ResourceKey.create(Registries.DIMENSION, ExoplanetRoute.PELAGIA_9.dimensionId()));
		ServerLevel nyx = server.getLevel(ResourceKey.create(Registries.DIMENSION, ExoplanetRoute.NYX_0.dimensionId()));
		ServerLevel sanct = server.getLevel(ResourceKey.create(Registries.DIMENSION, ExoplanetRoute.SANCTUARY.dimensionId()));
		helper.assertTrue(heph != null && pelagia != null && nyx != null && sanct != null, "All 4 Erebus dimensions must exist");

		BlockPos hephBase = ExoplanetLandmarkBuilder.ensureLandmarkBuilt(heph, ExoplanetRoute.HEPHAESTUS_IV);
		helper.assertTrue(heph.getBlockState(hephBase.above()).is(ModBlocks.FUSION_RELAY_WRECKAGE), "Hephaestus Forge Tower must contain Fusion Relay");
		helper.assertTrue(heph.getBlockState(hephBase.offset(-5, 0, -5)).is(ModBlocks.CHISELED_PYROCLAST), "Hephaestus Forge Tower must use custom Pyroclast blocks");

		BlockPos pelBase = ExoplanetLandmarkBuilder.ensureLandmarkBuilt(pelagia, ExoplanetRoute.PELAGIA_9);
		helper.assertTrue(pelagia.getBlockState(pelBase.above(2)).is(ModBlocks.ABYSSAL_VENT), "Pelagia Dome must contain Abyssal Bio-Vent");
		helper.assertTrue(pelagia.getBlockState(pelBase.offset(-5, 0, 0)).is(ModBlocks.PRESSURE_HULL_PLATING), "Pelagia Dome must use Pressure Hull Plating");

		BlockPos nyxBase = ExoplanetLandmarkBuilder.ensureLandmarkBuilt(nyx, ExoplanetRoute.NYX_0);
		helper.assertTrue(nyx.getBlockState(nyxBase.above(2)).is(ModBlocks.CRYO_BEACON), "Nyx Observatory must contain Cryo Beacon");
		helper.assertTrue(nyx.getBlockState(nyxBase.offset(-5, 0, -5)).is(ModBlocks.ZERO_K_PLATING), "Nyx Observatory must use Zero-K Plating");

		BlockPos sanctBase = ExoplanetLandmarkBuilder.ensureLandmarkBuilt(sanct, ExoplanetRoute.SANCTUARY);
		helper.assertTrue(sanct.getBlockState(sanctBase.offset(-4, 0, -4)).is(ModBlocks.ARK_TITANIUM_PLATING), "Sanctuary Monument must use Ark Titanium Plating");

		// Verify all 5 Starship Fuel Cell recipes (Bio-Fuel + 4 native exoplanet fuel paths) loaded in RecipeManager
		var rm = server.getRecipeManager();
		for (String id : new String[]{
			"starship_fuel_cell_bio",
			"starship_fuel_cell_hephaestus",
			"starship_fuel_cell_pelagia",
			"starship_fuel_cell_nyx",
			"starship_fuel_cell_sanctuary"
		}) {
			helper.assertTrue(
				rm.byKey(com.galacticpurpose.GalacticPurpose.id(id)).isPresent(),
				"Missing Starship Fuel Cell recipe: " + id
			);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "starship_link_lockout", skyAccess = true, timeoutTicks = 120)
	public void starshipOriginLockoutPropulsiveScannerRunwayAndWalkableOrbitalCabinWork(GameTestHelper helper) {
		ServerLevel overworld = helper.getLevel();
		MinecraftServer server = overworld.getServer();

		// 1. Build a stable Wormhole Core & confirm WormholeLinkRegistry marks it OPEN
		BlockPos coreRel = new BlockPos(4, 5, 4);
		BlockPos coreAbs = helper.absolutePos(coreRel);
		buildWormholeFrame(helper, coreRel);
		WormholeCoreBlockEntity core = helper.getBlockEntity(coreRel);
		core.configureStablePreset(overworld, ExoplanetRoute.HEPHAESTUS_IV, WormholeTier.PERSONAL);

		WormholeLinkRegistry registry = WormholeLinkRegistry.get(server);
		helper.assertTrue(
			registry.queryLiveStatus(server, overworld.dimension().location(), coreAbs) == WormholeLinkCalculator.LinkStatus.OPEN,
			"Open Wormhole Core must report OPEN in WormholeLinkRegistry"
		);

		// 2. Verify Natural Terrain Propulsive Landing Scanner (zero artificial launchpad; hydro-pontoons on Pelagia-9!)
		ServerLevel heph = server.getLevel(ResourceKey.create(Registries.DIMENSION, ExoplanetRoute.HEPHAESTUS_IV.dimensionId()));
		ServerLevel pelagia = server.getLevel(ResourceKey.create(Registries.DIMENSION, ExoplanetRoute.PELAGIA_9.dimensionId()));
		ServerLevel transit = server.getLevel(OrbitalTransitManager.ORBITAL_TRANSIT_KEY);
		helper.assertTrue(heph != null && pelagia != null && transit != null, "Hephaestus, Pelagia, and orbital_transit must exist");

		StarshipLandingCalculator.LandingSite hephSite = StarshipLandingCalculator.findNaturalLandingSite(heph, ExoplanetRoute.HEPHAESTUS_IV);
		StarshipLandingCalculator.LandingSite pelSite = StarshipLandingCalculator.findNaturalLandingSite(pelagia, ExoplanetRoute.PELAGIA_9);
		helper.assertFalse(hephSite.hydroPontoon(), "Hephaestus-IV natural landing should not deploy ocean hydro-pontoons");
		helper.assertTrue(pelSite.hydroPontoon(), "Pelagia-9 ocean landing must deploy buoyant hydro-pontoons");
		helper.assertFalse(
			heph.getBlockState(new BlockPos(hephSite.x(), hephSite.y(), hephSite.z())).is(ModBlocks.WORMHOLE_RETURN_BEACON),
			"Exoplanet landing sites must NOT auto-spawn single-block return beacons"
		);

		// 3. Simulate multiplayer sabotage: break Origin Wormhole Core -> live link MUST flip to DESTROYED!
		overworld.removeBlock(coreAbs, false);
		WormholeLinkCalculator.LinkStatus afterSabotage = registry.queryLiveStatus(server, overworld.dimension().location(), coreAbs);
		helper.assertTrue(
			afterSabotage == WormholeLinkCalculator.LinkStatus.DESTROYED,
			"Breaking Origin Wormhole must immediately flip status to DESTROYED, got " + afterSabotage
		);
		helper.assertTrue(
			WormholeLinkCalculator.evaluateOrbitalLaunch(true, afterSabotage, 2000) == WormholeLinkCalculator.LaunchDecision.ABORTED_WORMHOLE_OFFLINE,
			"Destroyed Origin Wormhole must strictly abort exoplanet liftoff"
		);

		// 4. Verify Magnetic Launch Runway + Horizontal Horizon Lander + Walkable 11x7x21 Panoramic Cabin & Cargo Sync!
		BlockPos runwayPad = WormholeMultiblockValidator.buildColossalLaunchRunway(overworld, coreAbs.above(16), Direction.NORTH);
		helper.assertTrue(
			overworld.getBlockState(runwayPad).is(ModBlocks.SHIP_ASSEMBLY_PAD),
			"Colossal Launch Runway must place Ship Assembly Pad at launch origin"
		);
		InterstellarShipEntity testShip = InterstellarShipEntity.spawnParked(overworld, coreAbs.getX() + 0.5, coreAbs.getY() + 1.0, coreAbs.getZ() + 0.5, 90.0f, 2500, false);
		testShip.inventory().setItem(0, new ItemStack(ModItems.FUSION_CORE, 3));
		var cargoTag = testShip.exportCargoTag(server.registryAccess());
		OrbitalShipCabinBuilder.buildCabin(transit, 0, cargoTag, server.registryAccess());

		BlockPos helmPos = OrbitalShipCabinBuilder.helmPos(0);
		BlockPos lockerPos = OrbitalShipCabinBuilder.cargoLockerPos(0);
		BlockPos frontGlass = OrbitalShipCabinBuilder.cabinOrigin(0).offset(0, 3, 10);
		BlockPos rearGlass = OrbitalShipCabinBuilder.cabinOrigin(0).offset(0, 3, -10);
		helper.assertTrue(transit.getBlockState(helmPos).is(ModBlocks.STARSHIP_HELM), "Orbital cabin must place Bridge Helm");
		helper.assertTrue(transit.getBlockState(frontGlass).is(ModBlocks.REINFORCED_VIEWPORT_GLASS), "Orbital cabin front nose must have panoramic Reinforced Viewport Glass");
		helper.assertTrue(transit.getBlockState(rearGlass).is(ModBlocks.REINFORCED_VIEWPORT_GLASS), "Orbital cabin stern must have unobstructed Reinforced Viewport Glass");
		var seats = transit.getEntitiesOfClass(com.galacticpurpose.entity.StarshipSeatEntity.class, new AABB(helmPos).inflate(4.0));
		helper.assertTrue(!seats.isEmpty(), "Orbital cabin must spawn a sittable Captain Command Chair behind the Bridge Helm");
		var lockerBe = transit.getBlockEntity(lockerPos);
		helper.assertTrue(
			lockerBe instanceof com.galacticpurpose.block.StarshipCargoLockerBlockEntity locker
				&& locker.getItem(0).is(ModItems.FUSION_CORE) && locker.getItem(0).getCount() == 3,
			"Orbital cabin Cargo Locker must preserve live synced ship cargo items"
		);
		testShip.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "starship_zero_g_pack_lock", timeoutTicks = 120)
	public void starshipZeroGPackUpAndArrivalLockWork(GameTestHelper helper) {
		ServerLevel overworld = helper.getLevel();
		MinecraftServer server = overworld.getServer();
		BlockPos spawn = helper.absolutePos(new BlockPos(4, 2, 4));
		helper.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);

		// 1. Verify Zero-G ship physics & Survival Pack-Up roundtrip (Fuel + Flux preserved, Cargo dropped safely)
		InterstellarShipEntity ship = InterstellarShipEntity.spawnParked(
			overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 90.0f, 1850, 420_000L, false
		);
		helper.assertTrue(ship.isNoGravity(), "Horizon Lander must always run zero-gravity (isNoGravity == true)");
		ship.inventory().setItem(0, new ItemStack(ModItems.FUSION_CORE, 2));

		boolean packed = ship.packUpIntoItem(null);
		helper.assertTrue(packed, "Parked ship must pack cleanly into a Blueprint item");
		helper.assertTrue(ship.isRemoved(), "Packed ship entity must be discarded");

		AABB dropBox = new AABB(spawn).inflate(4.0);
		var droppedItems = overworld.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, dropBox);
		ItemStack blueprintStack = ItemStack.EMPTY;
		boolean droppedCargo = false;
		for (var itemEnt : droppedItems) {
			ItemStack st = itemEnt.getItem();
			if (st.is(ModItems.STARSHIP_BLUEPRINT)) {
				blueprintStack = st;
			} else if (st.is(ModItems.FUSION_CORE) && st.getCount() == 2) {
				droppedCargo = true;
			}
			itemEnt.discard();
		}
		helper.assertTrue(!blueprintStack.isEmpty() && com.galacticpurpose.item.StarshipBlueprintItem.isPacked(blueprintStack),
			"Packing ship must drop a packed Starship Blueprint item");
		helper.assertTrue(droppedCargo, "Packing ship must safely drop onboard cargo items");
		helper.assertTrue(com.galacticpurpose.item.StarshipBlueprintItem.readPackedFuel(blueprintStack, 0) == 1850,
			"Packed blueprint must preserve exact remaining fuel");
		helper.assertTrue(com.galacticpurpose.item.StarshipBlueprintItem.readPackedEnergy(blueprintStack, 0L) == 420_000L,
			"Packed blueprint must preserve exact remaining flux energy");

		InterstellarShipEntity restored = InterstellarShipEntity.spawnParked(
			overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0.0f,
			com.galacticpurpose.item.StarshipBlueprintItem.readPackedFuel(blueprintStack, 0),
			com.galacticpurpose.item.StarshipBlueprintItem.readPackedEnergy(blueprintStack, 0L),
			false
		);
		helper.assertTrue(restored.fuelUnits() == 1850, "Restored ship must carry packed fuel");
		helper.assertTrue(restored.energyStored() == 420_000L, "Restored ship must carry packed flux energy");
		restored.discard();

		// 2. Verify glide approach spawn pre-loads landing chunks cleanly & starts in glide pitch -18 deg
		InterstellarShipEntity glideShip = InterstellarShipEntity.spawnForAutoDescent(
			overworld, spawn.getX() + 0.5, spawn.getY() + 60.0, spawn.getZ() + 0.5,
			spawn.getY(), false, 1500, 200_000L,
			new net.minecraft.nbt.CompoundTag(), server.registryAccess(),
			overworld.dimension().location(), spawn
		);
		helper.assertTrue(glideShip.pitchDeg() < 0.0f, "Glide approach must spawn nose-down (-18 deg), got " + glideShip.pitchDeg());
		helper.assertTrue(Math.abs(glideShip.getZ() - (spawn.getZ() + 78.5)) < 1.0,
			"Glide approach must spawn ~78 blocks upwind of target LZ at +46m height");
		helper.assertTrue(Math.abs(glideShip.getY() - (spawn.getY() + 46.0)) < 1.0,
			"Glide approach must spawn at landingY + 46m for natural smooth approach");
		glideShip.discard();

		// 3. Verify 90s deep-space transit Arrival Lock (cannot engage re-entry while elapsedTicks < CRUISE_DURATION_TICKS)
		var state = com.galacticpurpose.ship.OrbitalTransitState.get(server);
		java.util.UUID fakeId = java.util.UUID.randomUUID();
		var session = state.createSession(
			fakeId, false, ExoplanetRoute.HEPHAESTUS_IV,
			overworld.dimension().location(), spawn, 2000, 250_000L, new net.minecraft.nbt.CompoundTag()
		);
		session.elapsedTicks = 600; // only 30s into 90s transit!
		helper.assertFalse(
			OrbitalTransitManager.canEngageSession(session),
			"Mid-cruise session (< 1800 ticks) must reject early auto-descent activation"
		);
		session.elapsedTicks = com.galacticpurpose.ship.StarshipFlightPhysics.CRUISE_DURATION_TICKS;
		helper.assertTrue(
			OrbitalTransitManager.canEngageSession(session),
			"Session at arrival (>= 1800 ticks) must unlock auto-descent engagement"
		);
		state.removeSession(fakeId);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "starship_flight_director", timeoutTicks = 380)
	public void flightDirectorBindsConsumesDistanceFuelAndFerryLandsShip(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos start = helper.absolutePos(new BlockPos(2, 2, 2));
		BlockPos dest = helper.absolutePos(new BlockPos(28, 2, 2));
		for (int x = 1; x <= 30; x++) {
			helper.setBlock(new BlockPos(x, 1, 2), Blocks.STONE);
		}

		// Face AWAY (yaw = +90 -> -X, while dest is +X) to test zero-snap curved banking turn!
		InterstellarShipEntity ship = InterstellarShipEntity.spawnParked(
			level, start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 90.0f, 300, 100_000L, false
		);

		ItemStack director = new ItemStack(ModItems.FLIGHT_DIRECTOR);
		com.galacticpurpose.item.StarshipFlightDirectorItem.bindShip(director, ship, null);
		helper.assertTrue(
			ship.getUUID().equals(com.galacticpurpose.item.StarshipFlightDirectorItem.getBoundShipUuid(director)),
			"Flight Director must store bound ship UUID in CustomData"
		);

		// Too close (< 6 blocks) must reject without burning fuel
		helper.assertFalse(
			ship.tryStartRelocationFlight(null, start.getX() + 2.5, start.getY(), start.getZ() + 0.5),
			"Relocation < 6 blocks must be rejected"
		);
		helper.assertTrue(ship.fuelUnits() == 300, "Rejected relocation must not burn fuel");

		// Too far or insufficient fuel (> 240 blocks -> > 300 fuel) must reject without burning fuel
		helper.assertFalse(
			ship.tryStartRelocationFlight(null, start.getX() + 300.5, start.getY(), start.getZ() + 0.5),
			"Relocation exceeding remaining fuel must be rejected"
		);
		helper.assertTrue(ship.fuelUnits() == 300, "Insufficient-fuel relocation must not burn fuel");

		// Valid 26-block ferry flight: preserves initial heading (zero instant yaw snap) & starts live gradual fuel burn!
		int expectedCost = com.galacticpurpose.wormhole.WormholeLinkCalculator.computeRelocationFuelCost(26.0);
		boolean started = ship.tryStartRelocationFlight(null, dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5);
		helper.assertTrue(started, "Valid relocation must launch ship autonomously");
		helper.assertTrue(
			ship.shipMode() == InterstellarShipEntity.MODE_RELOCATING && Math.abs(ship.getYRot() - 90.0f) < 0.1f,
			"Expected MODE_RELOCATING and zero instant yaw snap at ignition"
		);
		helper.assertTrue(
			ship.fuelUnits() == 300,
			"Fuel should burn smoothly along the flight instead of disappearing in tick 0"
		);

		// Verify spool (0) -> banking climb turn (subPhase 1, rollDeg != 0) -> MODE_AUTO_DESCENT -> MODE_PARKED with exact total fuel consumed!
		helper.runAfterDelay(82, () -> {
			helper.assertTrue(
				ship.relocateSubPhase() == 1 || ship.shipMode() == InterstellarShipEntity.MODE_AUTO_DESCENT,
				"Expected ship to clear 3s spool and climb airborne"
			);
			helper.assertTrue(
				ship.fuelUnits() < 300 && Math.abs(ship.rollDeg()) > 4.0f,
				"Expected progressive fuel burn & visible airplane wing bank angle during curved turn, fuel="
					+ ship.fuelUnits() + " roll=" + ship.rollDeg()
			);
			helper.runAfterDelay(210, () -> {
				double finalDist = Math.hypot(ship.getX() - (dest.getX() + 0.5), ship.getZ() - (dest.getZ() + 0.5));
				helper.assertTrue(
					finalDist <= 2.5 && ship.shipMode() == InterstellarShipEntity.MODE_PARKED && ship.fuelUnits() == 300 - expectedCost,
					"Expected curved-turn ferry ship to reach LZ parked with exact final fuel, dist=" + finalDist
						+ " mode=" + ship.shipMode() + " fuel=" + ship.fuelUnits()
				);
				ship.discard();
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "starship_gravity_blast", timeoutTicks = 100)
	public void parkedShipFallsUnderGravityOverCraterAndReactsToBlastImpulse(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos lowerFloor = helper.absolutePos(new BlockPos(4, 1, 4));
		helper.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
		helper.setBlock(new BlockPos(4, 5, 4), Blocks.STONE);

		BlockPos topPad = helper.absolutePos(new BlockPos(4, 6, 4));
		InterstellarShipEntity ship = InterstellarShipEntity.spawnParked(
			level, topPad.getX() + 0.5, topPad.getY(), topPad.getZ() + 0.5, 0.0f, 1000, 100_000L, false
		);
		ship.applyBlastImpulse(new net.minecraft.world.phys.Vec3(0.0, 0.28, 0.0), 14.0f, -18.0f);
		helper.setBlock(new BlockPos(4, 5, 4), Blocks.AIR);

		helper.runAfterDelay(15, () -> {
			helper.assertTrue(
				Math.abs(ship.rollDeg()) > 0.5f && ship.getY() != topPad.getY(),
				"Blast impulse & crater void must move/rock parked lander under planetary gravity"
			);
			helper.runAfterDelay(45, () -> {
				helper.assertTrue(
					Math.abs(ship.getY() - (lowerFloor.getY() + 1.0)) <= 0.35,
					"Parked ship must settle cleanly onto lower crater floor y=" + (lowerFloor.getY() + 1.0) + ", was " + ship.getY()
				);
				ship.discard();
				helper.succeed();
			});
		});
	}

	private static void buildWormholeFrame(GameTestHelper helper, BlockPos corePos) {
		WormholeMultiblockValidator.buildStructure(helper.getLevel(), helper.absolutePos(corePos), WormholeTier.PERSONAL);
	}
}
