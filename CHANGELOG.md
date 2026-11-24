# Changelog

All notable changes to **Simple Map** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.6] - 2026-11-24

### Added
- **Official Ports for Minecraft 1.21.11 & 26.1 (Fabric & NeoForge)**: Added official multi-loader editions for Minecraft `1.21.11` and `26.1` with full feature parity (surface and layered cave mapping, circular minimap, waypoints and pins, Auto Night Mode, and Map Book sharing).

### Fixed
- **Vanilla Color Mode (Snow & Pure-White Blocks)**: Fixed snow and other pure-white (`0xFFFFFFFF`) blocks rendering pink in Vanilla color mode across all supported Minecraft versions (`1.20.1`, `1.21.1`, `1.21.11`, and `26.1`).
- **[26.1] Offline World-Save Map Reconstruction**: Resolved Anvil `.mca` region folders via `DimensionType.getStorageFolder(...)` in `MapManager` and `CaveTextureManager`, restoring distant surface backfill and offline cave slicing under Minecraft 26.1's `dimensions/<namespace>/<path>/region/` layout.
- **Cave Border Seam Stitching**: Enforced 8-neighbor FULL cave border validation in `CaveTextureManager` across all versions to eliminate tile gaps between adjacent subterranean regions.

### Changed
- **Smoother Minimap Scrolling (All Loaders)**: Updated minimap HUD rendering to use real frame `partialTick` interpolation instead of stepping at fixed 20 TPS ticks on `Fabric` (all versions), `Forge 1.20.1`, and `NeoForge 1.21.1`, delivering silky-smooth high-refresh-rate movement across every loader.
- **Smooth Fullscreen Map Zoom & Pan (All Loaders)**: Added cursor-anchored log-space smooth zoom (mouse wheel and zoom keys) and frame-synchronized, origin-anchored mouse dragging in `MapScreen` across `1.20.1`, `1.21.1`, `1.21.11`, and `26.1`.
- **Fullscreen Map Responsiveness (1.20.1 & 1.21.1)**: Disabled retained fullscreen FBO in `FullscreenMapFramebufferRenderer` in favor of direct `MapRenderPlan` replay, eliminating stale blurry frames and layer-switching latency while maintaining parity with 1.21.11 and 26.1.
- **HUD Magnetic Snapping & Settings Polish**: Added bidirectional 14px magnetic snapping between coordinates HUD and minimap dock in `MapConfigScreen`, removed obsolete STREAM mapping mode, and widened GPU memory budget throttles.

---

## [2.0.5] - 2026-11-23

### Fixed
- **Snow & Surface Covers ([#8](https://github.com/velorise2206/Simple-Map/issues/8))**: Snow layers (1–8 layers), wool/moss carpets, and rails now render properly on the map without artificial 1-block cliff shadows, and snow layers no longer trigger false underground roof detection.
- **Auto Night Mode**: Fixed inverted sky-brightness calculation in `MapRenderer` so daytime renders at full brightness while dusk/dawn transitions and night-time light glow work naturally.
- **The End & Nether Roof (AUTO Cave Mode)**: Fixed The End being treated as a cave dimension in AUTO mode, and allowed AUTO mode in The Nether to switch to Surface view when standing on top of the Nether roof (`Y >= 127`).
- **Mod Icon**: Added `logo.png` in shared resources and aligned `fabric.mod.json`, `mods.toml`, and `neoforge.mods.toml`, resolving the Mod Menu missing icon error on Fabric.
- **Remote Dimension Pin**: Viewing another dimension now displays its saved red X pin marker while keeping guideline dots and distance labels limited to the active dimension.

### Changed
- **Visual Polish**: Improved shallow 1-block water tint blending (e.g., Swamp dirt/clay), made clear glass/glass panes transparent to show blocks beneath, and added biome foliage tinting for lily pads.
- **Multi-Loader Architecture**: Consolidated `1.20.1` and `1.21.1` into shared `common/` modules with `PlatformBridge`, extracted `MapConfig`, split `MapScreen` into modular screens, and unified build/debug scripts in English.

### Optimized
- **VRAM & Render-Thread Stability**: Added centralized render-thread `GlTextureDeleter`, capped minimap FBO resolution to `<= 768x768` (4x less VRAM/fill-rate at 4x GUI scale), throttled far-zoom branch uploads to quarter-page milestones, skipped empty cave/daytime glow passes in `buildRenderPlan`, and reduced lock contention and per-page allocations.

---

## [2.0.4.1] - 2026-11-12

### Fixed
- **[Forge 1.20.1]** Fixed a critical Mixin crash (`InvalidInjectionException ... No refMap loaded`) triggered when opening the Server List, joining worlds, or launching alongside mods such as Simple Voice Chat ([#7](https://github.com/hungbbdzz/Simple-Map/issues/7)). The previous Forge 1.20.1 package omitted `simplemap.refmap.json` because Gradle restored `compileJava` from build cache after `clean`.
- **[Build System]** Disabled Gradle build caching on Forge `compileJava` so MixinGradle always generates `simplemap.refmap.json`, and added automated post-build refmap verification across `forge-1.20.1/build.gradle` and `build-all.bat`.

---

## [2.0.4] - 2026-11-08

### Fixed
- **[Forge & NeoForge]** Fixed a critical crash (`Registry Object not present`) occurring when crafting any item in certain multiplayer or connector environments ([#6](https://github.com/hungbbdzz/Simple-Map/issues/6)).
- **[Forge & NeoForge]** Added defensive registry checks across recipes (`CopyMapBookRecipe`, `MergeMapBooksRecipe`) and creative inventory tabs (`BuildCreativeModeTabContentsEvent`).
- **[Forge & NeoForge]** Added safe guard checks in `hasMapBookInInventory` to prevent registry lookups on unbound items.

### Changed
- **[All Loaders]** Separated Cave Map **AUTO** mode from **ON** mode:
  - In **AUTO** mode (Fair-Play Explorer), manual layer selection is restricted; the layer slider is locked and hidden to prevent above-ground X-ray inspection of subterranean bases or ores. The cave map dynamically activates only when the player physically ventures underground.
  - In **ON** mode, full manual layer slicing and slider controls remain freely accessible from anywhere.
  - Live configuration changes and server sync events now immediately trigger cache invalidation and UI state refreshes.

### Optimized
- **[Fabric]** Optimized crafting event performance by skipping client-side processing early, preventing unnecessary NBT tag copying on the render thread.

---

## [2.0.3] - 2026-11-02

### Added
- 1:1 9-slice vanilla frame style for minimap and full map UI.
- Smart screen & F3 minimap auto-visibility rules.
- 8-neighbor MCA chunk scanning validation to scrub bald boundary chunks and cleanly drain pending saves.

### Changed
- Refactored settings GUI layout to be fully responsive across all GUI scale factors without overlapping bottom buttons.
- Optimized surface LOD hierarchy traversal to maintain consistent 120+ FPS.

---

## [2.0.2] - 2026-10-28

### Fixed
- Restored true 64-segment circular minimap mesh rendering across all 4 loaders.
- Fixed viewport freeze and minimap render desync when panning at far zoom levels.

---

## [2.0.1] - 2026-10-15

### Added
- Complete multi-loader release suite: **Fabric 1.20.1**, **Fabric 1.21.1**, **Forge 1.20.1**, and **NeoForge 1.21.1**.
- Automatic cave layer mapping and outdoor transition scanner.
- Map Book exploration sync pipeline and merging mechanics.
- In-game coordinate link sharing in chat with one-click waypoint creation.
