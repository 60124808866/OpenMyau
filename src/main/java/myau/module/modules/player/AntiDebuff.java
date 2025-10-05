package myau.module.modules.player;

import myau.module.Category;
import myau.module.Module;
import myau.property.properties.BooleanProperty;

public class AntiDebuff extends Module {
    public final BooleanProperty blindness = new BooleanProperty("blindness", true);
    public final BooleanProperty nausea = new BooleanProperty("nausea", true);

    public AntiDebuff() {
        super("AntiDebuff", Category.Player, false);
    }
}
