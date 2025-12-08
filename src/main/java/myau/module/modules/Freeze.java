package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.Render2DEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Freeze extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    private final List<Packet<INetHandlerPlayClient>> packets = new ArrayList<>();
    private boolean delaying = false;
    private int timeout = 0;
    private boolean s08 = false;
    private final int color = new Color(209, 1, 1, 255).getRGB();
    
    public final BooleanProperty renderTimer = new BooleanProperty("render-timer", false);
    public final IntProperty maxTimeout = new IntProperty("max-timeout", 300, 50, 600);

    public Freeze() {
        super("Freeze", false);
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE || event.isCancelled()) {
            return;
        }

        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        Packet<?> packet = event.getPacket();

        // Check for S08 flag (teleport packet)
        if (!this.delaying && packet instanceof S08PacketPlayerPosLook) {
            this.s08 = true;
        }

        // Check for velocity packet targeting player
        if (packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity s12 = (S12PacketEntityVelocity) packet;
            if (s12.getEntityID() == mc.thePlayer.getEntityId()) {
                // Ignore velocity right after teleport
                if (this.s08) {
                    this.s08 = false;
                    return;
                }
                // Start delaying
                this.delaying = true;
            }
        }
        
        // Check for damage (entity status packet with hurt animation)
        if (packet instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus s19 = (S19PacketEntityStatus) packet;
            if (s19.getEntity(mc.theWorld) != null && 
                s19.getEntity(mc.theWorld).equals(mc.thePlayer) && 
                s19.getOpCode() == 2) { // OpCode 2 = hurt animation
                this.delaying = true;
            }
        }

        // Buffer relevant packets when delaying
        if (this.delaying && (packet instanceof S12PacketEntityVelocity 
                || packet instanceof S32PacketConfirmTransaction 
                || packet instanceof S08PacketPlayerPosLook)) {
            
            synchronized (this.packets) {
                @SuppressWarnings("unchecked")
                Packet<INetHandlerPlayClient> playPacket = (Packet<INetHandlerPlayClient>) packet;
                this.packets.add(playPacket);
            }
            
            event.setCancelled(true);
        }
    }

    @EventTarget(Priority.MEDIUM)
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }

        if (event.getType() == EventType.POST) {
            // Check timeout
            if (this.delaying && ++this.timeout >= this.maxTimeout.getValue()) {
                this.flush();
                ChatUtil.sendFormatted(Myau.clientName + "&cFreeze timed out.");
            }

            // Reset S08 flag
            this.s08 = false;
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || !this.renderTimer.getValue()) {
            return;
        }

        if (mc.thePlayer == null || this.timeout == 0) {
            return;
        }

        if (mc.currentScreen != null) {
            return;
        }

        this.renderTimer(this.timeout);
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.flush();
    }

    private void renderTimer(int ticks) {
        int widthOffset = ticks < 10 ? 4 : 
                         (ticks >= 10 && ticks < 100 ? 7 : 
                         (ticks >= 100 && ticks < 1000 ? 10 : 13));
        
        String text = String.valueOf(ticks);
        int width = mc.fontRendererObj.getStringWidth(text);
        ScaledResolution sr = new ScaledResolution(mc);
        
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();
        float yadd = 8.0f;
        
        mc.fontRendererObj.drawStringWithShadow(
            text,
            (float)(screenWidth / 2 - width + widthOffset),
            (float)(screenHeight / 2 + (int)yadd),
            this.color
        );
    }

    private void flush() {
        if (this.packets.isEmpty()) {
            this.delaying = false;
            this.timeout = 0;
            return;
        }

        synchronized (this.packets) {
            while (!this.packets.isEmpty()) {
                Packet<INetHandlerPlayClient> packet = this.packets.remove(0);
                
                try {
                    packet.processPacket(mc.getNetHandler());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        this.delaying = false;
        this.timeout = 0;
    }

    @Override
    public void onDisabled() {
        this.flush();
    }

    @Override
    public String[] getSuffix() {
        if (this.delaying && this.timeout > 0) {
            return new String[]{String.valueOf(this.timeout)};
        }
        return new String[]{"Ready"};
    }
}