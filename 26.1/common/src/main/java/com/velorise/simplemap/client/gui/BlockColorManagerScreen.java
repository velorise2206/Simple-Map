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

public final class BlockColorManagerScreen extends Screen {
    private static final int ROWS = 8;
    private static final int CONTENT_WIDTH = 420;
    private static final int PANEL_PADDING = 8;
    private final Screen parent;
    private final List<java.util.Map.Entry<String, Integer>> filtered = new ArrayList<>();
    private EditBox search;
    private int page;
    private boolean confirmClear;

    public BlockColorManagerScreen(Screen parent) {
        super(Component.translatable("gui.simplemap.block_colors_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        search = new EditBox(font, left, top + 24, 300, 20, Component.translatable("gui.simplemap.search_blocks"));
        search.setHint(Component.translatable("gui.simplemap.hint.search_registry_id"));
        search.setResponder(value -> {
            page = 0;
            rebuildRows();
        });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.translatable("gui.simplemap.done"), b -> closeToParent())
                .bounds(left + 322, top + 24, 98, 20).build());
        rebuildRows();
    }

    private void rebuildRows() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        filtered.clear();
        MapConfig.blockColorOverrides.entrySet().stream()
                .filter(entry -> query.isEmpty() || entry.getKey().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(java.util.Map.Entry::getKey))
                .forEach(filtered::add);
        int maxPage = Math.max(0, (filtered.size() - 1) / ROWS);
        page = Math.max(0, Math.min(page, maxPage));

        clearWidgets();
        int left = panelLeft();
        int top = panelTop();
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.translatable("gui.simplemap.done"), b -> closeToParent())
                .bounds(left + 322, top + 24, 98, 20).build());

        int start = page * ROWS;
        for (int row = 0; row < ROWS && start + row < filtered.size(); row++) {
            java.util.Map.Entry<String, Integer> entry = filtered.get(start + row);
            String id = entry.getKey();
            int y = top + 52 + row * 23;
            addRenderableWidget(Button.builder(Component.translatable("gui.simplemap.edit"), b -> edit(id))
                    .bounds(left + 306, y, 52, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.simplemap.reset"), b -> {
                MapColorProfile.resetBlockOverride(id);
                rebuildRows();
            }).bounds(left + 364, y, 56, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            if (page > 0) { page--; rebuildRows(); }
        }).bounds(left, top + 241, 36, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            if ((page + 1) * ROWS < filtered.size()) { page++; rebuildRows(); }
        }).bounds(left + 42, top + 241, 36, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(confirmClear ? "gui.simplemap.confirm_clear_all" : "gui.simplemap.clear_all"), b -> {
            if (!confirmClear) {
                confirmClear = true;
                rebuildRows();
            } else {
                confirmClear = false;
                MapColorProfile.clearAllBlockOverrides();
                rebuildRows();
            }
        }).bounds(left + 280, top + 241, 140, 20).build());
    }

    private void edit(String blockId) {
        int automatic = MapTextureManager.getInstance().resolveAutomaticBlockColor(blockId);
        String displayName = blockId;
        Identifier loc = Identifier.tryParse(blockId);
        if (loc != null && BuiltInRegistries.BLOCK.containsKey(loc)) {
            displayName = BuiltInRegistries.BLOCK.getValue(loc).getName().getString();
        }
        if (minecraft != null) minecraft.setScreen(new BlockColorScreen(this, blockId, displayName, 0, 0, automatic));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xCC080A0C);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left - PANEL_PADDING, top - PANEL_PADDING, left + CONTENT_WIDTH + PANEL_PADDING, top + 270, 0xF0111215);
        graphics.outline(left - PANEL_PADDING, top - PANEL_PADDING, CONTENT_WIDTH + PANEL_PADDING * 2, 278, 0xFF3B3E44);
        graphics.text(font, title, left, top, 0xFFFFFFFF, false);

        int start = page * ROWS;
        for (int row = 0; row < ROWS && start + row < filtered.size(); row++) {
            java.util.Map.Entry<String, Integer> entry = filtered.get(start + row);
            int y = top + 52 + row * 23;
            int color = 0xFF000000 | (entry.getValue() & 0x00FFFFFF);
            graphics.fill(left, y + 2, left + 18, y + 18, color);
            graphics.outline(left, y + 2, 18, 16, 0xFFFFFFFF);
            graphics.text(font, entry.getKey(), left + 25, y + 6, 0xFFE0E0E0, false);
        }
        int pages = Math.max(1, (filtered.size() + ROWS - 1) / ROWS);
        graphics.text(font,
                Component.translatable("gui.simplemap.block_colors.page_info", filtered.size(), page + 1, pages),
                left + 88, top + 247, 0xFF9A9A9A, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0 && page > 0) { page--; rebuildRows(); return true; }
        if (scrollY < 0 && (page + 1) * ROWS < filtered.size()) { page++; rebuildRows(); return true; }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int panelLeft() {
        return Math.max(10, (width - CONTENT_WIDTH) / 2);
    }

    private int panelTop() {
        return Math.max(10, Math.min(height - 270, height / 2 - 132));
    }

    private void closeToParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() { closeToParent(); }

    @Override
    public boolean isPauseScreen() { return false; }

    /** Small editor for a single registry-backed surface-block color override. */
    public static class BlockColorScreen extends Screen {
        private final Screen parent;
        private final String blockId;
        private final String blockName;
        private final int blockX;
        private final int blockZ;
        private final int automaticColor;
        private final List<PaletteSwatch> swatches = new ArrayList<>();
        private EditBox colorInput;

        public BlockColorScreen(Screen parent, String blockId, String blockName,
                int blockX, int blockZ, int automaticColor) {
            super(Component.translatable("gui.simplemap.block_color_editor_title"));
            this.parent = parent;
            this.blockId = blockId;
            this.blockName = blockName;
            this.blockX = blockX;
            this.blockZ = blockZ;
            this.automaticColor = automaticColor;
        }

        @Override
        protected void init() {
            int left = this.width / 2 - 120;
            int top = Math.max(14, this.height / 2 - 90);
            int currentColor = MapConfig.blockColorOverrides.getOrDefault(blockId, automaticColor);

            colorInput = new EditBox(this.font, left, top + 48, 160, 20, Component.translatable("gui.simplemap.narration.block_hex_color"));
            colorInput.setMaxLength(9);
            colorInput.setValue(MapColorProfile.formatColor(currentColor));
            colorInput.setHint(Component.literal("#RRGGBB"));
            colorInput.setResponder(value -> {
                try {
                    MapColorProfile.parseColor(value);
                    colorInput.setTextColor(0xFFFFFFFF);
                } catch (IllegalArgumentException ignored) {
                    colorInput.setTextColor(0xFFFF5555);
                }
            });
            addRenderableWidget(colorInput);
            addRenderableWidget(Button.builder(Component.translatable("gui.simplemap.save"), button -> saveOverride())
                    .bounds(left + 166, top + 48, 74, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.simplemap.automatic"), button -> resetOverride())
                    .bounds(left, top + 128, 116, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.simplemap.cancel"), button -> closeToParent())
                    .bounds(left + 124, top + 128, 116, 20).build());

            swatches.clear();
            for (int i = 0; i < Math.min(8, MapConfig.savedColors.size()); i++) {
                int x = left + (i % 4) * 60;
                int y = top + 76 + (i / 4) * 24;
                PaletteSwatch swatch = new PaletteSwatch(x, y, 56, 18, i, MapConfig.savedColors.get(i));
                swatches.add(swatch);
                addRenderableWidget(swatch);
            }
        }

        private void saveOverride() {
            try {
                int color = MapColorProfile.parseColor(colorInput.getValue());
                rememberColor(color);
                MapColorProfile.applyBlockOverride(blockId, color);
                closeToParent();
            } catch (IllegalArgumentException ignored) {
                colorInput.setTextColor(0xFFFF5555);
            }
        }

        private void resetOverride() {
            MapColorProfile.resetBlockOverride(blockId);
            closeToParent();
        }

        private void rememberColor(int color) {
            MapConfig.savedColors.remove(Integer.valueOf(color));
            MapConfig.savedColors.add(0, color);
            while (MapConfig.savedColors.size() > 8) {
                MapConfig.savedColors.remove(MapConfig.savedColors.size() - 1);
            }
        }

        private void closeToParent() {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        }

        @Override
        public void onClose() {
            closeToParent();
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            int left = this.width / 2 - 126;
            int top = Math.max(8, this.height / 2 - 96);
            guiGraphics.fill(0, 0, this.width, this.height, 0x88000000);
            guiGraphics.fill(left, top, left + 252, top + 166, 0xF0111215);
            guiGraphics.outline(left, top, 252, 166, 0xFF3B3E44);
            guiGraphics.centeredText(this.font, blockName, this.width / 2, top + 10, 0xFFFFFFFF);
            guiGraphics.centeredText(this.font, blockId, this.width / 2, top + 23, 0xFF9A9A9A);
            guiGraphics.text(this.font,
                    Component.translatable(MapConfig.savedColors.isEmpty()
                            ? "gui.simplemap.block_color.enter_color"
                            : "gui.simplemap.block_color.saved_palette"),
                    left + 6, top + 63, 0xFF9A9A9A, false);

            int previewColor = automaticColor;
            if (colorInput != null) {
                try {
                    previewColor = MapColorProfile.parseColor(colorInput.getValue());
                } catch (IllegalArgumentException ignored) {
                }
            }
            guiGraphics.fill(left + 218, top + 7, left + 244, top + 33,
                    0xFF000000 | (previewColor & 0x00FFFFFF));
            guiGraphics.outline(left + 217, top + 6, 28, 28, 0xFFFFFFFF);
            super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }

        private class PaletteSwatch extends AbstractButton {
            private final int slot;
            private final int color;

            PaletteSwatch(int x, int y, int width, int height, int slot, int color) {
                super(x, y, width, height, Component.translatable("gui.simplemap.palette.use_color", MapColorProfile.formatColor(color)));
                this.slot = slot;
                this.color = color;
            }

            @Override
            public void onPress(net.minecraft.client.input.InputWithModifiers input) {
                if (input.hasShiftDown() && slot < MapConfig.savedColors.size()) {
                    MapConfig.savedColors.remove(slot);
                    MapConfig.save();
                    BlockColorScreen.this.init(BlockColorScreen.this.width, BlockColorScreen.this.height);
                } else {
                    colorInput.setValue(MapColorProfile.formatColor(color));
                }
            }

            @Override
            protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
                int border = isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF55585D;
                guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                        0xFF000000 | (color & 0x00FFFFFF));
                guiGraphics.outline(getX(), getY(), getWidth(), getHeight(), border);
            }

            @Override
            public void updateWidgetNarration(NarrationElementOutput output) {
                defaultButtonNarrationText(output);
            }
        }
    }
}
