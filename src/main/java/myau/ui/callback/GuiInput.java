package myau.ui.callback;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.function.Consumer;

public class GuiInput extends GuiScreen {
    private final String title;
    private final String defaultValue;
    private final Consumer<String> callback;
    private GuiTextField textField;
    private GuiButton buttonOk;
    private GuiScreen caller;

    public GuiInput(String title, String defaultValue, Consumer<String> callback, GuiScreen caller) {
        this.title = title;
        this.defaultValue = defaultValue;
        this.callback = callback;
        this.caller = caller;
    }

    public static void prompt(String title, String defaultValue, Consumer<String> callback, GuiScreen caller) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiInput(title,defaultValue, callback, caller));
    }

    @Override
    public void initGui() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        textField = new GuiTextField(0, this.fontRendererObj, centerX - 100, centerY - 10, 200, 20);
        textField.setText(defaultValue);
        textField.setFocused(true);

        this.buttonList.add(buttonOk = new GuiButton(0, centerX - 100, centerY + 20, 95, 20, "Confirm"));
        this.buttonList.add(new GuiButton(1, centerX + 5, centerY + 20, 95, 20, "Cancel"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == buttonOk) {
            if (callback != null) callback.accept(textField.getText());
        }
        this.mc.displayGuiScreen(caller);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        textField.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            actionPerformed(buttonOk);
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            actionPerformed(null);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        try {
            super.mouseClicked(mouseX,mouseY,mouseButton);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        textField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, title, width / 2, height / 2 - 35, 0xFFFFFF);
        textField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void updateScreen() {
        textField.updateCursorCounter();
    }
}
