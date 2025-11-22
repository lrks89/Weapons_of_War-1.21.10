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

    /**
     * FIX: Changed the first parameter to 'Object' with @Coerce.
     * The game version 1.21.10 uses a specific internal class 'PlayerLikeEntity' for the renderer
     * that is causing descriptor mismatches. Using 'Object' + @Coerce creates a universal wildcard
     * that bypasses the strict type check entirely.
     */
    @Inject(method = "updateRenderState", at = @At("RETURN"))
    private void wowmod_capturePlayerReference(@Coerce Object playerObj, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        // We safely cast the Object back to PlayerEntity inside the method body.
        if (playerObj instanceof PlayerEntity player) {
            ((IPlayerStateExtension) state).wowmod_setPlayer(player);
        }
    }
}