package myau.module.modules;

import myau.module.Module;
import myau.module.category.Category;

public class NoHitDelay extends Module {
    public NoHitDelay() {
        super("NoHitDelay", true, true);
    }

    @Override
    public Category category() {
        return Category.COMBAT;
    }
}
