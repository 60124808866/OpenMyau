package myau.module.modules;

import myau.module.Module;
import myau.module.category.Category;

public class AntiObfuscate extends Module {
    public AntiObfuscate() {
        super("AntiObfuscate", false, true);
    }

    @Override
    public Category category() {
        return Category.MISC;
    }

    public String stripObfuscated(String input) {
        return input.replaceAll("§k", "");
    }
}
