package net.wowmod.animation.player_animations.player_weapons;

import java.util.Map;
import net.minecraft.util.Identifier;
import net.wowmod.animation.player_animations.PlayerAnimationState;

public class WeaponAnimationConfig {
    // Maps state names (e.g. "idle", "jumping") to animation resource strings
    public Map<String, String> animations;

    public Identifier getAnimation(PlayerAnimationState state) {
        if (animations == null) return null;

        // 1. Macro Match
        if (state == PlayerAnimationState.JUMPING ||
                state == PlayerAnimationState.LANDING_IDLE ||
                state == PlayerAnimationState.LANDING_WALKING ||
                state == PlayerAnimationState.LANDING_SPRINTING) {

            String jumpingBase = animations.get("jumping");

            if (jumpingBase != null && !jumpingBase.isEmpty()) {
                // REMOVE "_default" from these strings.
                // The code effectively becomes: [ConfigName] + [StateSuffix]
                String suffix = switch (state) {
                    case JUMPING -> "_ascending-falling"; // Was "_ascending-falling_default"
                    case LANDING_IDLE -> "_landing_idle";
                    case LANDING_WALKING -> "_jumping_landing_walking";
                    case LANDING_SPRINTING -> "_jumping_landing_sprinting";
                    default -> "";
                };
                return Identifier.of(jumpingBase + suffix);
            }
        }

        String key = state.toString().toLowerCase();

        // Specific handling for attack_standing to map to "attack" in JSON if needed
        if (state == PlayerAnimationState.STANDING_ATTACK && !animations.containsKey("standing_attack") && animations.containsKey("attack")) {
            return Identifier.of(animations.get("attack"));
        }

        String animId = animations.get(key);

        if (animId != null && !animId.isEmpty()) {
            return Identifier.of(animId);
        }

        return null;
    }
}