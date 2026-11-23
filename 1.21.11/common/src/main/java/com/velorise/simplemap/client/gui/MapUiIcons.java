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

public final class MapUiIcons {
    private static final net.minecraft.resources.Identifier ATLAS =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("simplemap", "gui/gui.png");
    private static final int ATLAS_SIZE = 256;

    public enum Icon {
        SETTINGS(0, 0, 16, 15),
        WAYPOINT_OUTLINE(0, 16, 12, 17),
        WAYPOINT_FILLED(16, 16, 12, 17),
        WAYPOINT_LIST(32, 16, 13, 17),
        REFRESH(0, 80, 14, 15),
        SUN(0, 48, 20, 20),
        MOON(32, 48, 16, 16),
        NIGHT_AUTO(48, 48, 19, 19),
        DIMENSION_PORTAL(0, 96, 12, 13),
        CAVE_ON(0, 112, 17, 14),
        CAVE_OFF(32, 112, 17, 14);

        final int u, v, width, height;

        Icon(int u, int v, int width, int height) {
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
        }
    }

    private MapUiIcons() { }

    public static void draw(net.minecraft.client.gui.GuiGraphics graphics, Icon icon,
            int centerX, int centerY, boolean enabled) {
        drawScaled(graphics, icon, centerX, centerY, enabled, 1.0f);
    }

    public static void drawScaled(net.minecraft.client.gui.GuiGraphics graphics, Icon icon,
            int centerX, int centerY, boolean enabled, float scale) {
        float safeScale = Math.max(0.25f, Math.min(2.0f, scale));
        int drawWidth = Math.max(1, Math.round(icon.width * safeScale));
        int drawHeight = Math.max(1, Math.round(icon.height * safeScale));
        float alpha = enabled ? 1.0f : 0.42f;
        int argb = ((Math.round(alpha * 255.0f) & 0xFF) << 24) | 0x00FFFFFF;
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ATLAS,
                centerX - drawWidth / 2, centerY - drawHeight / 2,
                (float) icon.u, (float) icon.v,
                drawWidth, drawHeight,
                icon.width, icon.height, ATLAS_SIZE, ATLAS_SIZE, argb);
    }
}
