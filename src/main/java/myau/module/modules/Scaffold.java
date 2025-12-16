package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.*;
import myau.management.RotationState;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

public class Scaffold extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final double[] placeOffsets = new double[]{
            0.03125,
            0.09375,
            0.15625,
            0.21875,
            0.28125,
            0.34375,
            0.40625,
            0.46875,
            0.53125,
            0.59375,
            0.65625,
            0.71875,
            0.78125,
            0.84375,
            0.90625,
            0.96875
    };
    private int rotationTick = 0;
    private int lastSlot = -1;
    private int blockCount = -1;
    private float yaw = -180.0F;
    private float pitch = 0.0F;
    private boolean canRotate = false;
    private int towerTick = 0;
    private int towerDelay = 0;
    private int stage = 0;
    private int startY = 256;
    private boolean shouldKeepY = false;
    private boolean towering = false;
    private EnumFacing targetFacing = null;
    public final ModeProperty rotationMode = new ModeProperty("rotations", 2, new String[]{"NONE", "DEFAULT", "BACKWARDS", "SIDEWAYS"});
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"NONE", "SILENT"});
    public final ModeProperty sprintMode = new ModeProperty("sprint", 0, new String[]{"NONE", "VANILLA"});
    public final PercentProperty groundMotion = new PercentProperty("ground-motion", 100);
    public final PercentProperty airMotion = new PercentProperty("air-motion", 100);
    public final PercentProperty speedMotion = new PercentProperty("speed-motion", 100);
    public final ModeProperty tower = new ModeProperty("tower", 0, new String[]{"NONE", "VANILLA", "EXTRA", "TELLY"});
    public final ModeProperty keepY = new ModeProperty("keep-y", 0, new String[]{"NONE", "VANILLA", "EXTRA", "TELLY"});
    public final BooleanProperty keepYonPress = new BooleanProperty("keep-y-on-press", false, () -> this.keepY.getValue() != 0);
    public final BooleanProperty disableWhileJumpActive = new BooleanProperty("no-keep-y-on-jump-potion", false, () -> this.keepY.getValue() != 0);
    public final BooleanProperty multiplace = new BooleanProperty("multi-place", true);
    public final BooleanProperty safeWalk = new BooleanProperty("safe-walk", true);
    public final BooleanProperty swing = new BooleanProperty("swing", true);
    public final BooleanProperty itemSpoof = new BooleanProperty("item-spoof", false);
    public final BooleanProperty blockCounter = new BooleanProperty("block-counter", true);

    private boolean shouldStopSprint() {
        if (this.isTowering()) {
            return false;
        } else {
            boolean stage = this.keepY.getValue() == 1 || this.keepY.getValue() == 2;
            return (!stage || this.stage <= 0) && this.sprintMode.getValue() == 0;
        }
    }

    private boolean canPlace() {
        BedNuker bedNuker = (BedNuker) Myau.moduleManager.modules.get(BedNuker.class);
        if (bedNuker.isEnabled() && bedNuker.isReady()) {
            return false;
        } else {
            LongJump longJump = (LongJump) Myau.moduleManager.modules.get(LongJump.class);
            return !longJump.isEnabled() || !longJump.isAutoMode() || longJump.isJumping();
        }
    }

    private EnumFacing getBestFacing(BlockPos blockPos1, BlockPos blockPos3) {
        double offset = 0.0;
        EnumFacing enumFacing = null;
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing != EnumFacing.DOWN) {
                BlockPos pos = blockPos1.offset(facing);
                if (pos.getY() <= blockPos3.getY()) {
                    double distance = pos.distanceSqToCenter((double) blockPos3.getX() + 0.5, (double) blockPos3.getY() + 0.5, (double) blockPos3.getZ() + 0.5);
                    if (enumFacing == null || distance < offset || distance == offset && facing == EnumFacing.UP) {
                        offset = distance;
                        enumFacing = facing;
                    }
                }
            }
        }
        return enumFacing;
    }

    private BlockData getBlockData() {
        int startY = MathHelper.floor_double(mc.thePlayer.posY);
        BlockPos targetPos = new BlockPos(
                MathHelper.floor_double(mc.thePlayer.posX),
                (this.stage != 0 && !this.shouldKeepY ? Math.min(startY, this.startY) : startY) - 1,
                MathHelper.floor_double(mc.thePlayer.posZ)
        );
        if (!BlockUtil.isReplaceable(targetPos)) {
            return null;
        } else {
            ArrayList<BlockPos> positions = new ArrayList<>();
            for (int x = -4; x <= 4; x++) {
                for (int y = -4; y <= 0; y++) {
                    for (int z = -4; z <= 4; z++) {
                        BlockPos pos = targetPos.add(x, y, z);
                        if (!BlockUtil.isReplaceable(pos)
                                && !BlockUtil.isInteractable(pos)
                                && !(
                                mc.thePlayer.getDistance((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5)
                                        > (double) mc.playerController.getBlockReachDistance()
                        )
                                && (this.stage == 0 || this.shouldKeepY || pos.getY() < this.startY)) {
                            for (EnumFacing facing : EnumFacing.VALUES) {
                                if (facing != EnumFacing.DOWN) {
                                    BlockPos blockPos = pos.offset(facing);
                                    if (BlockUtil.isReplaceable(blockPos)) {
                                        positions.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (positions.isEmpty()) {
                return null;
            } else {
                positions.sort(
                        Comparator.comparingDouble(
                                o -> o.distanceSqToCenter((double) targetPos.getX() + 0.5, (double) targetPos.getY() + 0.5, (double) targetPos.getZ() + 0.5)
                        )
                );
                BlockPos blockPos = positions.get(0);
                EnumFacing facing = this.getBestFacing(blockPos, targetPos);
                return facing == null ? null : new BlockData(blockPos, facing);
            }
        }
    }

    private void place(BlockPos blockPos, EnumFacing enumFacing, Vec3 vec3) {
        if (ItemUtil.isHoldingBlock() && this.blockCount > 0) {
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem(), blockPos, enumFacing, vec3)) {
                if (mc.playerController.getCurrentGameType() != GameType.CREATIVE) {
                    this.blockCount--;
                }
                if (this.swing.getValue()) {
                    mc.thePlayer.swingItem();
                } else {
                    PacketUtil.sendPacket(new C0APacketAnimation());
                }
            }
        }
    }

    private EnumFacing yawToFacing(float yaw) {
        if (yaw < -135.0F || yaw > 135.0F) {
            return EnumFacing.NORTH;
        } else if (yaw < -45.0F) {
            return EnumFacing.EAST;
        } else {
            return yaw < 45.0F ? EnumFacing.SOUTH : EnumFacing.WEST;
        }
    }

    private double distanceToEdge(EnumFacing enumFacing) {
        switch (enumFacing) {
            case NORTH:
                return mc.thePlayer.posZ - Math.floor(mc.thePlayer.posZ);
            case EAST:
                return Math.ceil(mc.thePlayer.posX) - mc.thePlayer.posX;
            case SOUTH:
                return Math.ceil(mc.thePlayer.posZ) - mc.thePlayer.posZ;
            case WEST:
            default:
                return mc.thePlayer.posX - Math.floor(mc.thePlayer.posX);
        }
    }

    private float getSpeed() {
        if (!mc.thePlayer.onGround) {
            return (float) this.airMotion.getValue() / 100.0F;
        } else {
            return MoveUtil.getSpeedLevel() > 0
                    ? (float) this.speedMotion.getValue() / 100.0F
                    : (float) this.groundMotion.getValue() / 100.0F;
        }
    }

    private double getRandomOffset() {
        return 0.2155 - RandomUtil.nextDouble(1.0E-4, 9.0E-4);
    }

    private float getCurrentYaw() {
        return MoveUtil.adjustYaw(
                mc.thePlayer.rotationYaw, (float) MoveUtil.getForwardValue(), (float) MoveUtil.getLeftValue()
        );
    }

    private boolean isDiagonal(float yaw) {
        float absYaw = Math.abs(yaw % 90.0F);
        return absYaw > 20.0F && absYaw < 70.0F;
    }

    private boolean isTowering() {
        if (mc.thePlayer.onGround && MoveUtil.isForwardPressed() && !PlayerUtil.isAirAbove()) {
            boolean keepY = this.keepY.getValue() == 3;
            boolean tower = this.tower.getValue() == 3;
            return keepY && this.stage > 0 || tower && mc.gameSettings.keyBindJump.isKeyDown();
        } else {
            return false;
        }
    }

    public Scaffold() {
        super("Scaffold", false);
    }

    public int getSlot() {
        return this.lastSlot;
    }

    @EventTarget(Priority.HIGH)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.rotationTick > 0) {
                this.rotationTick--;
            }
            if (mc.thePlayer.onGround) {
                if (this.stage > 0) {
                    this.stage--;
                }
                if (this.stage < 0) {
                    this.stage++;
                }
                if (this.stage == 0
                        && this.keepY.getValue() != 0
                        && (!(Boolean) this.keepYonPress.getValue() || PlayerUtil.isUsingItem())
                        && (!this.disableWhileJumpActive.getValue() || !mc.thePlayer.isPotionActive(Potion.jump))
                        && !mc.gameSettings.keyBindJump.isKeyDown()) {
                    this.stage = 1;
                }
                this.startY = this.shouldKeepY ? this.startY : MathHelper.floor_double(mc.thePlayer.posY);
                this.shouldKeepY = false;
                this.towering = false;
            }
            if (this.canPlace()) {
                ItemStack stack = mc.thePlayer.getHeldItem();
                int count = ItemUtil.isBlock(stack) ? stack.stackSize : 0;
                this.blockCount = Math.min(this.blockCount, count);
                if (this.blockCount <= 0) {
                    int slot = mc.thePlayer.inventory.currentItem;
                    if (this.blockCount == 0) {
                        slot--;
                    }
                    for (int i = slot; i > slot - 9; i--) {
                        int hotbarSlot = (i % 9 + 9) % 9;
                        ItemStack candidate = mc.thePlayer.inventory.getStackInSlot(hotbarSlot);
                        if (ItemUtil.isBlock(candidate)) {
                            mc.thePlayer.inventory.currentItem = hotbarSlot;
                            this.blockCount = candidate.stackSize;
                            break;
                        }
                    }
                }
                float currentYaw = this.getCurrentYaw();
                float yawDiffTo180 = RotationUtil.wrapAngleDiff(currentYaw - 180.0F, event.getYaw());
                float diagonalYaw = this.isDiagonal(currentYaw)
                        ? yawDiffTo180
                        : RotationUtil.wrapAngleDiff(currentYaw - 135.0F * ((currentYaw + 180.0F) % 90.0F < 45.0F ? 1.0F : -1.0F), event.getYaw());
                if (!this.canRotate) {
                    switch (this.rotationMode.getValue()) {
                        case 1:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                            } else {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                            }
                            break;
                        case 2:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                            } else {
                                this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                            }
                            break;
                        case 3:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                            } else {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                            }
                    }
                }
                BlockData blockData = this.getBlockData();
                Vec3 hitVec = null;
                if (blockData != null) {
                    double[] x = placeOffsets;
                    double[] y = placeOffsets;
                    double[] z = placeOffsets;
                    switch (blockData.facing()) {
                        case NORTH:
                            z = new double[]{0.0};
                            break;
                        case EAST:
                            x = new double[]{1.0};
                            break;
                        case SOUTH:
                            z = new double[]{1.0};
                            break;
                        case WEST:
                            x = new double[]{0.0};
                            break;
                        case DOWN:
                            y = new double[]{0.0};
                            break;
                        case UP:
                            y = new double[]{1.0};
                    }
                    float bestYaw = -180.0F;
                    float bestPitch = 0.0F;
                    float bestDiff = 0.0F;
                    for (double dx : x) {
                        for (double dy : y) {
                            for (double dz : z) {
                                double relX = (double) blockData.blockPos().getX() + dx - mc.thePlayer.posX;
                                double relY = (double) blockData.blockPos().getY() + dy - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
                                double relZ = (double) blockData.blockPos().getZ() + dz - mc.thePlayer.posZ;
                                float baseYaw = RotationUtil.wrapAngleDiff(this.yaw, event.getYaw());
                                float[] rotations = RotationUtil.getRotationsTo(relX, relY, relZ, baseYaw, this.pitch);
                                MovingObjectPosition mop = RotationUtil.rayTrace(rotations[0], rotations[1], mc.playerController.getBlockReachDistance(), 1.0F);
                                if (mop != null
                                        && mop.typeOfHit == MovingObjectType.BLOCK
                                        && mop.getBlockPos().equals(blockData.blockPos())
                                        && mop.sideHit == blockData.facing()) {
                                    float totalDiff = Math.abs(rotations[0] - baseYaw) + Math.abs(rotations[1] - this.pitch);
                                    if (bestYaw == -180.0F && bestPitch == 0.0F || totalDiff < bestDiff) {
                                        bestYaw = rotations[0];
                                        bestPitch = rotations[1];
                                        bestDiff = totalDiff;
                                        hitVec = mop.hitVec;
                                    }
                                }
                            }
                        }
                    }
                    if (bestYaw != -180.0F || bestPitch != 0.0F) {
                        this.yaw = bestYaw;
                        this.pitch = bestPitch;
                        this.canRotate = true;
                    }
                }
                if (this.canRotate && MoveUtil.isForwardPressed() && Math.abs(MathHelper.wrapAngleTo180_float(yawDiffTo180 - this.yaw)) < 90.0F) {
                    switch (this.rotationMode.getValue()) {
                        case 2:
                            this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                            break;
                        case 3:
                            this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                    }
                }
                if (this.rotationMode.getValue() != 0) {
                    float targetYaw = this.yaw;
                    float targetPitch = this.pitch;
                    if (this.towering && (mc.thePlayer.motionY > 0.0 || mc.thePlayer.posY > (double) (this.startY + 1))) {
                        float yawDiff = MathHelper.wrapAngleTo180_float(this.yaw - event.getYaw());
                        float tolerance = this.rotationTick >= 2 ? RandomUtil.nextFloat(90.0F, 95.0F) : RandomUtil.nextFloat(30.0F, 35.0F);
                        if (Math.abs(yawDiff) > tolerance) {
                            float clampedYaw = RotationUtil.clampAngle(yawDiff, tolerance);
                            targetYaw = RotationUtil.quantizeAngle(event.getYaw() + clampedYaw);
                            this.rotationTick = Math.max(this.rotationTick, 1);
                        }
                    }
                    if (this.isTowering()) {
                        float yawDelta = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - event.getYaw());
                        targetYaw = RotationUtil.quantizeAngle(event.getYaw() + yawDelta * RandomUtil.nextFloat(0.98F, 0.99F));
                        targetPitch = RotationUtil.quantizeAngle(RandomUtil.nextFloat(30.0F, 80.0F));
                        this.rotationTick = 3;
                        this.towering = true;
                    }
                    event.setRotation(targetYaw, targetPitch, 3);
                    if (this.moveFix.getValue() == 1) {
                        event.setPervRotation(targetYaw, 3);
                    }
                }
                if (blockData != null && hitVec != null && this.rotationTick <= 0) {
                    this.place(blockData.blockPos(), blockData.facing(), hitVec);
                    if (this.multiplace.getValue()) {
                        for (int i = 0; i < 3; i++) {
                            blockData = this.getBlockData();
                            if (blockData == null) {
                                break;
                            }
                            MovingObjectPosition mop = RotationUtil.rayTrace(this.yaw, this.pitch, mc.playerController.getBlockReachDistance(), 1.0F);
                            if (mop != null
                                    && mop.typeOfHit == MovingObjectType.BLOCK
                                    && mop.getBlockPos().equals(blockData.blockPos())
                                    && mop.sideHit == blockData.facing()) {
                                this.place(blockData.blockPos(), blockData.facing(), mop.hitVec);
                            } else {
                                hitVec = BlockUtil.getClickVec(blockData.blockPos(), blockData.facing());
                                double dx = hitVec.xCoord - mc.thePlayer.posX;
                                double dy = hitVec.yCoord - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
                                double dz = hitVec.zCoord - mc.thePlayer.posZ;
                                float[] rotations = RotationUtil.getRotationsTo(dx, dy, dz, event.getYaw(), event.getPitch());
                                if (!(Math.abs(rotations[0] - this.yaw) < 120.0F) || !(Math.abs(rotations[1] - this.pitch) < 60.0F)) {
                                    break;
                                }
                                mop = RotationUtil.rayTrace(rotations[0], rotations[1], mc.playerController.getBlockReachDistance(), 1.0F);
                                if (mop == null
                                        || mop.typeOfHit != MovingObjectType.BLOCK
                                        || !mop.getBlockPos().equals(blockData.blockPos())
                                        || mop.sideHit != blockData.facing()) {
                                    break;
                                }
                                this.place(blockData.blockPos(), blockData.facing(), mop.hitVec);
                            }
                        }
                    }
                }
                if (this.targetFacing != null) {
                    if (this.rotationTick <= 0) {
                        int playerBlockX = MathHelper.floor_double(mc.thePlayer.posX);
                        int playerBlockY = MathHelper.floor_double(mc.thePlayer.posY);
                        int playerBlockZ = MathHelper.floor_double(mc.thePlayer.posZ);
                        BlockPos belowPlayer = new BlockPos(playerBlockX, playerBlockY - 1, playerBlockZ);
                        hitVec = BlockUtil.getHitVec(belowPlayer, this.targetFacing, this.yaw, this.pitch);
                        this.place(belowPlayer, this.targetFacing, hitVec);
                    }
                    this.targetFacing = null;
                } else if (this.keepY.getValue() == 2 && this.stage > 0 && !mc.thePlayer.onGround) {
                    int nextBlockY = MathHelper.floor_double(mc.thePlayer.posY + mc.thePlayer.motionY);
                    if (nextBlockY <= this.startY && mc.thePlayer.posY > (double) (this.startY + 1)) {
                        this.shouldKeepY = true;
                        blockData = this.getBlockData();
                        if (blockData != null && this.rotationTick <= 0) {
                            hitVec = BlockUtil.getHitVec(blockData.blockPos(), blockData.facing(), this.yaw, this.pitch);
                            this.place(blockData.blockPos(), blockData.facing(), hitVec);
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled()) {
            if (!mc.thePlayer.isCollidedHorizontally
                    && mc.thePlayer.hurtTime <= 5
                    && !mc.thePlayer.isPotionActive(Potion.jump)
                    && mc.gameSettings.keyBindJump.isKeyDown()
                    && ItemUtil.isHoldingBlock()) {
                int yState = (int) (mc.thePlayer.posY % 1.0 * 100.0);
                switch (this.tower.getValue()) {
                    case 1:
                        switch (this.towerTick) {
                            case 0:
                                if (mc.thePlayer.onGround) {
                                    this.towerTick = 1;
                                    mc.thePlayer.motionY = -0.0784000015258789;
                                }
                                return;
                            case 1:
                                if (yState == 0 && PlayerUtil.isAirBelow()) {
                                    this.startY = MathHelper.floor_double(mc.thePlayer.posY);
                                    this.towerTick = 2;
                                    mc.thePlayer.motionY = 0.42F;
                                    if (MoveUtil.isForwardPressed()) {
                                        MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                                    } else {
                                        MoveUtil.setSpeed(0.0);
                                        event.setForward(0.0F);
                                        event.setStrafe(0.0F);
                                    }
                                    return;
                                } else {
                                    this.towerTick = 0;
                                    return;
                                }
                            case 2:
                                this.towerTick = 3;
                                mc.thePlayer.motionY = 0.75 - mc.thePlayer.posY % 1.0;
                                return;
                            case 3:
                                this.towerTick = 1;
                                mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                                return;
                            default:
                                this.towerTick = 0;
                                return;
                        }
                    case 2:
                        switch (this.towerTick) {
                            case 0:
                                if (mc.thePlayer.onGround) {
                                    this.towerTick = 1;
                                    mc.thePlayer.motionY = -0.0784000015258789;
                                }
                                return;
                            case 1:
                                if (yState == 0 && PlayerUtil.isAirBelow()) {
                                    this.startY = MathHelper.floor_double(mc.thePlayer.posY);
                                    if (!MoveUtil.isForwardPressed()) {
                                        this.towerDelay = 2;
                                        MoveUtil.setSpeed(0.0);
                                        event.setForward(0.0F);
                                        event.setStrafe(0.0F);
                                        EnumFacing facing = this.yawToFacing(MathHelper.wrapAngleTo180_float(this.yaw - 180.0F));
                                        double distance = this.distanceToEdge(facing);
                                        if (distance > 0.1) {
                                            if (mc.thePlayer.onGround) {
                                                Vec3i directionVec = facing.getDirectionVec();
                                                double offset = Math.min(this.getRandomOffset(), distance - 0.05);
                                                double jitter = RandomUtil.nextDouble(0.02, 0.03);
                                                AxisAlignedBB nextBox = mc.thePlayer
                                                        .getEntityBoundingBox()
                                                        .offset((double) directionVec.getX() * (offset - jitter), 0.0, (double) directionVec.getZ() * (offset - jitter));
                                                if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, nextBox).isEmpty()) {
                                                    mc.thePlayer.motionY = -0.0784000015258789;
                                                    mc.thePlayer
                                                            .setPosition(nextBox.minX + (nextBox.maxX - nextBox.minX) / 2.0, nextBox.minY, nextBox.minZ + (nextBox.maxZ - nextBox.minZ) / 2.0);
                                                }
                                                return;
                                            }
                                        } else {
                                            this.towerTick = 2;
                                            this.targetFacing = facing;
                                            mc.thePlayer.motionY = 0.42F;
                                        }
                                        return;
                                    } else {
                                        this.towerTick = 2;
                                        this.towerDelay++;
                                        mc.thePlayer.motionY = 0.42F;
                                        MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                                        return;
                                    }
                                } else {
                                    this.towerTick = 0;
                                    this.towerDelay = 0;
                                    return;
                                }
                            case 2:
                                this.towerTick = 3;
                                mc.thePlayer.motionY = mc.thePlayer.motionY - RandomUtil.nextDouble(0.00101, 0.00109);
                                return;
                            case 3:
                                if (this.towerDelay >= 4) {
                                    this.towerTick = 4;
                                    this.towerDelay = 0;
                                } else {
                                    this.towerTick = 1;
                                    mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                                }
                                return;
                            case 4:
                                this.towerTick = 5;
                                return;
                            case 5:
                                if (!PlayerUtil.isAirBelow()) {
                                    this.towerTick = 0;
                                } else {
                                    this.towerTick = 1;
                                    mc.thePlayer.motionY -= 0.08;
                                    mc.thePlayer.motionY *= 0.98F;
                                    mc.thePlayer.motionY -= 0.08;
                                    mc.thePlayer.motionY *= 0.98F;
                                }
                                return;
                            default:
                                this.towerTick = 0;
                                this.towerDelay = 0;
                                return;
                        }
                    default:
                        this.towerTick = 0;
                        this.towerDelay = 0;
                }
            } else {
                this.towerTick = 0;
                this.towerDelay = 0;
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            if (this.moveFix.getValue() == 1
                    && RotationState.isActived()
                    && RotationState.getPriority() == 3.0F
                    && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
            if (mc.thePlayer.onGround && this.stage > 0 && MoveUtil.isForwardPressed()) {
                mc.thePlayer.movementInput.jump = true;
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled()) {
            float speed = this.getSpeed();
            if (speed != 1.0F) {
                if (mc.thePlayer.movementInput.moveForward != 0.0F && mc.thePlayer.movementInput.moveStrafe != 0.0F) {
                    mc.thePlayer.movementInput.moveForward = mc.thePlayer.movementInput.moveForward * (1.0F / (float) Math.sqrt(2.0));
                    mc.thePlayer.movementInput.moveStrafe = mc.thePlayer.movementInput.moveStrafe * (1.0F / (float) Math.sqrt(2.0));
                }
                mc.thePlayer.movementInput.moveForward *= speed;
                mc.thePlayer.movementInput.moveStrafe *= speed;
            }
            if (this.shouldStopSprint()) {
                mc.thePlayer.setSprinting(false);
            }
        }
    }

    @EventTarget
    public void onSafeWalk(SafeWalkEvent event) {
        if (this.isEnabled() && this.safeWalk.getValue()) {
            if (mc.thePlayer.onGround && mc.thePlayer.motionY <= 0.0 && PlayerUtil.canMove(mc.thePlayer.motionX, mc.thePlayer.motionZ, -1.0)) {
                event.setSafeWalk(true);
            }
        }
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled()) {
            if (this.blockCounter.getValue()) {
                int count = 0;
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                    if (stack != null && stack.stackSize > 0) {
                        Item item = stack.getItem();
                        if (item instanceof ItemBlock) {
                            Block block = ((ItemBlock) item).getBlock();
                            if (!BlockUtil.isInteractable(block) && BlockUtil.isSolid(block)) {
                                count += stack.stackSize;
                            }
                        }
                    }
                }
                HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
                float scale = hud.scale.getValue();
                GlStateManager.pushMatrix();
                GlStateManager.scale(scale, scale, 0.0F);
                GlStateManager.disableDepth();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                mc.fontRendererObj
                        .drawString(
                                String.format("%d block%s left", count, count != 1 ? "s" : ""),
                                ((float) new ScaledResolution(mc).getScaledWidth() / 2.0F + (float) mc.fontRendererObj.FONT_HEIGHT * 1.5F) / scale,
                                (float) new ScaledResolution(mc).getScaledHeight() / 2.0F / scale - (float) mc.fontRendererObj.FONT_HEIGHT / 2.0F + 1.0F,
                                (count > 0 ? Color.WHITE.getRGB() : new Color(255, 85, 85).getRGB()) | -1090519040,
                                hud.shadow.getValue()
                        );
                GlStateManager.disableBlend();
                GlStateManager.enableDepth();
                GlStateManager.popMatrix();
            }
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (this.isEnabled()) {
            this.lastSlot = event.setSlot(this.lastSlot);
            event.setCancelled(true);
        }
    }

    @Override
    public void onEnabled() {
        if (mc.thePlayer != null) {
            this.lastSlot = mc.thePlayer.inventory.currentItem;
        } else {
            this.lastSlot = -1;
        }
        this.blockCount = -1;
        this.rotationTick = 3;
        this.yaw = -180.0F;
        this.pitch = 0.0F;
        this.canRotate = false;
        this.towerTick = 0;
        this.towerDelay = 0;
        this.towering = false;
    }

    @Override
    public void onDisabled() {
        if (mc.thePlayer != null && this.lastSlot != -1) {
            mc.thePlayer.inventory.currentItem = this.lastSlot;
        }
    }

    public static class BlockData {
        private final BlockPos blockPos;
        private final EnumFacing facing;

        public BlockData(BlockPos blockPos, EnumFacing enumFacing) {
            this.blockPos = blockPos;
            this.facing = enumFacing;
        }

        public BlockPos blockPos() {
            return this.blockPos;
        }

        public EnumFacing facing() {
            return this.facing;
        }
    }
}
