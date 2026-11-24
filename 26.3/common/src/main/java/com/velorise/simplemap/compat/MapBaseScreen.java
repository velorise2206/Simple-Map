package com.velorise.simplemap.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Minecraft 1.21.11 Screen base class bridging MouseButtonEvent / KeyEvent / CharacterEvent
 * to version-neutral hooks.
 */
public abstract class MapBaseScreen extends Screen {
    public record MapMouseEvent(double x, double y, int button, boolean doubleClick) {}
    public record MapKeyEvent(int keyCode, int scanCode, int modifiers) {}

    protected MapBaseScreen(Component title) {
        super(title);
    }

    protected void onMapRender(MapGuiAdapter gui, int mouseX, int mouseY, float partialTick) {}
    protected void onMapRenderBackground(MapGuiAdapter gui, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(((LegacyGuiGraphicsAdapter) gui).unwrap(), mouseX, mouseY, partialTick);
    }
    protected boolean onMapMouseClick(MapMouseEvent event) { return false; }
    protected boolean onMapMouseRelease(MapMouseEvent event) { return false; }
    protected boolean onMapMouseDrag(MapMouseEvent event, double dragX, double dragY) { return false; }
    protected boolean onMapMouseScroll(double mouseX, double mouseY, double scrollX, double scrollY) { return false; }
    protected boolean onMapKeyPress(MapKeyEvent event) { return false; }
    protected boolean onMapCharTyped(char codePoint, int modifiers) { return false; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        onMapRender(MapCompat.gui(guiGraphics), mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        onMapRenderBackground(MapCompat.gui(guiGraphics), mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (onMapMouseClick(new MapMouseEvent(event.x(), event.y(), event.button(), doubleClick))) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (onMapMouseRelease(new MapMouseEvent(event.x(), event.y(), event.button(), false))) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (onMapMouseDrag(new MapMouseEvent(event.x(), event.y(), event.button(), false), dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (onMapMouseScroll(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (onMapKeyPress(new MapKeyEvent(event.key(), event.keycode(), event.modifiers()))) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (onMapCharTyped((char) event.codepoint(), 0)) {
            return true;
        }
        return super.charTyped(event);
    }
}
