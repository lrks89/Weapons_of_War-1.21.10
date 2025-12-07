package net.wowmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class AnimationLivingEntityMixin {

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void scaleSwingSpeed(CallbackInfoReturnable<Integer> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity.getAttributeInstance(EntityAttributes.ATTACK_SPEED) == null) {
            return;
        }

        double attackSpeed = entity.getAttributeValue(EntityAttributes.ATTACK_SPEED);
        double referenceSpeed = 4.0;
        double baseDuration = 3.0;
        int duration = (int) Math.round(baseDuration * (referenceSpeed / attackSpeed));
        cir.setReturnValue(Math.max(1, duration));
    }
}