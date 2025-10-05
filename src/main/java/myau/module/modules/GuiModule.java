package myau.module.modules;

import myau.module.Module;
import myau.module.category.Category;
import myau.ui.ClickGui;
import myau.ui.controllable.FakeBrInterface;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class GuiModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private FakeBrInterface clickGui;

    public GuiModule() {
        super("ClickGui", false);
        setKey(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        clickGui = new FakeBrInterface();
        clickGui.initialize();
        mc.displayGuiScreen(clickGui);
    }

    @Override
    public Category category() {
        return Category.RENDER;
    }
}
