package myau.module.modules.misc;

import myau.module.Category;
import myau.module.Module;

public class AntiObfuscate extends Module {
    public AntiObfuscate() {
        super("AntiObfuscate", Category.Misc, false, true);
    }

    public String stripObfuscated(String input) {
        return input.replaceAll("§k", "");
    }
}
