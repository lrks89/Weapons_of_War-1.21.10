package net.wowmod.logic;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.particle.ParticleTypes;
import net.wowmod.WeaponsOfWar;
import net.wowmod.item.custom.IParryItem;
import net.wowmod.item.custom.ParryShieldItem;
import net.wowmod.item.custom.ParryWeaponItem;
import net.wowmod.util.IParryPlayer;
import net.wowmod.util.IParryStunnedEntity;

public class ParryLogic {

    private static final int PARRIED_STUN_DURATION = 20; // 1 second
    private static final int AXE_COOLDOWN_DURATION = 100; // 5 seconds
    private static final float COUNTER_MULTIPLIER = 1.5F;

    /**
     * Ticks down the stun timer and applies effects (particles, freezing).
     */
    public static void tickStunnedEntity(LivingEntity entity, int stunTicks) {
        if (stunTicks <= 0) return;

        // Effects only apply to non-players
        if (!(entity instanceof PlayerEntity) && !entity.getEntityWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) entity.getEntityWorld();

            // Stunned Particles
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

    /**
     * Checks for "Perfect Parry" and applies effects (Sound, Stun, Knockback).
     * @return true if the damage should be completely cancelled (successful parry).
     */
    public static boolean attemptPerfectParry(PlayerEntity player, ServerWorld world, DamageSource source) {
        if (!player.isBlocking()) return false;

        // Ensure unblockable damage (Fall, Magic, Void) is ignored
        if (source.isIn(DamageTypeTags.BYPASSES_SHIELD)) return false;

        ItemStack activeStack = player.getActiveItem();
        Item activeItem = activeStack.getItem();

        if (!(activeItem instanceof IParryItem parryItem)) return false;

        IParryPlayer parryPlayer = (IParryPlayer) player;
        long timeDelta = world.getTime() - parryPlayer.wowmod_getLastParryTime();

        // Check Parry Window
        if (timeDelta <= parryItem.getParryWindow()) {
            Entity attacker = source.getAttacker();

            if (attacker instanceof LivingEntity livingAttacker) {
                boolean isParryShield = activeItem instanceof ParryShieldItem;

                // Play Sound
                if (isParryShield) {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS,
                            1.0F, 1.2F + world.random.nextFloat() * 0.1F);
                } else {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS,
                            1.2F, 1.3F + world.random.nextFloat() * 0.1F);
                }

                // Logic for non-projectile attacks
                if (!(source.getSource() instanceof ProjectileEntity)) {
                    // Apply Stun
                    if (livingAttacker instanceof IParryStunnedEntity stunnedAttacker) {
                        stunnedAttacker.wowmod_setStunTicks(PARRIED_STUN_DURATION);
                    }

                    // Apply Knockback
                    double kbStrength = isParryShield ? 1.5 : 0.5;
                    Vec3d knockbackVector = player.getRotationVector().negate().normalize();
                    livingAttacker.takeKnockback(kbStrength, knockbackVector.x, knockbackVector.z);
                }

                return true; // Cancel damage
            }
        }
        return false;
    }

    /**
     * Calculates the modified damage for standard blocking, shield breaks, and counter-attacks.
     */
    public static float calculateModifiedDamage(PlayerEntity player, ServerWorld world, DamageSource source, float originalAmount) {
        // Ensure unblockable damage (Fall, Magic, Void) is ignored
        if (source.isIn(DamageTypeTags.BYPASSES_SHIELD)) return originalAmount;

        Entity attacker = source.getAttacker();

        // 1. Counter Attack Check (Player attacking a stunned target)
        if (attacker instanceof PlayerEntity && player instanceof IParryStunnedEntity stunnedTarget) {
            if (stunnedTarget.wowmod_getStunTicks() > 0) {
                return originalAmount * COUNTER_MULTIPLIER;
            }
        }

        // 2. Regular Block Check
        if (!player.isBlocking()) return originalAmount;

        ItemStack activeStack = player.getActiveItem();
        Item activeItem = activeStack.getItem();

        if (!(activeItem instanceof IParryItem parryItem)) return originalAmount;

        IParryPlayer parryPlayer = (IParryPlayer) player;
        long timeDelta = world.getTime() - parryPlayer.wowmod_getLastParryTime();

        // Ensure we are outside the perfect parry window (regular block)
        if (timeDelta > parryItem.getParryWindow()) {
            boolean isParryShield = activeItem instanceof ParryShieldItem;
            boolean isWeaponItem = activeItem instanceof ParryWeaponItem;

            if (isParryShield || isWeaponItem) {
                // Durability Damage
                if (!world.isClient()) {
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        activeStack.damage(1, world, serverPlayer, (item) -> {});
                    }
                }

                boolean shieldBroken = false;

                // Axe Disable Logic
                if (attacker instanceof LivingEntity livingAttacker) {
                    if (livingAttacker.getMainHandStack().isIn(ItemTags.AXES)) {
                        if (!world.isClient()) {
                            player.getItemCooldownManager().set(activeStack, AXE_COOLDOWN_DURATION);
                            if (player instanceof ServerPlayerEntity serverPlayer) {
                                serverPlayer.stopUsingItem();
                            }
                            playShieldBreakSound(player, world, isParryShield);
                        }
                        // We mark it as broken so we don't play the standard block sound below
                        shieldBroken = true;

                        // We DO NOT return here anymore.
                        // We fall through to allow the weapon/shield to block the specific amount defined in its stats.
                        // This fixes the bug where axes forced 50% damage regardless of the weapon's stats.
                    }
                }

                // Standard Reduction
                // Only play block sound if we didn't just play the break sound
                if (!shieldBroken) {
                    playBlockSound(player, world, isParryShield, isWeaponItem);
                }

                // Use the item's actual reduction stats
                return originalAmount * (1.0F - parryItem.getDamageReduction());
            }
        }

        return originalAmount;
    }

    private static void playShieldBreakSound(PlayerEntity player, ServerWorld world, boolean isShield) {
        if (isShield) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 0.8F + world.random.nextFloat() * 0.4F);
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0F, 0.8F + world.random.nextFloat() * 0.4F);
    }

    private static void playBlockSound(PlayerEntity player, ServerWorld world, boolean isShield, boolean isWeapon) {
        if (isShield) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 1.0F + world.random.nextFloat() * 0.1F);
        } else if (isWeapon) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1.2F, 1.1F + world.random.nextFloat() * 0.1F);
        }
    }
}