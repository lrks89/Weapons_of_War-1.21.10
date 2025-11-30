package net.wowmod.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.wowmod.util.RenderStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class PlayerEntityRendererMixin {

    // We inject into the superclass method. This works because PlayerEntityRenderer calls super.updateRenderState().
    // Targeting LivingEntityRenderer avoids the generic type signature mismatches in PlayerEntityRenderer.
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void captureSprinting(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        // Check if the entity being rendered is actually a player
        if (entity instanceof AbstractClientPlayerEntity player) {
            // Check if the state object supports our extension (applied via PlayerEntityRenderStateMixin)
            if (state instanceof RenderStateExtension extendedState) {
                extendedState.wowmod$setSprinting(player.isSprinting());
            }
        }
    }
}