package myau.ui.controllable.component.collection.configuration;

import myau.config.Config;
import myau.ui.controllable.FakeBrInterface;
import myau.ui.controllable.component.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.awt.*;

public class ConfigurationComponent extends Component {
    private final ConfigurationWindowBridge parent; // bridge interface to avoid circular dep compile issues if needed
    private final String configName;
    private final float baseOffsetY; // original offset within list (before scroll)
    private long lastClickTime;

    public ConfigurationComponent(String configName, ConfigurationWindowBridge parent, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(configName, posX, posY, offsetX, offsetY, width, height);
        this.parent = parent;
        this.configName = configName;
        this.baseOffsetY = offsetY;
    }

    public float getBaseOffsetY() {
        return baseOffsetY;
    }

    @Override
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        super.onDrawScreen(mouseX, mouseY, partialTicks);
        if (isHidden()) return;
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        boolean hovered = FakeBrInterface.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY(), getWidth(), getHeight());
        boolean selected = parent != null && parent.getSelectedConfigName() != null && parent.getSelectedConfigName().equalsIgnoreCase(configName);

        Color bg;
        if (selected) {
            bg = hovered ? new Color(80, 110, 80, 255) : new Color(70, 100, 70, 255);
        } else {
            bg = hovered ? new Color(60, 60, 60, 255) : new Color(50, 50, 50, 255);
        }
        myau.util.RenderUtil.enableRenderState();
        myau.util.RenderUtil.drawRect(getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + getHeight(), bg.getRGB());
        // bottom separator
        myau.util.RenderUtil.drawRect(getPosX(), getPosY() + getHeight() - 1, getPosX() + getWidth(), getPosY() + getHeight(), new Color(40, 40, 40, 255).getRGB());
        myau.util.RenderUtil.disableRenderState();
        fr.drawStringWithShadow(configName, getPosX() + 4, getPosY() + (getHeight() / 2f) - (fr.FONT_HEIGHT / 2f), 0xFFFFFFFF);
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return;
        if (!FakeBrInterface.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY(), getWidth(), getHeight())) return;
        long now = System.currentTimeMillis();
        parent.onConfigSelected(configName);
        if (now - lastClickTime < 400) { // double click
            new Config(configName, false).load();
        }
        lastClickTime = now;
    }

    // bridge interface implemented by window
    public interface ConfigurationWindowBridge {
        void onConfigSelected(String name);
        String getSelectedConfigName();
    }
}
