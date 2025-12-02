package myau.util;

import myau.enums.ChatColors;
import myau.mixin.IAccessorEntityRenderer;
import myau.mixin.IAccessorMinecraft;
import myau.mixin.IAccessorRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class RenderUtil {
    private static Minecraft mc;
    private static Frustum cameraFrustum;
    private static IntBuffer viewportBuffer;
    private static FloatBuffer modelViewBuffer;
    private static FloatBuffer projectionBuffer;
    private static FloatBuffer vectorBuffer;
    private static Map<Integer, EnchantmentData> enchantmentMap;

    static {
        RenderUtil.mc = Minecraft.getMinecraft();
        RenderUtil.cameraFrustum = new Frustum();
        RenderUtil.viewportBuffer = GLAllocation.createDirectIntBuffer(16);
        RenderUtil.modelViewBuffer = GLAllocation.createDirectFloatBuffer(16);
        RenderUtil.projectionBuffer = GLAllocation.createDirectFloatBuffer(16);
        RenderUtil.vectorBuffer = GLAllocation.createDirectFloatBuffer(4);
        RenderUtil.enchantmentMap = new EnchantmentMap();
    }

    private static ChatColors getColorForLevel(int currentLevel, int maxLevel) {
        if (currentLevel > maxLevel) {
            return ChatColors.LIGHT_PURPLE;
        }
        if (currentLevel == maxLevel) {
            return ChatColors.RED;
        }
        switch (currentLevel) {
            case 1: {
                return ChatColors.AQUA;
            }
            case 2: {
                return ChatColors.GREEN;
            }
            case 3: {
                return ChatColors.YELLOW;
            }
            case 4: {
                return ChatColors.GOLD;
            }
        }
        return ChatColors.GRAY;
    }

    public static void drawOutlinedString(String text, float x, float y) {
        String string2 = text.replaceAll("(?i)§[\\da-f]", "");
        RenderUtil.mc.fontRendererObj.drawString(string2, x + 1.0f, y, 0, false);
        RenderUtil.mc.fontRendererObj.drawString(string2, x - 1.0f, y, 0, false);
        RenderUtil.mc.fontRendererObj.drawString(string2, x, y + 1.0f, 0, false);
        RenderUtil.mc.fontRendererObj.drawString(string2, x, y - 1.0f, 0, false);
        RenderUtil.mc.fontRendererObj.drawString(text, x, y, -1, false);
    }

    public static void renderEnchantmentText(ItemStack itemStack, float x, float y, float scale) {
        NBTTagList nBTTagList;
        nBTTagList = itemStack.getItem() == Items.enchanted_book ? Items.enchanted_book.getEnchantments(itemStack) : itemStack.getEnchantmentTagList();
        if (nBTTagList != null) {
            for (int i = 0; i < nBTTagList.tagCount(); ++i) {
                EnchantmentData enchantmentData = enchantmentMap.get(nBTTagList.getCompoundTagAt(i).getInteger("id"));
                if (enchantmentData == null) {
                    continue;
                }
                short s = nBTTagList.getCompoundTagAt(i).getShort("lvl");
                ChatColors chatColors = RenderUtil.getColorForLevel(s, enchantmentData.maxLevel);
                RenderUtil.drawOutlinedString(ChatColors.formatColor(String.format("&r%s%s%d&r", enchantmentData.shortName, chatColors, (int) s)), x * (1.0f / scale), (y + (float) i * 4.0f) * (1.0f / scale));
            }
        }
    }

    public static void renderItemInGUI(ItemStack itemStack, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.clear(256);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0f, 1.0f, -0.01f);
        RenderUtil.mc.getRenderItem().zLevel = -150.0f;
        mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, x, y);
        mc.getRenderItem().renderItemOverlays(RenderUtil.mc.fontRendererObj, itemStack, x, y);
        RenderUtil.mc.getRenderItem().zLevel = 0.0f;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5f, 0.5f, 0.5f);
        GlStateManager.disableDepth();
        RenderUtil.renderEnchantmentText(itemStack, x, y, 0.5f);
        GlStateManager.enableDepth();
        GlStateManager.scale(2.0f, 2.0f, 2.0f);
        GlStateManager.popMatrix();
    }

    public static void renderPotionEffect(PotionEffect potionEffect, int x, int y) {
        int n3 = Potion.potionTypes[potionEffect.getPotionID()].getStatusIconIndex();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.clear(256);
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0f, 1.0f, -0.01f);
        mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/inventory.png"));
        Gui.drawModalRectWithCustomSizedTexture(x, y, n3 % 8 * 18, 198 + n3 / 8 * 18, 18, 18, 256.0f, 256.0f);
        GlStateManager.popMatrix();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    public static void drawRect(float x1, float y1, float x2, float y2, int color) {
        if (color == 0) {
            return;
        }
        RenderUtil.setColor(color);
        GL11.glBegin(GL11.GL_POLYGON);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y1);
        GL11.glEnd();
        GlStateManager.resetColor();
    }

    public static void drawRect3D(float x1, float y1, float x2, float y2, int color) {
        if (color == 0) {
            return;
        }
        RenderUtil.setColor(color);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i < 2; ++i) {
            GL11.glVertex2f(x1, y1);
            GL11.glVertex2f(x1, y2);
            GL11.glVertex2f(x2, y2);
            GL11.glVertex2f(x2, y1);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GlStateManager.resetColor();
    }

    public static void drawOutlineRect(float x1, float y1, float x2, float y2, float lineWidth, int backgroundColor, int lineColor) {
        RenderUtil.drawRect(0.0f, 0.0f, x2, 27.0f, backgroundColor);
        if (lineColor == 0) {
            return;
        }
        RenderUtil.setColor(lineColor);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        RenderUtil.setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawLine3D(Vec3 start, double endX, double endY, double endZ, float red, float green, float blue, float alpha, float lineWidth) {
        GlStateManager.pushMatrix();
        GlStateManager.color(red, green, blue, alpha);
        boolean bl = RenderUtil.mc.gameSettings.viewBobbing;
        RenderUtil.mc.gameSettings.viewBobbing = false;
        ((IAccessorEntityRenderer) RenderUtil.mc.entityRenderer).callSetupCameraTransform(((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks, 2);
        RenderUtil.mc.gameSettings.viewBobbing = bl;
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(start.xCoord, start.yCoord, start.zCoord);
        GL11.glVertex3d(endX - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(), endY - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(), endZ - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ());
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }

    public static void drawArrow(float centerX, float centerY, float angle, float length, float lineWidth, int color) {
        float f6 = angle + (float) Math.toRadians(45.0);
        float f7 = angle - (float) Math.toRadians(45.0);
        RenderUtil.setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(centerX, centerY);
        GL11.glVertex2f(centerX + length * (float) Math.cos(f6), centerY + length * (float) Math.sin(f6));
        GL11.glVertex2f(centerX, centerY);
        GL11.glVertex2f(centerX + length * (float) Math.cos(f7), centerY + length * (float) Math.sin(f7));
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawTriangle(float centerX, float centerY, float angle, float length, int color) {
        float f5 = angle + (float) Math.toRadians(26.25);
        float f6 = angle - (float) Math.toRadians(26.25);
        RenderUtil.setColor(color);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(9);
        GL11.glVertex2f(centerX, centerY);
        GL11.glVertex2f(centerX + length * (float) Math.cos(f5), centerY + length * (float) Math.sin(f5));
        GL11.glVertex2f(centerX + length * (float) Math.cos(f6), centerY + length * (float) Math.sin(f6));
        GL11.glEnd();
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GlStateManager.resetColor();
    }

    public static void drawFramebuffer(Framebuffer framebuffer) {
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        GlStateManager.bindTexture(framebuffer.framebufferTexture);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2d(0.0, 1.0);
        GL11.glVertex2d(0.0, 0.0);
        GL11.glTexCoord2d(0.0, 0.0);
        GL11.glVertex2d(0.0, scaledResolution.getScaledHeight());
        GL11.glTexCoord2d(1.0, 0.0);
        GL11.glVertex2d(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
        GL11.glTexCoord2d(1.0, 1.0);
        GL11.glVertex2d(scaledResolution.getScaledWidth(), 0.0);
        GL11.glEnd();
    }

    public static void fillCircle(double x, double y, double radius, int segments, int color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        RenderUtil.setColor(color);

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);

        GL11.glVertex2d(x, y);

        for (int i = 0; i <= segments; i++) {
            double angle = i * (Math.PI * 2.0 / segments);
            double px = x + Math.cos(angle) * radius;
            double py = y + Math.sin(angle) * radius;
            GL11.glVertex2d(px, py);
        }

        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.resetColor();
    }

    public static void drawCircle(double centerX, double centerY, double centerZ, double radius, int segments, int color) {
        RenderUtil.setColor(color);
        GL11.glLineWidth(3.0f);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= segments; ++i) {
            double d5 = (double) i * (Math.PI * 2 / (double) segments);
            GL11.glVertex3d(centerX + Math.cos(d5) * radius, centerY, centerZ + Math.sin(d5) * radius);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawEntityCircle(Entity entity, double radius, int segments, int color) {
        double d2 = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
        double d3 = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
        double d4 = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
        RenderUtil.drawCircle(d2, d3, d4, radius, segments, color);
    }

    public static void drawFilledBox(AxisAlignedBB axisAlignedBB, int red, int green, int blue) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        tessellator.draw();
    }

    public static void drawBoundingBox(AxisAlignedBB axisAlignedBB, int red, int green, int blue, int alpha, float lineWidth) {
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        RenderGlobal.drawOutlinedBoundingBox(axisAlignedBB, red, green, blue, alpha);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
    }

    public static void drawEntityBox(Entity entity, int red, int green, int blue) {
        double d2 = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        double d3 = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        double d4 = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        RenderUtil.drawFilledBox(entity.getEntityBoundingBox().expand(0.1f, 0.1f, 0.1f).offset(d2 - entity.posX, d3 - entity.posY, d4 - entity.posZ).offset(-((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()), red, green, blue);
    }

    public static void drawEntityBoundingBox(Entity entity, int red, int green, int blue, int alpha, float lineWidth, double expand) {
        double d2 = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        double d3 = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        double d4 = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        RenderUtil.drawBoundingBox(entity.getEntityBoundingBox().expand(expand, expand, expand).offset(d2 - entity.posX, d3 - entity.posY, d4 - entity.posZ).offset(-((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()), red, green, blue, alpha, lineWidth);
    }

    public static void drawBlockBox(BlockPos blockPos, double height, int red, int green, int blue) {
        RenderUtil.drawFilledBox(new AxisAlignedBB(blockPos.getX(), blockPos.getY(), blockPos.getZ(), (double) blockPos.getX() + 1.0, (double) blockPos.getY() + height, (double) blockPos.getZ() + 1.0).offset(-((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()), red, green, blue);
    }

    public static void drawBlockBoundingBox(BlockPos blockPos, double height, int red, int green, int blue, int alpha, float lineWidth) {
        RenderUtil.drawBoundingBox(new AxisAlignedBB(blockPos.getX(), blockPos.getY(), blockPos.getZ(), (double) blockPos.getX() + 1.0, (double) blockPos.getY() + height, (double) blockPos.getZ() + 1.0).offset(-((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()), red, green, blue, alpha, lineWidth);
    }

    public static void drawCornerESP(EntityPlayer entity, float red, float green, float blue) {
        float x = (float) (RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, ((IAccessorMinecraft) mc).getTimer().renderPartialTicks) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX());
        float y = (float) (RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, ((IAccessorMinecraft) mc).getTimer().renderPartialTicks) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY());
        float z = (float) (RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, ((IAccessorMinecraft) mc).getTimer().renderPartialTicks) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ());
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + entity.height / 2.0F, z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(-0.098F, -0.098F, 0.098F);
        float width = (float) (26.6 * entity.width / 2.0);
        float height = 12.0F;
        GlStateManager.color(red, green, blue);
        draw3DRect(width, height - 1.0F, width - 4.0F, height);
        draw3DRect(-width, height - 1.0F, -width + 4.0F, height);
        draw3DRect(-width, height, -width + 1.0F, height - 4.0F);
        draw3DRect(width, height, width - 1.0F, height - 4.0F);
        draw3DRect(width, -height, width - 4.0F, -height + 1.0F);
        draw3DRect(-width, -height, -width + 4.0F, -height + 1.0F);
        draw3DRect(-width, -height + 1.0F, -width + 1.0F, -height + 4.0F);
        draw3DRect(width, -height + 1.0F, width - 1.0F, -height + 4.0F);
        GlStateManager.color(0.0F, 0.0F, 0.0F);
        draw3DRect(width, height, width - 4.0F, height + 0.2F);
        draw3DRect(-width, height, -width + 4.0F, height + 0.2F);
        draw3DRect(-width - 0.2F, height + 0.2F, -width, height - 4.0F);
        draw3DRect(width + 0.2F, height + 0.2F, width, height - 4.0F);
        draw3DRect(width + 0.2F, -height, width - 4.0F, -height - 0.2F);
        draw3DRect(-width - 0.2F, -height, -width + 4.0F, -height - 0.2F);
        draw3DRect(-width - 0.2F, -height, -width, -height + 4.0F);
        draw3DRect(width + 0.2F, -height, width, -height + 4.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    public static void drawFake2DESP(EntityPlayer entity, float red, float green, float blue) {
        float x = (float) (RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, ((IAccessorMinecraft) mc).getTimer().renderPartialTicks) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX());
        float y = (float) (RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, ((IAccessorMinecraft) mc).getTimer().renderPartialTicks) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY());
        float z = (float) (RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, ((IAccessorMinecraft) mc).getTimer().renderPartialTicks) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ());
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + entity.height / 2.0F, z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(-0.1F, -0.1F, 0.1F);
        GlStateManager.color(red, green, blue);
        float width = (float) (23.3 * entity.width / 2.0);
        float height = 12.0F;
        draw3DRect(width, height, -width, height + 0.4F);
        draw3DRect(width, -height, -width, -height + 0.4F);
        draw3DRect(width, -height + 0.4F, width - 0.4F, height + 0.4F);
        draw3DRect(-width, -height + 0.4F, -width + 0.4F, height + 0.4F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    public static void draw3DRect(float x1, float y1, float x2, float y2) {
        GL11.glBegin(GL11.GL_POLYGON);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
    }

    public static Vector4d projectToScreen(Entity entity, double screenScale) {
        Vector4d vector4d;
        {
            double d3 = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
            double d4 = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
            double d5 = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
            AxisAlignedBB axisAlignedBB = entity.getEntityBoundingBox().expand(0.1f, 0.1f, 0.1f).offset(d3 - entity.posX, d4 - entity.posY, d5 - entity.posZ);
            vector4d = null;
            for (Vector3d vector3d : new Vector3d[]{new Vector3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vector3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vector3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vector3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vector3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vector3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ), new Vector3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vector3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)}) {
                GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewBuffer);
                GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionBuffer);
                GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuffer);
                if (!GLU.gluProject((float) (vector3d.x - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX()), (float) (vector3d.y - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY()), (float) (vector3d.z - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()), modelViewBuffer, projectionBuffer, viewportBuffer, vectorBuffer))
                    continue;
                vector3d = new Vector3d((double) vectorBuffer.get(0) / screenScale, (double) ((float) Display.getHeight() - vectorBuffer.get(1)) / screenScale, vectorBuffer.get(2));
                if (!(vector3d.z >= 0.0) || !(vector3d.z < 1.0)) continue;
                if (vector4d == null) {
                    vector4d = new Vector4d(vector3d.x, vector3d.y, vector3d.z, 0.0);
                }
                vector4d.x = Math.min(vector3d.x, vector4d.x);
                vector4d.y = Math.min(vector3d.y, vector4d.y);
                vector4d.z = Math.max(vector3d.x, vector4d.z);
                vector4d.w = Math.max(vector3d.y, vector4d.w);
            }
        }
        return vector4d;
    }

    public static boolean isInViewFrustum(AxisAlignedBB axisAlignedBB, double expand) {
        cameraFrustum.setPosition(RenderUtil.mc.getRenderViewEntity().posX, RenderUtil.mc.getRenderViewEntity().posY, RenderUtil.mc.getRenderViewEntity().posZ);
        return cameraFrustum.isBoundingBoxInFrustum(axisAlignedBB.expand(expand, expand, expand));
    }

    public static void enableRenderState() {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
    }

    public static void disableRenderState() {
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void setColor(int argb) {
        float f = (float) (argb >> 24 & 0xFF) / 255.0f;
        float f2 = (float) (argb >> 16 & 0xFF) / 255.0f;
        float f3 = (float) (argb >> 8 & 0xFF) / 255.0f;
        float f4 = (float) (argb & 0xFF) / 255.0f;
        GlStateManager.color(f2, f3, f4, f);
    }

    public static float lerpFloat(float current, float previous, float t) {
        return previous + (current - previous) * t;
    }

    public static double lerpDouble(double current, double previous, double t) {
        return previous + (current - previous) * t;
    }

    public static final class EnchantmentData {
        public final String shortName;
        public final int maxLevel;

        public EnchantmentData(String shortName, int maxLevel) {
            this.shortName = shortName;
            this.maxLevel = maxLevel;
        }
    }

    static final class EnchantmentMap extends HashMap<Integer, EnchantmentData> {
        EnchantmentMap() {
            this.put(0, new EnchantmentData("Pr", 4));
            this.put(1, new EnchantmentData("Fp", 4));
            this.put(2, new EnchantmentData("Ff", 4));
            this.put(3, new EnchantmentData("Bp", 4));
            this.put(4, new EnchantmentData("Pp", 4));
            this.put(5, new EnchantmentData("Re", 3));
            this.put(6, new EnchantmentData("Aq", 1));
            this.put(7, new EnchantmentData("Th", 3));
            this.put(8, new EnchantmentData("Ds", 3));
            this.put(16, new EnchantmentData("Sh", 5));
            this.put(17, new EnchantmentData("Sm", 5));
            this.put(18, new EnchantmentData("BoA", 5));
            this.put(19, new EnchantmentData("Kb", 2));
            this.put(20, new EnchantmentData("Fa", 2));
            this.put(21, new EnchantmentData("Lo", 3));
            this.put(32, new EnchantmentData("Ef", 5));
            this.put(33, new EnchantmentData("St", 1));
            this.put(34, new EnchantmentData("Ub", 3));
            this.put(35, new EnchantmentData("Fo", 3));
            this.put(48, new EnchantmentData("Po", 5));
            this.put(49, new EnchantmentData("Pu", 2));
            this.put(50, new EnchantmentData("Fl", 1));
            this.put(51, new EnchantmentData("Inf", 1));
            this.put(61, new EnchantmentData("LoS", 3));
            this.put(62, new EnchantmentData("Lu", 3));
        }
    }
}