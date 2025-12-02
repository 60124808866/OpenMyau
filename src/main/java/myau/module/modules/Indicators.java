package myau.module.modules;

import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.util.RenderUtil;
import myau.util.RotationUtil;
import myau.util.TeamUtil;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.awt.*;
import java.util.stream.Collectors;

public class Indicators extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final FloatProperty scale = new FloatProperty("scale", 1.0f, 0.5f, 1.5f);
    public final FloatProperty offset = new FloatProperty("offset", 50.0f, 0.0f, 255.0f);
    public final BooleanProperty directionCheck = new BooleanProperty("direction-check", true);
    public final BooleanProperty fireballs = new BooleanProperty("fireballs", true);
    public final BooleanProperty pearls = new BooleanProperty("pearls", true);
    public final BooleanProperty arrows = new BooleanProperty("arrows", true);
    public final BooleanProperty egg = new BooleanProperty("egg", true);
    public final BooleanProperty snowball = new BooleanProperty("snowball", true);

    private boolean shouldRender(Entity entity) {
        double d = (entity.posX - entity.lastTickPosX) * (Indicators.mc.thePlayer.posX - entity.posX) + (entity.posY - entity.lastTickPosY) * (Indicators.mc.thePlayer.posY + (double) Indicators.mc.thePlayer.getEyeHeight() - entity.posY - (double) entity.height / 2.0) + (entity.posZ - entity.lastTickPosZ) * (Indicators.mc.thePlayer.posZ - entity.posZ);
        if (d == 0.0) {
            return false;
        }
        if (d < 0.0) {
            if (this.directionCheck.getValue()) {
                return false;
            }
        }
        if (this.fireballs.getValue() && entity instanceof EntityFireball) return true;
        if (this.pearls.getValue() && entity instanceof EntityEnderPearl) return true;
        if (this.arrows.getValue() && entity instanceof EntityArrow) return true;
        if (this.egg.getValue() && entity instanceof EntityEgg) return true;
        if (this.snowball.getValue() && entity instanceof EntitySnowball) return true;
        return false;
    }

    private Item getIndicatorItem(Entity entity) {
        if (entity instanceof EntityFireball) {
            return Items.fire_charge;
        }
        if (entity instanceof EntityEnderPearl) {
            return Items.ender_pearl;
        }
        if (entity instanceof EntityArrow) {
            return Items.arrow;
        }
        if (entity instanceof EntityEgg) {
            return Items.egg;
        }
        if (entity instanceof EntitySnowball) {
            return Items.snowball;
        }
        return new Item();
    }

    private Color getIndicatorColor(Entity entity) {
        if (entity instanceof EntityFireball) {
            return new Color(12676363);
        }
        if (entity instanceof EntityEnderPearl) {
            return new Color(2458740);
        }
        if (entity instanceof EntityArrow) {
            return new Color(0x969696);
        }
        return new Color(-1);
    }

    public Indicators() {
        super("Indicators", false, true);
    }

    @EventTarget
    public void onRender(Render2DEvent render2DEvent) {
        if (!this.isEnabled()) {
            return;
        }
        for (Entity entity : TeamUtil.getLoadedEntitiesSorted().stream().filter(this::shouldRender).collect(Collectors.toList())) {
            float offset = 10.0f + this.offset.getValue();
            float yawBetween = RotationUtil.getYawBetween(RenderUtil.lerpDouble(Indicators.mc.thePlayer.posX, Indicators.mc.thePlayer.prevPosX, render2DEvent.getPartialTicks()), RenderUtil.lerpDouble(Indicators.mc.thePlayer.posZ, Indicators.mc.thePlayer.prevPosZ, render2DEvent.getPartialTicks()), RenderUtil.lerpDouble(entity.posX, entity.prevPosX, render2DEvent.getPartialTicks()), RenderUtil.lerpDouble(entity.posZ, entity.prevPosZ, render2DEvent.getPartialTicks()));
            if (Indicators.mc.gameSettings.thirdPersonView == 2) {
                yawBetween += 180.0f;
            }
            float x = (float) Math.sin(Math.toRadians(yawBetween));
            float z = (float) Math.cos(Math.toRadians(yawBetween)) * -1.0f;
            GlStateManager.pushMatrix();
            GlStateManager.disableDepth();
            GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0f);
            GlStateManager.translate((float) new ScaledResolution(mc).getScaledWidth() / 2.0f / this.scale.getValue(), (float) new ScaledResolution(mc).getScaledHeight() / 2.0f / this.scale.getValue(), 0.0f);
            GlStateManager.pushMatrix();
            GlStateManager.translate((offset + 0.0f) * x - 8.0f, (offset + 0.0f) * z - 8.0f, -300.0f);
            mc.getRenderItem().renderItemAndEffectIntoGUI(new ItemStack(this.getIndicatorItem(entity)), 0, 0);
            GlStateManager.popMatrix();
            String string = String.format("%dm", (int) Indicators.mc.thePlayer.getDistanceToEntity(entity));
            GlStateManager.pushMatrix();
            GlStateManager.translate((offset + 0.0f) * x - (float) Indicators.mc.fontRendererObj.getStringWidth(string) / 2.0f + 1.0f, (offset + 0.0f) * z + 1.0f, -100.0f);
            Indicators.mc.fontRendererObj.drawStringWithShadow(string, 0.0f, 0.0f, ChatColors.GRAY.toAwtColor() & 0xFFFFFF | 0xBF000000);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.translate((offset + 15.0f) * x + 1.0f, (offset + 15.0f) * z + 1.0f, -100.0f);
            RenderUtil.enableRenderState();
            RenderUtil.drawArrow(0.0f, 0.0f, (float) (Math.atan2(z, x) + Math.PI), 7.5f, 1.5f, this.getIndicatorColor(entity).getRGB());
            RenderUtil.disableRenderState();
            GlStateManager.popMatrix();
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    }
}
