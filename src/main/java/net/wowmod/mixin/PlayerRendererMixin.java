package net.wowmod.mixin;

import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.PlayerEntity;
import net.wowmod.util.IPlayerStateExtension;
import net.wowmod.util.IWeaponTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerRendererMixin {

    @Inject(method = "updateRenderState", at = @At("RETURN"))
    private void wowmod_captureState(@Coerce Object playerObj, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (playerObj instanceof PlayerEntity player) {
            // 1. Capture Player Reference (Existing)
            ((IPlayerStateExtension) state).wowmod_setPlayer(player);

            // 2. Capture Weapon Transform Data (New for 1.21.10)
            if (player instanceof IWeaponTransform source && state instanceof IWeaponTransform target) {
                target.wowmod_setWeaponRotation(source.wowmod_getWeaponRotation());
                target.wowmod_setWeaponPosition(source.wowmod_getWeaponPosition());
            }
        }
    }
}