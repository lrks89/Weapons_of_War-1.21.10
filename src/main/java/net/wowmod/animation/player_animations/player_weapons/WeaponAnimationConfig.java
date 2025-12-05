package net.wowmod.animation.player_animations.player_weapons;

import java.util.Map;
import net.minecraft.util.Identifier;
import net.wowmod.animation.player_animations.PlayerAnimationState;

public class WeaponAnimationConfig {
    // Maps state names (e.g. "idle", "jumping") to animation resource strings
    public Map<String, String> animations;

    public Identifier getAnimation(PlayerAnimationState state) {
        if (animations == null) return null;

        // 1. Macro Match (Jumping / Landing)
        if (state == PlayerAnimationState.JUMPING ||
                state == PlayerAnimationState.LANDING) {

            String jumpingBase = animations.get("jumping");

            if (jumpingBase != null && !jumpingBase.isEmpty()) {
                String suffix = switch (state) {
                    case JUMPING -> "_ascending-falling";
                    case LANDING -> "_landing";
                    default -> "";
                };
                return Identifier.of(jumpingBase + suffix);
            }
        }

        // 2. Split Attack Fallback Logic
        // If we need a STRIKE or RETURN animation, but the config doesn't list them explicitly,
        // we derive them from the "standing_attack" entry by appending suffixes.
        if (state == PlayerAnimationState.ATTACK_STRIKE || state == PlayerAnimationState.ATTACK_RETURN) {
            // First, try to find an explicit key (e.g. "attack_strike") in the JSON
            String explicitKey = state.toString().toLowerCase();
            if (animations.containsKey(explicitKey)) {
                return Identifier.of(animations.get(explicitKey));
            }

            // If not found, fall back to "standing_attack" base + suffix
            String base = animations.get("standing_attack");
            if (base == null) base = animations.get("attack"); // Support "attack" alias

            if (base != null && !base.isEmpty()) {
                String suffix = (state == PlayerAnimationState.ATTACK_STRIKE) ? "_strike" : "_return";
                return Identifier.of(base + suffix);
            }
        }

        // 3. Standard Lookup
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