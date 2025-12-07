package net.wowmod.animation.player_animations.player_weapons;

import java.util.List;
import java.util.Map;
import net.minecraft.util.Identifier;
import net.wowmod.animation.player_animations.PlayerAnimationState;

public class WeaponAnimationConfig {
    // Maps state names (e.g. "idle", "jumping") to animation resource strings
    public Map<String, String> animations;

    // Condition overrides
    public List<OverrideEntry> overrides;

    public static class OverrideEntry {
        public String condition; // e.g. "shield_offhand"
        public Map<String, String> animations;
    }

    /**
     * getAnimation with support for variable state checking
     */
    public Identifier getAnimation(PlayerAnimationState state, boolean isShieldOffhand) {
        // 1. Check Overrides first
        if (overrides != null) {
            for (OverrideEntry entry : overrides) {
                // Condition Check
                if ("shield_offhand".equalsIgnoreCase(entry.condition) && isShieldOffhand) {
                    Identifier overrideId = resolveFromMap(entry.animations, state);
                    if (overrideId != null) return overrideId;
                }
            }
        }

        // 2. Fallback to default map
        return resolveFromMap(this.animations, state);
    }

    /**
     * Helper to resolve the ID from a specific map (override or default)
     */
    private Identifier resolveFromMap(Map<String, String> map, PlayerAnimationState state) {
        if (map == null) return null;

        // 1. Macro Match (Jumping / Falling / Landing)
        // FIX: Added FALLING here so it uses the "jumping" key + suffix instead of looking for a "falling" key
        if (state == PlayerAnimationState.JUMPING ||
                state == PlayerAnimationState.FALLING ||
                state == PlayerAnimationState.LANDING) {

            String jumpingBase = map.get("jumping");

            if (jumpingBase != null && !jumpingBase.isEmpty()) {
                String suffix = switch (state) {
                    case JUMPING, FALLING -> "_ascending-falling"; // Both map to the loop
                    case LANDING -> "_landing";
                    default -> "";
                };
                return Identifier.of(jumpingBase + suffix);
            }
        }

        // 2. Split Attack Fallback Logic
        if (state == PlayerAnimationState.ATTACK_STRIKE || state == PlayerAnimationState.ATTACK_RETURN) {
            // First, try to find an explicit key (e.g. "attack_strike")
            String explicitKey = state.toString().toLowerCase();
            if (map.containsKey(explicitKey)) {
                return Identifier.of(map.get(explicitKey));
            }

            // If not found, fall back to "standing_attack" base + suffix
            String base = map.get("standing_attack");
            if (base == null) base = map.get("attack"); // Support "attack" alias

            if (base != null && !base.isEmpty()) {
                String suffix = (state == PlayerAnimationState.ATTACK_STRIKE) ? "_strike" : "_return";
                return Identifier.of(base + suffix);
            }
        }

        // 3. Blocking Fallback Logic (MACRO)
        if (state == PlayerAnimationState.BLOCKING_IDLE || state == PlayerAnimationState.BLOCKING_WALKING) {
            // A. Try specific key first (e.g. "blocking_idle")
            String specificKey = state.toString().toLowerCase();
            if (map.containsKey(specificKey)) {
                return Identifier.of(map.get(specificKey));
            }

            // B. Fallback to generic "blocking" key + Automatic Suffix
            if (map.containsKey("blocking")) {
                String base = map.get("blocking");
                // Automatically append _idle or _walking
                String suffix = (state == PlayerAnimationState.BLOCKING_IDLE) ? "_idle" : "_walking";
                return Identifier.of(base + suffix);
            }
        }

        // 4. Standard Lookup (Idle, Walking, Sprinting)
        String key = state.toString().toLowerCase();

        // Specific handling for attack_standing to map to "attack" in JSON if needed
        if (state == PlayerAnimationState.STANDING_ATTACK && !map.containsKey("standing_attack") && map.containsKey("attack")) {
            return Identifier.of(map.get("attack"));
        }

        String animId = map.get(key);

        if (animId != null && !animId.isEmpty()) {
            return Identifier.of(animId);
        }

        return null;
    }
}