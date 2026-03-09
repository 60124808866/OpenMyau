package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.AttackEvent;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.util.RenderUtil;
import myau.util.RotationUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S0FPacketSpawnMob;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Backtrack extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public final FloatProperty range;
    public final BooleanProperty adaptive;
    public final IntProperty normalDelay;
    public final IntProperty adaptiveDelay;
    public final BooleanProperty releaseOnHit;
    public final BooleanProperty interruptLagRange;
    public final BooleanProperty players;
    public final BooleanProperty teams;
    public final BooleanProperty botCheck;
    public final BooleanProperty esp;

    private final ConcurrentLinkedQueue<Packet<?>> incomingQueue = new ConcurrentLinkedQueue<>();
    private final Map<Integer, long[]> encodedPositions = new HashMap<>();

    private EntityLivingBase target;
    private EntityLivingBase lastAttacked;
    private long lastAttackTime = 0L;
    private static final long COMBAT_LOCK_MS = 3000L;
    private AxisAlignedBB realAABB;
    private long backtrackStartTime = 0L;
    private boolean lagRangeInterrupted = false;

    public Backtrack() {
        super("Backtrack", false);
        this.range = new FloatProperty("range", 3.5F, 1.0F, 8.0F);
        this.adaptive = new BooleanProperty("adaptive", true);
        this.normalDelay = new IntProperty("normal-delay", 100, 50, 1000, () -> !this.adaptive.getValue());
        this.adaptiveDelay = new IntProperty("adaptive-delay", 100, 50, 1000, this.adaptive::getValue);
        this.releaseOnHit = new BooleanProperty("release-on-hit", true);
        this.interruptLagRange = new BooleanProperty("interrupt-lagrange", true);
        this.players = new BooleanProperty("players", true);
        this.teams = new BooleanProperty("teams", true);
        this.botCheck = new BooleanProperty("bot-check", true);
        this.esp = new BooleanProperty("esp", true);
    }

    @Override
    public void onEnabled() {
        incomingQueue.clear();
        encodedPositions.clear();
        realAABB = null;
        backtrackStartTime = 0L;
        target = null;
        lastAttacked = null;
        lastAttackTime = 0L;
        lagRangeInterrupted = false;
    }

    @Override
    public void onDisabled() {
        setLagRangeEnabled(true);
        releaseIncoming();
        incomingQueue.clear();
        encodedPositions.clear();
        realAABB = null;
        target = null;
        lastAttacked = null;
        lastAttackTime = 0L;
    }

    private int currentMaxDelay() {
        return adaptive.getValue() ? adaptiveDelay.getValue() : normalDelay.getValue();
    }

    private LagRange getLagRange() {
        Module m = Myau.moduleManager.getModule(LagRange.class);
        return (m instanceof LagRange) ? (LagRange) m : null;
    }

    private void setLagRangeEnabled(boolean enabled) {
        if (!interruptLagRange.getValue()) return;
        LagRange lr = getLagRange();
        if (lr == null) return;
        if (enabled && lagRangeInterrupted) {
            lagRangeInterrupted = false;
            lr.setEnabled(true);
        } else if (!enabled && !lagRangeInterrupted && lr.isEnabled()) {
            lagRangeInterrupted = true;
            lr.setEnabled(false);
        }
    }

    private boolean isInCombat() {
        if (lastAttacked == null || lastAttacked != target || lastAttacked.isDead) return false;
        return System.currentTimeMillis() - lastAttackTime <= COMBAT_LOCK_MS;
    }

    private Vec3 getRealPosition(int entityId) {
        long[] enc = encodedPositions.get(entityId);
        if (enc == null) return null;
        return new Vec3(enc[0] / 32.0, enc[1] / 32.0, enc[2] / 32.0);
    }

    private boolean canBacktrack() {
        if (target == null || target.isDead) return false;
        Vec3 real = getRealPosition(target.getEntityId());
        if (real == null) return false;
        if (distanceTo(real) > range.getValue()) return false;
        if (backtrackStartTime > 0 && System.currentTimeMillis() - backtrackStartTime > currentMaxDelay()) return false;
        double distReal = distanceTo(real);
        double distCurrent = mc.thePlayer.getDistanceToEntity(target);
        return adaptive.getValue() ? distReal > distCurrent : distReal > distCurrent + 0.1;
    }

    private boolean shouldQueueIncoming(Packet<?> p) {
        if (p instanceof S12PacketEntityVelocity) return false;
        if (p instanceof S27PacketExplosion) return false;
        if (p instanceof S0FPacketSpawnMob) return false;
        if (p instanceof S14PacketEntity) {
            Entity e = ((S14PacketEntity) p).getEntity(mc.theWorld);
            return e != null && e == target;
        }
        if (p instanceof S18PacketEntityTeleport) return ((S18PacketEntityTeleport) p).getEntityId() == target.getEntityId();
        if (p instanceof S19PacketEntityHeadLook) return ((S19PacketEntityHeadLook) p).getEntity(mc.theWorld) == target;
        return false;
    }

    private void releaseIncoming() {
        if (mc.getNetHandler() == null) return;
        Packet<?> p;
        while ((p = incomingQueue.poll()) != null) processPacketUnchecked(p);
        backtrackStartTime = 0L;
        realAABB = null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.network.INetHandler> void processPacketUnchecked(Packet<T> packet) {
        packet.processPacket((T) Minecraft.getMinecraft().getNetHandler());
    }

    private void releaseAll() {
        releaseIncoming();
    }

    private void updateRealPosition(Packet<?> packet) {
        if (packet instanceof S14PacketEntity) {
            S14PacketEntity p = (S14PacketEntity) packet;
            Entity e = p.getEntity(mc.theWorld);
            if (e == null) return;
            int id = e.getEntityId();

            long[] base = encodedPositions.computeIfAbsent(id, k ->
                    new long[]{
                            Math.round(e.posX * 32.0),
                            Math.round(e.posY * 32.0),
                            Math.round(e.posZ * 32.0)
                    });

            base[0] += p.func_149062_c();
            base[1] += p.func_149061_d();
            base[2] += p.func_149064_e();

            if (target != null && id == target.getEntityId()) {
                double x = base[0] / 32.0;
                double y = base[1] / 32.0;
                double z = base[2] / 32.0;
                double hw = target.width / 2.0;
                realAABB = new AxisAlignedBB(x - hw, y, z - hw, x + hw, y + target.height, z + hw);
            }

        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport p = (S18PacketEntityTeleport) packet;
            int id = p.getEntityId();

            long encX = p.getX();
            long encY = p.getY();
            long encZ = p.getZ();
            encodedPositions.put(id, new long[]{encX, encY, encZ});

            if (target != null && id == target.getEntityId()) {
                double x = encX / 32.0;
                double y = encY / 32.0;
                double z = encZ / 32.0;
                double hw = target.width / 2.0;
                realAABB = new AxisAlignedBB(x - hw, y, z - hw, x + hw, y + target.height, z + hw);
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!isEnabled() || mc.thePlayer == null) return;
        if (event.getTarget() instanceof EntityLivingBase) {
            lastAttacked = (EntityLivingBase) event.getTarget();
            lastAttackTime = System.currentTimeMillis();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;
        Module scaffold = Myau.moduleManager.getModule(Scaffold.class);
        if (scaffold != null && scaffold.isEnabled()) {
            setLagRangeEnabled(true);
            releaseAll();
            incomingQueue.clear();
            return;
        }
        if (event.getType() == EventType.RECEIVE) {
            handleIncoming(event);
        }
    }

    private void handleIncoming(PacketEvent event) {
        Packet<?> packet = event.getPacket();
        updateRealPosition(packet);
        if (target == null) return;
        if (canBacktrack() && shouldQueueIncoming(packet)) {
            if (backtrackStartTime == 0L) backtrackStartTime = System.currentTimeMillis();
            incomingQueue.add(packet);
            event.setCancelled(true);
        } else if (!canBacktrack() && !incomingQueue.isEmpty()) {
            releaseIncoming();
        }
    }

    @EventTarget(Priority.LOW)
    public void onTick(TickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;
        if (event.getType() == EventType.PRE) tickPre();
    }

    private void tickPre() {
        EntityLivingBase newTarget = resolveTarget();
        if (newTarget != target) {
            setLagRangeEnabled(true);
            releaseAll();
        }
        target = newTarget;

        if (target == null) {
            setLagRangeEnabled(true);
            return;
        }

        encodedPositions.computeIfAbsent(target.getEntityId(), k ->
                new long[]{
                        Math.round(target.posX * 32.0),
                        Math.round(target.posY * 32.0),
                        Math.round(target.posZ * 32.0)
                });

        if (realAABB == null) {
            long[] enc = encodedPositions.get(target.getEntityId());
            if (enc != null) {
                double x = enc[0] / 32.0;
                double y = enc[1] / 32.0;
                double z = enc[2] / 32.0;
                double hw = target.width / 2.0;
                realAABB = new AxisAlignedBB(x - hw, y, z - hw, x + hw, y + target.height, z + hw);
            }
        }

        Vec3 real = getRealPosition(target.getEntityId());
        if (real == null) return;

        boolean shouldRelease = false;
        if (mc.thePlayer.hurtTime == mc.thePlayer.maxHurtTime && mc.thePlayer.maxHurtTime > 0) shouldRelease = true;
        if (distanceTo(real) > range.getValue()) shouldRelease = true;
        if (backtrackStartTime > 0 && System.currentTimeMillis() - backtrackStartTime > currentMaxDelay()) shouldRelease = true;
        if (releaseOnHit.getValue() && target.hurtTime == 1) shouldRelease = true;

        if (shouldRelease) {
            setLagRangeEnabled(true);
            releaseAll();
            return;
        }

        if (isInCombat() && !incomingQueue.isEmpty()) {
            setLagRangeEnabled(false);
        } else if (!isInCombat() || incomingQueue.isEmpty()) {
            setLagRangeEnabled(true);
        }
    }

    @EventTarget(Priority.HIGH)
    public void onRender3D(Render3DEvent event) {
        if (!isEnabled() || !esp.getValue()) return;
        if (target == null || realAABB == null) return;

        AxisAlignedBB visual = target.getEntityBoundingBox();
        double dx = Math.abs(realAABB.minX - visual.minX);
        double dy = Math.abs(realAABB.minY - visual.minY);
        double dz = Math.abs(realAABB.minZ - visual.minZ);
        if (dx < 0.05 && dy < 0.05 && dz < 0.05) return;

        Color color = (target instanceof EntityPlayer)
                ? TeamUtil.getTeamColor((EntityPlayer) target, 1.0F)
                : new Color(255, 60, 60);

        IAccessorRenderManager rm = (IAccessorRenderManager) mc.getRenderManager();
        AxisAlignedBB aabb = realAABB.offset(
                -rm.getRenderPosX(),
                -rm.getRenderPosY(),
                -rm.getRenderPosZ());

        RenderUtil.enableRenderState();
        RenderUtil.drawFilledBox(aabb, color.getRed(), color.getGreen(), color.getBlue());
        RenderUtil.disableRenderState();
    }

    private EntityLivingBase resolveTarget() {
        KillAura ka = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (ka != null && ka.isEnabled() && ka.getTarget() != null) return ka.getTarget();
        if (lastAttacked != null && !lastAttacked.isDead
                && System.currentTimeMillis() - lastAttackTime <= COMBAT_LOCK_MS
                && mc.thePlayer.getDistanceToEntity(lastAttacked) <= range.getValue() * 2.0F) {
            return lastAttacked;
        }
        ArrayList<EntityLivingBase> candidates = new ArrayList<>();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) continue;
            EntityLivingBase e = (EntityLivingBase) entity;
            if (isValidTarget(e) && mc.thePlayer.getDistanceToEntity(e) <= range.getValue()) candidates.add(e);
        }
        if (candidates.isEmpty()) return null;
        candidates.sort((a, b) -> Float.compare(RotationUtil.angleToEntity(a), RotationUtil.angleToEntity(b)));
        return candidates.get(0);
    }

    private boolean isValidTarget(EntityLivingBase e) {
        if (!mc.theWorld.loadedEntityList.contains(e)) return false;
        if (e == mc.thePlayer || e == mc.thePlayer.ridingEntity) return false;
        if (e == mc.getRenderViewEntity() || e == mc.getRenderViewEntity().ridingEntity) return false;
        if (e.deathTime > 0) return false;
        if (e instanceof EntityPlayer) {
            if (!players.getValue()) return false;
            EntityPlayer p = (EntityPlayer) e;
            if (TeamUtil.isFriend(p)) return false;
            if (teams.getValue() && TeamUtil.isSameTeam(p)) return false;
            if (botCheck.getValue() && TeamUtil.isBot(p)) return false;
            return true;
        }
        return false;
    }

    private double distanceTo(Vec3 v) {
        return mc.thePlayer.getDistance(v.xCoord, v.yCoord, v.zCoord);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{ currentMaxDelay() + "ms" };
    }
}