package net.wowmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.wowmod.item.custom.ParryShieldItem;
import net.wowmod.item.custom.WeaponItem;
import net.wowmod.util.IParryPlayer;
import net.wowmod.util.IParryStunnedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = LivingEntity.class, priority = 1001)
public abstract class LivingEntityBlockingMixin implements IParryStunnedEntity {

    // --- STUN AND BLOCK CONSTANTS ---
    private static final int PARRY_WINDOW_TICKS = 5;
    private static final int PARRIED_STUN_DURATION = 20; // 1 second
    private static final int AXE_COOLDOWN_DURATION = 100; // 5 seconds (20 ticks per second)

    // --- PARRY STUN TIMER AND INTERFACE IMPLEMENTATION ---
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
        LivingEntity entity = (LivingEntity) (Object) this;

        if (this.wowmod_parriedStunTicks > 0) {
            if (entity instanceof PlayerEntity) {
                // Players aren't stunned, just skip particle effects
            } else {
                this.wowmod_parriedStunTicks--;

                if (!entity.getEntityWorld().isClient()) {
                    ServerWorld serverWorld = (ServerWorld) entity.getEntityWorld();
                    //Stunned Particles
                    serverWorld.spawnParticles(
                            ParticleTypes.CRIT, entity.getX(), entity.getBodyY(0.9D), entity.getZ(),
                            1, 0.2D, 0.2D, 0.2D, 0.15D
                    );
                }
                Vec3d currentVelocity = entity.getVelocity();
                entity.setVelocity(0.0, currentVelocity.y, 0.0);
                entity.setAttacker(null);
            }
        }
    }

    // --- 1. BLOCKING CHECK (Enables Blocking for WeaponItem) ---
    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void wowmod_checkCustomBlock(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object)this instanceof PlayerEntity player)) { return; }

        if (player.isUsingItem()) {

            // Check if the item is one of our custom blocking items (Weapon or ParryShield
            if (player.getActiveItem().getItem() instanceof WeaponItem
                    || player.getActiveItem().getItem() instanceof ParryShieldItem) {




                if (player.getActiveItem().getUseAction() == UseAction.BLOCK) {
                    cir.setReturnValue(true);
                    cir.cancel();
                }
            }
        }
    }

    // --- 2A. PERFECT PARRY and PARRIED STUN MECHANICS (Handles damage cancellation and parry effects) ---
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

        if (timeDelta <= PARRY_WINDOW_TICKS) {
            Entity attacker = source.getAttacker();

            if (attacker instanceof LivingEntity livingAttacker) {
                boolean isParryShield = player.getActiveItem().getItem() instanceof ParryShieldItem;

                if (!(source.getSource() instanceof ProjectileEntity)) {

                    if (livingAttacker instanceof IParryStunnedEntity stunnedAttacker) {
                        stunnedAttacker.wowmod_setStunTicks(PARRIED_STUN_DURATION);
                    }

                    if (isParryShield) {
                        //ParryShield
                        Vec3d knockbackVector = player.getRotationVector().negate().normalize();
                        livingAttacker.takeKnockback(1.5, knockbackVector.x, knockbackVector.z);

                        serverWorld.playSound(
                                null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS,
                                1.0F, 1.2F + serverWorld.random.nextFloat() * 0.1F
                        );
                    } else {
                        //WeaponItem
                        Vec3d knockbackVector = player.getRotationVector().negate().normalize();
                        livingAttacker.takeKnockback(0.5, knockbackVector.x, knockbackVector.z);

                        serverWorld.playSound(
                                null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS,
                                1.2F, 1.3F + serverWorld.random.nextFloat() * 0.1F
                        );
                    }
                }
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }

    // --- 2B. REGULAR BLOCK MECHANICS --
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

        if (timeDelta > PARRY_WINDOW_TICKS) {
            Entity attacker = source.getAttacker();
            ItemStack activeStack = player.getActiveItem(); // Get the ItemStack here
            Item activeItem = activeStack.getItem();

            boolean isParryShield = activeItem instanceof ParryShieldItem;
            boolean isWeaponItem = activeItem instanceof WeaponItem;

            if (isParryShield || isWeaponItem) {

                if (!world.isClient() && !source.isIn(DamageTypeTags.BYPASSES_SHIELD)) {
                    ServerWorld serverWorld = (ServerWorld) world;

                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        activeStack.damage( 1, serverWorld, serverPlayer, (item) -> {}
                        );
                    }
                }

                // Axe Disable Shield Logic
                if (attacker instanceof LivingEntity livingAttacker) {

                    if (livingAttacker.getMainHandStack().isIn(ItemTags.AXES)) {

                        if (!world.isClient()) {
                            player.getItemCooldownManager().set(player.getActiveItem(), AXE_COOLDOWN_DURATION);

                            if (player instanceof ServerPlayerEntity serverPlayer) {
                                serverPlayer.stopUsingItem();
                            }

                            world.playSound(
                                    null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS,
                                    1.0F, 0.8F + world.random.nextFloat() * 0.4F);
                        }

                        if (isParryShield) {
                            return 0.0F;
                        } else if (isWeaponItem) {
                            return originalAmount * 0.5F;
                        }
                    }
                }
            }
            // Standard Block Reduction Logic
            if (isParryShield) {
                world.playSound(
                        null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, // Standard Shield Block Sound
                        1.0F, 1.0F + world.random.nextFloat() * 0.1F
                );
                return 0.0F;

            } else if (isWeaponItem) {
                world.playSound(
                        null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, // Weapon Block Sound
                        1.2F, 1.1F + world.random.nextFloat() * 0.1F
                );
                return originalAmount * 0.5F;
            }
        }
        return originalAmount;
    }

    // --- 3. SUPPRESS FLINCH & KNOCKBACK---
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

        // If the damage method returned TRUE (damage was applied) AND the player is blocking,
        // we set the return value to FALSE to suppress the flinch animation/effect.
        if (cir.getReturnValueZ() && ((PlayerEntity) entity).isBlocking()) {
            cir.setReturnValue(false);
        }
    }

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

    // --- 5. COUNTER ATTACK ---
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
            at = @At(value = "RETURN", ordinal = 1)
    )
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

            // Spawn many CRIT particles
            serverWorld.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.9D), target.getZ(),
                    10, 0.5D, 0.5D, 0.5D, 0.1D
            );
        }
    }
}