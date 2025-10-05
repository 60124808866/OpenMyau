package myau.ui.components;

import myau.Myau;
import myau.module.Module;
import myau.module.Category;
import myau.ui.Component;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CategoryComponent {
    public ArrayList<Component> modulesInCategory = new ArrayList<>();
    public String categoryName;
    private boolean categoryOpened;
    private int width;
    private int y;
    private int x;
    private final int bh;
    public boolean dragging;
    public int xx;
    public int yy;
    public boolean pin = false;
    private double marginY, marginX;

    public CategoryComponent(Category category) {
        this.categoryName = category.getName();
        this.width = 92;
        this.x = 5;
        this.y = 5;
        this.bh = 13;
        this.xx = 0;
        this.categoryOpened = false;
        this.dragging = false;

        List<Module> modules = getModulesByCategory(category);

        int tY = this.bh + 3;
        this.marginX = 80;
        this.marginY = 4.5;

        if (!modules.isEmpty()) {
            for (Module mod : modules) {
                if (mod != null) {
                    ModuleComponent b = new ModuleComponent(mod, this, tY);
                    this.modulesInCategory.add(b);
                    tY += 16;
                }
            }
        }
    }

    private List<Module> getModulesByCategory(Category category) {
        List<Module> modules = new ArrayList<>();

        try {
            if (Myau.moduleManager != null) {
                for (Module module : Myau.moduleManager.modules.values()) {
                    if (module != null && module.getCategory() == category) {
                        modules.add(module);
                    }
                }
            }

            modules.sort(Comparator.comparing(m -> m.getName().toLowerCase()));

        } catch (Exception e) {
            System.err.println("Error getting modules for category: " + category);
            e.printStackTrace();
        }

        return modules;
    }

    public ArrayList<Component> getModules() {
        return this.modulesInCategory;
    }

    public void setX(int n) {
        this.x = n;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void mousePressed(boolean d) {
        this.dragging = d;
    }

    public boolean isPin() {
        return this.pin;
    }

    public void cv(boolean on) {
        this.pin = on;
    }

    public boolean isOpened() {
        return this.categoryOpened;
    }

    public void setOpened(boolean on) {
        this.categoryOpened = on;
    }

    public void render(FontRenderer renderer) {
        this.width = 92;
        update();
        if (!this.modulesInCategory.isEmpty() && this.categoryOpened) {
            int categoryHeight = 0;
            Component moduleRenderManager;
            for (Iterator<Component> moduleInCategoryIterator = this.modulesInCategory.iterator(); moduleInCategoryIterator.hasNext(); categoryHeight += moduleRenderManager.getHeight()) {
                moduleRenderManager = moduleInCategoryIterator.next();
            }
            Gui.drawRect(this.x - 1, this.y, this.x + this.width + 1, this.y + this.bh + categoryHeight + 4, (new Color(0, 0, 0, 100).getRGB()));
        }
        Gui.drawRect((this.x - 2), this.y, (this.x + this.width + 2), (this.y + this.bh + 3), new Color(0, 0, 0, 200).getRGB());
        renderer.drawString(this.categoryName, (float) (this.x + 2), (float) (this.y + 4), -1, false);
        renderer.drawString(this.categoryOpened ? "-" : "+", (float) (this.x + marginX), (float) ((double) this.y + marginY), Color.white.getRGB(), false);
        if (this.categoryOpened && !this.modulesInCategory.isEmpty()) {
            Iterator<Component> var5 = this.modulesInCategory.iterator();
            AtomicInteger offset = new AtomicInteger(0);
            while (var5.hasNext()) {
                Component c2 = var5.next();
                c2.draw(offset);
                offset.incrementAndGet();
            }
        }
    }

    public void update() {
        int offset = this.bh + 3;

        Component component;
        for (Iterator<Component> iterator = this.modulesInCategory.iterator(); iterator.hasNext(); offset += component.getHeight()) {
            component = iterator.next();
            component.setComponentStartAt(offset);
        }
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public void handleDrag(int x, int y) {
        if (this.dragging) {
            this.setX(x - this.xx);
            this.setY(y - this.yy);
        }
    }

    public boolean isHovered(int x, int y) {
        return x >= this.x + 92 - 13 && x <= this.x + this.width && (float) y >= (float) this.y + 2.0F && y <= this.y + this.bh + 1;
    }

    public boolean mousePressed(int x, int y) {
        return x >= this.x + 77 && x <= this.x + this.width - 6 && (float) y >= (float) this.y + 2.0F && y <= this.y + this.bh + 1;
    }

    public boolean insideArea(int x, int y) {
        return x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.bh;
    }

    public String getName() {
        return categoryName;
    }

    public void setLocation(int parseInt, int parseInt1) {
        this.x = parseInt;
        this.y = parseInt1;
    }
}