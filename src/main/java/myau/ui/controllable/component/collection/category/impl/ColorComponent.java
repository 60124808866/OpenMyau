package myau.ui.controllable.component.collection.category.impl;

import myau.property.properties.ColorProperty;
import myau.ui.controllable.FakeBrInterface;
import myau.ui.controllable.component.Component;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;

import java.awt.*;

public class ColorComponent extends Component {
    private final ColorProperty property;
    // HSB state
    private float hue;          // 0-1
    private float saturation;   // 0-1
    private float brightness;   // 0-1

    // Dragging flags
    private boolean draggingPalette = false;
    private boolean draggingHue = false;

    // Layout constants
    private final float palettePadding = 4f;
    private final float hueSliderWidth = 8f;
    private final float headerHeight = 14f;
    private final float footerPadding = 4f;

    public ColorComponent(final ColorProperty property, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(property.getName(), posX, posY, offsetX, offsetY, width, height);
        this.property = property;
        // initial HSB from property
        loadFromProperty();
        // Increase height to fit palette (if user passed smaller height, override)
        setHeight(Math.max(height, 80));
    }

    private void loadFromProperty() {
        Color c = new Color(property.getValue());
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
    }

    private int currentColor() {
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    @Override
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        super.onDrawScreen(mouseX, mouseY, partialTicks);

        // If not dragging, keep HSB in sync with property (external changes)
        if (!draggingPalette && !draggingHue) {
            loadFromProperty();
        }

        float x = getPosX();
        float y = getPosY();
        float w = getWidth();
        float h = getHeight();

        // Hover highlight
        if (FakeBrInterface.mouseWithinBounds(mouseX, mouseY, x, y, w, h)) {
            RenderUtil.drawRect(x, y, x + w, y + h, new Color(60, 60, 60, 50).getRGB());
        }

        // Header: label + value (hex)
        String name = property.getName();
        String hex = String.format("#%06X", (0xFFFFFF & property.getValue()));
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(name, x, y + 2, 0xFFDDDDDD);
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(hex, x + w - Minecraft.getMinecraft().fontRendererObj.getStringWidth(hex), y + 2, 0xFFAAAAAA);

        // Compute palette area
        float paletteTop = y + headerHeight;
        float paletteHeight = h - headerHeight - footerPadding;
        float paletteWidth = w - hueSliderWidth - palettePadding * 2 - 2; // leave space for hue slider & margins
        if (paletteWidth < 10) paletteWidth = 10;
        if (paletteHeight < 10) paletteHeight = 10;
        float paletteX = x + palettePadding;
        float paletteY = paletteTop + palettePadding;
        float paletteInnerH = paletteHeight - palettePadding * 2;
        float paletteInnerW = paletteWidth;

        // Hue slider area (vertical)
        float hueX = paletteX + paletteInnerW + 4;
        float hueY = paletteY;
        float hueH = paletteInnerH;

        // Draw palette: left->right saturation, top->bottom brightness (inverted brightness for standard look or normal?)
        // We'll map y increasing downward to decreasing brightness for intuitive top=bright.
        drawSaturationBrightnessSquare(paletteX, paletteY, paletteInnerW, paletteInnerH);

        // Draw hue slider
        drawHueSlider(hueX, hueY, hueSliderWidth, hueH);

        // Palette picker indicator
        float pickX = paletteX + saturation * paletteInnerW;
        float pickY = paletteY + (1 - brightness) * paletteInnerH;
        drawPickerMarker(pickX, pickY, 4, new Color(255,255,255,200).getRGB(), new Color(0,0,0,160).getRGB());

        // Hue slider indicator
        float hueIndicatorY = hueY + (1 - hue) * hueH; // hue 0 (red) at bottom? More common: hue increases downward; keep as above.
        drawPickerMarker(hueX + hueSliderWidth / 2f, hueIndicatorY, 3, new Color(255,255,255,220).getRGB(), new Color(0,0,0,150).getRGB());

        // Color preview box (top-right inside header area)
        int col = currentColor();
        float previewSize = 10;
        float previewX = x + w - previewSize - 2;
        float previewY = y + headerHeight + 2;
        RenderUtil.drawRect(previewX, previewY, previewX + previewSize, previewY + previewSize, col | 0xFF000000);
        RenderUtil.drawOutlineRect(previewX, previewY, previewSize, previewSize, 1, 0, new Color(0,0,0,180).getRGB());

        // Handle dragging updates
        if (draggingPalette) {
            float sx = Math.max(0, Math.min(1, (mouseX - paletteX) / paletteInnerW));
            float by = Math.max(0, Math.min(1, (mouseY - paletteY) / paletteInnerH));
            saturation = sx;
            brightness = 1 - by; // invert mapping
            applyToProperty();
        }
        if (draggingHue) {
            float hy = Math.max(0, Math.min(1, (mouseY - hueY) / hueH));
            hue = 1 - hy; // invert so top = hue 1.0 (or could keep natural). Adjust for preference.
            applyToProperty();
        }
    }

    private void applyToProperty() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        property.setValue(0xFFFFFF & rgb); // ensure alpha stripped
    }

    private void drawSaturationBrightnessSquare(float x, float y, float w, float h) {
        // We render a grid of vertical strips blending saturation and brightness; step for performance.
        int steps = (int)Math.max(12, w / 2);
        float stripW = w / steps;
        for (int i = 0; i < steps; i++) {
            float sat = i / (float)(steps - 1);
            // vertical gradient from bright to dark at fixed saturation
            int topColor = Color.HSBtoRGB(hue, sat, 1f);
            int bottomColor = Color.HSBtoRGB(hue, sat, 0f);
            float sx1 = x + i * stripW;
            float sx2 = x + (i + 1) * stripW;
            drawVerticalGradientRect(sx1, y, sx2, y + h, topColor, bottomColor);
        }
        // border
        RenderUtil.drawOutlineRect(x, y, w, h, 1, 0, new Color(30,30,30,220).getRGB());
    }

    private void drawHueSlider(float x, float y, float w, float h) {
        int segments = (int)Math.max(30, h); // one pixel per segment if tall enough
        float segH = h / segments;
        for (int i = 0; i < segments; i++) {
            float localHue = 1 - i / (float)(segments - 1); // top -> hue=1
            int color = Color.HSBtoRGB(localHue, 1f, 1f);
            float sy1 = y + i * segH;
            float sy2 = y + (i + 1) * segH;
            RenderUtil.drawRect(x, sy1, x + w, sy2, color);
        }
        RenderUtil.drawOutlineRect(x, y, w, h, 1, 0, new Color(30,30,30,220).getRGB());
    }

    private void drawPickerMarker(float cx, float cy, float radius, int fill, int outline) {
        float r = radius;
        RenderUtil.drawRect(cx - r, cy - r, cx + r, cy + r, fill);
        RenderUtil.drawOutlineRect(cx - r, cy - r, r * 2, r * 2, 1, 0, outline);
    }

    private void drawVerticalGradientRect(float x1, float y1, float x2, float y2, int topColor, int bottomColor) {
        // Similar approach as in ColorSliderComponent's gradient, but vertical only
        float ta = (topColor >> 24 & 255) / 255.0F;
        float tr = (topColor >> 16 & 255) / 255.0F;
        float tg = (topColor >> 8 & 255) / 255.0F;
        float tb = (topColor & 255) / 255.0F;
        float ba = (bottomColor >> 24 & 255) / 255.0F;
        float br = (bottomColor >> 16 & 255) / 255.0F;
        float bg = (bottomColor >> 8 & 255) / 255.0F;
        float bb = (bottomColor & 255) / 255.0F;
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_ALPHA_TEST);
        org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        org.lwjgl.opengl.GL11.glShadeModel(org.lwjgl.opengl.GL11.GL_SMOOTH);
        net.minecraft.client.renderer.Tessellator tess = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x2, y1, 0).color(tr, tg, tb, ta).endVertex();
        wr.pos(x1, y1, 0).color(tr, tg, tb, ta).endVertex();
        wr.pos(x1, y2, 0).color(br, bg, bb, ba).endVertex();
        wr.pos(x2, y2, 0).color(br, bg, bb, ba).endVertex();
        tess.draw();
        org.lwjgl.opengl.GL11.glShadeModel(org.lwjgl.opengl.GL11.GL_FLAT);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_ALPHA_TEST);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        super.onMouseClicked(mouseX, mouseY, button);
        if (button != 0) return;

        float x = getPosX();
        float y = getPosY();
        float w = getWidth();
        float h = getHeight();
        float paletteWidth = w - hueSliderWidth - palettePadding * 2 - 2;
        float paletteTop = y + headerHeight;
        float paletteHeight = h - headerHeight - footerPadding;
        float paletteX = x + palettePadding;
        float paletteY = paletteTop + palettePadding;
        float paletteInnerH = paletteHeight - palettePadding * 2;
        float paletteInnerW = paletteWidth;
        float hueX = paletteX + paletteInnerW + 4;
        float hueY = paletteY;
        float hueH = paletteInnerH;

        // Check palette hit
        if (FakeBrInterface.mouseWithinBounds(mouseX, mouseY, paletteX, paletteY, paletteInnerW, paletteInnerH)) {
            draggingPalette = true;
            float sx = Math.max(0, Math.min(1, (mouseX - paletteX) / paletteInnerW));
            float by = Math.max(0, Math.min(1, (mouseY - paletteY) / paletteInnerH));
            saturation = sx;
            brightness = 1 - by;
            applyToProperty();
            return;
        }
        // Check hue slider
        if (FakeBrInterface.mouseWithinBounds(mouseX, mouseY, hueX, hueY, hueSliderWidth, hueH)) {
            draggingHue = true;
            float hy = Math.max(0, Math.min(1, (mouseY - hueY) / hueH));
            hue = 1 - hy;
            applyToProperty();
        }
    }

    @Override
    public void onMouseReleased(int mouseX, int mouseY, int button) {
        super.onMouseReleased(mouseX, mouseY, button);
        if (button == 0) {
            draggingPalette = false;
            draggingHue = false;
        }
    }
}
