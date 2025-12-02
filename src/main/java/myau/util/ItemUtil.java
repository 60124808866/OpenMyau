package myau.util;

import com.google.common.collect.Multimap;
import myau.mixin.IAccessorItemSword;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Iterator;

public class ItemUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ArrayList<Integer> specialItems = new SpecialItems();

    public static boolean isNotSpecialItem(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        Item item = itemStack.getItem();
        if (item instanceof ItemPotion) {
            return ((ItemPotion) item).getEffects(itemStack).stream().map(PotionEffect::getPotionID).noneMatch(specialItems::contains);
        }
        if (item instanceof ItemEnderPearl) return false;
        if (item instanceof ItemFood) {
            if (item != Items.spider_eye) return false;
        }
        if (item instanceof ItemMonsterPlacer) return false;
        return item != Items.nether_star;
    }

    public static boolean isBlock(ItemStack itemStack) {
        if (itemStack == null || itemStack.stackSize < 1) {
            return false;
        }
        Item item = itemStack.getItem();
        if (item instanceof ItemBlock) {
            return ItemUtil.isContainerBlock((ItemBlock) item);
        }
        return false;
    }

    public static boolean isProjectile(ItemStack itemStack) {
        if (itemStack == null || itemStack.stackSize < 1) {
            return false;
        }
        Item item = itemStack.getItem();
        if (item instanceof ItemEgg) return true;
        if (item instanceof ItemSnowball) return true;
        return false;
    }

    public static boolean isContainerBlock(ItemBlock itemBlock) {
        Block block = itemBlock.getBlock();
        if (BlockUtil.isInteractable(block)) return false;
        return BlockUtil.isSolid(block);
    }

    public static double getAttackBonus(ItemStack itemStack) {
        double attackBonus = 0.0;
        if (itemStack == null) {
            return 0.0;
        }
        Multimap<String, AttributeModifier> multimap = itemStack.getAttributeModifiers();
        for (String attributeName : multimap.keySet()) {
            if (!attributeName.equals("generic.attackDamage")) continue;
            Iterator<AttributeModifier> iterator = multimap.get(attributeName).iterator();
            if (!iterator.hasNext()) break;
            attackBonus += (iterator.next()).getAmount();
            break;
        }
        if (itemStack.isItemEnchanted()) {
            attackBonus = attackBonus + (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, itemStack) + (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, itemStack) * 1.25;
        }
        return attackBonus;
    }

    public static float getToolEfficiency(ItemStack itemStack) {
        float efficiency = 1.0f;
        if (itemStack != null) {
            if (itemStack.getItem() instanceof ItemTool) {
                int enchantLevel;
                efficiency = ((ItemTool) itemStack.getItem()).getToolMaterial().getEfficiencyOnProperMaterial();
                if (efficiency > 1.0f && (enchantLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack)) > 0) {
                    efficiency += (float) (enchantLevel * enchantLevel + 1);
                }
            }
        }
        return efficiency;
    }

    public static float getToolEfficiency(ItemStack itemStack, Block block) {
        float efficiency = 1.0f;
        if (itemStack != null) {
            efficiency = itemStack.canHarvestBlock(block) || !(itemStack.getItem() instanceof ItemPickaxe)
                    ? itemStack.getStrVsBlock(block) : 1.0f;
            if (itemStack.getItem() instanceof ItemTool) {
                int enchantLevel;
                if (efficiency > 1.0f && (enchantLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack)) > 0) {
                    efficiency += (float) (enchantLevel * enchantLevel + 1);
                }
            }
        }
        return efficiency;
    }

    public static double getArmorProtection(ItemStack itemStack) {
        double protection = 0.0;
        if (itemStack != null) {
            if (itemStack.getItem() instanceof ItemArmor) {
                protection = 0.0 + (double) ((ItemArmor) itemStack.getItem()).damageReduceAmount;
                if (itemStack.isItemEnchanted()) {
                    protection += (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, itemStack) * 0.8;
                    protection += (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.featherFalling.effectId, itemStack) * 0.05;
                    protection += (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.projectileProtection.effectId, itemStack) * 0.01;
                }
            }
        }
        return protection;
    }

    public static double getBowAttackBonus(ItemStack itemStack) {
        double attackBonus = 0.0;
        if (itemStack != null) {
            if (itemStack.getItem() instanceof ItemBow) {
                attackBonus = 2;
                if (itemStack.isItemEnchanted()) {
                    int power = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, itemStack);
                    if (power > 0) {
                        attackBonus += (double) (power + 1) * 0.25;
                    }
                    attackBonus += (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, itemStack) * 0.25;
                    attackBonus += (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.infinity.effectId, itemStack) * 0.05;
                }
            }
        }
        return attackBonus;
    }

    public static int findSwordInInventorySlot(int startSlot, boolean checkDurability) {
        int bestSlot = -1;
        double bestAttackBonus = 0.0;
        if (startSlot < 0) return bestSlot;
        for (int i = 0; i < 36; ++i) {
            int currentSlot = (startSlot + i) % 36;
            ItemStack itemStack = ItemUtil.mc.thePlayer.inventory.getStackInSlot(currentSlot);
            if (itemStack == null) continue;
            if (!(itemStack.getItem() instanceof ItemSword)) continue;
            if (checkDurability) {
                if (itemStack.isItemDamaged()) {
                    if (itemStack.getMaxDamage() - itemStack.getItemDamage() < 30) {
                        continue;
                    }
                }
            }
            double attackBonus = ItemUtil.getAttackBonus(itemStack);
            if (!(attackBonus > bestAttackBonus)) continue;
            bestSlot = currentSlot;
            bestAttackBonus = attackBonus;
        }
        return bestSlot;
    }

    public static int findBowInventorySlot(int startSlot, boolean checkDurability) {
        int bestSlot = -1;
        double bestAttackBonus = 0.0;
        if (startSlot < 0) return bestSlot;
        for (int i = 0; i < 36; ++i) {
            int currentSlot = (startSlot + i) % 36;
            ItemStack itemStack = ItemUtil.mc.thePlayer.inventory.getStackInSlot(currentSlot);
            if (itemStack == null) continue;
            if (!(itemStack.getItem() instanceof ItemBow)) continue;
            if (checkDurability) {
                if (itemStack.isItemDamaged()) {
                    if (itemStack.getMaxDamage() - itemStack.getItemDamage() < 30) {
                        continue;
                    }
                }
            }
            double attackBonus = ItemUtil.getBowAttackBonus(itemStack);
            if (!(attackBonus > bestAttackBonus)) continue;
            bestSlot = currentSlot;
            bestAttackBonus = attackBonus;
        }
        return bestSlot;
    }

    public static int findInventorySlot(String toolClass, int startSlot, boolean checkDurability) {
        int bestSlot = -1;
        float bestEfficiency = 1.0f;
        if (startSlot < 0) return bestSlot;
        for (int i = 0; i < 36; ++i) {
            int currentSlot = (startSlot + i) % 36;
            ItemStack itemStack = ItemUtil.mc.thePlayer.inventory.getStackInSlot(currentSlot);
            if (itemStack == null) continue;
            if (!(itemStack.getItem() instanceof ItemTool)) continue;
            if (!itemStack.getItem().getToolClasses(itemStack).contains(toolClass)) continue;
            if (checkDurability) {
                if (itemStack.isItemDamaged()) {
                    if (itemStack.getMaxDamage() - itemStack.getItemDamage() < 30) {
                        continue;
                    }
                }
            }
            float efficiency = ItemUtil.getToolEfficiency(itemStack);
            if (!(efficiency > bestEfficiency)) continue;
            bestSlot = currentSlot;
            bestEfficiency = efficiency;
        }
        return bestSlot;
    }

    public static int findInventorySlot(int currentSlot, Block block) {
        ItemStack currentItem = ItemUtil.mc.thePlayer.inventory.getStackInSlot(currentSlot);
        int bestSlot = currentSlot;
        float bestStrength = getToolEfficiency(currentItem, block);
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = ItemUtil.mc.thePlayer.inventory.getStackInSlot(i);
            if (itemStack == null) continue;
            float strength = getToolEfficiency(itemStack, block);
            if (!(strength > bestStrength)) continue;
            bestSlot = i;
            bestStrength = strength;
        }
        return bestSlot;
    }

    public static int findAndurilHotbarSlot(int currentSlot) {
        for (int i = currentSlot; i < currentSlot + 9; ++i) {
            ItemStack itemStack = ItemUtil.mc.thePlayer.inventory.getStackInSlot(i % 9);
            if (itemStack == null) continue;

            if (itemStack.getItem() instanceof ItemSword && itemStack.hasTagCompound()) {
                IAccessorItemSword itemSword = (IAccessorItemSword) itemStack.getItem();
                if (itemSword.getMaterial() == Item.ToolMaterial.IRON && itemStack.getTagCompound().hasKey("display", 10)) {
                    NBTTagList nbttaglist = itemStack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8);
                    for (int j = 0; j < nbttaglist.tagCount(); ++j) {
                        if (nbttaglist.getStringTagAt(j).contains("§9Justice")) {
                            return i % 9;
                        }
                    }
                }
            }
        }
        return -1;
    }

    public static int findArmorInventorySlot(int armorType, boolean checkDurability) {
        int bestSlot = -1;
        double bestProtection = 0.0;
        for (int i = 0; i < 40; ++i) {
            ItemStack itemStack = ItemUtil.mc.thePlayer.inventory.getStackInSlot(i);
            if (itemStack == null) continue;
            if (!(itemStack.getItem() instanceof ItemArmor)) continue;
            if (((ItemArmor) itemStack.getItem()).armorType != armorType) {
                continue;
            }
            if (checkDurability) {
                if (itemStack.isItemDamaged()) {
                    if (itemStack.getMaxDamage() - itemStack.getItemDamage() < 30) {
                        continue;
                    }
                }
            }
            double protection = ItemUtil.getArmorProtection(itemStack);
            if (!(protection >= bestProtection)) continue;
            bestSlot = i;
            bestProtection = protection;
        }
        return bestSlot;
    }

    public static int findInventorySlot(int startSlot, ItemType itemType) {
        int bestSlot = -1;
        int maxStackSize = 0;
        if (startSlot < 0) startSlot = 0;
        for (int i = 0; i < 36; ++i) {
            int currentSlot = (startSlot + i) % 36;
            ItemStack itemStack = ItemUtil.mc.thePlayer.inventory.getStackInSlot(currentSlot);
            if (itemStack == null) continue;
            if (!itemType.contains(itemStack)) continue;
            if (maxStackSize >= itemStack.stackSize) continue;
            bestSlot = currentSlot;
            maxStackSize = itemStack.stackSize;
        }
        return bestSlot;
    }

    public static int findInventorySlot(ItemType itemType) {
        int stackSize = 0;
        for (int i = 0; i < 36; ++i) {
            ItemStack itemStack = ItemUtil.mc.thePlayer.inventory.getStackInSlot(i);
            if (itemStack == null) continue;
            if (!itemType.contains(itemStack)) continue;
            stackSize += itemStack.stackSize;
        }
        return stackSize;
    }

    public static boolean hasRawUnbreakingEnchant() {
        ItemStack itemStack = ItemUtil.mc.thePlayer.getHeldItem();
        if (itemStack == null) {
            return false;
        }
        if (itemStack.hasTagCompound()) {
            NBTTagCompound tag = itemStack.getTagCompound();
            if (tag.hasKey("ExtraAttributes")) {
                NBTTagCompound extra = tag.getCompoundTag("ExtraAttributes");
                if (extra.hasKey("UHCid")) {
                    long id = extra.getLong("UHCid");
                    if (id == 50006L || id == 50009L) {
                        return true;
                    }
                }
            }
            if (tag.hasKey("HideFlags")
                    && itemStack.getItem() instanceof ItemSpade
                    && ((ItemSpade) itemStack.getItem()).getToolMaterial() == Item.ToolMaterial.EMERALD) {
                return true;
            }
        }
        if (itemStack.getItem() instanceof ItemEnchantedBook) {
            return false;
        }
        if (EnchantmentHelper.getEnchantments(itemStack).containsKey(19)) {
            return true;
        }
        return itemStack.getItem() instanceof ItemSword;
    }

    public static boolean isHoldingSword() {
        ItemStack itemStack = ItemUtil.mc.thePlayer.getHeldItem();
        if (itemStack == null) {
            return false;
        }
        return itemStack.getItem() instanceof ItemSword;
    }

    public static boolean isHoldingTool() {
        ItemStack itemStack = ItemUtil.mc.thePlayer.getHeldItem();
        if (itemStack == null) {
            return false;
        }
        return itemStack.getItem() instanceof ItemTool;
    }

    public static boolean isEating() {
        ItemStack itemStack = ItemUtil.mc.thePlayer.getHeldItem();
        if (itemStack == null) {
            return false;
        }
        if (ItemPotion.isSplash(itemStack.getItem().getMetadata(itemStack))) {
            return false;
        }
        return itemStack.getItemUseAction() == EnumAction.EAT || itemStack.getItemUseAction() == EnumAction.DRINK;
    }

    public static boolean isUsingBow() {
        ItemStack itemStack = ItemUtil.mc.thePlayer.getHeldItem();
        if (itemStack == null) {
            return false;
        }
        return itemStack.getItem() instanceof ItemBow;
    }

    public static boolean isHoldingNonEmpty() {
        ItemStack itemStack = ItemUtil.mc.thePlayer.getHeldItem();
        if (itemStack == null || itemStack.stackSize < 1) {
            return false;
        }
        return itemStack.getItem() instanceof ItemBlock;
    }

    public static boolean isHoldingBlock() {
        return ItemUtil.isBlock(ItemUtil.mc.thePlayer.getHeldItem());
    }

    public static boolean hasHoldItem() {
        ItemStack itemStack = ItemUtil.mc.thePlayer.getHeldItem();
        if (itemStack == null || itemStack.stackSize < 1) {
            return false;
        }
        return itemStack.getItem() instanceof ItemFireball;
    }

    static final class SpecialItems extends ArrayList<Integer> {
        SpecialItems() {
            this.add(1);
            this.add(3);
            this.add(5);
            this.add(6);
            this.add(8);
            this.add(10);
            this.add(11);
            this.add(12);
            this.add(14);
            this.add(21);
            this.add(22);
        }
    }

    public enum ItemType {
        Block {
            public boolean contains(ItemStack itemStack) {
                return isBlock(itemStack);
            }
        },
        Projectile {
            public boolean contains(ItemStack itemStack) {
                return isProjectile(itemStack);
            }
        },
        FishRod {
            public boolean contains(ItemStack itemStack) {
                return itemStack.getItem() instanceof ItemFishingRod;
            }
        },
        GoldApple {
            public boolean contains(ItemStack itemStack) {
                return itemStack.getItem() instanceof ItemAppleGold;
            }
        },
        Arrow {
            public boolean contains(ItemStack itemStack) {
                return itemStack.getItem() == Items.arrow;
            }
        };
        abstract public boolean contains(ItemStack itemStack);
    }
}
