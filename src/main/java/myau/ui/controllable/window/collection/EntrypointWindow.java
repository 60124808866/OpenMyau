package myau.ui.controllable.window.collection;

import myau.module.category.Category;
import myau.ui.controllable.FakeBrInterface;
import myau.ui.controllable.component.Component;
import myau.ui.controllable.component.collection.category.CategoryListComponent;
import myau.ui.controllable.component.collection.category.ModulePanelComponent;
import myau.ui.controllable.window.Window;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.awt.*;

public class EntrypointWindow extends Window {

    public Category selected = Category.COMBAT;

    private int scrollY;
    private final float footerHeight = 14f; // footer区域高度
    private String github = "https://github.com/60124808866/OpenMyau";
    private String footerText = "© 2025 OpenMyau"; // 版权文本

    private int openMyauClickCount = 0; // Easter egg counter
    private static final String EGG_WORD = "OpenMyau";
    private static final int EGG_TRIGGER_COUNT = 3;

    public EntrypointWindow(final float posX, final float posY, final float width, final float height) {
        super("马运", posX, posY, width, height);

    }

    @Override
    public void initialize() {
        super.initialize();

        float offest = 0;
        float categoryOffsetY = 15F; // Category 标签的 Y 偏移
        float categoryHeight = 20F;  // Category 标签的高度
        float gap = 5F; // CategoryListComponent 和 ModulePanelComponent 之间的间隙

        for (Category value : Category.values()) {
            getComponents().add(new CategoryListComponent(value.name(), value, this, 0, 0, offest, categoryOffsetY, this.getWidth() / Category.values().length, categoryHeight));
            offest += this.getWidth() / Category.values().length;
        }

        float modulePanelOffsetY = categoryOffsetY + categoryHeight + gap;
        float usableHeight = this.getHeight() - modulePanelOffsetY - footerHeight;
        getComponents().add(new ModulePanelComponent(this, 0, 0, 0, modulePanelOffsetY, this.getWidth(), usableHeight));

        onWindowMoved(getPosX(), getPosY());
    }

    @Override
    public void onScreenDraw(int mouseX, int mouseY, float partialTicks) {
        super.onScreenDraw(mouseX, mouseY, partialTicks);

        // 绘制footer版权声明
        drawFooter();
    }

    private void drawFooter() {
        // Footer 区域
        float footerTop = getPosY() + getHeight() - footerHeight;

        RenderUtil.enableRenderState();
        RenderUtil.drawRect(getPosX(), footerTop, getPosX() + getWidth(), getPosY() + getHeight(), new Color(38,38,38,255).getRGB());
        // Footer 顶部分割线
        RenderUtil.drawRect(getPosX(), footerTop, getPosX() + getWidth(), footerTop + 1, new Color(30,30,30,255).getRGB());
        RenderUtil.disableRenderState();

        // Footer 文本居中(右对齐偏移 5f)
        final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        int footerWidth = fontRenderer.getStringWidth(footerText);
        float footerTextX = getPosX() + (getWidth() - footerWidth) - 5f;
        float footerTextY = footerTop + (footerHeight - 8) / 2f; // 8 ~ 字体高度近似
        fontRenderer.drawStringWithShadow(footerText, footerTextX, footerTextY, 0xFFAAAAAA);
        fontRenderer.drawStringWithShadow(github, getPosX(), footerTextY, 0xFFAAAAAA);
        // 计算彩蛋点击区域矩形 (仅包含 EGG_WORD)
        int idx = footerText.indexOf(EGG_WORD);
        if (idx >= 0) {
            String prefix = footerText.substring(0, idx);
            int prefixW = fontRenderer.getStringWidth(prefix);
            int eggW = fontRenderer.getStringWidth(EGG_WORD);
            eggRegionX1 = footerTextX + prefixW;
            eggRegionX2 = eggRegionX1 + eggW;
            eggRegionY1 = footerTextY;
            eggRegionY2 = footerTextY + fontRenderer.FONT_HEIGHT;
        } else {
            eggRegionX1 = eggRegionX2 = eggRegionY1 = eggRegionY2 = -1; // disable
        }
    }
    // 彩蛋区域坐标缓存
    private float eggRegionX1, eggRegionX2, eggRegionY1, eggRegionY2;

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        super.onMouseClicked(mouseX, mouseY, button);
        if (button == 0 && eggRegionX1 != -1) {
            if (mouseX >= eggRegionX1 && mouseX <= eggRegionX2 && mouseY >= eggRegionY1 && mouseY <= eggRegionY2) {
                openMyauClickCount++;
                if (openMyauClickCount >= EGG_TRIGGER_COUNT) {
                    openMyauClickCount = 0; // reset
                    github = "ClickGUI By @zcychan & @DextromethorphanMC";
                }
            } else {
                // 点击外部可选择重置（可选）
                // openMyauClickCount = 0;
            }
        }
    }

    @Override
    public void onMouseReleased(int mouseX, int mouseY, int button) {
        super.onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onWindowResized() {
        super.onWindowResized();
        float componentWidth = this.getWidth() / Category.values().length;
        float offset = 0;
        float categoryOffsetY = 15F;
        float categoryHeight = 20F;
        float gap = 5F; // CategoryListComponent 和 ModulePanelComponent 之间的间隙

        for (Component component : getComponents()) {
            if (component instanceof CategoryListComponent) {
                component.setOffsetX(offset);
                component.setWidth(componentWidth);
                component.onMoved(getPosX(), getPosY());
                offset += componentWidth;
            } else if (component instanceof ModulePanelComponent) {
                float modulePanelOffsetY = categoryOffsetY + categoryHeight + gap;
                float usableHeight = this.getHeight() - modulePanelOffsetY - footerHeight;
                component.setOffsetY(modulePanelOffsetY);
                component.setWidth(this.getWidth());
                component.setHeight(usableHeight);
                component.onMoved(getPosX(), getPosY());
                // 重新布局所有模块以适应新的面板大小
                ((ModulePanelComponent) component).relayoutModules();
            }
        }
    }

    public float getFooterHeight() {
        return footerHeight;
    }

    public void setFooterText(String text) {
        this.footerText = text;
    }

    /**
     * 设置选中的分类并更新模块面板
     */
    public void setSelectedCategory(Category category) {
        this.selected = category;
        // 找到 ModulePanelComponent 并重新加载模块
        for (Component component : getComponents()) {
            if (component instanceof ModulePanelComponent) {
                ((ModulePanelComponent) component).loadModules(category);
                break;
            }
        }
    }

    private float calculateNextOffsetY() {
        float totalHeight = 0;
        for (Component component : getComponents()) {
            totalHeight += component.getHeight();
        }
        return totalHeight;
    }
}
