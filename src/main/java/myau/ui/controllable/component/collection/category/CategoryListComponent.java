package myau.ui.controllable.component.collection.category;

import myau.module.category.Category;
import myau.ui.controllable.FakeBrInterface;
import myau.ui.controllable.component.Component;
import myau.ui.controllable.window.Window;
import myau.ui.controllable.window.collection.EntrypointWindow;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;

public class CategoryListComponent extends Component {
    private final Category category;
    private final EntrypointWindow window;

    public CategoryListComponent(String label, Category category, final Window window, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(label, posX, posY, offsetX, offsetY, width, height);
        this.category = category;
        this.window = (EntrypointWindow) window;
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        super.onDrawScreen(mouseX, mouseY, partialTicks);
        RenderUtil.enableRenderState();

        RenderUtil.drawRect(getPosX(), getPosY(), getPosX() +  getWidth(), getPosY() + getHeight(), new Color(100, 45, 45, 255).getRGB());

        RenderUtil.disableRenderState();

        RenderUtil.drawRect(getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + getHeight(), 0xFF000000);
        final String name = StringUtils.capitalize(getLabel().toLowerCase());
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(name, getPosX() + (getWidth() / 2f) - (Minecraft.getMinecraft().fontRendererObj.getStringWidth(name) / 2f), getPosY() + (getHeight() / 2) - (Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT / 2f), -1);
    }

    @Override
    public void onMoved(float movedX, float movedY) {
        super.onMoved(movedX, movedY);
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        // 检查鼠标是否在当前组件范围内
        if (FakeBrInterface.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY(), getWidth(), getHeight())) {
            if (button == 0) { // 左键点击
                // 更新 EntrypointWindow 选中的 Category
                window.setSelectedCategory(category);
            }
        }
    }

    public Category getCategory() {
        return category;
    }
}
