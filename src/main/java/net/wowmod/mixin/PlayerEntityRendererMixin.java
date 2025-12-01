package net.wowmod.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.wowmod.util.RenderStateExtension;
import net.wowmod.util.IAnimatedPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void captureSprinting(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayerEntity player) {
            if (state instanceof RenderStateExtension extendedState) {
                extendedState.wowmod$setSprinting(player.isSprinting());

                extendedState.wowmod$setVerticalVelocity(player.getVelocity().y);
                extendedState.wowmod$setOnGround(player.isOnGround());

                boolean isElytra = ((LivingEntity)player).isGliding();
                extendedState.wowmod$setFlying(player.getAbilities().flying || isElytra);

                extendedState.wowmod$setSwimming(player.isInSwimmingPose());
                extendedState.wowmod$setRiding(player.hasVehicle());
                extendedState.wowmod$setClimbing(player.isClimbing());

                // --- Fluid Detection ---
                boolean inCollisionFluid = player.isInLava() || player.isSubmergedInWater() || player.isTouchingWater();
                extendedState.wowmod$setInWater(inCollisionFluid);

                if (player instanceof IAnimatedPlayer animatedPlayer) {
                    long diff = player.getEntityWorld().getTime() - animatedPlayer.wowmod$getLastLandTime();
                    extendedState.wowmod$setTimeSinceLanding(diff);

                    // New: Capture persistent fluid-below state from PlayerEntityMixin
                    extendedState.wowmod$setWasFluidBelow(animatedPlayer.wowmod$wasFluidBelow());
                }
            }
        }
    }
}