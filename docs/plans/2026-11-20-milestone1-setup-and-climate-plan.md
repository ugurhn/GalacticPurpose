# Milestone 0 & Milestone 1 Implementation Plan

> **For Antigravity:** Follow single-flow task execution step-by-step. Wait for user approval after Task 1 (Project Skeleton & Gradle Verification) before beginning Milestone 1 gameplay features!

**Goal:** Establish a cleanly compiling Minecraft 1.21.1 Fabric (Java 21 target) project skeleton with Galacticraft 5 / TeamReborn Energy compatibility boundaries, then implement Milestone 1 (Overworld progressive solar instability & temperature/radiation mechanics).

**Architecture:** Split `main` (server/common logic) and `client` (sky/HUD rendering) source sets via `net.fabricmc.fabric-loom-remap` with `officialMojangMappings()`. Isolate external Galacticraft calls behind `SpaceCompat` so the build compiles cleanly offline from GC5's unstable Maven while automatically bridging GC5 when present at compile/runtime.

**Tech Stack:** Minecraft 1.21.1, Fabric Loom Remap 1.19.4, Fabric Loader 0.20.1, Fabric API 0.116.17+1.21.1, TeamReborn Energy 4.1.0, Java 21 bytecode (Gradle 9.9.2 wrapper).

---

### Task 1 (Milestone 0 — Onay Kapısı): Project Skeleton & Gradle Setup

**Files:**
- Create: `LICENSE` (MIT)
- Create: `.gitignore`
- Create: `README.md`
- Create: `gradle.properties`
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`
- Create: `libs/.gitkeep`
- Create: `src/main/resources/fabric.mod.json`
- Create: `src/main/resources/galacticpurpose.mixins.json`
- Create: `src/client/resources/galacticpurpose.client.mixins.json`
- Create: `src/main/resources/assets/galacticpurpose/lang/en_us.json`
- Create: `src/main/resources/assets/galacticpurpose/lang/tr_tr.json`
- Create: `src/main/java/com/galacticpurpose/GalacticPurpose.java`
- Create: `src/main/java/com/galacticpurpose/compat/SpaceCompat.java`
- Create: `src/main/java/com/galacticpurpose/compat/SpaceCompatManager.java`
- Create: `src/client/java/com/galacticpurpose/client/GalacticPurposeClient.java`

**Verification:**
- Run `.\gradlew.bat build` and verify exit code 0 and produced JARs in `build/libs/`.
- **STOP and get user approval** on folder structure and `build.gradle` before proceeding to Task 2.

---

### Task 2 (Milestone 1 — Onay Sonrası): Config & Overworld Collapse State

**Files:**
- Create: `src/main/java/com/galacticpurpose/config/ModConfig.java`
- Create: `src/main/java/com/galacticpurpose/climate/EarthCollapseState.java`
- Create: `src/main/java/com/galacticpurpose/climate/SolarInstabilityCalculator.java`
- Test: `src/test/java/com/galacticpurpose/climate/SolarInstabilityCalculatorTest.java`

### Task 3 (Milestone 1 — Onay Sonrası): Overworld Day/Night Temperature, Radiation & Safe Shelter Rules

**Files:**
- Create: `src/main/java/com/galacticpurpose/climate/OverworldHazardTicker.java`
- Create: `src/main/java/com/galacticpurpose/registry/ModDamageTypes.java`
- Create: `src/main/java/com/galacticpurpose/network/ClimateSyncPayload.java`
- Modify: `src/main/java/com/galacticpurpose/GalacticPurpose.java`

### Task 4 (Milestone 1 — Onay Sonrası): Client Warning HUD & Atmospheric Visual Feedback

**Files:**
- Create: `src/client/java/com/galacticpurpose/client/hud/ClimateHudOverlay.java`
- Create: `src/client/java/com/galacticpurpose/client/sky/SolarInstabilityEffects.java`
- Modify: `src/client/java/com/galacticpurpose/client/GalacticPurposeClient.java`
