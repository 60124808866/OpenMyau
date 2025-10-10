package myau.ui.controllable.component.collection.category.impl;

import myau.property.properties.IntProperty;
import myau.ui.controllable.FakeBrInterface;
import myau.ui.controllable.component.Component;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft; // added for text rendering

import java.awt.*;

public class IntComponent extends Component {
    private final IntProperty property;
    private boolean dragging = false;
    private static final float EXTRA_VERTICAL_PADDING = 4f;

    public IntComponent(final IntProperty property, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(property.getName(), posX, posY, offsetX, offsetY, width, height);
        this.property = property;
        setHeight(getHeight() + EXTRA_VERTICAL_PADDING); // 增加垂直间距
    }

    private float computePercentage() {
        int range = property.getMaximum() - property.getMinimum();
        if (range <= 0) return 0f;
        return (property.getValue() - property.getMinimum()) / (float) range;
    }

    @Override
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        super.onDrawScreen(mouseX, mouseY, partialTicks);
        // Draw property name above slider
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
                property.getName(),
                getPosX(),
                getPosY() + 1,
                0xFFB0B0B0
        );
        // Draw current value (right aligned)
        String valueStr = String.valueOf(property.getValue());
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
                valueStr,
                getPosX() + getWidth() - Minecraft.getMinecraft().fontRendererObj.getStringWidth(valueStr),
                getPosY() + 1,
                0xFFEEAA00
        );
        RenderUtil.enableRenderState();
        boolean hovered = FakeBrInterface.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY(), getWidth(), getHeight());
        if (hovered) {
            RenderUtil.drawRect(getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + getHeight(), new Color(70,70,70,60).getRGB());
        }
        if (dragging) {
            if (!org.lwjgl.input.Mouse.isButtonDown(0)) {
                dragging = false;
            } else {
                float p = Math.max(0, Math.min(1, (mouseX - getPosX()) / Math.max(1f, getWidth())));
                int newValue = (int)(property.getMinimum() + p * (property.getMaximum() - property.getMinimum()));
                property.setValue(newValue);
            }
        }
        // 调整基线，向上抬一点让底部留白，文字与滑轨更松
        float baseLineY = getPosY() + getHeight() - 12 - (EXTRA_VERTICAL_PADDING / 2f);
        float lineY = baseLineY - 2;
        // 替换后续使用 lineY 的绘制逻辑
        // 主线与上下阴影
//        RenderUtil.drawRect(getPosX(), lineY, getPosX() + getWidth(), lineY + 1, new Color(90,90,90,255).getRGB());
//        RenderUtil.drawRect(getPosX(), lineY -1, getPosX() + getWidth(), lineY, new Color(55,55,55,255).getRGB());
//        RenderUtil.drawRect(getPosX(), lineY +1, getPosX() + getWidth(), lineY +2, new Color(55,55,55,255).getRGB());
        float pct = computePercentage();
        float minX = getPosX();
        float curX = getPosX() + getWidth() * pct;
        float maxX = getPosX() + getWidth() -1;
        float tickW = 0.5f;
        int endColor = new Color(180,150,90,255).getRGB();
        int curColor = new Color(255,220,120,255).getRGB();
//        RenderUtil.drawRect(minX - tickW, lineY -3, minX + tickW, lineY +4, endColor);
//        RenderUtil.drawRect(maxX - tickW, lineY -3, maxX + tickW, lineY +4, endColor);
//        RenderUtil.drawRect(curX - tickW, lineY -4, curX + tickW, lineY +5, curColor);
        float sliderY = getPosY() + getHeight() - 4;
        float sliderH = 5;
        RenderUtil.drawRect(getPosX(), sliderY, getPosX() + getWidth(), sliderY + sliderH, new Color(50,50,50,180).getRGB());
        RenderUtil.drawRect(getPosX(), sliderY, getPosX() + getWidth() * pct, sliderY + sliderH, new Color(0xEE,0xAA,0x00,255).getRGB());
        float handleW = 6f;
        float handleH = sliderH + 4f;
        RenderUtil.drawRect(curX - handleW/2f, sliderY -2, curX + handleW/2f, sliderY -2 + handleH, new Color(240,240,240,255).getRGB());
        RenderUtil.disableRenderState();
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        super.onMouseClicked(mouseX, mouseY, button);
        float sliderY = getPosY() + getHeight() - 5;
        if (button == 0 && FakeBrInterface.mouseWithinBounds(mouseX, mouseY, getPosX(), sliderY, getWidth(), 8)) {
            dragging = true;
            float p = Math.max(0, Math.min(1, (mouseX - getPosX()) / getWidth()));
            int newValue = (int)(property.getMinimum() + p * (property.getMaximum() - property.getMinimum()));
            property.setValue(newValue);
        }
    }

    @Override
    public void onMouseReleased(int mouseX, int mouseY, int button) {
        super.onMouseReleased(mouseX, mouseY, button);
        if (button == 0) dragging = false;
    }
}
