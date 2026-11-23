package com.velorise.simplemap.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Version-agnostic Screen base class.
 * On 1.21.1, bridges legacy mouse/key signatures and GuiGraphics into version-neutral hooks.
 * On 1.21.9+/26.x, only this single class changes to override MouseButtonEvent / KeyEvent / GuiGraphicsExtractor.
 */
public abstract class MapBaseScreen extends Screen {
    public record MapMouseEvent(double x, double y, int button, boolean doubleClick) {}
    public record MapKeyEvent(int keyCode, int scanCode, int modifiers) {}

    protected MapBaseScreen(Component title) {
        super(title);
    }

    protected void onMapRender(MapGuiAdapter gui, int mouseX, int mouseY, float partialTick) {}
    protected void onMapRenderBackground(MapGuiAdapter gui, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(((LegacyGuiGraphicsAdapter) gui).unwrap(), mouseX, mouseY, partialTick);
    }
    protected boolean onMapMouseClick(MapMouseEvent event) { return false; }
    protected boolean onMapMouseRelease(MapMouseEvent event) { return false; }
    protected boolean onMapMouseDrag(MapMouseEvent event, double dragX, double dragY) { return false; }
    protected boolean onMapMouseScroll(double mouseX, double mouseY, double scrollX, double scrollY) { return false; }
    protected boolean onMapKeyPress(MapKeyEvent event) { return false; }
    protected boolean onMapCharTyped(char codePoint, int modifiers) { return false; }

    protected static boolean isShiftHeld() { return Screen.hasShiftDown(); }
    protected static boolean isCtrlHeld() { return Screen.hasControlDown(); }
    protected static boolean isAltHeld() { return Screen.hasAltDown(); }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        onMapRender(MapCompat.gui(guiGraphics), mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        onMapRenderBackground(MapCompat.gui(guiGraphics), mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (onMapMouseClick(new MapMouseEvent(mouseX, mouseY, button, false))) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (onMapMouseRelease(new MapMouseEvent(mouseX, mouseY, button, false))) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (onMapMouseDrag(new MapMouseEvent(mouseX, mouseY, button, false), dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (onMapMouseScroll(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (onMapKeyPress(new MapKeyEvent(keyCode, scanCode, modifiers))) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (onMapCharTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }
}
