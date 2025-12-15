package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.module.Module;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import myau.property.properties.BooleanProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class SpeedMine extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final PercentProperty speed = new PercentProperty("speed", 15);
    public final IntProperty delay = new IntProperty("delay", 0, 0, 4);

    public final BooleanProperty groundSpoof = new BooleanProperty("ground-spoof", false);

    public SpeedMine() {
        super("SpeedMine", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (!mc.playerController.isInCreativeMode()) {

                if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK) {

                    ((IAccessorPlayerControllerMP) mc.playerController)
                            .setBlockHitDelay(Math.min(((IAccessorPlayerControllerMP) mc.playerController).getBlockHitDelay(), this.delay.getValue() + 1));

                    if (((IAccessorPlayerControllerMP) mc.playerController).getIsHittingBlock()) {

                        float curBlockDamageMP = ((IAccessorPlayerControllerMP) mc.playerController).getCurBlockDamageMP();
                        float baseDamage = 0.3F * (this.speed.getValue().floatValue() / 100.0F);

                        if (curBlockDamageMP < baseDamage) {
                            ((IAccessorPlayerControllerMP) mc.playerController).setCurBlockDamageMP(baseDamage);
                        }

                        if (this.groundSpoof.getValue()) {
                            float pitch = mc.thePlayer.rotationPitch;
                            if (pitch >= 80.0F && pitch <= 90.0F) {
                                mc.thePlayer.onGround = true;
                                mc.thePlayer.movementInput.jump = false;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public String[] getSuffix() {
        String spoofSuffix = this.groundSpoof.getValue() ? "GS" : "";
        return new String[]{String.format("%d%%", this.speed.getValue()), spoofSuffix};
    }
}
