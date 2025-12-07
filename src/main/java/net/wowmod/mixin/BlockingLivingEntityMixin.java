package net.wowmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.wowmod.WeaponsOfWar;
import net.wowmod.item.custom.ParryShieldItem;
import net.wowmod.item.custom.ParryWeaponItem;
import net.wowmod.util.IParryStunnedEntity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 1001)
public abstract class BlockingLivingEntityMixin implements IParryStunnedEntity {

    @Unique
    private static final Logger LOGGER = WeaponsOfWar.LOGGER;

    @Unique
    private int wowmod_parriedStunTicks = 0;

    // --- INTERFACE IMPLEMENTATIONS ---
    @Override
    public int wowmod_getStunTicks() {
        return this.wowmod_parriedStunTicks;
    }

    @Override
    public void wowmod_setStunTicks(int ticks) {
        this.wowmod_parriedStunTicks = ticks;
    }

    // --- STUN TICK LOGIC ---
    @Inject(method = "tick", at = @At("HEAD"))
    private void wowmod_applyStunAndDisableTick(CallbackInfo ci) {
        // REFINEMENT: Stun ticks should tick down for everyone.
        if (this.wowmod_parriedStunTicks > 0) {
            this.wowmod_parriedStunTicks--;

            LivingEntity entity = (LivingEntity) (Object) this;

            // The *effects* of being stunned only apply to non-players.
            if (!(entity instanceof PlayerEntity) && !entity.getEntityWorld().isClient()) {
                ServerWorld serverWorld = (ServerWorld) entity.getEntityWorld();

                //Stunned Particles
                serverWorld.spawnParticles(
                        ParticleTypes.CRIT, entity.getX(), entity.getBodyY(0.9D), entity.getZ(),
                        1, 0.2D, 0.2D, 0.2D, 0.15D
                );

                // Freeze entity movement (but allow gravity)
                Vec3d currentVelocity = entity.getVelocity();
                entity.setVelocity(0.0, currentVelocity.y, 0.0);
                entity.setAttacker(null);
            }
        }
    }

    // --- REFINEMENT: MOVED FROM PlayerEntityBlockingMixin ---
    // This mixin targets 'isBlocking', which is on LivingEntity
    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void wowmod_checkCustomBlock(CallbackInfoReturnable<Boolean> cir) {

        // We only want this logic to run for players
        if (!((Object)this instanceof PlayerEntity player)) {
            return;
        }

        if (player.isUsingItem()) {

            // Check if the item is one of our custom blocking items
            if (player.getActiveItem().getItem() instanceof ParryWeaponItem
                    || player.getActiveItem().getItem() instanceof ParryShieldItem) {

                if (player.getActiveItem().getUseAction() == UseAction.BLOCK) {
                    cir.setReturnValue(true);
                    cir.cancel();
                }
            }
        }
    }

    // --- REFINEMENT: MOVED FROM PlayerEntityBlockingMixin ---
    // This mixin targets 'takeKnockback', which is on LivingEntity
    @Inject(
            method = "takeKnockback(DDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void wowmod_cancelDefensiveKnockback(
            double strength, double x, double z, CallbackInfo ci
    ) {
        // We only want this logic to run for players
        if ((Object)this instanceof PlayerEntity player) {

            // REFINEMENT: Simplified logic.
            // If the player is blocking with one of our items, cancel ALL knockback.
            if (player.isBlocking()) {
                ci.cancel();
            }
        }
    }
}