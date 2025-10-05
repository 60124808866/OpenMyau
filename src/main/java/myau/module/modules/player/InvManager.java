package myau.module.modules.player;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.UpdateEvent;
import myau.events.WindowClickEvent;
import myau.module.Category;
import myau.module.Module;
import myau.util.ItemUtil;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings.GameType;
import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

public class InvManager extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int actionDelay = 0;
    private int oDelay = 0;
    private boolean inventoryOpen = false;
    public final IntProperty minDelay = new IntProperty("min-delay", 1, 0, 20);
    public final IntProperty maxDelay = new IntProperty("max-delay", 2, 0, 20);
    public final IntProperty openDelay = new IntProperty("open-delay", 1, 0, 20);
    public final BooleanProperty autoArmor = new BooleanProperty("auto-armor", true);
    public final BooleanProperty dropTrash = new BooleanProperty("drop-trash", false);
    public final IntProperty swordSlot = new IntProperty("sword-slot", 1, 0, 9);
    public final IntProperty pickaxeSlot = new IntProperty("pickaxe-slot", 3, 0, 9);
    public final IntProperty shovelSlot = new IntProperty("shovel-slot", 4, 0, 9);
    public final IntProperty axeSlot = new IntProperty("axe-slot", 5, 0, 9);
    public final IntProperty blocksSlot = new IntProperty("blocks-slot", 2, 0, 9);
    public final IntProperty blocks = new IntProperty("blocks", 128, 64, 2304);

    private boolean isValidGameMode() {
        GameType gameType = mc.playerController.getCurrentGameType();
        return gameType == GameType.SURVIVAL || gameType == GameType.ADVENTURE;
    }

    private int convertSlotIndex(int slot) {
        if (slot >= 36) {
            return 8 - (slot - 36);
        } else {
            return slot <= 8 ? slot + 36 : slot;
        }
    }

    private void clickSlot(int windowId, int slotId, int mouseButtonClicked, int mode) {
        mc.playerController.windowClick(windowId, slotId, mouseButtonClicked, mode, mc.thePlayer);
    }

    private int getStackSize(int slot) {
        if (slot == -1) {
            return 0;
        } else {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            return stack != null ? stack.stackSize : 0;
        }
    }

    public InvManager() {
        super("InvManager", Category.Player, false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.actionDelay > 0) {
                this.actionDelay--;
            }
            if (this.oDelay > 0) {
                this.oDelay--;
            }
            if (!(mc.currentScreen instanceof GuiInventory)) {
                this.inventoryOpen = false;
            } else if (!(((GuiInventory) mc.currentScreen).inventorySlots instanceof ContainerPlayer)) {
                this.inventoryOpen = false;
            } else {
                if (!this.inventoryOpen) {
                    this.inventoryOpen = true;
                    this.oDelay = this.openDelay.getValue() + 1;
                }
                if (this.oDelay <= 0 && this.actionDelay <= 0) {
                    if (this.isEnabled() && this.isValidGameMode()) {
                        ArrayList<Integer> equippedArmorSlots = new ArrayList<>(Arrays.asList(-1, -1, -1, -1));
                        ArrayList<Integer> inventoryArmorSlots = new ArrayList<>(Arrays.asList(-1, -1, -1, -1));
                        for (int i = 0; i < 4; i++) {
                            equippedArmorSlots.set(i, ItemUtil.findArmorInventorySlot(i, true));
                            inventoryArmorSlots.set(i, ItemUtil.findArmorInventorySlot(i, false));
                        }
                        int preferredSwordHotbarSlot = this.swordSlot.getValue() - 1;
                        int equippedSwordSlot = ItemUtil.findSwordInInventorySlot(preferredSwordHotbarSlot, true);
                        int inventorySwordSlot = ItemUtil.findSwordInInventorySlot(preferredSwordHotbarSlot, false);
                        int preferredPickaxeHotbarSlot = this.pickaxeSlot.getValue() - 1;
                        int equippedPickaxeSlot = ItemUtil.findInventorySlot("pickaxe", preferredPickaxeHotbarSlot, true);
                        int inventoryPickaxeSlot = ItemUtil.findInventorySlot("pickaxe", preferredPickaxeHotbarSlot, false);
                        int preferredShovelHotbarSlot = this.shovelSlot.getValue() - 1;
                        int equippedShovelSlot = ItemUtil.findInventorySlot("shovel", preferredShovelHotbarSlot, true);
                        int inventoryShovelSlot = ItemUtil.findInventorySlot("shovel", preferredShovelHotbarSlot, false);
                        int preferredAxeHotbarSlot = this.axeSlot.getValue() - 1;
                        int equippedAxeSlot = ItemUtil.findInventorySlot("axe", preferredAxeHotbarSlot, true);
                        int inventoryAxeSlot = ItemUtil.findInventorySlot("axe", preferredAxeHotbarSlot, false);
                        int preferredBlocksHotbarSlot = this.blocksSlot.getValue() - 1;
                        int inventoryBlocksSlot = ItemUtil.findInventorySlot(preferredBlocksHotbarSlot);
                        if (this.autoArmor.getValue()) {
                            for (int i = 0; i < 4; i++) {
                                int equippedSlot = equippedArmorSlots.get(i);
                                int inventorySlot = inventoryArmorSlots.get(i);
                                if (equippedSlot != -1 || inventorySlot != -1) {
                                    int playerArmorSlot = 39 - i;
                                    if (equippedSlot != playerArmorSlot && inventorySlot != playerArmorSlot) {
                                        if (mc.thePlayer.inventory.getStackInSlot(playerArmorSlot) != null) {
                                            if (mc.thePlayer.inventory.getFirstEmptyStack() != -1) {
                                                this.clickSlot(mc.thePlayer.inventoryContainer.windowId, this.convertSlotIndex(playerArmorSlot), 0, 1);
                                            } else {
                                                this.clickSlot(mc.thePlayer.inventoryContainer.windowId, this.convertSlotIndex(playerArmorSlot), 1, 4);
                                            }
                                        } else {
                                            int armorToEquipSlot = equippedSlot != -1 ? equippedSlot : inventorySlot;
                                            this.clickSlot(mc.thePlayer.inventoryContainer.windowId, this.convertSlotIndex(armorToEquipSlot), 0, 1);
                                        }
                                        return;
                                    }
                                }
                            }
                        }
                        LinkedHashSet<Integer> usedHotbarSlots = new LinkedHashSet<>();
                        if (preferredSwordHotbarSlot >= 0 && preferredSwordHotbarSlot <= 8 && (equippedSwordSlot != -1 || inventorySwordSlot != -1)) {
                            usedHotbarSlots.add(preferredSwordHotbarSlot);
                            if (equippedSwordSlot != preferredSwordHotbarSlot && inventorySwordSlot != preferredSwordHotbarSlot) {
                                int slot = equippedSwordSlot != -1 ? equippedSwordSlot : inventorySwordSlot;
                                this.clickSlot(mc.thePlayer.inventoryContainer.windowId, this.convertSlotIndex(slot), preferredSwordHotbarSlot, 2);
                                return;
                            }
                        }
                        if (preferredPickaxeHotbarSlot >= 0 && preferredPickaxeHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredPickaxeHotbarSlot) && (equippedPickaxeSlot != -1 || inventoryPickaxeSlot != -1)) {
                            usedHotbarSlots.add(preferredPickaxeHotbarSlot);
                            if (equippedPickaxeSlot != preferredPickaxeHotbarSlot && inventoryPickaxeSlot != preferredPickaxeHotbarSlot) {
                                int slot = equippedPickaxeSlot != -1 ? equippedPickaxeSlot : inventoryPickaxeSlot;
                                this.clickSlot(mc.thePlayer.inventoryContainer.windowId, this.convertSlotIndex(slot), preferredPickaxeHotbarSlot, 2);
                                return;
                            }
                        }
                        if (preferredShovelHotbarSlot >= 0 && preferredShovelHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredShovelHotbarSlot) && (equippedShovelSlot != -1 || inventoryShovelSlot != -1)) {
                            usedHotbarSlots.add(preferredShovelHotbarSlot);
                            if (equippedShovelSlot != preferredShovelHotbarSlot && inventoryShovelSlot != preferredShovelHotbarSlot) {
                                int slot = equippedShovelSlot != -1 ? equippedShovelSlot : inventoryShovelSlot;
                                this.clickSlot(mc.thePlayer.inventoryContainer.windowId, this.convertSlotIndex(slot), preferredShovelHotbarSlot, 2);
                                return;
                            }
                        }
                        if (preferredAxeHotbarSlot >= 0 && preferredAxeHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredAxeHotbarSlot) && (equippedAxeSlot != -1 || inventoryAxeSlot != -1)) {
                            usedHotbarSlots.add(preferredAxeHotbarSlot);
                            if (equippedAxeSlot != preferredAxeHotbarSlot && inventoryAxeSlot != preferredAxeHotbarSlot) {
                                int slot = equippedAxeSlot != -1 ? equippedAxeSlot : inventoryAxeSlot;
                                this.clickSlot(mc.thePlayer.inventoryContainer.windowId, this.convertSlotIndex(slot), preferredAxeHotbarSlot, 2);
                                return;
                            }
                        }
                        if (preferredBlocksHotbarSlot >= 0 && preferredBlocksHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredBlocksHotbarSlot) && inventoryBlocksSlot != -1) {
                            usedHotbarSlots.add(preferredBlocksHotbarSlot);
                            if (inventoryBlocksSlot != preferredBlocksHotbarSlot) {
                                this.clickSlot(mc.thePlayer.inventoryContainer.windowId, this.convertSlotIndex(inventoryBlocksSlot), preferredBlocksHotbarSlot, 2);
                                return;
                            }
                        }
                        if (this.dropTrash.getValue()) {
                            int currentBlockCount = this.getStackSize(inventoryBlocksSlot);
                            for (int i = 0; i < 36; i++) {
                                if (!equippedArmorSlots.contains(i)
                                        && !inventoryArmorSlots.contains(i)
                                        && equippedSwordSlot != i
                                        && inventorySwordSlot != i
                                        && equippedPickaxeSlot != i
                                        && inventoryPickaxeSlot != i
                                        && equippedShovelSlot != i
                                        && inventoryShovelSlot != i
                                        && equippedAxeSlot != i
                                        && inventoryAxeSlot != i
                                        && inventoryBlocksSlot != i) {
                                    ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                                    if (stack != null) {
                                        boolean isBlock = ItemUtil.isBlock(stack);
                                        if (ItemUtil.isNotSpecialItem(stack) || isBlock && currentBlockCount >= this.blocks.getValue()) {
                                            this.clickSlot(mc.thePlayer.inventoryContainer.windowId, this.convertSlotIndex(i), 1, 4);
                                            return;
                                        }
                                        if (isBlock) {
                                            currentBlockCount += stack.stackSize;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onClick(WindowClickEvent event) {
        this.actionDelay = RandomUtils.nextInt(this.minDelay.getValue() + 1, this.maxDelay.getValue() + 2);
    }

    @Override
    public void verifyValue(String mode) {
        switch (mode) {
            case "min-delay":
                if (this.minDelay.getValue() > this.maxDelay.getValue()) {
                    this.maxDelay.setValue(this.minDelay.getValue());
                }
                break;
            case "max-delay":
                if (this.minDelay.getValue() > this.maxDelay.getValue()) {
                    this.minDelay.setValue(this.maxDelay.getValue());
                }
        }
    }
}
