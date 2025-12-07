package net.wowmod.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class SlimedEffect extends StatusEffect {
    public SlimedEffect() {
        super(StatusEffectCategory.HARMFUL, 0x55FF55); // Slime Green
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
        if (entity instanceof PlayerEntity player) {
            // Force player to lower shield/weapon if they are currently blocking
            if (player.isBlocking()) {
                player.stopUsingItem();
            }
        }
        return true;
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        // Run every tick to ensure blocking is constantly suppressed
        return true;
    }
}