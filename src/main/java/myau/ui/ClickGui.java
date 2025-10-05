package myau.ui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import myau.Myau;
import myau.module.Category;
import myau.ui.components.CategoryComponent;
import net.minecraft.client.gui.*;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ClickGui extends GuiScreen {
    private static ClickGui instance;
    private final File configFile = new File("./config/Myau/", "clickgui.txt");
    private final ArrayList<CategoryComponent> categoryList;


    public ClickGui() {
        instance = this;

        this.categoryList = new ArrayList<>();
        int topOffset = 5;

        CategoryComponent combat = new CategoryComponent(Category.Combat);
        combat.setY(topOffset);
        categoryList.add(combat);
        topOffset += 20;

        CategoryComponent movement = new CategoryComponent(Category.Movement);
        movement.setY(topOffset);
        categoryList.add(movement);
        topOffset += 20;

        CategoryComponent render = new CategoryComponent(Category.Render);
        render.setY(topOffset);
        categoryList.add(render);
        topOffset += 20;

        CategoryComponent player = new CategoryComponent(Category.Player);
        player.setY(topOffset);
        categoryList.add(player);
        topOffset += 20;

        CategoryComponent misc = new CategoryComponent(Category.Misc);
        misc.setY(topOffset);
        categoryList.add(misc);

        loadPositions();
    }


    public static ClickGui getInstance() {
        return instance;
    }

    public void initGui() {
        super.initGui();
    }

    public void drawScreen(int x, int y, float p) {
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 100).getRGB());

        mc.fontRendererObj.drawStringWithShadow("Myau 250910+1", 4, this.height - 3 - mc.fontRendererObj.FONT_HEIGHT * 2, new Color(60, 162, 253).getRGB());
        mc.fontRendererObj.drawStringWithShadow("dev, ksyz", 4, this.height - 3 - mc.fontRendererObj.FONT_HEIGHT, new Color(60, 162, 253).getRGB());

        for (CategoryComponent category : categoryList) {
            category.render(this.fontRendererObj);
            category.handleDrag(x, y);

            for (Component module : category.getModules()) {
                module.update(x, y);
            }
        }
    }

    public void mouseClicked(int x, int y, int mouseButton) {
        Iterator<CategoryComponent> btnCat = categoryList.iterator();
        while (true) {
            CategoryComponent category;
            do {
                do {
                    if (!btnCat.hasNext()) {
                        return;
                    }

                    category = btnCat.next();
                    if (category.insideArea(x, y) && !category.isHovered(x, y) && !category.mousePressed(x, y) && mouseButton == 0) {
                        category.mousePressed(true);
                        category.xx = x - category.getX();
                        category.yy = y - category.getY();
                    }

                    if (category.mousePressed(x, y) && mouseButton == 0) {
                        category.setOpened(!category.isOpened());
                    }

                    if (category.isHovered(x, y) && mouseButton == 0) {
                        category.cv(!category.isPin());
                    }
                } while (!category.isOpened());
            } while (category.getModules().isEmpty());

            for (Component c : category.getModules()) {
                c.mouseDown(x, y, mouseButton);
            }
        }
    }

    public void mouseReleased(int x, int y, int s) {
        if (s == 0) {
            Iterator<CategoryComponent> iterator = categoryList.iterator();

            CategoryComponent categoryComponent;
            while (iterator.hasNext()) {
                categoryComponent = iterator.next();
                categoryComponent.mousePressed(false);
            }

            iterator = categoryList.iterator();

            while (true) {
                do {
                    do {
                        if (!iterator.hasNext()) {
                            return;
                        }

                        categoryComponent = iterator.next();
                    } while (!categoryComponent.isOpened());
                } while (categoryComponent.getModules().isEmpty());

                for (Component component : categoryComponent.getModules()) {
                    component.mouseReleased(x, y, s);
                }
            }
        }
    }

    public void keyTyped(char typedChar, int key) {
        if (key == 1) {
            this.mc.displayGuiScreen(null);
        } else {
            Iterator<CategoryComponent> btnCat = categoryList.iterator();

            while (true) {
                CategoryComponent cat;
                do {
                    do {
                        if (!btnCat.hasNext()) {
                            return;
                        }

                        cat = btnCat.next();
                    } while (!cat.isOpened());
                } while (cat.getModules().isEmpty());

                for (Component component : cat.getModules()) {
                    component.keyTyped(typedChar, key);
                }
            }
        }
    }

    public void onGuiClosed() {
        savePositions();
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    private void savePositions() {
        JsonObject json = new JsonObject();
        for (CategoryComponent cat : categoryList) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", cat.getX());
            pos.addProperty("y", cat.getY());
            pos.addProperty("open", cat.isOpened());
            json.add(cat.getName(), pos);
        }
        try (FileWriter writer = new FileWriter(configFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPositions() {
        if (!configFile.exists()) return;
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            for (CategoryComponent cat : categoryList) {
                if (json.has(cat.getName())) {
                    JsonObject pos = json.getAsJsonObject(cat.getName());
                    cat.setX(pos.get("x").getAsInt());
                    cat.setY(pos.get("y").getAsInt());
                    cat.setOpened(pos.get("open").getAsBoolean());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}