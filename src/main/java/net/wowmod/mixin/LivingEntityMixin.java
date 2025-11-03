package net.wowmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

//Data for Blocking Mechanic
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(value = "HEAD"),
            argsOnly = true,
            index = 3
    )
    private float blockHalfDamage(
            float originalAmount,
            net.minecraft.server.world.ServerWorld world,
            DamageSource source
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity instanceof PlayerEntity player) {

            if (player.isBlocking()) {
                boolean hasAttacker = source.getAttacker() != null;

                if (hasAttacker) {
                    // Apply 50% damage reduction
                    System.out.println("Weapon Block: Applied 50% reduction. Original: " + originalAmount);
                    return originalAmount * 0.5F;
                }
            }
        }
        return originalAmount;
    }
}