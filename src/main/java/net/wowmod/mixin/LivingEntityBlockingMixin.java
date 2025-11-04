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
import net.minecraft.entity.projectile.ProjectileEntity;
import net.wowmod.item.custom.WeaponItem;
import net.wowmod.util.IParryPlayer;
import net.wowmod.util.IParryStunnedEntity; // Note: You should update the methods in this interface
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityBlockingMixin implements IParryStunnedEntity {

    // PARRY STUN TIMER AND INTERFACE IMPLEMENTATION
    @Unique
    private int wowmod_parriedStunTicks = 0;

    @Override
    public int wowmod_getStunTicks() {
        // NOTE: This must match the method name in your IStunnedEntity interface
        return this.wowmod_parriedStunTicks;
    }

    @Override
    public void wowmod_setStunTicks(int ticks) {
        // NOTE: This must match the method name in your IStunnedEntity interface
        this.wowmod_parriedStunTicks = ticks;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void wowmod_applyParriedStunTick(CallbackInfo ci) {
        if (this.wowmod_parriedStunTicks > 0) {
            LivingEntity entity = (LivingEntity) (Object) this;
            if (entity instanceof PlayerEntity) {
                return;
            }

            this.wowmod_parriedStunTicks--;

            if (!entity.getEntityWorld().isClient()) {
                ServerWorld serverWorld = (ServerWorld) entity.getEntityWorld();

                // Spawn particles
                serverWorld.spawnParticles(
                        ParticleTypes.CRIT,
                        entity.getX(),
                        entity.getBodyY(0.9D), // Still positioned around the head/upper body
                        entity.getZ(),
                        // Count: Reduced significantly for a less busy burst
                        1,
                        // Delta X/Y/Z: Keep a moderate spread to make it an impact, not just a single point
                        0.2D,
                        0.2D,
                        0.2D,
                        // Speed/Velocity: A moderate speed for a quick, noticeable outward burst
                        0.15D
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

    // 2A. PERFECT PARRY and PARRIED STUN MECHANICS
    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void wowmod_parryCancel(
            net.minecraft.server.world.ServerWorld serverWorld,
            DamageSource source,
            float originalAmount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof PlayerEntity player) || !player.isBlocking()) { return; }

        IParryPlayer parryPlayer = (IParryPlayer) player;
        long timeDelta = serverWorld.getTime() - parryPlayer.wowmod_getLastParryTime();
        final int PARRY_WINDOW_TICKS = 5;

        if (timeDelta <= PARRY_WINDOW_TICKS) {
            Entity attacker = source.getAttacker();

            if (attacker instanceof LivingEntity livingAttacker) {

                if (!(source.getSource() instanceof ProjectileEntity)) {
                    final int PARRIED_STUN_DURATION = 30; // 1.5 seconds (30 ticks)

                    if (livingAttacker instanceof IParryStunnedEntity stunnedAttacker) {
                        stunnedAttacker.wowmod_setStunTicks(PARRIED_STUN_DURATION);
                    }

                    livingAttacker.setVelocity(Vec3d.ZERO);
                    livingAttacker.setAttacker(null);
                }
            }

            serverWorld.playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.PLAYERS,
                    1.5F, 1.5F + serverWorld.random.nextFloat() * 0.2F
            );

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
        if (!(entity instanceof PlayerEntity)) return;

        if (cir.getReturnValueZ() && ((PlayerEntity) entity).isBlocking()) {
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

    // 5. COUNTER ATTACK DAMAGE (1.5x damage against parried-stunned targets)
    @ModifyVariable(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(value = "HEAD"),
            argsOnly = true,
            index = 3
    )
    private float wowmod_counterAttackDamage(
            float originalAmount,
            net.minecraft.server.world.ServerWorld world,
            DamageSource source
    ) {

        LivingEntity target = (LivingEntity) (Object) this;
        if (!(target instanceof IParryStunnedEntity stunnedTarget) || stunnedTarget.wowmod_getStunTicks() <= 0) {
            return originalAmount;
        }

        Entity attacker = source.getAttacker();
        if (!(attacker instanceof PlayerEntity)) {
            return originalAmount;
        }

        final float COUNTER_MULTIPLIER = 1.5F;
        return originalAmount * COUNTER_MULTIPLIER;
    }
    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(value = "RETURN", ordinal = 1) // Injects after the main damage logic has run
    )

    // 6. COUNTER ATTACK PARTICLES
    private void wowmod_spawnCounterCritParticles(
            net.minecraft.server.world.ServerWorld world,
            DamageSource source,
            float originalAmount,
            CallbackInfoReturnable<Boolean> cir) {

        if (!cir.getReturnValueZ()) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        Entity attacker = source.getAttacker();

        if (!(attacker instanceof PlayerEntity)) {
            return;
        }

        if (!(target instanceof IParryStunnedEntity stunnedTarget) || stunnedTarget.wowmod_getStunTicks() <= 0) {
            return;
        }

        if (!world.isClient()) {
            ServerWorld serverWorld = (ServerWorld) world;

            // Spawn many CRIT particles (can adjust count and spread)
            serverWorld.spawnParticles(
                    ParticleTypes.CRIT,
                    target.getX(),
                    target.getBodyY(0.9D),
                    target.getZ(),
                    10, // Count of particles
                    0.5D, // Spread X
                    0.5D, // Spread Y
                    0.5D, // Spread Z
                    0.1D  // Speed
            );
        }
    }
}