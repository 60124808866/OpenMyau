package myau.ui.controllable.component.collection.category.impl;

import myau.property.properties.BooleanProperty;
import myau.ui.controllable.FakeBrInterface;
import myau.ui.controllable.component.Component;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;

import java.awt.*;

public class BooleanComponent extends Component {
    private final BooleanProperty property;

    public BooleanComponent(final BooleanProperty property, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(property.getName(), posX, posY, offsetX, offsetY, width, height);
        this.property = property;
    }

    @Override
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        super.onDrawScreen(mouseX, mouseY, partialTicks);

        // 检测鼠标悬停并绘制背景高亮
        boolean hovered = FakeBrInterface.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY(), getWidth(), getHeight());
        if (hovered) {
            RenderUtil.drawRect(getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + getHeight(), new Color(70, 70, 70, 100).getRGB());
        }

        // 绘制标签
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(getLabel(), getPosX(), getPosY() + 3, new Color(229, 229, 223, 255).getRGB());

        // 绘制状态文本（右对齐）
        String statusText = property.getValue() ? "ON" : "OFF";
        int statusColor = property.getValue() ? new Color(0, 255, 0, 255).getRGB() : new Color(255, 85, 85, 255).getRGB();
        // 右对齐显示，与其他组件的值保持一致
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
            statusText,
            getPosX() + getWidth() - Minecraft.getMinecraft().fontRendererObj.getStringWidth(statusText) - 2,
            getPosY() + 3,
            statusColor
        );
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        super.onMouseClicked(mouseX, mouseY, button);
        // 点击整个组件区域都可以切换状态
        if (button == 0 && FakeBrInterface.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY(), getWidth(), getHeight())) {
            property.setValue(!property.getValue());
        }
    }
}
