package myau.ui.controllable.component.collection.category.impl;

import myau.Myau;
import myau.module.Module;
import myau.property.properties.*;
import myau.ui.controllable.component.Component;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;

public class ModuleComponent extends Component {
    private final Module module;

    public ModuleComponent(final Module module, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(StringUtils.capitalize(module.getName().toLowerCase()), posX, posY, offsetX, offsetY, width, height);
        this.module = module;
    }

    @Override
    public void initialize() {
        super.initialize();

        float minWidth = 150; // 最小宽度
        float calculatedWidth = minWidth;

        // 计算模块标题所需的宽度
        String displayName = StringUtils.capitalize(module.getName().toLowerCase());
        float titleWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(displayName) + 20; // 加上边距
        calculatedWidth = Math.max(calculatedWidth, titleWidth);

        // 添加模块的属性组件
        if (Myau.propertyManager.properties.containsKey(module.getClass())) {
            float propertyOffsetY = 18; // 从标题下方开始
            float propertyHeight = 15; // 每个属性的高度

            // 先计算所有属性所需的宽度
            for (myau.property.Property<?> property : Myau.propertyManager.properties.get(module.getClass())) {
                // 计算标签宽度
                float labelWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(property.getName());

                // 计算值的宽度
                float valueWidth = 0;
                if (property instanceof BooleanProperty) {
                    valueWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth("OFF") + 18; // OFF文本 + 复选框
                } else if (property instanceof FloatProperty) {
                    valueWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth("999.99") + 5;
                } else if (property instanceof IntProperty) {
                    IntProperty intProp = (IntProperty) property;
                    valueWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(String.valueOf(intProp.getMaximum())) + 5;
                } else if (property instanceof PercentProperty) {
                    valueWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth("100%") + 5;
                } else if (property instanceof ModeProperty) {
                    ModeProperty modeProp = (ModeProperty) property;
                    String longestMode = "";
                    for (String mode : modeProp.getValuePrompt().split(", ")) {
                        if (mode.length() > longestMode.length()) {
                            longestMode = mode;
                        }
                    }
                    valueWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(longestMode) + 5;
                } else if (property instanceof TextProperty) {
                    valueWidth = 50; // 文本框固定宽度
                } else if (property instanceof ColorProperty) {
                    valueWidth = 20; // 颜色预览框宽度
                }

                // 计算这个属性需要的总宽度（标签 + 值 + 边距）
                float requiredWidth = labelWidth + valueWidth + 20; // 加上左右边距
                calculatedWidth = Math.max(calculatedWidth, requiredWidth);
            }

            // 设置计算出的宽度
            setWidth(calculatedWidth);

            for (myau.property.Property<?> property : Myau.propertyManager.properties.get(module.getClass())) {
                Component propertyComponent = null;

                if (property instanceof BooleanProperty) {
                    propertyComponent = new BooleanComponent((BooleanProperty) property, 0, 0, 5, propertyOffsetY, getWidth() - 10, propertyHeight);
                } else if (property instanceof FloatProperty) {
                    propertyComponent = new FloatComponent((FloatProperty) property, 0, 0, 5, propertyOffsetY, getWidth() - 10, propertyHeight);
                } else if (property instanceof IntProperty) {
                    propertyComponent = new IntComponent((IntProperty) property, 0, 0, 5, propertyOffsetY, getWidth() - 10, propertyHeight);
                } else if (property instanceof PercentProperty) {
                    propertyComponent = new PercentComponent((PercentProperty) property, 0, 0, 5, propertyOffsetY, getWidth() - 10, propertyHeight);
                } else if (property instanceof ModeProperty) {
                    propertyComponent = new ModeComponent((ModeProperty) property, 0, 0, 5, propertyOffsetY, getWidth() - 10, propertyHeight);
                } else if (property instanceof TextProperty) {
                    propertyComponent = new TextComponent((TextProperty) property, 0, 0, 5, propertyOffsetY, getWidth() - 10, propertyHeight);
                } else if (property instanceof ColorProperty) {
                    propertyComponent = new ColorComponent((ColorProperty) property, 0, 0, 5, propertyOffsetY, getWidth() - 10, propertyHeight);
                    propertyHeight = 35; // ColorComponent 需要更多空间
                }

                if (propertyComponent != null) {
                    propertyComponent.onMoved(getPosX(), getPosY());
                    getSubComponents().add(propertyComponent);
                    propertyOffsetY += propertyComponent.getHeight(); // 使用实际组件高度
                    propertyHeight = 15; // 重置为默认高度
                }
            }

            // 根据属性数量调整卡片高度
            float titleHeight = 18; // 标题区域高度
            float totalPropertiesHeight = 0;
            for (Component comp : getSubComponents()) {
                totalPropertiesHeight += comp.getHeight();
            }
            float padding = 5; // 底部留白
            setHeight(titleHeight + totalPropertiesHeight + padding);
        } else {
            // 没有属性的模块，设置宽度并只保留标题高度
            setWidth(calculatedWidth);
            setHeight(23); // 18 (标题) + 5 (底部留白)
        }
    }

    private void updateDynamicLayout() {
        float addedOffset = 0f;
        float totalBaseHeights = 0f;
        float totalDropdownExtra = 0f;
        for (Component comp : getSubComponents()) {
            // base height accumulation
            totalBaseHeights += comp.getHeight();
            // set new offsetY based on original + previously accumulated extra
            comp.setOffsetY(comp.getOriginalOffsetY() + addedOffset);
            comp.onMoved(getPosX(), getPosY());
            if (comp instanceof ModeComponent) {
                ModeComponent mc = (ModeComponent) comp;
                if (mc.isDropdownOpen()) {
                    float extra = mc.getDropdownExtraHeight();
                    addedOffset += extra; // push following components downward
                    totalDropdownExtra += extra;
                }
            }
        }
        float titleHeight = 18f;
        float padding = 5f;
        setHeight(titleHeight + totalBaseHeights + totalDropdownExtra + padding);
    }

    @Override
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        updateDynamicLayout();
        super.onDrawScreen(mouseX, mouseY, partialTicks);

        RenderUtil.enableRenderState();

        // 检查鼠标是否悬停在卡片上
        boolean hovered = mouseX >= getPosX() && mouseX <= getPosX() + getWidth() &&
                         mouseY >= getPosY() && mouseY <= getPosY() + getHeight();

        // 绘制模块卡片背景（带悬停效果）
        Color bgColor;
        if (module.isEnabled()) {
            bgColor = hovered ? new Color(70, 70, 70, 255) : new Color(60, 60, 60, 255);
        } else {
            bgColor = hovered ? new Color(55, 55, 55, 255) : new Color(45, 45, 45, 255);
        }
        RenderUtil.drawRect(getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + getHeight(), bgColor.getRGB());

        // 绘制标题区域背景（稍微深一点）
        Color titleBgColor = module.isEnabled() ? new Color(50, 50, 50, 255) : new Color(40, 40, 40, 255);
        RenderUtil.drawRect(getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + 18, titleBgColor.getRGB());

        // 绘制顶部边框（高亮显示启用状态）
        if (module.isEnabled()) {
            RenderUtil.drawRect(getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + 2, new Color(100, 200, 100, 255).getRGB());
        } else {
            RenderUtil.drawRect(getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + 1, new Color(70, 70, 70, 255).getRGB());
        }

        // 绘制标题区域底部分割线
        RenderUtil.drawRect(getPosX(), getPosY() + 18, getPosX() + getWidth(), getPosY() + 19, new Color(30, 30, 30, 255).getRGB());

        RenderUtil.disableRenderState();

        // 绘制模块名称（在标题区域居中）
        String displayName = StringUtils.capitalize(module.getName().toLowerCase());
        int textColor = module.isEnabled() ? 0xFFFFFFFF : 0xFF888888;
        float textX = getPosX() + (getWidth() / 2f) - (Minecraft.getMinecraft().fontRendererObj.getStringWidth(displayName) / 2f);
        float textY = getPosY() + 5; // 距离顶部5像素
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
            displayName,
            textX,
            textY,
            textColor
        );

        // 绘制子组件（属性）
        getSubComponents().forEach(component -> component.onDrawScreen(mouseX, mouseY, partialTicks));
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        // 先更新布局，确保点击判定使用最新位置
        updateDynamicLayout();
        // 检查是否点击了标题区域
        if (mouseX >= getPosX() && mouseX <= getPosX() + getWidth() &&
            mouseY >= getPosY() && mouseY <= getPosY() + 18) {
            if (button == 0) { // 左键切换模块状态
                module.toggle();
            }
        }

        // 传递给子组件
        getSubComponents().forEach(component -> component.onMouseClicked(mouseX, mouseY, button));
        // 子组件可能改变了下拉开关状态，再次更新布局
        updateDynamicLayout();
    }

    @Override
    public void onMouseReleased(int mouseX, int mouseY, int button) {
        // 布局保持同步
        updateDynamicLayout();
        super.onMouseReleased(mouseX, mouseY, button);
        // 传递给子组件
        getSubComponents().forEach(component -> component.onMouseReleased(mouseX, mouseY, button));
    }

    @Override
    public void onKeyTyped(char character, int keyCode) {
        super.onKeyTyped(character, keyCode);
        // 传递给子组件
        getSubComponents().forEach(component -> component.onKeyTyped(character, keyCode));
    }

    public Module getModule() {
        return module;
    }
}
