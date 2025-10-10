package myau.module.modules;

import myau.module.Module;
import myau.module.category.Category;
import myau.property.properties.PercentProperty;

public class NoHurtCam extends Module {
    public final PercentProperty multiplier = new PercentProperty("multiplier", 0);

    public NoHurtCam() {
        super("NoHurtCam", false, true);
    }

    @Override
    public Category category() {
        return Category.RENDER;
    }
}
