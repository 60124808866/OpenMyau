package myau.module.modules;

import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.events.Render3DEvent;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.module.category.Category;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import myau.property.properties.BooleanProperty;
import myau.property.properties.PercentProperty;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

public class ItemESP extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final PercentProperty opacity = new PercentProperty("opacity", 25);
    public final BooleanProperty outline = new BooleanProperty("outline", false);
    public final BooleanProperty itemCount = new BooleanProperty("item-count", true);
    public final BooleanProperty autoScale = new BooleanProperty("auto-scale", true);
    public final BooleanProperty emeralds = new BooleanProperty("emeralds", true);
    public final BooleanProperty diamonds = new BooleanProperty("diamonds", true);
    public final BooleanProperty goldd = new BooleanProperty("gold", true);
    public final BooleanProperty iron = new BooleanProperty("iron", true);

    private boolean shouldHighlightItem(int itemId) {
        return this.emeralds.getValue() && this.isEmeraldItem(itemId)
                || this.diamonds.getValue() && this.isDiamondItem(itemId)
                || this.goldd.getValue() && this.isGoldItem(itemId)
                || this.iron.getValue() && this.isIronItem(itemId);
    }

    private boolean isEmeraldItem(int itemId) {
        Item item = Item.getItemById(itemId);
        Block block = Block.getBlockFromItem(item);
        return item == Items.emerald || block == Blocks.emerald_block || block == Blocks.emerald_ore;
    }

    private boolean isDiamondItem(int itemId) {
        Item item = Item.getItemById(itemId);
        Block block = Block.getBlockFromItem(item);
        return item == Items.diamond
                || item == Items.diamond_sword
                || item == Items.diamond_pickaxe
                || item == Items.diamond_shovel
                || item == Items.diamond_axe
                || item == Items.diamond_hoe
                || item == Items.diamond_helmet
                || item == Items.diamond_chestplate
                || item == Items.diamond_leggings
                || item == Items.diamond_boots
                || block == Blocks.diamond_block
                || block == Blocks.diamond_ore;
    }

    private boolean isGoldItem(int itemId) {
        Item item = Item.getItemById(itemId);
        Block block = Block.getBlockFromItem(item);
        return item == Items.gold_ingot || item == Items.gold_nugget || item == Items.golden_apple || block == Blocks.gold_block || block == Blocks.gold_ore;
    }

    private boolean isIronItem(int itemId) {
        Item item = Item.getItemById(itemId);
        Block block = Block.getBlockFromItem(item);
        return item == Items.iron_ingot || block == Blocks.iron_block || block == Blocks.iron_ore;
    }

    private Color getItemColor(int itemId) {
        if (this.isEmeraldItem(itemId)) {
            return new Color(ChatColors.GREEN.toAwtColor());
        } else if (this.isDiamondItem(itemId)) {
            return new Color(ChatColors.AQUA.toAwtColor());
        } else if (this.isGoldItem(itemId)) {
            return new Color(ChatColors.YELLOW.toAwtColor());
        } else {
            return this.isIronItem(itemId) ? new Color(ChatColors.WHITE.toAwtColor()) : new Color(ChatColors.GRAY.toAwtColor());
        }
    }

    private int getItemPriority(int itemId) {
        if (this.isEmeraldItem(itemId)) {
            return 4;
        } else if (this.isDiamondItem(itemId)) {
            return 3;
        } else if (this.isGoldItem(itemId)) {
            return 2;
        } else {
            return this.isIronItem(itemId) ? 1 : 0;
        }
    }

    public ItemESP() {
        super("ItemESP", false);
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled()) {
            LinkedHashMap<ItemData, Integer> itemMap = new LinkedHashMap<>();
            for (Entity entity : TeamUtil.getLoadedEntitiesSorted()) {
                if (entity.ticksExisted >= 3
                        && (entity.ignoreFrustumCheck || RenderUtil.isInViewFrustum(entity.getEntityBoundingBox(), 0.125))
                        && entity instanceof EntityItem) {
                    EntityItem entityItem = (EntityItem) entity;
                    ItemStack stack = entityItem.getEntityItem();
                    if (stack.stackSize > 0) {
                        int itemId = Item.getIdFromItem(stack.getItem());
                        if (this.shouldHighlightItem(itemId)) {
                            double x = RenderUtil.lerpDouble(entityItem.posX, entityItem.lastTickPosX, event.getPartialTicks());
                            double y = RenderUtil.lerpDouble(entityItem.posY, entityItem.lastTickPosY, event.getPartialTicks());
                            double z = RenderUtil.lerpDouble(entityItem.posZ, entityItem.lastTickPosZ, event.getPartialTicks());
                            ItemData data = new ItemData(itemId, x, y, z);
                            Integer id = itemMap.get(data);
                            itemMap.put(new ItemData(itemId, x, y, z), stack.stackSize + (id == null ? 0 : id));
                        }
                    }
                }
            }
            for (Entry<ItemData, Integer> itemEntry : itemMap.entrySet().stream().sorted((entry1, entry2) -> {
                int o = this.getItemPriority(entry1.getKey().itemId);
                int o2 = this.getItemPriority(entry2.getKey().itemId);
                return Integer.compare(o, o2);
            }).collect(Collectors.toList())) {
                Color itemColor = this.getItemColor(itemEntry.getKey().itemId);
                double x = itemEntry.getKey().x - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
                double y = itemEntry.getKey().y - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
                double z = itemEntry.getKey().z - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
                double distance = mc.getRenderViewEntity().getDistance(itemEntry.getKey().x, itemEntry.getKey().y, itemEntry.getKey().z);
                double scale = 0.5 + 0.375 * ((Math.max(6.0, this.autoScale.getValue() ? distance : 6.0) - 6.0) / 28.0);
                AxisAlignedBB axisAlignedBB = new AxisAlignedBB(x - scale * 0.5, y, z - scale * 0.5, x + scale * 0.5, y + scale, z + scale * 0.5);
                RenderUtil.enableRenderState();
                if (this.opacity.getValue() > 0) {
                    RenderUtil.drawFilledBox(
                            axisAlignedBB, itemColor.getRed(), itemColor.getGreen(), itemColor.getBlue()
                    );
                    GlStateManager.resetColor();
                }
                if (this.outline.getValue()) {
                    RenderUtil.drawBoundingBox(axisAlignedBB, itemColor.getRed(), itemColor.getGreen(), itemColor.getBlue(), 255, 1.5F);
                    GlStateManager.resetColor();
                }
                RenderUtil.disableRenderState();
                if (this.itemCount.getValue()) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(x, y + scale * 0.5, z);
                    GlStateManager.rotate(mc.getRenderManager().playerViewY * -1.0F, 0.0F, 1.0F, 0.0F);
                    float flip = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
                    GlStateManager.rotate(mc.getRenderManager().playerViewX, flip, 0.0F, 0.0F);
                    double fontScale = -0.04375 - 0.0328125 * ((Math.max(6.0, this.autoScale.getValue() ? distance : 6.0) - 6.0) / 28.0);
                    GlStateManager.scale(fontScale, fontScale, 1.0);
                    GlStateManager.disableDepth();
                    String countText = String.format("%d", itemEntry.getValue());
                    RenderUtil.drawOutlinedString(
                            countText,
                            ((float) mc.fontRendererObj.getStringWidth(countText) / 2.0F - 0.5F) * -1.0F,
                            ((float) (mc.fontRendererObj.FONT_HEIGHT / 2) - 0.5F) * -1.0F
                    );
                    GlStateManager.enableDepth();
                    GlStateManager.resetColor();
                    GlStateManager.popMatrix();
                }
            }
        }
    }

    public static class ItemData {
        private final int hashCode;
        public final int itemId;
        public final double x;
        public final double y;
        public final double z;

        public ItemData(int id, double x, double y, double z) {
            this.itemId = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.hashCode = Objects.hash(id, (int) x, (int) y, (int) z);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object != null && this.getClass() == object.getClass()) {
                ItemData itemData = (ItemData) object;
                return this.itemId == itemData.itemId && (int) this.x == (int) itemData.x && (int) this.y == (int) itemData.y && (int) this.z == (int) itemData.z;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

    @Override
    public Category category() {
        return Category.RENDER;
    }
}
