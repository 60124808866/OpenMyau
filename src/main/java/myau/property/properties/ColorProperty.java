package myau.property.properties;

import com.google.gson.JsonObject;
import myau.property.Property;

import java.awt.*;
import java.util.function.BooleanSupplier;

public class ColorProperty extends Property<Integer> {
    public ColorProperty(String name, Integer color) {
        this(name, color, (BooleanSupplier)null);
    }

    public ColorProperty(String name, Integer color, Integer alpha) {
        this(name, color, alpha, null);
    }

    public ColorProperty(String string, Integer color, BooleanSupplier check) {
        super(string, color, check);
    }

    public ColorProperty(String name, Integer color, Integer alpha, BooleanSupplier check) {
        this(name, color & 0xFFFFFF | alpha << 24, check);
    }

    @Override
    public String getValuePrompt() {
        return "ARGB";
    }

    @Override
    public String formatValue() {
        String hex = String.format("%08X", this.getValue()).substring(0,8);
        return String.format("&r%s&c%s&a%s&9%s", hex.substring(0, 2), hex.substring(2, 4), hex.substring(4, 6), hex.substring(6, 8));
    }

    @Override
    public boolean parseString(String string) {
        return this.setValue(Integer.parseUnsignedInt(string.replace("#", ""), 16));
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        return this.parseString(jsonObject.get(this.getName()).getAsString().substring(0,8));
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(this.getName(), String.format("%08X", this.getValue()));
    }
}
