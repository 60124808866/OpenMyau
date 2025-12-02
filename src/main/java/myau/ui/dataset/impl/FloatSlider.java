package myau.ui.dataset.impl;

import myau.enums.ChatColors;
import myau.property.properties.FloatProperty;
import myau.ui.dataset.Slider;

public class FloatSlider extends Slider {
    private final FloatProperty property;

    public FloatSlider(FloatProperty property) {
        this.property = property;
    }

    @Override
    public double getInput() {
        return property.getValue();
    }

    @Override
    public double getMin() {
        return property.getMinimum();
    }

    @Override
    public double getMax() {
        return property.getMaximum();
    }

    @Override
    public void setValue(double value) {
        property.setValue(new Double(value).floatValue());
    }

    @Override
    public void setValueString(String value) {
        try {
            property.setValue(Float.parseFloat(value));
        } catch (Exception ignore) {
        }
    }

    @Override
    public String getName() {
        return property.getName().replace("-", " ");
    }

    @Override
    public String getValueString() {
        return property.getValue().toString();
    }

    @Override
    public String getValueColorString() {
        return ChatColors.formatColor(property.formatValue());
    }

    @Override
    public double getIncrement() {
        return 0.1;
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }

    @Override
    public void stepping(boolean increment) {
        if (increment) {
            if (property.getValue() >= property.getMaximum()) return;
            property.setValue(Math.round(property.getValue() * 10 + 1) / 10.0F);
        } else {
            if (property.getValue() <= property.getMinimum()) return;
            property.setValue(Math.round(property.getValue() * 10 - 1) / 10.0F);
        }
    }
}
