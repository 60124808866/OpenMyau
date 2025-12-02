package myau.module.modules;

import myau.Myau;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.Priority;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.*;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.stream.Collectors;

public class Radar extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty colorMode = new ModeProperty("color", 0, new String[]{"DEFAULT", "TEAMS", "HUD"});
    public final IntProperty position = new IntProperty("position", 0, 0, 4);
    public final IntProperty offsetX = new IntProperty("offsetX", 60, 0, 1000, () -> position.getValue() != 4);
    public final IntProperty offsetY = new IntProperty("offsetY", 60, 0, 1000, () -> position.getValue() != 4);
    public final IntProperty radarRadius = new IntProperty("radarRadius", 55, 10, 200);
    public final FloatProperty dotRadius = new FloatProperty("dotRadius", 1.5F, 0.1F, 5.0F);
    public final BooleanProperty showPlayers = new BooleanProperty("players", true);
    public final BooleanProperty showFriends = new BooleanProperty("friends", true);
    public final BooleanProperty showEnemies = new BooleanProperty("enemies", true);
    public final BooleanProperty showBots = new BooleanProperty("bots", false);
    public final BooleanProperty showPVP = new BooleanProperty("showPVP", false);
    public final ColorProperty fillColor = new ColorProperty("fillColor", Color.GRAY.getRGB(), 0x40);
    public final ColorProperty outlineColor = new ColorProperty("outlineColor", Color.DARK_GRAY.getRGB());
    public final ColorProperty crossColor = new ColorProperty("crossColor", Color.LIGHT_GRAY.getRGB(), 0x80);
    public Radar() {
        super("Radar", false);
    }

    private boolean shouldRender(EntityPlayer entityPlayer) {
        if (entityPlayer.deathTime > 0) {
            return false;
        } else if (mc.getRenderViewEntity().getDistanceToEntity(entityPlayer) > 512.0F) {
            return false;
        } else if (entityPlayer != mc.thePlayer && entityPlayer != mc.getRenderViewEntity()) {
            if (TeamUtil.isBot(entityPlayer)) {
                return this.showBots.getValue();
            } else if (TeamUtil.isFriend(entityPlayer)) {
                return this.showFriends.getValue();
            } else {
                return TeamUtil.isTarget(entityPlayer) ? this.showEnemies.getValue() : this.showPlayers.getValue();
            }
        } else {
            return false;
        }
    }

    private Color getEntityColor(EntityPlayer entityPlayer) {
        if (TeamUtil.isFriend(entityPlayer)) {
            Color color = Myau.friendManager.getColor();
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
        } else if (TeamUtil.isTarget(entityPlayer)) {
            Color color = Myau.targetManager.getColor();
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
        } else {
            switch (this.colorMode.getValue()) {
                case 0:
                    return TeamUtil.getTeamColor(entityPlayer, 1.0F);
                case 1:
                    int teamColor = TeamUtil.isSameTeam(entityPlayer) ? ChatColors.BLUE.toAwtColor() : ChatColors.RED.toAwtColor();
                    return new Color(teamColor | 255 << 24, true);
                case 2:
                    int color = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
                    return new Color(color | 255 << 24, true);
                default:
                    return Color.WHITE;
            }
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onRender(Render2DEvent event) {
        if (!this.isEnabled()) return;

        ScaledResolution sr = new ScaledResolution(mc);
        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);

        double centerX, centerY;
        if (position.getValue() == 4) {
            centerX = sr.getScaledWidth() / 2.0F;
            centerY = sr.getScaledHeight() / 2.0F;
        } else {
            centerX = (position.getValue() & 0x1) == 0x1 ? Math.max(sr.getScaledWidth() - offsetX.getValue(), 0) : Math.min(offsetX.getValue(), sr.getScaledWidth());
            centerY = (position.getValue() & 0x2) == 0x2 ? Math.max(sr.getScaledHeight() - offsetY.getValue(), 0) : Math.min(offsetY.getValue(), sr.getScaledHeight());
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(hud.scale.getValue(), hud.scale.getValue(), 1.0f);
        GlStateManager.translate(centerX, centerY, 0.0f);

        RenderUtil.enableRenderState();

        float yaw = (float)Math.toRadians(mc.thePlayer.rotationYaw);
        if (mc.gameSettings.thirdPersonView != 2) {
            yaw += (float)Math.toRadians(180.0F);
        }
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);

        this.drawRadarCircle(0.0, 0, yaw, radarRadius.getValue(), 64, fillColor.getValue(), outlineColor.getValue(), crossColor.getValue());
        for (EntityPlayer player : TeamUtil.getLoadedEntitiesSorted().stream().filter(entity -> entity instanceof EntityPlayer && this.shouldRender((EntityPlayer) entity)).map(EntityPlayer.class::cast).collect(Collectors.toList())) {
            double dx = (player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks()) - mc.thePlayer.posX;
            double dz = (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks()) - mc.thePlayer.posZ;

            double relX = dx * cos + dz * sin;
            double relY = dz * cos - dx * sin;

            double dist = Math.sqrt(relX * relX + relY * relY);
            double scale = dist < radarRadius.getValue() ? 1.0F : radarRadius.getValue() / dist;
            double px = relX * scale;
            double py = relY * scale;

            RenderUtil.fillCircle(px, py, dotRadius.getValue(), 12, getEntityColor(player).getRGB());

        }
        if (this.showPVP.getValue()) {
            double dx = - mc.thePlayer.posX;
            double dz = - mc.thePlayer.posZ;

            double relX = dx * cos + dz * sin;
            double relY = dz * cos - dx * sin;

            double dist = Math.sqrt(relX * relX + relY * relY);
            double scale = dist < radarRadius.getValue() * 2 ? 1.0F : radarRadius.getValue() * 2 / dist;
            double px = relX * scale;
            double py = relY * scale;
            GlStateManager.pushMatrix();
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.scale(hud.scale.getValue() / 2, hud.scale.getValue() / 2, 1.0f);
            mc.fontRendererObj.drawString("PVP",
                    (float) (px - mc.fontRendererObj.getStringWidth("PVP") / 2.0F),
                    (float) (py - mc.fontRendererObj.FONT_HEIGHT / 2.0F),
                    Color.WHITE.getRGB(), hud.shadow.getValue());
            GlStateManager.popMatrix();
        }
        RenderUtil.disableRenderState();
        GlStateManager.popMatrix();
    }

    public void drawRadarCircle(double x, double y, double angle, double radius,
                                       int segments,
                                       int fillColor,
                                       int outlineColor,
                                       int crossColor) {

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        if ((fillColor >>> 24) != 0) {
            RenderUtil.setColor(fillColor);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex2d(x, y);
            for (int i = 0; i <= segments; i++) {
                double angle1 = i * (Math.PI * 2 / segments);
                GL11.glVertex2d(
                        x + Math.cos(angle1) * radius,
                        y + Math.sin(angle1) * radius
                );
            }
            GL11.glEnd();
        }

        if ((outlineColor >>> 24) != 0) {
            RenderUtil.setColor(outlineColor);
            GL11.glLineWidth(2f);

            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (int i = 0; i <= segments; i++) {
                double angle1 = i * (Math.PI * 2 / segments);
                GL11.glVertex2d(
                        x + Math.cos(angle1) * radius,
                        y + Math.sin(angle1) * radius
                );
            }
            GL11.glEnd();
        }

        if ((crossColor >>> 24) != 0) {
            RenderUtil.setColor(crossColor);
            GL11.glLineWidth(1.5f);
            GL11.glBegin(GL11.GL_LINES);

            double dx1 = Math.sin(angle);
            double dy1 = Math.cos(angle);

            double dx2 = Math.sin(angle + Math.PI / 2);
            double dy2 = Math.cos(angle + Math.PI / 2);

            GL11.glVertex2d(x - dx1 * radius, y - dy1 * radius);
            GL11.glVertex2d(x + dx1 * radius, y + dy1 * radius);

            GL11.glVertex2d(x - dx2 * radius, y - dy2 * radius);
            GL11.glVertex2d(x + dx2 * radius, y + dy2 * radius);

            GL11.glEnd();

            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
            int color = hud.getColor(System.currentTimeMillis()).getRGB();
            mc.fontRendererObj.drawString("N",
                    (float) (x - dx1 * (radius + 5)) - mc.fontRendererObj.getStringWidth("N") / 2.0F,
                    (float) (y - dy1 * (radius + 5)) - mc.fontRendererObj.FONT_HEIGHT / 2.0F,
                    color, hud.shadow.getValue());
            mc.fontRendererObj.drawString("E",
                    (float) (x + dx2 * (radius + 5)) - mc.fontRendererObj.getStringWidth("E") / 2.0F,
                    (float) (y + dy2 * (radius + 5)) - mc.fontRendererObj.FONT_HEIGHT / 2.0F,
                    color, hud.shadow.getValue());
            mc.fontRendererObj.drawString("S",
                    (float) (x + dx1 * (radius + 5)) - mc.fontRendererObj.getStringWidth("S") / 2.0F,
                    (float) (y + dy1 * (radius + 5)) - mc.fontRendererObj.FONT_HEIGHT / 2.0F,
                    color, hud.shadow.getValue());
            mc.fontRendererObj.drawString("W",
                    (float) (x - dx2 * (radius + 5)) - mc.fontRendererObj.getStringWidth("W") / 2.0F,
                    (float) (y - dy2 * (radius + 5)) - mc.fontRendererObj.FONT_HEIGHT / 2.0F,
                    color, hud.shadow.getValue());
            GlStateManager.disableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.resetColor();
    }
}
