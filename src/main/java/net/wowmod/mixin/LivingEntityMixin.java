package net.wowmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void scaleSwingSpeed(CallbackInfoReturnable<Integer> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // --- TWEAK VALUES HERE ---

        // 1. Get the weapon's attribute speed (Defined in ModItems.java)
        // e.g. Dagger = Fast (high number), Greatsword = Slow (low number)
        double attackSpeed = entity.getAttributeValue(EntityAttributes.ATTACK_SPEED);

        // 2. Reference Speed (Vanilla player default is 4.0)
        double referenceSpeed = 4.0;

        // 3. Base Duration in Ticks
        // Vanilla default is 6.0 ticks (0.3s).
        // Since your animations are 0.5s (10 ticks), setting this to 10.0
        // ensures that a standard weapon plays your animation at 'natural' speed.
        double baseDuration = 3.0;

        // -------------------------

        // Calculation:
        // If Attack Speed is 4.0 (Normal) -> 10 * (4/4) = 10 ticks (0.5s) -> Perfect sync with your file
        // If Attack Speed is 2.0 (Slow)   -> 10 * (4/2) = 20 ticks (1.0s) -> Animation slows down to half speed
        // If Attack Speed is 8.0 (Fast)   -> 10 * (4/8) = 5 ticks (0.25s) -> Animation speeds up 2x
        int duration = (int) Math.round(baseDuration * (referenceSpeed / attackSpeed));

        // Safety: Ensure duration is never 0 to prevent crashes
        cir.setReturnValue(Math.max(1, duration));
    }
}