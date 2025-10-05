package myau.ui.controllable.component;

import java.util.ArrayList;

public class Component {
    private String label;
    private float posX;
    private float posY;
    private float offsetX;
    private float offsetY;
    private final float originalOffsetX;
    private final float originalOffsetY;
    private float width;
    private float height;
    private boolean hidden;
    private final ArrayList<Component> subComponents = new ArrayList<>();

    public Component(String label, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        this.label = label;
        this.posX = posX + offsetX;
        this.posY = posY + offsetY;
        this.offsetX = originalOffsetX = offsetX;
        this.offsetY = originalOffsetY = offsetY;
        this.width = width;
        this.height = height;
    }

    public void initialize() {

    }

    public void onMoved(float movedX, float movedY) {
        this.posX = movedX + offsetX;
        this.posY = movedY + offsetY;
        // 递归更新所有子组件的位置
        subComponents.forEach(component -> component.onMoved(this.posX, this.posY));
    }

    public void onDrawScreen(int mouseX, int mouseY, float partialTicks) {

    }

    public void onMouseClicked(int mouseX, int mouseY, int button) {

    }

    public void onMouseReleased(int mouseX, int mouseY, int button) {

    }

    public void onKeyTyped(char character, int keyCode) {

    }

    public void onGuiClosed() {

    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    public float getOriginalOffsetX() {
        return originalOffsetX;
    }

    public float getOriginalOffsetY() {
        return originalOffsetY;
    }
    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public ArrayList<Component> getSubComponents() {
        return subComponents;
    }

    /**
     * 计算下一个子组件应该放置的 Y 偏移量
     * @return 所有已添加子组件的总高度
     */
    public float calculateNextOffsetY() {
        float totalHeight = 0;
        for (Component subComponent : subComponents) {
            totalHeight += subComponent.getHeight();
        }
        return totalHeight;
    }
}