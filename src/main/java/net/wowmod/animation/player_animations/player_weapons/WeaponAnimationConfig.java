package net.wowmod.animation.player_animations.player_weapons;

import java.util.Map;
import net.minecraft.util.Identifier;
import net.wowmod.animation.player_animations.PlayerAnimationState;

public class WeaponAnimationConfig {
    // Maps state names (e.g. "idle", "jumping") to animation resource strings
    public Map<String, String> animations;

    public Identifier getAnimation(PlayerAnimationState state) {
        if (animations == null) return null;

        String key = state.toString().toLowerCase();

        // --- NEW: Specific handling for attack_standing to map to "attack" in JSON if needed ---
        if (state == PlayerAnimationState.STANDING_ATTACK && !animations.containsKey("standing_attack") && animations.containsKey("attack")) {
            return Identifier.of(animations.get("attack"));
        }

        String animId = animations.get(key);

        // 1. Direct Match: If "jump_ascend" is defined explicitly, use it.
        if (animId != null && !animId.isEmpty()) {
            return Identifier.of(animId);
        }

        // 2. Macro Match: Handle "jumping" shorthand
        if (state == PlayerAnimationState.JUMPING ||
                state == PlayerAnimationState.LANDING_IDLE ||
                state == PlayerAnimationState.LANDING_WALKING ||
                state == PlayerAnimationState.LANDING_SPRINTING) {

            // Check if the user defined a generic "jumping" key
            String jumpingBase = animations.get("jumping");

            if (jumpingBase != null && !jumpingBase.isEmpty()) {
                // Construct the specific ID based on the state suffix
                String suffix = switch (state) {
                    case JUMPING -> "_ascending-falling_default";
                    case LANDING_IDLE -> "_landing_idle_default";
                    case LANDING_WALKING -> "_jumping_landing_walking_default";
                    case LANDING_SPRINTING -> "_jumping_landing_sprinting_default";
                    default -> "";
                };
                return Identifier.of(jumpingBase + suffix);
            }
        }

        return null;
    }
}