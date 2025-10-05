package myau.mixin;

import myau.Myau;
import myau.module.modules.render.Xray;
import net.minecraft.block.BlockGrass;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin({BlockGrass.class})
public abstract class MixinBlockGrass {
    @Inject(
            method = {"getBlockLayer"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void getBlockLayer(CallbackInfoReturnable<EnumWorldBlockLayer> callbackInfoReturnable) {
        if (Myau.moduleManager != null) {
            if (Myau.moduleManager.modules.get(Xray.class).isEnabled()) {
                callbackInfoReturnable.setReturnValue(EnumWorldBlockLayer.TRANSLUCENT);
            }
        }
    }
}
