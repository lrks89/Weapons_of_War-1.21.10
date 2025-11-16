package net.wowmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.wowmod.WeaponsOfWar; // REFINEMENT: Added import for Logger
import net.wowmod.item.custom.ParryShieldItem;
import net.wowmod.item.custom.WeaponItem;
import net.wowmod.util.IParryPlayer;
import net.wowmod.util.IParryStunnedEntity;
import org.slf4j.Logger; // REFINEMENT: Added import for Logger
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique; // REFINEMENT: Added import
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


// REFINEMENT: The mixin must implement the interface it's adding!
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityBlockingMixin implements IParryPlayer {

    // REFINEMENT: Added Logger
    @Unique
    private static final Logger LOGGER = WeaponsOfWar.LOGGER;

    // --- REFINEMENT: Added the fields and methods for IParryPlayer ---
    @Unique
    private long wowmod_lastParryTime = 0L;

    @Override
    public long wowmod_getLastParryTime() {
        return this.wowmod_lastParryTime;
    }

    @Override
    public void wowmod_setLastParryTime(long time) {
        this.wowmod_lastParryTime = time;
    }
    // --- End IParryPlayer implementation ---

    // --- CONSTANTS (moved from other class) ---
    private static final int PARRY_WINDOW_TICKS = 5;
    private static final int PARRIED_STUN_DURATION = 20; // 1 second
    private static final int AXE_COOLDOWN_DURATION = 100; // 5 seconds (20 ticks per second)

    // --- 1. BLOCKING CHECK ---
    // REFINEMENT: This mixin was moved to LivingEntityBlockingMixin

    // --- 2A. PERFECT PARRY and PARRIED STUN MECHANICS ---
    // This mixin is fine, as it targets a method on PlayerEntity
    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void wowmod_parryCancel(
            ServerWorld serverWorld,
            DamageSource source,
            float originalAmount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (!player.isBlocking()) {
            return;
        }

        IParryPlayer parryPlayer = (IParryPlayer) player;
        long timeDelta = serverWorld.getTime() - parryPlayer.wowmod_getLastParryTime();

        // Check if it's a "Perfect Parry"
        if (timeDelta <= PARRY_WINDOW_TICKS) {
            Entity attacker = source.getAttacker();

            if (attacker instanceof LivingEntity livingAttacker) {
                boolean isParryShield = player.getActiveItem().getItem() instanceof ParryShieldItem;

                if (isParryShield) {
                    // ParryShield Sound
                    serverWorld.playSound(
                            null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS,
                            1.0F, 1.2F + serverWorld.random.nextFloat() * 0.1F
                    );
                } else {
                    //WeaponItem Sound
                    serverWorld.playSound(
                            null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS,
                            1.2F, 1.3F + serverWorld.random.nextFloat() * 0.1F
                    );
                }

                // Don't stun or knock back on projectile parries
                if (!(source.getSource() instanceof ProjectileEntity)) {

                    // Stun the attacker
                    if (livingAttacker instanceof IParryStunnedEntity stunnedAttacker) {
                        stunnedAttacker.wowmod_setStunTicks(PARRIED_STUN_DURATION);
                    }

                    // Apply knockback
                    if (isParryShield) {
                        Vec3d knockbackVector = player.getRotationVector().negate().normalize();
                        livingAttacker.takeKnockback(1.5, knockbackVector.x, knockbackVector.z);
                    } else {
                        Vec3d knockbackVector = player.getRotationVector().negate().normalize();
                        livingAttacker.takeKnockback(0.5, knockbackVector.x, knockbackVector.z);
                    }
                }

                // Cancel all damage
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }

    // --- 2B. REGULAR BLOCK & 5. COUNTER ATTACK ---
    // This mixin is fine, as it targets a method on PlayerEntity
    @ModifyVariable(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(value = "HEAD"),
            argsOnly = true,
            index = 3
    )
    private float wowmod_blockAndCounterLogic(
            float originalAmount,
            ServerWorld world,
            DamageSource source
    ) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        Entity attacker = source.getAttacker();

        // --- 5. COUNTER ATTACK LOGIC ---
        // First, check if the attacker is a player and the target is stunned
        if (attacker instanceof PlayerEntity && player instanceof IParryStunnedEntity stunnedTarget) {
            if (stunnedTarget.wowmod_getStunTicks() > 0) {
                final float COUNTER_MULTIPLIER = 1.5F;
                return originalAmount * COUNTER_MULTIPLIER;
            }
        }

        // --- 2B. REGULAR BLOCK LOGIC ---
        // If not a counter-attack, check for a regular block
        if (!player.isBlocking()) { return originalAmount; }

        IParryPlayer parryPlayer = (IParryPlayer) player;
        long timeDelta = world.getTime() - parryPlayer.wowmod_getLastParryTime();

        // Make sure we are *not* in the perfect parry window
        if (timeDelta > PARRY_WINDOW_TICKS) {
            ItemStack activeStack = player.getActiveItem();
            Item activeItem = activeStack.getItem();

            boolean isParryShield = activeItem instanceof ParryShieldItem;
            boolean isWeaponItem = activeItem instanceof WeaponItem;

            if (isParryShield || isWeaponItem) {

                // Damage the item
                if (!world.isClient() && !source.isIn(DamageTypeTags.BYPASSES_SHIELD)) {
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        // REFINEMENT 2: Fixed variable name from 'serverWorld' to 'world'
                        activeStack.damage( 1, world, serverPlayer, (item) -> {}
                        );
                    }
                }

                // Axe Disable Shield Logic
                if (attacker instanceof LivingEntity livingAttacker) {
                    if (livingAttacker.getMainHandStack().isIn(ItemTags.AXES)) {
                        if (!world.isClient()) {
                            // REFINEMENT 1: Cooldown is on the ItemStack ('activeStack'), not the Item ('activeItem')
                            player.getItemCooldownManager().set(activeStack, AXE_COOLDOWN_DURATION);

                            if (player instanceof ServerPlayerEntity serverPlayer) {
                                serverPlayer.stopUsingItem();
                            }

                            if (isParryShield) {
                                world.playSound(
                                        null, player.getX(), player.getY(), player.getZ(),
                                        SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS,
                                        1.0F, 0.8F + world.random.nextFloat() * 0.4F);
                                world.playSound(
                                        null, player.getX(), player.getY(), player.getZ(),
                                        SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS,
                                        1.0F, 0.8F + world.random.nextFloat() * 0.4F);
                            }
                            else if (isWeaponItem) {
                                world.playSound(
                                        null, player.getX(), player.getY(), player.getZ(),
                                        SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS,
                                        1.0F, 0.8F + world.random.nextFloat() * 0.4F);
                            }
                        }

                        // --- REFINEMENT: Shield break logic (knockback removed) ---
                        if (isParryShield) {

                            // We still need to stop the player from using the shield
                            if (player instanceof ServerPlayerEntity serverPlayer) {
                                serverPlayer.stopUsingItem();
                            }
                        }
                        // --- END REFINEMENT ---

                        // Still block the axe damage, but apply cooldown
                        return isParryShield ? 0.0F : originalAmount * 0.5F;
                    }
                }

                // Standard Block Reduction Logic
                if (isParryShield) {
                    world.playSound(
                            null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS,
                            1.0F, 1.0F + world.random.nextFloat() * 0.1F
                    );
                    return 0.0F; // Block 100%
                } else if (isWeaponItem) {
                    world.playSound(
                            null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS,
                            1.2F, 1.1F + world.random.nextFloat() * 0.1F
                    );
                    return originalAmount * 0.5F; // Block 50%
                }
            }
        }

        // Default: no change to damage
        return originalAmount;
    }

    // --- 3. SUPPRESS KNOCKBACK ---
    // REFINEMENT: This mixin was moved to LivingEntityBlockingMixin

    // --- 5B. COUNTER ATTACK PARTICLES ---
    // This mixin is fine, as it targets a method on PlayerEntity
    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(value = "RETURN", ordinal = 1)
    )
    private void wowmod_spawnCounterCritParticles(
            ServerWorld world,
            DamageSource source,
            float originalAmount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ()) { return; } // Damage was not successful

        LivingEntity target = (LivingEntity) (Object) this;
        Entity attacker = source.getAttacker();

        if (!(attacker instanceof PlayerEntity)) { return; }
        if (!(target instanceof IParryStunnedEntity stunnedTarget) || stunnedTarget.wowmod_getStunTicks() <= 0) {
            return;
        }

        // Spawn many CRIT particles
        world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.9D), target.getZ(),
                10, 0.5D, 0.5D, 0.5D, 0.1D
        );
    }
}