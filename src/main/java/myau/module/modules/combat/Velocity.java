package myau.module.modules.combat;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.enums.ChatColors;
import myau.enums.DelayModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.*;
import myau.mixin.IAccessorEntity;
import myau.module.Category;
import myau.module.Module;
import myau.module.modules.move.LongJump;
import myau.util.ChatUtil;
import myau.util.MoveUtil;
import myau.property.properties.*;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;

public class Velocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int chanceCounter = 0;
    private int delayChanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean jumpFlag = false;
    private boolean reverseFlag = false;
    private boolean delayActive = false;
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"VANILLA", "JUMP", "DELAY", "REVERSE"});
    public final IntProperty delayTicks = new IntProperty("delay-ticks", 3, 1, 20, () -> this.mode.getValue() == 2);
    public final PercentProperty delayChance = new PercentProperty("delay-chance", 100, () -> this.mode.getValue() == 2);
    public final PercentProperty chance = new PercentProperty("chance", 100);
    public final PercentProperty horizontal = new PercentProperty("horizontal", 0);
    public final PercentProperty vertical = new PercentProperty("vertical", 100);
    public final PercentProperty explosionHorizontal = new PercentProperty("explosions-horizontal", 100);
    public final PercentProperty explosionVertical = new PercentProperty("explosions-vertical", 100);
    public final BooleanProperty fakeCheck = new BooleanProperty("fake-check", true);
    public final BooleanProperty debugLog = new BooleanProperty("debug-log", false);

    private boolean isInLiquidOrWeb() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb();
    }

    private boolean canDelay() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        return mc.thePlayer.onGround && (!killAura.isEnabled() || !killAura.shouldAutoBlock());
    }

    public Velocity() {
        super("Velocity", Category.Combat, false);
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            this.pendingExplosion = false;
            this.allowNext = true;
        } else if (!this.allowNext || !(Boolean) this.fakeCheck.getValue()) {
            this.allowNext = true;
            if (this.pendingExplosion) {
                this.pendingExplosion = false;
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
            } else {
                this.chanceCounter = this.chanceCounter % 100 + this.chance.getValue();
                if (this.chanceCounter >= 100) {
                    this.jumpFlag = (this.mode.getValue() == 1 || this.mode.getValue() == 2) && event.getY() > 0.0;
                    this.delayActive = this.mode.getValue() == 3;
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
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.POST) {
            if (this.reverseFlag
                    && (
                    this.canDelay()
                            || this.isInLiquidOrWeb()
                            || Myau.delayManager.isDelay() >= (long) this.delayTicks.getValue()
            )) {
                Myau.delayManager.setDelayState(false, DelayModules.VELOCITY);
                this.reverseFlag = false;
            }
            if (this.delayActive) {
                MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                this.delayActive = false;
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer.onGround && mc.thePlayer.isSprinting() && !mc.thePlayer.isPotionActive(Potion.jump) && !this.isInLiquidOrWeb()) {
                mc.thePlayer.movementInput.jump = true;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
                if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                    LongJump longJump = (LongJump) Myau.moduleManager.modules.get(LongJump.class);
                    if (this.mode.getValue() == 2
                            && !this.reverseFlag
                            && !this.canDelay()
                            && !this.isInLiquidOrWeb()
                            && !this.pendingExplosion
                            && (!this.allowNext || !(Boolean) this.fakeCheck.getValue())
                            && (!longJump.isEnabled() || !longJump.canStartJump())) {
                        this.delayChanceCounter = this.delayChanceCounter % 100 + this.delayChance.getValue();
                        if (this.delayChanceCounter >= 100) {
                            Myau.delayManager.setDelayState(true, DelayModules.VELOCITY);
                            Myau.delayManager.delayedPacket.offer(packet);
                            event.setCancelled(true);
                            this.reverseFlag = true;
                            return;
                        }
                    }
                    if (this.debugLog.getValue()) {
                        ChatUtil.sendFormatted(
                                String.format(
                                        "%sVelocity (&otick: %d, x: %.2f, y: %.2f, z: %.2f&r)&r",
                                        Myau.clientName,
                                        mc.thePlayer.ticksExisted,
                                        (double) packet.getMotionX() / 8000.0,
                                        (double) packet.getMotionY() / 8000.0,
                                        (double) packet.getMotionZ() / 8000.0
                                )
                        );
                    }
                }
            } else if (!(event.getPacket() instanceof S27PacketExplosion)) {
                if (event.getPacket() instanceof S19PacketEntityStatus) {
                    S19PacketEntityStatus packet = (S19PacketEntityStatus) event.getPacket();
                    Entity entity = packet.getEntity(mc.theWorld);
                    if (entity != null && entity.equals(mc.thePlayer) && packet.getOpCode() == 2) {
                        this.allowNext = false;
                    }
                }
            } else {
                S27PacketExplosion packet = (S27PacketExplosion) event.getPacket();
                if (packet.func_149149_c() != 0.0F || packet.func_149144_d() != 0.0F || packet.func_149147_e() != 0.0F) {
                    this.pendingExplosion = true;
                    if (this.explosionHorizontal.getValue() == 0 || this.explosionVertical.getValue() == 0) {
                        event.setCancelled(true);
                    }
                    if (this.debugLog.getValue()) {
                        ChatUtil.sendFormatted(
                                String.format(
                                        "%sExplosion (&otick: %d, x: %.2f, y: %.2f, z: %.2f&r)&r",
                                        Myau.clientName,
                                        mc.thePlayer.ticksExisted,
                                        mc.thePlayer.motionX + (double) packet.func_149149_c(),
                                        mc.thePlayer.motionY + (double) packet.func_149144_d(),
                                        mc.thePlayer.motionZ + (double) packet.func_149147_e()
                                )
                        );
                    }
                }
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.onDisabled();
    }

    @Override
    public void onDisabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
    }

    @Override
    public String[] getSuffix() {
        boolean predictionMode = this.mode.getValue() == 1 || this.mode.getValue() == 2;
        return predictionMode && this.horizontal.getValue() == 100 && this.vertical.getValue() == 100
                ? new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())}
                : new String[]{
                ChatColors.formatColor(String.format(this.mode.getValue() == 3 ? "&m%d%%&r" : "%d%%", this.horizontal.getValue())),
                String.format("%d%%", this.vertical.getValue())
        };
    }
}
