# Velorise Simple Map

<p align="center">
  <a href="https://modrinth.com/mod/velorise-simple-map"><img src="https://img.shields.io/badge/Modrinth-Available_Now-00af5c?style=for-the-badge&logo=modrinth&logoColor=white" alt="Modrinth" /></a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/velorise-simple-map"><img src="https://img.shields.io/badge/CurseForge-Available_Now-f16436?style=for-the-badge&logo=curseforge&logoColor=white" alt="CurseForge" /></a>
  <a href="https://github.com/velorise2206/Simple-Map"><img src="https://img.shields.io/badge/GitHub-Repository-181717?style=for-the-badge&logo=github&logoColor=white" alt="GitHub" /></a>
  <a href="https://github.com/sponsors/velorise2206"><img src="https://img.shields.io/badge/Sponsor-GitHub_Sponsors-ea4aaa?style=for-the-badge&logo=githubsponsors&logoColor=white" alt="GitHub Sponsors" /></a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Release-v2.1.0-brightgreen.svg" alt="Release" />
  <img src="https://img.shields.io/badge/Minecraft-26.3%20%7C%2026.2%20%7C%2026.1%20%7C%201.21.11%20%7C%201.21.1%20%7C%201.20.1-blue.svg" alt="Minecraft Versions" />
  <img src="https://img.shields.io/badge/Loaders-NeoForge%20%7C%20Fabric%20%7C%20Forge-orange.svg" alt="Mod Loaders" />
  <a href="https://github.com/velorise2206/Simple-Map/issues"><img src="https://img.shields.io/github/issues/velorise2206/Simple-Map" alt="GitHub Open Issues" /></a>
  <a href="https://github.com/velorise2206/Simple-Map/issues?q=is%3Aissue+is%3Aclosed"><img src="https://img.shields.io/github/issues-closed/velorise2206/Simple-Map?color=brightgreen" alt="GitHub Closed Issues" /></a>
</p>

A modern, lightweight and vanilla-friendly Minecraft map mod featuring an all-in-one Minimap and World Map, 3D Cave mapping, accurate block and biome tinting, optimized GPU framebuffer rendering, and physical Map Book sharing.

---

## 📦 Multi-Loader Project Architecture

This repository contains all official editions of Velorise Simple Map:

| Subfolder | Minecraft Version | Mod Loader | Java | Status | Release Artifact |
| :--- | :---: | :---: | :---: | :---: | :--- |
| [`26.3/neoforge/`](26.3/neoforge/) | **26.3** | NeoForge 26.3.0+ | Java 25 | Stable Release | `simplemap-26.3-neoforge-2.1.0.jar` |
| [`26.3/fabric/`](26.3/fabric/) | **26.3** | Fabric Loader 0.19.4+ | Java 25 | Stable Release | `simplemap-26.3-fabric-2.1.0.jar` |
| [`26.2/neoforge/`](26.2/neoforge/) | **26.2** | NeoForge 26.2.0+ | Java 25 | Stable Release | `simplemap-26.2-neoforge-2.1.0.jar` |
| [`26.2/fabric/`](26.2/fabric/) | **26.2** | Fabric Loader 0.19.2+ | Java 25 | Stable Release | `simplemap-26.2-fabric-2.1.0.jar` |
| [`26.1/neoforge/`](26.1/neoforge/) | **26.1.2** | NeoForge 26.1.0+ | Java 25 | Stable Release | `simplemap-26.1.2-neoforge-2.1.0.jar` |
| [`26.1/fabric/`](26.1/fabric/) | **26.1.2** | Fabric Loader 0.19.2+ | Java 25 | Stable Release | `simplemap-26.1.2-fabric-2.1.0.jar` |
| [`1.21.11/neoforge/`](1.21.11/neoforge/) | **1.21.11** | NeoForge 21.11.0+ | Java 21 | Stable Release | `simplemap-1.21.11-neoforge-2.1.0.jar` |
| [`1.21.11/fabric/`](1.21.11/fabric/) | **1.21.11** | Fabric Loader 0.17.3+ | Java 21 | Stable Release | `simplemap-1.21.11-fabric-2.1.0.jar` |
| [`1.21.1/neoforge/`](1.21.1/neoforge/) | **1.21.1** | NeoForge 21.1.65+ | Java 21 | Stable Release | `simplemap-1.21.1-neoforge-2.1.0.jar` |
| [`1.21.1/fabric/`](1.21.1/fabric/) | **1.21.1** | Fabric Loader 0.16.9+ | Java 21 | Stable Release | `simplemap-1.21.1-fabric-2.1.0.jar` |
| [`1.20.1/forge/`](1.20.1/forge/) | **1.20.1** | Forge 47.3.0+ | Java 17 | Stable Release | `simplemap-1.20.1-forge-2.1.0.jar` |
| [`1.20.1/fabric/`](1.20.1/fabric/) | **1.20.1** | Fabric Loader 0.16.9+ | Java 17 | Stable Release | `simplemap-1.20.1-fabric-2.1.0.jar` |

---

## ⚡ Key Architectural Features (v2.1.0)

1. **Retained Framebuffer Geometry & Smooth Motion**:
   - Zero-allocation NDC circular winding geometry (and Blaze3D offscreen circular FBO on modern drops) eliminates stencil flushes and buffer spikes.
   - Real `partialTick` minimap interpolation plus cursor-anchored log-space smooth zoom and frame-synced map panning.

2. **Universal Block Color, Resource Reload & Foliage Engine**:
   - Dynamic `BakedModel` quad inspection, `BlockColors` provider probing, and automatic `F3+T` resource-pack color cache refresh across all loaders.
   - Accurate vanilla/modded block, grass, leaves, and custom biome foliage colors, plus responsive in-game `Custom Block Colors` overrides.

3. **Multi-Band Cave Engine (Legit AUTO & Full Manual)**:
   - Dynamic cave detection with fair-play **AUTO** mode (activates only when underground, preventing above-ground X-ray).
   - Full manual slicing mode with instant (0 ms) layer cycling, 8-neighbor FULL cave seam stitching, and zero CPU alloc per step.
   - Seamless Anvil `.mca` chunk reconstruction (including MC 26.x `dimensions/` and `block_states.palette` formats) for offline and remote exploration.

4. **Map Book Sharing & Waypoints System**:
   - Craftable physical Map Books (`book_map`) allowing players to record and trade explored territories.
   - Responsive HUD positioning with magnetic snapping, waypoint presets, death markers, and full vanilla/modded server compatibility.

---

## 🛠️ Build & Dev Instructions

### 1. Build All 12 Loaders in One Step
Run the master batch/PowerShell automation script from the root folder:
```cmd
build-all.bat
```
* Built `.jar` files will automatically be assembled into the centralized release directory (`../mods/simplemap/`).
* Requires JDK 25 (`JAVA25_HOME`), JDK 21 (`JAVA21_HOME`), and JDK 17 (`JAVA17_HOME`).

### 2. Build or Run a Specific Loader
Navigate into any `<mc_version>\<loader>` subproject folder and run Gradle:

```cmd
# Fabric 26.3 (Java 25)
cd 26.3\fabric
gradlew build
# or run client: ..\..\scripts\run-fabric.bat 26.3
# or run debug client: ..\..\scripts\run-fabric-debug.bat 26.3

# NeoForge 26.3 (Java 25)
cd 26.3\neoforge
gradlew build
# or run client: ..\..\scripts\run-neoforge.bat 26.3
# or run debug client: ..\..\scripts\run-neoforge-debug.bat 26.3

# NeoForge 1.21.1 (Java 21)
cd 1.21.1\neoforge
gradlew build
# or run client: ..\..\scripts\run-neoforge.bat 1.21.1
# or run debug client: ..\..\scripts\run-neoforge-debug.bat 1.21.1

# Forge 1.20.1 (Java 17)
cd 1.20.1\forge
gradlew build
# or run client: ..\..\scripts\run-forge-1.20.1.bat
# or run debug client: ..\..\scripts\run-forge-1.20.1-debug.bat

# Unified Master Client Launcher (Root — options 1–24 for all 12 ports in Normal/Debug):
run.bat
```

### 3. Debug & Telemetry Controls
When launched via any of the debug scripts:
* **`F8`**: Toggle in-game telemetry HUD overlay (FPS, Frame ms, Heap/GC, exact pipeline queues, FBO status).
* **`F9`**: Toggle 1 Hz metrics recording to CSV.
* **`F10`**: **Mark Debug Issue**: Triggers 20-second burst capture (250 ms rate) and records an explicit `USER_MARKER` event into `events.jsonl` with an immediate file flush.
* Telemetry files are written into `<loader>/run/simplemap-debug/<session>/` (`metrics.csv` & `events.jsonl`).

### 4. Changelogs & Issue Tracker
* **Changelog**: See [`CHANGELOG.md`](CHANGELOG.md) for full release notes and detailed version history.
* **Bug Reports & Feedback**: Encountered an issue or have a feature request? Please submit a ticket on our [GitHub Issue Tracker](https://github.com/velorise2206/Simple-Map/issues).

---

## 📄 License
Licensed under the [MIT License](LICENSE).
