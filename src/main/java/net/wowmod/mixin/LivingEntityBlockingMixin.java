package net.wowmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.consume.UseAction;
import net.wowmod.item.custom.WeaponItem;
import net.wowmod.util.IParryPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityBlockingMixin {

    //Injects Blocking Mechanic into game logic
    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void wowmod_checkCustomBlock(CallbackInfoReturnable<Boolean> cir) {

        if (!((Object)this instanceof PlayerEntity player)) {
            return;
        }

        if (player.isUsingItem()) {
            if (player.getActiveItem().getItem() instanceof WeaponItem) {
                if (player.getActiveItem().getUseAction() == UseAction.BLOCK) {
                    cir.setReturnValue(true);
                    cir.cancel();
                }
            }
        }
    }

    //Damage Modification & Parry Logic
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

            if (player.isBlocking()) { // This now correctly uses the result of wowmod_checkCustomBlock
                Entity attacker = source.getAttacker();

                if (attacker != null) {

                    // --- PARRY MECHANIC CHECK ---
                    IParryPlayer parryPlayer = (IParryPlayer) player;
                    long lastBlockTime = parryPlayer.wowmod_getLastParryTime();

                    long currentTime = world.getTime();
                    long timeDelta = currentTime - lastBlockTime;

                    final int PARRY_WINDOW_TICKS = 5;

                    if (timeDelta <= PARRY_WINDOW_TICKS) {
                        // FULL PARRY SUCCESS: 100% damage blocked
                        applyParryKnockback(attacker, player); // Assume this helper method exists
                        return 0.0F;
                    }
                    // --- END PARRY MECHANIC ---

                    // REGULAR BLOCK (FALLBACK)
                    return originalAmount * 0.5F;
                }
            }
        }

        return originalAmount;
    }

    // You would include the private applyParryKnockback method here if you added it previously
    private void applyParryKnockback(Entity attacker, PlayerEntity player) {
        // ... (implementation details from previous response)
    }
}
