package myau.module.modules;

import myau.Myau;
import myau.enums.FloatModules;
import myau.event.EventTarget;
import myau.event.types.Priority;
import myau.events.LivingUpdateEvent;
import myau.events.PlayerUpdateEvent;
import myau.events.RightClickMouseEvent;
import myau.module.Module;
import myau.util.BlockUtil;
import myau.util.ItemUtil;
import myau.util.PlayerUtil;
import myau.util.TeamUtil;
import myau.property.properties.BooleanProperty;
import myau.property.properties.PercentProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.BlockPos;

public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int lastSlot = -1;
    
    // 现有属性
    public final ModeProperty swordMode = new ModeProperty("sword-mode", 1, new String[]{"NONE", "VANILLA"});
    public final PercentProperty swordMotion = new PercentProperty("sword-motion", 100, () -> this.swordMode.getValue() != 0);
    public final BooleanProperty swordSprint = new BooleanProperty("sword-sprint", true, () -> this.swordMode.getValue() != 0);
    public final BooleanProperty botCheck = new BooleanProperty("bot-check", true);
    
    // >>> 新增属性: 剑模式范围检测
    public final BooleanProperty rangeCheck = new BooleanProperty("sword-range-check", false, () -> this.swordMode.getValue() != 0);
    public final FloatProperty triggerRange = new FloatProperty("trigger-range", 6.0F, 3.0F, 10.0F, () -> this.rangeCheck.getValue());
    // <<< 新增属性
    
    public final ModeProperty foodMode = new ModeProperty("food-mode", 0, new String[]{"NONE", "VANILLA", "FLOAT"});
    public final PercentProperty foodMotion = new PercentProperty("food-motion", 100, () -> this.foodMode.getValue() != 0);
    public final BooleanProperty foodSprint = new BooleanProperty("food-sprint", true, () -> this.foodMode.getValue() != 0);
    public final ModeProperty bowMode = new ModeProperty("bow-mode", 0, new String[]{"NONE", "VANILLA", "FLOAT"});
    public final PercentProperty bowMotion = new PercentProperty("bow-motion", 100, () -> this.bowMode.getValue() != 0);
    public final BooleanProperty bowSprint = new BooleanProperty("bow-sprint", true, () -> this.bowMode.getValue() != 0);

    public NoSlow() {
        super("NoSlow", false);
    }

    public boolean isSwordActive() {
        return this.swordMode.getValue() != 0 && ItemUtil.isHoldingSword();
    }

    public boolean isFoodActive() {
        return this.foodMode.getValue() != 0 && ItemUtil.isEating();
    }

    public boolean isBowActive() {
        return this.bowMode.getValue() != 0 && ItemUtil.isUsingBow();
    }

    public boolean isFloatMode() {
        return this.foodMode.getValue() == 2 && ItemUtil.isEating()
                || this.bowMode.getValue() == 2 && ItemUtil.isUsingBow();
    }

    public boolean isAnyActive() {
        return mc.thePlayer.isUsingItem() && (this.isSwordActive() || this.isFoodActive() || this.isBowActive());
    }

    public boolean canSprint() {
        return this.isSwordActive() && this.swordSprint.getValue()
                || this.isFoodActive() && this.foodSprint.getValue()
                || this.isBowActive() && this.bowSprint.getValue();
    }

    public int getMotionMultiplier() {
        if (ItemUtil.isHoldingSword()) {
            return this.swordMotion.getValue();
        } else if (ItemUtil.isEating()) {
            return this.foodMotion.getValue();
        } else {
            return ItemUtil.isUsingBow() ? this.bowMotion.getValue() : 100;
        }
    }
    
    // >>> 新增函数: 检测附近是否有敌人
    private boolean isEnemyNearby(double range) {
        // 遍历世界中的所有实体
        for (Object entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof net.minecraft.entity.player.EntityPlayer) {
                net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) entity;
    
                // 检查基础条件:
                // 1. 必须是另一个玩家 (不是自己)
                // 2. 不是朋友
                // 3. 不是队友
                // 4. 没有死亡
                // 5. 【新增】如果 botCheck 开启，则不能是 Bot
                if (player != mc.thePlayer
                    && !TeamUtil.isFriend(player)
                    && !TeamUtil.isSameTeam(player)
                    && player.deathTime <= 0
                    && (!this.botCheck.getValue() || !TeamUtil.isBot(player))) { // <--- 关键的新增条件
    
                    // 检查距离
                    if (mc.thePlayer.getDistanceToEntity(player) <= range) {
                        return true; // 找到一个附近的有效玩家
                    }
                }
            }
        }
        return false; // 未找到附近的敌对玩家
    }
    // <<< 新增函数

@EventTarget
public void onLivingUpdate(LivingUpdateEvent event) {
    if (this.isEnabled() && this.isAnyActive()) {
        
        boolean shouldBypassSlow = true;
        
        // 检查是否在剑模式下，并开启了范围检测
        if (this.isSwordActive() && this.rangeCheck.getValue()) {
            // 如果开启了范围检测，但附近没有敌人，则不触发NoSlow
            if (!this.isEnemyNearby((double) this.triggerRange.getValue())) { 
                shouldBypassSlow = false; // 禁用 NoSlow
            }
        }

        if (shouldBypassSlow) {
            // === 逻辑 A: 开启 NoSlow (自定义/无减速) ===
            float multiplier = (float) this.getMotionMultiplier() / 100.0F;
            mc.thePlayer.movementInput.moveForward *= multiplier;
            mc.thePlayer.movementInput.moveStrafe *= multiplier;
            if (!this.canSprint()) {
                mc.thePlayer.setSprinting(false);
            }
        } else {
            // === 逻辑 B: 关闭 NoSlow，恢复原版减速 (20% 速度) ===
            // 只有在剑模式下使用物品时，才强制应用原版减速和取消疾跑
            if (this.isSwordActive() && mc.thePlayer.isUsingItem()) {
                // 原版使用物品时（如格挡）将移动速度降低 80%，即只剩下 20% (0.2F)
                float vanillaSpeedMultiplier = 0.2F; 
                
                // 应用原版 20% 速度
                mc.thePlayer.movementInput.moveForward *= vanillaSpeedMultiplier;
                mc.thePlayer.movementInput.moveStrafe *= vanillaSpeedMultiplier;
                
                // 取消疾跑
                mc.thePlayer.setSprinting(false);
            }
        }
    }
}

    @EventTarget(Priority.LOW)
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled() && this.isFloatMode()) {
            int item = mc.thePlayer.inventory.currentItem;
            if (this.lastSlot != item && PlayerUtil.isUsingItem()) {
                this.lastSlot = item;
                Myau.floatManager.setFloatState(true, FloatModules.NO_SLOW);
            }
        } else {
            this.lastSlot = -1;
            Myau.floatManager.setFloatState(false, FloatModules.NO_SLOW);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            if (mc.objectMouseOver != null) {
                switch (mc.objectMouseOver.typeOfHit) {
                    case BLOCK:
                        BlockPos blockPos = mc.objectMouseOver.getBlockPos();
                        if (BlockUtil.isInteractable(blockPos) && !PlayerUtil.isSneaking()) {
                            return;
                        }
                        break;
                    case ENTITY:
                        Entity entityHit = mc.objectMouseOver.entityHit;
                        if (entityHit instanceof EntityVillager) {
                            return;
                        }
                        if (entityHit instanceof EntityLivingBase && TeamUtil.isShop((EntityLivingBase) entityHit)) {
                            return;
                        }
                }
            }
            if (this.isFloatMode() && !Myau.floatManager.isPredicted() && mc.thePlayer.onGround) {
                event.setCancelled(true);
                mc.thePlayer.motionY = 0.42F;
            }
        }
    }
}
