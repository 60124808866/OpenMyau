package myau.module.modules.render;

import myau.module.Category;
import myau.module.Module;
import myau.property.properties.PercentProperty;

public class NoHurtCam extends Module {
    public final PercentProperty multiplier = new PercentProperty("multiplier", 0);

    public NoHurtCam() {
        super("NoHurtCam", Category.Render, false, true);
    }
}
