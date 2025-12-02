
package myau.ui.components;

import myau.enums.ChatColors;
import myau.property.properties.BooleanProperty;
import myau.property.properties.TextProperty;
import myau.ui.ClickGui;
import myau.ui.Component;
import myau.ui.callback.GuiInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TextComponent implements Component {
    private final TextProperty property;
    private final ModuleComponent module;
    private int offsetY;
    private int x;
    private int y;

    public TextComponent(TextProperty property, ModuleComponent parentModule, int offsetY) {
        this.property = property;
        this.module = parentModule;
        this.x = parentModule.category.getX() + parentModule.category.getWidth();
        this.y = parentModule.category.getY() + parentModule.offsetY;
        this.offsetY = offsetY;
    }


    public void draw(AtomicInteger offset) {
        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        Minecraft.getMinecraft().fontRendererObj.drawString(this.property.getName().replace("-", " ") + ": " + ChatColors.formatColor(this.property.formatValue()), (float) ((this.module.category.getX() + 4) * 2), (float) ((this.module.category.getY() + this.offsetY + 5) * 2), -1, false);
        GL11.glPopMatrix();
    }

    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return 12;
    }

    public void update(int mousePosX, int mousePosY) {
        this.y = this.module.category.getY() + this.offsetY;
        this.x = this.module.category.getX();
    }

    public void mouseDown(int x, int y, int button) {
        if (this.isHovered(x, y) && button == 0 && this.module.panelExpand) {
            GuiInput.prompt(property.getName().replace("-", " "), property.getValue(), property::setValue, ClickGui.getInstance());
        }
    }

    @Override
    public void mouseReleased(int x, int y, int button) {

    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {

    }

    public boolean isHovered(int x, int y) {
        return x > this.x && x < this.x + this.module.category.getWidth() && y > this.y && y < this.y + 11;
    }


    @Override
    public boolean isVisible() {
        return property.isVisible();
    }
}
