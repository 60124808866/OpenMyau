package myau.ui.controllable;

import myau.ui.controllable.window.Window;
import myau.ui.controllable.window.collection.ConfigurationWindow;
import myau.ui.controllable.window.collection.EntrypointWindow;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;

public class FakeBrInterface extends GuiScreen {
    private final ArrayList<Window> chuanghu = new ArrayList<>();
    private static FakeBrInterface INSTANCE; // singleton reference

    public FakeBrInterface() {
        INSTANCE = this;
    }

    public static FakeBrInterface getInstance() { return INSTANCE; }


    public void initialize() {
        int x = 50;
        int y = 2;
        chuanghu.add(new EntrypointWindow(x, y, 313,325));
        chuanghu.add(new ConfigurationWindow("浙江省杭州市", x + 325, y, 213,225));
        chuanghu.forEach(Window::initialize);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        chuanghu.forEach(frame -> frame.onScreenDraw(mouseX,mouseY,partialTicks));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int wheel = Mouse.getEventDWheel();

        if (wheel != 0) {
            chuanghu.forEach(frame -> frame.onMouseWheel(mouseX, mouseY, wheel));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        chuanghu.forEach(frame -> frame.onKeyTyped(typedChar, keyCode));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        chuanghu.forEach(frame -> frame.onMouseClicked(mouseX,mouseY,mouseButton));
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        chuanghu.forEach(frame -> frame.onMouseReleased(mouseX,mouseY,mouseButton));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public static boolean mouseWithinBounds(float mouseX, float mouseY, float x,float y,float width,float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
