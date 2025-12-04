package net.wowmod.animation.player_animations;

public enum PlayerAnimationState {

    VANILLA_OVERRIDE(""),
    SNEAKING("idle_default"),
    IDLE("idle_default"),
    WALKING("walking_default"),
    SPRINTING("sprinting_default"),
    JUMPING("jumping_default_ascending-falling"),
    LANDING_IDLE("jumping_default_landing_idle"),
    LANDING_WALKING("jumping_default_landing_walking"),
    LANDING_SPRINTING("jumping_default_landing_sprinting"),
    FALLING("jumping_default_ascending-falling"),

    // Fallback
    STANDING_ATTACK("attack_standing_default"),

    // Split Phases
    ATTACK_STRIKE("attack_strike"),
    ATTACK_RETURN("attack_return");

    private final String defaultAnimationName;

    PlayerAnimationState(String defaultAnimationName) {
        this.defaultAnimationName = defaultAnimationName;
    }

    public String getDefaultAnimationName() {
        return defaultAnimationName;
    }
}