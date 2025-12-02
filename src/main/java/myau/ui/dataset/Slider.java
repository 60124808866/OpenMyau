package myau.ui.dataset;

public abstract class Slider {
    public abstract double getInput();

    public abstract double getMin();

    public abstract double getMax();

    public abstract void setValue(double value);

    public abstract void setValueString(String value);

    public abstract String getName();

    public abstract String getValueString();

    public abstract String getValueColorString();

    public abstract double getIncrement();

    public abstract boolean isVisible();

    public abstract void stepping(boolean increment);
}
