package net.wowmod.logic;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Identifier;
import net.wowmod.animation.player_animations.PlayerAnimationState;
import net.wowmod.animation.player_animations.player_weapons.WeaponAnimationConfig;
import net.wowmod.animation.player_animations.player_weapons.WeaponAnimationLoader;
import net.wowmod.util.RenderStateExtension;

/**
 * Central logic for determining which animation state the player is in.
 */
public class AnimationStateLogic {

    private static boolean isCustomJumpActive = false;

    /**
     * Determines the current animation state based on player render state and extension data.
     */
    public static PlayerAnimationState determineState(PlayerEntityRenderState state, RenderStateExtension ext) {

        // 1. Hard Overrides (Vanilla actions take precedence)
        if (state.isGliding || ext.wowmod$isRiding() || ext.wowmod$isClimbing() ||
                ext.wowmod$isInWater() || ext.wowmod$isSwimming()) {
            isCustomJumpActive = false;
            return PlayerAnimationState.VANILLA_OVERRIDE;
        }

        // 2. Attacks
        if (state.handSwingProgress > 0.0f) return PlayerAnimationState.STANDING_ATTACK;

        // 3. Sneaking
        if (state.sneaking) {
            isCustomJumpActive = false;
            return PlayerAnimationState.SNEAKING;
        }

        // 4. Jumping / Landing Mechanics
        float vy = (float) ext.wowmod$getVerticalVelocity();
        boolean onGround = ext.wowmod$isOnGround();
        long timeSinceLand = ext.wowmod$getTimeSinceLanding();

        // Detect Jump Start
        if ((onGround && vy > 0.05f) || (!onGround && vy > 0.2f)) {
            isCustomJumpActive = true;
        }

        if (onGround && (isCustomJumpActive || timeSinceLand < 10)) {
            if (timeSinceLand >= 10) isCustomJumpActive = false;

            // If moving, cancel landing animation and go straight to walk/sprint
            if (state.limbSwingAmplitude > 0.1f) {
                isCustomJumpActive = false;
            } else {
                return PlayerAnimationState.LANDING;
            }
        }

        if (!onGround) {
            if (isCustomJumpActive) return PlayerAnimationState.JUMPING;
            else if (vy != 0.0f) return PlayerAnimationState.FALLING;
        }

        // 5. Blocking
        // We need to resolve the weapon config to know if we *can* play a custom blocking animation
        WeaponAnimationConfig config = resolveWeaponConfig(ext);
        boolean isShieldOffhand = isShieldInOffhand(ext);

        if (state.isUsingItem && ext.wowmod$isBlocking()) {
            PlayerAnimationState potentialState = (state.limbSwingAmplitude > 0.1f)
                    ? PlayerAnimationState.BLOCKING_WALKING
                    : PlayerAnimationState.BLOCKING_IDLE;

            // Only use custom blocking if the config actually has it defined
            if (config != null && config.getAnimation(potentialState, isShieldOffhand) != null) {
                return potentialState;
            }
            // Otherwise fallback to Vanilla so shield rendering works correctly
            return PlayerAnimationState.VANILLA_OVERRIDE;
        }

        // 6. Other Item Use (Eating, drinking) -> Vanilla
        if (state.isUsingItem) {
            return PlayerAnimationState.VANILLA_OVERRIDE;
        }

        // 7. Movement
        if (state.limbSwingAmplitude > 0.1f) {
            return ext.wowmod$isSprinting() ? PlayerAnimationState.SPRINTING : PlayerAnimationState.WALKING;
        }

        return PlayerAnimationState.IDLE;
    }

    public static WeaponAnimationConfig resolveWeaponConfig(RenderStateExtension ext) {
        ItemStack mainHandStack = ext.wowmod$getMainHandStack();
        if (mainHandStack != null && !mainHandStack.isEmpty()) {
            Identifier itemID = mainHandStack.getItem().getRegistryEntry().getKey()
                    .map(key -> key.getValue()).orElse(null);

            if (itemID != null) {
                return WeaponAnimationLoader.WEAPON_ANIMATION_CONFIGS.get(itemID);
            }
        }
        return null;
    }

    public static boolean isShieldInOffhand(RenderStateExtension ext) {
        ItemStack offHandStack = ext.wowmod$getOffHandStack();
        return offHandStack != null && (offHandStack.getItem() instanceof ShieldItem);
    }
}