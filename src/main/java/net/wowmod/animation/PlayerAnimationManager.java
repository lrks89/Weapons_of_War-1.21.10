package net.wowmod.animation;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerAnimationManager {
    private static final Map<UUID, AnimationState> activeAnimations = new HashMap<>();

    public static void playAnimation(PlayerEntity player, Identifier animationId) {
        activeAnimations.put(player.getUuid(), new AnimationState(animationId, System.currentTimeMillis()));
    }

    public static AnimationState getState(PlayerEntity player) {
        return activeAnimations.get(player.getUuid());
    }

    public static class AnimationState {
        public final Identifier id;
        public final long startTime;

        public AnimationState(Identifier id, long startTime) {
            this.id = id;
            this.startTime = startTime;
        }
    }
}