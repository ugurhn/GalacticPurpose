# Interstellar Starship, Orbital Transit & Exoplanet Identity Implementation Plan

> **For Antigravity:** REQUIRED WORKFLOW: Use `.agent/workflows/execute-plan.md` to execute this plan in single-flow mode.

**Goal:** Transform Galactic Purpose into a full-scale Interstellar spaceflight mod with 36–72 block smooth Wormhole ambience, a 1.7× larger Erebus Black Hole, ~42 custom exoplanet & starship blocks with 4 major ruins, a 3D vertical SpaceX-landing Interstellar Starship with Origin Wormhole Link lockout & native planetary refueling, and a walkable 360° panoramic-glass `orbital_transit` spaceflight experience.

**Architecture:**
1. **Audio & Sky (Milestone 6):** Client-side `WormholeAmbienceSoundInstance` (`AbstractTickableSoundInstance`) driven by active `WormholeCoreBlockEntity` state/tier + 1.7× scale boost in `ErebusSkyRenderer`.
2. **Planet Identity & Ruins (Milestone 7):** Bulk data-driven registration of ~42 custom blocks + `ignis` & `starship_fuel_cell` items, procedural 16×16 pixel-art textures, blockstates/models/loot tables/crafting+stonecutter recipes, upgraded `noise_settings` for all 4 exoplanets, and 4 multi-story architectural ruins in `ExoplanetLandmarkBuilder`.
3. **SpaceX Starship & Origin Lockout (Milestone 8):** `WormholeLinkRegistry` Overworld `SavedData` tracking every open/closed/destroyed Wormhole Core; `InterstellarShipEntity` (vertical 9-block SpaceX Starship with 4 unfolding landing legs, natural-ground auto-land scanner, hydro-pontoons on Pelagia-9, free 3D flight, roaring vertical liftoff, 27-slot Cargo + 500k E Energy + Fuel Tank, cockpit HUD, and `ShipAssemblyPadBlock`).
4. **Walkable Orbital Spaceflight (Milestone 9):** `galacticpurpose:orbital_transit` multiplayer-slotted void dimension + `OrbitalTransitManager` + `11×7×21` walkable panoramic-glass Starship Deck (`StarshipHelmBlock`, live synced `StarshipCargoLockerBlock`, refuel terminal) + `OrbitalTransitSkyRenderer` (unique rotating 3D planets, Exit/Origin Wormhole sphere, 1.7× Erebus Black Hole, star streaks, `F5` thruster plumes, atmospheric re-entry flames) + updated `MissionCodexScreen` chapter & `/galacticpurpose ship` commands.

**Tech Stack:** Minecraft 1.21.1, Fabric Loader 0.20.1, Fabric API 0.116.17, Official Mojang Mappings, TeamReborn Energy 4.1.0, Java 21, JUnit 5, Fabric GameTest.

---

## Milestone 6 · Continuous 36–72 Block Wormhole Audio & 1.7× Larger Erebus Black Hole

### Task 6.1: Distance-Attenuated Continuous Wormhole Ambience Sound
**Files:**
- Create: `src/client/java/com/galacticpurpose/client/sound/WormholeAmbienceSoundInstance.java`
- Modify: `src/client/java/com/galacticpurpose/client/sound/ClientSoundController.java`
- Modify: `src/client/java/com/galacticpurpose/client/render/WormholeCoreBlockEntityRenderer.java`
- Modify: `src/main/java/com/galacticpurpose/block/WormholeCoreBlockEntity.java`

**Implementation:**
- Create `WormholeAmbienceSoundInstance extends AbstractTickableSoundInstance` bound to a `BlockPos` (`SoundEvents.CONDUIT_AMBIENT` for OPEN / `SoundEvents.BEACON_AMBIENT` for CHARGING / looping `WARDEN_HEARTBEAT` accent on UNSTABLE) with `Attenuation.NONE` and manual smooth distance falloff curve:
  - `PERSONAL`: `maxRange = 36.0` blocks (`volume = clamp(1.0 - dist / 36.0)^1.35 * 1.15f`).
  - `COLOSSAL`: `maxRange = 72.0` blocks (`volume = clamp(1.0 - dist / 72.0)^1.25 * 1.35f`).
  - Dynamic pitch modulation based on `WormholeState`, `chargePercent`, and `tempCelsius`.
  - Automatically stops when the `WormholeCoreBlockEntity` is removed, becomes `IDLE`, or the player moves past `maxRange + 12`.
- Hook `ClientSoundController.updateWormholeAmbience(WormholeCoreBlockEntity core)` from `WormholeCoreBlockEntityRenderer` (or client tick) and keep server-side `UNSTABLE` warning accents (`WARDEN_HEARTBEAT` / `BEACON_DEACTIVATE`) at wider range (`vol = 3.2f` Colossal / `1.8f` Personal).

**Verification:**
- `.\gradlew.bat test runGameTest build --no-daemon` -> PASS; in-game verify smooth hum audible across entire 45×41×45 observatory platform and fading smoothly up to ~70 blocks away.

### Task 6.2: Enlarge Erebus Black Hole Angular Volume (~1.7× Gargantua Scale)
**Files:**
- Modify: `src/client/java/com/galacticpurpose/client/sky/ErebusSkyRenderer.java`

**Implementation:**
- Scale `horizonRadius`, `lensingHalo`, upper/lower Einstein arches, and inner/outer tilted accretion disk spans by **`1.7×`** across all 4 Erebus dimensions (preserving crisp 83° tilt, soft inner fade, and 120 FPS single-pass rendering).
- Expose reusable static helper `ErebusSkyRenderer.renderErebusBlackHole(PoseStack, float scale, float time, float accentTint)` so `OrbitalTransitSkyRenderer` (Milestone 9) renders the exact same iconic giant black hole in deep space!

**Verification:**
- Compile & run `.\gradlew.bat test build --no-daemon` -> PASS.

---

## Milestone 7 · 4 Exoplanets Full Identity (~42 Custom Blocks, Native Fuel Materials & 4 Major Ruins)

### Task 7.1: Register ~42 Planet & Starship Blocks + Native Fuel Items
**Files:**
- Modify: `src/main/java/com/galacticpurpose/registry/ModBlocks.java`
- Modify: `src/main/java/com/galacticpurpose/registry/ModItems.java`
- Modify: `src/main/java/com/galacticpurpose/registry/ModItemGroups.java`
- Create: `src/main/java/com/galacticpurpose/block/ExoplanetFloraBlock.java` (glowing underwater/surface alien flora for `luminescent_sea_pod`, `frozen_quantum_spire`, `sanctuary_blossom`)
- Create: `src/main/java/com/galacticpurpose/item/StarshipFuelCellItem.java`

**Implementation:**
- Register all 4 planet block families (8 blocks each: terrain, sub-rock/ore, bricks, carved/chiseled, rotatable `RotatedPillarBlock` pillar, emissive lamp, flora/ash/plating) + 10 Starship & Engineering blocks (`starship_hull_plating`, `starship_ribbed_panel`, `starship_grate`, `reinforced_viewport_glass`, `ship_assembly_pad`, `starship_helm`, `starship_cargo_locker`, `starship_thruster_block`, `starship_light_strip`, `ignis_alloy_block`).
- Register `raw_ignis`, `ignis_ingot`, `starship_fuel_cell`, `starship_blueprint`.
- Group them cleanly in the `Galactic Purpose` creative tab.

### Task 7.2: Procedural Pixel-Art Textures, Models, Blockstates, Loot Tables, Crafting & Stonecutter Recipes, Translations
**Files:**
- Create scratch generator script (persisted in artifact scratch dir) to generate crisp, cohesive 16×16 PNG pixel-art textures for all ~42 blocks and 4 items in `src/main/resources/assets/galacticpurpose/textures/{block,item}/`.
- Generate matching blockstates, block/item models, block loot tables (`ignis_ore` drops `raw_ignis` with Fortune/Silk Touch support), crafting + Stonecutter JSON recipes (including all 4 native exoplanet `starship_fuel_cell` refueling recipes + `#c:ingots/ignis` tags & translations).
- Add complete English (`en_us.json`) and Turkish (`tr_tr.json`) names and tooltips.

### Task 7.3: Upgrade All 4 Exoplanet Worldgen (`noise_settings` + `placed_feature` Ores/Flora) & 4 Major Architectural Ruins
**Files:**
- Modify: `src/main/resources/data/galacticpurpose/worldgen/noise_settings/{hephaestus_iv,pelagia_9,nyx_0,sanctuary}.json`
- Modify: `src/main/resources/data/galacticpurpose/worldgen/biome/{hephaestus_iv,pelagia_9,nyx_0,sanctuary}.json`
- Create: `src/main/resources/data/galacticpurpose/worldgen/{configured_feature,placed_feature}/ignis_ore.json` + planet flora/crystal features
- Create: `src/main/java/com/galacticpurpose/exoplanet/ExoplanetLandmarkBuilder.java`
- Modify: `src/main/java/com/galacticpurpose/wormhole/ExoplanetTravelHandler.java`

**Implementation:**
- Swap `noise_settings` terrain from vanilla blocks to native planet blocks (`scorched_basalt` / `volcanic_slagstone`, `abyssal_shale` / `abyssal_coralstone`, `cryo_slate` / `frost_obsidian`, `sanctuary_loam` / `luminous_limestone`).
- Build 4 rich multi-story discoverable ruins (`Ancient Fusion Forge Tower`, `Sunken Abyssal Research Dome`, `Frozen Quantum Observatory Ruin`, `New Home Colony Monument`) using the new custom blocks, lamps, pillars, glass, relic blocks, and emergency supply chests (with logs + spare `starship_fuel_cell` & `lead_energy_cell`).
- **Remove** the artificial 5×5 landing stone pad and 1-block `Return Tether` auto-placement when arriving via Starship!

**Verification (TDD):**
- Add GameTest `exoplanetCustomTerrainBlocksAndLandmarksAndNativeFuelRecipesWork` verifying all 4 ruins generate with custom blocks & loot chests, and all 4 native fuel recipes resolve cleanly.
- Run: `.\gradlew.bat test runGameTest build --no-daemon` -> Expected: ALL JUnit & GameTests PASS, 0 `[ERROR]`/`[WARN]`.

---

## Milestone 8 · SpaceX Propulsive Starship (`InterstellarShipEntity`), Origin Wormhole Lockout & Assembly Pad

### Task 8.1: `WormholeLinkRegistry` (Origin Wormhole Link & Sabotage Lockout Logic)
**Files:**
- Create: `src/main/java/com/galacticpurpose/wormhole/WormholeLinkCalculator.java` (pure logic for unit testing)
- Create: `src/main/java/com/galacticpurpose/wormhole/WormholeLinkRegistry.java` (`SavedData` on Overworld)
- Modify: `src/main/java/com/galacticpurpose/block/WormholeCoreBlockEntity.java` (register OPEN, mark CLOSED on quench/power-loss, mark DESTROYED on break/meltdown)
- Create: `src/test/java/com/galacticpurpose/wormhole/WormholeLinkCalculatorTest.java`

**Step 1 (TDD):** Write unit tests for `WormholeLinkCalculator`:
- Open link + sufficient fuel -> `LAUNCH_READY`
- Closed / Destroyed / Melted origin link -> `ABORTED_WORMHOLE_OFFLINE`
- Open link + zero fuel -> `ABORTED_NO_FUEL`
- Unloaded chunk with `OPEN` registry state -> stays `LAUNCH_READY` (singleplayer safe!)
- Loaded chunk where core block is missing or `!= OPEN` -> immediately transitions to `ABORTED_WORMHOLE_OFFLINE` (multiplayer troll/sabotage proof!)

### Task 8.2: SpaceX Terrain Scanner & Propulsive Vertical Landing Physics (`StarshipLandingCalculator`)
**Files:**
- Create: `src/main/java/com/galacticpurpose/ship/StarshipLandingCalculator.java`
- Create: `src/test/java/com/galacticpurpose/ship/StarshipLandingCalculatorTest.java`

**Step 1 (TDD):** Write unit tests verifying:
- Flattest natural non-lava clearing selection within `48×48` around `(0, 0)` (rejects lava, avoids sheer cliffs, returns surface ocean Y + `hydroPontoon=true` on Pelagia-9).
- Smooth SpaceX suicide-burn descent velocity curve (`vy` smoothly brakes from `-2.4` at `Y+130` to `-0.08` at touchdown, legs deploy at `altitude <= 18.0`, touchdown locks at `0.0`).

### Task 8.3: `InterstellarShipEntity`, Ship Inventory/Fuel Menu, `ShipAssemblyPadBlock` & 3D Renderer
**Files:**
- Create: `src/main/java/com/galacticpurpose/entity/InterstellarShipEntity.java`
- Create: `src/main/java/com/galacticpurpose/menu/StarshipMenu.java`
- Create: `src/main/java/com/galacticpurpose/block/ShipAssemblyPadBlock.java`
- Create: `src/main/java/com/galacticpurpose/network/StarshipActionPayload.java` (C2S: ignite launch, toggle hover flight, open cargo, trigger SpaceX auto-land)
- Create: `src/client/java/com/galacticpurpose/client/render/InterstellarShipRenderer.java` (sleek 9-block SpaceX Starship 3D model, metallic hull, panoramic dark-glass nosecone, 4 animated hydraulic landing legs, Pelagia hydro-pontoons, blazing shock-diamond plasma plumes, dust ring)
- Create: `src/client/java/com/galacticpurpose/client/hud/StarshipCockpitHud.java` (1st-person glass cockpit frame + telemetry gauges: altitude, velocity, fuel, energy, Origin Wormhole Link GREEN/RED indicator)
- Create: `src/client/java/com/galacticpurpose/client/screen/StarshipScreen.java` (27-slot Cargo Bay + Fuel Cell slot + Energy Cell slot + Launch/Hover controls)
- Modify: `src/main/java/com/galacticpurpose/block/WormholeCoreBlockEntity.java` (detect riding `InterstellarShipEntity` flying straight into 45×41×45 Colossal Wormhole -> preserve ship cargo/fuel/energy & OriginLink -> launch into `orbital_transit`!)
- Modify: `src/main/java/com/galacticpurpose/registry/{ModEntities,ModMenus,ModNetworking}.java` & `GalacticPurposeClient.java`

**Verification (TDD):**
- Add GameTest `starshipAssemblyOriginLockoutAndSpaceXLandingWork`:
  - Assemble ship on `ShipAssemblyPadBlock`
  - Verify launch succeeds when Origin Wormhole is `OPEN` + fueled
  - Break Origin Wormhole -> verify ship telemetry reports `SIGNAL LOST` and aborts launch!
  - Verify SpaceX auto-landing descends onto natural ground with zero artificial launchpad blocks placed!
- Run: `.\gradlew.bat test runGameTest build --no-daemon` -> Expected: ALL JUnit & GameTests PASS.

---

## Milestone 9 · Walkable 360° Panoramic-Glass Orbital Spaceflight (`orbital_transit`), Codex & Commands

### Task 9.1: `galacticpurpose:orbital_transit` Void Space Dimension & Multiplayer Slot Builder
**Files:**
- Create: `src/main/resources/data/galacticpurpose/dimension/orbital_transit.json`
- Create: `src/main/resources/data/galacticpurpose/dimension_type/orbital_transit.json`
- Create: `src/main/resources/data/galacticpurpose/worldgen/biome/orbital_transit.json`
- Create: `src/main/java/com/galacticpurpose/ship/OrbitalTransitState.java` (`SavedData` storing active transit sessions: slotId, playerUUID, outbound vs return leg, destination/origin route, progress 0→1, ship Cargo/Fuel/Energy snapshot, OriginLink)
- Create: `src/main/java/com/galacticpurpose/ship/OrbitalShipCabinBuilder.java` (builds the sleek walkable `11×7×21` multi-windowed Starship Deck at `(slot * 256, 120, 0)` with front panoramic bridge glass, side & overhead skylights, rear wormhole viewport, `StarshipHelmBlock`, synced `StarshipCargoLockerBlockEntity`, refuel terminal, crafting bench)
- Create: `src/main/java/com/galacticpurpose/block/StarshipHelmBlock.java`
- Create: `src/main/java/com/galacticpurpose/block/StarshipCargoLockerBlock.java` & `StarshipCargoLockerBlockEntity.java`
- Create: `src/main/java/com/galacticpurpose/network/OrbitalTransitSyncPayload.java` (syncs transit phase, leg direction, planet route, approach progress, Origin Wormhole Link status, re-entry flash state to client)

### Task 9.2: `OrbitalTransitSkyRenderer` (Unique Rotating 3D Planets + Exit/Origin Wormhole + 1.7× Erebus + Thruster Plumes & Re-Entry)
**Files:**
- Create: `src/client/java/com/galacticpurpose/client/sky/OrbitalTransitSkyRenderer.java`
- Create: `src/client/java/com/galacticpurpose/client/sound/StarshipEngineSoundInstance.java` (deep cabin engine rumble + atmospheric entry roar + SpaceX liftoff thrust)
- Modify: `src/client/java/com/galacticpurpose/client/GalacticPurposeClient.java` (register `orbital_transit` `DimensionSpecialEffects` & thruster/re-entry world render hook)

**Implementation:**
- **Outbound (Wormhole → Planet):**
  - Rear (`-Z`): Glowing 3D **Exit Wormhole Sphere** shrinking smoothly into the dark void.
  - Overhead/Side: **1.7× Colossal Erebus Black Hole** bending light across the starfield.
  - Front (`+Z`): **Huge Rotating 3D Destination Planet** (custom layered volcanic / ocean-storm / cryo-quantum / emerald-cloud procedural 3D sphere!) growing steadily outside the panoramic bridge glass as starlight streaks past.
  - When player clicks **`[INITIATE SPACEX AUTO-LANDING]`** on `StarshipHelmBlock` (or after optional auto-engage): 3-second orange re-entry plasma flash wraps outside the windows -> transitions player + live cargo/fuel into `InterstellarShipEntity` descending via SpaceX Auto-Landing onto the planet!
- **Return (Planet → Origin Wormhole):**
  - Rear (`-Z`): Departing planet shrinks away behind the rear observation window.
  - Front (`+Z`): Glowing **Origin Wormhole Sphere** grows huge right ahead of the bridge glass!
  - Engaging **`[ENTER WORMHOLE]`** (or arriving at `progress == 1.0`) returns player + `InterstellarShipEntity` + all 27 cargo slots cleanly back to the exact **Origin Wormhole Observatory** coordinates!

### Task 9.3: Mission Codex Chapter Update, Test Commands & Full End-to-End Verification
**Files:**
- Modify: `src/main/java/com/galacticpurpose/command/GalacticPurposeCommands.java` (add `/galacticpurpose ship spawn`, `/galacticpurpose transit <planet>`, `/galacticpurpose link status`)
- Modify: `src/client/java/com/galacticpurpose/client/screen/MissionCodexScreen.java` & `en_us.json` / `tr_tr.json` (document Starship Assembly, Orbital Spaceflight, SpaceX Auto-Landing, Native Refueling & Origin Wormhole Lockout!)
- Modify: `src/gametest/java/com/galacticpurpose/gametest/GalacticPurposeGameTests.java` (add end-to-end `orbitalTransitWalkableCabinCargoSyncAndReturnLinkWork` GameTest)

**Final Verification:**
- Run: `.\gradlew.bat test --rerun-tasks runGameTest build --no-daemon`
- Verify: `0` test failures, `0` `[ERROR]` / `[WARN]` in GameTest logs, clean remapped JAR in `build/libs/galactic-purpose-0.1.0-alpha.1.jar`.
- Update `docs/plans/task.md` as each milestone finishes.
