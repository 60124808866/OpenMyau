package myau.mixin;

import myau.management.RotationState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = {RenderManager.class}, priority = 9999)
public abstract class MixinRenderManager {
    @Unique
    private float _prevRenderYawOffset;
    @Unique
    private float _renderYawOffset;
    @Unique
    private float _prevRotationYawHead;
    @Unique
    private float _rotationYawHead;
    @Unique
    private float _prevRotationPitch;
    @Unique
    private float _rotationPitch;

    @Inject(
            method = {"renderEntityStatic"},
            at = {@At("HEAD")}
    )
    private void renderEntityStatic(Entity entity, float float2, boolean boolean3, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (entity instanceof EntityPlayerSP && RotationState.isRotated(1)) {
            EntityPlayerSP entityPlayerSP = (EntityPlayerSP) entity;
            this._prevRenderYawOffset = entityPlayerSP.prevRenderYawOffset;
            this._renderYawOffset = entityPlayerSP.renderYawOffset;
            this._prevRotationYawHead = entityPlayerSP.prevRotationYawHead;
            this._rotationYawHead = entityPlayerSP.rotationYawHead;
            this._prevRotationPitch = entityPlayerSP.prevRotationPitch;
            this._rotationPitch = entityPlayerSP.rotationPitch;
            entityPlayerSP.prevRenderYawOffset = RotationState.getPrevRenderYawOffset();
            entityPlayerSP.renderYawOffset = RotationState.getRenderYawOffset();
            entityPlayerSP.prevRotationYawHead = RotationState.getPrevRotationYawHead();
            entityPlayerSP.rotationYawHead = RotationState.getRotationYawHead();
            entityPlayerSP.prevRotationPitch = RotationState.getPrevRotationPitch();
            entityPlayerSP.rotationPitch = RotationState.getRotationPitch();
        }
    }

    @Inject(
            method = {"renderEntityStatic"},
            at = {@At("RETURN")}
    )
    private void renderEntityStaticPost(Entity entity, float float2, boolean boolean3, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (entity instanceof EntityPlayerSP && RotationState.isRotated(1)) {
            EntityPlayerSP entityPlayerSP = (EntityPlayerSP) entity;
            entityPlayerSP.prevRenderYawOffset = this._prevRenderYawOffset;
            entityPlayerSP.renderYawOffset = this._renderYawOffset;
            entityPlayerSP.prevRotationYawHead = this._prevRotationYawHead;
            entityPlayerSP.rotationYawHead = this._rotationYawHead;
            entityPlayerSP.prevRotationPitch = this._prevRotationPitch;
            entityPlayerSP.rotationPitch = this._rotationPitch;
        }
    }
}
