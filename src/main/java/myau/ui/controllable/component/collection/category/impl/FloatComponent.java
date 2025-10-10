package myau.ui.controllable.component.collection.category.impl;

import myau.property.properties.FloatProperty;
import myau.ui.controllable.FakeBrInterface;
import myau.ui.controllable.component.Component;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft; // added back for text rendering

import java.awt.*;

public class FloatComponent extends Component {
    private final FloatProperty property;
    private boolean dragging = false;
    private static final float EXTRA_VERTICAL_PADDING = 4f;

    public FloatComponent(final FloatProperty property, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(property.getName(), posX, posY, offsetX, offsetY, width, height);
        this.property = property;
        // 增加额外高度以拉开文字与滑轨间距
        setHeight(getHeight() + EXTRA_VERTICAL_PADDING);
    }

    private float computePercentage() {
        float range = property.getMaximum() - property.getMinimum();
        if (range <= 0) return 0f;
        return (property.getValue() - property.getMinimum()) / range;
    }

    @Override
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        super.onDrawScreen(mouseX, mouseY, partialTicks);
        // 绘制名称（置于顶部，避免与轨道重叠）
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
                property.getName(),
                getPosX(),
                getPosY() + 1,
                0xFFB0B0B0
        );
        // 绘制当前值（右对齐）
        String valueStr = String.format("%.2f", property.getValue());
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
                valueStr,
                getPosX() + getWidth() - Minecraft.getMinecraft().fontRendererObj.getStringWidth(valueStr),
                getPosY() + 1,
                0xFF689FFF
        );
        // 状态准备
        RenderUtil.enableRenderState();
        boolean hovered = FakeBrInterface.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY(), getWidth(), getHeight());
        if (hovered) {
            RenderUtil.drawRect(getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + getHeight(), new Color(70, 70, 70, 60).getRGB());
        }
        // 处理拖动
        if (dragging) {
            if (!org.lwjgl.input.Mouse.isButtonDown(0)) {
                dragging = false;
            } else {
                float dragPct = Math.max(0, Math.min(1, (mouseX - getPosX()) / Math.max(1f, getWidth())));
                float newValue = property.getMinimum() + dragPct * (property.getMaximum() - property.getMinimum());
                property.setValue(newValue);
            }
        }
        // 调整轨道基线: 原公式基础上补偿额外内边距的一半让底部留白
        float baseLineY = getPosY() + getHeight() - 12 - (EXTRA_VERTICAL_PADDING / 2f);
        float lineY = baseLineY - 2;
        // 主线与上下阴影
//        RenderUtil.drawRect(getPosX(), lineY, getPosX() + getWidth(), lineY + 1, new Color(90, 90, 90, 255).getRGB());
//        RenderUtil.drawRect(getPosX(), lineY - 1, getPosX() + getWidth(), lineY, new Color(55, 55, 55, 255).getRGB());
//        RenderUtil.drawRect(getPosX(), lineY + 1, getPosX() + getWidth(), lineY + 2, new Color(55, 55, 55, 255).getRGB());
        float pct = computePercentage();
        float minX = getPosX();
        float curX = getPosX() + getWidth() * pct;
        float maxX = getPosX() + getWidth() - 1;
        float tickW = 0.5f;
        int endColor = new Color(160, 160, 160, 255).getRGB();
        int curColor = new Color(255, 255, 255, 255).getRGB();
//        RenderUtil.drawRect(minX - tickW, lineY - 3, minX + tickW, lineY + 4, endColor);
//        RenderUtil.drawRect(maxX - tickW, lineY - 3, maxX + tickW, lineY + 4, endColor);
//        RenderUtil.drawRect(curX - tickW, lineY - 4, curX + tickW, lineY + 5, curColor);
        // 底部槽与进度
        float sliderY = getPosY() + getHeight() - 4;
        float sliderH = 5;
        RenderUtil.drawRect(getPosX(), sliderY, getPosX() + getWidth(), sliderY + sliderH, new Color(50, 50, 50, 180).getRGB());
        RenderUtil.drawRect(getPosX(), sliderY, getPosX() + getWidth() * pct, sliderY + sliderH, new Color(0x66, 0x99, 0xFF, 255).getRGB());
        // 手柄(修正 drawRect 坐标形式)
        float handleW = 6f;
        float handleH = sliderH + 4f;
        RenderUtil.drawRect(curX - handleW / 2f, sliderY - 2, curX + handleW / 2f, sliderY - 2 + handleH, new Color(235, 235, 235, 255).getRGB());
        RenderUtil.disableRenderState();
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        super.onMouseClicked(mouseX, mouseY, button);
        // 扩大点击区域，包括整个滑块高度
        float sliderY = getPosY() + getHeight() - 5;
        if (button == 0 && FakeBrInterface.mouseWithinBounds(mouseX, mouseY, getPosX(), sliderY, getWidth(), 8)) {
            dragging = true;
            // 立即应用拖动
            float percentage = Math.max(0, Math.min(1, (mouseX - getPosX()) / getWidth()));
            float newValue = property.getMinimum() + percentage * (property.getMaximum() - property.getMinimum());
            property.setValue(newValue);
        }
    }

    @Override
    public void onMouseReleased(int mouseX, int mouseY, int button) {
        super.onMouseReleased(mouseX, mouseY, button);
        if (button == 0) {
            dragging = false;
        }
    }
}
