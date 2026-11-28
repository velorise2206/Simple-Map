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

public class MapScreen extends Screen {
    private static final int TOOLBAR_BUTTON_SIZE = 22;
    private static final int TOOLBAR_MARGIN = 3;
    private static final int TOOLBAR_GAP = 2;
    private static final float TOOLBAR_ICON_SCALE = 1.0f;
    private static final int PLAYER_PANEL_MARGIN = 4;
    private static final int PLAYER_PANEL_HEIGHT = 15;
    private static final int ZOOM_PANEL_MARGIN = 4;
    private static final int ZOOM_PANEL_HEIGHT = 15;
    private static final float DEFAULT_MAP_SCALE = 0.5f;
    private static final long OPEN_ANIMATION_NANOS = 1_000_000_000L;
    /** 5x default view opens at 6x and settles back to 5x. */
    private static final float OPEN_ANIMATION_START_MULTIPLIER = 1.2f;
    private static final long CURSOR_CACHE_NANOS = 120_000_000L;
    private static final long VIEWPORT_INTERACTION_SETTLE_NANOS = 750_000_000L;
    private static final long ACTIVE_PAN_SETTLE_NANOS = 65_000_000L;
    private static final int POPUP_SCREEN_MARGIN = 6;
    private static final int POPUP_HEADER_HEIGHT = 20;
    private static final int POPUP_ROW_HEIGHT = 20;
    private static final double MOMENTUM_FRICTION = 7.5;
    private static final double MAX_MOMENTUM_BLOCKS_PER_SECOND = 12_000.0;
    private static final double ZOOM_SMOOTH_SPEED = 18.0;
    private static float rememberedScale = Float.NaN;
    double centerX;
    double centerZ;
    float scale = DEFAULT_MAP_SCALE;
    private float visualScale = DEFAULT_MAP_SCALE;
    private float currentRenderScale = DEFAULT_MAP_SCALE;
    private boolean zoomAnchorActive = false;
    private double zoomAnchorWorldX;
    private double zoomAnchorWorldZ;
    private double zoomAnchorScreenOffsetX;
    private double zoomAnchorScreenOffsetZ;
    private double dragAnchorCenterX;
    private double dragAnchorCenterZ;
    private double dragAnchorMouseX;
    private double dragAnchorMouseY;
    private double dragCurrentMouseX;
    private double dragCurrentMouseY;
    private double dragLastSampleCenterX;
    private double dragLastSampleCenterZ;
    private boolean isDragging = false;
    private long openAnimationStartNanos = System.nanoTime();
    private long lastFrameNanos;
    private long lastDragSampleNanos;
    private long lastViewportInteractionNanos;
    private double momentumX;
    private double momentumZ;
    private int cachedCursorX = Integer.MIN_VALUE;
    private int cachedCursorZ = Integer.MIN_VALUE;
    private long cachedCursorRevision = Long.MIN_VALUE;
    private long cachedCursorAtNanos;
    private BlockInfo cachedCursorInfo;
    private boolean cursorCacheValid;
    private boolean temporaryVsyncDisabled;
    private final StringBuilder overlayTextBuilder = new StringBuilder(64);
    private int cachedPlayerTextX = Integer.MIN_VALUE;
    private int cachedPlayerTextY = Integer.MIN_VALUE;
    private int cachedPlayerTextZ = Integer.MIN_VALUE;
    private String cachedPlayerLang = "";
    private String cachedPlayerText = "";
    private int cachedZoomTextKey = Integer.MIN_VALUE;
    private String cachedZoomText = "";
    private int cachedCoordTextX = Integer.MIN_VALUE;
    private int cachedCoordTextY = Integer.MIN_VALUE;
    private int cachedCoordTextZ = Integer.MIN_VALUE;
    private int cachedCoordWaterY = Integer.MIN_VALUE;
    private boolean cachedCoordHasY;
    private String cachedCoordText = "";
    private int cachedBiomeX = Integer.MIN_VALUE;
    private int cachedBiomeY = Integer.MIN_VALUE;
    private int cachedBiomeZ = Integer.MIN_VALUE;
    private String cachedBiomeDimension = "";
    private long cachedBiomeCaveRevision = Long.MIN_VALUE;
    private String cachedBiomeText;
    private boolean hasFocusedCoordinateOverride;
    private double focusedCoordinateX;
    private double focusedCoordinateZ;
    private long focusedCoordinateUntilNanos;
    private final CaveStateClassifier caveStateClassifier = CaveStateClassifier.getInstance();
    private final MapVisualClassifier visualClassifier = MapVisualClassifier.getInstance();

    // Popup context menu for waypoints and teleportation
    private boolean isPopupMenuOpen = false;
    private double popupX = 0;
    private double popupY = 0;
    private double popupWorldX = 0;
    private double popupWorldZ = 0;
    private WaypointManager.Waypoint clickedWaypoint = null;

    // Track drag vs click
    private double dragStartX = 0;
    private double dragStartZ = 0;

    public double getCenterX() { return this.centerX; }
    public double getCenterZ() { return this.centerZ; }
    public float getScale() { return this.scale; }

    public MapScreen() {
        super(Component.translatable("gui.simplemap.world_map"));
        Minecraft mc = Minecraft.getInstance();
        MapManager manager = MapManager.getInstance();
        String override = manager.getViewDimensionOverride();
        this.selectedDimension = (override != null && !override.isBlank()) ? override : "LIVE";
        if (mc.player != null) {
            double dimRatio = manager.getCrossDimensionCoordinateScale();
            this.centerX = mc.player.getX() * dimRatio;
            this.centerZ = mc.player.getZ() * dimRatio;
        }
    }

    /** Opens the requested waypoint's dimension and places it at map centre. */
    public void focusOnWaypoint(WaypointManager.Waypoint waypoint) {
        if (waypoint == null || this.minecraft == null) return;
        hasFocusedCoordinateOverride = true;
        MapManager manager = MapManager.getInstance();
        String target = manager.resolveDimensionResourceId(waypoint.dimension);
        if (target.equals(manager.getLiveDimensionResourceId())) {
            selectedDimension = "LIVE";
            manager.returnToLiveDimension(this.minecraft);
        } else {
            selectedDimension = target;
            manager.switchToDimension(target);
        }
        centerX = waypoint.x;
        centerZ = waypoint.z;
        scale = Math.max(getMinimumStableScale(), 0.75f);
        visualScale = scale;
        currentRenderScale = scale;
        cancelMotionAndOpenAnimation();
        cursorCacheValid = false;
        isPopupMenuOpen = false;
        updateToolbarTooltips();
    }

    /** Centers the fullscreen map on a clicked chat coordinate and highlights the target block. */
    public void focusOnCoordinates(double targetX, double targetZ, String targetDimension) {
        hasFocusedCoordinateOverride = true;
        MapManager manager = MapManager.getInstance();
        if (targetDimension != null && !targetDimension.isBlank()) {
            String target = manager.resolveDimensionResourceId(targetDimension);
            if (target.equals(manager.getLiveDimensionResourceId())) {
                selectedDimension = "LIVE";
                if (this.minecraft != null) {
                    manager.returnToLiveDimension(this.minecraft);
                }
            } else {
                selectedDimension = target;
                manager.switchToDimension(target);
            }
        }
        centerX = targetX;
        centerZ = targetZ;
        scale = Math.max(getMinimumStableScale(), Math.max(scale, 1.25f));
        visualScale = scale;
        currentRenderScale = scale;
        focusedCoordinateX = targetX;
        focusedCoordinateZ = targetZ;
        focusedCoordinateUntilNanos = System.nanoTime() + 6_000_000_000L;
        cancelMotionAndOpenAnimation();
        cursorCacheValid = false;
        isPopupMenuOpen = false;
        updateToolbarTooltips();
    }

    private Button waypointsToggleButton;
    private Button waypointListButton;
    private Button refreshMapButton;
    private Button minimapConfigButton;
    private Button nightModeButton;
    private Button caveLayerModeButton;
    private Button dimensionSwitchButton;
    private EditBox caveYInputBox;
    private EditBox gotoCoordsInputBox;
    private int topCoordPanelLeft;
    private int topCoordPanelTop;
    private int topCoordPanelRight;
    private int topCoordPanelBottom;
    private CaveLayerSlider caveLayerSlider;
    private long syncedCaveRevision = Long.MIN_VALUE;
    private int syncedEffectiveCaveMode = Integer.MIN_VALUE;
    private String selectedDimension = "LIVE";
    private final Map<String, double[]> dimensionCameras = new HashMap<>();
    private int toolbarStartX;
    private int toolbarStartY;
    private int toolbarStepY;
    private int toolbarWidth;
    private int toolbarRows;

    @Override
    protected void init() {
        markViewportInteraction();
        FullscreenMapFramebufferRenderer.getInstance().resetFailureState();
        if (this.minecraft != null) {
            /*
             * The fullscreen map has its own configured FPS budget and does not
             * render the 3D level behind this opaque screen.  On a 120/130 Hz
             * display, GLFW VSync otherwise drops the whole screen to the next
             * divisor (~60 FPS) whenever a single frame misses the refresh slot.
             * Bypass VSync only for this screen, without changing/saving the
             * player's option, and restore it from removed().
             */
            // Keep hardware VSync active when enabled by the user (matching smooth GuiMap standard)
            // so fast horizontal/diagonal pans synchronize with display refresh and never tear.
            MapManager.getInstance().updateWorldAndDimension(this.minecraft);
        }
        if (this.width > 0 && !this.hasFocusedCoordinateOverride) {
            float initialScale = (Float.isFinite(rememberedScale) && rememberedScale > 0.0f)
                    ? Math.min(12.0f, rememberedScale)
                    : (this.width - 2) / 1000.0f;
            this.scale = Math.max(getMinimumStableScale(), initialScale);
            this.visualScale = this.scale;
            this.currentRenderScale = this.scale;
        }

        // Pos 1: Settings
        minimapConfigButton = new PixelIconButton(0, 0, TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, MapUiIcons.Icon.SETTINGS, button -> {
            if (this.minecraft != null) {
                this.minecraft.gui.setScreen(new MapConfigScreen(this));
            }
        });
        this.addRenderableWidget(minimapConfigButton);

        // Pos 2: Waypoint Toggle Visibility
        waypointsToggleButton = new PixelIconButton(0, 0, TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, MapUiIcons.Icon.WAYPOINT_OUTLINE,
                button -> {
                    MapConfig.waypointsVisible = !MapConfig.waypointsVisible;
                    MapConfig.save();
                    updateToolbarTooltips();
                });
        this.addRenderableWidget(waypointsToggleButton);

        // Pos 3: Waypoint List Manager (Flag directly below Waypoint Toggle)
        waypointListButton = new PixelIconButton(0, 0, TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, MapUiIcons.Icon.WAYPOINT_LIST, button -> {
            if (this.minecraft != null) {
                this.minecraft.gui.setScreen(new WaypointListScreen(this));
            }
        });
        this.addRenderableWidget(waypointListButton);

        // Pos 4: Night Mode
        nightModeButton = new PixelIconButton(0, 0, TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, MapUiIcons.Icon.SUN, button -> {
            MapConfig.minimapNightMode = (MapConfig.minimapNightMode + 1) % 3;
            MapConfig.save();
            MapManager.getInstance().onNightModeChanged();
            updateToolbarTooltips();
        });
        this.addRenderableWidget(nightModeButton);

        // Pos 5: Refresh
        refreshMapButton = new PixelIconButton(0, 0, TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, MapUiIcons.Icon.REFRESH, button -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                if (Minecraft.getInstance().hasShiftDown()) {
                    MapManager manager = MapManager.getInstance();
                    if (!"LIVE".equals(selectedDimension) || !manager.isViewingLiveDimension()) {
                        selectedDimension = "LIVE";
                        manager.returnToLiveDimension(this.minecraft);
                        if (caveLayerSlider != null) {
                            caveLayerSlider.syncBoundsFromDimension();
                        }
                        updateCaveControlLayout();
                        cursorCacheValid = false;
                        cachedBiomeText = null;
                        isPopupMenuOpen = false;
                        updateToolbarTooltips();
                    }
                    this.centerX = this.minecraft.player.getX();
                    this.centerZ = this.minecraft.player.getZ();
                    this.scale = DEFAULT_MAP_SCALE;
                    rememberedScale = this.scale;
                    this.visualScale = this.scale;
                    this.currentRenderScale = this.scale;
                    clampScaleForCurrentMode();
                    cancelMotionAndOpenAnimation();
                    FullscreenMapFramebufferRenderer.getInstance().invalidate();
                    return;
                }
                ChunkScanner.getInstance().requestRefresh(this.minecraft);
                if (CaveMode.isActive(this.minecraft)) {
                    CaveTextureManager.getInstance().uploadDirtyTextures(true);
                } else {
                    MapTextureManager.getInstance().uploadDirtyTextures(true);
                }
            }
        });
        this.addRenderableWidget(refreshMapButton);

        // Dimension Switcher (above Cave controls)
        dimensionSwitchButton = new PixelIconButton(0, 0, TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE,
                MapUiIcons.Icon.DIMENSION_PORTAL, button -> cycleViewedDimension());
        this.addRenderableWidget(dimensionSwitchButton);

        if (this.minecraft != null && this.minecraft.level != null) {
            caveLayerModeButton = new PixelIconButton(0, 0, TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, MapUiIcons.Icon.CAVE_OFF, button -> {
                if (MapConfig.getEffectiveCaveMapMode() == 0) return;
                markViewportInteraction();
                CaveMode.cycleCaveType(this.minecraft);
                clampScaleForCurrentMode();
                FullscreenMapFramebufferRenderer.getInstance().invalidate();
                FullscreenMapFramebufferRenderer.getInstance().resetFailureState();
                MapRenderer.getInstance().clearFullscreenFallback();
                this.lastViewportInteractionNanos = 0L;
                MapPerformanceGovernor.getInstance().setFullscreenState(true, false);
                if (CaveMode.getCaveType(this.minecraft) == CaveMode.CaveType.OFF) {
                    MapManager.getInstance().ensureCeilingSurfaceRegionsBackfilled();
                    ChunkScanner.getInstance().requestImmediateSurfaceRefresh(this.minecraft);
                } else if (CaveMode.getCaveType(this.minecraft) == CaveMode.CaveType.LAYERED) {
                    ChunkScanner.getInstance().requestImmediateCaveLayerRefresh(this.minecraft);
                } else {
                    ChunkScanner.getInstance().requestImmediateCaveLayerRefresh(
                            this.minecraft);
                }
                if (caveLayerSlider != null) caveLayerSlider.syncFromMode();
                updateCaveControlLayout();
            });
            this.addRenderableWidget(caveLayerModeButton);

            DimensionMapProfile viewedProfile = MapManager.getInstance()
                    .getCurrentDimensionProfile();
            int minimumY = viewedProfile == null
                    ? this.minecraft.level.getMinY() : viewedProfile.minY();
            int maximumY = viewedProfile == null
                    ? this.minecraft.level.getMaxY() : viewedProfile.maxY();
            double initialValue = CaveMode.hasManualTopY(this.minecraft)
                    ? CaveLayerSlider.normalizeNumeric(CaveMode.getSelectedTopY(this.minecraft), minimumY, maximumY)
                    : 0.0;
            caveLayerSlider = new CaveLayerSlider(0, 0, 156, 18,
                    minimumY, maximumY, initialValue);
            caveLayerSlider.setTooltip(Tooltip.create(Component.translatable("gui.simplemap.tooltip.cave_top_y")));
            this.addRenderableWidget(caveLayerSlider);

            caveYInputBox = new EditBox(this.font, 0, 0, 62, 18, Component.literal("Cave Top Y"));
            caveYInputBox.setMaxLength(12);
            caveYInputBox.setHint(Component.literal("Y: AUTO"));
            caveYInputBox.setTooltip(Tooltip.create(Component.literal("Type Cave Top Y (or 'auto') and press Enter")));
            caveYInputBox.setVisible(false);
            caveYInputBox.active = false;
            this.addRenderableWidget(caveYInputBox);

            updateCaveControlLayout();
        }

        gotoCoordsInputBox = new EditBox(this.font, 0, 4, 172, 18, Component.literal("Go to coordinates"));
        gotoCoordsInputBox.setMaxLength(64);
        gotoCoordsInputBox.setHint(Component.literal("X [Y] Z (Enter)"));
        gotoCoordsInputBox.setVisible(false);
        gotoCoordsInputBox.active = false;
        this.addRenderableWidget(gotoCoordsInputBox);

        if (this.minecraft != null && !CaveMode.isActive(this.minecraft)) {
            MapManager.getInstance().ensureCeilingSurfaceRegionsBackfilled();
        }
        layoutToolbar();
        updateToolbarTooltips();
    }

    private void layoutToolbar() {
        Button[] buttons = { minimapConfigButton, waypointsToggleButton, waypointListButton,
                nightModeButton, refreshMapButton };
        toolbarStartX = TOOLBAR_MARGIN;
        toolbarStartY = TOOLBAR_MARGIN;
        toolbarStepY = TOOLBAR_BUTTON_SIZE + TOOLBAR_GAP;
        toolbarWidth = TOOLBAR_BUTTON_SIZE;
        toolbarRows = buttons.length;
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setX(toolbarStartX);
            buttons[i].setY(toolbarStartY + i * toolbarStepY);
            buttons[i].setWidth(TOOLBAR_BUTTON_SIZE);
        }
    }

    private void drawToolbarBackground(GuiGraphicsExtractor guiGraphics) {
        // Intentionally empty
    }

    private void updateToolbarTooltips() {
        if (minimapConfigButton != null) minimapConfigButton.setTooltip(Tooltip.create(
                Component.translatable("gui.simplemap.tooltip.settings_button")));
        if (waypointListButton != null) waypointListButton.setTooltip(Tooltip.create(
                Component.translatable("gui.simplemap.tooltip.waypoint_manager_button")));
        if (waypointsToggleButton != null) waypointsToggleButton.setTooltip(Tooltip.create(
                Component.translatable("gui.simplemap.tooltip.waypoint_visibility",
                        Component.translatable(MapConfig.waypointsVisible ? "gui.simplemap.value.on" : "gui.simplemap.value.off"))));
        if (refreshMapButton != null) refreshMapButton.setTooltip(Tooltip.create(
                Component.translatable("gui.simplemap.tooltip.refresh_map")));
        if (nightModeButton != null) nightModeButton.setTooltip(Tooltip.create(
                Component.translatable("gui.simplemap.tooltip.map_brightness", getNightModeMessage())));
        if (dimensionSwitchButton != null) dimensionSwitchButton.setTooltip(Tooltip.create(
                Component.translatable("gui.simplemap.tooltip.dimension_switch", getDimensionName(selectedDimension))));
        updateCaveTooltip();
    }

    private String getDimensionName(String dim) {
        if ("LIVE".equals(dim)) {
            String live = MapManager.getInstance().getLiveDimensionResourceId();
            return Component.translatable("gui.simplemap.dimension.current",
                    MapManager.displayDimensionName(live)).getString();
        }
        return MapManager.displayDimensionName(dim);
    }

    private void cycleViewedDimension() {
        if (this.minecraft == null) return;
        MapManager manager = MapManager.getInstance();
        String liveDimension = manager.getLiveDimensionResourceId();

        // "Current" already represents the player's live dimension. Excluding that
        // same resource id removes redundant cycles such as Overworld -> Current
        // while the player is already in the Overworld.
        if (!"LIVE".equals(selectedDimension) && selectedDimension.equals(liveDimension)) {
            selectedDimension = "LIVE";
        }
        saveCurrentDimensionCamera();

        List<String> options = new ArrayList<>();
        options.add("LIVE");
        for (String dimension : manager.getSelectableDimensions()) {
            if (dimension == null || dimension.isBlank()
                    || dimension.equals(liveDimension) || options.contains(dimension)) {
                continue;
            }
            options.add(dimension);
        }
        if (options.size() <= 1) {
            selectedDimension = "LIVE";
            updateToolbarTooltips();
            return;
        }
        int currentIndex = options.indexOf(selectedDimension);
        String previous = selectedDimension;
        selectedDimension = options.get((currentIndex + 1 + options.size()) % options.size());

        if ("LIVE".equals(selectedDimension)) manager.returnToLiveDimension(this.minecraft);
        else manager.switchToDimension(selectedDimension);
        restoreDimensionCamera(previous, selectedDimension);
        if (caveLayerSlider != null) caveLayerSlider.syncBoundsFromDimension();
        cursorCacheValid = false;
        isPopupMenuOpen = false;
        updateToolbarTooltips();
    }

    private void saveCurrentDimensionCamera() {
        // Intentionally unused: Camera scales camera position proportionally from the current view
        // rather than jumping to a stale cached coordinate from an earlier dimension visit.
    }

    private void restoreDimensionCamera(String previous, String target) {
        MapManager mapManager = MapManager.getInstance();
        String from = "LIVE".equals(previous)
                ? mapManager.getLiveDimensionResourceId() : previous;
        String to = "LIVE".equals(target)
                ? mapManager.getLiveDimensionResourceId() : target;
        // Scale the current map camera proportionally by each dimension's coordinateScale()
        // (e.g. 8:1 Nether -> Overworld, 1:8 Overworld -> Nether), matching cross-dimension coordinate scaling
        // (GuiMap.playerDimDiv). This opens the target dimension at the exact corresponding
        // relative position of the current map view instead of snapping to an old saved camera.
        DimensionMapProfile fromProfile = mapManager.getDimensionProfile(from);
        DimensionMapProfile toProfile = mapManager.getDimensionProfile(to);
        double ratio = fromProfile.coordinateScale() / toProfile.coordinateScale();
        if (Double.isFinite(ratio) && ratio > 0.0) {
            centerX *= ratio;
            centerZ *= ratio;
        }
        cachedBiomeText = null;
        cancelMotionAndOpenAnimation();
    }

    private void updateCaveTooltip() {
        if (caveLayerModeButton == null) return;
        String detail = switch (CaveMode.getCaveType(this.minecraft)) {
            case OFF -> Component.translatable("gui.simplemap.cave.detail.off").getString();
            case LAYERED -> Component.translatable("gui.simplemap.cave.detail.layered").getString();
            case FULL -> Component.translatable("gui.simplemap.cave.detail.full").getString();
        };
        String permission = MapConfig.getEffectiveCaveMapMode() == 0
                ? "\n" + Component.translatable("gui.simplemap.cave.permission.disabled").getString()
                : "\n" + Component.translatable("gui.simplemap.cave.permission.enabled").getString();
        caveLayerModeButton.setTooltip(Tooltip.create(Component.literal(
                getCaveLayerModeMessage().getString() + "\n" + detail + permission)));
    }

    private void updateCaveControlLayout() {
        int effectiveCaveMode = MapConfig.getEffectiveCaveMapMode();
        syncedCaveRevision = CaveMode.getRevision();
        syncedEffectiveCaveMode = effectiveCaveMode;
        int controlY = this.height - TOOLBAR_BUTTON_SIZE - TOOLBAR_MARGIN;
        if (caveLayerModeButton != null) {
            caveLayerModeButton.active = effectiveCaveMode != 0;
            updateCaveTooltip();
            caveLayerModeButton.setWidth(TOOLBAR_BUTTON_SIZE);
            caveLayerModeButton.setX(TOOLBAR_MARGIN);
            caveLayerModeButton.setY(controlY);
        }
        if (dimensionSwitchButton != null) {
            dimensionSwitchButton.setWidth(TOOLBAR_BUTTON_SIZE);
            dimensionSwitchButton.setX(TOOLBAR_MARGIN);
            dimensionSwitchButton.setY(controlY - TOOLBAR_BUTTON_SIZE - TOOLBAR_GAP);
        }
        if (caveLayerSlider == null || caveLayerModeButton == null) return;

        int availableWidth = Math.max(44, this.width - 38);
        CaveMode.CaveType caveType = CaveMode.getCaveType(this.minecraft);
        // Keeps the cave-start AUTO/manual control available for both
        // layered CAVE and FULL. FULL ignores the exact Top Y for projection, but
        // the value still decides whether cave mode is manually active.
        boolean manualAllowed = MapConfig.getEffectiveCaveMapMode() == 2;
        caveLayerSlider.visible = caveType != CaveMode.CaveType.OFF && manualAllowed;
        caveLayerSlider.active = caveType != CaveMode.CaveType.OFF && manualAllowed;
        if (!caveLayerSlider.visible) {
            hideCaveYInputBox();
            return;
        }

        int rightColX = TOOLBAR_MARGIN + TOOLBAR_BUTTON_SIZE + TOOLBAR_GAP + 2;
        caveLayerSlider.setWidth(Math.min(190, availableWidth));
        caveLayerSlider.setX(rightColX);
        caveLayerSlider.setY(controlY + 2);

        if (caveYInputBox != null) {
            caveYInputBox.setX(rightColX);
            caveYInputBox.setY(controlY - TOOLBAR_BUTTON_SIZE - TOOLBAR_GAP + 2);
            caveYInputBox.setVisible(true);
            caveYInputBox.active = true;
            if (!caveYInputBox.isFocused()) {
                syncCaveYInputValue();
            }
        }
    }

    private void syncCaveYInputValue() {
        if (caveYInputBox == null) return;
        if (this.minecraft != null && CaveMode.hasManualTopY(this.minecraft)) {
            String expected = Integer.toString(CaveMode.getSelectedTopY(this.minecraft));
            if (!expected.equals(caveYInputBox.getValue())) {
                caveYInputBox.setValue(expected);
            }
        } else if (!caveYInputBox.getValue().isEmpty()) {
            caveYInputBox.setValue("");
        }
    }

    private void unfocusCaveYInputBox() {
        if (caveYInputBox == null) return;
        caveYInputBox.setFocused(false);
        if (this.getFocused() == caveYInputBox) {
            this.setFocused(null);
        }
        syncCaveYInputValue();
    }

    private void hideCaveYInputBox() {
        if (caveYInputBox == null) return;
        caveYInputBox.setFocused(false);
        caveYInputBox.setVisible(false);
        caveYInputBox.active = false;
        if (this.getFocused() == caveYInputBox) {
            this.setFocused(null);
        }
    }

    private void submitCaveYInputBox() {
        if (caveYInputBox == null || this.minecraft == null) return;
        String raw = caveYInputBox.getValue() == null ? "" : caveYInputBox.getValue().trim();
        if (raw.isEmpty() || "auto".equalsIgnoreCase(raw)) {
            CaveMode.setAutoTopY(this.minecraft, true);
        } else {
            try {
                int parsedY = Integer.parseInt(raw);
                CaveMode.setManualLayer(this.minecraft, parsedY, true);
            } catch (NumberFormatException ignored) {
                return;
            }
        }
        ChunkScanner.getInstance().requestImmediateCaveLayerRefresh(this.minecraft);
        FullscreenMapFramebufferRenderer.getInstance().invalidate();
        FullscreenMapFramebufferRenderer.getInstance().resetFailureState();
        MapRenderer.getInstance().clearFullscreenFallback();
        if (caveLayerSlider != null) {
            caveLayerSlider.syncFromMode();
        }
        updateCaveControlLayout();
        unfocusCaveYInputBox();
    }

    private void openGotoCoordsInputBox() {
        if (gotoCoordsInputBox == null) return;
        unfocusCaveYInputBox();
        int boxWidth = Math.min(180, Math.max(120, this.width - 24));
        gotoCoordsInputBox.setWidth(boxWidth);
        gotoCoordsInputBox.setX((this.width - boxWidth) / 2);
        gotoCoordsInputBox.setY(4);
        gotoCoordsInputBox.setValue("");
        gotoCoordsInputBox.setVisible(true);
        gotoCoordsInputBox.active = true;
        this.setFocused(gotoCoordsInputBox);
        gotoCoordsInputBox.setFocused(true);
    }

    private void closeGotoCoordsInputBox() {
        if (gotoCoordsInputBox == null) return;
        gotoCoordsInputBox.setFocused(false);
        gotoCoordsInputBox.setVisible(false);
        gotoCoordsInputBox.active = false;
        if (this.getFocused() == gotoCoordsInputBox) {
            this.setFocused(null);
        }
    }

    private void submitGotoCoordsInputBox() {
        if (gotoCoordsInputBox == null) return;
        String raw = gotoCoordsInputBox.getValue() == null ? "" : gotoCoordsInputBox.getValue().trim();
        if (raw.isEmpty()) {
            closeGotoCoordsInputBox();
            return;
        }
        java.util.List<Double> nums = new java.util.ArrayList<>(3);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(raw);
        while (matcher.find() && nums.size() < 3) {
            try {
                nums.add(Double.parseDouble(matcher.group()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (nums.size() < 2) return;
        double targetX = nums.get(0);
        double targetZ = nums.size() >= 3 ? nums.get(2) : nums.get(1);
        closeGotoCoordsInputBox();
        String currentDim = MapManager.getInstance().getCurrentDimensionResourceId();
        focusOnCoordinates(targetX, targetZ, currentDim);
    }

    private void drawCaveControlBackground(GuiGraphicsExtractor guiGraphics) {
        // Intentionally empty. The cave icon and vanilla slider remain individually
        // readable without covering the underlying map with another dark rectangle.
    }

    private Component getWaypointsToggleMessage() {
        return Component.translatable("gui.simplemap.waypoints_short",
                Component.translatable(MapConfig.waypointsVisible ? "gui.simplemap.value.on" : "gui.simplemap.value.off"));
    }

    private Component getNightModeMessage() {
        Component mode = switch (MapConfig.minimapNightMode) {
            case 1 -> Component.translatable("gui.simplemap.value.auto");
            case 2 -> Component.translatable("gui.simplemap.value.on");
            default -> Component.translatable("gui.simplemap.value.off");
        };
        return Component.translatable(
                CaveMode.isActive(this.minecraft) ? "gui.simplemap.cave_light" : "gui.simplemap.night_mode",
                mode);
    }

    private Component getCaveLayerModeMessage() {
        return switch (CaveMode.getCaveType(this.minecraft)) {
            case OFF -> Component.translatable("gui.simplemap.cave_mode.off");
            case LAYERED -> Component.translatable("gui.simplemap.cave_mode.cave");
            case FULL -> Component.translatable("gui.simplemap.cave_mode.full");
        };
    }

    private void cancelMotionAndOpenAnimation() {
        momentumX = 0.0;
        momentumZ = 0.0;
        openAnimationStartNanos = 0L;
        zoomAnchorActive = false;
    }

    private float openAnimationMultiplier(long now) {
        if (openAnimationStartNanos == 0L) return 1.0f;
        float progress = Math.min(1.0f,
                (now - openAnimationStartNanos) / (float) OPEN_ANIMATION_NANOS);
        if (progress >= 1.0f) {
            openAnimationStartNanos = 0L;
            return 1.0f;
        }
        // Smoothstep keeps the 3x-to-2x opening motion visible through the full second.
        float eased = progress * progress * (3.0f - 2.0f * progress);
        return OPEN_ANIMATION_START_MULTIPLIER
                + (1.0f - OPEN_ANIMATION_START_MULTIPLIER) * eased;
    }

    private void updateMomentum(long now) {
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return;
        }
        double seconds = Math.min(0.050, Math.max(0.0, (now - lastFrameNanos) / 1_000_000_000.0));
        lastFrameNanos = now;
        if (seconds <= 0.0) return;

        // Smooth zoom interpolation in log-space (cursor-anchored when zooming via mouse wheel)
        if (Math.abs(visualScale - scale) > scale * 0.0005f) {
            double logVisual = Math.log(Math.max(1e-5, visualScale));
            double logTarget = Math.log(Math.max(1e-5, scale));
            double factor = 1.0 - Math.exp(-ZOOM_SMOOTH_SPEED * seconds);
            double nextLog = logVisual + (logTarget - logVisual) * factor;
            if (Math.abs(logTarget - nextLog) < 0.0005) {
                visualScale = scale;
            } else {
                visualScale = (float) Math.exp(nextLog);
            }
            if (zoomAnchorActive) {
                centerX = zoomAnchorWorldX - zoomAnchorScreenOffsetX / visualScale;
                centerZ = zoomAnchorWorldZ - zoomAnchorScreenOffsetZ / visualScale;
            }
            if (visualScale == scale) {
                zoomAnchorActive = false;
            }
        } else if (visualScale != scale) {
            visualScale = scale;
            if (zoomAnchorActive) {
                centerX = zoomAnchorWorldX - zoomAnchorScreenOffsetX / visualScale;
                centerZ = zoomAnchorWorldZ - zoomAnchorScreenOffsetZ / visualScale;
                zoomAnchorActive = false;
            }
        }

        if (isDragging) {
            // Sample live sub-pixel cursor coordinates every render frame instead of waiting
            // for discrete mouseDragged events, eliminating 0-event/2-event pan micro-stutter.
            dragCurrentMouseX = sampleSubpixelMouseX(dragCurrentMouseX);
            dragCurrentMouseY = sampleSubpixelMouseY(dragCurrentMouseY);
            double activeScale = Math.max(0.0001f, this.visualScale);
            double targetCenterX = dragAnchorCenterX - (dragCurrentMouseX - dragAnchorMouseX) / activeScale;
            double targetCenterZ = dragAnchorCenterZ - (dragCurrentMouseY - dragAnchorMouseY) / activeScale;
            double stepX = targetCenterX - dragLastSampleCenterX;
            double stepZ = targetCenterZ - dragLastSampleCenterZ;
            this.centerX = targetCenterX;
            this.centerZ = targetCenterZ;
            if (Math.abs(stepX) > 1e-6 || Math.abs(stepZ) > 1e-6) {
                double sampleDt = lastDragSampleNanos == 0L ? seconds
                        : Math.max(0.001, Math.min(0.050, (now - lastDragSampleNanos) / 1_000_000_000.0));
                momentumX = clampMomentum(momentumX * 0.35 + (stepX / sampleDt) * 0.65);
                momentumZ = clampMomentum(momentumZ * 0.35 + (stepZ / sampleDt) * 0.65);
                dragLastSampleCenterX = targetCenterX;
                dragLastSampleCenterZ = targetCenterZ;
                lastDragSampleNanos = now;
            } else if (now - lastDragSampleNanos > 60_000_000L) {
                double holdDecay = Math.exp(-MOMENTUM_FRICTION * 2.0 * seconds);
                momentumX *= holdDecay;
                momentumZ *= holdDecay;
            }
            return;
        }
        if (Math.abs(momentumX) < 0.5 && Math.abs(momentumZ) < 0.5) {
            momentumX = 0.0;
            momentumZ = 0.0;
            return;
        }
        centerX += momentumX * seconds;
        centerZ += momentumZ * seconds;
        double damping = Math.exp(-MOMENTUM_FRICTION * seconds);
        momentumX *= damping;
        momentumZ *= damping;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Override to draw a plain dark background instead of Minecraft 1.21's
        // built-in gaussian blur post-processing shader (which blurs our map).
        // We draw the background ourselves in render(), so this is intentionally empty.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        long frameNow = System.nanoTime();
        updateMomentum(frameNow);
        clampScaleForCurrentMode();
        currentRenderScale = visualScale * openAnimationMultiplier(frameNow);
        // Keep cave button active state, tooltip, and slider synchronized live
        // whenever server config sync or CaveMode revision changes while MapScreen is open.
        if (caveLayerModeButton != null
                && (syncedCaveRevision != CaveMode.getRevision()
                        || syncedEffectiveCaveMode != MapConfig.getEffectiveCaveMapMode())) {
            if (caveLayerSlider != null && !caveLayerSlider.isDragging()) {
                caveLayerSlider.syncFromMode();
            }
            updateCaveControlLayout();
        }
        if (caveLayerSlider != null && caveLayerSlider.isDragging()) {
            caveLayerSlider.flushPendingLiveScrub(frameNow);
        }
        // AUTO is a live view: keep the compact Y readout synchronized with the
        // player's stable scan band without turning the slider into manual mode.
        if (caveLayerSlider != null && !caveLayerSlider.isDragging()
                && !CaveMode.hasManualTopY(this.minecraft)) {
            caveLayerSlider.syncFromMode();
        }
        // Fullscreen dark background
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF0D0D0D);

        // Thin 2-pixel border at screen edges (clean 2-pixel margin)
        guiGraphics.fill(0, 0, this.width, 1, 0xFF2D3033); // top
        guiGraphics.fill(0, this.height - 1, this.width, this.height, 0xFF2D3033); // bottom
        guiGraphics.fill(0, 0, 1, this.height, 0xFF2D3033); // left
        guiGraphics.fill(this.width - 1, 0, this.width, this.height, 0xFF2D3033); // right

        // If map is not unlocked, draw a lock message and only render widgets
        if (!com.velorise.simplemap.SimpleMap.isMapUnlocked(this.minecraft.player)) {
            drawToolbarBackground(guiGraphics);
            drawCaveControlBackground(guiGraphics);
            super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            String title = Component.translatable("gui.simplemap.map_locked.title").getString();
            String subtitle = Component.translatable(MapManager.getInstance().hasLearnedMap()
                ? "gui.simplemap.map_locked.hold_book"
                : "gui.simplemap.map_locked.craft_book").getString();
            int tw1 = this.font.width(title);
            int tw2 = this.font.width(subtitle);
            guiGraphics.text(this.font, title, (this.width - tw1) / 2, this.height / 2 - 20, 0xFFFF5555, false);
            guiGraphics.text(this.font, subtitle, (this.width - tw2) / 2, this.height / 2, 0xFFAAAAAA, false);
            return;
        }

        // Viewport occupies the full screen (minus 1px border)
        int vx = 1, vy = 1, vw = this.width - 2, vh = this.height - 2;

        // Mouse world coordinates relative to fullscreen viewport centre
        double mouseWorldX = centerX + (mouseX - vx - vw / 2.0) / currentRenderScale;
        double mouseWorldZ = centerZ + (mouseY - vy - vh / 2.0) / currentRenderScale;

        // Scanning, IO and texture publication are handled by MapViewportCoordinator
        // from client tick. Retained composition may reuse one texture for many
        // visual frames, so refresh the lightweight fullscreen demand independently
        // from atlas replay. Otherwise demand expires while the FBO is doing exactly
        // what it should: not calling MapRenderer every frame.
        double demandGuard = FullscreenMapFramebufferRenderer.demandOverscanPixels();
        // Use the stable post-animation zoom for loading. Otherwise the one-second
        // opening animation changes page bounds every frame and repeatedly rebases
        // the centre-out planner while the final viewport is still cold.
        float demandScale = scale;
        double demandHalfW = (vw * 0.5 + demandGuard)
                / Math.max(0.0001f, demandScale);
        double demandHalfH = (vh * 0.5 + demandGuard)
                / Math.max(0.0001f, demandScale);
        boolean viewportInteracting = isViewportInteracting(frameNow);
        MapViewportCoordinator.getInstance().submitFullscreen(
                centerX - demandHalfW, centerX + demandHalfW,
                centerZ - demandHalfH, centerZ + demandHalfH,
                demandScale, centerX, centerZ, viewportInteracting);

        // Draw map fullscreen directly to the main framebuffer, North-up, no rotation.
        boolean drawPlayerInCurrentDimension = MapManager.getInstance().isViewingLiveDimension();
        MapRenderer.getInstance().drawMap(
            guiGraphics,
            vx, vy, vw, vh,
            centerX, centerZ, currentRenderScale,
            drawPlayerInCurrentDimension,
            false,
            false, mouseWorldX, mouseWorldZ,
            partialTick, false
        );

        if (!MapManager.getInstance().isViewingLiveDimension()
                && !MapManager.getInstance().hasSavedDataForCurrentDimension()) {
            String message = Component.translatable("gui.simplemap.no_saved_data",
                    MapManager.displayDimensionName(
                            MapManager.getInstance().getCurrentDimensionResourceId())).getString();
            int textWidth = this.font.width(message);
            guiGraphics.fill((this.width - textWidth) / 2 - 6, this.height / 2 - 8,
                    (this.width + textWidth) / 2 + 6, this.height / 2 + 8, 0xB0000000);
            guiGraphics.text(this.font, message,
                    (this.width - textWidth) / 2, this.height / 2 - 4,
                    0xFFB8B8B8, false);
        }

        // Highlight target coordinate when opened from a clickable chat coordinate
        if (frameNow < focusedCoordinateUntilNanos) {
            float screenTargetX = (float) (vx + vw * 0.5 + (focusedCoordinateX - centerX) * currentRenderScale);
            float screenTargetY = (float) (vy + vh * 0.5 + (focusedCoordinateZ - centerZ) * currentRenderScale);
            if (screenTargetX >= vx && screenTargetX <= vx + vw
                    && screenTargetY >= vy && screenTargetY <= vy + vh) {
                float remSec = (focusedCoordinateUntilNanos - frameNow) / 1_000_000_000.0f;
                float pulse = 0.5f + 0.5f * (float) Math.sin((6.0f - remSec) * 6.5f);
                float ringR = 7.0f + pulse * 4.5f;
                int alpha = Math.min(255, (int) (Math.min(1.0f, remSec * 1.2f) * 235.0f));
                int ringColor = (alpha << 24) | 0x00E5FF;
                int shadowColor = (Math.min(255, alpha / 2) << 24);
                drawCircleRing(guiGraphics, screenTargetX, screenTargetY, ringR - 1.5f, ringR + 1.5f, 48, shadowColor);
                drawCircleRing(guiGraphics, screenTargetX, screenTargetY, ringR - 0.75f, ringR + 0.75f, 48, ringColor);
                drawSolidCircleHelper(guiGraphics, screenTargetX, screenTargetY, 2.2f, 24, ringColor, 0.0f);
            }
        }

        // Draw GUI widgets (buttons)
        guiGraphics.nextStratum();
        drawToolbarBackground(guiGraphics);
        drawCaveControlBackground(guiGraphics);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        // Draw coordinates overlay
        drawCoordsOverlay(guiGraphics, mouseX, mouseY);

        // Draw the custom context menu if open
        if (isPopupMenuOpen) {
            drawPopupMenu(guiGraphics, mouseX, mouseY);
        }
    }

    private static final java.util.Map<String, net.minecraft.resources.Identifier> CIRCLE_TEX_CACHE =
            new java.util.LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, net.minecraft.resources.Identifier> eldest) {
                    if (size() > 32) {
                        final net.minecraft.resources.Identifier evicted = eldest.getValue();
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc != null) {
                            mc.execute(() -> {
                                if (mc.getTextureManager() != null) {
                                    mc.getTextureManager().release(evicted);
                                }
                            });
                        }
                        return true;
                    }
                    return false;
                }
            };
    private static int circleTexCounter = 0;

    private static net.minecraft.resources.Identifier getOrCreateCircleTexture(int texSize, float innerFrac, int color) {
        int safeColor = (color & 0xFF000000) == 0 ? (0xFF000000 | color) : color;
        int innerBucket = Math.round(innerFrac * 200.0f);
        String key = texSize + ":" + innerBucket + ":" + safeColor;
        net.minecraft.resources.Identifier existing = CIRCLE_TEX_CACHE.get(key);
        if (existing != null) return existing;

        com.mojang.blaze3d.platform.NativeImage img = new com.mojang.blaze3d.platform.NativeImage(texSize, texSize, true);
        int baseA = (safeColor >>> 24) & 0xFF;
        int rgb = safeColor & 0x00FFFFFF;
        float half = texSize * 0.5f;
        float outerR = half - 1.0f;
        float innerR = innerFrac <= 0.0f ? -2.0f : outerR * innerFrac;
        for (int py = 0; py < texSize; py++) {
            float dy = (py + 0.5f) - half;
            for (int px = 0; px < texSize; px++) {
                float dx = (px + 0.5f) - half;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float outerCov = Math.max(0.0f, Math.min(1.0f, outerR - dist + 0.5f));
                float innerCov = innerR < 0.0f ? 1.0f : Math.max(0.0f, Math.min(1.0f, dist - innerR + 0.5f));
                float cov = outerCov * innerCov;
                int a = Math.round(baseA * cov);
                img.setPixel(px, py, a > 0 ? ((a << 24) | rgb) : 0);
            }
        }
        final int texNum = ++circleTexCounter;
        net.minecraft.client.renderer.texture.DynamicTexture tex =
                new net.minecraft.client.renderer.texture.DynamicTexture(() -> "simplemap_circle_" + texNum, img);
        net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.fromNamespaceAndPath(
                "simplemap", "dynamic/circle_" + texNum);
        net.minecraft.client.Minecraft.getInstance().getTextureManager().register(id, tex);
        CIRCLE_TEX_CACHE.put(key, id);
        return id;
    }

    public static void drawSolidCircleHelper(GuiGraphicsExtractor guiGraphics, float cx, float cy, float radius, int numSegments,
            int color, float z) {
        if (radius <= 0.5f) return;
        int approxDiameter = Math.max(2, Math.round(radius * 2.0f));
        int texSize = approxDiameter <= 32 ? 32 : (approxDiameter <= 96 ? 128 : 256);
        net.minecraft.resources.Identifier id = getOrCreateCircleTexture(texSize, 0.0f, color);
        float outerTexR = texSize * 0.5f - 1.0f;
        float scaleFactor = radius / outerTexR;
        int halfTex = texSize / 2;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(cx, cy);
        guiGraphics.pose().scale(scaleFactor, scaleFactor);
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, id,
                -halfTex, -halfTex, 0.0f, 0.0f, texSize, texSize, texSize, texSize, texSize, texSize);
        guiGraphics.pose().popMatrix();
    }

    public static void drawCircleRing(GuiGraphicsExtractor guiGraphics, float cx, float cy, float r1, float r2, int numSegments,
            int color) {
        float innerR = Math.min(r1, r2);
        float outerR = Math.max(r1, r2);
        if (outerR <= 0.5f) return;
        int approxDiameter = Math.max(2, Math.round(outerR * 2.0f));
        int texSize = approxDiameter <= 96 ? 128 : 256;
        float innerFrac = Math.max(0.0f, Math.min(0.98f, innerR / outerR));
        net.minecraft.resources.Identifier id = getOrCreateCircleTexture(texSize, innerFrac, color);
        float outerTexR = texSize * 0.5f - 1.0f;
        float scaleFactor = outerR / outerTexR;
        int halfTex = texSize / 2;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(cx, cy);
        guiGraphics.pose().scale(scaleFactor, scaleFactor);
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, id,
                -halfTex, -halfTex, 0.0f, 0.0f, texSize, texSize, texSize, texSize, texSize, texSize);
        guiGraphics.pose().popMatrix();
    }

    private static class BlockInfo {
        final String name;
        final String id;
        final int y;
        final int waterSurfaceY;
        final boolean cached;

        BlockInfo(String name, String id, int y) {
            this(name, id, y, Integer.MIN_VALUE, false);
        }

        BlockInfo(String name, String id, int y, int waterSurfaceY) {
            this(name, id, y, waterSurfaceY, false);
        }

        BlockInfo(String name, String id, int y, int waterSurfaceY, boolean cached) {
            this.name = name;
            this.id = id;
            this.y = y;
            this.waterSurfaceY = waterSurfaceY;
            this.cached = cached;
        }

        boolean hasWaterSurface() {
            return waterSurfaceY != Integer.MIN_VALUE;
        }
    }

    /** Small vanilla-backed button whose foreground comes from our shared icon atlas. */
    private static final class PixelIconButton extends Button {
        private final MapUiIcons.Icon icon;

        private PixelIconButton(int x, int y, int width, int height,
                MapUiIcons.Icon icon, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.icon = icon;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            if (!visible) {
                return;
            }
            MapUiIcons.Icon displayedIcon = icon;
            if (icon == MapUiIcons.Icon.WAYPOINT_OUTLINE) {
                displayedIcon = MapConfig.waypointsVisible
                        ? MapUiIcons.Icon.WAYPOINT_FILLED
                        : MapUiIcons.Icon.WAYPOINT_OUTLINE;
            } else if (icon == MapUiIcons.Icon.SUN) {
                displayedIcon = switch (MapConfig.minimapNightMode) {
                    case 1 -> MapUiIcons.Icon.NIGHT_AUTO;
                    case 2 -> MapUiIcons.Icon.MOON;
                    default -> MapUiIcons.Icon.SUN;
                };
            } else if (icon == MapUiIcons.Icon.CAVE_ON || icon == MapUiIcons.Icon.CAVE_OFF) {
                // The hollow cave icon means cave view is active; the solid icon means OFF.
                displayedIcon = CaveMode.getCaveType(Minecraft.getInstance()) == CaveMode.CaveType.OFF
                        ? MapUiIcons.Icon.CAVE_OFF
                        : MapUiIcons.Icon.CAVE_ON;
            }
            if (isHoveredOrFocused()) {
                graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x12FFFFFF);
            }
            MapUiIcons.drawScaled(graphics, displayedIcon,
                    getX() + getWidth() / 2, getY() + getHeight() / 2,
                    active, TOOLBAR_ICON_SCALE);
        }
    }

    private void drawCoordsOverlay(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Calculate world coordinates under the mouse cursor (viewport starts at x=1, y=1)
        double worldX = centerX + (mouseX - 1 - (this.width - 2) / 2.0) / currentRenderScale;
        double worldZ = centerZ + (mouseY - 1 - (this.height - 2) / 2.0) / currentRenderScale;

        int bx = (int) Math.floor(worldX);
        int bz = (int) Math.floor(worldZ);

        // Resolve live world data first. Outside the client's chunk radius, fall back
        // to the persistent surface/vertical archive instead of dropping the Y value.
        int yCoord = 0;
        int waterSurfaceY = Integer.MIN_VALUE;
        boolean hasY = false;
        boolean chunkLoaded = !this.isDragging
                && MapManager.getInstance().isViewingLiveDimension()
                && this.minecraft != null && this.minecraft.level != null
                && this.minecraft.level.hasChunk(bx >> 4, bz >> 4);
        BlockInfo resolved = this.isDragging
                ? null
                : resolveCursorBlockInfo(bx, bz, chunkLoaded, System.nanoTime());
        if (resolved != null && (!resolved.name.isEmpty() || resolved.cached)) {
            yCoord = resolved.y;
            waterSurfaceY = resolved.waterSurfaceY;
            hasY = true;
        }

        String coordText = buildCoordinateText(bx, bz, hasY,
                yCoord, waterSurfaceY);

        String blockText = null;
        if (MapConfig.cursorBlockEnabled && resolved != null && resolved.name != null && !resolved.name.isBlank()) {
            blockText = resolved.name;
        }

        String biomeText = null;
        if (MapConfig.cursorBiomeEnabled && hasY
                && this.minecraft != null && this.minecraft.level != null) {
            biomeText = resolveCursorBiomeText(bx, yCoord, bz, chunkLoaded);
        }

        int coordWidth = this.font.width(coordText);
        int blockWidth = blockText == null ? 0 : this.font.width(blockText);
        int biomeWidth = biomeText == null ? 0 : this.font.width(biomeText);
        int panelWidth = Math.max(coordWidth, Math.max(blockWidth, biomeWidth)) + 10;
        int lines = 1 + (blockText == null ? 0 : 1) + (biomeText == null ? 0 : 1);
        int panelHeight = 3 + lines * 10;
        int panelX = (this.width - panelWidth) / 2;
        // Nudge coordinate readout closer to the top edge
        int panelY = 4;
        topCoordPanelLeft = panelX;
        topCoordPanelTop = panelY;
        topCoordPanelRight = panelX + panelWidth;
        topCoordPanelBottom = panelY + panelHeight;
        if (gotoCoordsInputBox != null && gotoCoordsInputBox.isVisible()) {
            // Keep the top-center slot clear for the active Go-To coordinate input box.
            drawZoomPanel(guiGraphics);
            return;
        }
        boolean topPanelHovered = isInside(mouseX, mouseY,
                topCoordPanelLeft, topCoordPanelTop, topCoordPanelRight, topCoordPanelBottom);
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight,
                topPanelHovered ? 0xAA000000 : 0x88000000);
        if (topPanelHovered) {
            guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0x88FFFFFF);
            guiGraphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0x88FFFFFF);
            guiGraphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0x88FFFFFF);
            guiGraphics.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, 0x88FFFFFF);
        }
        int lineY = panelY + 3;
        guiGraphics.text(this.font, coordText,
                panelX + (panelWidth - coordWidth) / 2, lineY, 0xFFFFFFFF, false);
        lineY += 10;
        if (blockText != null) {
            guiGraphics.text(this.font, blockText,
                    panelX + (panelWidth - blockWidth) / 2, lineY, 0xFFD8D8D8, false);
            lineY += 10;
        }
        if (biomeText != null) {
            guiGraphics.text(this.font, biomeText,
                    panelX + (panelWidth - biomeWidth) / 2, lineY, 0xFFA8D69C, false);
        }

        drawZoomPanel(guiGraphics);

        // Render player coordinates at bottom-right. Bounds and text use one shared
        // calculation so the clickable panel cannot drift below the label.
        if (this.minecraft != null && this.minecraft.player != null) {
            String playerText = getPlayerCoordinatesText();
            int playerPanelWidth = this.font.width(playerText) + 10;
            int panelRight = this.width - PLAYER_PANEL_MARGIN;
            int panelBottom = this.height - PLAYER_PANEL_MARGIN;
            int panelLeft = panelRight - playerPanelWidth;
            int panelTop = panelBottom - PLAYER_PANEL_HEIGHT;
            boolean hovered = isInside(mouseX, mouseY,
                    panelLeft, panelTop, panelRight, panelBottom);
            guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom,
                    hovered ? 0xAA000000 : 0x78000000);
            if (hovered) {
                guiGraphics.outline(panelLeft, panelTop, playerPanelWidth,
                        PLAYER_PANEL_HEIGHT, 0x88FFFFFF);
            }
            guiGraphics.text(this.font, playerText, panelLeft + 5, panelTop + 3,
                    0xFFFFFFFF, false);
            if (hovered) {
                guiGraphics.setTooltipForNextFrame(this.font,
                        Component.translatable("gui.simplemap.copy_coords"),
                        mouseX, mouseY);
            }
        }
    }

    private void drawZoomPanel(GuiGraphicsExtractor guiGraphics) {
        String zoomText = getZoomText();
        int panelWidth = this.font.width(zoomText) + 10;
        int left = (this.width - panelWidth) / 2;
        int bottom = this.height - ZOOM_PANEL_MARGIN;
        int top = bottom - ZOOM_PANEL_HEIGHT;
        guiGraphics.fill(left, top, left + panelWidth, bottom, 0x78000000);
        guiGraphics.text(this.font, zoomText, left + 5, top + 3,
                0xFFE6E6E6, false);
    }

    private String getZoomText() {
        // MapScreen.scale is expressed in logical GUI pixels per block. the 
        // fullscreen label is effectively physical framebuffer pixels per block on
        // a 1080p display. Printing the logical value made 0.262x in Simple Map look
        // comparable to 0.262x in world units even though GUI scale 3 renders it at 0.786
        // physical pixels/block. Report the actual screen density so screenshots and
        // performance tests use the same unit.
        double guiScale = this.minecraft == null || this.minecraft.getWindow() == null
                ? 1.0D : Math.max(1.0D, this.minecraft.getWindow().getGuiScale());
        float displayedScale = (float) (Math.max(getMinimumStableScale(), this.currentRenderScale)
                * guiScale);
        int decimals = displayedScale < 1.0f ? 3 : displayedScale < 10.0f ? 2 : 1;
        int divisor = decimals == 3 ? 1000 : decimals == 2 ? 100 : 10;
        int quantized = Math.round(displayedScale * divisor);
        int key = quantized * 31 + divisor;
        if (key == cachedZoomTextKey) return cachedZoomText;
        int fraction = Math.abs(quantized % divisor);
        overlayTextBuilder.setLength(0);
        overlayTextBuilder.append(quantized / divisor).append('.');
        if (decimals >= 2 && fraction < divisor / 10) overlayTextBuilder.append('0');
        if (decimals == 3 && fraction < 10) overlayTextBuilder.append('0');
        overlayTextBuilder.append(fraction).append('x');
        cachedZoomTextKey = key;
        cachedZoomText = overlayTextBuilder.toString();
        return cachedZoomText;
    }

    private String getPlayerCoordinatesText() {
        int x = (int) Math.floor(this.minecraft.player.getX());
        int y = (int) Math.floor(this.minecraft.player.getY());
        int z = (int) Math.floor(this.minecraft.player.getZ());
        String lang = this.minecraft.getLanguageManager() != null
                ? this.minecraft.getLanguageManager().getSelected() : "";
        if (x == cachedPlayerTextX && y == cachedPlayerTextY
                && z == cachedPlayerTextZ && lang.equals(cachedPlayerLang)) return cachedPlayerText;
        overlayTextBuilder.setLength(0);
        overlayTextBuilder.append(Component.translatable("gui.simplemap.player_x_prefix").getString());
        appendGroupedInteger(overlayTextBuilder, x);
        overlayTextBuilder.append(" | Y: ");
        appendGroupedInteger(overlayTextBuilder, y);
        overlayTextBuilder.append(" | Z: ");
        appendGroupedInteger(overlayTextBuilder, z);
        cachedPlayerTextX = x;
        cachedPlayerTextY = y;
        cachedPlayerTextZ = z;
        cachedPlayerLang = lang;
        cachedPlayerText = overlayTextBuilder.toString();
        return cachedPlayerText;
    }

    private String buildCoordinateText(int x, int z, boolean hasY,
            int y, int waterSurfaceY) {
        if (x == cachedCoordTextX && z == cachedCoordTextZ
                && hasY == cachedCoordHasY
                && (!hasY || (y == cachedCoordTextY
                        && waterSurfaceY == cachedCoordWaterY))) {
            return cachedCoordText;
        }
        overlayTextBuilder.setLength(0);
        overlayTextBuilder.append("X: ");
        appendGroupedInteger(overlayTextBuilder, x);
        if (hasY) {
            overlayTextBuilder.append("  Y: ").append(y);
            if (waterSurfaceY != Integer.MIN_VALUE) {
                overlayTextBuilder.append(" (").append(waterSurfaceY).append(')');
            }
        }
        overlayTextBuilder.append("  Z: ");
        appendGroupedInteger(overlayTextBuilder, z);
        cachedCoordTextX = x;
        cachedCoordTextY = y;
        cachedCoordTextZ = z;
        cachedCoordWaterY = waterSurfaceY;
        cachedCoordHasY = hasY;
        cachedCoordText = overlayTextBuilder.toString();
        return cachedCoordText;
    }

    private String resolveCursorBiomeText(int x, int y, int z) {
        boolean chunkLoaded = MapManager.getInstance().isViewingLiveDimension()
                && this.minecraft != null && this.minecraft.level != null
                && this.minecraft.level.hasChunk(x >> 4, z >> 4);
        return resolveCursorBiomeText(x, y, z, chunkLoaded);
    }

    private static boolean isInvalidPlainsForDimension(String biomeId, boolean isNether, boolean isEnd) {
        return (isNether || isEnd) && "minecraft:plains".equals(biomeId);
    }

    private static String inferNetherBiomeFromBlockId(String blockId) {
        if (blockId == null || blockId.isBlank()) return "minecraft:nether_wastes";
        if (blockId.contains("crimson") || blockId.contains("nether_wart_block") || blockId.contains("weeping_vines")) {
            return "minecraft:crimson_forest";
        }
        if (blockId.contains("warped") || blockId.contains("twisting_vines")) {
            return "minecraft:warped_forest";
        }
        if (blockId.contains("soul_sand") || blockId.contains("soul_soil") || blockId.contains("bone_block")) {
            return "minecraft:soul_sand_valley";
        }
        if (blockId.contains("basalt") || blockId.contains("blackstone") || blockId.contains("magma_block")) {
            return "minecraft:basalt_deltas";
        }
        return "minecraft:nether_wastes";
    }

    private String resolveSingleplayerGeneratorBiomeId(String currentDim, int x, int y, int z) {
        if (this.minecraft == null) return null;
        var server = this.minecraft.getSingleplayerServer();
        if (server == null) return null;
        try {
            net.minecraft.resources.Identifier dimLoc =
                    net.minecraft.resources.Identifier.tryParse(currentDim);
            if (dimLoc == null) return null;
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> levelKey =
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION, dimLoc);
            net.minecraft.server.level.ServerLevel serverLevel = server.getLevel(levelKey);
            if (serverLevel == null) return null;
            var chunkSource = serverLevel.getChunkSource();
            var biomeSource = chunkSource.getGenerator().getBiomeSource();
            var randomState = chunkSource.randomState();
            if (biomeSource == null || randomState == null) return null;
            var resolver = biomeSource.createUncachedResolver(randomState);
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> holder =
                    serverLevel.getBiomeManager()
                            .withDifferentSource(resolver)
                            .getBiome(new net.minecraft.core.BlockPos(x, y, z));
            if (holder != null) {
                return holder.unwrapKey()
                        .map(k -> k.identifier().toString())
                        .orElse(null);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private String resolveCursorBiomeText(int x, int y, int z, boolean chunkLoaded) {
        MapManager mapManager = MapManager.getInstance();
        String currentDim = mapManager.getCurrentDimensionResourceId();
        long caveRev = CaveMode.getRevision();
        if (x == cachedBiomeX && y == cachedBiomeY && z == cachedBiomeZ
                && currentDim.equals(cachedBiomeDimension)
                && caveRev == cachedBiomeCaveRevision
                && cachedBiomeText != null) {
            return cachedBiomeText;
        }

        boolean isNether = "minecraft:the_nether".equals(currentDim);
        boolean isEnd = "minecraft:the_end".equals(currentDim);
        String resolvedId = null;

        if (chunkLoaded && this.minecraft != null && this.minecraft.level != null) {
            try {
                int chunkX = x >> 4;
                int chunkZ = z >> 4;
                int localX = x & 15;
                int localZ = z & 15;
                int sampleX = x;
                int sampleZ = z;
                if (localX < 2 && !this.minecraft.level.hasChunk(chunkX - 1, chunkZ)) {
                    sampleX = (chunkX << 4) + 2;
                } else if (localX > 13 && !this.minecraft.level.hasChunk(chunkX + 1, chunkZ)) {
                    sampleX = (chunkX << 4) + 13;
                }
                if (localZ < 2 && !this.minecraft.level.hasChunk(chunkX, chunkZ - 1)) {
                    sampleZ = (chunkZ << 4) + 2;
                } else if (localZ > 13 && !this.minecraft.level.hasChunk(chunkX, chunkZ + 1)) {
                    sampleZ = (chunkX << 4) + 13;
                }
                net.minecraft.core.BlockPos biomePos = new net.minecraft.core.BlockPos(sampleX, y, sampleZ);
                resolvedId = this.minecraft.level.getBiome(biomePos).unwrapKey()
                        .map(key -> key.identifier().toString())
                        .orElse(null);
                if ("minecraft:plains".equals(resolvedId)
                        || isInvalidPlainsForDimension(resolvedId, isNether, isEnd)) {
                    net.minecraft.world.level.chunk.LevelChunk chunk = this.minecraft.level.getChunk(chunkX, chunkZ);
                    if (chunk != null) {
                        String ownNoiseBiome = chunk.getNoiseBiome(sampleX >> 2, y >> 2, sampleZ >> 2)
                                .unwrapKey()
                                .map(key -> key.identifier().toString())
                                .orElse(null);
                        if (ownNoiseBiome != null && !ownNoiseBiome.isBlank()) {
                            resolvedId = ownNoiseBiome;
                        }
                    }
                    if (isInvalidPlainsForDimension(resolvedId, isNether, isEnd)) {
                        resolvedId = null;
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        boolean caveActive = CaveMode.isActive(this.minecraft);
        int targetLayer = caveActive
                ? (CaveMode.isFullView(this.minecraft) ? Integer.MIN_VALUE : CaveMode.getLayerY(this.minecraft))
                : Integer.MIN_VALUE;

        if ((resolvedId == null || resolvedId.isBlank()) && caveActive) {
            resolvedId = CaveMapManager.getInstance().getBiomeId(targetLayer, x, z);
            if (isInvalidPlainsForDimension(resolvedId, isNether, isEnd)) {
                resolvedId = null;
            }
        }

        if (resolvedId == null || resolvedId.isBlank() || "minecraft:plains".equals(resolvedId)) {
            String surfaceBiome = mapManager.getBiomeId(x, z);
            if (surfaceBiome != null && !surfaceBiome.isBlank()
                    && !isInvalidPlainsForDimension(surfaceBiome, isNether, isEnd)) {
                if (resolvedId == null || resolvedId.isBlank()
                        || !"minecraft:plains".equals(surfaceBiome)) {
                    resolvedId = surfaceBiome;
                }
            }
        }

        int regionX = x >> 9;
        int regionZ = z >> 9;
        boolean viewportMoving = isViewportInteracting(System.nanoTime());
        if (!viewportMoving && currentRenderScale >= 0.125f
                && (resolvedId == null || resolvedId.isBlank() || "minecraft:plains".equals(resolvedId))
                && mapManager.hasRegionFile(regionX, regionZ)
                && !mapManager.isRegionLoadedInCache(regionX, regionZ)) {
            mapManager.getRegion(regionX, regionZ, true);
            MapProcessor.getInstance().enqueueSurfaceLoad(regionX, regionZ, 120_000);
        }

        if ((resolvedId == null || resolvedId.isBlank() || "minecraft:plains".equals(resolvedId))
                && (caveActive || CaveMapManager.getInstance().getRegion(
                        Integer.MIN_VALUE, regionX, regionZ, false) != null)) {
            String caveBiome = CaveMapManager.getInstance().getBiomeId(
                    caveActive ? targetLayer : Integer.MIN_VALUE, x, z);
            if (caveBiome != null && !caveBiome.isBlank()
                    && !isInvalidPlainsForDimension(caveBiome, isNether, isEnd)) {
                if (resolvedId == null || resolvedId.isBlank()
                        || !"minecraft:plains".equals(caveBiome)) {
                    resolvedId = caveBiome;
                }
            }
        }

        // In Singleplayer / LAN host, query the integrated server's thread-safe noise
        // BiomeSource + BiomeManager zoom math only when stationary so panning at far
        // zoom never stalls on server noise evaluation.
        if (!viewportMoving
                && (resolvedId == null || resolvedId.isBlank() || "minecraft:plains".equals(resolvedId))) {
            String generatorBiome = resolveSingleplayerGeneratorBiomeId(currentDim, x, y, z);
            if (generatorBiome != null && !generatorBiome.isBlank()
                    && !isInvalidPlainsForDimension(generatorBiome, isNether, isEnd)) {
                resolvedId = generatorBiome;
            }
        }

        if (resolvedId == null || resolvedId.isBlank()) {
            if (isNether) {
                String blkId = cachedCursorInfo != null && cachedCursorX == x && cachedCursorZ == z
                        ? cachedCursorInfo.id : null;
                if (blkId == null || blkId.isBlank()) {
                    blkId = caveActive
                            ? CaveMapManager.getInstance().getBlockId(targetLayer, x, z)
                            : mapManager.getBlockId(x, z);
                }
                resolvedId = inferNetherBiomeFromBlockId(blkId);
            } else if (isEnd) {
                resolvedId = "minecraft:the_end";
            } else {
                return null;
            }
        }

        cachedBiomeText = formatBiomeDisplayName(resolvedId);
        cachedBiomeX = x;
        cachedBiomeY = y;
        cachedBiomeZ = z;
        cachedBiomeDimension = currentDim;
        cachedBiomeCaveRevision = caveRev;
        return cachedBiomeText;
    }

    private static String formatBlockDisplayName(String blockId) {
        if (blockId == null || blockId.isBlank()
                || "minecraft:air".equals(blockId)
                || "minecraft:cave_air".equals(blockId)
                || "minecraft:void_air".equals(blockId)) {
            return "";
        }
        try {
            net.minecraft.resources.Identifier loc = net.minecraft.resources.Identifier.tryParse(blockId);
            if (loc != null && net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(loc)) {
                net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(loc);
                if (block != null && block != net.minecraft.world.level.block.Blocks.AIR) {
                    String translated = block.getName().getString();
                    if (translated != null && !translated.isBlank()) {
                        return translated;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        int colon = blockId.indexOf(':');
        String path = colon >= 0 ? blockId.substring(colon + 1) : blockId;
        return prettifyId(path);
    }

    private static String formatBiomeDisplayName(String biomeId) {
        if (biomeId == null || biomeId.isBlank()) return null;
        int colon = biomeId.indexOf(':');
        String namespace = colon >= 0 ? biomeId.substring(0, colon) : "minecraft";
        String path = colon >= 0 ? biomeId.substring(colon + 1) : biomeId;
        String translationKey = "biome." + namespace + "." + path;
        try {
            net.minecraft.locale.Language language = net.minecraft.locale.Language.getInstance();
            if (language != null && language.has(translationKey)) {
                String translated = Component.translatable(translationKey).getString();
                if (translated != null && !translated.isBlank()
                        && !translated.equals(translationKey)) {
                    return translated;
                }
            }
        } catch (Throwable ignored) {
        }
        return prettifyId(path);
    }

    private static void appendGroupedInteger(StringBuilder builder, int value) {
        long absolute = Math.abs((long) value);
        if (value < 0) builder.append('-');
        long divisor = 1L;
        int digits = 1;
        while (divisor <= absolute / 10L) {
            divisor *= 10L;
            digits++;
        }
        while (divisor > 0L) {
            builder.append((char) ('0' + (absolute / divisor) % 10L));
            divisor /= 10L;
            digits--;
            if (digits > 0 && digits % 3 == 0) builder.append(',');
        }
    }

    private static boolean isInside(double mouseX, double mouseY,
            int left, int top, int right, int bottom) {
        return mouseX >= left && mouseX <= right
                && mouseY >= top && mouseY <= bottom;
    }

    /**
     * Looks up cursor height without requiring the Minecraft chunk to remain loaded.
     * Surface mode uses the persisted .smdat column, while layered/full cave modes
     * use the v3 vertical-run archive (with the older full-cave snapshot as a fallback).
     */
    private BlockInfo resolveCursorBlockInfo(int bx, int bz, boolean chunkLoaded, long now) {
        long revision = CaveMode.getRevision();
        if (cursorCacheValid && cachedCursorX == bx && cachedCursorZ == bz
                && cachedCursorRevision == revision
                && now - cachedCursorAtNanos < CURSOR_CACHE_NANOS) {
            return cachedCursorInfo;
        }
        BlockInfo resolved = null;
        if (MapManager.getInstance().isViewingLiveDimension()
                && chunkLoaded && this.minecraft != null && this.minecraft.level != null) {
            resolved = getBlockInfoAt(this.minecraft.level, bx, bz);
        }
        if (resolved == null || resolved.name.isEmpty()) {
            BlockInfo cached = getCachedBlockInfoAt(bx, bz);
            if (cached != null) resolved = cached;
        }
        cachedCursorX = bx;
        cachedCursorZ = bz;
        cachedCursorRevision = revision;
        cachedCursorAtNanos = now;
        cachedCursorInfo = resolved;
        cursorCacheValid = true;
        return resolved;
    }

    private BlockInfo getCachedBlockInfoAt(int bx, int bz) {
        if (this.minecraft == null || this.minecraft.level == null) return null;
        MapManager manager = MapManager.getInstance();
        CaveMapManager caveManager = CaveMapManager.getInstance();

        if (CaveMode.isActive(this.minecraft)) {
            boolean fullView = CaveMode.isFullView(this.minecraft);
            int targetLayer = fullView
                    ? Integer.MIN_VALUE
                    : CaveMode.getLayerY(this.minecraft);
            int cachedY = caveManager.getSurfaceY(targetLayer, bx, bz);
            if (cachedY != CaveMapManager.NO_SURFACE) {
                String blockId = caveManager.getBlockId(targetLayer, bx, bz);
                if (blockId == null || blockId.isBlank()) {
                    MapBlockData surfData = manager.getBlockData(bx, bz);
                    if (surfData != null && !surfData.isEmpty()
                            && (fullView || Math.abs(surfData.getReliefY() - cachedY) <= 2)) {
                        blockId = manager.getBlockId(bx, bz);
                    }
                }
                String blockName = formatBlockDisplayName(blockId);
                return new BlockInfo(blockName, blockId != null ? blockId : "", cachedY, Integer.MIN_VALUE, true);
            }
            if (fullView) {
                MapBlockData surfData = manager.getBlockData(bx, bz);
                if (surfData != null && !surfData.isEmpty()) {
                    String blockId = manager.getBlockId(bx, bz);
                    String blockName = formatBlockDisplayName(blockId);
                    int waterSurface = surfData.isFluid() && !surfData.isGlowing()
                            ? surfData.topY : Integer.MIN_VALUE;
                    return new BlockInfo(blockName, blockId != null ? blockId : "",
                            surfData.getReliefY(), waterSurface, true);
                }
            }
            return null;
        }

        MapBlockData data = manager.getBlockData(bx, bz);
        int regionX = bx >> 9;
        int regionZ = bz >> 9;
        if (data == null) {
            boolean viewportMoving = isViewportInteracting(System.nanoTime());
            if (!viewportMoving && currentRenderScale >= 0.125f
                    && manager.hasRegionFile(regionX, regionZ)
                    && !manager.isRegionLoadedInCache(regionX, regionZ)) {
                manager.getRegion(regionX, regionZ, true);
                MapProcessor.getInstance().enqueueSurfaceLoad(regionX, regionZ, 120_000);
            }
            // Only query Full Cave in Surface mode if the CaveRegion is already resident in memory;
            // otherwise CaveMapManager.getSurfaceY triggers background .mca Anvil slicing!
            if (caveManager.getRegion(Integer.MIN_VALUE, regionX, regionZ, false) != null) {
                int caveY = caveManager.getSurfaceY(Integer.MIN_VALUE, bx, bz);
                if (caveY != CaveMapManager.NO_SURFACE) {
                    String blockId = caveManager.getBlockId(Integer.MIN_VALUE, bx, bz);
                    String blockName = formatBlockDisplayName(blockId);
                    return new BlockInfo(blockName, blockId != null ? blockId : "", caveY, Integer.MIN_VALUE, true);
                }
            }
            return null;
        }
        if (data.isEmpty()) return null;
        String blockId = manager.getBlockId(bx, bz);
        if ((blockId == null || blockId.isBlank())
                && caveManager.getRegion(Integer.MIN_VALUE, regionX, regionZ, false) != null) {
            blockId = caveManager.getBlockId(Integer.MIN_VALUE, bx, bz);
        }
        String blockName = formatBlockDisplayName(blockId);
        int waterSurface = data.isFluid() && !data.isGlowing()
                ? data.topY : Integer.MIN_VALUE;
        return new BlockInfo(blockName, blockId != null ? blockId : "", data.getReliefY(), waterSurface, true);
    }

    private BlockInfo getBlockInfoAt(net.minecraft.world.level.Level level, int bx, int bz) {
        boolean scanFromWorldTop = CaveMode.shouldScanFromWorldTop(level);
        int minBuildHeight = level.getMinY();
        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos(bx, 0, bz);

        if (CaveMode.isActive(this.minecraft)) {
            boolean fullView = CaveMode.isFullView(this.minecraft);
            int layerY = fullView ? Integer.MIN_VALUE : CaveMode.getLayerY(this.minecraft);
            int maxBuildY = level.getMaxY();
            int ceilingCap = level.dimensionType().hasCeiling() ? 122 : maxBuildY;
            int scanMaximum = fullView
                    ? ceilingCap
                    : Math.min(ceilingCap, CaveMode.getScanMaximum(level, layerY));
            int scanMinimum = fullView
                    ? minBuildHeight
                    : CaveMode.getScanMinimum(level, layerY);

            int surfaceY = CaveMapManager.getInstance().getSurfaceY(layerY, bx, bz);
            if (surfaceY != CaveMapManager.NO_SURFACE) {
                // 1. If the cave floor is under water (surfaceY = floorY), walk up to find waterSurfaceY
                // so the cursor displays Y: floorY (waterSurfaceY) consistently.
                if (surfaceY + 1 <= scanMaximum) {
                    pos.setY(surfaceY + 1);
                    net.minecraft.world.level.block.state.BlockState aboveState = level.getBlockState(pos);
                    if (aboveState.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                        int waterTopY = surfaceY + 1;
                        for (int wy = surfaceY + 2; wy <= scanMaximum; wy++) {
                            pos.setY(wy);
                            if (!level.getBlockState(pos).getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                                break;
                            }
                            waterTopY = wy;
                        }
                        pos.setY(surfaceY);
                        net.minecraft.world.level.block.state.BlockState floorState = level.getBlockState(pos);
                        String floorId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                                .getKey(floorState.getBlock()).toString();
                        return new BlockInfo(floorState.getBlock().getName().getString(),
                                floorId, surfaceY, waterTopY);
                    }
                    // 2. Check surfaceY + 1 in case a thin floor block (carpet/rail) adjusted height by -1
                    if (!aboveState.isAir()
                            && !CaveMode.isCaveOpenOrInvisible(aboveState, MapConfig.displayFlowers)
                            && CaveMode.isShortFloorBlock(level, pos, aboveState)) {
                        String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                                .getKey(aboveState.getBlock()).toString();
                        return new BlockInfo(aboveState.getBlock().getName().getString(),
                                blockId, surfaceY);
                    }
                }
                pos.setY(surfaceY);
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    return createBlockInfo(level, pos, surfaceY);
                }
                String cachedBlockId = CaveMapManager.getInstance().getBlockId(layerY, bx, bz);
                return new BlockInfo(formatBlockDisplayName(cachedBlockId),
                        cachedBlockId != null ? cachedBlockId : "", surfaceY, Integer.MIN_VALUE, true);
            }

            // If this chunk is already live in CaveRegion at the current layer and getSurfaceY
            // is NO_SURFACE, this pixel is genuinely empty cave void — do not return a fake Y: -64.
            if (CaveMapManager.getInstance().isChunkLiveAtLayer(layerY, bx >> 4, bz >> 4)) {
                return null;
            }

            boolean isCaveDim = scanFromWorldTop;
            boolean hasCeiling = level.dimensionType().hasCeiling();
            boolean shouldEnterGround = fullView && !isCaveDim;
            boolean underair = fullView && !hasCeiling;
            boolean passedSolidRoof = fullView || isCaveDim;
            boolean showFlowers = MapConfig.displayFlowers;

            for (int y = scanMaximum; y >= scanMinimum; y--) {
                pos.setY(y);
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                if (state.isAir()) {
                    if (!shouldEnterGround) underair = true;
                    continue;
                }
                if (shouldEnterGround) {
                    if (CaveMode.isUndergroundSolidRoof(state)) {
                        shouldEnterGround = false;
                        underair = false;
                        passedSolidRoof = true;
                    }
                    continue;
                }
                if (!underair) {
                    if (CaveMode.isCaveOpenOrInvisible(state, showFlowers)) {
                        underair = true;
                        continue;
                    }
                    boolean isLavaFluid = state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)
                            || state.is(net.minecraft.world.level.block.Blocks.LAVA);
                    boolean isOpenSkyWater = !fullView && !passedSolidRoof
                            && state.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
                    if (isLavaFluid || isOpenSkyWater) {
                        underair = true;
                    } else if (!fullView && !passedSolidRoof
                            && !CaveMode.isUndergroundSolidRoof(state)) {
                        underair = true;
                    } else {
                        passedSolidRoof = true;
                        continue;
                    }
                }
                if (CaveMode.isCaveOpenOrInvisible(state, showFlowers)
                        || (fullView && state.is(net.minecraft.tags.BlockTags.LEAVES))) {
                    continue;
                }
                if (CaveMode.isShortFloorBlock(level, pos, state) && y > minBuildHeight) {
                    String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                            .getKey(state.getBlock()).toString();
                    return new BlockInfo(state.getBlock().getName().getString(),
                            blockId, y - 1);
                }
                return createBlockInfo(level, pos, y);
            }
            return null;
        }

        int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, bx, bz);
        if (surfaceY <= minBuildHeight && scanFromWorldTop) {
            surfaceY = level.getMaxY();
        }
        int highestY = minBuildHeight;
        for (int y = Math.min(level.getMaxY(), surfaceY); y >= minBuildHeight; y--) {
            pos.setY(y);
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
            if (!state.isAir()
                    && !CaveMode.isCaveOpenOrInvisible(state, MapConfig.displayFlowers)
                    && state.getMapColor(level, pos) != net.minecraft.world.level.material.MapColor.NONE) {
                highestY = y;
                break;
            }
        }

        if (highestY > minBuildHeight) {
            pos.setY(highestY);
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
            String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            return createBlockInfo(level, pos, highestY);
        }
        return new BlockInfo("", "", minBuildHeight);
    }

    private boolean isRenderableSliceState(net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos.MutableBlockPos pos,
            net.minecraft.world.level.block.state.BlockState state) {
        boolean openEmitter = state.getLightEmission() > 0
                && state.getFluidState().isEmpty()
                && (state.is(net.minecraft.world.level.block.Blocks.FIRE)
                        || state.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)
                        || caveStateClassifier.isCollisionEmpty(level, pos, state));
        boolean flower = MapConfig.displayFlowers
                && visualClassifier.info(state).flower();
        if (openEmitter || flower || !state.getFluidState().isEmpty()) return true;
        return !state.isAir()
                && !caveStateClassifier.isCollisionEmpty(level, pos, state)
                && state.getMapColor(level, pos) != net.minecraft.world.level.material.MapColor.NONE;
    }

    private BlockInfo getCaveBlockInfoAtY(net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos.MutableBlockPos pos, int openY) {
        pos.setY(openY);
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        boolean openEmitter = state.getLightEmission() > 0
                && state.getFluidState().isEmpty()
                && (state.is(net.minecraft.world.level.block.Blocks.FIRE)
                        || state.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)
                        || caveStateClassifier.isCollisionEmpty(level, pos, state));
        if (openEmitter) {
            String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getKey(state.getBlock()).toString();
            return new BlockInfo(state.getBlock().getName().getString(), blockId, openY);
        }
        if (state.getFluidState().isEmpty()) {
            boolean openSpace = state.isAir() || caveStateClassifier.isCollisionEmpty(level, pos, state);
            if (!openSpace || openY <= level.getMinY()) return null;
            pos.setY(openY - 1);
            state = level.getBlockState(pos);
            boolean floorOpen = state.isAir()
                    || (caveStateClassifier.isCollisionEmpty(level, pos, state) && state.getFluidState().isEmpty());
            if (floorOpen) return null;
        }
        String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(state.getBlock()).toString();
        return createBlockInfo(level, pos, pos.getY());
    }

    private BlockInfo createBlockInfo(net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos.MutableBlockPos pos, int visibleY) {
        pos.setY(visibleY);
        net.minecraft.world.level.block.state.BlockState visibleState = level.getBlockState(pos);
        if (visibleState.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
            int waterSurfaceY = visibleY;
            int bottomY = visibleY;
            net.minecraft.world.level.block.state.BlockState bottomState = visibleState;
            for (int y = visibleY - 1; y >= level.getMinY(); y--) {
                pos.setY(y);
                net.minecraft.world.level.block.state.BlockState candidate = level.getBlockState(pos);
                if (candidate.getFluidState().is(net.minecraft.tags.FluidTags.WATER)
                        || candidate.isAir()
                        || caveStateClassifier.isCollisionEmpty(level, pos, candidate)) continue;
                bottomY = y;
                bottomState = candidate;
                break;
            }
            String bottomId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getKey(bottomState.getBlock()).toString();
            return new BlockInfo(bottomState.getBlock().getName().getString(), bottomId,
                    bottomY, waterSurfaceY);
        }
        String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(visibleState.getBlock()).toString();
        return new BlockInfo(visibleState.getBlock().getName().getString(), blockId, visibleY);
    }

    private static String prettifyId(String id) {
        String[] parts = id.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private String getPopupMenuHeaderText() {
        if (clickedWaypoint != null) {
            String rawName = clickedWaypoint.name != null && !clickedWaypoint.name.isBlank()
                    ? clickedWaypoint.name : "Waypoint";
            return rawName.length() > 26 ? rawName.substring(0, 24) + "..." : rawName;
        }
        int bx = (int) Math.floor(popupWorldX);
        int bz = (int) Math.floor(popupWorldZ);
        return String.format(Locale.ROOT, "X: %d, Z: %d", bx, bz);
    }

    private int getPopupMenuHeight() {
        int rows = clickedWaypoint != null ? 5 : 3;
        return POPUP_HEADER_HEIGHT + 1 + rows * POPUP_ROW_HEIGHT;
    }

    private int getPopupMenuWidth() {
        Component optAddOrDelete = clickedWaypoint != null
                ? Component.translatable("gui.simplemap.popup.delete_waypoint")
                : Component.translatable("gui.simplemap.add_waypoint");
        Component optTeleport = Component.translatable("gui.simplemap.teleport_here");
        Component optShare = Component.translatable("gui.simplemap.popup.share_location");
        int maxW = Math.max(this.font.width(optAddOrDelete),
                Math.max(this.font.width(optTeleport), this.font.width(optShare)));
        if (clickedWaypoint != null) {
            Component optEdit = Component.translatable("gui.simplemap.edit_waypoint");
            boolean isCurrentlyFollowing = MapConfig.pinActive &&
                    Math.abs(MapConfig.pinWorldX - clickedWaypoint.x) < 0.01 &&
                    Math.abs(MapConfig.pinWorldZ - clickedWaypoint.z) < 0.01;
            Component optFollow = isCurrentlyFollowing
                    ? Component.translatable("gui.simplemap.popup.stop_following")
                    : Component.translatable("gui.simplemap.popup.follow_waypoint");
            maxW = Math.max(maxW, Math.max(this.font.width(optEdit), this.font.width(optFollow)));
        }
        int headerW = this.font.width(getPopupMenuHeaderText()) + 14;
        int desired = Math.max(116, Math.max(maxW + 18, headerW + 14));
        if (this.width > POPUP_SCREEN_MARGIN * 2) {
            return Math.min(desired, Math.max(100, this.width - POPUP_SCREEN_MARGIN * 2));
        }
        return desired;
    }

    private void clampPopupPosition(double anchorX, double anchorY) {
        int menuWidth = getPopupMenuWidth();
        int menuHeight = getPopupMenuHeight();
        double targetX = anchorX;
        double targetY = anchorY;
        if (this.width > 0 && targetX + menuWidth + POPUP_SCREEN_MARGIN > this.width) {
            targetX = anchorX - menuWidth - 2;
        }
        if (this.height > 0 && targetY + menuHeight + POPUP_SCREEN_MARGIN > this.height) {
            targetY = anchorY - menuHeight - 2;
        }
        double maxAllowedX = Math.max(POPUP_SCREEN_MARGIN, this.width - menuWidth - POPUP_SCREEN_MARGIN);
        double maxAllowedY = Math.max(POPUP_SCREEN_MARGIN, this.height - menuHeight - POPUP_SCREEN_MARGIN);
        popupX = Math.max(POPUP_SCREEN_MARGIN, Math.min(maxAllowedX, targetX));
        popupY = Math.max(POPUP_SCREEN_MARGIN, Math.min(maxAllowedY, targetY));
    }

    private void drawPopupMenuRow(GuiGraphicsExtractor guiGraphics, int x, int rowY, int width,
            Component label, boolean hovered, boolean enabled, int textColor, int accentColor) {
        if (hovered && enabled) {
            guiGraphics.fill(x + 1, rowY, x + width - 1, rowY + POPUP_ROW_HEIGHT - 1, 0xFF262B33);
            guiGraphics.fill(x + 1, rowY + 2, x + 3, rowY + POPUP_ROW_HEIGHT - 3, accentColor);
        }
        guiGraphics.text(this.font, label, x + 8, rowY + 6,
                enabled ? textColor : 0xFF6E737B, false);
    }

    private void drawPopupMenu(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((float)(0), (float)(0)); // Draw on top of everything

        int menuWidth = getPopupMenuWidth();
        int menuHeight = getPopupMenuHeight();
        double maxAllowedX = Math.max(POPUP_SCREEN_MARGIN, this.width - menuWidth - POPUP_SCREEN_MARGIN);
        double maxAllowedY = Math.max(POPUP_SCREEN_MARGIN, this.height - menuHeight - POPUP_SCREEN_MARGIN);
        int x = (int) Math.round(Math.max(POPUP_SCREEN_MARGIN, Math.min(maxAllowedX, popupX)));
        int y = (int) Math.round(Math.max(POPUP_SCREEN_MARGIN, Math.min(maxAllowedY, popupY)));
        popupX = x;
        popupY = y;

        // Subtle drop shadow + dark slate surface + crisp border
        guiGraphics.fill(x + 2, y + 2, x + menuWidth + 2, y + menuHeight + 2, 0x66000000);
        guiGraphics.fill(x, y, x + menuWidth, y + menuHeight, 0xF214171C);
        guiGraphics.fill(x + 1, y + 1, x + menuWidth - 1, y + POPUP_HEADER_HEIGHT, 0xFF1D222A);
        guiGraphics.fill(x + 1, y + POPUP_HEADER_HEIGHT, x + menuWidth - 1, y + POPUP_HEADER_HEIGHT + 1, 0xFF323842);
        guiGraphics.outline(x, y, menuWidth, menuHeight, 0xFF454C59);

        // Header badge + title (Waypoint name or World X/Z coordinates)
        int badgeColor = 0xFF00E5FF;
        if (clickedWaypoint != null) {
            badgeColor = clickedWaypoint.deathPoint ? 0xFFFF5252
                    : (clickedWaypoint.iconType == 0 ? 0xFFFF3D00
                    : (clickedWaypoint.iconType == 1 ? 0xFF00E676
                    : (clickedWaypoint.iconType == 2 ? 0xFF2979FF
                    : (clickedWaypoint.iconType == 3 ? 0xFFFFD600 : 0xFFFFD54F))));
        }
        guiGraphics.fill(x + 6, y + 6, x + 14, y + 14, 0xFF0B0D10);
        guiGraphics.fill(x + 7, y + 7, x + 13, y + 13, badgeColor);
        int headerColor = clickedWaypoint != null ? 0xFFFFE082 : 0xFFB0BEC5;
        guiGraphics.text(this.font, getPopupMenuHeaderText(), x + 18, y + 6, headerColor, false);

        int rowsStartY = y + POPUP_HEADER_HEIGHT + 1;
        boolean insideMenuX = mouseX >= x && mouseX <= x + menuWidth;
        boolean canTeleport = MapTeleportController.canTeleport(this.minecraft);

        if (clickedWaypoint != null) {
            boolean isCurrentlyFollowing = MapConfig.pinActive &&
                    Math.abs(MapConfig.pinWorldX - clickedWaypoint.x) < 0.01 &&
                    Math.abs(MapConfig.pinWorldZ - clickedWaypoint.z) < 0.01;
            Component followText = isCurrentlyFollowing
                    ? Component.translatable("gui.simplemap.popup.stop_following")
                    : Component.translatable("gui.simplemap.popup.follow_waypoint");

            Component[] labels = new Component[] {
                    Component.translatable("gui.simplemap.edit_waypoint"),
                    Component.translatable("gui.simplemap.popup.delete_waypoint"),
                    Component.translatable("gui.simplemap.teleport_here"),
                    Component.translatable("gui.simplemap.popup.share_location"),
                    followText
            };
            boolean[] enabled = new boolean[] { true, true, canTeleport, true, true };
            int[] textColors = new int[] { 0xFFFFFFFF, 0xFFFF8A80, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF };
            int[] accents = new int[] { 0xFF00E5FF, 0xFFFF5252, 0xFF00E5FF, 0xFF00E5FF, 0xFFFFD54F };

            for (int i = 0; i < labels.length; i++) {
                int rowY = rowsStartY + i * POPUP_ROW_HEIGHT;
                boolean hovered = insideMenuX && mouseY >= rowY && mouseY < rowY + POPUP_ROW_HEIGHT;
                drawPopupMenuRow(guiGraphics, x, rowY, menuWidth, labels[i],
                        hovered, enabled[i], textColors[i], accents[i]);
            }
        } else {
            Component[] labels = new Component[] {
                    Component.translatable("gui.simplemap.add_waypoint"),
                    Component.translatable("gui.simplemap.teleport_here"),
                    Component.translatable("gui.simplemap.popup.share_location")
            };
            boolean[] enabled = new boolean[] { true, canTeleport, true };
            for (int i = 0; i < labels.length; i++) {
                int rowY = rowsStartY + i * POPUP_ROW_HEIGHT;
                boolean hovered = insideMenuX && mouseY >= rowY && mouseY < rowY + POPUP_ROW_HEIGHT;
                drawPopupMenuRow(guiGraphics, x, rowY, menuWidth, labels[i],
                        hovered, enabled[i], 0xFFFFFFFF, 0xFF00E5FF);
            }
        }

        guiGraphics.pose().popMatrix();
    }

    private void markViewportInteraction() {
        long now = System.nanoTime();
        if (lastViewportInteractionNanos == 0L
                || now - lastViewportInteractionNanos
                        >= VIEWPORT_INTERACTION_SETTLE_NANOS) {
            MapViewportCoordinator.getInstance().beginFullscreenInteraction();
        }
        lastViewportInteractionNanos = now;
    }

    private boolean isViewportInteracting(long nowNanos) {
        return isDragging
                || (caveLayerSlider != null && caveLayerSlider.isDragging())
                || Math.abs(momentumX) > 0.5 || Math.abs(momentumZ) > 0.5
                || Math.abs(visualScale - scale) > scale * 0.001f
                || nowNanos - lastViewportInteractionNanos
                        < ACTIVE_PAN_SETTLE_NANOS;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(); int scanCode = event.keycode(); int modifiers = event.modifiers();
        if (caveYInputBox != null && caveYInputBox.isVisible() && caveYInputBox.isFocused()) {
            if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN
                    || keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER) {
                submitCaveYInputBox();
                return true;
            }
            if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
                unfocusCaveYInputBox();
                return true;
            }
            if (caveYInputBox.keyPressed(event)) {
                return true;
            }
            if (caveYInputBox.isFocused()) {
                return true;
            }
        }
        if (gotoCoordsInputBox != null && gotoCoordsInputBox.isVisible()) {
            if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN
                    || keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER) {
                submitGotoCoordsInputBox();
                return true;
            }
            if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
                closeGotoCoordsInputBox();
                return true;
            }
            if (gotoCoordsInputBox.keyPressed(event)) {
                return true;
            }
            if (gotoCoordsInputBox.isFocused()) {
                return true;
            }
        }
        if (SimpleMap.ClientModEvents.OPEN_MAP_KEY.matches(event)) {
            consumeBufferedClicks(SimpleMap.ClientModEvents.OPEN_MAP_KEY);
            this.onClose();
            return true;
        }
        if (SimpleMap.ClientModEvents.ZOOM_IN_KEY.matches(event)) {
            consumeBufferedClicks(SimpleMap.ClientModEvents.ZOOM_IN_KEY);
            markViewportInteraction();
            if (openAnimationStartNanos != 0L) {
                this.visualScale = this.currentRenderScale;
            }
            cancelMotionAndOpenAnimation();
            this.scale = Math.min(12.0f, this.scale * 1.15f);
            return true;
        }
        if (SimpleMap.ClientModEvents.ZOOM_OUT_KEY.matches(event)) {
            consumeBufferedClicks(SimpleMap.ClientModEvents.ZOOM_OUT_KEY);
            markViewportInteraction();
            if (openAnimationStartNanos != 0L) {
                this.visualScale = this.currentRenderScale;
            }
            cancelMotionAndOpenAnimation();
            this.scale = Math.max(getMinimumStableScale(), this.scale / 1.15f);
            return true;
        }
        if ((keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_HOME
                || SimpleMap.ClientModEvents.CENTER_FULL_MAP_KEY.matches(event))
                && this.minecraft != null && this.minecraft.player != null) {
            consumeBufferedClicks(SimpleMap.ClientModEvents.CENTER_FULL_MAP_KEY);
            markViewportInteraction();
            this.centerX = this.minecraft.player.getX();
            this.centerZ = this.minecraft.player.getZ();
            this.scale = DEFAULT_MAP_SCALE;
            rememberedScale = this.scale;
            this.visualScale = this.scale;
            this.currentRenderScale = this.scale;
            cancelMotionAndOpenAnimation();
            return true;
        }
        if (MapDebugRecorder.DEBUG_ENABLED) {
            if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_F8
                    || SimpleMap.ClientModEvents.TOGGLE_DEBUG_OVERLAY_KEY.matches(event)) {
                consumeBufferedClicks(SimpleMap.ClientModEvents.TOGGLE_DEBUG_OVERLAY_KEY);
                MapDebugRecorder.getInstance().toggleOverlay();
                return true;
            }
            if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_F9
                    || SimpleMap.ClientModEvents.TOGGLE_DEBUG_RECORDING_KEY.matches(event)) {
                consumeBufferedClicks(SimpleMap.ClientModEvents.TOGGLE_DEBUG_RECORDING_KEY);
                MapDebugRecorder.getInstance().toggleRecording(this.minecraft);
                return true;
            }
            if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_F10
                    || SimpleMap.ClientModEvents.MARK_DEBUG_ISSUE_KEY.matches(event)) {
                consumeBufferedClicks(SimpleMap.ClientModEvents.MARK_DEBUG_ISSUE_KEY);
                MapDebugRecorder.getInstance().markIssue(this.minecraft);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private static void consumeBufferedClicks(net.minecraft.client.KeyMapping mapping) {
        while (mapping.consumeClick()) {
            // Prevent Screen.keyPressed and the next client tick from handling the same key.
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
        // If map is not unlocked, only allow clicking screen widgets (buttons)
        if (!SimpleMap.isMapUnlocked(this.minecraft.player)) {
            return super.mouseClicked(event, doubleClick);
        }

        // Click the top-center cursor-coordinate panel to open Go-To coordinate input.
        if (button == 0 && (gotoCoordsInputBox == null || !gotoCoordsInputBox.isVisible())
                && topCoordPanelRight > topCoordPanelLeft
                && isInside(mouseX, mouseY,
                        topCoordPanelLeft, topCoordPanelTop, topCoordPanelRight, topCoordPanelBottom)) {
            openGotoCoordsInputBox();
            return true;
        }
        if (gotoCoordsInputBox != null && gotoCoordsInputBox.isVisible()
                && !isInside(mouseX, mouseY,
                        gotoCoordsInputBox.getX(), gotoCoordsInputBox.getY(),
                        gotoCoordsInputBox.getX() + gotoCoordsInputBox.getWidth(),
                        gotoCoordsInputBox.getY() + gotoCoordsInputBox.getHeight())) {
            closeGotoCoordsInputBox();
        }
        if (caveYInputBox != null && caveYInputBox.isVisible() && caveYInputBox.isFocused()
                && !isInside(mouseX, mouseY,
                        caveYInputBox.getX(), caveYInputBox.getY(),
                        caveYInputBox.getX() + caveYInputBox.getWidth(),
                        caveYInputBox.getY() + caveYInputBox.getHeight())) {
            unfocusCaveYInputBox();
        }

        // Click the visible bottom-right player-coordinate panel to share in chat.
        if (this.minecraft != null && this.minecraft.player != null) {
            String playerText = getPlayerCoordinatesText();
            int panelWidth = this.font.width(playerText) + 10;
            int panelRight = this.width - PLAYER_PANEL_MARGIN;
            int panelBottom = this.height - PLAYER_PANEL_MARGIN;
            int panelLeft = panelRight - panelWidth;
            int panelTop = panelBottom - PLAYER_PANEL_HEIGHT;
            if (isInside(mouseX, mouseY,
                    panelLeft, panelTop, panelRight, panelBottom)) {
                net.minecraft.world.entity.player.Player player = this.minecraft.player;
                String shareText = Component.translatable("gui.simplemap.share_player_coords",
                        (int) Math.floor(player.getX()),
                        (int) Math.floor(player.getY()),
                        (int) Math.floor(player.getZ())).getString();
                // Copy to Clipboard
                this.minecraft.keyboardHandler.setClipboard(shareText);
                player.sendSystemMessage(Component.translatable("gui.simplemap.coords_copied"));

                // Open Chat Screen with the pre-filled text
                this.onClose();
                this.minecraft.gui.setScreen(new net.minecraft.client.gui.screens.ChatScreen(shareText, false));
                return true;
            }
        }

        // 1. If context menu is open, handle its options first
        if (isPopupMenuOpen && button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            int menuWidth = getPopupMenuWidth();
            int menuHeight = getPopupMenuHeight();
            int rowsStartY = (int) Math.round(popupY) + POPUP_HEADER_HEIGHT + 1;
            if (mouseX >= popupX && mouseX <= popupX + menuWidth
                    && mouseY >= popupY && mouseY <= popupY + menuHeight) {
                if (mouseY < rowsStartY) {
                    // Clicked on the header bar: keep menu open without triggering a row
                    return true;
                }
                int rowIndex = (int) Math.floor((mouseY - rowsStartY) / (double) POPUP_ROW_HEIGHT);
                if (clickedWaypoint != null) {
                    Waypoint targetWp = clickedWaypoint;
                    if (rowIndex == 0) {
                        // Row 0: Edit Waypoint
                        isPopupMenuOpen = false;
                        clickedWaypoint = null;
                        if (this.minecraft != null) {
                            this.minecraft.gui.setScreen(new WaypointListScreen.AddWaypointScreen(
                                    this, targetWp));
                        }
                    } else if (rowIndex == 1) {
                        // Row 1: Delete Waypoint
                        WaypointManager.getInstance().removeWaypoint(targetWp);
                        clickedWaypoint = null;
                        isPopupMenuOpen = false;
                    } else if (rowIndex == 2) {
                        // Row 2: Teleport Here
                        if (MapTeleportController.canTeleport(this.minecraft)) {
                            isPopupMenuOpen = false;
                            clickedWaypoint = null;
                            int tpX = (int) Math.floor(targetWp.x);
                            int tpZ = (int) Math.floor(targetWp.z);
                            String targetDimension = targetWp.dimension != null && !targetWp.dimension.isBlank()
                                    ? targetWp.dimension
                                    : MapManager.getInstance().getCurrentDimensionResourceId();
                            int tpY = (int) Math.floor(targetWp.y);
                            if (MapTeleportController.teleport(this.minecraft, targetDimension,
                                    tpX, tpY, tpZ)) {
                                this.onClose();
                            }
                        }
                    } else if (rowIndex == 3) {
                        // Row 3: Share Location
                        String dimension = targetWp.dimension != null && !targetWp.dimension.isBlank()
                                ? targetWp.dimension
                                : MapManager.getInstance().getCurrentDimensionResourceId();
                        isPopupMenuOpen = false;
                        clickedWaypoint = null;
                        this.onClose();
                        WaypointManager.shareLocation(this.minecraft, targetWp.name,
                                targetWp.x, targetWp.y, targetWp.z, true, dimension);
                    } else if (rowIndex == 4) {
                        // Row 4: Follow / Stop Following Waypoint
                        boolean isCurrentlyFollowing = MapConfig.pinActive &&
                                Math.abs(MapConfig.pinWorldX - targetWp.x) < 0.01 &&
                                Math.abs(MapConfig.pinWorldZ - targetWp.z) < 0.01;
                        if (isCurrentlyFollowing) {
                            PinNavigation.clear();
                        } else {
                            PinNavigation.activate(targetWp.x, targetWp.z);
                        }
                        MapManager.getInstance().savePin();
                        isPopupMenuOpen = false;
                        clickedWaypoint = null;
                    }
                } else {
                    if (rowIndex == 0) {
                        // Row 0: Add Waypoint
                        isPopupMenuOpen = false;
                        if (this.minecraft != null) {
                            String waypointDimension = MapManager.getInstance()
                                    .getCurrentDimensionResourceId();
                            int waypointY = resolveTeleportTargetY(
                                    (int) Math.floor(popupWorldX),
                                    (int) Math.floor(popupWorldZ), waypointDimension);
                            this.minecraft.gui.setScreen(new WaypointListScreen.AddWaypointScreen(this,
                                    popupWorldX, waypointY, popupWorldZ,
                                    waypointDimension));
                        }
                    } else if (rowIndex == 1) {
                        // Row 1: Teleport Here
                        if (MapTeleportController.canTeleport(this.minecraft)) {
                            isPopupMenuOpen = false;
                            int tpX = (int) Math.floor(popupWorldX);
                            int tpZ = (int) Math.floor(popupWorldZ);
                            String targetDimension = MapManager.getInstance()
                                    .getCurrentDimensionResourceId();
                            int targetY = resolveTeleportTargetY(tpX, tpZ, targetDimension);
                            if (MapTeleportController.teleport(this.minecraft, targetDimension,
                                    tpX, targetY, tpZ)) {
                                this.onClose();
                            }
                        }
                    } else if (rowIndex == 2) {
                        // Row 2: Share Location
                        String dimension = MapManager.getInstance()
                                .getCurrentDimensionResourceId();
                        int targetY = resolveTeleportTargetY(
                                (int) Math.floor(popupWorldX),
                                (int) Math.floor(popupWorldZ), dimension);
                        isPopupMenuOpen = false;
                        this.onClose();
                        WaypointManager.shareLocation(this.minecraft, null,
                                popupWorldX, targetY, popupWorldZ, true, dimension);
                    }
                }
                return true;
            }
            isPopupMenuOpen = false; // Clicked outside the menu, close it
            clickedWaypoint = null;
        }

        // 2. Standard button clicks
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        // 3. Left click: start drag tracking; determine if it's a click vs drag on mouseReleased
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            if (openAnimationStartNanos != 0L) {
                this.visualScale = this.currentRenderScale;
            }
            cancelMotionAndOpenAnimation();
            this.isDragging = true;
            double subX = sampleSubpixelMouseX(mouseX);
            double subY = sampleSubpixelMouseY(mouseY);
            dragStartX = mouseX;
            dragStartZ = mouseY;
            dragAnchorMouseX = subX;
            dragAnchorMouseY = subY;
            dragCurrentMouseX = subX;
            dragCurrentMouseY = subY;
            dragAnchorCenterX = this.centerX;
            dragAnchorCenterZ = this.centerZ;
            dragLastSampleCenterX = this.centerX;
            dragLastSampleCenterZ = this.centerZ;
            lastDragSampleNanos = System.nanoTime();
            return true;
        }

        // 4. Right click opens the popup context menu (clamped inside screen bounds)
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) {
            popupWorldX = centerX + (mouseX - this.width / 2.0) / currentRenderScale;
            popupWorldZ = centerZ + (mouseY - this.height / 2.0) / currentRenderScale;

            // Check if right-clicking on an existing waypoint
            clickedWaypoint = null;
            java.util.List<WaypointManager.Waypoint> waypoints = WaypointManager.getInstance()
                    .getWaypointsForDimension(MapManager.getInstance()
                            .getCurrentDimensionResourceId());
            double nearestDistance = Double.POSITIVE_INFINITY;
            for (WaypointManager.Waypoint wp : waypoints) {
                double hoverRadius = MapRenderer.waypointHitRadiusWorld(
                        wp, currentRenderScale);
                if (Math.abs(popupWorldX - wp.x) <= hoverRadius && Math.abs(popupWorldZ - wp.z) <= hoverRadius) {
                    double distance = Math.hypot(popupWorldX - wp.x,
                            popupWorldZ - wp.z);
                    if (distance < nearestDistance) {
                        clickedWaypoint = wp;
                        nearestDistance = distance;
                    }
                }
            }

            clampPopupPosition(mouseX, mouseY);
            isPopupMenuOpen = true;
            return true;
        }

        return false;
    }

    private int resolveTeleportTargetY(int blockX, int blockZ, String targetDimension) {
        MapManager manager = MapManager.getInstance();
        int minimumY = this.minecraft != null && this.minecraft.level != null
                ? this.minecraft.level.getMinY() : -64;

        // Only query ClientLevel blocks when the map is displaying that live level.
        // A remotely viewed Nether/End/modded map must never sample Overworld chunks.
        if (manager.isViewingLiveDimension() && this.minecraft != null
                && this.minecraft.level != null) {
            net.minecraft.world.level.Level level = this.minecraft.level;
            if (level.hasChunk(blockX >> 4, blockZ >> 4)) {
                BlockInfo live = getBlockInfoAt(level, blockX, blockZ);
                if (live != null && live.y > level.getMinY()) {
                    int surfaceY = live.waterSurfaceY != Integer.MIN_VALUE
                            ? live.waterSurfaceY : live.y;
                    return surfaceY + 2;
                }
            }
        }

        BlockInfo cached = getCachedBlockInfoAt(blockX, blockZ);
        if (cached != null && cached.y > minimumY) {
            int surfaceY = cached.waterSurfaceY != Integer.MIN_VALUE
                    ? cached.waterSurfaceY : cached.y;
            return surfaceY + 2;
        }
        return MapTeleportController.defaultTargetY(targetDimension);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            if (this.caveLayerSlider != null && this.caveLayerSlider.isDragging()) {
                this.caveLayerSlider.onRelease(event);
                this.setDragging(false);
                return true;
            }
            boolean wasDragging = this.isDragging;
            this.isDragging = false;

            // If mouse barely moved, treat as a click -> place/remove pin
            double moved = Math.sqrt(Math.pow(mouseX - dragStartX, 2) + Math.pow(mouseY - dragStartZ, 2));
            if (wasDragging && moved < 4.0 && !isPopupMenuOpen
                    && com.velorise.simplemap.SimpleMap.isMapUnlocked(this.minecraft.player)) {
                momentumX = 0.0;
                momentumZ = 0.0;
                double clickWorldX = centerX + (mouseX - 1 - (this.width - 2) / 2.0) / currentRenderScale;
                double clickWorldZ = centerZ + (mouseY - 1 - (this.height - 2) / 2.0) / currentRenderScale;

                double snappedPinX = Math.floor(clickWorldX) + 0.5;
                double snappedPinZ = Math.floor(clickWorldZ) + 0.5;

                // Click same pin again within 4 screen-pixels -> remove
                if (MapConfig.pinActive && Math.abs(snappedPinX - MapConfig.pinWorldX) < 4.0 / currentRenderScale && Math.abs(snappedPinZ - MapConfig.pinWorldZ) < 4.0 / currentRenderScale) {
                    PinNavigation.clear();
                } else {
                    PinNavigation.activate(snappedPinX, snappedPinZ);
                }
                MapManager.getInstance().savePin();
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
        if (!com.velorise.simplemap.SimpleMap.isMapUnlocked(this.minecraft.player)) {
            return super.mouseDragged(event, dragX, dragY);
        }

        if (this.isDragging) {
            markViewportInteraction();
            this.zoomAnchorActive = false;
            this.dragCurrentMouseX = mouseX;
            this.dragCurrentMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private double sampleSubpixelMouseX(double fallback) {
        if (this.minecraft == null || this.minecraft.mouseHandler == null
                || this.minecraft.getWindow() == null) {
            return fallback;
        }
        int screenW = this.minecraft.getWindow().getScreenWidth();
        if (screenW <= 0) return fallback;
        return this.minecraft.mouseHandler.xpos()
                * (double) this.minecraft.getWindow().getGuiScaledWidth() / (double) screenW;
    }

    private double sampleSubpixelMouseY(double fallback) {
        if (this.minecraft == null || this.minecraft.mouseHandler == null
                || this.minecraft.getWindow() == null) {
            return fallback;
        }
        int screenH = this.minecraft.getWindow().getScreenHeight();
        if (screenH <= 0) return fallback;
        return this.minecraft.mouseHandler.ypos()
                * (double) this.minecraft.getWindow().getGuiScaledHeight() / (double) screenH;
    }

    private static double clampMomentum(double value) {
        return Math.max(-MAX_MOMENTUM_BLOCKS_PER_SECOND,
                Math.min(MAX_MOMENTUM_BLOCKS_PER_SECOND, value));
    }

    private float getMinimumStableScale() {
        double guiScale = this.minecraft == null || this.minecraft.getWindow() == null
                ? 1.0D : Math.max(1.0D, this.minecraft.getWindow().getGuiScale());
        return Math.max(0.015625f, (float) (0.0625D / guiScale));
    }

    private void clampScaleForCurrentMode() {
        float minimum = getMinimumStableScale();
        if (this.scale < minimum) {
            this.scale = minimum;
        }
        if (this.visualScale < minimum) {
            this.visualScale = minimum;
        }
        this.currentRenderScale = Math.max(this.currentRenderScale, minimum);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!com.velorise.simplemap.SimpleMap.isMapUnlocked(this.minecraft.player)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        markViewportInteraction();
        if (openAnimationStartNanos != 0L) {
            this.visualScale = this.currentRenderScale;
        }
        cancelMotionAndOpenAnimation();

        // Viewport centre (viewport starts at x=1, y=1)
        double vpCentreX = 1 + (this.width - 2) / 2.0;
        double vpCentreZ = 1 + (this.height - 2) / 2.0;

        // Anchor smooth zoom to the exact world position currently under the mouse cursor
        this.zoomAnchorScreenOffsetX = mouseX - vpCentreX;
        this.zoomAnchorScreenOffsetZ = mouseY - vpCentreZ;
        double anchorScale = Math.max(0.0001f, this.visualScale);
        this.zoomAnchorWorldX = this.centerX + this.zoomAnchorScreenOffsetX / anchorScale;
        this.zoomAnchorWorldZ = this.centerZ + this.zoomAnchorScreenOffsetZ / anchorScale;
        this.zoomAnchorActive = true;

        float minimumScale = getMinimumStableScale();
        this.scale = Math.max(minimumScale,
                Math.min(12.0f, this.scale * (float) Math.pow(1.15, scrollY)));
        if (this.isDragging) {
            double subX = sampleSubpixelMouseX(mouseX);
            double subY = sampleSubpixelMouseY(mouseY);
            this.dragAnchorMouseX = subX;
            this.dragAnchorMouseY = subY;
            this.dragCurrentMouseX = subX;
            this.dragCurrentMouseY = subY;
            this.dragAnchorCenterX = this.centerX;
            this.dragAnchorCenterZ = this.centerZ;
            this.dragLastSampleCenterX = this.centerX;
            this.dragLastSampleCenterZ = this.centerZ;
        }

        return true;
    }

    private final class CaveLayerSlider extends AbstractSliderButton {
        private static final double AUTO_ZONE = 0.045;
        private static final long LIVE_SCRUB_DEBOUNCE_NANOS = 60_000_000L;
        private int minimumY;
        private int maximumY;
        private boolean dragging;
        private boolean pendingLiveCommit;
        private long lastLiveCommitNanos;
        private boolean lastCommittedAuto = true;
        private int lastCommittedY = Integer.MIN_VALUE;

        private CaveLayerSlider(int x, int y, int width, int height,
                int minimumY, int maximumY, double initialValue) {
            super(x, y, width, height, Component.empty(), initialValue);
            this.minimumY = minimumY;
            this.maximumY = maximumY;
            updateMessage();
        }

        private void syncFromMode() {
            this.value = CaveMode.hasManualTopY(MapScreen.this.minecraft)
                    ? normalizeNumeric(CaveMode.getSelectedTopY(MapScreen.this.minecraft), minimumY, maximumY)
                    : 0.0;
            updateMessage();
        }

        private void syncBoundsFromDimension() {
            DimensionMapProfile profile = MapManager.getInstance()
                    .getCurrentDimensionProfile();
            if (profile != null) {
                this.minimumY = profile.minY();
                this.maximumY = profile.maxY();
            } else if (MapScreen.this.minecraft != null
                    && MapScreen.this.minecraft.level != null) {
                this.minimumY = MapScreen.this.minecraft.level.getMinY();
                this.maximumY = MapScreen.this.minecraft.level.getMaxY();
            }
            syncFromMode();
        }

        private boolean isDragging() {
            return dragging;
        }

        private int selectedY() {
            double numericValue = Math.max(0.0, Math.min(1.0,
                    (this.value - AUTO_ZONE) / (1.0 - AUTO_ZONE)));
            return minimumY + (int) Math.round(numericValue * (maximumY - minimumY));
        }

        private boolean isAutoSelection() {
            return this.value <= AUTO_ZONE;
        }

        @Override
        protected void updateMessage() {
            boolean fullCave = CaveMode.getCaveType(Minecraft.getInstance()) == CaveMode.CaveType.FULL;
            Component label;
            if (fullCave) {
                if (isAutoSelection()) {
                    label = Component.translatable(dragging
                            ? "gui.simplemap.cave_slider.full_auto_drag"
                            : "gui.simplemap.cave_slider.full_auto");
                } else {
                    label = Component.translatable(dragging
                            ? "gui.simplemap.cave_slider.full_y_drag"
                            : "gui.simplemap.cave_slider.full_y", selectedY());
                }
            } else {
                if (isAutoSelection()) {
                    label = Component.translatable(dragging
                            ? "gui.simplemap.cave_slider.layer_auto_drag"
                            : "gui.simplemap.cave_slider.layer_auto");
                } else {
                    label = Component.translatable(dragging
                            ? "gui.simplemap.cave_slider.layer_y_drag"
                            : "gui.simplemap.cave_slider.layer_y", selectedY());
                }
            }
            setMessage(label);
        }

        @Override
        protected void applyValue() {
            if (dragging) {
                pendingLiveCommit = true;
                flushPendingLiveScrub(System.nanoTime());
                return;
            }
            commitLayer(true);
        }

        private void flushPendingLiveScrub(long nowNanos) {
            if (!pendingLiveCommit) return;
            if (lastLiveCommitNanos != 0L
                    && nowNanos - lastLiveCommitNanos < LIVE_SCRUB_DEBOUNCE_NANOS) {
                return;
            }
            pendingLiveCommit = false;
            boolean autoNow = isAutoSelection();
            int yNow = autoNow ? Integer.MIN_VALUE : selectedY();
            if (autoNow == lastCommittedAuto && yNow == lastCommittedY) {
                return;
            }
            lastLiveCommitNanos = nowNanos;
            commitLayer(false);
        }

        @Override
        public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            MapScreen.this.markViewportInteraction();
            dragging = true;
            super.onClick(event, doubleClick);
        }

        @Override
        protected void onDrag(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
            MapScreen.this.markViewportInteraction();
            dragging = true;
            super.onDrag(event, dragX, dragY);
        }

        void finishDrag() {
            boolean shouldCommit = dragging;
            dragging = false;
            pendingLiveCommit = false;
            if (shouldCommit) commitLayer(true);
        }

        @Override
        public void onRelease(net.minecraft.client.input.MouseButtonEvent event) {
            finishDrag();
            super.onRelease(event);
        }

        private void commitLayer(boolean persist) {
            MapScreen.this.lastViewportInteractionNanos = 0L;
            MapPerformanceGovernor.getInstance().setFullscreenState(true, false);
            boolean autoNow = isAutoSelection();
            int yNow = autoNow ? Integer.MIN_VALUE : selectedY();
            boolean changed = autoNow != lastCommittedAuto || yNow != lastCommittedY;
            lastCommittedAuto = autoNow;
            lastCommittedY = yNow;
            if (autoNow) {
                this.value = 0.0;
                CaveMode.setAutoTopY(MapScreen.this.minecraft, persist);
            } else {
                CaveMode.setManualLayer(MapScreen.this.minecraft, yNow, persist);
            }
            if (changed || persist) {
                ChunkScanner.getInstance().requestImmediateCaveLayerRefresh(MapScreen.this.minecraft);
                FullscreenMapFramebufferRenderer.getInstance().invalidate();
                FullscreenMapFramebufferRenderer.getInstance().resetFailureState();
                MapRenderer.getInstance().clearFullscreenFallback();
            }
            updateMessage();
            if (MapScreen.this.caveLayerModeButton != null) {
                MapScreen.this.updateCaveTooltip();
            }
        }

        private static double normalizeNumeric(int layerY, int minimumY, int maximumY) {
            if (maximumY <= minimumY) return 0.0;
            int clamped = Math.max(minimumY, Math.min(maximumY, layerY));
            double numeric = (double) (clamped - minimumY) / (maximumY - minimumY);
            return AUTO_ZONE + numeric * (1.0 - AUTO_ZONE);
        }
    }

    @Override
    public void onClose() {
        if (this.caveLayerSlider != null && this.caveLayerSlider.isDragging()) {
            this.caveLayerSlider.finishDrag();
        }
        if (Float.isFinite(this.scale) && this.scale > 0.0f) {
            rememberedScale = this.scale;
        }
        // Retain the selected dimension in MapManager when closing MapScreen so
        // the Minimap can display the chosen dimension with proportional coordinate scaling
        // (1:8 Nether <-> Overworld), cross-dimension scaling.
        MapViewportCoordinator.getInstance().closeFullscreen();
        FullscreenMapFramebufferRenderer.getInstance().destroy();
        super.onClose();
    }

    @Override
    public void removed() {
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause game in singleplayer
    }


}