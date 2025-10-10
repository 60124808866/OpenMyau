package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.module.category.Category;
import myau.util.PacketUtil;
import myau.util.RandomUtil;
import myau.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S08PacketPlayerPosLook.EnumFlags;

public class NoRotate extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean reset = false;

    public NoRotate() {
        super("NoRotate", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            if (mc.thePlayer.rotationYaw != -180.0F || mc.thePlayer.rotationPitch != 0.0F) {
                if (event.getPacket() instanceof S02PacketChat) {
                    String msg = ((S02PacketChat) event.getPacket()).getChatComponent().getFormattedText();
                    if (msg.contains("§e§lProtect your bed and destroy the enemy beds.") || msg.contains("§eYou will respawn in §r§c1 §r§esecond!")) {
                        this.reset = true;
                    }
                }
                if (event.getPacket() instanceof S08PacketPlayerPosLook) {
                    if (this.reset) {
                        this.reset = false;
                        return;
                    }
                    S08PacketPlayerPosLook packet = (S08PacketPlayerPosLook) event.getPacket();
                    event.setCancelled(true);
                    double x = packet.getX();
                    double y = packet.getY();
                    double z = packet.getZ();
                    float yaw = packet.getYaw();
                    float pitch = packet.getPitch();
                    if (packet.func_179834_f().contains(EnumFlags.X)) {
                        x += mc.thePlayer.posX;
                    } else {
                        mc.thePlayer.motionX = 0.0;
                    }
                    if (packet.func_179834_f().contains(EnumFlags.Y)) {
                        y += mc.thePlayer.posY;
                    } else {
                        mc.thePlayer.motionY = 0.0;
                    }
                    if (packet.func_179834_f().contains(EnumFlags.Z)) {
                        z += mc.thePlayer.posZ;
                    } else {
                        mc.thePlayer.motionZ = 0.0;
                    }
                    if (packet.func_179834_f().contains(EnumFlags.X_ROT)) {
                        pitch += mc.thePlayer.rotationPitch;
                    }
                    if (packet.func_179834_f().contains(EnumFlags.Y_ROT)) {
                        yaw += mc.thePlayer.rotationYaw;
                    }
                    mc.thePlayer
                            .setPositionAndRotation(
                                    x,
                                    y,
                                    z,
                                    RotationUtil.quantizeAngle(mc.thePlayer.rotationYaw + RandomUtil.nextFloat(-0.01F, 0.01F)),
                                    RotationUtil.quantizeAngle(mc.thePlayer.rotationPitch + RandomUtil.nextFloat(-0.01F, 0.01F))
                            );
                    PacketUtil.sendPacketNoEvent(
                            new C06PacketPlayerPosLook(
                                    mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY, mc.thePlayer.posZ, yaw % 360.0F, pitch % 360.0F, false
                            )
                    );
                }
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.reset = false;
    }

    @Override
    public void onDisabled() {
        this.reset = false;
    }

    @Override
    public Category category() {
        return Category.MISC;
    }
}
