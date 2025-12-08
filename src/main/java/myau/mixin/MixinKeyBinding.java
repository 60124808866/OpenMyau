package myau.mixin;

import myau.event.EventManager;
import myau.events.SwapItemEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = {KeyBinding.class}, priority = 9999)
public abstract class MixinKeyBinding {
    @Shadow
    private String keyDescription;

    @Inject(
            method = {"isPressed"},
            at = {@At("RETURN")},
            cancellable = true
    )
    private void isPressed(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (callbackInfoReturnable.getReturnValue()) {
            Minecraft mc = Minecraft.getMinecraft();
            for (int i = 0; i < 9; i++) {
                if (mc.gameSettings.keyBindsHotbar[i].getKeyDescription().equals(this.keyDescription)) {
                    SwapItemEvent event = new SwapItemEvent(i, 0);
                    EventManager.call(event);
                    if (event.isCancelled()) {
                        callbackInfoReturnable.setReturnValue(false);
                    }
                }
            }
        }
    }
}
