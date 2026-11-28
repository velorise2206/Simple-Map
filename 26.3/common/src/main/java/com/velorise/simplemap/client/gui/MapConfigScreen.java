package com.velorise.simplemap.client.gui;
import com.velorise.simplemap.client.cave.CaveTextureManager.CaveMapManager;
import com.velorise.simplemap.client.color.SurfaceColorizer.MapVisualClassifier;
import com.velorise.simplemap.client.MapConfig;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.Identifier;
import java.util.Set;
import com.velorise.simplemap.client.waypoint.PinNavigation.WaypointManager;
import com.velorise.simplemap.client.waypoint.PinNavigation.WaypointManager.Waypoint;
import com.velorise.simplemap.client.cave.*;
import com.velorise.simplemap.client.renderer.*;
import com.velorise.simplemap.client.color.*;
import com.velorise.simplemap.client.color.SurfaceColorizer.MapColorProfile;
import com.velorise.simplemap.client.*;
import com.velorise.simplemap.client.MinimapRenderer.MinimapPosition;
import com.velorise.simplemap.client.session.MapSessionManager;
import com.velorise.simplemap.ServerConfig;
import com.velorise.simplemap.network.NetworkHandler;
import com.velorise.simplemap.network.payload.SyncConfigPayload;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import java.util.Comparator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.LinkedHashSet;
import com.velorise.simplemap.client.MapWorkScheduler.MapProcessor;
import com.velorise.simplemap.client.waypoint.PinNavigation.WaypointManager;
import com.velorise.simplemap.client.waypoint.PinNavigation.WaypointManager.Waypoint;
import com.velorise.simplemap.client.MapDebugRecorder.ExactPageState;
import com.velorise.simplemap.client.MapViewportCoordinator.MapSurfaceDemandPolicy;
import com.velorise.simplemap.client.cave.*;
import com.velorise.simplemap.client.renderer.*;
import com.velorise.simplemap.client.color.*;
import com.velorise.simplemap.SimpleMap;
import com.velorise.simplemap.client.*;
import com.velorise.simplemap.client.MapManager.DimensionMapProfile;

import com.velorise.simplemap.client.MapManager.MapTeleportController;
import com.velorise.simplemap.client.waypoint.*;
import com.velorise.simplemap.client.cave.CaveTextureManager.CaveMapManager.CaveStateClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapConfigScreen extends Screen {
private final Screen parent;
private static com.mojang.blaze3d.pipeline.RenderTarget minimapTarget = null;
private boolean isDraggingMinimap = false;
private double dragOffsetX = 0;
private double dragOffsetY = 0;

// Coords dragging & selection state
private boolean selectedMinimap = false;
private boolean selectedCoords = false;
private boolean isDraggingCoords = false;
private double dragCoordsOffsetX = 0;
private double dragCoordsOffsetY = 0;

// UI Tab & Side Panel State
private int activeTab = 0; // 0 = Map, 1 = Coords, 2 = Colors, 3 = System
private Button tabMapButton;
private Button tabCoordsButton;
private Button tabColorsButton;
private Button tabSystemButton;
private Button doneButton;
private Button resetToDefaultButton;

// Minimap controls reference
private Button minimapToggleButton;
private Button minimapRotateButton;
private Button minimapShapeButton;
private Button minimapFrameButton;
private SimpleSlider minimapSizeSlider;
private SimpleSlider minimapZoomSlider;

// Player marker controls reference
private Button pointerModeButton;
private SimpleSlider pointerScaleSlider;
private SimpleSlider pinScaleSlider;

// Coords controls reference
private Button coordsToggleButton;
private SimpleSlider coordsScaleSlider;
private Button compassLettersToggleButton;
private Button showInScreensButton;
private Button minimapWaypointsButton;
private Button mapStyleButton;
private Button displayFlowersButton;
private Button cursorBiomeButton;
private Button cursorBlockButton;

// Shared color editor: one input and a small reusable saved-color palette.
private static final String[] COLOR_TARGET_NAMES = { "Arrow", "Ring", "Compass", "Coords" };
private int selectedColorTarget = 0;
private Button colorTargetButton;
private Button presetColorButton;
private Button savePaletteColorButton;
private Button customBlockColorsButton;
private EditBox sharedColorInput;
private final List<ColorSwatchButton> paletteButtons = new ArrayList<>();

// Keep references so slider can sync
private SimpleSlider scanSlider;

// Require Book toggle button
private Button requireBookButton;
private Button autoClearPinButton;
private Button deathWaypointButton;
private Button deathWaypointLimitButton;
private Button alwaysRescanButton;
private Button reloadAllRegionsButton;
private Button blockColourModeButton;
private Button terrainSlopesButton;
private Button fastFullscreenButton;
private Button caveScanButton;

// Shared responsive panel geometry. Init and render use the same numbers so
// controls cannot drift outside the card at different GUI scales.
private int panelLeft = 10;
private int panelTop = 15;
private int panelWidth = 270;
private int panelRight = 280;
private int panelBottom = 0;

private static final int SCAN_CHUNK_MIN = 4;
private static final int SCAN_CHUNK_MAX = 390; // 390 represents AUTO/MAX
private int paletteLabelY = 104;

public MapConfigScreen(Screen parent) {
    super(Component.translatable("gui.simplemap.settings_title"));
    this.parent = parent;
}

@Override
protected void init() {
    if (!(parent instanceof MapScreen)) {
        // Global/server config screen opened from the Mods list.
        int btnWidth = 176;
        int btnHeight = 20;
        boolean remoteServer = isRemoteServerSession();
        boolean remoteExtension = remoteServer && MapConfig.serverExtensionAvailable;

        requireBookButton = Button.builder(
                getServerRequireBookMessage(remoteServer, remoteExtension),
                button -> {
                    ServerConfig.requireMapBook = !ServerConfig.requireMapBook;
                    if (this.minecraft != null && this.minecraft.getSingleplayerServer() != null) {
                        MapConfig.applyServerSyncConfig(ServerConfig.requireMapBook, ServerConfig.caveMapMode);
                    } else {
                        MapConfig.serverRequireMapBook = ServerConfig.requireMapBook;
                    }
                    ServerConfig.save();
                    syncIntegratedServerConfig();
                    button.setMessage(getServerRequireBookMessage(false, false));
                }).bounds((this.width - btnWidth) / 2, this.height / 2 - 30, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.translatable(
                        remoteServer && !remoteExtension
                                ? "gui.simplemap.tooltip.server_book_ext_required"
                                : "gui.simplemap.tooltip.server_book")))
                .build();
        requireBookButton.active = !remoteServer;
        this.addRenderableWidget(requireBookButton);

        caveScanButton = Button.builder(
                getServerCaveModeMessage(remoteServer, remoteExtension),
                button -> {
                    if (remoteServer && !remoteExtension) {
                        MapConfig.localCaveMapMode = (MapConfig.localCaveMapMode + 1) % 3;
                        if (MapConfig.localCaveMapMode == 0) {
                            CaveMapManager.getInstance().deactivate();
                        }
                        CaveMode.notifyModeChanged();
                        MapConfig.save();
                        button.setMessage(getServerCaveModeMessage(true, false));
                        return;
                    }
                    ServerConfig.caveMapMode = (ServerConfig.caveMapMode + 1) % 3;
                    if (this.minecraft != null && this.minecraft.getSingleplayerServer() != null) {
                        MapConfig.applyServerSyncConfig(ServerConfig.requireMapBook, ServerConfig.caveMapMode);
                    } else {
                        MapConfig.serverCaveMapMode = ServerConfig.caveMapMode;
                    }
                    if (ServerConfig.caveMapMode == 0) {
                        CaveMapManager.getInstance().deactivate();
                    }
                    CaveMode.notifyModeChanged();
                    ServerConfig.save();
                    syncIntegratedServerConfig();
                    button.setMessage(getServerCaveModeMessage(false, false));
                }).bounds((this.width - btnWidth) / 2, this.height / 2, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.server_cave_scan")))
                .build();
        caveScanButton.active = !remoteExtension;
        this.addRenderableWidget(caveScanButton);

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.simplemap.done"),
                button -> {
                    ServerConfig.save();
                    if (this.minecraft != null) {
                        this.minecraft.gui.setScreen(this.parent);
                    }
                }).bounds((this.width - 88) / 2, this.height / 2 + 38, 88, btnHeight).build());
        return;
    }

    // ==================== RESPONSIVE LEFT PANEL ====================
    panelLeft = Math.max(4, Math.min(10, this.width / 24));
    panelTop = Math.max(6, Math.min(15, (this.height - 224) / 2));
    panelWidth = Math.min(270, Math.max(210, this.width - 120));
    panelRight = panelLeft + panelWidth;
    panelBottom = panelTop + 224;

    // ==================== TABS (Top of Left Panel) ====================
    int tabY = panelTop + 1;
    int tabSpacing = 4;
    int startX = panelLeft + 4;
    int tabW = Math.max(48, (panelWidth - 8 - tabSpacing * 3) / 4);

    tabMapButton = Button.builder(
            getTabButtonLabel(0, "gui.simplemap.tab.map.name"),
            button -> {
                activeTab = 0;
                updateTabButtons();
                updateWidgetVisibility();
            }).bounds(startX, tabY, tabW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tab.minimap")))
            .build();
    this.addRenderableWidget(tabMapButton);

    tabCoordsButton = Button.builder(
            getTabButtonLabel(1, "gui.simplemap.tab.coords.name"),
            button -> {
                activeTab = 1;
                updateTabButtons();
                updateWidgetVisibility();
            }).bounds(startX + tabW + tabSpacing, tabY, tabW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tab.coords")))
            .build();
    this.addRenderableWidget(tabCoordsButton);

    tabColorsButton = Button.builder(
            getTabButtonLabel(2, "gui.simplemap.tab.colors.name"),
            button -> {
                activeTab = 2;
                updateTabButtons();
                updateWidgetVisibility();
            }).bounds(startX + (tabW + tabSpacing) * 2, tabY, tabW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tab.colors")))
            .build();
    this.addRenderableWidget(tabColorsButton);

    tabSystemButton = Button.builder(
            getTabButtonLabel(3, "gui.simplemap.tab.system.name"),
            button -> {
                activeTab = 3;
                updateTabButtons();
                updateWidgetVisibility();
            }).bounds(startX + (tabW + tabSpacing) * 3, tabY, tabW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tab.system")))
            .build();
    this.addRenderableWidget(tabSystemButton);

    // ==================== CONTROLS GRID ====================
    int col1X = panelLeft + 6;
    int columnGap = 8;
    int colW = Math.max(90, (panelWidth - 12 - columnGap) / 2);
    int col2X = col1X + colW + columnGap;
    int gridWidth = colW * 2 + columnGap;
    int r1Y = tabY + 28;
    int r2Y = r1Y + 24;
    int r3Y = r2Y + 24;
    int r4Y = r3Y + 24;
    int r5Y = r4Y + 24;
    int r6Y = r5Y + 24;
    int r7Y = r6Y + 24;
    int doneY = r7Y + 24;
    paletteLabelY = r3Y + 12;

    // Row 1
    minimapToggleButton = Button.builder(
            getToggleMessage(),
            button -> {
                MapConfig.minimapEnabled = !MapConfig.minimapEnabled;
                button.setMessage(getToggleMessage());
            }).bounds(col1X, r1Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.minimap_toggle")))
            .build();
    this.addRenderableWidget(minimapToggleButton);

    double initialSizeVal = (double) (MapConfig.minimapSize - 16) / (150 - 16);
    minimapSizeSlider = new SimpleSlider(
            col2X, r1Y, colW, 20,
            initialSizeVal,
            val -> Component.translatable("gui.simplemap.option.size", 16 + (int) (val * (150 - 16))).getString(),
            val -> MapConfig.minimapSize = 16 + (int) (val * (150 - 16)));
    minimapSizeSlider.setTooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.minimap_size")));
    this.addRenderableWidget(minimapSizeSlider);

    // Row 2
    minimapShapeButton = Button.builder(
            getShapeMessage(),
            button -> {
                MapConfig.minimapCircle = !MapConfig.minimapCircle;
                button.setMessage(getShapeMessage());
            }).bounds(col1X, r2Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.minimap_shape")))
            .build();
    this.addRenderableWidget(minimapShapeButton);

    double initialZoomVal = (MapConfig.minimapZoom - MapConfig.MINIMUM_ZOOM_SCALE)
            / (2.0 - MapConfig.MINIMUM_ZOOM_SCALE);
    minimapZoomSlider = new SimpleSlider(
            col2X, r2Y, colW, 20,
            initialZoomVal,
            val -> String.format(java.util.Locale.ROOT, "%.2fx",
                    MapConfig.MINIMUM_ZOOM_SCALE + val * (2.0 - MapConfig.MINIMUM_ZOOM_SCALE)),
            val -> MapConfig.minimapZoom = (float) (MapConfig.MINIMUM_ZOOM_SCALE
                    + val * (2.0 - MapConfig.MINIMUM_ZOOM_SCALE)));
    minimapZoomSlider.setTooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.minimap_zoom")));
    this.addRenderableWidget(minimapZoomSlider);

    // Row 3
    minimapRotateButton = Button.builder(
            getRotateMessage(),
            button -> {
                MapConfig.minimapRotate = !MapConfig.minimapRotate;
                button.setMessage(getRotateMessage());
            }).bounds(col1X, r3Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.north_lock")))
            .build();
    this.addRenderableWidget(minimapRotateButton);

    double initialPointerScaleVal = (MapConfig.playerMarkerScale - 0.1) / (1.0 - 0.1);
    pointerScaleSlider = new SimpleSlider(
            col2X, r3Y, colW, 20,
            initialPointerScaleVal,
            val -> Component.translatable("gui.simplemap.option.pointer_scale",
                    String.format(java.util.Locale.ROOT, "%.2f", 0.1 + val * (1.0 - 0.1))).getString(),
            val -> MapConfig.playerMarkerScale = (float) (0.1 + val * (1.0 - 0.1)));
    pointerScaleSlider.setTooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.pointer_scale")));
    this.addRenderableWidget(pointerScaleSlider);

    // Row 4
    pointerModeButton = Button.builder(
            getPointerModeMessage(),
            button -> {
                MapConfig.playerMarkerMode = (MapConfig.playerMarkerMode + 1) % 2;
                button.setMessage(getPointerModeMessage());
            }).bounds(col1X, r4Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.pointer_mode")))
            .build();
    this.addRenderableWidget(pointerModeButton);

    double initialPinScaleVal = (MapConfig.pinScale - 0.1) / (1.0 - 0.1);
    pinScaleSlider = new SimpleSlider(
            col2X, r4Y, colW, 20,
            initialPinScaleVal,
            val -> Component.translatable("gui.simplemap.option.pin_scale",
                    String.format(java.util.Locale.ROOT, "%.2f", 0.1 + val * (1.0 - 0.1))).getString(),
            val -> {
                MapConfig.pinScale = (float) (0.1 + val * (1.0 - 0.1));
                MapConfig.save();
            });
    pinScaleSlider.setTooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.pin_scale")));
    this.addRenderableWidget(pinScaleSlider);

    // ==================== SHARED COLOR EDITOR ====================
    colorTargetButton = Button.builder(
            getColorTargetMessage(),
            button -> {
                selectedColorTarget = (selectedColorTarget + 1) % COLOR_TARGET_NAMES.length;
                button.setMessage(getColorTargetMessage());
                syncSharedColorInput();
            }).bounds(col1X, r1Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.color_target")))
            .build();
    this.addRenderableWidget(colorTargetButton);

    presetColorButton = Button.builder(
            getSelectedPresetMessage(),
            button -> {
                setSelectedTargetColor(nextPresetColor(getSelectedTargetColor()));
                syncSharedColorInput();
            }).bounds(col2X, r1Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.color_preset")))
            .build();
    this.addRenderableWidget(presetColorButton);

    int saveButtonWidth = Math.max(44, Math.min(56, colW / 2));
    int colorInputWidth = Math.max(72, gridWidth - saveButtonWidth - columnGap);
    int saveButtonX = col1X + colorInputWidth + columnGap;
    sharedColorInput = new EditBox(this.font, col1X, r2Y, colorInputWidth, 20,
            Component.translatable("gui.simplemap.narration.shared_hex_color"));
    sharedColorInput.setMaxLength(9);
    sharedColorInput.setValue(MapColorProfile.formatColor(getSelectedTargetColor()));
    sharedColorInput.setHint(Component.literal("#RRGGBB"));
    sharedColorInput.setResponder(this::previewSharedColor);
    sharedColorInput.setTooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.shared_color_input")));
    this.addRenderableWidget(sharedColorInput);

    savePaletteColorButton = Button.builder(
            Component.translatable("gui.simplemap.save"),
            button -> saveSharedColor())
            .bounds(saveButtonX, r2Y, saveButtonWidth, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.save_palette_color")))
            .build();
    this.addRenderableWidget(savePaletteColorButton);

    paletteButtons.clear();
    int swatchGap = 4;
    int swatchWidth = Math.max(28, (gridWidth - swatchGap * 3) / 4);
    for (int i = 0; i < 8; i++) {
        int swatchX = col1X + (i % 4) * (swatchWidth + swatchGap);
        int swatchY = r3Y + (i / 4) * 24;
        ColorSwatchButton swatch = new ColorSwatchButton(swatchX, swatchY, swatchWidth, 20, i);
        paletteButtons.add(swatch);
        this.addRenderableWidget(swatch);
    }

    mapStyleButton = Button.builder(
            getMapStyleMessage(),
            button -> {
                MapConfig.mapColorProfile = (MapConfig.mapColorProfile + 1) % MapColorProfile.NAMES.length;
                MapConfig.save();
                button.setMessage(getMapStyleMessage());
                MapManager.getInstance().reloadAllRegions();
            }).bounds(col1X, r5Y, gridWidth, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.map_style")))
            .build();
    this.addRenderableWidget(mapStyleButton);

    customBlockColorsButton = Button.builder(
            Component.translatable("gui.simplemap.option.block_overrides", MapConfig.blockColorOverrides.size()),
            b -> {
                if (this.minecraft != null) this.minecraft.gui.setScreen(new BlockColorManagerScreen(this));
            }).bounds(col1X, r6Y, gridWidth, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.block_overrides")))
            .build();
    this.addRenderableWidget(customBlockColorsButton);

    minimapFrameButton = Button.builder(
            getFrameMessage(),
            button -> {
                MapConfig.minimapFrameStyle = (MapConfig.minimapFrameStyle + 1) % 3;
                MapConfig.save();
                button.setMessage(getFrameMessage());
            }).bounds(col1X, r5Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.minimap_frame")))
            .build();
    this.addRenderableWidget(minimapFrameButton);

    compassLettersToggleButton = Button.builder(
            getCompassLettersMessage(),
            button -> {
                MapConfig.compassLettersVisible = !MapConfig.compassLettersVisible;
                button.setMessage(getCompassLettersMessage());
            }).bounds(col2X, r5Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.compass_labels")))
            .build();
    this.addRenderableWidget(compassLettersToggleButton);

    showInScreensButton = Button.builder(
            getShowInScreensMessage(),
            button -> {
                MapConfig.showMinimapInScreens = !MapConfig.showMinimapInScreens;
                button.setMessage(getShowInScreensMessage());
            }).bounds(col1X, r6Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.show_in_screens")))
            .build();
    this.addRenderableWidget(showInScreensButton);

    minimapWaypointsButton = Button.builder(
            getMinimapWaypointsMessage(),
            button -> {
                MapConfig.minimapWaypointsVisible = !MapConfig.minimapWaypointsVisible;
                MapConfig.save();
                button.setMessage(getMinimapWaypointsMessage());
            }).bounds(col2X, r6Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.minimap_waypoints")))
            .build();
    this.addRenderableWidget(minimapWaypointsButton);

    // ==================== COORDS CONTROLS ====================
    coordsToggleButton = Button.builder(
            getCoordsToggleMessage(),
            button -> {
                MapConfig.coordsEnabled = !MapConfig.coordsEnabled;
                button.setMessage(getCoordsToggleMessage());
            }).bounds(col1X, r1Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.coordinates")))
            .build();
    this.addRenderableWidget(coordsToggleButton);

    double initialCoordsScaleVal = (MapConfig.coordsScale - 0.1) / (2.0 - 0.1);
    coordsScaleSlider = new SimpleSlider(
            col2X, r1Y, colW, 20,
            initialCoordsScaleVal,
            val -> Component.translatable("gui.simplemap.option.coords_scale",
                    String.format(java.util.Locale.ROOT, "%.2f", 0.1 + val * (2.0 - 0.1))).getString(),
            val -> MapConfig.coordsScale = (float) (0.1 + val * (2.0 - 0.1)));
    coordsScaleSlider.setTooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.coords_scale")));
    this.addRenderableWidget(coordsScaleSlider);

    cursorBiomeButton = Button.builder(
            getCursorBiomeMessage(),
            button -> {
                MapConfig.cursorBiomeEnabled = !MapConfig.cursorBiomeEnabled;
                MapConfig.save();
                button.setMessage(getCursorBiomeMessage());
            }).bounds(col1X, r2Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.cursor_biome")))
            .build();
    this.addRenderableWidget(cursorBiomeButton);

    cursorBlockButton = Button.builder(
            getCursorBlockMessage(),
            button -> {
                MapConfig.cursorBlockEnabled = !MapConfig.cursorBlockEnabled;
                MapConfig.save();
                button.setMessage(getCursorBlockMessage());
            }).bounds(col2X, r2Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.cursor_block")))
            .build();
    this.addRenderableWidget(cursorBlockButton);

    // ==================== SYSTEM CONTROLS ====================
    int initialChunks = pointsToChunkBudget(MapConfig.scanPointsPerTick);
    double initialScanVal = (double) (initialChunks - SCAN_CHUNK_MIN)
            / (SCAN_CHUNK_MAX - SCAN_CHUNK_MIN);
    scanSlider = new SimpleSlider(
            col2X, r1Y, colW, 20,
            initialScanVal,
            val -> formatScanBudget(sliderToChunkBudget(val)),
            val -> {
                int chunks = sliderToChunkBudget(val);
                MapConfig.scanPointsPerTick = chunkBudgetToPoints(chunks);
            });
    scanSlider.setTooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.scan_slider")));
    this.addRenderableWidget(scanSlider);

    alwaysRescanButton = Button.builder(
            getAlwaysRescanMessage(),
            button -> {
                MapConfig.alwaysRescanExplored = !MapConfig.alwaysRescanExplored;
                MapConfig.save();
                button.setMessage(getAlwaysRescanMessage());
            }).bounds(col1X, r2Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.rescan_loaded")))
            .build();
    this.addRenderableWidget(alwaysRescanButton);

    reloadAllRegionsButton = Button.builder(
            Component.translatable("gui.simplemap.reload_regions"),
            button -> {
                MapManager.getInstance().reloadAllRegions();
                button.setMessage(Component.translatable("gui.simplemap.reloading"));
            }).bounds(col2X, r2Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.reload_regions")))
            .build();
    this.addRenderableWidget(reloadAllRegionsButton);

    autoClearPinButton = Button.builder(
            getAutoClearPinMessage(),
            button -> {
                MapConfig.autoClearPin = !MapConfig.autoClearPin;
                MapConfig.save();
                button.setMessage(getAutoClearPinMessage());
            }).bounds(col1X, r3Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.auto_clear_pin")))
            .build();
    this.addRenderableWidget(autoClearPinButton);

    fastFullscreenButton = Button.builder(
            getFastFullscreenMessage(),
            b -> {
                MapConfig.fastFullscreenLoading = !MapConfig.fastFullscreenLoading;
                b.setMessage(getFastFullscreenMessage());
                MapConfig.save();
            }).bounds(col2X, r3Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.fast_fullscreen")))
            .build();
    this.addRenderableWidget(fastFullscreenButton);

    displayFlowersButton = Button.builder(
            getDisplayFlowersMessage(),
            button -> {
                MapConfig.displayFlowers = !MapConfig.displayFlowers;
                button.setMessage(getDisplayFlowersMessage());
                refreshLoadedMapData();
            }).bounds(col1X, r4Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.map_flowers")))
            .build();
    this.addRenderableWidget(displayFlowersButton);

    blockColourModeButton = Button.builder(
            getBlockColourModeMessage(),
            button -> {
                MapConfig.blockColourMode = (MapConfig.blockColourMode + 1) % 2;
                button.setMessage(getBlockColourModeMessage());
                refreshLoadedMapData();
            }).bounds(col2X, r4Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.block_colors")))
            .build();
    this.addRenderableWidget(blockColourModeButton);

    terrainSlopesButton = Button.builder(
            getTerrainSlopesMessage(),
            button -> {
                MapConfig.terrainSlopes = (MapConfig.terrainSlopes + 1) % 3;
                button.setMessage(getTerrainSlopesMessage());
                refreshLoadedMapData();
            }).bounds(col1X, r1Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.terrain_shading")))
            .build();
    this.addRenderableWidget(terrainSlopesButton);

    deathWaypointButton = Button.builder(
            getDeathWaypointMessage(),
            b -> {
                MapConfig.createDeathWaypoint = !MapConfig.createDeathWaypoint;
                b.setMessage(getDeathWaypointMessage());
                if (deathWaypointLimitButton != null) deathWaypointLimitButton.active = MapConfig.createDeathWaypoint;
                MapConfig.save();
            }).bounds(col1X, r5Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.death_points")))
            .build();
    this.addRenderableWidget(deathWaypointButton);

    deathWaypointLimitButton = Button.builder(
            getDeathWaypointLimitMessage(),
            b -> {
                MapConfig.maxDeathWaypoints = switch (MapConfig.maxDeathWaypoints) {
                    case 0 -> 1;
                    case 1 -> 3;
                    case 3 -> 5;
                    case 5 -> 10;
                    default -> 0;
                };
                b.setMessage(getDeathWaypointLimitMessage());
                MapConfig.save();
            }).bounds(col2X, r5Y, colW, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.death_limit")))
            .build();
    deathWaypointLimitButton.active = MapConfig.createDeathWaypoint;
    this.addRenderableWidget(deathWaypointLimitButton);

    resetToDefaultButton = Button.builder(
            Component.translatable("gui.simplemap.restore_defaults"),
            button -> openResetConfirmation())
            .bounds(col1X, r7Y, gridWidth, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.restore_defaults")))
            .build();
    this.addRenderableWidget(resetToDefaultButton);

    // ==================== DONE BUTTON ====================
    doneButton = Button.builder(
            Component.translatable("gui.simplemap.done"),
            button -> {
                MapConfig.save();
                if (this.minecraft != null)
                    this.minecraft.gui.setScreen(this.parent);
            }).bounds(panelLeft + (panelWidth - 88) / 2, doneY, 88, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.done_settings")))
            .build();
    this.addRenderableWidget(doneButton);

    // Sync visibility at startup
    updateWidgetVisibility();
}

private void openResetConfirmation() {
    if (this.minecraft == null) return;
    this.minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
        if (confirmed) {
            applyDefaultPreferences();
        }
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this);
        }
    }, Component.translatable("gui.simplemap.confirm_restore.title"),
            Component.translatable("gui.simplemap.confirm_restore.message"),
            Component.translatable("gui.simplemap.confirm_restore.confirm"),
            Component.translatable("gui.simplemap.cancel")));
}

private void applyDefaultPreferences() {
    MapConfig.resetPreferencesToDefaults();
    selectedMinimap = false;
    selectedCoords = false;
    isDraggingMinimap = false;
    isDraggingCoords = false;
    CaveMode.clearManualLayer();
    CaveMapManager.getInstance().deactivate();
    ChunkScanner.getInstance().reset();
    MapManager.getInstance().reloadAllRegions();
    CaveTextureManager.getInstance().invalidateStyle();
}

private Component getTabButtonLabel(int tabIndex, String key) {
    String label = Component.translatable(key).getString();
    return Component.literal(activeTab == tabIndex ? "§6§l" + label : label);
}

private void updateTabButtons() {
    if (tabMapButton != null)
        tabMapButton.setMessage(getTabButtonLabel(0, "gui.simplemap.tab.map.name"));
    if (tabCoordsButton != null)
        tabCoordsButton.setMessage(getTabButtonLabel(1, "gui.simplemap.tab.coords.name"));
    if (tabColorsButton != null)
        tabColorsButton.setMessage(getTabButtonLabel(2, "gui.simplemap.tab.colors.name"));
    if (tabSystemButton != null)
        tabSystemButton.setMessage(getTabButtonLabel(3, "gui.simplemap.tab.system.name"));
}

private void updateWidgetVisibility() {
    boolean hideAll = selectedMinimap || selectedCoords;
    boolean showMap = activeTab == 0 && !hideAll;
    boolean showCoords = activeTab == 1 && !hideAll;
    boolean showColors = activeTab == 2 && !hideAll;
    boolean showSystem = activeTab == 3 && !hideAll;

    if (tabMapButton != null) tabMapButton.visible = !hideAll;
    if (tabCoordsButton != null) tabCoordsButton.visible = !hideAll;
    if (tabColorsButton != null) tabColorsButton.visible = !hideAll;
    if (tabSystemButton != null) tabSystemButton.visible = !hideAll;
    if (doneButton != null) doneButton.visible = !hideAll;

    if (minimapToggleButton != null)
        minimapToggleButton.visible = showMap;
    if (minimapRotateButton != null)
        minimapRotateButton.visible = showMap;
    if (minimapShapeButton != null)
        minimapShapeButton.visible = showMap;
    if (minimapFrameButton != null)
        minimapFrameButton.visible = showMap;
    if (minimapSizeSlider != null)
        minimapSizeSlider.visible = showMap;
    if (minimapZoomSlider != null)
        minimapZoomSlider.visible = showMap;
    if (pointerModeButton != null)
        pointerModeButton.visible = showMap;
    if (pointerScaleSlider != null)
        pointerScaleSlider.visible = showMap;
    if (pinScaleSlider != null)
        pinScaleSlider.visible = showMap;
    if (colorTargetButton != null)
        colorTargetButton.visible = showColors;
    if (presetColorButton != null)
        presetColorButton.visible = showColors;
    if (sharedColorInput != null)
        sharedColorInput.visible = showColors;
    if (savePaletteColorButton != null)
        savePaletteColorButton.visible = showColors;
    if (customBlockColorsButton != null) {
        customBlockColorsButton.visible = showColors;
        customBlockColorsButton.setMessage(Component.translatable(
                "gui.simplemap.option.block_overrides", MapConfig.blockColorOverrides.size()));
    }
    refreshPaletteButtons();
    if (compassLettersToggleButton != null)
        compassLettersToggleButton.visible = showMap;
    if (showInScreensButton != null)
        showInScreensButton.visible = showMap;
    if (minimapWaypointsButton != null)
        minimapWaypointsButton.visible = showMap;
    if (mapStyleButton != null)
        mapStyleButton.visible = showColors;

    if (coordsToggleButton != null)
        coordsToggleButton.visible = showCoords;
    if (coordsScaleSlider != null)
        coordsScaleSlider.visible = showCoords;
    if (cursorBiomeButton != null)
        cursorBiomeButton.visible = showCoords;
    if (cursorBlockButton != null)
        cursorBlockButton.visible = showCoords;

    if (autoClearPinButton != null)
        autoClearPinButton.visible = showSystem;
    if (alwaysRescanButton != null)
        alwaysRescanButton.visible = showSystem;
    if (reloadAllRegionsButton != null)
        reloadAllRegionsButton.visible = showSystem;
    if (resetToDefaultButton != null)
        resetToDefaultButton.visible = showSystem;
    if (scanSlider != null)
        scanSlider.visible = showSystem;
    if (displayFlowersButton != null)
        displayFlowersButton.visible = showSystem;
    if (blockColourModeButton != null)
        blockColourModeButton.visible = showSystem;
    if (terrainSlopesButton != null)
        terrainSlopesButton.visible = showSystem;
    if (fastFullscreenButton != null)
        fastFullscreenButton.visible = showSystem;
    if (deathWaypointButton != null)
        deathWaypointButton.visible = showSystem;
    if (deathWaypointLimitButton != null) {
        deathWaypointLimitButton.visible = showSystem;
        deathWaypointLimitButton.active = showSystem && MapConfig.createDeathWaypoint;
    }
}

private static Component onOffComponent(boolean value) {
    return Component.translatable(value ? "gui.simplemap.value.on" : "gui.simplemap.value.off");
}

private Component getToggleMessage() {
    return Component.translatable("gui.simplemap.option.minimap",
            Component.translatable(MapConfig.minimapEnabled
                    ? "gui.simplemap.value.enabled"
                    : "gui.simplemap.value.disabled"));
}

private Component getRotateMessage() {
    return Component.translatable("gui.simplemap.option.north_lock",
            onOffComponent(!MapConfig.minimapRotate));
}

private Component getShapeMessage() {
    return Component.translatable("gui.simplemap.option.shape",
            Component.translatable(MapConfig.minimapCircle
                    ? "gui.simplemap.value.circle"
                    : "gui.simplemap.value.square"));
}

private Component getFrameMessage() {
    Component frameValue = switch (MapConfig.minimapFrameStyle) {
        case 1 -> Component.translatable("gui.simplemap.value.frame_vanilla");
        case 2 -> Component.translatable("gui.simplemap.value.frame_frameless");
        default -> Component.translatable("gui.simplemap.value.frame_minimal");
    };
    return Component.translatable("gui.simplemap.option.frame", frameValue);
}


private Component getCoordsToggleMessage() {
    return Component.translatable("gui.simplemap.option.coordinates",
            onOffComponent(MapConfig.coordsEnabled));
}

private Component getDeathWaypointMessage() {
    return Component.translatable("gui.simplemap.option.death_points",
            onOffComponent(MapConfig.createDeathWaypoint));
}

private Component getDeathWaypointLimitMessage() {
    return Component.translatable("gui.simplemap.option.death_limit", MapConfig.maxDeathWaypoints);
}

private Component getAutoClearPinMessage() {
    return Component.translatable("gui.simplemap.option.auto_clear_pin",
            onOffComponent(MapConfig.autoClearPin));
}

private Component getPointerModeMessage() {
    return Component.translatable("gui.simplemap.option.marker",
            Component.translatable(MapConfig.playerMarkerMode == 1
                    ? "gui.simplemap.value.arrow"
                    : "gui.simplemap.value.skin"));
}

private Component getCompassLettersMessage() {
    return Component.translatable("gui.simplemap.option.compass_labels",
            onOffComponent(MapConfig.compassLettersVisible));
}

private Component getShowInScreensMessage() {
    return Component.translatable("gui.simplemap.option.show_in_screens",
            onOffComponent(MapConfig.showMinimapInScreens));
}

private Component getMinimapWaypointsMessage() {
    return Component.translatable("gui.simplemap.option.minimap_waypoints",
            onOffComponent(MapConfig.minimapWaypointsVisible));
}

private Component getMapStyleMessage() {
    int index = Math.max(0, Math.min(MapColorProfile.NAMES.length - 1, MapConfig.mapColorProfile));
    Component styleName = switch (index) {
        case 1 -> Component.translatable("gui.simplemap.style.vibrant");
        case 2 -> Component.translatable("gui.simplemap.style.natural");
        case 3 -> Component.translatable("gui.simplemap.style.contrast");
        default -> Component.translatable("gui.simplemap.style.balanced");
    };
    return Component.translatable("gui.simplemap.option.style", styleName);
}

private Component getColorTargetMessage() {
    Component targetName = switch (selectedColorTarget) {
        case 1 -> Component.translatable("gui.simplemap.color_target.ring");
        case 2 -> Component.translatable("gui.simplemap.color_target.compass");
        case 3 -> Component.translatable("gui.simplemap.color_target.coords");
        default -> Component.translatable("gui.simplemap.color_target.arrow");
    };
    return Component.translatable("gui.simplemap.option.edit_color", targetName);
}

private Component getSelectedPresetMessage() {
    return Component.translatable("gui.simplemap.option.color_preset",
            getPresetColorName(getSelectedTargetColor()));
}

private int getSelectedTargetColor() {
    return switch (selectedColorTarget) {
        case 1 -> MapConfig.minimapRingColor;
        case 2 -> MapConfig.compassLetterColor;
        case 3 -> MapConfig.coordsTextColor;
        default -> MapConfig.playerPointerColor;
    };
}

private void setSelectedTargetColor(int color) {
    switch (selectedColorTarget) {
        case 1 -> MapConfig.minimapRingColor = color;
        case 2 -> MapConfig.compassLetterColor = color;
        case 3 -> MapConfig.coordsTextColor = color;
        default -> MapConfig.playerPointerColor = color;
    }
    if (presetColorButton != null) presetColorButton.setMessage(getSelectedPresetMessage());
}

private void syncSharedColorInput() {
    if (sharedColorInput != null) {
        sharedColorInput.setValue(MapColorProfile.formatColor(getSelectedTargetColor()));
        sharedColorInput.setTextColor(0xFFFFFFFF);
    }
    if (presetColorButton != null) presetColorButton.setMessage(getSelectedPresetMessage());
}

private void previewSharedColor(String value) {
    try {
        setSelectedTargetColor(MapColorProfile.parseColor(value));
        sharedColorInput.setTextColor(0xFFFFFFFF);
    } catch (IllegalArgumentException ignored) {
        sharedColorInput.setTextColor(0xFFFF5555);
    }
}

private void saveSharedColor() {
    try {
        int color = MapColorProfile.parseColor(sharedColorInput.getValue());
        setSelectedTargetColor(color);
        MapConfig.savedColors.remove(Integer.valueOf(color));
        MapConfig.savedColors.add(0, color);
        while (MapConfig.savedColors.size() > 8) {
            MapConfig.savedColors.remove(MapConfig.savedColors.size() - 1);
        }
        MapConfig.save();
        refreshPaletteButtons();
    } catch (IllegalArgumentException ignored) {
        sharedColorInput.setTextColor(0xFFFF5555);
    }
}

private void applySavedColor(int slot) {
    if (slot < 0 || slot >= MapConfig.savedColors.size()) return;
    setSelectedTargetColor(MapConfig.savedColors.get(slot));
    syncSharedColorInput();
}

private void refreshPaletteButtons() {
    for (int i = 0; i < paletteButtons.size(); i++) {
        ColorSwatchButton swatch = paletteButtons.get(i);
        boolean hasColor = i < MapConfig.savedColors.size();
        if (hasColor) swatch.setColor(MapConfig.savedColors.get(i));
        swatch.visible = activeTab == 2 && hasColor;
    }
}

private int nextPresetColor(int currentColor) {
    for (int i = 0; i < MapConfig.POINTER_COLORS.length; i++) {
        if (currentColor == MapConfig.POINTER_COLORS[i]) {
            return MapConfig.POINTER_COLORS[(i + 1) % MapConfig.POINTER_COLORS.length];
        }
    }
    return MapConfig.POINTER_COLORS[0];
}

private String getPresetColorName(int color) {
    for (int i = 0; i < MapConfig.POINTER_COLORS.length; i++) {
        if (color == MapConfig.POINTER_COLORS[i]) {
            return switch (i) {
                case 0 -> Component.translatable("gui.simplemap.color.cyan").getString();
                case 1 -> Component.translatable("gui.simplemap.color.green").getString();
                case 2 -> Component.translatable("gui.simplemap.color.red").getString();
                case 3 -> Component.translatable("gui.simplemap.color.yellow").getString();
                case 4 -> Component.translatable("gui.simplemap.color.purple").getString();
                case 5 -> Component.translatable("gui.simplemap.color.orange").getString();
                case 6 -> Component.translatable("gui.simplemap.color.white").getString();
                case 7 -> Component.translatable("gui.simplemap.color.rainbow").getString();
                default -> MapConfig.POINTER_COLOR_NAMES[i];
            };
        }
    }
    return Component.translatable("gui.simplemap.color.custom").getString();
}

private Component getFastFullscreenMessage() {
    return Component.translatable("gui.simplemap.option.loading",
            Component.translatable(MapConfig.fastFullscreenLoading
                    ? "gui.simplemap.value.fast"
                    : "gui.simplemap.value.balanced"));
}

private Component getAlwaysRescanMessage() {
    return Component.translatable("gui.simplemap.option.rescan_loaded",
            onOffComponent(MapConfig.alwaysRescanExplored));
}

private static int pointsToChunkBudget(int points) {
    if (points >= 100000) return SCAN_CHUNK_MAX;
    return Math.max(SCAN_CHUNK_MIN,
            Math.min(SCAN_CHUNK_MAX - 1, (points + 255) / 256));
}

private static int chunkBudgetToPoints(int chunks) {
    if (chunks >= SCAN_CHUNK_MAX) return 100000;
    return Math.max(1000, Math.min(99999, chunks * 256));
}

private static int sliderToChunkBudget(double value) {
    return SCAN_CHUNK_MIN + (int) Math.round(value
            * (SCAN_CHUNK_MAX - SCAN_CHUNK_MIN));
}

private static String formatScanBudget(int chunks) {
    return chunks >= SCAN_CHUNK_MAX
            ? Component.translatable("gui.simplemap.option.scan_auto").getString()
            : Component.translatable("gui.simplemap.option.scan_chunks", chunks).getString();
}

private Component getMapRevealModeMessage() {
    return Component.translatable("gui.simplemap.option.mapping_mode",
            Component.translatable("gui.simplemap.value.stream"));
}

private Component getDisplayFlowersMessage() {
    return Component.translatable("gui.simplemap.option.map_flowers",
            onOffComponent(MapConfig.displayFlowers));
}

private Component getBlockColourModeMessage() {
    return Component.translatable("gui.simplemap.option.block_colors",
            Component.translatable(MapConfig.blockColourMode == 1
                    ? "gui.simplemap.value.vanilla"
                    : "gui.simplemap.value.accurate"));
}

private Component getTerrainSlopesMessage() {
    Component mode = switch (MapConfig.terrainSlopes) {
        case 0 -> Component.translatable("gui.simplemap.value.off");
        case 1 -> Component.translatable("gui.simplemap.value.2d");
        default -> Component.translatable("gui.simplemap.value.3d");
    };
    return Component.translatable("gui.simplemap.option.terrain_shading", mode);
}

private Component getCursorBiomeMessage() {
    return Component.translatable("gui.simplemap.option.cursor_biome",
            onOffComponent(MapConfig.cursorBiomeEnabled));
}

private Component getCursorBlockMessage() {
    return Component.translatable("gui.simplemap.option.cursor_block",
            onOffComponent(MapConfig.cursorBlockEnabled));
}

private void refreshLoadedMapData() {
    MapConfig.save();
    MapManager.getInstance().reloadAllRegions();
    if (this.minecraft == null || this.minecraft.player == null) return;
    ChunkScanner.getInstance().requestRefresh(this.minecraft);
    MapTextureManager.getInstance().invalidateStyle();
    CaveTextureManager.getInstance().invalidateStyle();
}

private boolean isRemoteServerSession() {
    return this.minecraft != null && this.minecraft.getConnection() != null && !this.minecraft.isLocalServer();
}

private void syncIntegratedServerConfig() {
    if (this.minecraft == null || this.minecraft.getSingleplayerServer() == null) return;
    net.minecraft.server.MinecraftServer server = this.minecraft.getSingleplayerServer();
    SyncConfigPayload payload = new SyncConfigPayload(ServerConfig.requireMapBook, ServerConfig.caveMapMode);
    server.execute(() -> {
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            NetworkHandler.sendToPlayer(player, payload);
        }
    });
}

private Component getServerRequireBookMessage(boolean remoteServer, boolean remoteExtension) {
    if (remoteServer && !remoteExtension) {
        return Component.translatable("gui.simplemap.server.book_mod_required");
    }
    boolean enabled = remoteServer ? MapConfig.serverRequireMapBook : ServerConfig.requireMapBook;
    Component val = Component.translatable(enabled
            ? "gui.simplemap.value.required"
            : "gui.simplemap.value.optional");
    String suffix = remoteServer
            ? Component.translatable("gui.simplemap.server.suffix_server").getString()
            : "";
    return Component.translatable("gui.simplemap.server.book", val, suffix);
}

private Component getServerCaveModeMessage(boolean remoteServer, boolean remoteExtension) {
    int mode = remoteServer
            ? (remoteExtension ? MapConfig.serverCaveMapMode : MapConfig.localCaveMapMode)
            : ServerConfig.caveMapMode;
    Component name = switch (mode) {
        case 1 -> Component.translatable("gui.simplemap.value.auto");
        case 2 -> Component.translatable("gui.simplemap.value.on");
        default -> Component.translatable("gui.simplemap.value.off");
    };
    String authority = remoteServer
            ? Component.translatable(remoteExtension
                    ? "gui.simplemap.server.suffix_server"
                    : "gui.simplemap.server.suffix_local").getString()
            : "";
    return Component.translatable("gui.simplemap.server.cave_scan", name, authority);
}

@Override
public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
    // The in-map editor suppresses Minecraft's default blur. The global Mod
    // Config branch invokes the superclass explicitly to get the normal blurred
    // game backdrop.
}

@Override
public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
    if (!(parent instanceof MapScreen)) {
        // Global Mod Config uses Minecraft's blurred in-game background and a
        // compact translucent card. The map's own settings screen deliberately
        // keeps its existing custom preview background below.
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        int panelWidth = 216;
        int panelHeight = 140;
        int panelLeft = (this.width - panelWidth) / 2;
        int panelTop = (this.height - panelHeight) / 2 - 10;
        guiGraphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xA818191C);
        guiGraphics.outline(panelLeft, panelTop, panelWidth, panelHeight, 0xB8666A70);
        guiGraphics.centeredText(this.font, Component.translatable("gui.simplemap.server_settings_title"),
                this.width / 2, panelTop + 16, 0xFFF2F2F2);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        return;
    }

    if (!selectedMinimap && !selectedCoords) {
        // Semi-transparent dark overlay — game view shows through behind
        guiGraphics.fill(0, 0, this.width, this.height, 0xAA000000);
    } else {
        guiGraphics.fill(0, 0, this.width, this.height, 0x30000000);
    }

    int size = MapConfig.minimapSize;
    int[] minimapPosition = MinimapPosition.resolve(this.width, this.height, size);
    int x = minimapPosition[0];
    int y = minimapPosition[1];

    boolean isHovered = mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;

    // 1. Render Minimap
    if (MapConfig.minimapEnabled) {
        int borderColor = isHovered || isDraggingMinimap || selectedMinimap
                ? 0xFF00C8FF : MapConfig.minimapRingColor;
        float cx = x + size / 2.0f;
        float cy = y + size / 2.0f;
        float radius = size / 2.0f;
        int borderThickness = Math.max(2, size / 32);

        if (MapConfig.minimapCircle) {
            int tHalf = Math.max(1, borderThickness / 2);
            cx = x + radius;
            cy = y + radius;
            float clipRadius = radius - tHalf;
            int circleBorderColor = isHovered || isDraggingMinimap || selectedMinimap
                    ? 0xFF00C8FF : 0xE0000000;

            MapScreen.drawSolidCircleHelper(guiGraphics, cx, cy, radius, 64, 0xFF080A0C, 0.0f);
            if (this.minecraft != null && this.minecraft.player != null) {
                double dimScale = MapManager.getInstance().getCrossDimensionCoordinateScale();
                double interpX = net.minecraft.util.Mth.lerp(partialTick, this.minecraft.player.xo,
                        this.minecraft.player.getX()) * dimScale;
                double interpZ = net.minecraft.util.Mth.lerp(partialTick, this.minecraft.player.zo,
                        this.minecraft.player.getZ()) * dimScale;
                boolean rendered = MinimapFramebufferRenderer.getInstance().render(
                        guiGraphics, x, y, size, interpX, interpZ,
                        MapConfig.minimapZoom, MapConfig.minimapRotate, partialTick);
                if (!rendered) {
                    MapRenderer.getInstance().drawMap(
                            guiGraphics, x, y, size, size,
                            interpX, interpZ,
                            MapConfig.minimapZoom,
                            false, MapConfig.minimapRotate, true, 0, 0,
                            partialTick);
                }
                MapRenderer.getInstance().drawMinimapPlayerOverlay(
                        guiGraphics, cx, cy, MapConfig.minimapRotate, partialTick);
            }

            float ringWidth = 1.0f;
            float innerRingR = radius - 0.5f;
            float outerRingR = innerRingR + ringWidth;
            switch (MapConfig.minimapFrameStyle) {
                case 2 -> {
                    // Frameless: draw nothing
                }
                case 1 -> {
                    MapScreen.drawCircleRing(guiGraphics, cx, cy, innerRingR - 0.5f, innerRingR + 1.0f, 64, 0xFFD6BE96);
                }
                case 0 -> {
                    MapScreen.drawCircleRing(guiGraphics, cx, cy, innerRingR, outerRingR, 64, borderColor);
                }
            }

            if (isHovered || isDraggingMinimap || selectedMinimap) {
                MapScreen.drawSolidCircleHelper(guiGraphics, cx, cy, clipRadius, 64, 0x4400C8FF, 0.3f);
            }
            if (this.minecraft != null && this.minecraft.player != null) {
                drawCompassDirectionsHelper(guiGraphics, this.minecraft, cx, cy, radius, ringWidth,
                        this.minecraft.player, true, partialTick);
            }
        } else {
            // SQUARE PREVIEW
            if (MapConfig.minimapFrameStyle == 1) {
                MinimapRenderer.renderVanillaMapFrame9Slice(
                        guiGraphics,
                        MinimapRenderer.getInstance().resolveFrameTexture(this.minecraft),
                        x, y, size);
            } else if (MapConfig.minimapFrameStyle == 0 || isHovered || isDraggingMinimap || selectedMinimap) {
                guiGraphics.outline(x - 1, y - 1, size + 2, size + 2, borderColor);
            }

            if (this.minecraft != null && this.minecraft.player != null) {
                double dimScale = MapManager.getInstance().getCrossDimensionCoordinateScale();
                double interpX = net.minecraft.util.Mth.lerp(partialTick, this.minecraft.player.xo,
                        this.minecraft.player.getX()) * dimScale;
                double interpZ = net.minecraft.util.Mth.lerp(partialTick, this.minecraft.player.zo,
                        this.minecraft.player.getZ()) * dimScale;
                boolean rendered = MinimapFramebufferRenderer.getInstance().render(
                        guiGraphics, x, y, size, interpX, interpZ,
                        MapConfig.minimapZoom, MapConfig.minimapRotate, partialTick);
                if (!rendered) {
                    MapRenderer.getInstance().drawMap(
                            guiGraphics, x, y, size, size,
                            interpX, interpZ,
                            MapConfig.minimapZoom,
                            true, MapConfig.minimapRotate, true, 0, 0,
                            partialTick);
                }
                MapRenderer.getInstance().drawMinimapPlayerOverlay(
                        guiGraphics, cx, cy, MapConfig.minimapRotate, partialTick);
            }

            // Draw directions (square)
            if (this.minecraft != null && this.minecraft.player != null) {
                drawCompassDirectionsHelper(guiGraphics, this.minecraft, cx, cy, radius, borderThickness,
                        this.minecraft.player, false, partialTick);
            }

            if (isHovered || isDraggingMinimap || selectedMinimap) {
                guiGraphics.fill(x, y, x + size, y + size, 0x4400C8FF);
            }
        }
    } else {
        int borderColor = isHovered || selectedMinimap ? 0xFF00C8FF : 0xFF666666;
        guiGraphics.fill(x, y, x + size, y + size, 0x55000000);
        guiGraphics.outline(x, y, size, size, borderColor);
        String text = Component.translatable("gui.simplemap.preview.minimap_disabled").getString();
        guiGraphics.text(this.font, text, x + (size - this.font.width(text)) / 2, y + (size - 9) / 2,
                0xFFAAAAAA, false);
    }

    // 2. Render Coordinates (Coords)
    String coords = "0, 80, 0";
    if (this.minecraft != null && this.minecraft.player != null) {
        coords = String.format("%d, %d, %d",
                (int) Math.floor(this.minecraft.player.getX()),
                (int) Math.floor(this.minecraft.player.getY()),
                (int) Math.floor(this.minecraft.player.getZ()));
    }

    int cx, cy;
    if (MapConfig.coordsEnabled) {
        int textWidth = (int) (this.font.width(coords) * MapConfig.coordsScale);
        int textHeight = (int) (9 * MapConfig.coordsScale);

        if (MapConfig.coordsXPercent < 0 || MapConfig.coordsYPercent < 0) {
            cx = x + (size - textWidth) / 2;
            cy = y + size + 4;
        } else {
            cx = (int) (this.width * MapConfig.coordsXPercent);
            cy = (int) (this.height * MapConfig.coordsYPercent);
        }

        cx = Math.max(2, Math.min(cx, this.width - textWidth - 2));
        cy = Math.max(2, Math.min(cy, this.height - textHeight - 2));

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((float)(cx), (float)(cy));
        guiGraphics.pose().scale((float)(MapConfig.coordsScale), (float)(MapConfig.coordsScale));

        int rawWidth = this.font.width(coords);
        guiGraphics.fill(-3, -2, rawWidth + 3, 9, 0x88000000);
        int coordsColor = MapRenderer.getInstance().getActualPointerColor(MapConfig.coordsTextColor);
        guiGraphics.text(this.font, coords, 0, 0, (coordsColor & 0xFF000000) == 0 ? (0xFF000000 | coordsColor) : coordsColor, false);
        guiGraphics.pose().popMatrix();

        boolean isCoordsHovered = mouseX >= cx - 3 && mouseX < cx + textWidth + 3 && mouseY >= cy - 2
                && mouseY < cy + textHeight + 2;
        if (isCoordsHovered || selectedCoords || isDraggingCoords) {
            int borderColor = isCoordsHovered || isDraggingCoords ? 0xFF00C8FF : 0xFF2D3033;
            guiGraphics.outline(cx - 4, cy - 3, textWidth + 8, textHeight + 6, borderColor);
        }
    } else {
        String text = Component.translatable("gui.simplemap.preview.coords_disabled").getString();
        int rawWidth = this.font.width(text);
        int placeholderW = (int) (rawWidth * MapConfig.coordsScale);
        int placeholderH = (int) (9 * MapConfig.coordsScale);

        if (MapConfig.coordsXPercent < 0 || MapConfig.coordsYPercent < 0) {
            cx = x + (size - placeholderW) / 2;
            cy = y + size + 4;
        } else {
            cx = (int) (this.width * MapConfig.coordsXPercent);
            cy = (int) (this.height * MapConfig.coordsYPercent);
        }

        cx = Math.max(2, Math.min(cx, this.width - placeholderW - 2));
        cy = Math.max(2, Math.min(cy, this.height - placeholderH - 2));

        guiGraphics.fill(cx - 3, cy - 2, cx + placeholderW + 3, cy + placeholderH + 2, 0x55000000);
        guiGraphics.outline(cx - 4, cy - 3, placeholderW + 8, placeholderH + 6, 0xFF666666);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((float)(cx), (float)(cy));
        guiGraphics.pose().scale((float)(MapConfig.coordsScale), (float)(MapConfig.coordsScale));
        guiGraphics.text(this.font, text, 0, 0, 0xFFAAAAAA, false);
        guiGraphics.pose().popMatrix();

        boolean isCoordsHovered = mouseX >= cx - 3 && mouseX < cx + placeholderW + 3 && mouseY >= cy - 2
                && mouseY < cy + placeholderH + 2;
        if (isCoordsHovered || selectedCoords || isDraggingCoords) {
            int borderColor = isCoordsHovered || isDraggingCoords ? 0xFF00C8FF : 0xFF2D3033;
            guiGraphics.outline(cx - 4, cy - 3, placeholderW + 8, placeholderH + 6, borderColor);
        }
    }

    if (activeTab == 2) {
        String paletteLabel = Component.translatable(MapConfig.savedColors.isEmpty()
                ? "gui.simplemap.palette.empty_hint"
                : "gui.simplemap.palette.saved_hint").getString();
        guiGraphics.text(this.font, paletteLabel, panelLeft + 6,
                paletteLabelY, 0xFFB0B0B0, false);
    }

    // Draw widgets (sliders, buttons, editbox)
    super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

    // Simple hint above Done button
    int panelCenterX = doneButton.getX() + doneButton.getWidth() / 2;
    int hintY = doneButton.getY() - 14;

    String hint = activeTab == 2 ? Component.translatable("gui.simplemap.hint.color_tab").getString()
            : ((selectedMinimap || selectedCoords)
                    ? Component.translatable("gui.simplemap.hint.drag_move").getString()
                    : (activeTab <= 1 ? Component.translatable("gui.simplemap.hint.click_drag").getString() : ""));
    if (!hint.isEmpty()) {
        guiGraphics.centeredText(this.font, hint, panelCenterX, hintY, 0xFF808080);
    }
}

@Override
public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
    double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
    if (!(parent instanceof MapScreen)) {
        return super.mouseClicked(event, doubleClick);
    }
    if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
        int size = MapConfig.minimapSize;
        int[] minimapPosition = MinimapPosition.resolve(this.width, this.height, size);
        int x = minimapPosition[0];
        int y = minimapPosition[1];

        String coords = MapConfig.coordsEnabled ? "0, 80, 0"
                : Component.translatable("gui.simplemap.preview.coords_disabled").getString();
        if (MapConfig.coordsEnabled && this.minecraft != null && this.minecraft.player != null) {
            coords = String.format("%d, %d, %d",
                    (int) Math.floor(this.minecraft.player.getX()),
                    (int) Math.floor(this.minecraft.player.getY()),
                    (int) Math.floor(this.minecraft.player.getZ()));
        }
        int textWidth = (int) (this.font.width(coords) * MapConfig.coordsScale);
        int textHeight = (int) (9 * MapConfig.coordsScale);
        int cx;
        int cy;
        if (MapConfig.coordsXPercent < 0 || MapConfig.coordsYPercent < 0) {
            cx = x + (size - textWidth) / 2;
            cy = y + size + 4;
        } else {
            cx = (int) (this.width * MapConfig.coordsXPercent);
            cy = (int) (this.height * MapConfig.coordsYPercent);
        }
        cx = Math.max(2, Math.min(cx, this.width - textWidth - 2));
        cy = Math.max(2, Math.min(cy, this.height - textHeight - 2));

        boolean insideMinimap = mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
        boolean insideCoords = mouseX >= cx - 3 && mouseX < cx + textWidth + 3
                && mouseY >= cy - 2 && mouseY < cy + textHeight + 2;

        // While NOT in position-edit mode, let visible setting buttons/sliders handle clicks first
        // so buttons sitting on top of a tucked-away minimap remain cleanly clickable.
        if (!selectedMinimap && !selectedCoords && super.mouseClicked(event, doubleClick)) {
            return true;
        }

        // 1. Check Minimap click (works both to enter edit mode and to grab while already editing)
        if (insideMinimap) {
            this.selectedMinimap = true;
            this.selectedCoords = false;
            updateWidgetVisibility();
            if (MapConfig.minimapEnabled) {
                this.isDraggingMinimap = true;
                this.dragOffsetX = mouseX - x;
                this.dragOffsetY = mouseY - y;
            }
            return true;
        }

        // 2. Check Coords click (works both to enter edit mode and to grab while already editing)
        if (insideCoords) {
            this.selectedCoords = true;
            this.selectedMinimap = false;
            updateWidgetVisibility();
            this.isDraggingCoords = true;
            this.dragCoordsOffsetX = mouseX - cx;
            this.dragCoordsOffsetY = mouseY - cy;
            return true;
        }

        // 3. Clicking empty space while already in position-edit mode -> exit edit mode & save
        if (selectedMinimap || selectedCoords) {
            selectedMinimap = false;
            selectedCoords = false;
            isDraggingMinimap = false;
            isDraggingCoords = false;
            MapConfig.save();
            updateWidgetVisibility();
            return true;
        }

        // 4. Clicking empty space on Map or Coords tab -> enter position-edit mode
        // (hides setting widgets so a minimap/coords hidden behind buttons can be grabbed easily)
        if (activeTab == 0 || activeTab == 1) {
            this.selectedMinimap = (activeTab == 0);
            this.selectedCoords = (activeTab == 1);
            updateWidgetVisibility();
            return true;
        }
        return false;
    }
    return super.mouseClicked(event, doubleClick);
}

@Override
public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
    double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
    if (!(parent instanceof MapScreen)) {
        return super.mouseReleased(event);
    }
    if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
        isDraggingMinimap = false;
        isDraggingCoords = false;
    }
    return super.mouseReleased(event);
}

@Override
public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
    double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
    if (!(parent instanceof MapScreen)) {
        return super.mouseDragged(event, dragX, dragY);
    }
    final double MAGNET_SNAP_RADIUS = 14.0;
    if (isDraggingMinimap) {
        int requestedX = (int) Math.round(mouseX - dragOffsetX);
        int requestedY = (int) Math.round(mouseY - dragOffsetY);
        MinimapPosition.setFromTopLeft(requestedX, requestedY, this.width, this.height, MapConfig.minimapSize);
        // Magnet snap: if detached coords are near the minimap's dock spot, snap them back together
        if (MapConfig.coordsXPercent >= 0f && MapConfig.coordsYPercent >= 0f) {
            int size = MapConfig.minimapSize;
            int[] minimapPos = MinimapPosition.resolve(this.width, this.height, size);
            int mx = minimapPos[0];
            int my = minimapPos[1];
            String coords = "0, 64, 0";
            if (this.minecraft != null && this.minecraft.player != null) {
                coords = String.format("%d, %d, %d",
                        (int) Math.floor(this.minecraft.player.getX()),
                        (int) Math.floor(this.minecraft.player.getY()),
                        (int) Math.floor(this.minecraft.player.getZ()));
            }
            int textWidth = (int) (this.font.width(coords) * MapConfig.coordsScale);
            int textHeight = (int) (9 * MapConfig.coordsScale);
            int cx = Math.max(2, Math.min((int) (this.width * MapConfig.coordsXPercent), this.width - textWidth - 2));
            int cy = Math.max(2, Math.min((int) (this.height * MapConfig.coordsYPercent), this.height - textHeight - 2));
            double dockCenterX = mx + size / 2.0;
            double dockY = my + size + 4.0;
            double coordsCenterX = cx + textWidth / 2.0;
            if (Math.hypot(coordsCenterX - dockCenterX, cy - dockY) <= MAGNET_SNAP_RADIUS) {
                MapConfig.coordsXPercent = -1.0f;
                MapConfig.coordsYPercent = -1.0f;
            }
        }
        return true;
    }
    if (isDraggingCoords) {
        double curX = mouseX - dragCoordsOffsetX;
        double curY = mouseY - dragCoordsOffsetY;
        int size = MapConfig.minimapSize;
        int[] minimapPos = MinimapPosition.resolve(this.width, this.height, size);
        int mx = minimapPos[0];
        int my = minimapPos[1];
        String coords = "0, 64, 0";
        if (this.minecraft != null && this.minecraft.player != null) {
            coords = String.format("%d, %d, %d",
                    (int) Math.floor(this.minecraft.player.getX()),
                    (int) Math.floor(this.minecraft.player.getY()),
                    (int) Math.floor(this.minecraft.player.getZ()));
        }
        int textWidth = (int) (this.font.width(coords) * MapConfig.coordsScale);
        double dockCenterX = mx + size / 2.0;
        double dockY = my + size + 4.0;
        double curCenterX = curX + textWidth / 2.0;
        if (MapConfig.minimapEnabled
                && Math.hypot(curCenterX - dockCenterX, curY - dockY) <= MAGNET_SNAP_RADIUS) {
            // Magnet snap back to minimap dock position
            MapConfig.coordsXPercent = -1.0f;
            MapConfig.coordsYPercent = -1.0f;
        } else {
            MapConfig.coordsXPercent = Math.max(0f, Math.min(1f, (float) (curX / this.width)));
            MapConfig.coordsYPercent = Math.max(0f, Math.min(1f, (float) (curY / this.height)));
        }
        return true;
    }
    return super.mouseDragged(event, dragX, dragY);
}

@Override
public void onClose() {
    MapConfig.save();
    if (this.minecraft != null) {
        this.minecraft.gui.setScreen(this.parent);
    } else {
        super.onClose();
    }
}

@Override
public void removed() {
    MapConfig.save();
    super.removed();
}

@Override
public boolean isPauseScreen() {
    return false;
}

private class ColorSwatchButton extends AbstractButton {
    private final int slot;
    private int color;

    ColorSwatchButton(int x, int y, int width, int height, int slot) {
        super(x, y, width, height, Component.translatable("gui.simplemap.palette.saved_color"));
        this.slot = slot;
    }

    void setColor(int color) {
        this.color = color;
        String formatted = MapColorProfile.formatColor(color);
        setMessage(Component.translatable("gui.simplemap.palette.use_color", formatted));
        setTooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.palette_swatch", formatted)));
    }

    @Override
    public void onPress(net.minecraft.client.input.InputWithModifiers input) {
        if (input.hasShiftDown() && slot < MapConfig.savedColors.size()) {
            MapConfig.savedColors.remove(slot);
            MapConfig.save();
            refreshPaletteButtons();
        } else {
            applySavedColor(slot);
        }
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int border = isHoveredOrFocused() || color == getSelectedTargetColor() ? 0xFFFFFFFF : 0xFF55585D;
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF161719);
        guiGraphics.fill(getX() + 3, getY() + 3, getX() + getWidth() - 3, getY() + getHeight() - 3,
                0xFF000000 | (color & 0x00FFFFFF));
        guiGraphics.outline(getX(), getY(), getWidth(), getHeight(), border);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}

// Custom slider with silent setter for external sync from EditBox (must NOT override AbstractSliderButton.setValue)
private static class SimpleSlider extends AbstractSliderButton {
    private final java.util.function.Consumer<Double> onChange;
    private final java.util.function.Function<Double, String> labelProvider;

    public SimpleSlider(int x, int y, int width, int height, double defaultValue,
            java.util.function.Function<Double, String> labelProvider,
            java.util.function.Consumer<Double> onChange) {
        super(x, y, width, height, Component.literal(labelProvider.apply(defaultValue)), defaultValue);
        this.labelProvider = labelProvider;
        this.onChange = onChange;
    }

    /**
     * Called from the EditBox responder to push a new normalised value (0..1)
     * into the slider WITHOUT triggering applyValue() to avoid a feedback loop.
     */
    public void setSliderValueSilently(double newValue) {
        this.value = Math.max(0.0, Math.min(1.0, newValue));
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(labelProvider.apply(this.value)));
    }

    @Override
    protected void applyValue() {
        onChange.accept(this.value);
    }

    @Override
    public void onRelease(net.minecraft.client.input.MouseButtonEvent event) {
        super.onRelease(event);
        MapConfig.save();
    }
}

private void fillFloat(GuiGraphicsExtractor guiGraphics, float minX, float minY, float maxX, float maxY, int color) {
    guiGraphics.fill(Math.round(minX), Math.round(minY), Math.round(maxX), Math.round(maxY), color);
}

private void drawCompassDirectionsHelper(GuiGraphicsExtractor guiGraphics, Minecraft mc, float cx, float cy, float radius,
        float borderThickness, net.minecraft.world.entity.player.Player player, boolean isCircle,
        float partialTick) {
    if (!MapConfig.compassLettersVisible || radius * 2.0f < 60.0f) {
        return;
    }

    float playerYaw;
    if (MapConfig.minimapRotate) {
        playerYaw = net.minecraft.util.Mth.rotLerp(partialTick, player.yRotO, player.getYRot());
    } else {
        playerYaw = 180.0f;
    }

    float angleN = -playerYaw + 90.0f;
    float angleE = -playerYaw + 180.0f;
    float angleS = -playerYaw + 270.0f;
    float angleW = -playerYaw;

    float limit;
    if (isCircle) {
        limit = radius * 0.915f;
    } else {
        limit = radius + (MapConfig.minimapFrameStyle == 1 ? borderThickness / 2.0f : 0.0f);
    }

    int letterColor = MapRenderer.getInstance().getActualPointerColor(MapConfig.compassLetterColor);
    drawDirectionLetterHelper(guiGraphics, mc, cx, cy, limit, angleN, Component.translatable("gui.simplemap.compass.n").getString(), letterColor, isCircle);
    drawDirectionLetterHelper(guiGraphics, mc, cx, cy, limit, angleE, Component.translatable("gui.simplemap.compass.e").getString(), letterColor, isCircle);
    drawDirectionLetterHelper(guiGraphics, mc, cx, cy, limit, angleS, Component.translatable("gui.simplemap.compass.s").getString(), letterColor, isCircle);
    drawDirectionLetterHelper(guiGraphics, mc, cx, cy, limit, angleW, Component.translatable("gui.simplemap.compass.w").getString(), letterColor, isCircle);
}

private void drawDirectionLetterHelper(GuiGraphicsExtractor guiGraphics, Minecraft mc, float cx, float cy, float limit,
        float angleDegrees, String letter, int color, boolean isCircle) {
    float rad = (float) Math.toRadians(angleDegrees);
    float cos = (float) Math.cos(rad);
    float sin = (float) Math.sin(rad);

    float x, y;
    if (isCircle) {
        x = cx + limit * cos;
        y = cy + limit * sin;
    } else {
        float scale = 1.0f / Math.max(Math.abs(cos), Math.abs(sin));
        x = cx + limit * cos * scale;
        y = cy + limit * sin * scale;
    }

    guiGraphics.pose().pushMatrix();
    guiGraphics.pose().translate((float)(x), (float)(y));
    guiGraphics.pose().scale((float)(0.7f), (float)(0.7f));

    int textWidth = mc.font.width(letter);
    guiGraphics.text(mc.font, letter, -textWidth / 2, -4, (color & 0xFF000000) == 0 ? (0xFF000000 | color) : color, true);
    guiGraphics.pose().popMatrix();
}

/** Searchable editor for every per-block custom color override. */



}
