package net.wowmod.animation.player_animations;

public enum PlayerAnimationState {

    VANILLA_OVERRIDE(""),
    SNEAKING("idle_default"),
    IDLE("idle_default"),
    WALKING("walking_default"),
    SPRINTING("sprinting_default"),
    JUMP_ASCEND("jumping_default_1"),
    JUMP_DESCENT("jumping_default_2"),
    LANDING_IDLE("jumping_default_3"),
    LANDING_WALKING("jumping_default_4"),
    LANDING_SPRINTING("jumping_default_5"),
    FALLING("jumping_default_2"),
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