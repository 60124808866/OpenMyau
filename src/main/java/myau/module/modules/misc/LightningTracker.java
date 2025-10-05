package myau.module.modules.misc;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.module.Category;
import myau.module.Module;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity;

public class LightningTracker extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private String getDirection(double playerX, double playerZ, double lightningX, double lightningZ) {
        double threshold = Math.sqrt(2.0) - 1.0;
        double xDiff = lightningX - playerX;
        double yDiff = lightningZ - playerZ;
        if (Math.abs(xDiff) > Math.abs(yDiff)) {
            if (Math.abs(yDiff / xDiff) <= threshold) {
                return xDiff > 0.0 ? "E" : "W";
            } else if (xDiff > 0.0) {
                return yDiff > 0.0 ? "SE" : "NE";
            } else {
                return yDiff > 0.0 ? "SW" : "NW";
            }
        } else if (Math.abs(yDiff) > 0.0) {
            if (Math.abs(xDiff / yDiff) <= threshold) {
                return yDiff > 0.0 ? "S" : "N";
            } else if (yDiff > 0.0) {
                return xDiff > 0.0 ? "SE" : "SW";
            } else {
                return xDiff > 0.0 ? "NE" : "NW";
            }
        } else {
            return "?";
        }
    }

    public LightningTracker() {
        super("LightningTracker", Category.Misc, false, true);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.RECEIVE && event.getPacket() instanceof S2CPacketSpawnGlobalEntity) {
            S2CPacketSpawnGlobalEntity packet = (S2CPacketSpawnGlobalEntity) event.getPacket();
            if (packet.func_149053_g() == 1) {
                double x = (double) packet.func_149051_d() / 32.0;
                double y = (double) packet.func_149050_e() / 32.0;
                double z = (double) packet.func_149049_f() / 32.0;
                double distance = mc.thePlayer.getDistance(x, y, z);
                String direction = this.getDirection(mc.thePlayer.posX, mc.thePlayer.posZ, x, z);
                ChatUtil.sendFormatted(
                        String.format(
                                "&8[&e%s&8] &7X: &f&l%d&r &7Y: &f&l%d&r &7Z: &f&l%d&r &7D: &6&l%d&r &6%s&r",
                                this.getName(),
                                (int) x,
                                (int) y,
                                (int) z,
                                (int) distance,
                                direction
                        )
                );
            }
        }
    }
}
