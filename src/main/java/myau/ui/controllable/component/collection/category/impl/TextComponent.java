package myau.ui.controllable.component.collection.category.impl;

import myau.property.properties.TextProperty;
import myau.ui.controllable.FakeBrInterface;
import myau.ui.controllable.component.Component;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.awt.*;

public class TextComponent extends Component {
    private final TextProperty property;
    private boolean editing = false;
    private String tempValue = "";

    public TextComponent(final TextProperty property, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(property.getName(), posX, posY, offsetX, offsetY, width, height);
        this.property = property;
        this.tempValue = property.getValue();
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
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
            getLabel(),
            getPosX(),
            getPosY() + 3,
            new Color(229, 229, 223, 255).getRGB()
        );

        // 绘制文本框
        float textBoxX = getPosX() + getWidth() - 45;
        float textBoxWidth = 43;
        RenderUtil.drawRect(textBoxX, getPosY() + 1, textBoxX + textBoxWidth, getPosY() + getHeight() - 1,
            editing ? new Color(70, 70, 70, 255).getRGB() : new Color(50, 50, 50, 255).getRGB());
        RenderUtil.drawOutlineRect(textBoxX, getPosY() + 1, textBoxWidth, getHeight() - 2, 1, 0,
            editing ? new Color(0xff689FFF).getRGB() : new Color(100, 100, 100, 255).getRGB());

        // 绘制值
        String displayText = editing ? tempValue + "_" : property.getValue();
        if (Minecraft.getMinecraft().fontRendererObj.getStringWidth(displayText) > textBoxWidth - 4) {
            displayText = displayText.substring(Math.max(0, displayText.length() - 6)) + "...";
        }
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
            displayText,
            textBoxX + 2,
            getPosY() + 3,
            new Color(255, 255, 255, 255).getRGB()
        );
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        super.onMouseClicked(mouseX, mouseY, button);
        float textBoxX = getPosX() + getWidth() - 45;
        if (button == 0 && FakeBrInterface.mouseWithinBounds(mouseX, mouseY, textBoxX, getPosY(), 43, getHeight())) {
            editing = !editing;
            if (editing) {
                tempValue = "";
            } else {
                if (!tempValue.isEmpty()) {
                    property.setValue(tempValue);
                }
            }
        } else if (editing) {
            editing = false;
            if (!tempValue.isEmpty()) {
                property.setValue(tempValue);
            }
        }
    }

    @Override
    public void onKeyTyped(char character, int keyCode) {
        super.onKeyTyped(character, keyCode);
        if (editing) {
            if (keyCode == Keyboard.KEY_RETURN) {
                editing = false;
                if (!tempValue.isEmpty()) {
                    property.setValue(tempValue);
                }
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!tempValue.isEmpty()) {
                    tempValue = tempValue.substring(0, tempValue.length() - 1);
                }
            } else if (Character.isLetterOrDigit(character) || character == ' ' || character == '_' || character == '-') {
                tempValue += character;
            }
        }
    }
}
