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

    // ★ 保留的配置（但不再检测范围）
    public final BooleanProperty rangeCheck = new BooleanProperty("sword-range-check", false);

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
    //               判断模块是否激活
    // ========================================================
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
        return mc.thePlayer.isUsingItem() && (isSwordActive() || isFoodActive() || isBowActive());
    }

    public boolean canSprint() {
        return isSwordActive() && swordSprint.getValue()
                || isFoodActive() && foodSprint.getValue()
                || isBowActive() && bowSprint.getValue();
    }

    public int getMotionMultiplier() {
        if (ItemUtil.isHoldingSword())
            return swordMotion.getValue();
        else if (ItemUtil.isEating())
            return foodMotion.getValue();
        else
            return ItemUtil.isUsingBow() ? bowMotion.getValue() : 100;
    }

    // ========================================================
    //        KillAura 判断（与 AutoTool 完全一致）
    // ========================================================
    private boolean isKillAuraActive() {
        KillAura ka = (KillAura) Myau.moduleManager.modules.get(KillAura.class);

        if (ka == null || !ka.isEnabled())
            return false;

        return TeamUtil.isEntityLoaded(ka.getTarget())
                && ka.isAttackAllowed();
    }

    // ========================================================
    //                  主 NoSlow 逻辑
    // ========================================================
    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && this.isAnyActive()) {

            boolean shouldBypassSlow = true;

            // -----------------------------------------------------------
            // ★ 新逻辑：
            // rangeCheck = true → 必须 KillAura 正在攻击才能触发 NoSlow
            // rangeCheck = false → 不受 KillAura 限制，旧 NoSlow 行为
            // -----------------------------------------------------------
            if (this.isSwordActive() && this.rangeCheck.getValue()) {
                if (!isKillAuraActive()) {
                    shouldBypassSlow = false;
                }
            }

            if (shouldBypassSlow) {
                // === A: 启动 NoSlow ===
                float multiplier = (float) this.getMotionMultiplier() / 100F;
                mc.thePlayer.movementInput.moveForward *= multiplier;
                mc.thePlayer.movementInput.moveStrafe *= multiplier;

                if (!this.canSprint()) {
                    mc.thePlayer.setSprinting(false);
                }
            } else {
                // === B: 恢复原版剑减速（20%） ===
                if (isSwordActive() && mc.thePlayer.isUsingItem()) {
                    float vanilla = 0.2F;
                    mc.thePlayer.movementInput.moveForward *= vanilla;
                    mc.thePlayer.movementInput.moveStrafe *= vanilla;
                    mc.thePlayer.setSprinting(false);
                }
            }
        }
    }

    // ========================================================
    //                   FLOAT 模式处理
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
    //                 右键处理（保持原逻辑）
    // ========================================================
    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            if (mc.objectMouseOver != null) {
                switch (mc.objectMouseOver.typeOfHit) {
                    case BLOCK:
                        BlockPos bp = mc.objectMouseOver.getBlockPos();
                        if (BlockUtil.isInteractable(bp) && !PlayerUtil.isSneaking()) return;
                        break;
                    case ENTITY:
                        Entity e = mc.objectMouseOver.entityHit;
                        if (e instanceof EntityVillager) return;
                        if (e instanceof EntityLivingBase && TeamUtil.isShop((EntityLivingBase) e)) return;
                }
            }
            if (this.isFloatMode() && !Myau.floatManager.isPredicted() && mc.thePlayer.onGround) {
                event.setCancelled(true);
                mc.thePlayer.motionY = 0.42F;
            }
        }
    }
}
