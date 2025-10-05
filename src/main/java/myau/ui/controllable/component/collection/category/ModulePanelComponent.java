package myau.ui.controllable.component.collection.category;

import myau.Myau;
import myau.module.Module;
import myau.module.category.Category;
import myau.ui.controllable.component.Component;
import myau.ui.controllable.component.collection.category.impl.ModuleComponent;
import myau.ui.controllable.window.collection.EntrypointWindow;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class ModulePanelComponent extends Component {
    private final EntrypointWindow window;
    private Category currentCategory = Category.COMBAT;
    private float scrollOffset = 0; // 滚动偏移量

    public ModulePanelComponent(EntrypointWindow window, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super("ModulePanel", posX, posY, offsetX, offsetY, width, height);
        this.window = window;
    }

    @Override
    public void initialize() {
        super.initialize();
        loadModules(currentCategory);
    }

    /**
     * 加载指定分类的模块（瀑布流布局）
     */
    public void loadModules(Category category) {
        this.currentCategory = category;
        getSubComponents().clear();
        scrollOffset = 0; // 重置滚动偏移

        float minCardHeight = 30; // 最小卡片高度（会根据属性数量自动增加）
        float padding = 5; // 卡片之间的间距
        float marginLeft = 5; // 左边距
        float marginTop = 5; // 上边距

        // 收集所有符合分类的模块，并分为有设置和无设置两组
        java.util.List<Module> modulesWithSettings = new java.util.ArrayList<>();
        java.util.List<Module> modulesWithoutSettings = new java.util.ArrayList<>();

        for (Module module : Myau.moduleManager.modules.values()) {
            if (module.category() == category) {
                if (Myau.propertyManager.properties.containsKey(module.getClass()) &&
                    !Myau.propertyManager.properties.get(module.getClass()).isEmpty()) {
                    modulesWithSettings.add(module);
                } else {
                    modulesWithoutSettings.add(module);
                }
            }
        }

        // 先创建所有ModuleComponent并计算它们的宽度
        java.util.List<ModuleComponent> moduleComponents = new java.util.ArrayList<>();
        for (Module module : modulesWithSettings) {
            ModuleComponent moduleComponent = new ModuleComponent(
                module,
                0, 0,
                0, 0,
                150, minCardHeight
            );
            moduleComponent.initialize();
            moduleComponents.add(moduleComponent);
        }

        // 使用瀑布流算法布局 - 动态列数
        layoutWithMasonryAlgorithm(moduleComponents, marginLeft, marginTop, padding);

        // 添加到子组件列表
        for (ModuleComponent component : moduleComponents) {
            getSubComponents().add(component);
        }

        // 如果有无设置的模块，创建 "No Settings" 区域
        if (!modulesWithoutSettings.isEmpty()) {
            // 找到最大的Y位置
            float maxY = marginTop;
            for (ModuleComponent comp : moduleComponents) {
                float compBottom = comp.getOffsetY() + comp.getHeight();
                if (compBottom > maxY) {
                    maxY = compBottom;
                }
            }

            float currentX = marginLeft;
            float currentY = maxY + padding * 2 + 20; // 增加间距 + 标题高度

            for (Module module : modulesWithoutSettings) {
                ModuleComponent moduleComponent = new ModuleComponent(
                    module,
                    0, 0,
                    currentX, currentY,
                    150, minCardHeight
                );
                moduleComponent.initialize();

                // 检查是否需要换行
                if (currentX > marginLeft && currentX + moduleComponent.getWidth() > this.getWidth() - marginLeft) {
                    currentX = marginLeft;
                    currentY += minCardHeight + padding;
                }

                moduleComponent.setOffsetX(currentX);
                moduleComponent.setOffsetY(currentY);
                moduleComponent.onMoved(getPosX(), getPosY());
                getSubComponents().add(moduleComponent);

                currentX += moduleComponent.getWidth() + padding;
            }
        }
    }

    /**
     * 瀑布流布局算法 - 动态列数
     */
    private void layoutWithMasonryAlgorithm(java.util.List<ModuleComponent> components, float marginLeft, float marginTop, float padding) {
        if (components.isEmpty()) return;

        // 计算平均卡片宽度来决定列数
        float totalWidth = 0;
        for (ModuleComponent comp : components) {
            totalWidth += comp.getWidth();
        }
        float avgWidth = totalWidth / components.size();
        
        // 根据平均宽度计算合理的列数
        float availableWidth = this.getWidth() - marginLeft * 2;
        int columns = Math.max(1, (int)(availableWidth / (avgWidth + padding)));
        
        // 创建列高度数组
        float[] columnHeights = new float[columns];
        float[] columnXPositions = new float[columns];
        
        // 初始化列的X位置（均匀分布）
        float columnWidth = availableWidth / columns;
        for (int i = 0; i < columns; i++) {
            columnHeights[i] = marginTop;
            columnXPositions[i] = marginLeft + i * columnWidth;
        }

        // 为每个组件找到最适合的列
        for (ModuleComponent component : components) {
            // 找到最短的列
            int shortestColumn = 0;
            float minHeight = columnHeights[0];
            for (int i = 1; i < columns; i++) {
                if (columnHeights[i] < minHeight) {
                    minHeight = columnHeights[i];
                    shortestColumn = i;
                }
            }

            // 放置组件
            float offsetX = columnXPositions[shortestColumn];
            float offsetY = columnHeights[shortestColumn];

            component.setOffsetX(offsetX);
            component.setOffsetY(offsetY);
            component.onMoved(getPosX(), getPosY());

            // 更新该列的高度
            columnHeights[shortestColumn] += component.getHeight() + padding;
        }
    }

    /**
     * 更新所有模块组件的宽度和位置（瀑布流/砌砖布局 + No Settings 区域）
     */
    public void updateModuleWidths() {
        float cardWidth = 100;
        float padding = 5;
        float marginLeft = 5;
        float marginTop = 5;

        int cardsPerRow = (int) ((this.getWidth() - marginLeft) / (cardWidth + padding));
        if (cardsPerRow < 1) cardsPerRow = 1;

        // 分离有设置和无设置的模块
        java.util.List<Component> componentsWithSettings = new java.util.ArrayList<>();
        java.util.List<Component> componentsWithoutSettings = new java.util.ArrayList<>();

        for (Component component : getSubComponents()) {
            if (!(component instanceof ModuleComponent)) continue;
            ModuleComponent moduleComp = (ModuleComponent) component;

            if (Myau.propertyManager.properties.containsKey(moduleComp.getModule().getClass()) &&
                !Myau.propertyManager.properties.get(moduleComp.getModule().getClass()).isEmpty()) {
                componentsWithSettings.add(component);
            } else {
                componentsWithoutSettings.add(component);
            }
        }

        // 使用瀑布流布局放置有设置的模块
        float[] columnHeights = new float[cardsPerRow];
        for (int i = 0; i < cardsPerRow; i++) {
            columnHeights[i] = marginTop;
        }

        for (Component component : componentsWithSettings) {
            // 找到最短的列
            int shortestColumn = 0;
            float minHeight = columnHeights[0];
            for (int i = 1; i < cardsPerRow; i++) {
                if (columnHeights[i] < minHeight) {
                    minHeight = columnHeights[i];
                    shortestColumn = i;
                }
            }

            // 在最短的列中放置卡片
            float offsetX = marginLeft + shortestColumn * (cardWidth + padding);
            float offsetY = columnHeights[shortestColumn];

            component.setOffsetX(offsetX);
            component.setOffsetY(offsetY);
            component.setWidth(cardWidth);
            component.onMoved(getPosX(), getPosY());

            // 更新该列的高度
            columnHeights[shortestColumn] += component.getHeight() + padding;
        }

        // 放置无设置的模块
        if (!componentsWithoutSettings.isEmpty()) {
            float maxColumnHeight = columnHeights[0];
            for (int i = 1; i < cardsPerRow; i++) {
                if (columnHeights[i] > maxColumnHeight) {
                    maxColumnHeight = columnHeights[i];
                }
            }

            float noSettingsSectionY = maxColumnHeight + padding * 2;
            float currentX = marginLeft;
            float currentY = noSettingsSectionY + 20;

            for (Component component : componentsWithoutSettings) {
                if (currentX + cardWidth > this.getWidth() - marginLeft) {
                    currentX = marginLeft;
                    currentY += component.getHeight() + padding;
                }

                component.setOffsetX(currentX);
                component.setOffsetY(currentY);
                component.setWidth(cardWidth);
                component.onMoved(getPosX(), getPosY());

                currentX += cardWidth + padding;
            }
        }
    }

    /**
     * 重新布局所有模块（在窗口大小改变时调用）
     */
    public void relayoutModules() {
        if (getSubComponents().isEmpty()) return;

        float minCardHeight = 30;
        float padding = 5;
        float marginLeft = 5;
        float marginTop = 5;

        // 分离有设置和无设置的模块
        java.util.List<ModuleComponent> modulesWithSettings = new java.util.ArrayList<>();
        java.util.List<ModuleComponent> modulesWithoutSettings = new java.util.ArrayList<>();

        for (Component component : getSubComponents()) {
            if (!(component instanceof ModuleComponent)) continue;
            ModuleComponent moduleComp = (ModuleComponent) component;

            if (Myau.propertyManager.properties.containsKey(moduleComp.getModule().getClass()) &&
                !Myau.propertyManager.properties.get(moduleComp.getModule().getClass()).isEmpty()) {
                modulesWithSettings.add(moduleComp);
            } else {
                modulesWithoutSettings.add(moduleComp);
            }
        }

        // 重新计算每个组件的宽度
        for (ModuleComponent moduleComp : modulesWithSettings) {
            // 清除旧的子组件
            moduleComp.getSubComponents().clear();
            // 重新初始化以计算新的宽度
            moduleComp.initialize();
        }

        for (ModuleComponent moduleComp : modulesWithoutSettings) {
            moduleComp.getSubComponents().clear();
            moduleComp.initialize();
        }

        // 使用瀑布流算法重新布局有设置的模块
        layoutWithMasonryAlgorithm(modulesWithSettings, marginLeft, marginTop, padding);

        // 布局无设置的模块
        if (!modulesWithoutSettings.isEmpty()) {
            // 找到最大的Y位置
            float maxY = marginTop;
            for (ModuleComponent comp : modulesWithSettings) {
                float compBottom = comp.getOffsetY() + comp.getHeight();
                if (compBottom > maxY) {
                    maxY = compBottom;
                }
            }

            float currentX = marginLeft;
            float currentY = maxY + padding * 2 + 20; // 增加间距 + 标题高度

            for (ModuleComponent moduleComponent : modulesWithoutSettings) {
                // 检查是否需要换行
                if (currentX > marginLeft && currentX + moduleComponent.getWidth() > this.getWidth() - marginLeft) {
                    currentX = marginLeft;
                    currentY += moduleComponent.getHeight() + padding;
                }

                moduleComponent.setOffsetX(currentX);
                moduleComponent.setOffsetY(currentY);
                moduleComponent.onMoved(getPosX(), getPosY());

                currentX += moduleComponent.getWidth() + padding;
            }
        }

        // 重置滚动偏移
        scrollOffset = 0;
    }

    @Override
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        super.onDrawScreen(mouseX, mouseY, partialTicks);

        RenderUtil.enableRenderState();
        // 绘制面板背景
        RenderUtil.drawRect(getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + getHeight(), new Color(40, 40, 40, 255).getRGB());
        RenderUtil.disableRenderState();

        // 启用裁剪，防止卡片溢出
        enableScissor(getPosX(), getPosY(), getWidth(), getHeight());

        // 应用滚动偏移
        GL11.glPushMatrix();
        GL11.glTranslatef(0, -scrollOffset, 0);

        // 只绘制有设置的模块（受滚动控制）
        getSubComponents().forEach(component -> {
            if (component instanceof ModuleComponent) {
                ModuleComponent moduleComp = (ModuleComponent) component;
                // 只绘制有设置的模块
                if (Myau.propertyManager.properties.containsKey(moduleComp.getModule().getClass()) &&
                    !Myau.propertyManager.properties.get(moduleComp.getModule().getClass()).isEmpty()) {
                    component.onDrawScreen(mouseX, (int)(mouseY + scrollOffset), partialTicks);
                }
            }
        });

        GL11.glPopMatrix();

        // 禁用裁剪
        disableScissor();

        // 绘制 "No Settings" 区域（不受滚动控制，固定在底部）
        drawNoSettingsSection(mouseX, mouseY, partialTicks);
    }

    /**
     * 绘制固定在底部的 "No Settings" 区域
     */
    private void drawNoSettingsSection(int mouseX, int mouseY, float partialTicks) {
        // 检查是否有无设置的模块
        java.util.List<Component> noSettingsComponents = new java.util.ArrayList<>();
        for (Component component : getSubComponents()) {
            if (component instanceof ModuleComponent) {
                ModuleComponent moduleComp = (ModuleComponent) component;
                if (!Myau.propertyManager.properties.containsKey(moduleComp.getModule().getClass()) ||
                    Myau.propertyManager.properties.get(moduleComp.getModule().getClass()).isEmpty()) {
                    noSettingsComponents.add(component);
                }
            }
        }

        if (noSettingsComponents.isEmpty()) return;

        // 计算 No Settings 区域的高度
        float cardHeight = 23; // 无设置模块的固定高度
        float padding = 5;
        float marginLeft = 5;
        float titleHeight = 20;
        float cardWidth = 100;

        int cardsPerRow = (int) ((this.getWidth() - marginLeft) / (cardWidth + padding));
        if (cardsPerRow < 1) cardsPerRow = 1;

        int rows = (int) Math.ceil((double) noSettingsComponents.size() / cardsPerRow);
        float noSettingsSectionHeight = titleHeight + rows * (cardHeight + padding) + padding;

        // No Settings 区域固定在面板底部
        float noSettingsSectionY = getPosY() + getHeight() - noSettingsSectionHeight;

        // 绘制 No Settings 区域背景（略深一点）
        RenderUtil.enableRenderState();
        RenderUtil.drawRect(getPosX(), noSettingsSectionY, getPosX() + getWidth(), getPosY() + getHeight(),
                          new Color(35, 35, 35, 255).getRGB());

        // 绘制分割线
        RenderUtil.drawRect(getPosX() + marginLeft, noSettingsSectionY,
                          getPosX() + getWidth() - marginLeft, noSettingsSectionY + 1,
                          new Color(60, 60, 60, 255).getRGB());
        RenderUtil.disableRenderState();

        // 绘制 "No Settings" 标题
        String title = "No Settings";
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
            title,
            getPosX() + marginLeft,
            noSettingsSectionY + 5,
            new Color(150, 150, 150, 255).getRGB()
        );

        // 启用裁剪（仅针对 No Settings 区域）
        enableScissor(getPosX(), noSettingsSectionY + titleHeight, getWidth(), noSettingsSectionHeight - titleHeight);

        // 绘制无设置的模块卡片
        float currentX = getPosX() + marginLeft;
        float currentY = noSettingsSectionY + titleHeight;

        for (Component component : noSettingsComponents) {
            if (currentX + cardWidth > getPosX() + getWidth() - marginLeft) {
                currentX = getPosX() + marginLeft;
                currentY += cardHeight + padding;
            }

            // 临时设置组件位置并绘制
            float originalPosX = component.getPosX();
            float originalPosY = component.getPosY();

            component.setPosX(currentX);
            component.setPosY(currentY);
            component.onDrawScreen(mouseX, mouseY, partialTicks);

            // 恢复原始位置
            component.setPosX(originalPosX);
            component.setPosY(originalPosY);

            currentX += cardWidth + padding;
        }

        // 禁用裁剪
        disableScissor();
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        super.onMouseClicked(mouseX, mouseY, button);
        // 传递给子组件（考虑滚动偏移）
        int adjustedMouseY = (int)(mouseY + scrollOffset);
        getSubComponents().forEach(component -> component.onMouseClicked(mouseX, adjustedMouseY, button));
    }

    /**
     * 处理鼠标滚轮事件
     */
    public void handleMouseWheel(int mouseX, int mouseY, int wheel) {
        // 检查鼠标是否在面板范围内
        if (mouseX >= getPosX() && mouseX <= getPosX() + getWidth() &&
            mouseY >= getPosY() && mouseY <= getPosY() + getHeight()) {

            float scrollSpeed = 20f; // 滚动速度
            scrollOffset -= wheel > 0 ? scrollSpeed : -scrollSpeed;

            // 计算最大滚动偏移
            float maxScroll = calculateTotalHeight() - getHeight();
            if (maxScroll < 0) maxScroll = 0;

            // 限制滚动范围
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        }
    }

    /**
     * 计算所有卡片的总高度
     */
    private float calculateTotalHeight() {
        if (getSubComponents().isEmpty()) return 0;

        float maxY = 0;
        for (Component component : getSubComponents()) {
            float componentBottom = component.getOffsetY() + component.getHeight();
            if (componentBottom > maxY) {
                maxY = componentBottom;
            }
        }
        return maxY + 5; // 加上底部边距
    }

    /**
     * 启用 OpenGL 裁剪
     */
    private void enableScissor(float x, float y, float width, float height) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int scale = sr.getScaleFactor();

        // OpenGL 的坐标系是从左下角开始的，需要转换
        int scissorX = (int)(x * scale);
        int scissorY = (int)(Minecraft.getMinecraft().displayHeight - (y + height) * scale);
        int scissorWidth = (int)(width * scale);
        int scissorHeight = (int)(height * scale);

        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    /**
     * 禁用 OpenGL 裁剪
     */
    private void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public float getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(float scrollOffset) {
        this.scrollOffset = scrollOffset;
    }
}
