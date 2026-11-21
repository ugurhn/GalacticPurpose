# Galactic Purpose — Interstellar Starship, Space Transit & Exoplanet Identity Design

**Date:** 2026-11-21  
**Target Platform:** Minecraft 1.21.1 • Fabric Loader • Java 21 • Standalone & Space-Mod Compatible  
**License:** MIT (100% original copyright-free models, procedural shaders/renderers, textures, and lore)

---

## 1. Vision & Core Realism Pillars

1. **Wormholes Belong in Deep Space, Not on Planet Dirt:**
   - Opening the 45×41×45 Colossal Wormhole Observatory on Mars/Earth creates a spacetime tunnel whose exit aperture hangs **in orbital deep space above the destination exoplanet** — never a tiny 1-block Return Tether on a stone pad.
2. **Walkable 360° Panoramic Starship + SpaceX Propulsive Vertical Landing:**
   - **In Deep Space (`galacticpurpose:orbital_transit`):** The player walks freely inside a full-scale (`11×7×21`) glass-canopy Starship Cabin (`Starship Hull Plating` + panoramic `Reinforced Viewport Glass` front/sides/roof/rear). Instead of moving blocks (which jitters without Valkyrien Skies), the **deep space sky moves dynamically around the ship**: behind the rear window shrinks the Exit Wormhole, overhead looms the **1.7× larger Erebus Black Hole**, and ahead of the front bridge glass grows a **huge, unique rotating 3D planet** (`Hephaestus-IV`, `Pelagia-9`, `Nyx-0`, `Sanctuary`, or `Mars/Earth`). The player can walk to their live synced Cargo Locker, equip Thermal/Pressure Armor, refuel, look out any window (or view the full ship exterior in `F5` with blazing thruster plumes), and engage **Auto-Landing** from the `StarshipHelmBlock`.
   - **Atmospheric Entry, SpaceX Landing & Planetary Flight (`InterstellarShipEntity`):** Engaging Auto-Landing triggers atmospheric re-entry flames and spawns the player inside the real 3D vertical SpaceX-style Starship (`~9` blocks tall, metallic fuselage, glass nosecone cockpit, aero fins, 4 articulated hydraulic landing legs) at `groundY + 135`. An automated terrain scanner picks the flattest natural non-lava clearing (**zero artificial launchpad blocks spawned!**; on ocean-world Pelagia-9, buoyant hydro-pontoons deploy directly onto the water). Retro-thrusters fire a SpaceX suicide burn, landing legs unfold at `Y + 18`, a ground dust/steam ring blasts outward, and the ship touches down smoothly onto the untouched alien surface.
3. **Interstellar Isolation & Origin Wormhole Link Lockout (`WormholeLinkRegistry`):**
   - Every Colossal Wormhole Core registers its live state `(coreId, dim, pos, OPEN/CLOSED/DESTROYED)` in Overworld `SavedData` (`WormholeLinkRegistry`).
   - When a Starship flies through a Colossal Wormhole, it binds to that exact **Origin Wormhole Link**.
   - To leave an exoplanet, no galaxy-map UI is required: the ship's Quantum Telemetry automatically targets the bound Origin Wormhole.
   - **Real Sabotage / Failure Consequences:** If a multiplayer friend breaks, closes, starves of coolant/fuel, or blows up the origin Wormhole back on Mars/Earth, `WormholeLinkRegistry` marks it offline. The ship's cockpit alarms **`[SIGNAL LOST] Origin Wormhole Offline — Launch Aborted!`** and refuses to launch into empty void. (In singleplayer, unloaded open cores remain `OPEN` so solo exploration never false-triggers!)
   - **In-Planet Refueling:** Every exoplanet provides native resources (`Ignis Ore`, `Luminescent Sea Pod`, `Frost Obsidian + Quantum Shards`, `Sanctuary Blossom`) craftable into `Starship Fuel Cells` so players can refuel for liftoff on-site.
4. **36–72 Block Smooth Wormhole Ambience & 1.7× Larger Erebus Black Hole:**
   - Replace short vanilla `playSound` ticks (which clip at ~16 blocks) with a continuous client `WormholeAmbienceSoundInstance` attenuating smoothly out to `36` blocks (Personal) and `72` blocks (Colossal).
   - Enlarge `ErebusSkyRenderer` angular scale by `~1.7×` so the black hole truly dominates the sky both on exoplanets and during orbital transit.
5. **Full Exoplanet Visual Identity (~42 New Blocks + 4 Major Discoverable Ruins):**
   - Replace all vanilla placeholder terrain across the 4 exoplanets with unique custom blocks, ores, bricks, carved tiles, pillars, glowing lamps, alien flora, and starship hull blocks (all with Crafting + Stonecutter recipes and REI/JEI/EMI support), plus 4 architectural landmark ruins containing mission relics and emergency fuel caches.

---

## 2. System Architecture & Data Flow

```text
[Mars/Earth 45x41x45 Colossal Wormhole (OPEN)]
        │  (1) Player flies InterstellarShipEntity straight into glowing core
        │      -> Saves OriginLink(originDim, originPos, route) + Cargo(27) + Fuel + Energy
        ▼
[galacticpurpose:orbital_transit (Multiplayer X-Slot: slot * 256, Y=120, Z=0)]
        │  (2) Walkable 11x7x21 Panoramic Glass Starship Deck (live synced Cargo + Helm)
        │      -> OrbitalTransitSkyRenderer: Rear Exit Wormhole, Overhead 1.7x Erebus,
        │         Approaching Unique 3D Planet, Star Streaks, F5 Exterior Plumes
        │  (3) Player triggers "Initiate SpaceX Auto-Landing" at StarshipHelmBlock
        ▼
[Destination Exoplanet (Hephaestus-IV / Pelagia-9 / Nyx-0 / Sanctuary)]
        │  (4) Auto-Land Scanner finds flattest natural ground (no artificial pad!)
        │      -> SpaceX Vertical Retro-Burn + Unfolding Landing Legs + Soft Touchdown
        │  (5) Explore custom terrain & major ruin, loot relic, refuel from native blocks
        │  (6) Board parked Starship -> Origin Wormhole Link check via WormholeLinkRegistry
        │      -> If CLOSED/DESTROYED: RED [SIGNAL LOST] Launch Lockout!
        │      -> If OPEN + Fueled: Roaring SpaceX Vertical Liftoff (Y -> +260)
        ▼
[galacticpurpose:orbital_transit (Return Leg)]
        │  (7) Departing Planet shrinks behind rear glass; Origin Wormhole grows ahead
        │  (8) Enter Wormhole -> Emerge right back at Mars/Earth Observatory with Ship & Cargo!
```

---

## 3. Content Breakdown

### 3.1 Custom Blocks (~42 Blocks, All With Textures, Models, Loot Tables & Recipes)
- **Hephaestus-IV (8):** `scorched_basalt`, `volcanic_slagstone`, `ignis_ore`, `pyroclast_bricks`, `chiseled_pyroclast`, `pyroclast_pillar`, `magma_conduit_lamp`, `ember_ash`
- **Pelagia-9 (8):** `abyssal_shale`, `abyssal_coralstone`, `pelagic_bricks`, `carved_pelagic_tile`, `pelagic_pillar`, `bioluminescent_lamp`, `luminescent_sea_pod`, `pressure_hull_plating`
- **Nyx-0 (8):** `cryo_slate`, `frost_obsidian`, `cryo_bricks`, `carved_cryo_tile`, `cryo_pillar`, `void_crystal_lamp`, `frozen_quantum_spire`, `zero_k_plating`
- **Sanctuary (8):** `sanctuary_loam`, `luminous_limestone`, `sanctuary_bricks`, `carved_sanctuary_stone`, `sanctuary_pillar`, `aether_lantern`, `sanctuary_blossom`, `ark_titanium_plating`
- **Starship & Engineering Set (10):** `starship_hull_plating`, `starship_ribbed_panel`, `starship_grate`, `reinforced_viewport_glass`, `ship_assembly_pad`, `starship_helm`, `starship_cargo_locker`, `starship_thruster_block`, `starship_light_strip`, `ignis_alloy_block`

### 3.2 New Items
- `raw_ignis`, `ignis_ingot` (smelted from `ignis_ore`)
- `starship_fuel_cell` (craftable from Exotic Bio-Fuel OR native Hephaestus/Pelagia/Nyx/Sanctuary resources!)
- `starship_blueprint` (used on `ship_assembly_pad` to assemble the 3D `InterstellarShipEntity`)

### 3.3 Major Exoplanet Landmark Ruins (Generated on First Arrival Near Exploration Offset)
- **Hephaestus-IV:** *Ancient Fusion Forge Tower* (multi-story `pyroclast_bricks` / `pyroclast_pillar` / `magma_conduit_lamp` industrial refinery over lava, housing `ancient_fusion_relay` + expedition supply chest with `expedition_log_1` & spare fuel).
- **Pelagia-9:** *Sunken Abyssal Research Dome* (underwater `pressure_hull_plating` + `reinforced_viewport_glass` illuminated dome on the ocean floor housing `abyssal_bio_vent` + chest with `expedition_log_2` & spare fuel).
- **Nyx-0:** *Frozen Quantum Observatory Ruin* (`cryo_bricks` / `cryo_pillar` arched crystal sanctuary lit by `void_crystal_lamp` & `frozen_quantum_spire`, housing `quantum_crystal_cluster` + chest with `expedition_log_3` & spare fuel).
- **Sanctuary:** *New Home Colony Monument* (`sanctuary_bricks` / `ark_titanium_plating` / `aether_lantern` sunlit monument celebrating victory, zero artificial landing pad cluttering the touchdown meadow).

---

## 4. Performance, Multiplayer & Safety Guarantees

- **120 FPS Target:** `orbital_transit` is a pure void dimension with only `~400` cabin blocks + single-pass procedural 3D planet/wormhole/black hole sky rendering. `InterstellarShipRenderer` uses batched procedural geometry with zero per-frame allocations.
- **Multiplayer Isolation:** Each transiting ship is assigned a unique `orbital_transit` slot (`x = slotId * 256`), and `WormholeLinkRegistry` uses thread-safe Overworld `SavedData` keyed by `(dimension, BlockPos)` + UUID.
- **Zero Unfair Soft-Locks:** If stranded with low fuel, every planet provides craftable native `Starship Fuel Cell` recipes + ruin emergency caches; if stranded because a friend broke the origin Wormhole, restoring the origin Wormhole immediately unlocks launch, OR building a new Wormhole on-site remains possible.
- **Backwards Compatibility:** Existing 7×5×7 Personal Wormhole continues working for solo on-foot scouting & fast testing; `/galacticpurpose ship spawn` & `/galacticpurpose transit <planet>` testing commands allow instant verification of every phase.
