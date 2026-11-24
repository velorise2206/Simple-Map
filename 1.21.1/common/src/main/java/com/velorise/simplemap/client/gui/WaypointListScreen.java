package com.velorise.simplemap.client.gui;
import com.velorise.simplemap.client.cave.CaveTextureManager.CaveMapManager;
import com.velorise.simplemap.client.color.SurfaceColorizer.MapVisualClassifier;
import com.velorise.simplemap.client.MapConfig;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.client.gui.GuiGraphics;
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

public class WaypointListScreen extends Screen {
private static final String DIM_OVERWORLD = "minecraft:overworld";
private static final String DIM_NETHER = "minecraft:the_nether";
private static final String DIM_END = "minecraft:the_end";
private static final int ROW_HEIGHT = 38;

private final Screen parent;
private final String previousViewDimension;
private final boolean previousViewWasLive;
private boolean previousViewRestored;
private final List<WaypointManager.Waypoint> filteredWaypoints = new ArrayList<>();
private final List<Button> tabButtons = new ArrayList<>();
private int selectedTab;
private double scrollAmount;
private EditBox searchBox;
private WaypointManager.Waypoint pendingDelete;
private long pendingDeleteUntilNanos;
private WaypointManager.Waypoint focusedWaypoint;

private int panelLeft;
private int panelTop;
private int panelWidth;
private int panelHeight;
private double previewCenterX;
private double previewCenterZ;
private String previewDimension;
private float previewScale = 0.55f;
private boolean previewInitialized;
private boolean draggingMap;
private double dragStartX;
private double dragStartY;

public WaypointListScreen(Screen parent) {
    super(Component.translatable("gui.simplemap.waypoint_manager_title"));
    this.parent = parent;
    this.previousViewDimension = MapManager.getInstance().getCurrentDimensionResourceId();
    this.previousViewWasLive = MapManager.getInstance().isViewingLiveDimension();
    this.previewDimension = this.previousViewDimension;
}

@Override
protected void init() {
    tabButtons.clear();

    if (!previewInitialized) {
        previewInitialized = true;
        if (parent instanceof MapScreen mapScreen && mapScreen.scale > 0.05f) {
            previewScale = mapScreen.scale;
            previewCenterX = mapScreen.centerX;
            previewCenterZ = mapScreen.centerZ;
        } else if (minecraft != null && minecraft.player != null) {
            previewScale = Math.max(0.35f, Math.min(0.85f, (this.width - 2) / 1200.0f));
            previewCenterX = minecraft.player.getX();
            previewCenterZ = minecraft.player.getZ();
        }
    }

    // Sleek, translucent left-docked glass panel matching AddWaypointScreen
    panelWidth = Math.min(256, Math.max(220, this.width - 24));
    panelLeft = Math.max(8, Math.min(14, this.width / 28));
    panelTop = Math.max(8, Math.min(14, this.height / 24));
    panelHeight = Math.max(184, this.height - panelTop * 2);

    int leftX = panelLeft + 8;
    int fieldWidth = panelWidth - 16;

    String[] tabs = {
        Component.translatable("gui.simplemap.waypoint_tab.all").getString(),
        Component.translatable("gui.simplemap.waypoint_tab.overworld").getString(),
        Component.translatable("gui.simplemap.waypoint_tab.nether").getString(),
        Component.translatable("gui.simplemap.waypoint_tab.end").getString(),
        Component.translatable("gui.simplemap.waypoint_tab.modded").getString()
    };
    int tabGap = 2;
    int tabWidth = Math.max(28, (fieldWidth - tabGap * (tabs.length - 1)) / tabs.length);
    int tabY = panelTop + 21;
    for (int index = 0; index < tabs.length; index++) {
        final int tab = index;
        Button button = Button.builder(Component.literal(tabs[index]), b -> setTab(tab))
                .bounds(leftX + index * (tabWidth + tabGap), tabY, tabWidth, 16).build();
        tabButtons.add(button);
        addRenderableWidget(button);
    }

    searchBox = new EditBox(font, leftX, panelTop + 40, fieldWidth, 16,
            Component.translatable("gui.simplemap.search_waypoints"));
    searchBox.setHint(Component.literal("Search name, coords or dim..."));
    searchBox.setResponder(value -> refreshWaypoints());
    addRenderableWidget(searchBox);

    int halfButton = (fieldWidth - 4) / 2;
    int bottomY = panelTop + panelHeight - 23;
    addRenderableWidget(Button.builder(Component.translatable("gui.simplemap.add_waypoint"), button -> openAddScreen())
            .bounds(leftX, bottomY, halfButton, 18).build());
    addRenderableWidget(Button.builder(Component.translatable("gui.simplemap.done"), button -> onClose())
            .bounds(leftX + halfButton + 4, bottomY, halfButton, 18).build());

    refreshWaypoints();
    updateTabLabels();
}

private void openAddScreen() {
    if (minecraft == null || minecraft.player == null) return;
    double targetX = focusedWaypoint != null ? focusedWaypoint.x : minecraft.player.getX();
    double targetY = focusedWaypoint != null && focusedWaypoint.hasY
            ? focusedWaypoint.y : minecraft.player.getY();
    double targetZ = focusedWaypoint != null ? focusedWaypoint.z : minecraft.player.getZ();
    String targetDim = focusedWaypoint != null && focusedWaypoint.dimension != null
            ? focusedWaypoint.dimension
            : MapManager.getInstance().getLiveDimensionResourceId();
    minecraft.setScreen(new AddWaypointScreen(this, targetX, targetY, targetZ, targetDim));
}

private void setTab(int tab) {
    selectedTab = tab;
    scrollAmount = 0;
    pendingDelete = null;
    refreshWaypoints();
    updateTabLabels();
}

private void updateTabLabels() {
    String[] tabs = {
        Component.translatable("gui.simplemap.waypoint_tab.all").getString(),
        Component.translatable("gui.simplemap.waypoint_tab.overworld").getString(),
        Component.translatable("gui.simplemap.waypoint_tab.nether").getString(),
        Component.translatable("gui.simplemap.waypoint_tab.end").getString(),
        Component.translatable("gui.simplemap.waypoint_tab.modded").getString()
    };
    for (int index = 0; index < tabButtons.size(); index++) {
        tabButtons.get(index).setMessage(Component.literal(
                index == selectedTab ? "§b" + tabs[index] : tabs[index]));
    }
}

private void refreshWaypoints() {
    filteredWaypoints.clear();
    String filter = searchBox == null ? ""
            : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
    for (WaypointManager.Waypoint waypoint : WaypointManager.getInstance().getAllWaypoints()) {
        if (!matchesTab(waypoint)) continue;
        String y = waypoint.hasY ? Integer.toString((int) Math.floor(waypoint.y)) : "";
        String searchable = (waypoint.name + " " + (int) Math.floor(waypoint.x) + " "
                + y + " " + (int) Math.floor(waypoint.z) + " " + waypoint.dimension)
                .toLowerCase(Locale.ROOT);
        if (filter.isEmpty() || searchable.contains(filter)) filteredWaypoints.add(waypoint);
    }
    filteredWaypoints.sort(Comparator
            .comparing((WaypointManager.Waypoint waypoint) -> waypoint.dimension)
            .thenComparing(waypoint -> waypoint.name.toLowerCase(Locale.ROOT)));
    if (focusedWaypoint != null && !filteredWaypoints.contains(focusedWaypoint)) {
        focusedWaypoint = filteredWaypoints.isEmpty() ? null : filteredWaypoints.get(0);
    } else if (focusedWaypoint == null && !filteredWaypoints.isEmpty()) {
        focusedWaypoint = filteredWaypoints.get(0);
    }
    clampScroll();
}

private boolean matchesTab(WaypointManager.Waypoint waypoint) {
    return switch (selectedTab) {
        case 1 -> isVanillaDimension(waypoint.dimension, "overworld");
        case 2 -> isVanillaDimension(waypoint.dimension, "the_nether");
        case 3 -> isVanillaDimension(waypoint.dimension, "the_end");
        case 4 -> !isVanillaDimension(waypoint.dimension, "overworld")
                && !isVanillaDimension(waypoint.dimension, "the_nether")
                && !isVanillaDimension(waypoint.dimension, "the_end");
        default -> true;
    };
}

private int listTop() {
    return panelTop + 60;
}

private int listBottom() {
    return panelTop + panelHeight - 28;
}

private int listWidth() {
    return panelWidth - 16;
}

private int listLeft() {
    return panelLeft + 8;
}

private boolean isInsidePanel(double mouseX, double mouseY) {
    return mouseX >= panelLeft && mouseX <= panelLeft + panelWidth
            && mouseY >= panelTop && mouseY <= panelTop + panelHeight;
}

private double getMapAreaCenterX() {
    int rightAreaLeft = panelLeft + panelWidth;
    int rightAreaWidth = Math.max(40, this.width - rightAreaLeft);
    return rightAreaLeft + rightAreaWidth * 0.5;
}

private void focusWaypointOnPreview(WaypointManager.Waypoint waypoint) {
    if (waypoint == null) return;
    focusedWaypoint = waypoint;
    previewCenterX = waypoint.x;
    previewCenterZ = waypoint.z;
    String targetDim = MapManager.getInstance().resolveDimensionResourceId(waypoint.dimension);
    previewDimension = targetDim;
    ensurePreviewDimensionViewed(targetDim);
    if (parent instanceof MapScreen mapScreen) {
        mapScreen.centerX = waypoint.x;
        mapScreen.centerZ = waypoint.z;
    }
}

private void ensurePreviewDimensionViewed(String dimension) {
    if (minecraft == null || dimension == null || dimension.isBlank()) return;
    MapManager manager = MapManager.getInstance();
    String target = manager.resolveDimensionResourceId(dimension);
    if (target.equals(manager.getCurrentDimensionResourceId())) return;
    if (target.equals(manager.getLiveDimensionResourceId())) {
        manager.returnToLiveDimension(minecraft);
    } else {
        manager.switchToDimension(target);
    }
}

private void restorePreviousMapView() {
    if (previousViewRestored || minecraft == null) return;
    previousViewRestored = true;
    MapManager manager = MapManager.getInstance();
    if (previousViewWasLive) manager.returnToLiveDimension(minecraft);
    else if (previousViewDimension != null) manager.switchToDimension(previousViewDimension);
}

private int maximumScroll() {
    return Math.max(0, filteredWaypoints.size() * ROW_HEIGHT
            - (listBottom() - listTop() - 4));
}

private void clampScroll() {
    scrollAmount = Math.max(0, Math.min(maximumScroll(), scrollAmount));
}

@Override
public boolean mouseScrolled(double mouseX, double mouseY, double scrollX,
        double scrollY) {
    if (isInsidePanel(mouseX, mouseY)) {
        if (mouseY >= listTop() && mouseY <= listBottom()) {
            scrollAmount -= scrollY * 22.0;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    previewScale = Math.max(0.0625f,
            Math.min(8.0f, previewScale + (float) scrollY * 0.15f * previewScale));
    return true;
}

@Override
public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (super.mouseClicked(mouseX, mouseY, button)) return true;
    if (!isInsidePanel(mouseX, mouseY)) {
        if (button == 0) {
            draggingMap = true;
            dragStartX = mouseX;
            dragStartY = mouseY;
            return true;
        } else if (button == 1 && minecraft != null) {
            float backgroundScale = Math.max(0.0625f, previewScale);
            double anchorScreenX = getMapAreaCenterX();
            double anchorScreenY = this.height * 0.5;
            double mapCenterX = previewCenterX - (anchorScreenX - this.width * 0.5) / backgroundScale;
            double mapCenterZ = previewCenterZ - (anchorScreenY - this.height * 0.5) / backgroundScale;
            double clickedX = Math.floor(mapCenterX + (mouseX - this.width * 0.5) / backgroundScale) + 0.5;
            double clickedZ = Math.floor(mapCenterZ + (mouseY - this.height * 0.5) / backgroundScale) + 0.5;
            String dim = MapManager.getInstance().getCurrentDimensionResourceId();
            minecraft.setScreen(new AddWaypointScreen(this, clickedX, clickedZ, dim));
            return true;
        }
        return false;
    }
    if (button != 0 || mouseY < listTop() || mouseY > listBottom()) return false;

    int left = listLeft();
    int width = listWidth();
    if (mouseX < left || mouseX > left + width) return false;

    int startY = listTop() + 2 - (int) scrollAmount;
    for (int index = 0; index < filteredWaypoints.size(); index++) {
        int rowY = startY + index * ROW_HEIGHT;
        if (rowY + ROW_HEIGHT < listTop() || rowY > listBottom()) continue;
        if (mouseY < rowY || mouseY >= rowY + ROW_HEIGHT) continue;

        WaypointManager.Waypoint waypoint = filteredWaypoints.get(index);
        RowActions actions = rowActions(rowY);
        if (mouseY >= actions.buttonY && mouseY <= actions.buttonY + actions.buttonHeight) {
            if (inside(mouseX, actions.pinX, actions.pinWidth)) {
                toggleTracked(waypoint);
                return true;
            }
            if (inside(mouseX, actions.editX, actions.editWidth)) {
                if (minecraft != null) minecraft.setScreen(new AddWaypointScreen(this, waypoint));
                return true;
            }
            if (inside(mouseX, actions.shareX, actions.shareWidth)) {
                share(waypoint);
                return true;
            }
            if (inside(mouseX, actions.teleportX, actions.teleportWidth)) {
                teleport(waypoint);
                return true;
            }
            if (inside(mouseX, actions.deleteX, actions.deleteWidth)) {
                confirmDelete(waypoint);
                return true;
            }
        }
        focusWaypointOnPreview(waypoint);
        return true;
    }
    return false;
}

@Override
public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
    if (draggingMap && button == 0) {
        previewCenterX -= dragX / Math.max(0.01f, previewScale);
        previewCenterZ -= dragY / Math.max(0.01f, previewScale);
        return true;
    }
    return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
}

@Override
public boolean mouseReleased(double mouseX, double mouseY, int button) {
    if (draggingMap && button == 0) {
        draggingMap = false;
        return true;
    }
    return super.mouseReleased(mouseX, mouseY, button);
}

private static boolean inside(double mouseX, int x, int width) {
    return mouseX >= x && mouseX <= x + width;
}

private RowActions rowActions(int rowY) {
    int right = listLeft() + listWidth() - 5;
    int gap = 2;
    int buttonHeight = 13;
    int buttonY = rowY + ROW_HEIGHT - buttonHeight - 3;
    int deleteWidth = 28;
    int teleportWidth = 18;
    int shareWidth = 26;
    int editWidth = 22;
    int pinWidth = 24;
    int deleteX = right - deleteWidth;
    int teleportX = deleteX - gap - teleportWidth;
    int shareX = teleportX - gap - shareWidth;
    int editX = shareX - gap - editWidth;
    int pinX = editX - gap - pinWidth;
    return new RowActions(pinX, editX, shareX, teleportX, deleteX,
            pinWidth, editWidth, shareWidth, teleportWidth, deleteWidth,
            buttonY, buttonHeight);
}

private void toggleTracked(WaypointManager.Waypoint waypoint) {
    boolean tracked = isTracked(waypoint);
    if (tracked) {
        PinNavigation.clear();
    } else {
        PinNavigation.activate(waypoint.x, waypoint.z);
    }
    MapManager.getInstance().savePin();
    if (!tracked) openOnMap(waypoint);
}

private void openOnMap(WaypointManager.Waypoint waypoint) {
    if (minecraft == null) return;
    previousViewRestored = true;
    MapScreen map = parent instanceof MapScreen parentMap
            ? parentMap : new MapScreen();
    minecraft.setScreen(map);
    map.focusOnWaypoint(waypoint);
}

private void share(WaypointManager.Waypoint waypoint) {
    restorePreviousMapView();
    if (parent instanceof MapScreen map) {
        map.onClose();
    } else if (minecraft != null
            && !MapManager.getInstance().isViewingLiveDimension()) {
        MapManager.getInstance().returnToLiveDimension(minecraft);
    }
    WaypointManager.shareLocation(minecraft, waypoint.name,
            waypoint.x, waypoint.y, waypoint.z, waypoint.hasY,
            waypoint.dimension);
}

private boolean isTracked(WaypointManager.Waypoint waypoint) {
    return MapConfig.pinActive
            && Math.abs(MapConfig.pinWorldX - waypoint.x) < 0.01
            && Math.abs(MapConfig.pinWorldZ - waypoint.z) < 0.01;
}

private void teleport(WaypointManager.Waypoint waypoint) {
    if (minecraft == null) return;
    String targetDimension = MapManager.getInstance()
            .resolveDimensionResourceId(waypoint.dimension);
    int targetY = waypoint.hasY
            ? (int) Math.floor(waypoint.y)
            : MapTeleportController.defaultTargetY(targetDimension);
    if (MapTeleportController.teleport(minecraft, targetDimension,
            (int) Math.floor(waypoint.x), targetY, (int) Math.floor(waypoint.z))) {
        previousViewRestored = true;
        MapManager.getInstance().returnToLiveDimension(minecraft);
        MapViewportCoordinator.getInstance().closeFullscreen();
        minecraft.setScreen(null);
    }
}

private void confirmDelete(WaypointManager.Waypoint waypoint) {
    long now = System.nanoTime();
    if (pendingDelete == waypoint && now <= pendingDeleteUntilNanos) {
        WaypointManager.getInstance().removeWaypoint(waypoint);
        pendingDelete = null;
        refreshWaypoints();
        return;
    }
    pendingDelete = waypoint;
    pendingDeleteUntilNanos = now + 3_000_000_000L;
}

@Override
public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY,
        float partialTick) {
}

@Override
public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    if (pendingDelete != null && System.nanoTime() > pendingDeleteUntilNanos) {
        pendingDelete = null;
    }

    float backgroundScale = Math.max(0.0625f, previewScale);
    double anchorScreenX = getMapAreaCenterX();
    double anchorScreenY = height * 0.5;
    double mapCenterX = previewCenterX - (anchorScreenX - width * 0.5) / backgroundScale;
    double mapCenterZ = previewCenterZ - (anchorScreenY - height * 0.5) / backgroundScale;
    double mouseWorldX = mapCenterX + (mouseX - width * 0.5) / backgroundScale;
    double mouseWorldZ = mapCenterZ + (mouseY - height * 0.5) / backgroundScale;

    // 1. Render unobstructed live map + all waypoints in the background
    MapRenderer.getInstance().drawMap(guiGraphics, 0, 0, width, height,
            mapCenterX, mapCenterZ, backgroundScale,
            MapManager.getInstance().isViewingLiveDimension(), false, false,
            mouseWorldX, mouseWorldZ, partialTick, false);

    RenderSystem.disableDepthTest();
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    // 2. Highlight currently focused waypoint on the map
    if (focusedWaypoint != null) {
        int markerScreenX = (int) Math.round(width * 0.5 + (focusedWaypoint.x - mapCenterX) * backgroundScale);
        int markerScreenY = (int) Math.round(height * 0.5 + (focusedWaypoint.z - mapCenterZ) * backgroundScale);
        guiGraphics.renderOutline(markerScreenX - 11, markerScreenY - 11, 22, 22, 0xD000E5FF);
        guiGraphics.renderOutline(markerScreenX - 12, markerScreenY - 12, 24, 24, 0x5500E5FF);
    }

    // 3. Subtle interaction hint at the top of the open map area
    String mapHint = "Click row: focus · Drag map: pan · Scroll: zoom · Right-click map: +waypoint";
    int hintW = font.width(mapHint);
    int hintX = Math.max(panelLeft + panelWidth + 8, (int) Math.round(anchorScreenX) - hintW / 2);
    if (hintX + hintW + 6 < width) {
        guiGraphics.fill(hintX - 5, 6, hintX + hintW + 5, 19, 0x680C0E12);
        guiGraphics.drawString(font, mapHint, hintX, 9, 0xFFD0D8E0, false);
    }

    // 4. Sleek translucent glass panel on the left (matching AddWaypointScreen)
    guiGraphics.fill(panelLeft, panelTop, panelLeft + panelWidth,
            panelTop + panelHeight, 0x7A101318);
    guiGraphics.fill(panelLeft, panelTop, panelLeft + panelWidth,
            panelTop + 18, 0x88181D24);
    guiGraphics.renderOutline(panelLeft, panelTop, panelWidth, panelHeight, 0x984A5462);

    // Header with title and waypoint count badge
    guiGraphics.drawString(font, title, panelLeft + 8, panelTop + 5, 0xFFFFFFFF, false);
    String countBadge = Integer.toString(filteredWaypoints.size());
    int countW = font.width(countBadge);
    guiGraphics.fill(panelLeft + panelWidth - countW - 12, panelTop + 3,
            panelLeft + panelWidth - 6, panelTop + 15, 0x6600E5FF);
    guiGraphics.drawString(font, countBadge, panelLeft + panelWidth - countW - 9,
            panelTop + 5, 0xFFFFFFFF, false);

    int left = listLeft();
    int top = listTop();
    int bottom = listBottom();
    int listW = listWidth();
    guiGraphics.fill(left, top, left + listW, bottom, 0x480A0C10);
    guiGraphics.renderOutline(left, top, listW, bottom - top, 0x7838424E);

    guiGraphics.enableScissor(left + 1, top + 1, left + listW - 1, bottom - 1);
    int startY = top + 2 - (int) scrollAmount;
    for (int index = 0; index < filteredWaypoints.size(); index++) {
        int rowY = startY + index * ROW_HEIGHT;
        if (rowY + ROW_HEIGHT < top || rowY > bottom) continue;
        WaypointManager.Waypoint waypoint = filteredWaypoints.get(index);
        boolean hover = mouseX >= left && mouseX <= left + listW
                && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT
                && mouseY >= top && mouseY <= bottom;
        boolean focused = waypoint == focusedWaypoint;
        int background = focused
                ? (hover ? 0x961E3648 : 0x82182C3C)
                : (hover ? 0x7A242C38 : (index % 2 == 0 ? 0x58141820 : 0x66181D26));
        guiGraphics.fill(left + 2, rowY + 1, left + listW - 2,
                rowY + ROW_HEIGHT - 1, background);
        if (focused) {
            guiGraphics.renderOutline(left + 2, rowY + 1, listW - 4,
                    ROW_HEIGHT - 2, 0x9800E5FF);
        }

        renderWaypointIcon(guiGraphics, waypoint, left + 6, rowY + 5);
        String rawName = waypoint.deathPoint ? waypoint.name + " [Death]" : waypoint.name;
        String dimShort = shortDimensionLabel(waypoint.dimension);
        int dimW = font.width(dimShort);
        int maxNameW = Math.max(40, listW - 34 - dimW - 10);
        String trimmedName = font.plainSubstrByWidth(rawName, maxNameW);
        guiGraphics.drawString(font, trimmedName, left + 27, rowY + 4,
                waypoint.deathPoint ? 0xFFFF8A80 : 0xFFFFFFFF, false);
        guiGraphics.drawString(font, dimShort, left + listW - dimW - 5, rowY + 4,
                0xFF8CA0B5, false);

        String yText = waypoint.hasY ? Integer.toString((int) Math.floor(waypoint.y)) : "—";
        String coordinates = String.format(Locale.ROOT, "%d, %s, %d",
                (int) Math.floor(waypoint.x), yText, (int) Math.floor(waypoint.z));
        RowActions actions = rowActions(rowY);
        int maxCoordW = Math.max(30, actions.pinX - (left + 27) - 3);
        String trimmedCoords = font.plainSubstrByWidth(coordinates, maxCoordW);
        guiGraphics.drawString(font, trimmedCoords, left + 27, rowY + 22,
                0xFFA8B2BD, false);

        boolean tracked = isTracked(waypoint);
        drawCompactButton(guiGraphics, actions.pinX, actions.buttonY,
                actions.pinWidth, actions.buttonHeight,
                tracked ? "Stop" : "Pin", tracked ? 0x881E5E3A : 0x66242C36, mouseX, mouseY);
        drawCompactButton(guiGraphics, actions.editX, actions.buttonY,
                actions.editWidth, actions.buttonHeight,
                "Edit", 0x66242C36, mouseX, mouseY);
        drawCompactButton(guiGraphics, actions.shareX, actions.buttonY,
                actions.shareWidth, actions.buttonHeight,
                "Share", 0x66242C36, mouseX, mouseY);
        drawCompactButton(guiGraphics, actions.teleportX, actions.buttonY,
                actions.teleportWidth, actions.buttonHeight,
                "TP", 0x6622384A, mouseX, mouseY);
        boolean confirming = pendingDelete == waypoint;
        drawCompactButton(guiGraphics, actions.deleteX, actions.buttonY,
                actions.deleteWidth, actions.buttonHeight,
                confirming ? "Sure?" : "Del",
                confirming ? 0xAA8B2626 : 0x663A2226, mouseX, mouseY);
    }
    guiGraphics.disableScissor();

    // Thin scrollbar indicator inside the list well
    int maxScroll = maximumScroll();
    if (maxScroll > 0) {
        int trackH = bottom - top - 4;
        int thumbH = Math.max(14, (int) ((double) trackH * trackH / (trackH + maxScroll)));
        int thumbY = top + 2 + (int) ((trackH - thumbH) * (scrollAmount / maxScroll));
        guiGraphics.fill(left + listW - 3, thumbY, left + listW - 1, thumbY + thumbH, 0x9900E5FF);
    }

    if (filteredWaypoints.isEmpty()) {
        String empty = "No matching waypoints";
        guiGraphics.drawCenteredString(font, empty, panelLeft + panelWidth / 2,
                top + (bottom - top) / 2 - 4, 0xFFA8B2BD);
    }

    super.render(guiGraphics, mouseX, mouseY, partialTick);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
}

private static String shortDimensionLabel(String dimension) {
    if (isVanillaDimension(dimension, "overworld")) return "Overworld";
    if (isVanillaDimension(dimension, "the_nether")) return "Nether";
    if (isVanillaDimension(dimension, "the_end")) return "End";
    return MapManager.displayDimensionName(dimension);
}

private void renderWaypointIcon(GuiGraphics guiGraphics,
        WaypointManager.Waypoint waypoint, int x, int y) {
    guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, 0x660C0F14);
    guiGraphics.renderOutline(x - 1, y - 1, 18, 18, 0x804A5462);
    String itemId = WaypointManager.resolveIconItemId(waypoint);
    ResourceLocation location = ResourceLocation.tryParse(itemId);
    Item item = location == null ? Items.COMPASS : BuiltInRegistries.ITEM.get(location);
    if (item == null || item == Items.AIR) item = Items.COMPASS;
    guiGraphics.renderFakeItem(new ItemStack(item), x, y);
    RenderSystem.disableDepthTest();
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
}

private void drawCompactButton(GuiGraphics guiGraphics, int x, int y,
        int width, int height, String text, int baseColor, int mouseX, int mouseY) {
    boolean hover = mouseX >= x && mouseX <= x + width
            && mouseY >= y && mouseY <= y + height;
    int background = hover ? 0x9938485C : baseColor;
    guiGraphics.fill(x, y, x + width, y + height, background);
    guiGraphics.renderOutline(x, y, width, height,
            hover ? 0xFF00E5FF : 0x805A6675);
    guiGraphics.pose().pushPose();
    float scale = 0.78f;
    int textWidth = Math.round(font.width(text) * scale);
    float drawX = x + (width - textWidth) * 0.5f;
    float drawY = y + (height - 8 * scale) * 0.5f + 0.5f;
    guiGraphics.pose().translate(drawX, drawY, 0.0f);
    guiGraphics.pose().scale(scale, scale, 1.0f);
    guiGraphics.drawString(font, text, 0, 0, 0xFFFFFFFF, false);
    guiGraphics.pose().popPose();
}

private static boolean isVanillaDimension(String dimension, String path) {
    if (dimension == null) return false;
    String normalized = dimension.toLowerCase(Locale.ROOT);
    return normalized.equals(path) || normalized.equals("minecraft:" + path)
            || (path.equals("the_nether") && normalized.equals("nether"))
            || (path.equals("the_end") && normalized.equals("end"));
}

@Override
public void onClose() {
    restorePreviousMapView();
    if (minecraft != null) minecraft.setScreen(parent);
}

@Override
public boolean isPauseScreen() {
    return false;
}

private record RowActions(int pinX, int editX, int shareX,
        int teleportX, int deleteX, int pinWidth, int editWidth, int shareWidth,
        int teleportWidth, int deleteWidth, int buttonY, int buttonHeight) {
}

/**
 * Dedicated create/edit workflow for waypoints.
 */
public static class AddWaypointScreen extends Screen {
    private static final String[] MINING_ITEMS = {
            "minecraft:iron_pickaxe", "minecraft:iron_axe", "minecraft:minecart",
            "minecraft:raw_iron", "minecraft:diamond", "minecraft:chest"
    };
    private static final String[] COMBAT_ITEMS = {
            "minecraft:iron_sword", "minecraft:bow", "minecraft:shield",
            "minecraft:nether_star", "minecraft:experience_bottle", "minecraft:target"
    };
    private static final String[] TRAVEL_ITEMS = {
            "minecraft:compass", "minecraft:ender_eye", "minecraft:elytra",
            "minecraft:red_bed", "minecraft:recovery_compass", "minecraft:lodestone"
    };
    private static final String[] FOOD_ITEMS = {
            "minecraft:golden_apple", "minecraft:bread", "minecraft:cooked_beef",
            "minecraft:carrot", "minecraft:cake", "minecraft:honey_bottle"
    };

    private final Screen parent;
    private final WaypointManager.Waypoint original;
    private final String previousViewDimension;
    private final boolean previousViewWasLive;
    private final List<String> dimensions = new ArrayList<>();
    private final List<ItemIconButton> iconButtons = new ArrayList<>();
    private final List<ItemIconButton> searchButtons = new ArrayList<>();
    private final List<String> searchResults = new ArrayList<>();
    private final List<Button> tabButtons = new ArrayList<>();

    private double draftX;
    private double draftY;
    private double draftZ;
    private String draftDimension;
    private String draftName;
    private float waypointScale;
    private int selectedIconType = -1;
    private String selectedIconItem;
    private int activeTab;
    private String validationMessage = "";
    private boolean previousViewRestored;

    private EditBox nameInput;
    private EditBox xInput;
    private EditBox yInput;
    private EditBox zInput;
    private EditBox searchBox;
    private Button dimensionButton;
    private SimpleSlider localScaleSlider;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private float previewScale = 0.55f;
    private double mapPanOffsetX = 0.0;
    private double mapPanOffsetZ = 0.0;
    private boolean draggingMap = false;
    private double dragStartX = 0.0;
    private double dragStartY = 0.0;

    public AddWaypointScreen(Screen parent, double worldX, double worldZ, String dimension) {
        this(parent, worldX, defaultY(dimension), worldZ, dimension);
    }

    public AddWaypointScreen(Screen parent, double worldX, double worldY, double worldZ,
            String dimension) {
        super(Component.translatable("gui.simplemap.add_waypoint"));
        this.parent = parent;
        this.original = null;
        this.draftX = worldX;
        this.draftY = worldY;
        this.draftZ = worldZ;
        this.draftDimension = normalizeDimension(dimension);
        this.draftName = "";
        this.waypointScale = 1.0f;
        this.selectedIconItem = "minecraft:compass";
        this.previousViewDimension = MapManager.getInstance()
                .getCurrentDimensionResourceId();
        this.previousViewWasLive = MapManager.getInstance().isViewingLiveDimension();
        rebuildDimensions();
    }

    public AddWaypointScreen(Screen parent, WaypointManager.Waypoint waypoint) {
        super(Component.translatable("gui.simplemap.edit_waypoint"));
        this.parent = parent;
        this.original = waypoint;
        this.draftX = waypoint.x;
        this.draftY = waypoint.hasY ? waypoint.y : defaultY(waypoint.dimension);
        this.draftZ = waypoint.z;
        this.draftDimension = normalizeDimension(waypoint.dimension);
        this.draftName = waypoint.name;
        this.waypointScale = waypoint.scale;
        this.selectedIconType = -1;
        this.selectedIconItem = WaypointManager.resolveIconItemId(waypoint);
        this.previousViewDimension = MapManager.getInstance()
                .getCurrentDimensionResourceId();
        this.previousViewWasLive = MapManager.getInstance().isViewingLiveDimension();
        rebuildDimensions();
    }

    private static double defaultY(String dimension) {
        Minecraft mc = Minecraft.getInstance();
        String normalized = normalizeDimension(dimension);
        if (mc.player != null && MapManager.getInstance().getLiveDimensionResourceId()
                .equals(normalized)) {
            return mc.player.getY();
        }
        return MapTeleportController.defaultTargetY(normalized);
    }

    private static String normalizeDimension(String dimension) {
        if (dimension == null || dimension.isBlank()) return "minecraft:overworld";
        return MapManager.getInstance().resolveDimensionResourceId(dimension);
    }

    private void rebuildDimensions() {
        Set<String> ordered = new LinkedHashSet<>();
        ordered.add(draftDimension);
        ordered.add(normalizeDimension(MapManager.getInstance().getCurrentDimensionId()));
        ordered.add(normalizeDimension(MapManager.getInstance().getLiveDimensionResourceId()));
        for (String dimension : MapManager.getInstance().getSelectableDimensions()) {
            ordered.add(normalizeDimension(dimension));
        }
        for (WaypointManager.Waypoint waypoint : WaypointManager.getInstance().getAllWaypoints()) {
            ordered.add(normalizeDimension(waypoint.dimension));
        }
        dimensions.clear();
        dimensions.addAll(ordered);
    }

    @Override
    protected void init() {
        ensureDraftDimensionViewed();
        iconButtons.clear();
        searchButtons.clear();
        tabButtons.clear();

        if (parent instanceof MapScreen mapScreen && mapScreen.scale > 0.05f) {
            previewScale = mapScreen.scale;
        } else {
            previewScale = Math.max(0.35f, Math.min(0.85f, (this.width - 2) / 1200.0f));
        }

        // Compact, translucent left-docked card so the live map and waypoints
        // remain unobstructed across the screen.
        panelWidth = Math.min(216, Math.max(188, this.width - 24));
        panelHeight = Math.min(212, Math.max(198, this.height - 12));
        panelLeft = Math.max(8, Math.min(14, this.width / 28));
        panelTop = Math.max(6, (this.height - panelHeight) / 2);

        int leftX = panelLeft + 8;
        int fieldWidth = panelWidth - 16;
        int top = panelTop + 22;

        nameInput = new EditBox(this.font, leftX, top, fieldWidth, 18,
                Component.translatable("gui.simplemap.narration.waypoint_name"));
        nameInput.setMaxLength(64);
        nameInput.setValue(draftName);
        nameInput.setHint(Component.literal(defaultName()));
        addRenderableWidget(nameInput);

        int coordinateY = top + 28;
        int coordinateGap = 4;
        int coordinateWidth = (fieldWidth - coordinateGap * 2) / 3;
        xInput = coordinateBox(leftX, coordinateY, coordinateWidth, draftX, "gui.simplemap.narration.x_coord");
        yInput = coordinateBox(leftX + coordinateWidth + coordinateGap, coordinateY,
                coordinateWidth, draftY, "gui.simplemap.narration.y_coord");
        zInput = coordinateBox(leftX + (coordinateWidth + coordinateGap) * 2,
                coordinateY, coordinateWidth, draftZ, "gui.simplemap.narration.z_coord");

        int halfButton = (fieldWidth - 4) / 2;
        int row3Y = top + 50;
        dimensionButton = Button.builder(dimensionLabel(), button -> cycleDimension())
                .bounds(leftX, row3Y, halfButton, 18).build();
        addRenderableWidget(dimensionButton);

        double sliderValue = (waypointScale - 0.5) / 1.5;
        localScaleSlider = new SimpleSlider(leftX + halfButton + 4, row3Y, halfButton, 18,
                Math.max(0.0, Math.min(1.0, sliderValue)),
                value -> Component.translatable("gui.simplemap.waypoint.size_slider",
                        String.format(Locale.ROOT, "%.1f", 0.5 + value * 1.5)).getString(),
                value -> waypointScale = (float) (0.5 + value * 1.5));
        addRenderableWidget(localScaleSlider);

        int row4Y = top + 71;
        addRenderableWidget(Button.builder(Component.translatable("gui.simplemap.waypoint.player_pos"), button -> usePlayerPosition())
                .bounds(leftX, row4Y, halfButton, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.simplemap.waypoint.pick_on_map"), button -> openMapPicker())
                .bounds(leftX + halfButton + 4, row4Y, halfButton, 18).build());

        String[] tabKeys = {
                "gui.simplemap.icon_tab.mine",
                "gui.simplemap.icon_tab.fight",
                "gui.simplemap.icon_tab.move",
                "gui.simplemap.icon_tab.food",
                "gui.simplemap.icon_tab.find"
        };
        int tabGap = 2;
        int tabWidth = Math.max(28, (fieldWidth - tabGap * 4) / 5);
        int tabY = top + 93;
        for (int index = 0; index < tabKeys.length; index++) {
            final int tab = index;
            Button button = Button.builder(Component.translatable(tabKeys[index]), b -> setTab(tab))
                    .bounds(leftX + index * (tabWidth + tabGap), tabY, tabWidth, 16).build();
            tabButtons.add(button);
            addRenderableWidget(button);
        }

        int iconStartY = top + 113;
        addItemGroupButtons(MINING_ITEMS, 0, leftX, iconStartY, fieldWidth);
        addItemGroupButtons(COMBAT_ITEMS, 1, leftX, iconStartY, fieldWidth);
        addItemGroupButtons(TRAVEL_ITEMS, 2, leftX, iconStartY, fieldWidth);
        addItemGroupButtons(FOOD_ITEMS, 3, leftX, iconStartY, fieldWidth);

        searchBox = new EditBox(this.font, leftX, iconStartY, fieldWidth, 16,
                Component.translatable("gui.simplemap.narration.search_waypoint_icon"));
        searchBox.setMaxLength(48);
        searchBox.setHint(Component.translatable("gui.simplemap.hint.search_item_id"));
        searchBox.setResponder(value -> updateSearchResults());
        addRenderableWidget(searchBox);

        int searchY = iconStartY + 19;
        int iconSlotStep = Math.max(24, fieldWidth / 6);
        int iconOffsetX = leftX + Math.max(0, (fieldWidth - (5 * iconSlotStep + 22)) / 2);
        for (int index = 0; index < 6; index++) {
            final int resultIndex = index;
            ItemIconButton button = new ItemIconButton(
                    iconOffsetX + index * iconSlotStep,
                    searchY,
                    22, 22, "", () -> selectedIconItem, b -> {
                        if (resultIndex < searchResults.size()) {
                            selectIcon(searchResults.get(resultIndex));
                        }
                    });
            searchButtons.add(button);
            addRenderableWidget(button);
        }

        int bottomY = panelTop + panelHeight - 23;
        addRenderableWidget(Button.builder(Component.translatable("gui.simplemap.cancel"), button -> onClose())
                .bounds(leftX, bottomY, halfButton, 18).build());
        addRenderableWidget(Button.builder(Component.translatable(original == null ? "gui.simplemap.add" : "gui.simplemap.save"),
                button -> saveWaypoint())
                .bounds(leftX + halfButton + 4, bottomY, halfButton, 18).build());

        updateTabVisibility();
    }

    private static final java.util.regex.Pattern THREE_COORD_PATTERN = java.util.regex.Pattern.compile(
            "^\\s*[\\[\\(\\{]?"
                    + "(?:[xX]\\s*[:=]?\\s*)?([+-]?\\d+(?:\\.\\d+)?)"
                    + "(?:\\s*[,;/|]\\s*|\\s+)"
                    + "(?:[yY]\\s*[:=]?\\s*)?([+-]?\\d+(?:\\.\\d+)?)"
                    + "(?:\\s*[,;/|]\\s*|\\s+)"
                    + "(?:[zZ]\\s*[:=]?\\s*)?([+-]?\\d+(?:\\.\\d+)?)"
                    + "[\\]\\)\\}]?\\s*$");

    private EditBox coordinateBox(int x, int y, int width, double value, String narrationKey) {
        EditBox box = new EditBox(this.font, x, y, width, 18, Component.translatable(narrationKey)) {
            @Override
            public void insertText(String textToWrite) {
                if (tryApplyPastedCoordinates(textToWrite)) {
                    return;
                }
                super.insertText(textToWrite);
            }
        };
        box.setMaxLength(18);
        box.setValue(formatCoordinate(value));
        addRenderableWidget(box);
        return box;
    }

    private boolean tryApplyPastedCoordinates(String raw) {
        if (raw == null || raw.length() < 5) return false;
        java.util.regex.Matcher matcher = THREE_COORD_PATTERN.matcher(raw);
        if (!matcher.matches()) return false;
        try {
            double px = Double.parseDouble(matcher.group(1));
            double py = Double.parseDouble(matcher.group(2));
            double pz = Double.parseDouble(matcher.group(3));
            if (!Double.isFinite(px) || !Double.isFinite(py) || !Double.isFinite(pz)
                    || Math.abs(px) > 30_000_000.0
                    || Math.abs(py) > 30_000_000.0
                    || Math.abs(pz) > 30_000_000.0) {
                return false;
            }
            draftX = px;
            draftY = py;
            draftZ = pz;
            validationMessage = "";
            updateCoordinateInputs();
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static String formatCoordinate(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String defaultName() {
        int count = WaypointManager.getInstance().getWaypointsForDimension(draftDimension).size() + 1;
        return Component.translatable("gui.simplemap.waypoint.default_name", count).getString();
    }

    private Component dimensionLabel() {
        return Component.translatable("gui.simplemap.waypoint.dim_label",
                MapManager.displayDimensionName(draftDimension));
    }

    private void cycleDimension() {
        syncDraftQuietly();
        int index = Math.max(0, dimensions.indexOf(draftDimension));
        draftDimension = dimensions.get((index + 1) % dimensions.size());
        ensureDraftDimensionViewed();
        dimensionButton.setMessage(dimensionLabel());
    }

    private void usePlayerPosition() {
        if (minecraft == null || minecraft.player == null) return;
        draftX = minecraft.player.getX();
        draftY = minecraft.player.getY();
        draftZ = minecraft.player.getZ();
        mapPanOffsetX = 0.0;
        mapPanOffsetZ = 0.0;
        draftDimension = normalizeDimension(MapManager.getInstance().getLiveDimensionResourceId());
        ensureDraftDimensionViewed();
        ensureDimensionPresent(draftDimension);
        updateCoordinateInputs();
        dimensionButton.setMessage(dimensionLabel());
        validationMessage = "";
    }

    private void openMapPicker() {
        if (!syncDraftFromFields()) return;
        if (minecraft != null) {
            minecraft.setScreen(new WaypointMapPickerScreen(this, draftX, draftZ));
        }
    }

    void acceptPickedLocation(double x, double z, String dimension) {
        draftX = x;
        draftZ = z;
        mapPanOffsetX = 0.0;
        mapPanOffsetZ = 0.0;
        draftDimension = normalizeDimension(dimension);
        ensureDimensionPresent(draftDimension);
        if (minecraft != null && minecraft.player != null
                && draftDimension.equals(normalizeDimension(MapManager.getInstance().getLiveDimensionResourceId()))) {
            draftY = minecraft.player.getY();
        }
        updateCoordinateInputs();
        validationMessage = "";
    }

    private void ensureDimensionPresent(String dimension) {
        if (!dimensions.contains(dimension)) dimensions.add(0, dimension);
    }

    private void ensureDraftDimensionViewed() {
        if (minecraft == null) return;
        MapManager manager = MapManager.getInstance();
        String target = normalizeDimension(draftDimension);
        if (target.equals(manager.getCurrentDimensionResourceId())) return;
        if (target.equals(manager.getLiveDimensionResourceId())) {
            manager.returnToLiveDimension(minecraft);
        } else {
            manager.switchToDimension(target);
        }
    }

    private void restorePreviousMapView() {
        if (previousViewRestored || minecraft == null) return;
        previousViewRestored = true;
        MapManager manager = MapManager.getInstance();
        if (previousViewWasLive) manager.returnToLiveDimension(minecraft);
        else manager.switchToDimension(previousViewDimension);
    }

    private void updateCoordinateInputs() {
        if (xInput != null) xInput.setValue(formatCoordinate(draftX));
        if (yInput != null) yInput.setValue(formatCoordinate(draftY));
        if (zInput != null) zInput.setValue(formatCoordinate(draftZ));
    }

    private boolean syncDraftFromFields() {
        try {
            draftX = parseCoordinate(xInput.getValue(), "X");
            draftY = parseCoordinate(yInput.getValue(), "Y");
            draftZ = parseCoordinate(zInput.getValue(), "Z");
            draftName = nameInput.getValue().trim();
            validationMessage = "";
            return true;
        } catch (IllegalArgumentException exception) {
            validationMessage = exception.getMessage();
            return false;
        }
    }

    private void syncDraftQuietly() {
        try {
            draftX = Double.parseDouble(xInput.getValue().trim());
            draftY = Double.parseDouble(yInput.getValue().trim());
            draftZ = Double.parseDouble(zInput.getValue().trim());
        } catch (RuntimeException ignored) {
        }
        draftName = nameInput.getValue().trim();
    }

    private static double parseCoordinate(String text, String axis) {
        try {
            double value = Double.parseDouble(text.trim());
            if (!Double.isFinite(value) || Math.abs(value) > 30_000_000.0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    Component.translatable("gui.simplemap.waypoint.invalid_coord", axis).getString());
        }
    }

    private void saveWaypoint() {
        if (!syncDraftFromFields()) return;
        String name = draftName.isBlank() ? defaultName() : draftName;
        WaypointManager.Waypoint replacement = new WaypointManager.Waypoint(
                name, draftX, draftY, draftZ, selectedIconType, selectedIconItem,
                waypointScale, draftDimension, original != null && original.deathPoint);

        boolean trackedOriginal = original != null && MapConfig.pinActive
                && Math.abs(MapConfig.pinWorldX - original.x) < 0.01
                && Math.abs(MapConfig.pinWorldZ - original.z) < 0.01;
        if (original == null) {
            WaypointManager.getInstance().addWaypoint(replacement);
        } else if (!WaypointManager.getInstance().updateWaypoint(original, replacement)) {
            validationMessage = Component.translatable("gui.simplemap.waypoint.not_found").getString();
            return;
        }
        if (trackedOriginal) {
            PinNavigation.updateDestination(replacement.x, replacement.z);
            MapManager.getInstance().savePin();
        }
        restorePreviousMapView();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private void setTab(int tab) {
        activeTab = tab;
        updateTabVisibility();
    }

    private void addItemGroupButtons(String[] items, int tab, int startX, int startY, int fieldWidth) {
        int slotStep = Math.max(24, fieldWidth / 6);
        int offsetX = startX + Math.max(0, (fieldWidth - (5 * slotStep + 22)) / 2);
        for (int index = 0; index < items.length; index++) {
            String itemId = items[index];
            ItemIconButton button = new ItemIconButton(
                    offsetX + (index % 6) * slotStep,
                    startY + (index / 6) * 24 + 8,
                    22, 22, itemId, () -> selectedIconItem, b -> selectIcon(itemId));
            button.tab = tab;
            iconButtons.add(button);
            addRenderableWidget(button);
        }
    }

    private void selectIcon(String itemId) {
        selectedIconType = -1;
        selectedIconItem = itemId == null || itemId.isBlank() ? "minecraft:compass" : itemId;
    }

    private void updateTabVisibility() {
        for (int index = 0; index < tabButtons.size(); index++) {
            Button button = tabButtons.get(index);
            String plain = Component.translatable(switch (index) {
                case 0 -> "gui.simplemap.icon_tab.mine";
                case 1 -> "gui.simplemap.icon_tab.fight";
                case 2 -> "gui.simplemap.icon_tab.move";
                case 3 -> "gui.simplemap.icon_tab.food";
                default -> "gui.simplemap.icon_tab.find";
            }).getString();
            button.setMessage(Component.literal(index == activeTab ? "§b" + plain : plain));
        }
        for (ItemIconButton button : iconButtons) button.visible = button.tab == activeTab;
        boolean searching = activeTab == 4;
        if (searchBox != null) searchBox.visible = searching;
        for (ItemIconButton button : searchButtons) button.visible = false;
        if (searching) updateSearchResults();
    }

    private void updateSearchResults() {
        searchResults.clear();
        if (activeTab != 4 || searchBox == null) return;
        String query = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        for (ResourceLocation key : BuiltInRegistries.ITEM.keySet()) {
            if (key.getPath().equals("air")) continue;
            String id = key.toString();
            if (query.isEmpty() || id.toLowerCase(Locale.ROOT).contains(query)) {
                searchResults.add(id);
                if (searchResults.size() >= searchButtons.size()) break;
            }
        }
        for (int index = 0; index < searchButtons.size(); index++) {
            ItemIconButton button = searchButtons.get(index);
            if (index < searchResults.size()) {
                button.itemId = searchResults.get(index);
                button.visible = true;
            } else {
                button.itemId = "";
                button.visible = false;
            }
        }
    }

    private boolean isInsidePanel(double mouseX, double mouseY) {
        return mouseX >= panelLeft && mouseX <= panelLeft + panelWidth
                && mouseY >= panelTop && mouseY <= panelTop + panelHeight;
    }

    private double getMapAreaCenterX() {
        int rightAreaLeft = panelLeft + panelWidth;
        int rightAreaWidth = Math.max(40, this.width - rightAreaLeft);
        return rightAreaLeft + rightAreaWidth * 0.5;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (!isInsidePanel(mouseX, mouseY)) {
            if (button == 0) {
                draggingMap = true;
                dragStartX = mouseX;
                dragStartY = mouseY;
                return true;
            } else if (button == 1) {
                double anchorScreenX = getMapAreaCenterX();
                double anchorScreenY = this.height * 0.5;
                double mapCenterX = (draftX + mapPanOffsetX)
                        - (anchorScreenX - this.width * 0.5) / previewScale;
                double mapCenterZ = (draftZ + mapPanOffsetZ)
                        - (anchorScreenY - this.height * 0.5) / previewScale;
                draftX = Math.floor(mapCenterX + (mouseX - this.width * 0.5) / previewScale) + 0.5;
                draftZ = Math.floor(mapCenterZ + (mouseY - this.height * 0.5) / previewScale) + 0.5;
                mapPanOffsetX = 0.0;
                mapPanOffsetZ = 0.0;
                updateCoordinateInputs();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingMap && button == 0) {
            mapPanOffsetX -= dragX / Math.max(0.01f, previewScale);
            mapPanOffsetZ -= dragY / Math.max(0.01f, previewScale);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingMap && button == 0) {
            draggingMap = false;
            if (Math.hypot(mouseX - dragStartX, mouseY - dragStartY) < 4.0
                    && !isInsidePanel(mouseX, mouseY)) {
                double anchorScreenX = getMapAreaCenterX();
                double anchorScreenY = this.height * 0.5;
                double mapCenterX = (draftX + mapPanOffsetX)
                        - (anchorScreenX - this.width * 0.5) / previewScale;
                double mapCenterZ = (draftZ + mapPanOffsetZ)
                        - (anchorScreenY - this.height * 0.5) / previewScale;
                draftX = Math.floor(mapCenterX + (mouseX - this.width * 0.5) / previewScale) + 0.5;
                draftZ = Math.floor(mapCenterZ + (mouseY - this.height * 0.5) / previewScale) + 0.5;
                mapPanOffsetX = 0.0;
                mapPanOffsetZ = 0.0;
                updateCoordinateInputs();
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isInsidePanel(mouseX, mouseY)) {
            previewScale = Math.max(0.0625f,
                    Math.min(8.0f, previewScale + (float) scrollY * 0.15f * previewScale));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY,
            float partialTick) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        syncDraftQuietly();
        float backgroundScale = Math.max(0.0625f, previewScale);
        double anchorScreenX = getMapAreaCenterX();
        double anchorScreenY = height * 0.5;
        double mapCenterX = (draftX + mapPanOffsetX)
                - (anchorScreenX - width * 0.5) / backgroundScale;
        double mapCenterZ = (draftZ + mapPanOffsetZ)
                - (anchorScreenY - height * 0.5) / backgroundScale;
        double mouseWorldX = mapCenterX
                + (mouseX - width * 0.5) / backgroundScale;
        double mouseWorldZ = mapCenterZ
                + (mouseY - height * 0.5) / backgroundScale;

        // Render full, unobstructed live map + existing waypoints in the background
        // without any full-screen dark veil so terrain and markers stay crystal clear.
        MapRenderer.getInstance().drawMap(guiGraphics, 0, 0, width, height,
                mapCenterX, mapCenterZ, backgroundScale,
                MapManager.getInstance().isViewingLiveDimension(), false, false,
                mouseWorldX, mouseWorldZ, partialTick, false);

        // Draw the live target waypoint marker at its exact world position on the map
        int markerScreenX = (int) Math.round(width * 0.5 + (draftX - mapCenterX) * backgroundScale);
        int markerScreenY = (int) Math.round(height * 0.5 + (draftZ - mapCenterZ) * backgroundScale);
        guiGraphics.renderOutline(markerScreenX - 10, markerScreenY - 10, 20, 20, 0xB000E5FF);
        renderScaledItem(guiGraphics, selectedIconItem,
                markerScreenX, markerScreenY,
                0.20f * MapConfig.waypointScale * waypointScale);
        String liveName = (draftName == null || draftName.isBlank()) ? defaultName() : draftName;
        String mapPreview = liveName + " [" + Math.round(draftX) + ", " + Math.round(draftZ) + "]";
        int labelW = font.width(mapPreview);
        int labelY = (int) Math.round(markerScreenY + 13.0 * Math.max(0.8f, waypointScale));
        guiGraphics.fill(markerScreenX - labelW / 2 - 3, labelY - 2,
                markerScreenX + labelW / 2 + 3, labelY + 9, 0x78000000);
        guiGraphics.drawCenteredString(font, mapPreview, markerScreenX, labelY, 0xFFFFFFFF);

        // Subtle map interaction hint at top of the open map area
        String mapHint = Component.translatable("gui.simplemap.waypoint.map_hint").getString();
        int hintW = font.width(mapHint);
        int hintX = (int) Math.round(anchorScreenX) - hintW / 2;
        guiGraphics.fill(hintX - 5, 6, hintX + hintW + 5, 19, 0x680C0E12);
        guiGraphics.drawString(font, mapHint, hintX, 9, 0xFFD0D8E0, false);

        // Sleek translucent glass panel on the left (map remains visible behind & around it)
        guiGraphics.fill(panelLeft, panelTop, panelLeft + panelWidth,
                panelTop + panelHeight, 0x7A101318);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + panelWidth,
                panelTop + 18, 0x88181D24);
        guiGraphics.renderOutline(panelLeft, panelTop, panelWidth, panelHeight, 0x984A5462);

        // Header with title and inline selected icon preview
        renderScaledItem(guiGraphics, selectedIconItem,
                panelLeft + 12, panelTop + 9, 0.75f);
        guiGraphics.drawString(font, title, panelLeft + 24, panelTop + 5, 0xFFFFFFFF, false);

        int leftX = panelLeft + 8;
        int top = panelTop + 22;
        int fieldWidth = panelWidth - 16;
        int coordinateGap = 4;
        int coordinateWidth = (fieldWidth - coordinateGap * 2) / 3;
        guiGraphics.drawString(font, "X", leftX + 1, top + 20, 0xFFA8B2BD, false);
        guiGraphics.drawString(font, "Y", leftX + coordinateWidth + coordinateGap + 1,
                top + 20, 0xFFA8B2BD, false);
        guiGraphics.drawString(font, "Z", leftX + (coordinateWidth + coordinateGap) * 2 + 1,
                top + 20, 0xFFA8B2BD, false);

        if (!validationMessage.isBlank()) {
            guiGraphics.drawString(font, validationMessage, leftX,
                    panelTop + panelHeight - 34, 0xFFFF6B6B, false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private static void renderItem(GuiGraphics guiGraphics, String itemId, int x, int y) {
        ResourceLocation location = ResourceLocation.tryParse(itemId);
        Item item = location == null ? Items.COMPASS : BuiltInRegistries.ITEM.get(location);
        if (item == null || item == Items.AIR) item = Items.COMPASS;
        guiGraphics.renderFakeItem(new ItemStack(item), x, y);
    }

    private static void renderScaledItem(GuiGraphics guiGraphics, String itemId,
            int centerX, int centerY, float scale) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, centerY, 80.0);
        float safeScale = Math.max(0.35f, Math.min(2.2f, scale));
        guiGraphics.pose().scale(safeScale, safeScale, 1.0f);
        renderItem(guiGraphics, itemId, -8, -8);
        guiGraphics.pose().popPose();
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void onClose() {
        restorePreviousMapView();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class SimpleSlider extends AbstractSliderButton {
        private final java.util.function.Consumer<Double> onChange;
        private final java.util.function.Function<Double, String> labelProvider;

        SimpleSlider(int x, int y, int width, int height, double defaultValue,
                java.util.function.Function<Double, String> labelProvider,
                java.util.function.Consumer<Double> onChange) {
            super(x, y, width, height,
                    Component.literal(labelProvider.apply(defaultValue)), defaultValue);
            this.labelProvider = labelProvider;
            this.onChange = onChange;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(labelProvider.apply(value)));
        }

        @Override
        protected void applyValue() {
            onChange.accept(value);
        }
    }

    private static class ItemIconButton extends Button {
        private String itemId;
        private int tab = -1;
        private final java.util.function.Supplier<String> selectedSupplier;

        ItemIconButton(int x, int y, int width, int height, String itemId,
                java.util.function.Supplier<String> selectedSupplier, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.itemId = itemId;
            this.selectedSupplier = selectedSupplier;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY,
                float partialTick) {
            if (!visible) return;
            boolean selected = itemId != null && !itemId.isBlank()
                    && selectedSupplier != null && itemId.equals(selectedSupplier.get());
            int bg = selected ? 0x88243B52 : (isHoveredOrFocused() ? 0x70222830 : 0x4812161C);
            int border = selected ? 0xCC00D4FF : (isHoveredOrFocused() ? 0xAA9098A4 : 0x66404854);
            guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
            guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), border);
            if (itemId != null && !itemId.isBlank()) {
                renderItem(guiGraphics, itemId, getX() + (width - 16) / 2,
                        getY() + (height - 16) / 2);
            }
        }
    }

    public static final class WaypointMapPickerScreen extends Screen {
        private static final float MIN_SCALE = 0.015625f;
        private static final float MAX_SCALE = 12.0f;

        private final AddWaypointScreen parent;
        private double centerX;
        private double centerZ;
        private float scale = 0.5f;
        private boolean dragging;
        private double pressX;
        private double pressY;

        public WaypointMapPickerScreen(AddWaypointScreen parent, double centerX,
                double centerZ) {
            super(Component.translatable("gui.simplemap.waypoint.picker_title"));
            this.parent = parent;
            this.centerX = centerX;
            this.centerZ = centerZ;
        }

        @Override
        protected void init() {
            addRenderableWidget(Button.builder(Component.translatable("gui.simplemap.cancel"), button -> onClose())
                    .bounds(width / 2 - 45, height - 27, 90, 20).build());
        }

        @Override
        public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY,
                float partialTick) {
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.fill(0, 0, width, height, 0xFF0D0D0D);
            int vx = 1;
            int vy = 1;
            int vw = width - 2;
            int vh = height - 2;
            double mouseWorldX = centerX + (mouseX - vx - vw / 2.0) / scale;
            double mouseWorldZ = centerZ + (mouseY - vy - vh / 2.0) / scale;

            MapRenderer.getInstance().drawMap(guiGraphics, vx, vy, vw, vh,
                    centerX, centerZ, scale,
                    MapManager.getInstance().isViewingLiveDimension(), false, false,
                    mouseWorldX, mouseWorldZ, partialTick, false);

            String instruction = Component.translatable("gui.simplemap.waypoint.picker_instruction").getString();
            int instructionWidth = font.width(instruction);
            int boxLeft = (width - instructionWidth) / 2 - 7;
            guiGraphics.fill(boxLeft, 7, boxLeft + instructionWidth + 14, 25, 0xD0101010);
            guiGraphics.renderOutline(boxLeft, 7, instructionWidth + 14, 18, 0xFF707070);
            guiGraphics.drawString(font, instruction, boxLeft + 7, 12, 0xFFFFFFFF, false);

            String cursor = String.format("X: %d  Z: %d",
                    (int) Math.floor(mouseWorldX), (int) Math.floor(mouseWorldZ));
            int cursorWidth = font.width(cursor);
            guiGraphics.fill(width - cursorWidth - 13, height - 26,
                    width - 5, height - 8, 0xD0101010);
            guiGraphics.drawString(font, cursor, width - cursorWidth - 9,
                    height - 21, 0xFFFFFFFF, false);

            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (super.mouseClicked(mouseX, mouseY, button)) return true;
            if (button == 0) {
                dragging = true;
                pressX = mouseX;
                pressY = mouseY;
                return true;
            }
            if (button == 1) {
                onClose();
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button,
                double dragX, double dragY) {
            if (dragging && button == 0) {
                centerX -= dragX / scale;
                centerZ -= dragY / scale;
                return true;
            }
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0 && dragging) {
                dragging = false;
                double moved = Math.hypot(mouseX - pressX, mouseY - pressY);
                if (moved < 4.0) {
                    double worldX = centerX + (mouseX - 1 - (width - 2) / 2.0) / scale;
                    double worldZ = centerZ + (mouseY - 1 - (height - 2) / 2.0) / scale;
                    double selectedX = Math.floor(worldX) + 0.5;
                    double selectedZ = Math.floor(worldZ) + 0.5;
                    parent.acceptPickedLocation(selectedX, selectedZ,
                            MapManager.getInstance().getCurrentDimensionResourceId());
                    MapViewportCoordinator.getInstance().closeFullscreen();
                    if (minecraft != null) minecraft.setScreen(parent);
                    return true;
                }
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX,
                double scrollY) {
            float oldScale = scale;
            float factor = scrollY > 0 ? 1.15f : 1.0f / 1.15f;
            scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale * factor));
            if (oldScale != scale) {
                double beforeX = centerX + (mouseX - width / 2.0) / oldScale;
                double beforeZ = centerZ + (mouseY - height / 2.0) / oldScale;
                centerX = beforeX - (mouseX - width / 2.0) / scale;
                centerZ = beforeZ - (mouseY - height / 2.0) / scale;
            }
            return true;
        }

        @Override
        public void onClose() {
            MapViewportCoordinator.getInstance().closeFullscreen();
            if (minecraft != null) minecraft.setScreen(parent);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
}
