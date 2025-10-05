package myau.ui.components;

import myau.enums.ChatColors;
import myau.property.properties.ColorProperty;
import myau.ui.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ColorSliderComponent implements Component {

    private final ModuleComponent parentModule;
    private final ColorProperty property;
    private int offsetY;
    private boolean draggingHue, draggingSat, draggingBri;
    private float hue, saturation, brightness;

    public ColorSliderComponent(ColorProperty property, ModuleComponent parentModule, int offsetY) {
        this.parentModule = parentModule;
        this.offsetY = offsetY;
        this.property = property;

        Color c = new Color(property.getValue());
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
    }

    @Override
    public void draw(java.util.concurrent.atomic.AtomicInteger offset) {
        int x = parentModule.category.getX() + 4;
        int y = parentModule.category.getY() + offsetY;
        int width = parentModule.category.getWidth() - 8;
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(property.getName().replace("-", " ") + ": " + ChatColors.formatColor(property.formatValue()), (float) (x * 2), (float) ((int) ((float) (this.parentModule.category.getY() + this.offsetY + 3) * 2.0F)), -1);
        GL11.glPopMatrix();
        if (!draggingHue && !draggingSat && !draggingBri) {
            Color color = new Color(property.getValue());
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
            hue = hsb[0];
            saturation = hsb[1];
            brightness = hsb[2];
        }
        int colorPreviewSize = 6;
        int colorPreviewX = x + width - colorPreviewSize;
        int colorPreviewY = y + 2;
        int previewColor = Color.HSBtoRGB(hue, saturation, brightness);
        Gui.drawRect(colorPreviewX - 6, colorPreviewY, colorPreviewX + colorPreviewSize, colorPreviewY + colorPreviewSize, previewColor);
        int baseY = y + 10;
        int satY = baseY + 4 + 2;
        int briY = satY + 4 + 2;
        drawHueBar(x, baseY, width);
        drawPointer(x, baseY, width, hue);
        drawGradientRect(x, satY, x + width, satY + 4, Color.WHITE.getRGB(), Color.getHSBColor(hue, 1f, 1f).getRGB());
        drawPointer(x, satY, width, saturation);
        drawGradientRect(x, briY, x + width, briY + 4, Color.BLACK.getRGB(), Color.getHSBColor(hue, saturation, 1f).getRGB());
        drawPointer(x, briY, width, brightness);
    }

    private void drawHueBar(int x, int y, int width) {
        for (int i = 0; i < width; i++) {
            float hue = (float) i / (float) width;
            int color = Color.HSBtoRGB(hue, 1f, 1f);
            Gui.drawRect(x + i, y, x + i + 1, y + 4, color);
        }
    }

    private void drawPointer(int x, int y, int width, float value) {
        int posX = x + (int) (width * value);
        Gui.drawRect(posX - 1, y, posX, y + 4, new Color(0, 0, 0, 200).getRGB());
    }

    @Override
    public void update(int mouseX, int mouseY) {
        int baseX = parentModule.category.getX() + 4;
        int width = parentModule.category.getWidth() - 8;
        boolean changed = false;

        if (draggingHue) {
            hue = getSliderValue(mouseX, baseX, width);
            changed = true;
        }
        if (draggingSat) {
            saturation = getSliderValue(mouseX, baseX, width);
            changed = true;
        }
        if (draggingBri) {
            brightness = getSliderValue(mouseX, baseX, width);
            changed = true;
        }

        if (changed) {
            int signed = Color.HSBtoRGB(hue, saturation, brightness);
            property.setValue(new Color(signed).getRGB());
        }
    }

    private float getSliderValue(int mouseX, int startX, int width) {
        double d = Math.min(width, Math.max(0, mouseX - startX));
        return (float) roundToPrecision(d / width, 3);
    }

    private static double roundToPrecision(double v, int precision) {
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(precision, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (button != 0 || !parentModule.panelExpand) return;
        int baseY = parentModule.category.getY() + offsetY + 10;
        if (isHovered(mouseX, mouseY, baseY)) draggingHue = true;
        else if (isHovered(mouseX, mouseY, baseY + 4 + 2)) draggingSat = true;
        else if (isHovered(mouseX, mouseY, baseY + (4 + 2) * 2)) draggingBri = true;
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
        draggingHue = draggingSat = draggingBri = false;
    }

    private boolean isHovered(int mx, int my, int sliderY) {
        int startX = parentModule.category.getX() + 4;
        int endX = startX + parentModule.category.getWidth() - 8;
        return mx >= startX && mx <= endX && my >= sliderY && my <= sliderY + 4;
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return 10 + 17;
    }

    private void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        float sa = (float) (startColor >> 24 & 255) / 255.0F;
        float sr = (float) (startColor >> 16 & 255) / 255.0F;
        float sg = (float) (startColor >> 8 & 255) / 255.0F;
        float sb = (float) (startColor & 255) / 255.0F;
        float ea = (float) (endColor >> 24 & 255) / 255.0F;
        float er = (float) (endColor >> 16 & 255) / 255.0F;
        float eg = (float) (endColor >> 8 & 255) / 255.0F;
        float eb = (float) (endColor & 255) / 255.0F;
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer world = tessellator.getWorldRenderer();
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_ALPHA_TEST);
        org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        org.lwjgl.opengl.GL11.glShadeModel(org.lwjgl.opengl.GL11.GL_SMOOTH);
        world.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        world.pos(right, top, 0).color(er, eg, eb, ea).endVertex();
        world.pos(left, top, 0).color(sr, sg, sb, sa).endVertex();
        world.pos(left, bottom, 0).color(sr, sg, sb, sa).endVertex();
        world.pos(right, bottom, 0).color(er, eg, eb, ea).endVertex();
        tessellator.draw();
        org.lwjgl.opengl.GL11.glShadeModel(org.lwjgl.opengl.GL11.GL_FLAT);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_ALPHA_TEST);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
    }

}
