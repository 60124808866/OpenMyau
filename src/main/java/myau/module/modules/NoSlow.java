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

    // === 剑模式 ===
    public final ModeProperty swordMode = new ModeProperty("sword-mode", 1, new String[]{"NONE", "VANILLA"});
    public final PercentProperty swordMotion = new PercentProperty("sword-motion", 100, () -> this.swordMode.getValue() != 0);
    public final BooleanProperty swordSprint = new BooleanProperty("sword-sprint", true, () -> this.swordMode.getValue() != 0);
    public final BooleanProperty botCheck = new BooleanProperty("bot-check", true);

    // === 新增：范围检测 ===
    public final BooleanProperty rangeCheck = new BooleanProperty("sword-range-check", false, () -> this.swordMode.getValue() != 0);
    public final FloatProperty triggerRange = new FloatProperty("trigger-range", 6.0F, 3.0F, 10.0F, () -> this.rangeCheck.getValue());

    // === 食物模式 ===
    public final ModeProperty foodMode = new ModeProperty("food-mode", 0, new String[]{"NONE", "VANILLA", "FLOAT"});
    public final PercentProperty foodMotion = new PercentProperty("food-motion", 100, () -> this.foodMode.getValue() != 0);
    public final BooleanProperty foodSprint = new BooleanProperty("food-sprint", true, () -> this.foodMode.getValue() != 0);

    // === 弓模式 ===
    public final ModeProperty bowMode = new ModeProperty("bow-mode", 0, new String[]{"NONE", "VANILLA", "FLOAT"});
    public final PercentProperty bowMotion = new PercentProperty("bow-motion", 100, () -> this.bowMode.getValue() != 0);
    public final BooleanProperty bowSprint = new BooleanProperty("bow-sprint", true, () -> this.bowMode.getValue() != 0);

    public NoSlow() {
        super("NoSlow", false);
    }

    // ========================================================
    //                   Mode 判断
    // ========================================================
    public boolean isSwordActive() { return this.swordMode.getValue() != 0 && ItemUtil.isHoldingSword(); }
    public boolean isFoodActive() { return this.foodMode.getValue() != 0 && ItemUtil.isEating(); }
    public boolean isBowActive() { return this.bowMode.getValue() != 0 && ItemUtil.isUsingBow(); }

    public boolean isFloatMode() {
        return this.foodMode.getValue() == 2 && ItemUtil.isEating()
            || this.bowMode.getValue() == 2 && ItemUtil.isUsingBow();
    }

    public boolean isAnyActive() {
        return mc.thePlayer.isUsingItem() && (isSwordActive() || isFoodActive() || isBowActive());
    }

    public boolean canSprint() {
        return (isSwordActive() && swordSprint.getValue())
            || (isFoodActive() && foodSprint.getValue())
            || (isBowActive() && bowSprint.getValue());
    }

    public int getMotionMultiplier() {
        if (ItemUtil.isHoldingSword()) return swordMotion.getValue();
        if (ItemUtil.isEating()) return foodMotion.getValue();
        return ItemUtil.isUsingBow() ? bowMotion.getValue() : 100;
    }

    // ========================================================
    //                  KillAura 检测
    // ========================================================
    private boolean isKillAuraActive() {
        KillAura ka = (KillAura) Myau.moduleManager.modules.get(KillAura.class);

        if (ka == null || !ka.isEnabled())
            return false;

        return TeamUtil.isEntityLoaded(ka.getTarget()) && ka.isAttackAllowed();
    }

    // ========================================================
    //                 附近敌人检测 (用于关闭 NoSlow)
    // ========================================================
    private boolean isEnemyNearby(double range) {

        for (Object o : mc.theWorld.loadedEntityList) {

            if (o instanceof net.minecraft.entity.player.EntityPlayer) {
                net.minecraft.entity.player.EntityPlayer p = (net.minecraft.entity.player.EntityPlayer) o;

                if (p == mc.thePlayer) continue;
                if (TeamUtil.isFriend(p)) continue;
                if (TeamUtil.isSameTeam(p)) continue;
                if (p.deathTime > 0) continue;
                if (botCheck.getValue() && TeamUtil.isBot(p)) continue;

                if (mc.thePlayer.getDistanceToEntity(p) <= range)
                    return true;
            }
        }
        return false;
    }

    // ========================================================
    //                   主 NoSlow 逻辑
    // ========================================================
    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!this.isEnabled() || !this.isAnyActive())
            return;

        boolean shouldBypassSlow = false;

        // ① KillAura → 强制开启 NoSlow
        if (isKillAuraActive()) {
            shouldBypassSlow = true;
        }

        // ② 未开启 KA → 判断 enemyNearby 来关闭 NoSlow
        else {
            if (this.isSwordActive() && this.rangeCheck.getValue()) {

                boolean enemyNear = this.isEnemyNearby(this.triggerRange.getValue());

                if (enemyNear) {
                    shouldBypassSlow = false; // 附近有人 → 禁用 NoSlow
                } else {
                    shouldBypassSlow = true;  // 附近没人 → 启用
                }

            } else {
                // 未开启 rangeCheck → 默认启用 NoSlow
                shouldBypassSlow = true;
            }
        }

        // 应用最终结果
        if (shouldBypassSlow) {

            float multiplier = (float) this.getMotionMultiplier() / 100F;
            mc.thePlayer.movementInput.moveForward *= multiplier;
            mc.thePlayer.movementInput.moveStrafe *= multiplier;

            if (!this.canSprint())
                mc.thePlayer.setSprinting(false);

        } else {

            // 关闭 NoSlow → 恢复原版 20% 减速
            if (this.isSwordActive() && mc.thePlayer.isUsingItem()) {
                mc.thePlayer.movementInput.moveForward *= 0.2F;
                mc.thePlayer.movementInput.moveStrafe *= 0.2F;
                mc.thePlayer.setSprinting(false);
            }
        }
    }

    // ========================================================
    //                Float 模式处理
    // ========================================================
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

    // ========================================================
    //                  右键事件
    // ========================================================
    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {

        if (!this.isEnabled()) return;

        if (mc.objectMouseOver != null) {
            switch (mc.objectMouseOver.typeOfHit) {
                case BLOCK:
                    BlockPos pos = mc.objectMouseOver.getBlockPos();
                    if (BlockUtil.isInteractable(pos) && !PlayerUtil.isSneaking())
                        return;
                    break;

                case ENTITY:
                    Entity e = mc.objectMouseOver.entityHit;
                    if (e instanceof EntityVillager) return;
                    if (e instanceof EntityLivingBase && TeamUtil.isShop((EntityLivingBase)e))
                        return;
            }
        }

        if (this.isFloatMode() && !Myau.floatManager.isPredicted() && mc.thePlayer.onGround) {
            event.setCancelled(true);
            mc.thePlayer.motionY = 0.42F;
        }
    }
}
