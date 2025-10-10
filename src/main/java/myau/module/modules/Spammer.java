package myau.module.modules;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.module.category.Category;
import myau.util.TimerUtil;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.TextProperty;
import net.minecraft.client.Minecraft;

public class Spammer extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil timer = new TimerUtil();
    private int charOffset = 19968;
    public final TextProperty text = new TextProperty("text", "meow");
    public final FloatProperty delay = new FloatProperty("delay", 3.5F, 0.0F, 3600.0F);
    public final IntProperty random = new IntProperty("random", 0, 0, 10);

    public Spammer() {
        super("Spammer", false);
    }

    @Override
    public Category category() {
        return Category.MISC;
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled()) {
            if (this.timer.hasTimeElapsed((long) (this.delay.getValue() * 1000.0F))) {
                this.timer.reset();
                String text = this.text.getValue();
                if (this.random.getValue() > 0) {
                    text = String.format("%s ", text);
                    for (int i = 0; i < this.random.getValue(); i++) {
                        text = String.format("%s%s", text, (char) this.charOffset);
                        this.charOffset++;
                        if (this.charOffset > 40959) {
                            this.charOffset = 19968;
                        }
                    }
                }
                mc.thePlayer.sendChatMessage(text);
            }
        }
    }
}
