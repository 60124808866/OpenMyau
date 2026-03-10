package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.*;
import myau.management.RotationState;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.MoveUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;

import java.util.*;

public class Clutch extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final Map<String, Integer> BLOCK_SCORE = new HashMap<>();

    private float serverYaw;
    private float serverPitch;
    private float aimYaw;
    private float aimPitch;
    private boolean hasAim;
    private boolean resetting;
    private BlockPos targetBlock;
    private EnumFacing targetFacing;
    private Vec3 targetHitVec;
    private boolean placeQueued;
    private int lastSlot = -1;
    private int plannedSlot = -1;
    private boolean slotSwapped = false;
    private int clutchBlocksPlaced = 0;
    private int lastPlacedX = Integer.MIN_VALUE;
    private int lastPlacedY = Integer.MIN_VALUE;
    private int lastPlacedZ = Integer.MIN_VALUE;
    private BlockPos lockedBlock = null;
    private EnumFacing lockedFacing = null;
    private float lockedAimYaw;
    private float lockedAimPitch;

    private static final double HW = 0.3;
    private static final double[][] CORNERS = {{-HW, -HW}, {HW, -HW}, {-HW, HW}, {HW, HW}};
    private static final double INSET = 0.05;
    private static final double STEP = 0.2;
    private static final double JIT = STEP * 0.1;

    public final FloatProperty reach = new FloatProperty("reach", 4.5f, 0.5f, 6.0f);
    public final IntProperty speed = new IntProperty("speed", 8, 1, 100);
    public final IntProperty snapbackSpeed = new IntProperty("snapback-speed", 12, 1, 100);
    public final IntProperty maxBlocks = new IntProperty("max-blocks", 10, 0, 50);
    public final IntProperty rotationTolerance = new IntProperty("rotation-tolerance", 25, 5, 100);
    public final BooleanProperty simulateFuture = new BooleanProperty("simulate-future-pos", true);
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"NONE", "SILENT"});

    public Clutch() {
        super("Clutch", false);
        BLOCK_SCORE.put("obsidian", 0);
        BLOCK_SCORE.put("end_stone", 1);
        BLOCK_SCORE.put("planks", 2);
        BLOCK_SCORE.put("log", 2);
        BLOCK_SCORE.put("log2", 2);
        BLOCK_SCORE.put("glass", 3);
        BLOCK_SCORE.put("stained_glass", 3);
        BLOCK_SCORE.put("stainedGlass", 3);
        BLOCK_SCORE.put("hardened_clay", 4);
        BLOCK_SCORE.put("stained_hardened_clay", 4);
        BLOCK_SCORE.put("hardenedClay", 4);
        BLOCK_SCORE.put("stainedHardenedClay", 4);
        BLOCK_SCORE.put("clay", 4);
        BLOCK_SCORE.put("stone", 5);
        BLOCK_SCORE.put("cloth", 5);
    }

    @Override
    public void onEnabled() {
        if (mc.thePlayer == null) return;
        serverYaw = mc.thePlayer.rotationYaw;
        serverPitch = mc.thePlayer.rotationPitch;
        aimYaw = serverYaw;
        aimPitch = serverPitch;
        hasAim = false;
        resetting = false;
        clutchBlocksPlaced = 0;
        lastSlot = mc.thePlayer.inventory.currentItem;
        targetBlock = null;
        targetFacing = null;
        targetHitVec = null;
        placeQueued = false;
        slotSwapped = false;
        plannedSlot = -1;
        lockedBlock = null;
        lockedFacing = null;
    }

    @Override
    public void onDisabled() {
        if (mc.thePlayer == null) return;
        restoreSlot();
        targetBlock = null;
        targetFacing = null;
        targetHitVec = null;
        hasAim = false;
        resetting = false;
        lockedBlock = null;
        lockedFacing = null;
    }

    @EventTarget(Priority.HIGH)
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled()) return;
        if (event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;

        serverYaw = event.getYaw();
        serverPitch = event.getPitch();

        if (mc.thePlayer.onGround) clutchBlocksPlaced = 0;

        BlockPos belowFeet = new BlockPos(
                MathHelper.floor_double(mc.thePlayer.posX),
                MathHelper.floor_double(mc.thePlayer.posY) - 1,
                MathHelper.floor_double(mc.thePlayer.posZ)
        );

        if (!isReplaceable(belowFeet)) {
            clearAim();
            return;
        }

        plannedSlot = findBestBlockSlot();
        if (plannedSlot == -1) {
            clearAim();
            return;
        }

        if (mc.thePlayer.inventory.currentItem != plannedSlot) {
            if (!slotSwapped) lastSlot = mc.thePlayer.inventory.currentItem;
            mc.thePlayer.inventory.currentItem = plannedSlot;
            slotSwapped = true;
        }

        ItemStack held = mc.thePlayer.inventory.getCurrentItem();
        if (held == null || !(held.getItem() instanceof ItemBlock)) {
            clearAim();
            return;
        }

        // Validate existing lock before searching for a new one
        boolean lockValid = lockedBlock != null && lockedFacing != null && isLockedTargetValid();
        if (!lockValid) {
            lockedBlock = null;
            lockedFacing = null;
            findClutchTarget();
        }

        if (targetBlock != null && targetHitVec != null) {
            aimYaw = lockedAimYaw;
            aimPitch = lockedAimPitch;
            hasAim = true;
            resetting = false;
        }

        if (resetting) {
            float[] sm = smoothStep(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, true);
            if (Math.abs(sm[0] - mc.thePlayer.rotationYaw) < 0.5f && Math.abs(sm[1] - mc.thePlayer.rotationPitch) < 0.5f) {
                resetting = false;
            } else {
                event.setRotation(sm[0], sm[1], 4);
                if (moveFix.getValue() != 0) event.setPervRotation(sm[0], 4);
            }
            return;
        }

        if (hasAim && targetBlock != null) {
            float[] sm = smoothStep(aimYaw, aimPitch, false);
            event.setRotation(sm[0], sm[1], 4);
            if (moveFix.getValue() != 0) event.setPervRotation(sm[0], 4);

            if (withinTolerance(sm[0], sm[1])) {
                int max = maxBlocks.getValue();
                if (max == 0 || clutchBlocksPlaced < max) {
                    MovingObjectPosition mop = rayTrace(sm[0], sm[1], reach.getValue());
                    if (mop != null
                            && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                            && mop.getBlockPos().equals(targetBlock)
                            && mop.sideHit == targetFacing) {
                        targetHitVec = mop.hitVec;
                        placeQueued = true;
                    }
                }
            }
        }

        if (placeQueued) {
            placeQueued = false;
            doPlace();
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!isEnabled()) return;
        if (moveFix.getValue() == 1
                && RotationState.isActived()
                && RotationState.getPriority() == 4
                && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
        }
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (isEnabled()) {
            lastSlot = event.setSlot(lastSlot);
            event.setCancelled(true);
        }
    }

    private boolean isLockedTargetValid() {
        if (isReplaceable(lockedBlock)) return false;
        MovingObjectPosition mop = rayTrace(lockedAimYaw, lockedAimPitch, reach.getValue());
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;
        if (!mop.getBlockPos().equals(lockedBlock)) return false;
        if (mop.sideHit != lockedFacing) return false;
        targetHitVec = mop.hitVec;
        targetBlock = lockedBlock;
        targetFacing = lockedFacing;
        return true;
    }

    private void findClutchTarget() {
        double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
        Vec3 eye = mc.thePlayer.getPositionEyes(1.0f);
        double reachD = reach.getValue();

        double futureX = px, futureY = py, futureZ = pz;
        if (simulateFuture.getValue()) {
            double mx = mc.thePlayer.motionX, my = mc.thePlayer.motionY, mz = mc.thePlayer.motionZ;
            for (int t = 0; t < 20; t++) {
                my = (my - 0.08) * 0.98;
                futureY += my;
                futureX += mx * 0.91;
                futureZ += mz * 0.91;
                if (futureY < py - 2 || my >= 0) break;
            }
        }

        int feetX = MathHelper.floor_double(px);
        int feetY = MathHelper.floor_double(py);
        int feetZ = MathHelper.floor_double(pz);

        List<Object[]> candidates = new ArrayList<>();
        for (int y = feetY - 1; y >= feetY - 4; y--) {
            for (int x = feetX - 5; x <= feetX + 4; x++) {
                for (int z = feetZ - 5; z <= feetZ + 4; z++) {
                    BlockPos bp = new BlockPos(x, y, z);
                    if (isReplaceable(bp)) continue;

                    double curDist = dist2AABB(px, py, pz, x, y, z);
                    double futureDist = dist2AABB(futureX, futureY, futureZ, x, y, z);
                    double score = simulateFuture.getValue() ? curDist * 0.3 + futureDist * 0.7 : curDist;

                    if (x == lastPlacedX && y == lastPlacedY && z == lastPlacedZ) score *= 0.95;

                    candidates.add(new Object[]{score, bp});
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(o -> (Double) o[0]));

        for (Object[] cand : candidates) {
            BlockPos bp = (BlockPos) cand[1];
            boolean underPlayer = isUnderPlayer(bp, px, py, pz);
            if (tryAimAtBlock(bp, eye, reachD, underPlayer)) return;
        }

        targetBlock = null;
        targetFacing = null;
        targetHitVec = null;
    }

    private boolean tryAimAtBlock(BlockPos bp, Vec3 eye, double reachD, boolean underPlayer) {
        boolean faceSouth = Math.abs(eye.zCoord - (bp.getZ() + 1)) < Math.abs(eye.zCoord - bp.getZ());
        boolean faceEast = Math.abs(eye.xCoord - (bp.getX() + 1)) < Math.abs(eye.xCoord - bp.getX());
        float baseYaw = serverYaw, basePit = serverPitch;

        int n = (int) Math.round(1.0 / STEP);
        List<float[]> rotCands = new ArrayList<>();
        rotCands.add(new float[]{0f, baseYaw, basePit});

        for (int r = 0; r <= n; r++) {
            double v = clamp(r * STEP + randJit(), 0, 1);
            for (int c = 0; c <= n; c++) {
                double u = clamp(c * STEP + randJit(), 0, 1);

                if (underPlayer) {
                    float[] rv = getRotationsWrapped(eye, bp.getX() + u, bp.getY() + 1 - INSET, bp.getZ() + v);
                    rotCands.add(new float[]{angCost(rv[0], rv[1], baseYaw, basePit), rv[0], rv[1]});
                }

                float[] rZ = getRotationsWrapped(eye, bp.getX() + u, bp.getY() + v, faceSouth ? bp.getZ() + 1 - INSET : bp.getZ() + INSET);
                rotCands.add(new float[]{angCost(rZ[0], rZ[1], baseYaw, basePit), rZ[0], rZ[1]});

                float[] rX = getRotationsWrapped(eye, faceEast ? bp.getX() + 1 - INSET : bp.getX() + INSET, bp.getY() + v, bp.getZ() + u);
                rotCands.add(new float[]{angCost(rX[0], rX[1], baseYaw, basePit), rX[0], rX[1]});
            }
        }

        rotCands.sort(Comparator.comparingDouble(a -> a[0]));

        for (float[] rc : rotCands) {
            float yawTest = unwrapYaw(rc[1], serverYaw);
            float pitTest = rc[2];

            MovingObjectPosition mop = rayTrace(yawTest, pitTest, reachD);
            if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) continue;
            if (!mop.getBlockPos().equals(bp)) continue;
            if (mop.sideHit == EnumFacing.DOWN) continue;
            if (mop.sideHit == EnumFacing.UP && !underPlayer) continue;

            targetBlock = bp;
            targetFacing = mop.sideHit;
            targetHitVec = mop.hitVec;
            lockedBlock = bp;
            lockedFacing = mop.sideHit;
            lockedAimYaw = yawTest;
            lockedAimPitch = pitTest;
            aimYaw = yawTest;
            aimPitch = pitTest;
            return true;
        }

        return false;
    }

    private void doPlace() {
        if (targetBlock == null || targetFacing == null || targetHitVec == null) return;
        ItemStack held = mc.thePlayer.inventory.getCurrentItem();
        if (held == null || !(held.getItem() instanceof ItemBlock)) return;

        boolean placed = mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, held, targetBlock, targetFacing, targetHitVec);

        if (placed) {
            if (targetFacing != EnumFacing.UP) clutchBlocksPlaced++;
            lastPlacedX = targetBlock.getX();
            lastPlacedY = targetBlock.getY();
            lastPlacedZ = targetBlock.getZ();
            mc.thePlayer.swingItem();
        }
    }

    private int findBestBlockSlot() {
        int best = -1, bestScore = Integer.MIN_VALUE;
        for (int slot = 8; slot >= 0; slot--) {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(slot);
            if (s == null || s.stackSize == 0) continue;
            if (!(s.getItem() instanceof ItemBlock)) continue;
            Block block = ((ItemBlock) s.getItem()).getBlock();
            String name = block.getUnlocalizedName().replace("tile.", "").replace("block.", "");
            Integer score = BLOCK_SCORE.get(name);
            if (score == null) {
                String lower = name.toLowerCase();
                if (lower.contains("clay")) score = 4;
                else if (lower.contains("stone") || lower.contains("brick")) score = 5;
                else if (lower.contains("plank") || lower.contains("log")) score = 2;
                else if (lower.contains("glass")) score = 3;
                else if (lower.contains("wool") || lower.contains("cloth")) score = 5;
                else continue;
            }
            if (score > bestScore) {
                bestScore = score;
                best = slot;
            }
        }
        return best;
    }

    private void restoreSlot() {
        if (slotSwapped && lastSlot != -1 && mc.thePlayer != null && mc.thePlayer.inventory.currentItem != lastSlot) {
            mc.thePlayer.inventory.currentItem = lastSlot;
        }
        slotSwapped = false;
    }

    private void clearAim() {
        if (hasAim) resetting = true;
        hasAim = false;
        targetBlock = null;
        targetFacing = null;
        targetHitVec = null;
        lockedBlock = null;
        lockedFacing = null;
        lastPlacedX = lastPlacedY = lastPlacedZ = Integer.MIN_VALUE;
        clutchBlocksPlaced = 0;
        restoreSlot();
    }

    private float[] smoothStep(float targetYaw, float targetPitch, boolean snapback) {
        float curYaw = serverYaw, curPit = serverPitch;
        float dYaw = targetYaw - curYaw, dPit = targetPitch - curPit;

        if (Math.abs(dYaw) < 0.1f) curYaw = targetYaw;
        if (Math.abs(dPit) < 0.1f) curPit = targetPitch;
        if (curYaw == targetYaw && curPit == targetPitch) return new float[]{curYaw, curPit};

        float maxStep = snapback ? snapbackSpeed.getValue() : speed.getValue();
        maxStep *= (float) (1.0 - Math.random() * 0.2);

        float total = Math.abs(dYaw) + Math.abs(dPit);
        if (total <= maxStep) return new float[]{targetYaw, targetPitch};

        float scale = maxStep / total;
        return new float[]{curYaw + dYaw * scale, curPit + dPit * scale};
    }

    private boolean withinTolerance(float yaw, float pitch) {
        float dy = Math.abs(MathHelper.wrapAngleTo180_float(yaw - serverYaw));
        float dp = Math.abs(MathHelper.wrapAngleTo180_float(pitch - serverPitch));
        return dy <= rotationTolerance.getValue() && dp <= rotationTolerance.getValue();
    }

    private boolean isUnderPlayer(BlockPos bp, double px, double py, double pz) {
        if (bp.getY() >= MathHelper.floor_double(py)) return false;
        for (double[] c : CORNERS) {
            if (bp.getX() == MathHelper.floor_double(px + c[0]) && bp.getZ() == MathHelper.floor_double(pz + c[1]))
                return true;
        }
        return false;
    }

    private boolean isReplaceable(BlockPos pos) {
        Block b = mc.theWorld.getBlockState(pos).getBlock();
        return b == Blocks.air || b == Blocks.water || b == Blocks.flowing_water || b == Blocks.lava || b == Blocks.flowing_lava || b == Blocks.fire;
    }

    private double dist2AABB(double px, double py, double pz, int bx, int by, int bz) {
        double cx = clamp(px, bx, bx + 1);
        double cy = clamp(py, by, by + 1);
        double cz = clamp(pz, bz, bz + 1);
        double dx = px - cx, dy = py - cy, dz = pz - cz;
        return dx * dx + dy * dy + dz * dz;
    }

    private float[] getRotationsWrapped(Vec3 eye, double tx, double ty, double tz) {
        double dx = tx - eye.xCoord, dy = ty - eye.yCoord, dz = tz - eye.zCoord;
        double hd = Math.sqrt(dx * dx + dz * dz);
        float yaw = normYaw((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, hd));
        return new float[]{yaw, pitch};
    }

    private MovingObjectPosition rayTrace(float yaw, float pitch, double range) {
        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        double dirX = -Math.sin(yr) * Math.cos(pr);
        double dirY = -Math.sin(pr);
        double dirZ = Math.cos(yr) * Math.cos(pr);
        Vec3 start = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 end = start.addVector(dirX * range, dirY * range, dirZ * range);
        return mc.theWorld.rayTraceBlocks(start, end);
    }

    private float normYaw(float y) {
        y = ((y % 360f) + 360f) % 360f;
        return y > 180f ? y - 360f : y;
    }

    private float unwrapYaw(float yaw, float prev) {
        return prev + ((((yaw - prev + 180f) % 360f) + 360f) % 360f - 180f);
    }

    private float angCost(float y, float p, float baseY, float baseP) {
        float dy = y - baseY;
        while (dy <= -180f) dy += 360f;
        while (dy > 180f) dy -= 360f;
        return Math.abs(dy) + Math.abs(p - baseP);
    }

    private double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private double randJit() {
        return Math.random() * JIT * 2 - JIT;
    }

    public int getSlot() {
        return lastSlot;
    }
}