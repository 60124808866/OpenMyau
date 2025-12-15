package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.mixin.IAccessorKeyBinding;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.ItemUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;


public class AutoMiner extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    // 配置选项
    public final BooleanProperty autoWalk = new BooleanProperty("Auto-Walk", true);
    public final BooleanProperty turnFromBedrock = new BooleanProperty("Turn-From-Bedrock", true);
    public final BooleanProperty switchBack = new BooleanProperty("Tool-Switch-Back", true);
    public final IntProperty switchDelay = new IntProperty("Tool-Delay", 0, 0, 5);

    // AutoTool 参数
    private int previousSlot = -1;
    private int toolDelayCounter = 0;


    public AutoMiner() {
        super("AutoMiner", false);
    }


    // 强制按键
    private void pressKey(KeyBinding key, boolean pressed) {
        ((IAccessorKeyBinding) key).setPressed(pressed);
    }

    private void pressForward(boolean pressed) {
        pressKey(mc.gameSettings.keyBindForward, pressed);
    }


    @Override
    public void onDisabled() {
        super.onDisabled();

        pressForward(false);

        if (switchBack.getValue() && previousSlot != -1) {
            mc.thePlayer.inventory.currentItem = previousSlot;
        }

        previousSlot = -1;
        toolDelayCounter = 0;
    }


    @EventTarget
    public void onTick(TickEvent event) {

        if (!this.isEnabled() || event.getType() != EventType.PRE)
            return;

        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        // -------------------------
        // 1. 自动前进（长廊挖掘模式）
        // -------------------------
        if (autoWalk.getValue()) {
            pressForward(true);
        }


        // -------------------------
        // 2. 获取玩家前方现在指向的方块
        // -------------------------
        MovingObjectPosition hit = mc.objectMouseOver;

        if (hit == null || hit.typeOfHit != MovingObjectType.BLOCK)
            return;

        BlockPos pos = hit.getBlockPos();
        EnumFacing face = hit.sideHit;

        // 如果目标是基岩
        if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.bedrock) {
            if (turnFromBedrock.getValue()) {
                turnAway();
            }
            return;
        }


        // -------------------------
        // 3. AutoTool（自动切换最佳工具）
        // -------------------------
        boolean canSwitch = toolDelayCounter >= switchDelay.getValue();

        if (canSwitch) {
            int bestSlot = ItemUtil.findInventorySlot(
                    mc.thePlayer.inventory.currentItem,
                    mc.theWorld.getBlockState(pos).getBlock()
            );

            if (mc.thePlayer.inventory.currentItem != bestSlot) {
                if (previousSlot == -1) previousSlot = mc.thePlayer.inventory.currentItem;
                mc.thePlayer.inventory.currentItem = bestSlot;
            }
        }

        toolDelayCounter++;


        // -------------------------
        // 4. ⭐真正的后台挖掘逻辑（不会因 GUI 阻断）
        // -------------------------
        mc.playerController.onPlayerDamageBlock(pos, face);
        mc.thePlayer.swingItem(); // GUI 打开时也能播放


        // 没有停止，只要前方有方块就持续攻击 → 挖长廊
    }


    // 遇到基岩自动换方向
    private void turnAway() {
        EntityPlayerSP p = mc.thePlayer;
        EnumFacing f = p.getHorizontalFacing();

        switch (f) {
            case NORTH:
                p.rotationYaw = -90f;
                break;
            case EAST:
                p.rotationYaw = 0f;
                break;
            case SOUTH:
                p.rotationYaw = 90f;
                break;
            case WEST:
                p.rotationYaw = 180f;
                break;
        }
    }
}
