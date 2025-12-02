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
        String animId = animations.get(key);

        // 1. Direct Match: If "jump_ascend" is defined explicitly, use it.
        if (animId != null && !animId.isEmpty()) {
            return Identifier.of(animId);
        }

        // 2. Macro Match: Handle "jumping" shorthand
        if (state == PlayerAnimationState.JUMP_ASCEND ||
                state == PlayerAnimationState.JUMP_DESCENT ||
                state == PlayerAnimationState.LANDING_IDLE ||
                state == PlayerAnimationState.LANDING_WALKING ||
                state == PlayerAnimationState.LANDING_SPRINTING) {

            // Check if the user defined a generic "jumping" key
            String jumpingBase = animations.get("jumping");

            if (jumpingBase != null && !jumpingBase.isEmpty()) {
                // Construct the specific ID based on the state suffix
                String suffix = switch (state) {
                    case JUMP_ASCEND -> "_1";
                    case JUMP_DESCENT -> "_2";
                    case LANDING_IDLE -> "_3";
                    case LANDING_WALKING -> "_4";
                    case LANDING_SPRINTING -> "_5";
                    default -> "";
                };
                return Identifier.of(jumpingBase + suffix);
            }
        }

        return null;
    }
}