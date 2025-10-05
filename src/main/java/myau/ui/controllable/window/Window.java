package myau.ui.controllable.window;

import myau.ui.controllable.FakeBrInterface;
import myau.ui.controllable.component.Component;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;
import java.util.ArrayList;

public class Window {
    private final String label;
    private float posX, posY, lastPosX, lastPosY, width, height;
    private boolean dragging;
    private boolean hiding = false;
    private boolean resizing; // 是否正在调整大小
    private float lastMouseX, lastMouseY; // 记录开始调整时的鼠标位置
    private static final float RESIZE_CORNER_SIZE = 10f; // 右下角调整区域大小
    private final float minWidth; // 最小宽度（使用初始化时的宽度）
    private final float minHeight; // 最小高度（使用初始化时的高度）
    private final ArrayList<Component> components = new ArrayList<>();

    public Window(String label, float posX, float posY, float width, float height) {
        this.label = label;
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
        this.minWidth = width; // 使用初始宽度作为最小宽度
        this.minHeight = height; // 使用初始高度作为最小高度
    }

    public void initialize() {
        components.forEach(Component::initialize);
    }

    public void onWindowMoved(final float posX, final float posY) {
        this.posX = posX;
        this.posY = posY;
        components.forEach(component -> component.onMoved(posX, posY));
    }

    public void onScreenDraw(int mouseX, int mouseY, float partialTicks) {
        // 处理调整大小
        if (isResizing()) {
            float newWidth = mouseX - getPosX();
            float newHeight = mouseY - getPosY();

            // 限制最小尺寸
            if (newWidth >= minWidth) {
                setWidth(newWidth);
            }
            if (newHeight >= minHeight) {
                setHeight(newHeight);
            }
        }

        if (isDragging()) {
            setPosX(mouseX + getLastPosX());
            setPosY(mouseY + getLastPosY());
            onWindowMoved(getPosX(), getPosY());
        }
        if (getPosX() < 0) {
            setPosX(0);
            onWindowMoved(getPosX(), getPosY());
        }
        if (getPosX() + getWidth() > new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth()) {
            setPosX(new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth() - getWidth());
            onWindowMoved(getPosX(), getPosY());
        }
        if (getPosY() < 0) {
            setPosY(0);
            onWindowMoved(getPosX(), getPosY());
        }
        if (getPosY() + getHeight() > new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight()) {
            setPosY(new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight() - getHeight());
            onWindowMoved(getPosX(), getPosY());
        }

        RenderUtil.enableRenderState();

        RenderUtil.drawRect(getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + getHeight(), new Color(45, 45, 45, 255).getRGB());
        RenderUtil.drawRect(getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + 15, new Color(35, 35, 35, 255).getRGB());

        RenderUtil.disableRenderState();
        final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        fontRenderer.drawStringWithShadow(getLabel(),
                (getPosX() + (getWidth() / 2)) - fontRenderer.getStringWidth(getLabel()),
                getPosY() + 3f,
                -1);
        components.forEach(component -> component.onDrawScreen(mouseX, mouseY, partialTicks));

        // 绘制右下角调整大小指示器（恢复到原来的位置）
        RenderUtil.enableRenderState();
        float circleX = getPosX() + getWidth() - (RESIZE_CORNER_SIZE / 2);
        float circleY = getPosY() + getHeight() - (RESIZE_CORNER_SIZE / 2);
        float circleRadius = RESIZE_CORNER_SIZE / 2;
        RenderUtil.drawFilledCircle(circleX, circleY, circleRadius, new Color(80, 80, 80, 200).getRGB());
        RenderUtil.disableRenderState();
    }


    public void onMouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            // 检查是否点击了标题栏
            final boolean hovered = FakeBrInterface.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY(), getWidth(), 15);
            if (hovered) {
                setLastPosX(getPosX() - mouseX);
                setLastPosY(getPosY() - mouseY);
                setDragging(true);
            }
        }
        if (FakeBrInterface.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY(), getWidth(), getHeight())) {
            components.forEach(component -> component.onMouseClicked(mouseX, mouseY, button));
        }
        if (button == 0) {
            // 检查是否点击了右下角调整大小区域（恢复原来的位置）
            float cornerX = getPosX() + getWidth() - RESIZE_CORNER_SIZE;
            float cornerY = getPosY() + getHeight() - RESIZE_CORNER_SIZE;
            final boolean resizeHovered = FakeBrInterface.mouseWithinBounds(mouseX, mouseY, cornerX, cornerY, RESIZE_CORNER_SIZE, RESIZE_CORNER_SIZE);

            if (resizeHovered) {
                setResizing(true);
                setLastMouseX(mouseX);
                setLastMouseY(mouseY);
            }
        }
    }

    public void onMouseReleased(int mouseX, int mouseY, int button) {
        if (button == 0) {
            if (isDragging()) {
                setDragging(false);
            }
            if (isResizing()) {
                setResizing(false);
            }
        }
        if (FakeBrInterface.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY(), getWidth(), getHeight())) {
            components.forEach(component -> component.onMouseReleased(mouseX, mouseY, button));
        }
    }

    public void onKeyTyped(char character, int key) {
        components.forEach(component -> component.onKeyTyped(character, key));
    }

    public void onMouseWheel(int mouseX, int mouseY, int wheel) {
        components.forEach(component -> {
            if (component instanceof myau.ui.controllable.component.collection.category.ModulePanelComponent) {
                ((myau.ui.controllable.component.collection.category.ModulePanelComponent) component).handleMouseWheel(mouseX, mouseY, wheel);
            }
        });
    }

    public void onClosed() {

    }

    public boolean isDragging() {
        return dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    public String getLabel() {
        return label;
    }

    public float getPosX() {
        return posX;
    }

    public void setPosX(float posX) {
        this.posX = posX;
    }

    public float getPosY() {
        return posY;
    }

    public void setPosY(float posY) {
        this.posY = posY;
    }

    public float getLastPosX() {
        return lastPosX;
    }

    public void setLastPosX(float lastPosX) {
        this.lastPosX = lastPosX;
    }

    public float getLastPosY() {
        return lastPosY;
    }

    public void setLastPosY(float lastPosY) {
        this.lastPosY = lastPosY;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
        onWindowResized();
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
        onWindowResized();
    }

    public void onWindowResized() {
        // 子类可以重写此方法来处理窗口大小改变
    }

    public ArrayList<Component> getComponents() {
        return components;
    }

    public boolean isResizing() {
        return resizing;
    }

    public void setResizing(boolean resizing) {
        this.resizing = resizing;
    }

    public float getLastMouseX() {
        return lastMouseX;
    }

    public void setLastMouseX(float lastMouseX) {
        this.lastMouseX = lastMouseX;
    }

    public float getLastMouseY() {
        return lastMouseY;
    }

    public void setLastMouseY(float lastMouseY) {
        this.lastMouseY = lastMouseY;
    }

    public boolean isHiding() {
        return hiding;
    }

    public void setHiding(boolean hiding) {
        this.hiding = hiding;
    }
}
