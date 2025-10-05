package myau.util;

import myau.Myau;
import myau.module.modules.move.KeepSprint;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.ForgeHooks;

public class PlayerUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isJumping() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
    }

    public static boolean isSneaking() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
    }

    public static boolean isMovingLeft() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
    }

    public static boolean isMovingRight() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
    }

    public static boolean isAttacking() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.gameSettings.keyBindAttack.getKeyCode());
    }

    public static boolean isUsingItem() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.gameSettings.keyBindUseItem.getKeyCode());
    }

    public static boolean canFly(float fallThreshold) {
        if (!mc.thePlayer.capabilities.allowFlying && !mc.thePlayer.capabilities.disableDamage) {
            PotionEffect jumpEffect = mc.thePlayer.getActivePotionEffect(Potion.jump);
            float jumpBoost = jumpEffect != null ? (float) (jumpEffect.getAmplifier() + 1) : 0.0F;
            float fallDistance = mc.thePlayer.fallDistance;
            if (mc.thePlayer.motionY < -0.67 || !isAirBelow()) {
                fallDistance -= (float) mc.thePlayer.motionY;
            }
            return MathHelper.ceiling_float_int(fallDistance - fallThreshold - jumpBoost) > 0;
        } else {
            return false;
        }
    }

    public static boolean canFly(int checkHeight) {
        if (!mc.thePlayer.capabilities.allowFlying && !mc.thePlayer.capabilities.disableDamage) {
            int playerY = MathHelper.floor_double(mc.thePlayer.posY);
            for (int offset = 0; offset <= checkHeight; ++offset) {
                int currentY = playerY - offset;
                if (currentY < 0) {
                    break;
                }
                Block block = mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, currentY, mc.thePlayer.posZ)).getBlock();
                if (!(block instanceof BlockAir)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean isInWater() {
        return checkInWater(mc.thePlayer.getEntityBoundingBox().expand(-1.0E-6, 0.0, -1.0E-6));
    }

    public static boolean checkInWater(AxisAlignedBB boundingBox) {
        if (!mc.thePlayer.isInWater() && !mc.thePlayer.isInLava()) {
            int minY = MathHelper.floor_double(boundingBox.minY);
            if (minY < 0) {
                return true;
            } else {
                int minX = MathHelper.floor_double(boundingBox.minX);
                int maxX = MathHelper.floor_double(boundingBox.maxX + 1.0);
                int minZ = MathHelper.floor_double(boundingBox.minZ);
                int maxZ = MathHelper.floor_double(boundingBox.maxZ + 1.0);
                for (int x = minX; x < maxX; ++x) {
                    for (int z = minZ; z < maxZ; ++z) {
                        for (int y = minY; y >= 0; --y) {
                            if (!BlockUtil.isReplaceable(new BlockPos(x, y, z))) {
                                return false;
                            }
                        }
                    }
                }
                return true;
            }
        } else {
            return false;
        }
    }

    public static boolean canMove(double x, double z) {
        return PlayerUtil.canMove(x, z, -1.0);
    }

    public static boolean canMove(double x, double z, double y) {
        AxisAlignedBB boundingBox = PlayerUtil.mc.thePlayer.getEntityBoundingBox().offset(x, y, z);
        return PlayerUtil.mc.theWorld.getCollidingBoundingBoxes(PlayerUtil.mc.thePlayer, boundingBox).isEmpty();
    }

    public static boolean isAirBelow() {
        AxisAlignedBB axisAlignedBB = PlayerUtil.mc.thePlayer.getEntityBoundingBox().offset(0.0, -1.0, 0.0);
        return !PlayerUtil.mc.theWorld.getCollidingBoundingBoxes(PlayerUtil.mc.thePlayer, axisAlignedBB).isEmpty();
    }

    public static boolean isAirAbove() {
        AxisAlignedBB axisAlignedBB = PlayerUtil.mc.thePlayer.getEntityBoundingBox().offset(0.0, 1.0, 0.0);
        return !PlayerUtil.mc.theWorld.getCollidingBoundingBoxes(PlayerUtil.mc.thePlayer, axisAlignedBB).isEmpty();
    }

    public static boolean canReach(BlockPos blockPos, double reach) {
        return PlayerUtil.isBlockWithinReach(blockPos, PlayerUtil.mc.thePlayer.posX, PlayerUtil.mc.thePlayer.posY + (double) PlayerUtil.mc.thePlayer.getEyeHeight(), PlayerUtil.mc.thePlayer.posZ, reach);
    }

    public static boolean isBlockWithinReach(BlockPos blockPos, double x, double y, double z, double reach) {
        return blockPos.distanceSqToCenter(x, y, z) < Math.pow(reach, 2.0);
    }

    public static void attackEntity(Entity target) {
        if (ForgeHooks.onPlayerAttackTarget(mc.thePlayer, target)) {
            if (target.canAttackWithItem() && !target.hitByEntity(mc.thePlayer)) {
                float baseDamage = (float) mc.thePlayer.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
                float enchantmentBonus = EnchantmentHelper.getModifierForCreature(
                        mc.thePlayer.getHeldItem(),
                        target instanceof EntityLivingBase ? ((EntityLivingBase) target).getCreatureAttribute() : EnumCreatureAttribute.UNDEFINED
                );
                int knockbackLevel = EnchantmentHelper.getKnockbackModifier(mc.thePlayer);
                if (mc.thePlayer.isSprinting()) {
                    ++knockbackLevel;
                }
                if (baseDamage > 0.0F || enchantmentBonus > 0.0F) {
                    boolean isCritical = mc.thePlayer.fallDistance > 0.0F
                            && !mc.thePlayer.onGround
                            && !mc.thePlayer.isOnLadder()
                            && !mc.thePlayer.isInWater()
                            && !mc.thePlayer.isPotionActive(Potion.blindness)
                            && mc.thePlayer.ridingEntity == null;
                    if (isCritical && baseDamage > 0.0F) {
                        baseDamage *= 1.5F;
                    }
                    baseDamage += enchantmentBonus;
                    boolean isFireAspectApplied = false;
                    int fireAspectLevel = EnchantmentHelper.getFireAspectModifier(mc.thePlayer);
                    if (target instanceof EntityLivingBase && fireAspectLevel > 0 && !target.isBurning()) {
                        isFireAspectApplied = true;
                        target.setFire(1);
                    }
                    double originalMotionX = target.motionX;
                    double originalMotionY = target.motionY;
                    double originalMotionZ = target.motionZ;
                    if (target.attackEntityFrom(DamageSource.causePlayerDamage(mc.thePlayer), baseDamage)) {
                        if (knockbackLevel > 0) {
                            target.addVelocity(
                                    -MathHelper.sin(mc.thePlayer.rotationYaw * (float) Math.PI / 180.0F) * (float) knockbackLevel * 0.5F,
                                    0.1,
                                    MathHelper.cos(mc.thePlayer.rotationYaw * (float) Math.PI / 180.0F) * (float) knockbackLevel * 0.5F
                            );
                            KeepSprint keepSprint = (KeepSprint) Myau.moduleManager.modules.get(KeepSprint.class);
                            if (keepSprint.isEnabled()
                                    && (!keepSprint.groundOnly.getValue() || mc.thePlayer.onGround)
                                    && (!keepSprint.reachOnly.getValue() || !(RotationUtil.distanceToEntity(target) <= 3.0))) {
                                mc.thePlayer.motionX *= 0.6 + 0.4 * (1.0 - keepSprint.slowdown.getValue().doubleValue() / 100.0);
                                mc.thePlayer.motionZ *= 0.6 + 0.4 * (1.0 - keepSprint.slowdown.getValue().doubleValue() / 100.0);
                            } else {
                                mc.thePlayer.motionX *= 0.6;
                                mc.thePlayer.motionZ *= 0.6;
                                mc.thePlayer.setSprinting(false);
                            }
                        }
                        if (target instanceof EntityPlayerMP && target.velocityChanged) {
                            ((EntityPlayerMP) target).playerNetServerHandler.sendPacket(new S12PacketEntityVelocity(target));
                            target.velocityChanged = false;
                            target.motionX = originalMotionX;
                            target.motionY = originalMotionY;
                            target.motionZ = originalMotionZ;
                        }
                        if (isCritical) {
                            mc.thePlayer.onCriticalHit(target);
                        }
                        if (enchantmentBonus > 0.0F) {
                            mc.thePlayer.onEnchantmentCritical(target);
                        }
                        if (baseDamage >= 18.0F) {
                            mc.thePlayer.triggerAchievement(AchievementList.overkill);
                        }
                        mc.thePlayer.setLastAttacker(target);
                        if (target instanceof EntityLivingBase) {
                            EnchantmentHelper.applyThornEnchantments((EntityLivingBase) target, mc.thePlayer);
                        }
                        EnchantmentHelper.applyArthropodEnchantments(mc.thePlayer, target);
                        if (target instanceof EntityLivingBase) {
                            mc.thePlayer.addStat(StatList.damageDealtStat, Math.round(baseDamage * 10.0F));
                            if (fireAspectLevel > 0) {
                                target.setFire(fireAspectLevel * 4);
                            }
                        }
                        mc.thePlayer.addExhaustion(0.3F);
                    } else if (isFireAspectApplied) {
                        target.extinguish();
                    }
                }
            }
        }
    }
}
