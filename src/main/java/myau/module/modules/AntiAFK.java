package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.UpdateEvent;
import myau.mixin.IAccessorKeyBinding;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;

public class AntiAFK extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ============================================================
    //        Properties（GUI 可调） — 与 AimAssist 风格一致
    // ============================================================

    // 左走多久（tick）
    public final IntProperty leftTime =
            new IntProperty("left-time", 40, 5, 200);

    // 右走多久（tick）
    public final IntProperty rightTime =
            new IntProperty("right-time", 40, 5, 200);

    // 无操作多久后开始 AntiAFK（tick）
    public final IntProperty activateAfter =
            new IntProperty("activate-after", 200, 20, 2000);

    // 是否启用跳跃动作
    public final BooleanProperty enableJump =
            new BooleanProperty("enable-jump", true);

    // 跳跃间隔（tick）
    public final IntProperty jumpInterval =
            new IntProperty("jump-interval", 100, 20, 300);


    // ============================================================
    //                    内部运行参数
    // ============================================================

    private int noInputTicks = 0;
    private int walkTimer = 0;
    private boolean walkingRight = true;


    // ============================================================
    //                        构造函数
    // ============================================================

    public AntiAFK() {
        super("AntiAFK", false);
    }


    // ============================================================
    //                       主事件：Update
    // ============================================================

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE || !this.isEnabled()) return;

        GameSettings gs = mc.gameSettings;

        // 玩家手动按键 → 重置 AFK 计时
        if (gs.keyBindJump.isPressed()
                || gs.keyBindForward.isPressed()
                || gs.keyBindBack.isPressed()
                || gs.keyBindLeft.isPressed()
                || gs.keyBindRight.isPressed()) {

            noInputTicks = 0;
        } else {
            noInputTicks++;
        }

        // 未到触发时间，停止动作
        if (noInputTicks < activateAfter.getValue()) return;


        // ============================================================
        //     每 5 tick 释放所有按键，防止卡住
        // ============================================================
        if (mc.thePlayer.ticksExisted % 5 == 0) {
            ((IAccessorKeyBinding)gs.keyBindRight).setPressed(false);
            ((IAccessorKeyBinding)gs.keyBindLeft).setPressed(false);
            ((IAccessorKeyBinding)gs.keyBindJump).setPressed(false);
        }


        // ============================================================
        //                   左右移动循环（可调节）
        // ============================================================
        walkTimer++;

        if (walkingRight) {

            // 按右键
            ((IAccessorKeyBinding)gs.keyBindRight).setPressed(true);

            if (walkTimer >= rightTime.getValue()) {
                walkTimer = 0;
                walkingRight = false; // 切换到左
            }

        } else {

            // 按左键
            ((IAccessorKeyBinding)gs.keyBindLeft).setPressed(true);

            if (walkTimer >= leftTime.getValue()) {
                walkTimer = 0;
                walkingRight = true; // 切换到右
            }
        }


        // ============================================================
        //                     跳跃（可开关）
        // ============================================================
        if (enableJump.getValue()
                && mc.thePlayer.ticksExisted % jumpInterval.getValue() == 0) {

            ((IAccessorKeyBinding)gs.keyBindJump).setPressed(true);
        }
    }
}
