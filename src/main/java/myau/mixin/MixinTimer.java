package myau.mixin;

import net.minecraft.util.Timer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {Timer.class}, priority = 9999)
public abstract class MixinTimer {

    @Shadow
    public float timerSpeed;

    @Inject(
            method = {"updateTimer"},
            at = @At("HEAD")
    )
    private void updateTimer(CallbackInfo ci) {
        this.timerSpeed = myau.module.modules.Timer.getRequestedSpeed();
    }
}