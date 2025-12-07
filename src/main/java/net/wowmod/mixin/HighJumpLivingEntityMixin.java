package net.wowmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.wowmod.util.IAnimatedPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class HighJumpLivingEntityMixin {

    // 1. SUPPRESSION: Prevent normal jump if charging
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void suppressJumpWhileCharging(CallbackInfo ci) {
        // Only run on client side for now as input check is client-side
        if ((Object) this instanceof PlayerEntity player && player.getEntityWorld().isClient()) {
            if (player instanceof IAnimatedPlayer animatedPlayer) {
                // If we have any charge...
                if (animatedPlayer.wowmod$getHighJumpCharge() > 0.0f) {
                    // ...AND the jump key is currently HELD down
                    if (MinecraftClient.getInstance().options.jumpKey.isPressed()) {
                        // Then we are charging, so CANCEL the vanilla jump.
                        ci.cancel();
                    }
                }
            }
        }
    }

    // 2. BOOST: Apply extra velocity if released
    @Inject(method = "jump", at = @At("TAIL"))
    private void applyHighJumpBoost(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player) {
            if (player instanceof IAnimatedPlayer animatedPlayer) {
                float charge = animatedPlayer.wowmod$getHighJumpCharge();

                if (charge > 0.0f) {
                    Vec3d velocity = player.getVelocity();

                    // Boost: 1.0x (base) + extra
                    // CHANGED: Reduced multiplier from 1.5f (2.5x max) to 0.75f (1.75x max)
                    float extraBoost = 0.35f;
                    float multiplier = 1.0f + (charge * extraBoost);

                    player.setVelocity(velocity.x, velocity.y * multiplier, velocity.z);
                    player.velocityDirty = true;

                    // Reset charge after use
                    animatedPlayer.wowmod$setHighJumpCharge(0.0f);
                }
            }
        }
    }
}