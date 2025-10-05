package myau.ui.controllable.component.collection.category.impl;

import myau.property.properties.ModeProperty;
import myau.ui.controllable.FakeBrInterface;
import myau.ui.controllable.component.Component;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;

import java.awt.*;

public class ModeComponent extends Component {
    private final ModeProperty property;
    private boolean dropdownOpen = false;
    private final String[] modes;
    private final float baseHeight; // 原始高度

    public ModeComponent(final ModeProperty property, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(property.getName(), posX, posY, offsetX, offsetY, width, height);
        this.property = property;
        this.modes = property.getValuePrompt().split(", ");
        this.baseHeight = height;
    }

    @Override
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        super.onDrawScreen(mouseX, mouseY, partialTicks);

        // 检测鼠标悬停并绘制背景高亮
        boolean hovered = FakeBrInterface.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY(), getWidth(), getHeight());
        if (hovered && !dropdownOpen) {
            RenderUtil.drawRect(getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + getHeight(), new Color(70, 70, 70, 100).getRGB());
        }

        // 绘制标签
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
            getLabel(),
            getPosX(),
            getPosY() + 3,
            new Color(229, 229, 223, 255).getRGB()
        );

        // 绘制下拉框
        float dropdownWidth = 80;
        float dropdownX = getPosX() + getWidth() - dropdownWidth;
        float dropdownY = getPosY() + 1;
        float dropdownHeight = getHeight() - 2;

        // 下拉框背景
        RenderUtil.drawRect(dropdownX, dropdownY, dropdownX + dropdownWidth, dropdownY + dropdownHeight,
            new Color(50, 50, 50, 255).getRGB());
        RenderUtil.drawOutlineRect(dropdownX, dropdownY, dropdownWidth, dropdownHeight, 1, 0,
            new Color(100, 100, 100, 255).getRGB());

        // 当前选中的模式文本
        String currentMode = property.getModeString();
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
            currentMode,
            dropdownX + 3,
            dropdownY + 3,
            new Color(0xff99AAFF).getRGB()
        );

        // 下拉箭头
        String arrow = dropdownOpen ? "▲" : "▼";
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
            arrow,
            dropdownX + dropdownWidth - 10,
            dropdownY + 3,
            new Color(200, 200, 200, 255).getRGB()
        );

        // 如果下拉框打开，绘制选项列表
        if (dropdownOpen) {
            float optionHeight = 12;
            float listHeight = modes.length * optionHeight;
            float listY = dropdownY + dropdownHeight;

            // 下拉列表背景
            RenderUtil.drawRect(dropdownX, listY, dropdownX + dropdownWidth, listY + listHeight,
                new Color(45, 45, 45, 255).getRGB());
            RenderUtil.drawOutlineRect(dropdownX, listY, dropdownWidth, listHeight, 1, 0,
                new Color(100, 100, 100, 255).getRGB());

            // 绘制每个选项
            for (int i = 0; i < modes.length; i++) {
                float optionY = listY + i * optionHeight;
                boolean optionHovered = FakeBrInterface.mouseWithinBounds(mouseX, mouseY, dropdownX, optionY, dropdownWidth, optionHeight);
                boolean isSelected = i == property.getValue();

                // 选项背景
                if (optionHovered) {
                    RenderUtil.drawRect(dropdownX, optionY, dropdownX + dropdownWidth, optionY + optionHeight,
                        new Color(70, 70, 70, 255).getRGB());
                } else if (isSelected) {
                    RenderUtil.drawRect(dropdownX, optionY, dropdownX + dropdownWidth, optionY + optionHeight,
                        new Color(60, 60, 60, 255).getRGB());
                }

                // 选项文本
                int textColor = isSelected ? new Color(0xff99AAFF).getRGB() : new Color(200, 200, 200, 255).getRGB();
                Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
                    modes[i],
                    dropdownX + 3,
                    optionY + 2,
                    textColor
                );
            }
        }
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        super.onMouseClicked(mouseX, mouseY, button);

        if (button != 0) return; // 只响应左键

        float dropdownWidth = 80;
        float dropdownX = getPosX() + getWidth() - dropdownWidth;
        float dropdownY = getPosY() + 1;
        float dropdownHeight = getHeight() - 2;

        // 检查是否点击下拉框头部
        if (FakeBrInterface.mouseWithinBounds(mouseX, mouseY, dropdownX, dropdownY, dropdownWidth, dropdownHeight)) {
            dropdownOpen = !dropdownOpen;
            return;
        }

        // 如果下拉框打开，检查是否点击了某个选项
        if (dropdownOpen) {
            float optionHeight = 12;
            float listY = dropdownY + dropdownHeight;

            for (int i = 0; i < modes.length; i++) {
                float optionY = listY + i * optionHeight;
                if (FakeBrInterface.mouseWithinBounds(mouseX, mouseY, dropdownX, optionY, dropdownWidth, optionHeight)) {
                    property.setValue(i);
                    dropdownOpen = false;
                    return;
                }
            }

            // 点击下拉列表外部，关闭下拉框
            dropdownOpen = false;
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        dropdownOpen = false;
    }

    public boolean isDropdownOpen() { return dropdownOpen; }
    public float getDropdownExtraHeight() { return dropdownOpen ? modes.length * 12 : 0; }
}
