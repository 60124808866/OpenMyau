package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.ItemUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;

public class AutoAnduril extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int previousSlot = -1;
    private int currentSlot = -1;
    private int intervalTick = -1;
    private int holdTick = -1;
    public final IntProperty interval = new IntProperty("interval", 40, 0, 100);
    public final IntProperty hold = new IntProperty("hold", 1, 0, 20);
    public final BooleanProperty speedCheck = new BooleanProperty("speedCheck", false);
    public final IntProperty debug = new IntProperty("debug", 0, 0, 9);

    public AutoAnduril() {
        super("AutoAnduril", false);
    }

    public boolean canSwap() {
        if (mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && mc.gameSettings.keyBindAttack.isKeyDown()) return false;
        ItemStack currentItem = mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem);
        if (currentItem != null) {
            if (currentItem.getItem() instanceof ItemBlock && mc.gameSettings.keyBindUseItem.isKeyDown()) return false;
            if (!(currentItem.getItem() instanceof ItemSword) && mc.thePlayer.isUsingItem()) return false;
        }
        InvWalk invWalk = (InvWalk) Myau.moduleManager.modules.get(InvWalk.class);
        return mc.currentScreen == null || mc.currentScreen instanceof myau.ui.ClickGui
                || invWalk.isEnabled() && invWalk.canInvWalk();
    }

    public boolean hasSpeed() {
        if (!speedCheck.getValue()) return false;
        PotionEffect potionEffect = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed);
        if (potionEffect == null) return false;
        return (potionEffect.getAmplifier() > 0);
    }

    @EventTarget(Priority.LOWEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.currentSlot != -1 && this.currentSlot != mc.thePlayer.inventory.currentItem) {
                this.currentSlot = -1;
                this.previousSlot = -1;
                this.intervalTick = interval.getValue();
                this.holdTick = -1;
            }

            if (this.intervalTick > 0) {
                this.intervalTick--;
            } else if (intervalTick == 0) {
                if (canSwap() && !hasSpeed()) {
                    int slot = ItemUtil.findAndurilHotbarSlot(mc.thePlayer.inventory.currentItem);
                    if (debug.getValue() != 0 && slot == -1) slot = debug.getValue() - 1;
                    if (slot != -1 && slot != mc.thePlayer.inventory.currentItem) {
                        this.previousSlot = mc.thePlayer.inventory.currentItem;
                        this.currentSlot = mc.thePlayer.inventory.currentItem = slot;
                        this.intervalTick = -1;
                        this.holdTick = hold.getValue();
                        return;
                    } else {
                        this.intervalTick = interval.getValue();
                        this.holdTick = -1;
                    }
                }
            }
            if (this.holdTick > 0) {
                this.holdTick--;
            } else if (holdTick == 0) {
                if (this.previousSlot != -1 && canSwap()) {
                    mc.thePlayer.inventory.currentItem = this.previousSlot;
                    this.previousSlot = -1;
                    this.holdTick = -1;
                    this.intervalTick = interval.getValue();
                }
            }
        }
    }

    @Override
    public void onEnabled() {
        this.previousSlot = -1;
        this.currentSlot = -1;
        this.intervalTick = this.interval.getValue();
        this.holdTick = -1;
    }

    @Override
    public void onDisabled() {
        this.previousSlot = -1;
        this.currentSlot = -1;
        this.intervalTick = -1;
        this.holdTick = -1;
    }
}
