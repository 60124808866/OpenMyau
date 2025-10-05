
package myau.ui.components;

import myau.Myau;
import myau.module.Module;
import myau.module.modules.render.HUD;
import myau.property.Property;
import myau.property.properties.*;
import myau.ui.Component;
import myau.ui.dataset.impl.FloatSlider;
import myau.ui.dataset.impl.IntSlider;
import myau.ui.dataset.impl.PercentageSlider;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ModuleComponent implements Component {
    public Module mod;
    public CategoryComponent category;
    public int offsetY;
    private final ArrayList<Component> settings;
    public boolean panelExpand;

    public ModuleComponent(Module mod, CategoryComponent category, int offsetY) {
        this.mod = mod;
        this.category = category;
        this.offsetY = offsetY;
        this.settings = new ArrayList<>();
        this.panelExpand = false;
        int y = offsetY + 12;
        if (!Myau.propertyManager.properties.get(mod.getClass()).isEmpty()) {
            for (Property<?> baseProperty : Myau.propertyManager.properties.get(mod.getClass())) {
                if (baseProperty instanceof BooleanProperty) {
                    BooleanProperty property = (BooleanProperty) baseProperty;
                    CheckBoxComponent c = new CheckBoxComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof FloatProperty) {
                    FloatProperty property = (FloatProperty) baseProperty;
                    SliderComponent c = new SliderComponent(new FloatSlider(property), this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof IntProperty) {
                    IntProperty property = (IntProperty) baseProperty;
                    SliderComponent c = new SliderComponent(new IntSlider(property), this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof PercentProperty) {
                    PercentProperty property = (PercentProperty) baseProperty;
                    SliderComponent c = new SliderComponent(new PercentageSlider(property), this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof ModeProperty) {
                    ModeProperty property = (ModeProperty) baseProperty;
                    ModeComponent c = new ModeComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof ColorProperty) {
                    ColorProperty property = (ColorProperty) baseProperty;
                    ColorSliderComponent c = new ColorSliderComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof TextProperty) {
                    TextProperty property = (TextProperty) baseProperty;
                    TextComponent c = new TextComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                }
            }
        }

        this.settings.add(new BindComponent(this, y));
    }

    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
        int y = this.offsetY + 16;

        for (Component c : this.settings) {
            c.setComponentStartAt(y);
            if (c.isVisible()) {
                y += c.getHeight();
            }
        }
    }

    public void draw(AtomicInteger offset) {
        int textColor;
        if (this.mod.isEnabled()) {
            textColor = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis(), offset.get()).getRGB();
        } else {
            textColor = new Color(102, 102, 102).getRGB();
        }
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(this.mod.getName(), (float) (this.category.getX() + this.category.getWidth() / 2 - Minecraft.getMinecraft().fontRendererObj.getStringWidth(this.mod.getName()) / 2), (float) (this.category.getY() + this.offsetY + 4), textColor);
        if (this.panelExpand && !this.settings.isEmpty()) {
            for (Component c : this.settings) {
                if (c.isVisible()) {
                    c.draw(offset);
                    offset.incrementAndGet();
                }
            }
        }


    }

    public int getHeight() {
        if (!this.panelExpand) {
            return 16;
        } else {
            int h = 16;
            for (Component c : this.settings) {
                if (c.isVisible()) {
                    h += c.getHeight();
                }
            }
            return h;
        }
    }

    public void update(int mousePosX, int mousePosY) {
        if(!panelExpand) return;
        if (!this.settings.isEmpty()) {
            for (Component c : this.settings) {
                if (c.isVisible()) {
                    c.update(mousePosX, mousePosY);
                }
            }
        }

    }

    public void mouseDown(int x, int y, int button) {
        if (this.isHovered(x, y) && button == 0) {
            this.mod.toggle();
        }

        if (this.isHovered(x, y) && button == 1) {
            this.panelExpand = !this.panelExpand;
        }

        if(!panelExpand) return;
        for (Component c : this.settings) {
            if (c.isVisible()) {
                c.mouseDown(x, y, button);
            }
        }

    }

    public void mouseReleased(int x, int y, int button) {
        if(!panelExpand) return;
        for (Component c : this.settings) {
            if (c.isVisible()) {
                c.mouseReleased(x, y, button);
            }
        }

    }

    public void keyTyped(char chatTyped, int keyCode) {
        if(!panelExpand) return;
        for (Component c : this.settings) {
            if (c.isVisible()) {
                c.keyTyped(chatTyped, keyCode);
            }
        }

    }

    public boolean isHovered(int x, int y) {
        return x > this.category.getX() && x < this.category.getX() + this.category.getWidth() && y > this.category.getY() + this.offsetY && y < this.category.getY() + 16 + this.offsetY;
    }


    @Override
    public boolean isVisible() {
        return true;
    }
}
