package myau.management;

import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.TickEvent;
import myau.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class BlinkManager {
    public static Minecraft mc = Minecraft.getMinecraft();
    public BlinkModules blinkModule = BlinkModules.NONE;
    public boolean blinking = false;
    public Deque<Packet<?>> blinkedPackets = new ConcurrentLinkedDeque<>();

    public boolean offerPacket(Packet<?> packet) {
        if (packet == null) return false;

        if (this.blinkModule == BlinkModules.NONE 
            || packet instanceof C00PacketKeepAlive 
            || packet instanceof C01PacketChatMessage) {
            return false;
        }

        if (this.blinkedPackets.isEmpty() && packet instanceof C0FPacketConfirmTransaction) {
            return false;
        }

        this.blinkedPackets.offer(packet);
        return true;
    }

    public boolean setBlinkState(boolean state, BlinkModules module) {
        if (module == BlinkModules.NONE) return false;

        try {
            if (state) {
                this.blinkModule = module;
                this.blinking = true;
            } else {
                if (blinkModule != module) return false;

                this.blinking = false;

                if (mc.thePlayer != null && mc.theWorld != null && mc.getNetHandler() != null) {
                    while (!blinkedPackets.isEmpty()) {
                        try {
                            Packet<?> blinkedPacket = blinkedPackets.poll();
                            if (blinkedPacket != null) PacketUtil.sendPacket(blinkedPacket);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                this.blinkModule = BlinkModules.NONE;
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.blinking = false;
            this.blinkedPackets.clear();
            this.blinkModule = BlinkModules.NONE;
        }

        return true;
    }

    public BlinkModules getBlinkingModule() {
        return this.blinkModule;
    }

    public long countMovement() {
        return this.blinkedPackets.stream().filter(packet -> packet instanceof C03PacketPlayer).count();
    }

    public boolean isBlinking() {
        return blinking;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        try {
            if (event.getPacket() != null &&
                (event.getPacket() instanceof C00Handshake
                 || event.getPacket() instanceof C00PacketLoginStart
                 || event.getPacket() instanceof C00PacketServerQuery
                 || event.getPacket() instanceof C01PacketPing
                 || event.getPacket() instanceof C01PacketEncryptionResponse)) {
                this.setBlinkState(false, this.blinkModule);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        try {
            if (event.getType() == EventType.POST 
                && mc.thePlayer != null 
                && mc.theWorld != null 
                && mc.thePlayer.isDead) {
                this.setBlinkState(false, this.blinkModule);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
