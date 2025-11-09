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

    // Stun and Block Constants
    private static final int PARRY_WINDOW_TICKS = 5;
    private static final int PARRIED_STUN_DURATION = 20; // 1 second
    private static final int AXE_COOLDOWN_DURATION = 100; // 5 seconds (20 ticks per second)

    // PARRY STUN TIMER AND INTERFACE IMPLEMENTATION
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

    @Inject(method = "tick", at = @At("HEAD"))
    private void wowmod_applyStunAndDisableTick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Stun Tick Logic (for all LivingEntities)
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



    // 1. BLOCKING CHECK (Enables Blocking for WeaponItem)
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



    // 2A. PERFECT PARRY and PARRIED STUN MECHANICS (Handles damage cancellation and parry effects)
    // NOTE: AXE CHECK IS NOT HERE. A PERFECT PARRY ALWAYS WORKS.
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
        // CRITICAL CHECK: Player must be blocking to proceed!
        if (!(entity instanceof PlayerEntity player) || !player.isBlocking()) { return; }

        IParryPlayer parryPlayer = (IParryPlayer) player;
        long timeDelta = serverWorld.getTime() - parryPlayer.wowmod_getLastParryTime();

        // If within the perfect parry window
        if (timeDelta <= PARRY_WINDOW_TICKS) {

            Entity attacker = source.getAttacker();

            if (attacker instanceof LivingEntity livingAttacker) {
                // Determine if a shield or weapon was used for the parry
                boolean isParryShield = player.getActiveItem().getItem() instanceof ParryShieldItem;

                // Apply Stun and Knockback if not a projectile
                if (!(source.getSource() instanceof ProjectileEntity)) {

                    if (livingAttacker instanceof IParryStunnedEntity stunnedAttacker) {
                        stunnedAttacker.wowmod_setStunTicks(PARRIED_STUN_DURATION);
                    }

                    if (isParryShield) {
                        // Apply knockback to the attacker
                        Vec3d knockbackVector = player.getRotationVector().negate().normalize();
                        livingAttacker.takeKnockback(1.0, knockbackVector.x, knockbackVector.z);

                        // ✅ SHIELD PARRY SOUND EFFECT
                        serverWorld.playSound(
                                null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS,
                                1.0F, 1.2F + serverWorld.random.nextFloat() * 0.1F
                        );
                    } else {
                        // WeaponItem parry logic
                        livingAttacker.setVelocity(Vec3d.ZERO);
                        livingAttacker.setAttacker(null);

                        // Original Weapon Parry Sound Effect
                        serverWorld.playSound(
                                null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS,
                                1.2F, 1.3F + serverWorld.random.nextFloat() * 0.1F
                        );
                    }
                }

                // Cancel damage entirely due to successful parry (100% block)
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }



    // 2B. REGULAR BLOCK MECHANICS (Differentiates between Shield 100% and Weapon 50% block)
    // AXE CHECK IS HERE - IT ONLY AFFECTS THE REGULAR BLOCK
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

        // If parry window missed (regular block)
        if (timeDelta > PARRY_WINDOW_TICKS) {

            Entity attacker = source.getAttacker();
            ItemStack activeStack = player.getActiveItem(); // Get the ItemStack here
            Item activeItem = activeStack.getItem();

            // DECLARE AND INITIALIZE THE VARIABLES HERE
            boolean isParryShield = activeItem instanceof ParryShieldItem;
            boolean isWeaponItem = activeItem instanceof WeaponItem;

            // Check if it's a blocking item
            if (isParryShield || isWeaponItem) {

                // --- FIX: DURABILITY DAMAGE LOGIC MOVED UP ---
                // Standard Block/Durability Damage Logic (runs before axe check)
                if (!world.isClient() && !source.isIn(DamageTypeTags.BYPASSES_SHIELD)) {
                    // The new .damage() method requires ServerWorld and ServerPlayerEntity
                    ServerWorld serverWorld = (ServerWorld) world;
                    // Check if it's a ServerPlayerEntity before casting and damaging
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        activeStack.damage( // Use the activeStack here
                                1, // int amount
                                serverWorld, // ServerWorld world
                                serverPlayer, // ServerPlayerEntity user
                                (item) -> {} // Additional break logic lambda
                        );
                    }
                }
                // ---------------------------------------------

                // ⚔️ AXE DISABLE SHIELD/WEAPON LOGIC
                if (attacker instanceof LivingEntity livingAttacker) {
                    // Check if the attacker is holding a recognized axe
                    if (livingAttacker.getMainHandStack().isIn(ItemTags.AXES)) {

                        if (!world.isClient()) {

                            // *** MODIFIED LOGIC: Apply cooldown to the active item's ItemCooldownManager ***
                            player.getItemCooldownManager().set(player.getActiveItem(), AXE_COOLDOWN_DURATION);

                            // 🛑 FIX: Cancel the block animation/use action on the server
                            if (player instanceof ServerPlayerEntity serverPlayer) {
                                serverPlayer.stopUsingItem();
                            }

                            world.playSound(
                                    null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS,
                                    1.0F, 0.8F + world.random.nextFloat() * 0.4F);
                        }

                        // Return the reduced/zero damage amount after durability damage and cooldown are applied
                        if (isParryShield) {
                            return 0.0F;
                        } else if (isWeaponItem) {
                            return originalAmount * 0.5F;
                        }
                    }
                }
            }
            // ------------------------------------

            // Standard Block Reduction Logic (runs if not disabled by axe)
            if (isParryShield) {
                // ✅ Parry Shield: BLOCK 100% of damage
                world.playSound(
                        null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, // Standard Shield Block Sound
                        1.0F, 1.0F + world.random.nextFloat() * 0.1F
                );
                return 0.0F;

            } else if (isWeaponItem) { // Added explicit check for WeaponItem
                // ⚔️ WeaponItem: BLOCK 50% of damage
                world.playSound(
                        null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, // Weapon Block Sound
                        1.2F, 1.1F + world.random.nextFloat() * 0.1F
                );
                return originalAmount * 0.5F;
            }
        }

        // Return original amount if in the parry window (2A handles full cancellation)
        return originalAmount;
    }



    // 3. SUPPRESS FLINCH (Prevents flinching on successful block)
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

    // 4. SUPPRESS FLINCH KNOCKBACK (Prevents knockback on block)
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



    // 6. COUNTER ATTACK PARTICLES
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