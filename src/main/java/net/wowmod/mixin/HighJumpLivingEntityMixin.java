package net.wowmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.wowmod.logic.JumpMechanics;
import net.wowmod.util.IAnimatedPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class HighJumpLivingEntityMixin {

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void suppressJumpWhileCharging(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player && player.getEntityWorld().isClient()) {
            if (player instanceof IAnimatedPlayer animatedPlayer) {
                if (animatedPlayer.wowmod$getHighJumpCharge() > 0.0f) {
                    if (MinecraftClient.getInstance().options.jumpKey.isPressed()) {
                        ci.cancel();
                    }
                }
            }
        }
    }

    @Inject(method = "jump", at = @At("TAIL"))
    private void applyHighJumpBoost(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player) {
            // Delegate math to Logic class
            JumpMechanics.applyHighJumpBoost(player);
        }
    }
}