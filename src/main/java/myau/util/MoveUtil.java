package myau.util;

import myau.Myau;
import myau.management.RotationState;
import myau.module.modules.combat.TargetStrafe;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class MoveUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isForwardPressed() {
        if (MoveUtil.mc.gameSettings.keyBindForward.isKeyDown() != MoveUtil.mc.gameSettings.keyBindBack.isKeyDown())
            return true;
        return MoveUtil.mc.gameSettings.keyBindLeft.isKeyDown() != MoveUtil.mc.gameSettings.keyBindRight.isKeyDown();
    }

    public static int getForwardValue() {
        int forwardValue = 0;
        if (MoveUtil.mc.gameSettings.keyBindForward.isKeyDown()) {
            ++forwardValue;
        }
        if (MoveUtil.mc.gameSettings.keyBindBack.isKeyDown()) {
            --forwardValue;
        }
        return forwardValue;
    }

    public static int getLeftValue() {
        int leftValue = 0;
        if (MoveUtil.mc.gameSettings.keyBindLeft.isKeyDown()) {
            ++leftValue;
        }
        if (MoveUtil.mc.gameSettings.keyBindRight.isKeyDown()) {
            --leftValue;
        }
        return leftValue;
    }

    public static float getMoveYaw() {
        return MoveUtil.adjustYaw(RotationState.isActived() ? RotationState.getSmoothedYaw() : MoveUtil.mc.thePlayer.rotationYaw, MoveUtil.mc.thePlayer.movementInput.moveForward, MoveUtil.mc.thePlayer.movementInput.moveStrafe);
    }

    public static float adjustYaw(float yaw, float forward, float strafe) {
        TargetStrafe targetStrafe = (TargetStrafe) Myau.moduleManager.modules.get(TargetStrafe.class);
        if (targetStrafe.isEnabled()) {
            if (!Float.isNaN(targetStrafe.getTargetYaw())) {
                return targetStrafe.getTargetYaw();
            }
        }
        if (forward < 0.0f) {
            yaw += 180.0f;
        }
        if (strafe != 0.0f) {
            float multiplier = forward == 0.0f ? 1.0f : 0.5f * Math.signum(forward);
            yaw += -90.0f * multiplier * Math.signum(strafe);
        }
        return MathHelper.wrapAngleTo180_float(yaw);
    }

    public static float getDirectionYaw() {
        if (MoveUtil.getSpeed() == 0.0) {
            return MathHelper.wrapAngleTo180_float(MoveUtil.mc.thePlayer.rotationYaw);
        }
        return MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(MoveUtil.mc.thePlayer.motionZ, MoveUtil.mc.thePlayer.motionX)) - 90.0f);
    }

    public static double getBaseMoveSpeed() {
        double baseSpeed = 0.28015;
        if (MoveUtil.getSpeedTime() > 0) {
            baseSpeed = 0.28015 * (1.0 + 0.15 * (double) MoveUtil.getSpeedLevel());
        }
        return baseSpeed;
    }

    public static double getBaseJumpHigh(int speedLevel) {
        double jumpHeight = 0.452;
        if (speedLevel == 1) {
            jumpHeight = 0.49720000000000003;
        } else if (speedLevel >= 2) {
            jumpHeight *= 1.2;
        }
        return jumpHeight;
    }

    public static double getJumpMotion() {
        int speedLevel = 0;
        if (MoveUtil.getSpeedTime() > 0) {
            speedLevel = MoveUtil.getSpeedLevel();
        }
        return MoveUtil.getBaseJumpHigh(speedLevel);
    }

    public static double getSpeed() {
        return MoveUtil.getSpeed(MoveUtil.mc.thePlayer.motionX, MoveUtil.mc.thePlayer.motionZ);
    }

    public static double getSpeed(double motionX, double motionZ) {
        return Math.hypot(motionX, motionZ);
    }

    public static void setSpeed(double speed) {
        MoveUtil.setSpeed(speed, MoveUtil.getDirectionYaw());
    }

    public static void setSpeed(double speed, float yaw) {
        MoveUtil.mc.thePlayer.motionX = -Math.sin(Math.toRadians(yaw)) * speed;
        MoveUtil.mc.thePlayer.motionZ = Math.cos(Math.toRadians(yaw)) * speed;
    }

    public static void addSpeed(double speed, float yaw) {
        MoveUtil.mc.thePlayer.motionX += -Math.sin(Math.toRadians(yaw)) * speed;
        MoveUtil.mc.thePlayer.motionZ += Math.cos(Math.toRadians(yaw)) * speed;
    }

    public static int getSpeedLevel() {
        int speedLevel = 0;
        if (MoveUtil.mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            speedLevel = (MoveUtil.mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
        }
        return speedLevel;
    }

    public static int getSpeedTime() {
        if (MoveUtil.mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            return MoveUtil.mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getDuration();
        }
        return 0;
    }

    public static float getAllowedHorizontalDistance() {
        float slipperiness = MoveUtil.mc.thePlayer.worldObj.getBlockState(new BlockPos(MathHelper.floor_double(MoveUtil.mc.thePlayer.posX), MathHelper.floor_double(MoveUtil.mc.thePlayer.getEntityBoundingBox().minY) - 1, MathHelper.floor_double(MoveUtil.mc.thePlayer.posZ))).getBlock().slipperiness * 0.91f;
        return MoveUtil.mc.thePlayer.getAIMoveSpeed() * (0.16277136f / (slipperiness * slipperiness * slipperiness));
    }

    public static double[] predictMovement() {
        float strafeInput = (float) MoveUtil.getLeftValue() * 0.98f;
        float forwardInput = (float) MoveUtil.getForwardValue() * 0.98f;
        float inputMagnitude = strafeInput * strafeInput + forwardInput * forwardInput;
        if (inputMagnitude >= 1.0E-4f) {
            inputMagnitude = MathHelper.sqrt_float(inputMagnitude);
            if (inputMagnitude < 1.0f) {
                inputMagnitude = 1.0f;
            }
            inputMagnitude = MoveUtil.getAllowedHorizontalDistance() / inputMagnitude;
            float sinYaw = MathHelper.sin(MoveUtil.mc.thePlayer.rotationYaw * (float) Math.PI / 180.0f);
            float cosYaw = MathHelper.cos(MoveUtil.mc.thePlayer.rotationYaw * (float) Math.PI / 180.0f);
            strafeInput *= inputMagnitude;
            forwardInput *= inputMagnitude;
            return new double[]{strafeInput * cosYaw - forwardInput * sinYaw, forwardInput * cosYaw + strafeInput * sinYaw};
        }
        return new double[]{0.0, 0.0};
    }

    public static void fixStrafe(float targetYaw) {
        float angle = MathHelper.wrapAngleTo180_float(MoveUtil.adjustYaw(MoveUtil.mc.thePlayer.rotationYaw, MoveUtil.getForwardValue(), MoveUtil.getLeftValue()) - targetYaw + 22.5f);
        switch ((int) (angle + 180.0f) / 45 % 8) {
            case 0: {
                MoveUtil.mc.thePlayer.movementInput.moveForward = -1.0f;
                MoveUtil.mc.thePlayer.movementInput.moveStrafe = 0.0f;
                break;
            }
            case 1: {
                MoveUtil.mc.thePlayer.movementInput.moveForward = -1.0f;
                MoveUtil.mc.thePlayer.movementInput.moveStrafe = 1.0f;
                break;
            }
            case 2: {
                MoveUtil.mc.thePlayer.movementInput.moveForward = 0.0f;
                MoveUtil.mc.thePlayer.movementInput.moveStrafe = 1.0f;
                break;
            }
            case 3: {
                MoveUtil.mc.thePlayer.movementInput.moveForward = 1.0f;
                MoveUtil.mc.thePlayer.movementInput.moveStrafe = 1.0f;
                break;
            }
            case 4: {
                MoveUtil.mc.thePlayer.movementInput.moveForward = 1.0f;
                MoveUtil.mc.thePlayer.movementInput.moveStrafe = 0.0f;
                break;
            }
            case 5: {
                MoveUtil.mc.thePlayer.movementInput.moveForward = 1.0f;
                MoveUtil.mc.thePlayer.movementInput.moveStrafe = -1.0f;
                break;
            }
            case 6: {
                MoveUtil.mc.thePlayer.movementInput.moveForward = 0.0f;
                MoveUtil.mc.thePlayer.movementInput.moveStrafe = -1.0f;
                break;
            }
            case 7: {
                MoveUtil.mc.thePlayer.movementInput.moveForward = -1.0f;
                MoveUtil.mc.thePlayer.movementInput.moveStrafe = -1.0f;
                break;
            }
        }
        if (MoveUtil.mc.thePlayer.movementInput.sneak) {
            MoveUtil.mc.thePlayer.movementInput.moveForward *= 0.3f;
            MoveUtil.mc.thePlayer.movementInput.moveStrafe *= 0.3f;
        }
    }
}
