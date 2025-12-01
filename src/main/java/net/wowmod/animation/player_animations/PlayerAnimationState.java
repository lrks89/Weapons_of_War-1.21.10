package net.wowmod.animation.player_animations;

public enum PlayerAnimationState {

    VANILLA_OVERRIDE(""),
    SNEAKING("idle_default"),
    IDLE("idle_default"),
    WALKING("walking_default"),
    SPRINTING("sprinting_default"),
    JUMP_ASCEND("jumping_default_1"),
    JUMP_DESCENT("jumping_default_2"),
    LANDING("jumping_default_3"),
    FALLING("jumping_default_2");

    private final String defaultAnimationName;

    PlayerAnimationState(String defaultAnimationName) {
        this.defaultAnimationName = defaultAnimationName;
    }

    public String getDefaultAnimationName() {
        return defaultAnimationName;
    }
}