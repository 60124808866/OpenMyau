package myau.mixin;

import myau.Myau;
import myau.module.modules.AntiObbyTrap;
import myau.module.modules.Jesus;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(value = {World.class}, priority = 9999)
public abstract class MixinWorld {
    @Redirect(
            method = {"handleMaterialAcceleration"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;isPushedByWater()Z"
            )
    )
    private boolean handleMaterialAcceleration(Entity entity) {
        if (entity instanceof EntityPlayerSP && Myau.moduleManager != null) {
            Jesus jesus = (Jesus) Myau.moduleManager.modules.get(Jesus.class);
            if (jesus.isEnabled() && jesus.noPush.getValue()) {
                return false;
            }
        }
        return entity.isPushedByWater();
    }

    @Redirect(
            method = {"rayTraceBlocks(Lnet/minecraft/util/Vec3;Lnet/minecraft/util/Vec3;ZZZ)Lnet/minecraft/util/MovingObjectPosition;"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/BlockPos;)Lnet/minecraft/block/state/IBlockState;"
            )
    )
    private IBlockState rayTraceBlocks(World world, BlockPos blockPos) {
        if (Myau.moduleManager == null) {
            return world.getBlockState(blockPos);
        } else {
            AntiObbyTrap antiObbyTrap = (AntiObbyTrap) Myau.moduleManager.modules.get(AntiObbyTrap.class);
            if (antiObbyTrap.isEnabled() && antiObbyTrap.isInsideBlock(world, blockPos)) {
                if (antiObbyTrap.setAir.getValue()) {
                    world.setBlockToAir(blockPos);
                }
                return Blocks.air.getDefaultState();
            } else {
                return world.getBlockState(blockPos);
            }
        }
    }
}
