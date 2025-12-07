package net.wowmod.networking;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.wowmod.util.IAnimatedPlayer;

public class LongJumpHandler {

    public static void performLongJump(PlayerEntity player) {
        if (player == null || !player.isOnGround()) return;

        IAnimatedPlayer animatedPlayer = (IAnimatedPlayer) player;
        if (animatedPlayer.wowmod$getLongJumpCooldown() > 0) return;

        // --- Calculation ---
        float yaw = player.getYaw();
        double rad = Math.toRadians(yaw);
        double forwardX = -Math.sin(rad);
        double forwardZ = Math.cos(rad);

        // Power settings
        double horizontalForce = 0.85;
        double verticalForce = 0.45;

        Vec3d jumpVec = new Vec3d(forwardX * horizontalForce, verticalForce, forwardZ * horizontalForce);

        player.setVelocity(jumpVec);
        player.velocityDirty = true;

        // FIX: Explicitly sync velocity to the client player to prevent rubber-banding
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayer));
        }

        // --- Effects ---
        animatedPlayer.wowmod$setLongJumpCooldown(20); // 1 second cooldown
        player.fallDistance = 0;
        player.addExhaustion(0.5f);

        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 0.5f);

        if (player.getEntityWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY(), player.getZ(),
                    5, 0.2, 0.1, 0.2, 0.1);
        }
    }
}