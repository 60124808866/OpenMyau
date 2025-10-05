package myau.ui.controllable.window.collection;

import myau.config.Config;
import myau.ui.controllable.FakeBrInterface;
import myau.ui.controllable.component.collection.configuration.ConfigurationComponent;
import myau.ui.controllable.window.Window;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConfigurationWindow extends Window implements ConfigurationComponent.ConfigurationWindowBridge {

    private final List<ConfigurationComponent> configEntries = new ArrayList<>();
    private String selectedConfigName;
    private float listWidth = 180f; // left panel width (dynamic after recalc)
    private float entryHeight = 18f;
    private float listInnerPadding = 4f;
    private float scrollOffset; // positive -> move up
    private float maxScroll;
    private boolean creatingNameMode = false;
    private String creatingBuffer = "";
    private boolean visualCheckbox = false; // for sketch's save visual checkbox

    // button layout (dynamic after recalc)
    private float buttonWidth = 150f;
    private float buttonHeight = 30f;
    private float buttonGap = 15f;
    // layout helpers
    private final float horizontalGap = 20f; // gap between list and button column
    private final float rightPadding = 10f; // right edge padding to prevent overflow
    private final float topButtonsOffsetY = 30f; // first button Y offset
    private final float bottomCheckboxOffsetY = 25f; // distance from bottom for checkbox

    public ConfigurationWindow(String label, float posX, float posY, float width, float height) {
        super(label, posX, posY, width, height);
    }

    @Override
    public void initialize() {
        super.initialize();
        recalcLayout();
        reloadConfigs();
        onWindowMoved(getPosX(), getPosY());
    }

    /**
     * Recalculate panel & button sizes based on current window size.
     * Keeps some minimums; if space is too small everything shrinks but stays usable.
     */
    private void recalcLayout() {
        float totalWidth = getWidth();
        float minList = 110f;
        float minButton = 100f;
        float desiredList = totalWidth * 0.42f; // allocate ~42% to list
        // ensure there's horizontal space for list + gap + buttons + right padding
        float remaining = totalWidth - desiredList - horizontalGap - rightPadding - 4f; // 4f: list side padding approx
        if (desiredList < minList) desiredList = minList;
        if (remaining < minButton) {
            // try shrink list further to satisfy button min width
            float need = minButton - remaining;
            if (desiredList - need > minList) {
                desiredList -= need;
            } else {
                // clamp list at min; buttons may still be min afterwards
                desiredList = Math.max(minList, totalWidth - horizontalGap - minButton - rightPadding - 4f);
            }
        }
        listWidth = Math.max(minList, Math.min(desiredList, totalWidth - horizontalGap - minButton - rightPadding - 4f));
        buttonWidth = Math.max(minButton, totalWidth - listWidth - horizontalGap - rightPadding - 4f);

        // Ensure button doesn't overflow window right edge
        float maxButtonWidth = totalWidth - listWidth - horizontalGap - rightPadding - 2f;
        if (buttonWidth > maxButtonWidth) {
            buttonWidth = Math.max(minButton, maxButtonWidth);
        }

        // Vertical sizing for buttons (4 buttons)
        float availableHeight = getHeight() - topButtonsOffsetY - bottomCheckboxOffsetY - 15f; // subtract checkbox + title bar
        float minBtnH = 20f;
        float desiredBtnH = 30f;
        float desiredGap = 15f;
        // required height with desired sizes
        float required = desiredBtnH * 4 + desiredGap * 3;
        if (required <= availableHeight) {
            buttonHeight = desiredBtnH;
            buttonGap = desiredGap;
        } else {
            // scale down proportionally but not below minBtnH and 4f gap
            float minRequired = minBtnH * 4 + 4f * 3;
            float clampAvail = Math.max(minRequired, availableHeight);
            // distribute
            float totalBtnHeightShare = clampAvail * 0.75f; // 75% height to buttons
            float totalGapShare = clampAvail - totalBtnHeightShare;
            buttonHeight = Math.max(minBtnH, totalBtnHeightShare / 4f);
            buttonGap = Math.max(4f, totalGapShare / 3f);
        }
    }

    private void reloadConfigs() {
        configEntries.clear();
        File dir = new File("./config/Myau/");
        if (!dir.exists()) dir.mkdirs();
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
        if (files != null) {
            float offsetY = listInnerPadding; // inside list
            for (File f : files) {
                String name = f.getName();
                if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
                ConfigurationComponent entry = new ConfigurationComponent(name, this, getPosX(), getPosY(), 2, 15 + offsetY, listWidth - 4, entryHeight);
                entry.onMoved(getPosX(), getPosY());
                configEntries.add(entry);
                offsetY += entryHeight + 2;
            }
            float contentHeight = offsetY;
            float visibleHeight = getHeight() - 15 - 10; // exclude title and bottom padding
            maxScroll = Math.max(0, contentHeight - visibleHeight);
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        }
    }

    private void updateEntryPositions() {
        for (ConfigurationComponent entry : configEntries) {
            float base = entry.getBaseOffsetY();
            float newOffsetY = base - scrollOffset; // apply scroll
            entry.setOffsetY(newOffsetY);
            entry.onMoved(getPosX(), getPosY());
            // hide if outside
            float top = entry.getPosY();
            float bottom = top + entry.getHeight();
            float clipTop = getPosY() + 15; // below title bar
            float clipBottom = getPosY() + getHeight() - 5; // above bottom padding
            entry.setHidden(bottom < clipTop || top > clipBottom);
        }
    }

    @Override
    public void onScreenDraw(int mouseX, int mouseY, float partialTicks) {
        // ensure up-to-date layout each frame (cheap) in case external code adjusted size
        recalcLayout();
        super.onScreenDraw(mouseX, mouseY, partialTicks);
        // draw left list frame
        RenderUtil.enableRenderState();
        float listX1 = getPosX() + 2;
        float listY1 = getPosY() + 15; // under title
        float listX2 = getPosX() + listWidth;
        float listY2 = getPosY() + getHeight() - 5;
        RenderUtil.drawRect(listX1, listY1, listX2, listY2, new Color(35, 35, 35, 255).getRGB());
        // manual border
        int borderColor = new Color(0,0,0,200).getRGB();
        RenderUtil.drawRect(listX1, listY1, listX2, listY1 + 1, borderColor); // top
        RenderUtil.drawRect(listX1, listY2 - 1, listX2, listY2, borderColor); // bottom
        RenderUtil.drawRect(listX1, listY1, listX1 + 1, listY2, borderColor); // left
        RenderUtil.drawRect(listX2 - 1, listY1, listX2, listY2, borderColor); // right
        RenderUtil.disableRenderState();

        updateEntryPositions();
        for (ConfigurationComponent entry : configEntries) {
            entry.onDrawScreen(mouseX, mouseY, partialTicks);
        }

        // right side buttons - ensure they stay within window bounds
        float rightStartX = getPosX() + listWidth + horizontalGap;
        // Clamp to prevent overflow
        float maxRightX = getPosX() + getWidth() - buttonWidth - rightPadding;
        if (rightStartX > maxRightX) {
            rightStartX = maxRightX;
        }
        float currentY = getPosY() + topButtonsOffsetY; // first button Y

        drawButton(rightStartX, currentY, "Create Config", mouseX, mouseY);
        currentY += buttonHeight + buttonGap;
        drawButton(rightStartX, currentY, "Delete Config", mouseX, mouseY);
        currentY += buttonHeight + buttonGap;
        drawButton(rightStartX, currentY, "Refresh Config", mouseX, mouseY);
        currentY += buttonHeight + buttonGap;
        drawButton(rightStartX, currentY, "Save Config", mouseX, mouseY);

        // bottom visual checkbox - also clamp position
        float boxY = getPosY() + getHeight() - bottomCheckboxOffsetY;
        float boxX = rightStartX;
        float boxSize = 14f;
        RenderUtil.enableRenderState();
        RenderUtil.drawRect(boxX, boxY, boxX + boxSize, boxY + boxSize, new Color(30,30,30,255).getRGB());
        if (visualCheckbox) {
            RenderUtil.drawRect(boxX + 3, boxY + 3, boxX + boxSize - 3, boxY + boxSize - 3, new Color(90,200,90,255).getRGB());
        }
        RenderUtil.disableRenderState();
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        // Ensure text doesn't overflow
        String checkboxText = "Save Visual";
        float textX = boxX + boxSize + 6;
        float maxTextX = getPosX() + getWidth() - fr.getStringWidth(checkboxText) - rightPadding;
        if (textX > maxTextX) {
            textX = maxTextX;
        }
        fr.drawStringWithShadow(checkboxText, textX, boxY + 3, 0xFFFFFFFF);

        // create name input overlay
        if (creatingNameMode) {
            String tip = "Enter Name: " + creatingBuffer + (System.currentTimeMillis() / 500 % 2 == 0 ? "_" : "");
            float w = fr.getStringWidth(tip) + 20;
            float h = 20;
            float cx = getPosX() + (getWidth()/2f) - (w/2f);
            float cy = getPosY() + (getHeight()/2f) - (h/2f);
            RenderUtil.enableRenderState();
            RenderUtil.drawRect(cx, cy, cx + w, cy + h, new Color(20,20,20,240).getRGB());
            // border
            int overlayBorder = new Color(100,100,100,255).getRGB();
            RenderUtil.drawRect(cx, cy, cx + w, cy + 1, overlayBorder);
            RenderUtil.drawRect(cx, cy + h - 1, cx + w, cy + h, overlayBorder);
            RenderUtil.drawRect(cx, cy, cx + 1, cy + h, overlayBorder);
            RenderUtil.drawRect(cx + w - 1, cy, cx + w, cy + h, overlayBorder);
            RenderUtil.disableRenderState();
            fr.drawStringWithShadow(tip, cx + 5, cy + 6, 0xFFFFFFFF);
        }
    }

    private void drawButton(float x, float y, String text, int mouseX, int mouseY) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        float w = buttonWidth;
        float h = buttonHeight;
        boolean hovered = FakeBrInterface.mouseWithinBounds(mouseX, mouseY, x, y, w, h);
        RenderUtil.enableRenderState();
        Color base = hovered ? new Color(65,65,65,255) : new Color(55,55,55,255);
        RenderUtil.drawRect(x, y, x + w, y + h, base.getRGB());
        int border = new Color(0,0,0,220).getRGB();
        RenderUtil.drawRect(x, y, x + w, y + 1, border);
        RenderUtil.drawRect(x, y + h - 1, x + w, y + h, border);
        RenderUtil.drawRect(x, y, x + 1, y + h, border);
        RenderUtil.drawRect(x + w - 1, y, x + w, y + h, border);
        RenderUtil.disableRenderState();
        fr.drawStringWithShadow(text, x + (w/2f) - fr.getStringWidth(text)/2f, y + (h/2f) - fr.FONT_HEIGHT/2f, 0xFFFFFFFF);
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        super.onMouseClicked(mouseX, mouseY, button);
        if (button != 0) return;
        // pass to entries first
        for (ConfigurationComponent entry : configEntries) {
            entry.onMouseClicked(mouseX, mouseY, button);
        }

        // buttons (dynamic positions) - use same clamping logic as drawing
        float rightStartX = getPosX() + listWidth + horizontalGap;
        float maxRightX = getPosX() + getWidth() - buttonWidth - rightPadding;
        if (rightStartX > maxRightX) {
            rightStartX = maxRightX;
        }
        float currentY = getPosY() + topButtonsOffsetY; // first button Y
        if (isButton(mouseX, mouseY, rightStartX, currentY)) { // create
            creatingNameMode = true;
            creatingBuffer = "";
            return;
        }
        currentY += buttonHeight + buttonGap;
        if (isButton(mouseX, mouseY, rightStartX, currentY)) { // delete
            if (selectedConfigName != null) {
                File f = new File("./config/Myau/" + selectedConfigName + ".json");
                if (f.exists()) f.delete();
                selectedConfigName = null;
                reloadConfigs();
            }
            return;
        }
        currentY += buttonHeight + buttonGap;
        if (isButton(mouseX, mouseY, rightStartX, currentY)) { // refresh
            reloadConfigs();
            return;
        }
        currentY += buttonHeight + buttonGap;
        if (isButton(mouseX, mouseY, rightStartX, currentY)) { // save
            if (selectedConfigName != null) {
                new Config(selectedConfigName, true).save();
                if (visualCheckbox) {
                    // placeholder: could save extra UI state in future
                }
            }
            return;
        }
        // checkbox
        float boxY = getPosY() + getHeight() - bottomCheckboxOffsetY;
        float boxX = rightStartX;
        float boxSize = 14f;
        if (FakeBrInterface.mouseWithinBounds(mouseX, mouseY, boxX, boxY, boxSize, boxSize)) {
            visualCheckbox = !visualCheckbox;
        }
    }

    private boolean isButton(int mouseX, int mouseY, float x, float y) {
        return FakeBrInterface.mouseWithinBounds(mouseX, mouseY, x, y, buttonWidth, buttonHeight);
    }

    @Override
    public void onKeyTyped(char character, int key) {
        super.onKeyTyped(character, key);
        if (!creatingNameMode) return;
        if (character == '\n' || character == '\r') {
            finalizeCreate();
            return;
        }
        if ((int)character == 27) { // ESC
            creatingNameMode = false;
            return;
        }
        if (character == '\b') {
            if (!creatingBuffer.isEmpty()) creatingBuffer = creatingBuffer.substring(0, creatingBuffer.length()-1);
            return;
        }
        if (Character.isLetterOrDigit(character) || character=='_' || character=='-') {
            if (creatingBuffer.length() < 32) creatingBuffer += character;
        }
    }

    private void finalizeCreate() {
        creatingNameMode = false;
        String name = creatingBuffer.trim();
        if (name.isEmpty()) {
            name = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        }
        String baseName = name;
        int index = 1;
        while (new File("./config/Myau/" + name + ".json").exists()) {
            name = baseName + "_" + index++;
        }
        new Config(name, true).save();
        selectedConfigName = name;
        reloadConfigs();
    }

    @Override
    public void onMouseWheel(int mouseX, int mouseY, int wheel) {
        super.onMouseWheel(mouseX, mouseY, wheel);
        // Only scroll if on list area
        float listX1 = getPosX() + 2;
        float listY1 = getPosY() + 15;
        float listX2 = getPosX() + listWidth;
        float listY2 = getPosY() + getHeight() - 5;
        if (FakeBrInterface.mouseWithinBounds(mouseX, mouseY, listX1, listY1, listWidth-2, listY2 - listY1)) {
            if (wheel < 0) { // up (negative usually forward)
                scrollOffset -= entryHeight; // move downwards visually
            } else if (wheel > 0) {
                scrollOffset += entryHeight;
            }
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        }
    }

    @Override
    public void onWindowResized() {
        super.onWindowResized();
        recalcLayout();
        reloadConfigs();
    }

    // Bridge implementation
    @Override
    public void onConfigSelected(String name) {
        selectedConfigName = name;
    }

    @Override
    public String getSelectedConfigName() {
        return selectedConfigName;
    }
}
