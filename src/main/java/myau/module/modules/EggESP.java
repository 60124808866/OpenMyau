package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.IntProperty;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class EggESP extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    // Cache of found eggs
    private final CopyOnWriteArraySet<BlockPos> eggs = new CopyOnWriteArraySet<>();

    // Settings
    public final IntProperty range = new IntProperty("range", 48, 8, 128);
    public final IntProperty yRange = new IntProperty("y-range", 32, 8, 128);
    public final BooleanProperty outline = new BooleanProperty("outline", true);
    public final ColorProperty color = new ColorProperty("color", new Color(108, 0, 210).getRGB());

    public EggESP() {
        super("EggESP", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) return;
        if (event.getType() != EventType.POST) return;

        if (mc.theWorld == null || mc.thePlayer == null) {
            eggs.clear();
            return;
        }

        // Don’t scan every tick to reduce load
        // (every 10 ticks ≈ twice per second)
        if (mc.thePlayer.ticksExisted % 10 != 0) return;

        scanForEggs();
    }

    private void scanForEggs() {
        eggs.clear();

        BlockPos base = mc.thePlayer.getPosition();
        int r = range.getValue();
        int yr = yRange.getValue();

        int minY = Math.max(0, base.getY() - yr);
        int maxY = Math.min(255, base.getY() + yr);

        for (int x = base.getX() - r; x <= base.getX() + r; x++) {
            for (int z = base.getZ() - r; z <= base.getZ() + r; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    // Avoid chunk loads / unnecessary lookups
                    if (!mc.theWorld.isBlockLoaded(pos, false)) continue;

                    if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.dragon_egg) {
                        eggs.add(pos);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (!this.isEnabled()) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        Color c = new Color(color.getValue());

        RenderUtil.enableRenderState();

        for (BlockPos pos : eggs) {
            // If egg got broken / moved, drop it from cache
            if (!mc.theWorld.isBlockLoaded(pos, false) ||
                    mc.theWorld.getBlockState(pos).getBlock() != Blocks.dragon_egg) {
                eggs.remove(pos);
                continue;
            }

            if (outline.getValue()) {
                RenderUtil.drawBlockBoundingBox(pos, 1.0, c.getRed(), c.getGreen(), c.getBlue(), 255, 1.5F);
            }
            RenderUtil.drawBlockBox(pos, 1.0, c.getRed(), c.getGreen(), c.getBlue());
        }

        RenderUtil.disableRenderState();
    }
}