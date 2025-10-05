package myau.module.modules.render;

import myau.Myau;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.Priority;
import myau.events.Render2DEvent;
import myau.events.Render3DEvent;
import myau.events.ResizeEvent;
import myau.mixin.IAccessorEntityRenderer;
import myau.mixin.IAccessorRenderManager;
import myau.module.Category;
import myau.module.Module;
import myau.util.ColorUtil;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import myau.util.shader.GlowShader;
import myau.util.shader.OutlineShader;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.player.EntityPlayer;

import javax.vecmath.Vector4d;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class ESP extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final OutlineShader outlineRenderer = new OutlineShader();
    private final GlowShader glowShader = new GlowShader();
    private Framebuffer framebuffer = null;
    private boolean outline = true;
    private boolean glow = true;
    public final ModeProperty mode = new ModeProperty("mode", 2, new String[]{"NONE", "2D", "3D", "OUTLINE"});
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"DEFAULT", "TEAMS", "HUD"});
    public final ModeProperty healthBar = new ModeProperty("health-bar", 0, new String[]{"NONE", "2D", "RAVEN"});
    public final BooleanProperty players = new BooleanProperty("players", true);
    public final BooleanProperty friends = new BooleanProperty("friends", true);
    public final BooleanProperty enemies = new BooleanProperty("enemies", true);
    public final BooleanProperty self = new BooleanProperty("self", false);
    public final BooleanProperty bots = new BooleanProperty("bots", false);

    private boolean shouldRenderPlayer(EntityPlayer entityPlayer) {
        if (entityPlayer.deathTime > 0) {
            return false;
        } else if (mc.getRenderViewEntity().getDistanceToEntity(entityPlayer) > 512.0F) {
            return false;
        } else if (!entityPlayer.ignoreFrustumCheck && !RenderUtil.isInViewFrustum(entityPlayer.getEntityBoundingBox(), 0.1F)) {
            return false;
        } else if (entityPlayer != mc.thePlayer && entityPlayer != mc.getRenderViewEntity()) {
            if (TeamUtil.isBot(entityPlayer)) {
                return this.bots.getValue();
            } else if (TeamUtil.isFriend(entityPlayer)) {
                return this.friends.getValue();
            } else {
                return TeamUtil.isTarget(entityPlayer) ? this.enemies.getValue() : this.players.getValue();
            }
        } else {
            return this.self.getValue() && mc.gameSettings.thirdPersonView != 0;
        }
    }

    private Color getEntityColor(EntityPlayer entityPlayer) {
        if (TeamUtil.isFriend(entityPlayer)) {
            return Myau.friendManager.getColor();
        } else if (TeamUtil.isTarget(entityPlayer)) {
            return Myau.targetManager.getColor();
        } else {
            switch (this.color.getValue()) {
                case 0:
                    return TeamUtil.getTeamColor(entityPlayer, 1.0F);
                case 1:
                    int teamColor = TeamUtil.isSameTeam(entityPlayer) ? ChatColors.BLUE.toAwtColor() : ChatColors.RED.toAwtColor();
                    return new Color(teamColor);
                case 2:
                    int hudColor = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
                    return new Color(hudColor);
                default:
                    return new Color(-1);
            }
        }
    }

    public ESP() {
        super("ESP", Category.Render, false);
    }

    public boolean isOutlineEnabled() {
        return this.outline;
    }

    public boolean isGlowEnabled() {
        return this.glow;
    }

    @EventTarget
    public void onResize(ResizeEvent event) {
        if (this.framebuffer != null) {
            this.framebuffer.deleteFramebuffer();
        }
        this.framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
    }

    @EventTarget(Priority.HIGH)
    public void onRender(Render2DEvent event) {
        if (this.isEnabled() && (this.mode.getValue() == 1 || this.mode.getValue() == 3 || this.healthBar.getValue() == 1)) {
            List<EntityPlayer> renderedEntities = TeamUtil.getLoadedEntitiesSorted().stream().filter(entity -> entity instanceof EntityPlayer && this.shouldRenderPlayer((EntityPlayer) entity)).map(EntityPlayer.class::cast).collect(Collectors.toList());
            if (!renderedEntities.isEmpty()) {
                if (this.mode.getValue() == 3) {
                    GlStateManager.pushMatrix();
                    GlStateManager.pushAttrib();
                    if (this.framebuffer == null) {
                        this.framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
                    }
                    this.framebuffer.bindFramebuffer(false);
                    ((IAccessorEntityRenderer) mc.entityRenderer).callSetupCameraTransform(event.getPartialTicks(), 0);
                    boolean shadow = mc.gameSettings.entityShadows;
                    mc.gameSettings.entityShadows = false;
                    this.outline = false;
                    this.glow = false;
                    this.glowShader.use();
                    for (EntityPlayer player : renderedEntities) {
                        Color entityColor = this.getEntityColor(player);
                        this.glowShader.W(entityColor);
                        boolean invisible = player.isInvisible();
                        player.setInvisible(false);
                        mc.getRenderManager().renderEntityStatic(player, event.getPartialTicks(), true);
                        player.setInvisible(invisible);
                    }
                    this.glowShader.stop();
                    this.glow = true;
                    this.outline = true;
                    mc.gameSettings.entityShadows = shadow;
                    mc.entityRenderer.disableLightmap();
                    mc.entityRenderer.setupOverlayRendering();
                    mc.getFramebuffer().bindFramebuffer(false);
                    this.outlineRenderer.use();
                    RenderUtil.drawFramebuffer(this.framebuffer);
                    this.outlineRenderer.stop();
                    this.framebuffer.framebufferClear();
                    mc.getFramebuffer().bindFramebuffer(false);
                    GlStateManager.popAttrib();
                    GlStateManager.popMatrix();
                }
                if (this.mode.getValue() == 1 || this.healthBar.getValue() == 1) {
                    RenderUtil.enableRenderState();
                    double scaleFactor = new ScaledResolution(mc).getScaleFactor();
                    double scale = scaleFactor / Math.pow(scaleFactor, 2.0);
                    GlStateManager.pushMatrix();
                    GlStateManager.scale(scale, scale, scale);
                    for (EntityPlayer player : renderedEntities) {
                        ((IAccessorEntityRenderer) mc.entityRenderer).callSetupCameraTransform(event.getPartialTicks(), 0);
                        Vector4d screenPosition = RenderUtil.projectToScreen(player, scaleFactor);
                        mc.entityRenderer.setupOverlayRendering();
                        if (screenPosition != null) {
                            float x = (float) screenPosition.x;
                            float y = (float) screenPosition.y;
                            float z = (float) screenPosition.z;
                            float w = (float) screenPosition.w;
                            if (this.mode.getValue() == 1) {
                                int color = this.getEntityColor(player).getRGB();
                                RenderUtil.drawOutlineRect(x, y, z, w, 3.0F, 0, (color & 16579836) >> 2 | color & 0xFF000000);
                                RenderUtil.drawOutlineRect(x, y, z, w, 1.5F, 0, color);
                            }
                            if (this.healthBar.getValue() == 1) {
                                float heal = player.getHealth() + player.getAbsorptionAmount();
                                float percent = Math.min(Math.max(heal / player.getMaxHealth(), 0.0F), 1.0F);
                                float box = (z - x) * 0.08F;
                                Color healthColor = ColorUtil.getHealthBlend(percent);
                                RenderUtil.drawLine(x - box, y, x - box, w, 3.0F, ColorUtil.darker(healthColor, 0.2F).getRGB());
                                RenderUtil.drawLine(x - box, w, x - box, w + (y - w) * percent, 1.5F, healthColor.getRGB());
                            }
                        }
                    }
                    GlStateManager.popMatrix();
                    RenderUtil.disableRenderState();
                }
            }
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled() && (this.mode.getValue() == 2 || this.healthBar.getValue() == 2)) {
            RenderUtil.enableRenderState();
            for (EntityPlayer player : TeamUtil.getLoadedEntitiesSorted().stream().filter(entity -> entity instanceof EntityPlayer && this.shouldRenderPlayer((EntityPlayer) entity)).map(EntityPlayer.class::cast).collect(Collectors.toList())) {
                if (player.ignoreFrustumCheck || RenderUtil.isInViewFrustum(player.getEntityBoundingBox(), 0.1F)) {
                    if (this.mode.getValue() == 2) {
                        Color color = this.getEntityColor(player);
                        RenderUtil.drawEntityBoundingBox(player, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 1.5F, 0.1F);
                        GlStateManager.resetColor();
                    }
                    if (this.healthBar.getValue() == 2) {
                        double x = RenderUtil.lerpDouble(player.posX, player.lastTickPosX, event.getPartialTicks())
                                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
                        double y = RenderUtil.lerpDouble(player.posY, player.lastTickPosY, event.getPartialTicks())
                                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY()
                                - 0.1F;
                        double z = RenderUtil.lerpDouble(player.posZ, player.lastTickPosZ, event.getPartialTicks())
                                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
                        GlStateManager.pushMatrix();
                        GlStateManager.translate(x, y, z);
                        GlStateManager.rotate(mc.getRenderManager().playerViewY * -1.0F, 0.0F, 1.0F, 0.0F);
                        float heal = player.getHealth() + player.getAbsorptionAmount();
                        float percent = Math.min(Math.max(heal / player.getMaxHealth(), 0.0F), 1.0F);
                        Color healthColor = ColorUtil.getHealthBlend(percent);
                        float height = player.height + 0.2F;
                        RenderUtil.drawRect3D(0.57250005F, -0.027500002F, 0.7275F, height + 0.027500002F, Color.black.getRGB());
                        RenderUtil.drawRect3D(0.6F, 0.0F, 0.70000005F, height, Color.darkGray.getRGB());
                        RenderUtil.drawRect3D(0.6F, 0.0F, 0.70000005F, height * percent, healthColor.getRGB());
                        GlStateManager.popMatrix();
                    }
                }
            }
            RenderUtil.disableRenderState();
        }
    }
}
