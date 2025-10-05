package myau.module.modules.render;

import myau.Myau;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.Render2DEvent;
import myau.module.Category;
import myau.module.Module;
import myau.module.modules.combat.KillAura;
import myau.util.ColorUtil;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import myau.util.TimerUtil;
import myau.property.properties.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TargetHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat healthFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    private static final DecimalFormat diffFormat = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));
    private final TimerUtil lastAttackTimer = new TimerUtil();
    private final TimerUtil animTimer = new TimerUtil();
    private EntityLivingBase lastTarget = null;
    private EntityLivingBase target = null;
    private ResourceLocation headTexture = null;
    private float oldHealth = 0.0F;
    private float newHealth = 0.0F;
    private float maxHealth = 0.0F;
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"DEFAULT", "HUD"});
    public final ModeProperty posX = new ModeProperty("position-x", 1, new String[]{"LEFT", "MIDDLE", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 1, new String[]{"TOP", "MIDDLE", "BOTTOM"});
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final IntProperty offX = new IntProperty("offset-x", 0, -255, 255);
    public final IntProperty offY = new IntProperty("offset-y", 40, -255, 255);
    public final PercentProperty background = new PercentProperty("background", 25);
    public final BooleanProperty head = new BooleanProperty("head", true);
    public final BooleanProperty indicator = new BooleanProperty("indicator", true);
    public final BooleanProperty outline = new BooleanProperty("outline", false);
    public final BooleanProperty animations = new BooleanProperty("animations", true);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty kaOnly = new BooleanProperty("ka-only", true);
    public final BooleanProperty chatPreview = new BooleanProperty("chat-preview", false);

    private EntityLivingBase resolveTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura.isEnabled() && killAura.isAttackAllowed() && TeamUtil.isEntityLoaded(killAura.getTarget())) {
            return killAura.getTarget();
        } else if (!(Boolean) this.kaOnly.getValue()
                && !this.lastAttackTimer.hasTimeElapsed(1500L)
                && TeamUtil.isEntityLoaded(this.lastTarget)) {
            return this.lastTarget;
        } else {
            return this.chatPreview.getValue() && mc.currentScreen instanceof GuiChat ? mc.thePlayer : null;
        }
    }

    private ResourceLocation getSkin(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(entityLivingBase.getName());
            if (playerInfo != null) {
                return playerInfo.getLocationSkin();
            }
        }
        return null;
    }

    private Color getTargetColor(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            if (TeamUtil.isFriend((EntityPlayer) entityLivingBase)) {
                return Myau.friendManager.getColor();
            }
            if (TeamUtil.isTarget((EntityPlayer) entityLivingBase)) {
                return Myau.targetManager.getColor();
            }
        }
        switch (this.color.getValue()) {
            case 0:
                if (!(entityLivingBase instanceof EntityPlayer)) {
                    return new Color(-1);
                }
                return TeamUtil.getTeamColor((EntityPlayer) entityLivingBase, 1.0F);
            case 1:
                int rgb = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
                return new Color(rgb);
            default:
                return new Color(-1);
        }
    }

    public TargetHUD() {
        super("TargetHUD", Category.Render, false, true);
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled() && mc.thePlayer != null) {
            EntityLivingBase entityLivingBase = this.target;
            this.target = this.resolveTarget();
            if (this.target != null) {
                float health = (mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) / 2.0F;
                float abs = this.target.getAbsorptionAmount() / 2.0F;
                float heal = this.target.getHealth() / 2.0F + abs;
                if (this.target != entityLivingBase) {
                    this.headTexture = null;
                    this.animTimer.setTime();
                    this.oldHealth = heal;
                    this.newHealth = heal;
                }
                if (!this.animations.getValue() || this.animTimer.hasTimeElapsed(150L)) {
                    this.oldHealth = this.newHealth;
                    this.newHealth = heal;
                    this.maxHealth = this.target.getMaxHealth() / 2.0F;
                    if (this.oldHealth != this.newHealth) {
                        this.animTimer.reset();
                    }
                }
                ResourceLocation resourceLocation = this.getSkin(this.target);
                if (resourceLocation != null) {
                    this.headTexture = resourceLocation;
                }
                float elapsedTime = (float) Math.min(Math.max(this.animTimer.getElapsedTime(), 0L), 150L);
                float healthRatio = Math.min(Math.max(RenderUtil.lerpFloat(this.newHealth, this.oldHealth, elapsedTime / 150.0F) / this.maxHealth, 0.0F), 1.0F);
                Color targetColor = this.getTargetColor(this.target);
                Color healthBarColor = this.color.getValue() == 0 ? ColorUtil.getHealthBlend(healthRatio) : targetColor;
                float healthDeltaRatio = Math.min(Math.max((health - heal + 1.0F) / 2.0F, 0.0F), 1.0F);
                Color healthDeltaColor = ColorUtil.getHealthBlend(healthDeltaRatio);
                ScaledResolution scaledResolution = new ScaledResolution(mc);
                String targetNameText = ChatColors.formatColor(String.format("&r%s&r", TeamUtil.stripName(this.target)));
                int targetNameWidth = mc.fontRendererObj.getStringWidth(targetNameText);
                String healthText = ChatColors.formatColor(
                        String.format("&r&f%s%s❤&r", healthFormat.format(heal), abs > 0.0F ? "&6" : "&c")
                );
                int healthTextWidth = mc.fontRendererObj.getStringWidth(healthText);
                String statusText = ChatColors.formatColor(String.format("&r&l%s&r", heal == health ? "D" : (heal < health ? "W" : "L")));
                int statusTextWidth = mc.fontRendererObj.getStringWidth(statusText);
                String healthDiffText = ChatColors.formatColor(
                        String.format("&r%s&r", heal == health ? "0.0" : diffFormat.format(health - heal))
                );
                int healthDiffWidth = mc.fontRendererObj.getStringWidth(healthDiffText);
                float barContentWidth = Math.max(
                        (float) targetNameWidth + (this.indicator.getValue() ? 2.0F + (float) statusTextWidth + 2.0F : 0.0F),
                        (float) healthTextWidth + (this.indicator.getValue() ? 2.0F + (float) healthDiffWidth + 2.0F : 0.0F)
                );
                float headIconOffset = this.head.getValue() && this.headTexture != null ? 25.0F : 0.0F;
                float barTotalWidth = Math.max(headIconOffset + 70.0F, headIconOffset + 2.0F + barContentWidth + 2.0F);
                float posX = this.offX.getValue().floatValue() / this.scale.getValue();
                switch (this.posX.getValue()) {
                    case 1:
                        posX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() / 2.0F - barTotalWidth / 2.0F;
                        break;
                    case 2:
                        posX *= -1.0F;
                        posX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() - barTotalWidth;
                }
                float posY = this.offY.getValue().floatValue() / this.scale.getValue();
                switch (this.posY.getValue()) {
                    case 1:
                        posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() / 2.0F - 13.5F;
                        break;
                    case 2:
                        posY *= -1.0F;
                        posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() - 27.0F;
                }
                GlStateManager.pushMatrix();
                GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);
                GlStateManager.translate(posX, posY, -450.0F);
                RenderUtil.enableRenderState();
                int backgroundColor = new Color(0.0F, 0.0F, 0.0F, (float) this.background.getValue() / 100.0F).getRGB();
                int outlineColor = this.outline.getValue() ? targetColor.getRGB() : new Color(0, 0, 0, 0).getRGB();
                RenderUtil.drawOutlineRect(0.0F, 0.0F, barTotalWidth, 27.0F, 1.5F, backgroundColor, outlineColor);
                RenderUtil.drawRect(headIconOffset + 2.0F, 22.0F, barTotalWidth - 2.0F, 25.0F, ColorUtil.darker(healthBarColor, 0.2F).getRGB());
                RenderUtil.drawRect(headIconOffset + 2.0F, 22.0F, headIconOffset + 2.0F + healthRatio * (barTotalWidth - 2.0F - headIconOffset - 2.0F), 25.0F, healthBarColor.getRGB());
                RenderUtil.disableRenderState();
                GlStateManager.disableDepth();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                mc.fontRendererObj.drawString(targetNameText, headIconOffset + 2.0F, 2.0F, -1, this.shadow.getValue());
                mc.fontRendererObj.drawString(healthText, headIconOffset + 2.0F, 12.0F, -1, this.shadow.getValue());
                if (this.indicator.getValue()) {
                    mc.fontRendererObj.drawString(statusText, barTotalWidth - 2.0F - (float) statusTextWidth, 2.0F, healthDeltaColor.getRGB(), this.shadow.getValue());
                    mc.fontRendererObj.drawString(healthDiffText, barTotalWidth - 2.0F - (float) healthDiffWidth, 12.0F, ColorUtil.darker(healthDeltaColor, 0.8F).getRGB(), this.shadow.getValue());
                }
                if (this.head.getValue() && this.headTexture != null) {
                    GlStateManager.color(1.0F, 1.0F, 1.0F);
                    mc.getTextureManager().bindTexture(this.headTexture);
                    Gui.drawScaledCustomSizeModalRect(2, 2, 8.0F, 8.0F, 8, 8, 23, 23, 64.0F, 64.0F);
                    Gui.drawScaledCustomSizeModalRect(2, 2, 40.0F, 8.0F, 8, 8, 23, 23, 64.0F, 64.0F);
                    GlStateManager.color(1.0F, 1.0F, 1.0F);
                }
                GlStateManager.disableBlend();
                GlStateManager.enableDepth();
                GlStateManager.popMatrix();
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() != Action.ATTACK) {
                return;
            }
            Entity entity = packet.getEntityFromWorld(mc.theWorld);
            if (entity instanceof EntityLivingBase) {
                if (entity instanceof EntityArmorStand) {
                    return;
                }
                this.lastAttackTimer.reset();
                this.lastTarget = (EntityLivingBase) entity;
            }
        }
    }
}
