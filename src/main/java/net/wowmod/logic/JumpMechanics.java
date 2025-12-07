package net.wowmod.logic;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.wowmod.util.IAnimatedPlayer;

public class JumpMechanics {

    private static final float MAX_CHARGE_MULTIPLIER = 0.35f; // +35% height at max charge

    /**
     * Applies the high jump boost based on the charge stored in the player.
     * @param player The player jumping
     */
    public static void applyHighJumpBoost(PlayerEntity player) {
        if (!(player instanceof IAnimatedPlayer animatedPlayer)) return;

        float charge = animatedPlayer.wowmod$getHighJumpCharge();

        if (charge > 0.0f) {
            Vec3d velocity = player.getVelocity();

            // Logic: 1.0x (base) + (charge% * extra)
            // Charge is 0.0 to 1.0
            float multiplier = 1.0f + (charge * MAX_CHARGE_MULTIPLIER);

            player.setVelocity(velocity.x, velocity.y * multiplier, velocity.z);
            player.velocityDirty = true;

            // Consume charge
            animatedPlayer.wowmod$setHighJumpCharge(0.0f);
        }
    }
}