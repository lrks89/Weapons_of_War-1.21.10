package net.wowmod.mixin;

import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.PlayerEntity;
import net.wowmod.util.IPlayerStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerRendererMixin {

    @Inject(method = "updateRenderState", at = @At("RETURN"))
    private void wowmod_capturePlayerReference(@Coerce Object playerObj, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        // We safely cast the Object back to PlayerEntity inside the method body.
        if (playerObj instanceof PlayerEntity player) {
            ((IPlayerStateExtension) state).wowmod_setPlayer(player);
        }
    }
}