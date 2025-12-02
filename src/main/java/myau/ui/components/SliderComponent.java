
package myau.ui.components;

import myau.Myau;
import myau.module.modules.HUD;
import myau.ui.ClickGui;
import myau.ui.Component;
import myau.ui.callback.GuiInput;
import myau.ui.dataset.Slider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;

public class SliderComponent implements Component {
    private final Slider slider;
    private final ModuleComponent parentModule;
    private int offsetY;
    private int x;
    private int y;
    private boolean dragging = false;
    private double sliderWidth;
    private long increment = 0;
    private long decrement = 0;

    public SliderComponent(Slider slider, ModuleComponent parentModule, int offsetY) {
        this.slider = slider;
        this.parentModule = parentModule;
        this.x = parentModule.category.getX() + parentModule.category.getWidth();
        this.y = parentModule.category.getY() + parentModule.offsetY;
        this.offsetY = offsetY;
    }

    public void draw(AtomicInteger offset) {
        Gui.drawRect(this.parentModule.category.getX() + 4, this.parentModule.category.getY() + this.offsetY + 11, this.parentModule.category.getX() + 4 + this.parentModule.category.getWidth() - 8, this.parentModule.category.getY() + this.offsetY + 15, -12302777);
        int sliderStart = this.parentModule.category.getX() + 4;
        int sliderEnd = this.parentModule.category.getX() + 4 + (int) this.sliderWidth;
        if (sliderEnd - sliderStart > 84) {
            sliderEnd = sliderStart + 84;
        }
        Gui.drawRect(sliderStart, this.parentModule.category.getY() + this.offsetY + 11, sliderEnd, this.parentModule.category.getY() + this.offsetY + 15, ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis(), offset.get()).getRGB());
        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(this.slider.getName() + ": " + this.slider.getValueColorString(), (float) ((int) ((float) (this.parentModule.category.getX() + 4) * 2.0F)), (float) ((int) ((float) (this.parentModule.category.getY() + this.offsetY + 3) * 2.0F)), -1);
        GL11.glPopMatrix();
    }

    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return 16;
    }

    public void update(int mousePosX, int mousePosY) {
        this.y = this.parentModule.category.getY() + this.offsetY;
        this.x = this.parentModule.category.getX();

        double d = Math.min(this.parentModule.category.getWidth() - 8, Math.max(0, mousePosX - this.x));
        this.sliderWidth = (double) (this.parentModule.category.getWidth() - 8) *
                (this.slider.getInput() - this.slider.getMin()) /
                (this.slider.getMax() - this.slider.getMin());

        if (this.dragging) {
            if (d == 0.0D) {
                this.slider.setValue(this.slider.getMin());
            } else {
                double rawValue = d / (double) (this.parentModule.category.getWidth() - 8)
                        * (this.slider.getMax() - this.slider.getMin())
                        + this.slider.getMin();

                double increment = this.slider.getIncrement();
                if (increment > 0) {
                    rawValue = Math.round(rawValue / increment) * increment;
                }
                double n = roundToPrecision(rawValue, 2);
                n = Math.max(this.slider.getMin(), Math.min(this.slider.getMax(), n));
                this.slider.setValue(n);
            }
        }
        if (this.increment != 0 && this.increment < System.currentTimeMillis()) {
            this.increment = System.currentTimeMillis() + 50;
            this.slider.stepping(true);
        }
        if (this.decrement != 0 && this.decrement < System.currentTimeMillis()) {
            this.decrement = System.currentTimeMillis() + 50;
            this.slider.stepping(false);
        }
    }


    private static double roundToPrecision(double v, int precision) {
        if (precision < 0) {
            return 0.0D;
        } else {
            BigDecimal bd = new BigDecimal(v);
            bd = bd.setScale(precision, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }
    }

    public void mouseDown(int x, int y, int button) {
        if (this.isTextHovered(x, y) && button == 0 && this.parentModule.panelExpand) {
            GuiInput.prompt(slider.getName().replace("-", " "), slider.getValueString(), slider::setValueString, ClickGui.getInstance());
            return;
        }

        if (this.isLeftHalfHovered(x, y) && this.parentModule.panelExpand) {
            if (button == 0) {
                this.dragging = true;
            } else if(button == 1 && this.decrement == 0) {
                this.decrement = System.currentTimeMillis() + 500;
                this.slider.stepping(false);
            }
        }

        if (this.isRightHalfHovered(x, y) && this.parentModule.panelExpand) {
            if (button == 0) {
                this.dragging = true;
            } else if(button == 1 && this.increment == 0) {
                this.increment = System.currentTimeMillis() + 500;
                this.slider.stepping(true);
            }
        }

    }

    public void mouseReleased(int x, int y, int button) {
        this.dragging = false;
        this.increment = 0;
        this.decrement = 0;
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {

    }

    public boolean isTextHovered(int x, int y) {
        return x > this.x && x < this.x + this.parentModule.category.getWidth() && y > this.y && y < this.y + 8;
    }

    public boolean isLeftHalfHovered(int x, int y) {
        return x > this.x && x < this.x + this.parentModule.category.getWidth() / 2 + 1 && y > this.y + 8 && y < this.y + 16;
    }

    public boolean isRightHalfHovered(int x, int y) {
        return x > this.x + this.parentModule.category.getWidth() / 2 && x < this.x + this.parentModule.category.getWidth() && y > this.y + 8 && y < this.y + 16;
    }


    @Override
    public boolean isVisible() {
        return slider.isVisible();
    }
}
