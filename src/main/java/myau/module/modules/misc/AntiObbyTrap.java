package myau.module.modules.misc;

import myau.module.Category;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class AntiObbyTrap extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final BooleanProperty setAir = new BooleanProperty("set-air", true);

    public AntiObbyTrap() {
        super("AntiObbyTrap", Category.Misc, false);
    }

    public boolean isInsideBlock(World world, BlockPos blockPos) {
        IBlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (block.getMaterial().isSolid() && block.isFullBlock()) {
            Vec3 hitVec = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
            return block.getCollisionBoundingBox(mc.theWorld, blockPos, blockState).isVecInside(hitVec);
        } else {
            return false;
        }
    }
}
