package myau.module.modules;

import myau.Myau;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.Render2DEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.module.category.Category;
import myau.util.ChatUtil;
import myau.util.ColorUtil;
import myau.util.SoundUtil;
import myau.util.TeamUtil;
import myau.property.properties.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BedTracker extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final ScheduledExecutorService executor;
    private final LinkedHashMap<String, Long> alertCooldowns;
    private final LinkedHashSet<EntityEnderPearl> trackedPearls;
    private final LinkedHashSet<String> whitelistedPlayers;
    private final Color wBed;
    private final Color rBed;
    private final Color yBed;
    private final Color gBed;
    private BlockPos bedPos;
    private long lastMarcoTime;
    private boolean waiting;
    public final BooleanProperty alerts;
    public final IntProperty alertRange;
    public final BooleanProperty alertOnPearl;
    public final ModeProperty alertSound;
    public final IntProperty alertFrequency;
    public final BooleanProperty marco;
    public final IntProperty marcoRange;
    public final BooleanProperty marcoOnPreal;
    public final TextProperty marcoText;
    public final IntProperty marcoDelay;
    public final BooleanProperty hud;
    public final ModeProperty hudPosX;
    public final ModeProperty hudPosY;
    public final IntProperty hudOffX;
    public final IntProperty hudOffY;
    public final FloatProperty hudScale;
    public final BooleanProperty hudShadow;

    private void playAlertSound() {
        switch (this.alertSound.getValue()) {
            case 1:
                SoundUtil.playSound("mob.cat.meow");
                break;
            case 2:
                SoundUtil.playSound("random.anvil_land");
        }
    }

    private Color getHudColor(int distance) {
        if (distance < 0) {
            return this.wBed;
        } else if (distance <= 100) {
            return this.gBed;
        } else if (distance <= 114) {
            return ColorUtil.interpolate((float) (114 - distance) / 14.0F, this.yBed, this.gBed);
        } else {
            return distance <= 128 ? ColorUtil.interpolate((float) (128 - distance) / 14.0F, this.rBed, this.yBed) : this.rBed;
        }
    }

    private boolean isBed(BlockPos blockPos) {
        return blockPos != null && mc.theWorld.getBlockState(blockPos).getBlock() == Blocks.bed;
    }

    public BedTracker() {
        super("BedTracker", false, true);
        this.executor = Executors.newScheduledThreadPool(1);
        this.alertCooldowns = new LinkedHashMap<>();
        this.trackedPearls = new LinkedHashSet<>();
        this.whitelistedPlayers = new LinkedHashSet<>();
        this.wBed = new Color(ChatColors.WHITE.toAwtColor());
        this.rBed = new Color(ChatColors.RED.toAwtColor());
        this.yBed = new Color(ChatColors.YELLOW.toAwtColor());
        this.gBed = new Color(ChatColors.GREEN.toAwtColor());
        this.bedPos = null;
        this.lastMarcoTime = -1L;
        this.waiting = false;
        this.alerts = new BooleanProperty("alerts", true);
        this.alertRange = new IntProperty("alerts-range", 48, 8, 128, this.alerts::getValue);
        this.alertOnPearl = new BooleanProperty("alerts-on-pearl", true);
        this.alertSound = new ModeProperty("alerts-sound", 1, new String[]{"NONE", "MEOW", "ANVIL"}, () -> this.alerts.getValue() || this.alertOnPearl.getValue());
        this.alertFrequency = new IntProperty("alerts-frequency", 5, 1, 30, () -> this.alerts.getValue() || this.alertOnPearl.getValue());
        this.marco = new BooleanProperty("macro", false);
        this.marcoRange = new IntProperty("macro-range", 24, 8, 128, this.marco::getValue);
        this.marcoOnPreal = new BooleanProperty("macro-on-pearl", false);
        this.marcoText = new TextProperty("macro-text", "/lobby", () -> this.marco.getValue() || this.marcoOnPreal.getValue());
        this.marcoDelay = new IntProperty("macro-delay", 1, 1, 10, () -> this.marco.getValue() || this.marcoOnPreal.getValue());
        this.hud = new BooleanProperty("hud", true);
        this.hudPosX = new ModeProperty("hud-position-x", 0, new String[]{"LEFT", "MIDDLE", "RIGHT"}, this.hud::getValue);
        this.hudPosY = new ModeProperty("hud-position-y", 0, new String[]{"TOP", "MIDDLE", "BOTTOM"}, this.hud::getValue);
        this.hudOffX = new IntProperty("hud-offset-x", 2, 0, 255, this.hud::getValue);
        this.hudOffY = new IntProperty("hud-offset-y", 2, 0, 255, this.hud::getValue);
        this.hudScale = new FloatProperty("hud-scale", 1.0F, 0.5F, 1.5F, this.hud::getValue);
        this.hudShadow = new BooleanProperty("hud-shadow", true, this.hud::getValue);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST && this.isBed(this.bedPos)) {
            long millis = System.currentTimeMillis();
            boolean pearl = false;
            boolean marco = false;
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityEnderPearl) {
                    EntityEnderPearl enderPearl = (EntityEnderPearl) entity;
                    if (!this.trackedPearls.contains(enderPearl)) {
                        this.trackedPearls.add(enderPearl);
                        if (this.alertOnPearl.getValue()) {
                            ChatUtil.sendFormatted(String.format("%s%s: &fDetected &5Ender Pearl&r &e&l⚠&r", Myau.clientName, this.getName()));
                            pearl = true;
                        }
                        if (this.marcoOnPreal.getValue() && this.lastMarcoTime + (long) this.marcoDelay.getValue() * 1000L <= millis) {
                            this.lastMarcoTime = millis;
                            marco = true;
                        }
                    }
                }
            }
            for (EntityPlayer player : mc.theWorld
                    .loadedEntityList
                    .stream()
                    .filter(entity -> entity instanceof EntityPlayer)
                    .map(entity -> (EntityPlayer) entity)
                    .filter(entityPlayer -> !TeamUtil.isBot(entityPlayer) && !this.whitelistedPlayers.contains(entityPlayer.getName()))
                    .collect(Collectors.toList())) {
                if (TeamUtil.isSameTeam(player)) {
                    this.whitelistedPlayers.add(player.getName());
                } else {
                    double distance = player.getDistance((double) this.bedPos.getX() + 0.5, (double) this.bedPos.getY() + 0.5, (double) this.bedPos.getZ() + 0.5);
                    String name = player.getName();
                    String text = player.getDisplayName().getFormattedText();
                    ItemStack item = player.getHeldItem();
                    boolean isPearl = item != null && item.getItem() instanceof ItemEnderPearl;
                    if (this.alerts.getValue() && distance < (double) this.alertRange.getValue()) {
                        Long cooldown = this.alertCooldowns.get(name);
                        if (cooldown == null || cooldown + (long) this.alertFrequency.getValue() * 1000L <= millis) {
                            this.alertCooldowns.put(name, millis);
                            ChatUtil.sendFormatted(
                                    String.format("%s%s: %s&r &fis %d blocks away from your bed &e&l⚠&r", Myau.clientName, this.getName(), text, (int) distance + 1)
                            );
                            pearl = true;
                        }
                    }
                    if (this.alertOnPearl.getValue() && isPearl) {
                        Long cooldown = this.alertCooldowns.get(name);
                        if (cooldown == null || cooldown + (long) this.alertFrequency.getValue() * 1000L <= millis) {
                            this.alertCooldowns.put(name, millis);
                            ChatUtil.sendFormatted(
                                    String.format("%s%s: %s&r &fhas &5Ender Pearl&r &e&l⚠&r", Myau.clientName, this.getName(), text)
                            );
                            pearl = true;
                        }
                    }
                    if ((
                            this.marco.getValue() && distance < (double) this.marcoRange.getValue()
                                    || this.marcoOnPreal.getValue() && isPearl
                    )
                            && this.lastMarcoTime + (long) this.marcoDelay.getValue() * 1000L <= millis) {
                        this.lastMarcoTime = millis;
                        marco = true;
                    }
                }
            }
            if (pearl) {
                this.playAlertSound();
            }
            if (marco) {
                ChatUtil.sendRaw(
                        String.format(
                                ChatColors.formatColor("%s%s: &fRunning &6%s&r"),
                                ChatColors.formatColor(Myau.clientName),
                                this.getName(),
                                this.marcoText.getValue()
                        )
                );
                ChatUtil.sendMessage(this.marcoText.getValue());
            }
        }
    }

    @EventTarget(Priority.LOW)
    public void onRender(Render2DEvent event) {
        if (this.isEnabled() && this.hud.getValue()) {
            if (mc.theWorld != null && mc.thePlayer != null && !mc.gameSettings.showDebugInfo) {
                GuiScreen currentScreen = mc.currentScreen;
                if (currentScreen == null || currentScreen instanceof GuiChat) {
                    int distanceSq = 0;
                    boolean hasBed = this.isBed(this.bedPos);
                    if (hasBed) {
                        double xDiff = mc.thePlayer.posX - (double) this.bedPos.getX();
                        double zDiff = mc.thePlayer.posZ - (double) this.bedPos.getZ();
                        distanceSq = (int) Math.sqrt(xDiff * xDiff + zDiff * zDiff) + 1;
                    }
                    String text = ChatColors.formatColor(
                            String.format(
                                    "&fBed: %s%s",
                                    !hasBed ? "&cfalse&r" : "&atrue&r",
                                    !hasBed ? "" : String.format(" &7| &fDistance: &r%d%s", distanceSq, distanceSq >= 128 ? " &c&l⚠&r" : "")
                            )
                    );
                    ScaledResolution scaledResolution = new ScaledResolution(mc);
                    float width = (float) mc.fontRendererObj.getStringWidth(text);
                    float height = (float) mc.fontRendererObj.FONT_HEIGHT - 1.0F;
                    float scale = (float) this.hudOffX.getValue() / this.hudScale.getValue();
                    switch (this.hudPosX.getValue()) {
                        case 0:
                            scale++;
                            break;
                        case 1:
                            scale += (float) scaledResolution.getScaledWidth() / this.hudScale.getValue() / 2.0F - width / 2.0F;
                            break;
                        case 2:
                            scale = (scale + 1.0F) * -1.0F;
                            scale += (float) scaledResolution.getScaledWidth() / this.hudScale.getValue() - width;
                    }
                    float offset = (float) this.hudOffY.getValue() / this.hudScale.getValue();
                    switch (this.hudPosY.getValue()) {
                        case 0:
                            offset++;
                            break;
                        case 1:
                            offset += (float) scaledResolution.getScaledHeight() / this.hudScale.getValue() / 2.0F - height / 2.0F;
                            break;
                        case 2:
                            offset = (offset + 1.0F) * -1.0F;
                            offset += (float) scaledResolution.getScaledHeight() / this.hudScale.getValue() - height;
                    }
                    GlStateManager.pushMatrix();
                    GlStateManager.scale(this.hudScale.getValue(), this.hudScale.getValue(), 1.0F);
                    GlStateManager.translate(scale, offset, 0.0F);
                    GlStateManager.disableDepth();
                    GlStateManager.enableBlend();
                    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    mc.fontRendererObj.drawString(text, 0.0F, 0.0F, this.getHudColor(distanceSq).getRGB(), this.hudShadow.getValue());
                    GlStateManager.disableBlend();
                    GlStateManager.enableDepth();
                    GlStateManager.popMatrix();
                }
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.waiting = false;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            if (event.getPacket() instanceof S02PacketChat) {
                String msg = ((S02PacketChat) event.getPacket()).getChatComponent().getFormattedText();
                if (msg.contains("§e§lProtect your bed and destroy the enemy bed") || msg.contains("§e§lDestroy the enemy bed and then eliminate them")) {
                    this.alertCooldowns.clear();
                    this.trackedPearls.clear();
                    this.whitelistedPlayers.clear();
                    this.bedPos = null;
                    this.waiting = true;
                }
            }
            if (event.getPacket() instanceof S08PacketPlayerPosLook && this.waiting) {
                this.waiting = false;
                this.executor
                        .schedule(
                                () -> {
                                    int x = MathHelper.floor_double(mc.thePlayer.posX);
                                    int y = MathHelper.floor_double(mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
                                    int z = MathHelper.floor_double(mc.thePlayer.posZ);
                                    for (int i = x - 25; i <= x + 25; i++) {
                                        for (int j = y - 25; j <= y + 25; j++) {
                                            for (int k = z - 25; k <= z + 25; k++) {
                                                BlockPos blockPos = new BlockPos(i, j, k);
                                                if (this.isBed(blockPos)) {
                                                    this.bedPos = blockPos;
                                                    ChatUtil.sendFormatted(
                                                            String.format(
                                                                    "%s%s: &fWhitelisted your bed at (%d, %d, %d) &a&l✔&r",
                                                                    Myau.clientName,
                                                                    this.getName(),
                                                                    this.bedPos.getX(),
                                                                    this.bedPos.getY(),
                                                                    this.bedPos.getZ()
                                                            )
                                                    );
                                                    SoundUtil.playSound("note.pling");
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                },
                                3000L,
                                TimeUnit.MILLISECONDS
                        );
            }
        }
    }

    @Override
    public void onDisabled() {
        this.alertCooldowns.clear();
        this.trackedPearls.clear();
        this.whitelistedPlayers.clear();
        this.bedPos = null;
    }

    @Override
    public Category category() {
        return Category.MISC;
    }
}
