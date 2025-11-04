package net.wowmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.consume.UseAction;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.particle.ParticleTypes;
import net.wowmod.item.custom.WeaponItem;
import net.wowmod.util.IParryPlayer;
import net.wowmod.util.IStunnedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityBlockingMixin implements IStunnedEntity {

    //PARRY STUN TIMER AND INTERFACE IMPLEMENTATION
    @Unique
    private int wowmod_stunTicks = 0;

    @Override
    public int wowmod_getStunTicks() {
        return this.wowmod_stunTicks;
    }

    @Override
    public void wowmod_setStunTicks(int ticks) {
        this.wowmod_stunTicks = ticks;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void wowmod_applyStunTick(CallbackInfo ci) {
        if (this.wowmod_stunTicks > 0) {
            LivingEntity entity = (LivingEntity) (Object) this;
            if (entity instanceof PlayerEntity) {
                return;
            }

            this.wowmod_stunTicks--;

            if (!entity.getEntityWorld().isClient()) {
                ServerWorld serverWorld = (ServerWorld) entity.getEntityWorld();

                // Spawn particles
                serverWorld.spawnParticles(
                        ParticleTypes.CRIT,
                        entity.getX(),
                        entity.getBodyY(0.9D),
                        entity.getZ(),
                        1,
                        0.2D,
                        0.2D,
                        0.2D,
                        0.0D
                );
            }
            entity.setVelocity(Vec3d.ZERO);
            entity.setAttacker(null);
        }
    }
    // 1. BLOCKING CHECK (Enables Blocking for WeaponItem)
    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void wowmod_checkCustomBlock(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object)this instanceof PlayerEntity player)) { return; }
        if (player.isUsingItem()) {
            if (player.getActiveItem().getItem() instanceof WeaponItem) {
                if (player.getActiveItem().getUseAction() == UseAction.BLOCK) {
                    cir.setReturnValue(true);
                    cir.cancel();
                }
            }
        }
    }

    // 2A. PERFECT PARRY and STUN MECHANICS
    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void wowmod_parryCancel(
            net.minecraft.server.world.ServerWorld serverWorld, // Changed name to avoid conflict
            DamageSource source,
            float originalAmount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof PlayerEntity player) || !player.isBlocking()) { return; }

        IParryPlayer parryPlayer = (IParryPlayer) player;
        // Use the passed-in serverWorld instance for time
        long timeDelta = serverWorld.getTime() - parryPlayer.wowmod_getLastParryTime();
        final int PARRY_WINDOW_TICKS = 5;

        if (timeDelta <= PARRY_WINDOW_TICKS) {
            // **PERFECT PARRY SUCCESS**

            Entity attacker = source.getAttacker();

            // --- TIMED STUN APPLICATION ---
            if (attacker instanceof LivingEntity livingAttacker) {
                final int STUN_DURATION = 30; // 1.5 seconds (30 ticks)

                // Set the countdown timer on the attacker
                if (livingAttacker instanceof IStunnedEntity stunnedAttacker) {
                    stunnedAttacker.wowmod_setStunTicks(STUN_DURATION);
                }

                // Immediately apply stun effects for the first tick
                livingAttacker.setVelocity(Vec3d.ZERO);
                livingAttacker.setAttacker(null);
            }
            // ------------------------------

            serverWorld.playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.PLAYERS,
                    1.5F, 1.5F + serverWorld.random.nextFloat() * 0.2F
            );

            // Cancel the method (No damage, no red flash, no flinch)
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    // 2B. BLOCK MECHANICS
    @ModifyVariable(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(value = "HEAD"),
            argsOnly = true,
            index = 3
    )
    private float wowmod_regularBlockReduction(
            float originalAmount,
            net.minecraft.server.world.ServerWorld world,
            DamageSource source
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof PlayerEntity player) || !player.isBlocking()) { return originalAmount; }

        IParryPlayer parryPlayer = (IParryPlayer) player;
        long timeDelta = world.getTime() - parryPlayer.wowmod_getLastParryTime();
        final int PARRY_WINDOW_TICKS = 5;

        if (timeDelta > PARRY_WINDOW_TICKS) {
            world.playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.PLAYERS,
                    1.5F, 1.0F + world.random.nextFloat() * 0.1F
            );
            return originalAmount * 0.5F;
        }

        return originalAmount;
    }

    // 3. SUPPRESS FLINCH
    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("RETURN"),
            cancellable = true)
    private void wowmod_suppressFlinchOnBlock(
            net.minecraft.server.world.ServerWorld world,
            DamageSource source,
            float originalAmount,
            CallbackInfoReturnable<Boolean> cir) {

        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof PlayerEntity player)) return;

        if (cir.getReturnValueZ() && player.isBlocking()) {
            cir.setReturnValue(false);
        }
    }

    // 4. SUPPRESS FLINCH KNOCKBACK
    @Inject(
            method = "takeKnockback(DDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void wowmod_cancelDefensiveKnockback(
            double strength, double x, double z, CallbackInfo ci
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof PlayerEntity player) {
            if (player.isBlocking()) {
                ci.cancel();
            }
        }
    }
}