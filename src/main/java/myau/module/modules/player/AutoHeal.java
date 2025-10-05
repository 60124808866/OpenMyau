package myau.module.modules.player;

import myau.event.EventTarget;
import myau.event.types.Priority;
import myau.events.*;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.module.Category;
import myau.module.Module;
import myau.util.PacketUtil;
import myau.util.TimerUtil;
import myau.property.properties.BooleanProperty;
import myau.property.properties.PercentProperty;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.potion.Potion;

public class AutoHeal extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil timer = new TimerUtil();
    private boolean shouldHeal = false;
    private int prevSlot = -1;
    public final PercentProperty health = new PercentProperty("health", 35);
    public final IntProperty delay = new IntProperty("delay", 4000, 0, 5000);
    public final BooleanProperty regenCheck = new BooleanProperty("regen-check", false);

    private int findHealingItem() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.hasDisplayName()) {
                String name = stack.getDisplayName();
                if (stack.getItem() instanceof ItemSkull && (name.contains("§6") && name.contains("Golden Head") || name.matches("\\S+§c's Head"))) {
                    return i;
                }
                if (stack.getItem() instanceof ItemSoup
                        && (name.contains("§a") && name.contains("Tasty Soup") || name.contains("§a") && name.contains("Assist Soup"))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean hasRegenEffect() {
        return this.regenCheck.getValue() && mc.thePlayer.isPotionActive(Potion.regeneration);
    }

    public AutoHeal() {
        super("AutoHeal", Category.Player, false);
    }

    public boolean isSwitching() {
        return this.prevSlot != -1;
    }

    @EventTarget(Priority.HIGH)
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            this.prevSlot = -1;
        } else {
            switch (event.getType()) {
                case PRE:
                    boolean precent = (float) Math.ceil(mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) / mc.thePlayer.getMaxHealth()
                            <= (float) this.health.getValue() / 100.0F;
                    if (this.shouldHeal
                            && precent
                            && !this.hasRegenEffect()
                            && this.timer.hasTimeElapsed(this.delay.getValue().intValue())) {
                        int slot = this.findHealingItem();
                        if (slot != -1) {
                            this.prevSlot = mc.thePlayer.inventory.currentItem;
                            mc.thePlayer.inventory.currentItem = slot;
                            ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                            PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                            this.timer.reset();
                        }
                    }
                    this.shouldHeal = precent;
                    break;
                case POST:
                    if (this.prevSlot != -1) {
                        mc.thePlayer.inventory.currentItem = this.prevSlot;
                        this.prevSlot = -1;
                    }
            }
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }
}
