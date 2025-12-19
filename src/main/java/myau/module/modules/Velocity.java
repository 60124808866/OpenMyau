package myau.module.modules;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.enums.DelayModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.*;
import myau.mixin.IAccessorEntity; // ✅ 已适配你的 mixin 包路径
import myau.module.Module;
import myau.util.ChatUtil;
import myau.util.MoveUtil;
import myau.util.RotationUtil;
import myau.management.RotationState;
import myau.property.properties.*;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;

import java.util.Random;

public class Velocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private int chanceCounter = 0;
    private int delayChanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean reverseFlag = false;
    private boolean pendingJumpReset = false;
    private boolean jumpFlag = false;
    private int rotatoTickCounter = 0;
    private float[] targetRotation = null;
    private double knockbackX = 0;
    private double knockbackZ = 0;

    // 记录延迟开始时间（用于毫秒级控制）
    private long delayStartTime = 0;

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{
            "VANILLA", "JUMP", "Grim", "Legit-NoXZ"
    });

    public final PercentProperty chance = new PercentProperty("chance", 100);
    public final PercentProperty horizontal = new PercentProperty("horizontal", 100);
    public final PercentProperty vertical = new PercentProperty("vertical", 100);
    public final PercentProperty explosionHorizontal = new PercentProperty("explosions-horizontal", 100);
    public final PercentProperty explosionVertical = new PercentProperty("explosions-vertical", 100);
    public final BooleanProperty fakeCheck = new BooleanProperty("fake-check", true);
    public final BooleanProperty debugLog = new BooleanProperty("debug-log", false);

    // ✅ 改为 FloatProperty，支持小数延迟
    public final FloatProperty delayTicks = new FloatProperty("delay-ticks", 1.5F, 0.1F, 10.0F, () ->
            this.mode.getValue() == 2);
    public final PercentProperty delayChance = new PercentProperty("delay-chance", 100, () ->
            this.mode.getValue() == 2);

    // ✅ 新增：Grim 模式的 JumpReset 开关
    public final BooleanProperty grimJumpReset = new BooleanProperty("JumpReset", true, () ->
            this.mode.getValue() == 2);

    public final BooleanProperty rotate = new BooleanProperty("Rotate", true, () ->
            this.mode.getValue() == 3);
    public final IntProperty rotateTick = new IntProperty("RotateTick", 2, 1, 12, () ->
            this.mode.getValue() == 3 && this.rotate.getValue());
    public final BooleanProperty autoMove = new BooleanProperty("AutoMove", true, () ->
            this.mode.getValue() == 3 && this.rotate.getValue());
    public final BooleanProperty jumpReset = new BooleanProperty("jump-reset", true, () ->
            this.mode.getValue() == 3);
    public final PercentProperty reduceChance = new PercentProperty("reduce-chance", 100, () ->
            this.mode.getValue() == 3);

    public final BooleanProperty USerDP = new BooleanProperty("USer DP", false, () ->
            this.mode.getValue() == 1);
    public final IntProperty ExhIemDP = new IntProperty("ExhIemDP", 1, 1, 5, () ->
            this.mode.getValue() == 1 && this.USerDP.getValue());

    private boolean isInLiquidOrWeb() {
        return mc.thePlayer != null && (mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb());
    }

    // ✅ 修复 1/2: 移除 onGround 限制，防止空中 delay 死锁
    private boolean canDelay() {
        if (mc.thePlayer == null) return false; // 不再检查 onGround
        try {
            KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
            return killAura == null || !killAura.isEnabled() || !killAura.shouldAutoBlock();
        } catch (Exception e) {
            return true;
        }
    }

    public Velocity() {
        super("Velocity", false);
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled() || mc.thePlayer == null) {
            this.pendingExplosion = false;
            this.allowNext = true;
            return;
        }

        // ✅ 保留你现有的 Grim 逻辑（百分比减少 + jumpFlag）
        if (this.mode.getValue() == 2) {
            if (this.horizontal.getValue() > 0) {
                event.setX(event.getX() * (double) this.horizontal.getValue() / 100.0);
                event.setZ(event.getZ() * (double) this.horizontal.getValue() / 100.0);
            } else {
                event.setX(mc.thePlayer.motionX);
                event.setZ(mc.thePlayer.motionZ);
            }
            if (this.vertical.getValue() > 0) {
                event.setY(event.getY() * (double) this.vertical.getValue() / 100.0);
            } else {
                event.setY(mc.thePlayer.motionY);
            }

            if (this.grimJumpReset.getValue() && event.getY() > 0.0) {
                this.jumpFlag = true;
            }

            return;
        }

        if (this.mode.getValue() == 3) {
            if (new Random().nextInt(100) >= this.reduceChance.getValue()) {
                return;
            }

            if (this.jumpReset.getValue() && mc.thePlayer.onGround) {
                this.pendingJumpReset = true;
            }

            if (this.horizontal.getValue() > 0) {
                event.setX(event.getX() * (double) this.horizontal.getValue() / 100.0);
                event.setZ(event.getZ() * (double) this.horizontal.getValue() / 100.0);
            } else {
                event.setX(mc.thePlayer.motionX);
                event.setZ(mc.thePlayer.motionZ);
            }
            if (this.vertical.getValue() > 0) {
                event.setY(event.getY() * (double) this.vertical.getValue() / 100.0);
            } else {
                event.setY(mc.thePlayer.motionY);
            }

            if (this.rotate.getValue() && event.getY() > 0.0) {
                this.knockbackX = event.getX();
                this.knockbackZ = event.getZ();
                if (Math.abs(this.knockbackX) > 0.01 || Math.abs(this.knockbackZ) > 0.01) {
                    this.rotatoTickCounter = 1;
                }
            }

            return;
        }

        // 原有 VANILLA / JUMP / fakeCheck 逻辑（mode == 0 或 1）
        if (!this.allowNext || !(Boolean) this.fakeCheck.getValue()) {
            this.allowNext = true;
            if (this.pendingExplosion) {
                this.pendingExplosion = false;
                handleExplosion(event);
            } else {
                this.chanceCounter = (this.chanceCounter % 100) + this.chance.getValue();
                if (this.chanceCounter >= 100) {
                    this.jumpFlag = (this.mode.getValue() == 1) && event.getY() > 0.0;

                    if (this.mode.getValue() == 1 && event.getY() > 0.0) {
                        if (this.horizontal.getValue() > 0) {
                            event.setX(event.getX() * (double) this.horizontal.getValue() / 100.0);
                            event.setZ(event.getZ() * (double) this.horizontal.getValue() / 100.0);
                        } else {
                            event.setX(mc.thePlayer.motionX);
                            event.setZ(mc.thePlayer.motionZ);
                        }
                        if (this.vertical.getValue() > 0) {
                            event.setY(event.getY() * (double) this.vertical.getValue() / 100.0);
                        } else {
                            event.setY(mc.thePlayer.motionY);
                        }
                    } else {
                        applyVanilla(event);
                    }
                }
            }
        }
    }

    private void applyVanilla(KnockbackEvent event) {
        if (this.horizontal.getValue() > 0) {
            event.setX(event.getX() * (double) this.horizontal.getValue() / 100.0);
            event.setZ(event.getZ() * (double) this.horizontal.getValue() / 100.0);
        } else {
            event.setX(mc.thePlayer.motionX);
            event.setZ(mc.thePlayer.motionZ);
        }
        if (this.vertical.getValue() > 0) {
            event.setY(event.getY() * (double) this.vertical.getValue() / 100.0);
        } else {
            event.setY(mc.thePlayer.motionY);
        }
    }

    private void handleExplosion(KnockbackEvent event) {
        if (this.explosionHorizontal.getValue() > 0) {
            event.setX(event.getX() * (double) this.explosionHorizontal.getValue() / 100.0);
            event.setZ(event.getZ() * (double) this.explosionHorizontal.getValue() / 100.0);
        } else {
            event.setX(mc.thePlayer.motionX);
            event.setZ(mc.thePlayer.motionZ);
        }
        if (this.explosionVertical.getValue() > 0) {
            event.setY(event.getY() * (double) this.explosionVertical.getValue() / 100.0);
        } else {
            event.setY(mc.thePlayer.motionY);
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer != null
                    && mc.thePlayer.onGround
                    && this.mode.getValue() == 2
                    && this.grimJumpReset.getValue()
                    && !isInLiquidOrWeb()
                    && !mc.gameSettings.keyBindJump.isKeyDown()) {

                mc.thePlayer.movementInput.jump = true;
                if (this.debugLog.getValue()) {
                    ChatUtil.sendFormatted(String.format("%s[Grim] JumpReset executed (auto)&r", Myau.clientName));
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE || event.isCancelled() || mc.thePlayer == null)
            return;

        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
            if (packet.getEntityID() != mc.thePlayer.getEntityId()) return;

            if (this.mode.getValue() == 2) {
                try {
                    LongJump longJump = (LongJump) Myau.moduleManager.getModule(LongJump.class);
                    boolean canProceed = !this.reverseFlag
                            && !this.canDelay()
                            && !this.isInLiquidOrWeb()
                            && !this.pendingExplosion
                            && (!this.allowNext || !(Boolean) this.fakeCheck.getValue())
                            && (longJump == null || !longJump.isEnabled() || !longJump.canStartJump());

                    if (canProceed) {
                        this.delayChanceCounter = (this.delayChanceCounter % 100) + this.delayChance.getValue();
                        if (this.delayChanceCounter >= 100) {
                            this.delayStartTime = System.currentTimeMillis(); // ✅ 重置计时
                            Myau.delayManager.setDelayState(true, DelayModules.VELOCITY);
                            Myau.delayManager.delayedPacket.offer(packet);
                            event.setCancelled(true);
                            this.reverseFlag = true;
                            if (this.debugLog.getValue()) {
                                ChatUtil.sendFormatted(String.format("%s[Grim] Delayed velocity packet&r", Myau.clientName));
                            }
                            this.delayChanceCounter = 0;
                        }
                    }
                } catch (Exception ignored) {}
                return;
            }

            if (this.mode.getValue() == 1 && this.USerDP.getValue() && mc.thePlayer != null && !mc.thePlayer.onGround) {
                Myau.delayManager.setDelayState(true, DelayModules.VELOCITY);
                Myau.delayManager.delayedPacket.offer(packet);
                event.setCancelled(true);
                if (this.debugLog.getValue()) {
                    ChatUtil.sendFormatted(String.format("%s[Jump] Delayed velocity packet in air&r", Myau.clientName));
                }
                return;
            }

            if (this.debugLog.getValue()) {
                ChatUtil.sendFormatted(String.format(
                        "%sVelocity (tick: %d, x: %.2f, y: %.2f, z: %.2f)&r",
                        Myau.clientName,
                        mc.thePlayer.ticksExisted,
                        (double) packet.getMotionX() / 8000.0,
                        (double) packet.getMotionY() / 8000.0,
                        (double) packet.getMotionZ() / 8000.0
                ));
            }
        } else if (event.getPacket() instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus packet = (S19PacketEntityStatus) event.getPacket();
            if (mc.theWorld != null) {
                Entity entity = packet.getEntity(mc.theWorld);
                if (entity != null && entity.equals(mc.thePlayer) && packet.getOpCode() == 2) {
                    this.allowNext = false;
                }
            }
        } else if (event.getPacket() instanceof S27PacketExplosion) {
            S27PacketExplosion packet = (S27PacketExplosion) event.getPacket();
            if (packet.func_149149_c() != 0.0F || packet.func_149144_d() != 0.0F || packet.func_149147_e() != 0.0F) {
                this.pendingExplosion = true;
                if (this.explosionHorizontal.getValue() == 0 || this.explosionVertical.getValue() == 0) {
                    event.setCancelled(true);
                }
                if (this.debugLog.getValue()) {
                    ChatUtil.sendFormatted(String.format(
                            "%sExplosion (tick: %d, x: %.2f, y: %.2f, z: %.2f)&r",
                            Myau.clientName,
                            mc.thePlayer.ticksExisted,
                            mc.thePlayer.motionX + (double) packet.func_149149_c(),
                            mc.thePlayer.motionY + (double) packet.func_149144_d(),
                            mc.thePlayer.motionZ + (double) packet.func_149147_e()
                    ));
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.mode.getValue() == 3 && this.rotatoTickCounter > 0 && this.rotatoTickCounter <= this.rotateTick.getValue()) {
                if (this.rotatoTickCounter == 1) {
                    double deltaX = -this.knockbackX;
                    double deltaZ = -this.knockbackZ;
                    this.targetRotation = RotationUtil.getRotationsTo(deltaX, 0, deltaZ, event.getYaw(), event.getPitch());
                }
                if (this.targetRotation != null) {
                    event.setRotation(this.targetRotation[0], this.targetRotation[1], 2);
                    event.setPervRotation(this.targetRotation[0], 2);
                }
            }
        }

        if (event.getType() == EventType.POST) {
            if (this.mode.getValue() == 3 && this.pendingJumpReset && mc.thePlayer != null) {
                mc.thePlayer.jump();
                this.pendingJumpReset = false;
                if (this.debugLog.getValue()) {
                    ChatUtil.sendFormatted(String.format("%s[Legit-NoXZ] Jump executed&r", Myau.clientName));
                }
            }

            if (this.mode.getValue() == 2 && this.reverseFlag) {
                // ✅ 修复 2/2: 确保超时也能释放，避免死锁
                if (mc.thePlayer.onGround || this.isInLiquidOrWeb()) {
                    Myau.delayManager.setDelayState(false, DelayModules.VELOCITY);
                    this.reverseFlag = false;
                } else {
                    float delayMs = this.delayTicks.getValue() * 50.0F;
                    if (System.currentTimeMillis() - this.delayStartTime >= delayMs) {
                        Myau.delayManager.setDelayState(false, DelayModules.VELOCITY);
                        this.reverseFlag = false;
                    }
                }
            }

            if (this.mode.getValue() == 1 && this.USerDP.getValue() && !Myau.delayManager.delayedPacket.isEmpty()) {
                if (mc.thePlayer != null && (mc.thePlayer.onGround || this.isInLiquidOrWeb())) {
                    Myau.delayManager.setDelayState(false, DelayModules.VELOCITY);
                }
            }

            if (this.mode.getValue() == 3 && this.rotatoTickCounter > 0 && this.rotatoTickCounter <= this.rotateTick.getValue()) {
                this.rotatoTickCounter++;
                if (this.rotatoTickCounter > this.rotateTick.getValue()) {
                    this.rotatoTickCounter = 0;
                    this.targetRotation = null;
                    this.knockbackX = 0;
                    this.knockbackZ = 0;
                }
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (this.isEnabled() && this.mode.getValue() == 3 && this.rotatoTickCounter > 0 && this.rotatoTickCounter <= this.rotateTick.getValue()) {
            if (this.autoMove.getValue() && mc.thePlayer != null) {
                mc.thePlayer.movementInput.moveForward = 1.0F;
            }
            if (this.targetRotation != null && RotationState.isActived() && RotationState.getPriority() == 2.0F && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.onDisabled();
    }

    @Override
    public void onEnabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.chanceCounter = 0;
        this.delayChanceCounter = 0;
        this.reverseFlag = false;
        this.pendingJumpReset = false;
        this.rotatoTickCounter = 0;
        this.targetRotation = null;
        this.delayStartTime = 0;
        this.jumpFlag = false;
    }

    @Override
    public void onDisabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.chanceCounter = 0;
        this.delayChanceCounter = 0;
        this.reverseFlag = false;
        this.pendingJumpReset = false;
        this.rotatoTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0;
        this.knockbackZ = 0;
        this.delayStartTime = 0;
        this.jumpFlag = false;
        Myau.delayManager.delayedPacket.clear();
    }

    @Override
    public String[] getSuffix() {
        if (this.mode.getValue() == 3) {
            return new String[]{"Legit-NoXZ"};
        }
        String modeName = this.mode.getModeString();
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName)};
    }
}
