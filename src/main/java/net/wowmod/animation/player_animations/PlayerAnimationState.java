package net.wowmod.animation.player_animations;

public enum PlayerAnimationState {

    VANILLA_OVERRIDE(""),
    SNEAKING("idle_default"),
    IDLE("idle_default"),
    WALKING("walking_default"),
    SPRINTING("sprinting_default"),
    JUMPING("jumping_descending_default"),
    LANDING_IDLE("jumping_landing_default"),
    LANDING_WALKING("jumping_landing_walking_default"),
    LANDING_SPRINTING("jumping_landing_sprinting_default"),
    FALLING("jumping_descending_default"),
    STANDING_ATTACK("attack_standing_default");

    //SPRINTING_ATTACK("sprinting_attack_default"),
    //JUMPING_ATTACK("jumping_attack_default);

    private final String defaultAnimationName;

    PlayerAnimationState(String defaultAnimationName) {
        this.defaultAnimationName = defaultAnimationName;
    }

    public String getDefaultAnimationName() {
        return defaultAnimationName;
    }
}