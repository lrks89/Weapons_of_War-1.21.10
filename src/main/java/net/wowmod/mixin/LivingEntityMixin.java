package net.wowmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.wowmod.util.IParryPlayer;
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

                    // --- PARRY MECHANIC CHECK ---
                    IParryPlayer parryPlayer = (IParryPlayer) player;
                    long lastBlockTime = parryPlayer.wowmod_getLastParryTime(); // Retrieve time

                    long currentTime = world.getTime();
                    long timeDelta = currentTime - lastBlockTime;

                    final int PARRY_WINDOW_TICKS = 5; // e.g., 0.25 seconds

                    if (timeDelta <= PARRY_WINDOW_TICKS) {
                        // FULL PARRY SUCCESS: 100% damage blocked (return 0)
                        System.out.println("PARRY SUCCESS! 100% damage blocked.");
                        return 0.0F;
                    }
                    // --- END PARRY MECHANIC ---

                    // REGULAR BLOCK (FALLBACK)
                    System.out.println("REGULAR BLOCK: Applied 50% damage reduction.");
                    return originalAmount * 0.5F;
                }
            }
        }

        return originalAmount;
    }
}