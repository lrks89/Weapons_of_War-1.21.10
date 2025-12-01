package net.wowmod.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.wowmod.animation.PlayerAnimationState;
import net.wowmod.animation.Animation;
import net.wowmod.animation.AnimationLoader;
import net.wowmod.util.RenderStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import org.joml.Quaternionf; // Needed for rotations in vanilla Elytra rendering context

@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin extends BipedEntityModel<PlayerEntityRenderState> {

    // Store state of the custom jump in the Model instance itself (client-side only)
    @Unique private static boolean wowmod$isCustomJumpActive = false;

    public PlayerEntityModelMixin(ModelPart root) {
        super(root);
    }

    /**
     * The main injection method. It acts as an orchestrator:
     * 1. Get extended state.
     * 2. Determine the intended animation state via FSM.
     * 3. Apply the corresponding custom animation or fall back to vanilla.
     */
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    public void replaceAnimations(PlayerEntityRenderState state, CallbackInfo ci) {

        // 1. Safely extract extended state data
        RenderStateExtension ext = (state instanceof RenderStateExtension e) ? e : null;
        // If the extension is not available (e.g., older versions), we must exit
        if (ext == null) return;

        // 2. Determine the desired animation state using the Finite State Machine (FSM)
        PlayerAnimationState animState = determineAnimationState(state, ext);

        // 3. Handle Vanilla Override
        if (animState == PlayerAnimationState.VANILLA_OVERRIDE) {
            return; // Exit: Use vanilla logic already applied by the base method
        }

        // 4. Map State to Animation Name (if not vanilla override)
        String animName = switch (animState) {
            case IDLE -> "default_idle";
            case WALKING -> "default_walking";
            case SPRINTING -> "default_sprinting";
            case JUMP_ASCEND -> "default_jumping_1";
            case JUMP_DESCENT, FALLING -> "default_jumping_2";
            case LANDING -> "default_jumping_3";
            case SNEAKING -> "default_idle"; // Uses default_idle animation data, but relies on vanilla sneaking pose
            default -> "default_idle"; // Handle any future or unexpected enum states
        };

        Animation anim = AnimationLoader.ANIMATIONS.get(animName);
        if (anim == null) return;

        // 5. Apply the custom animation poses to the model parts
        applyCustomAnimation(anim, state, animState, ext);
    }

    /**
     * Contains the complete Finite State Machine (FSM) logic for selecting the player's animation state.
     * This separates complex decision-making from model application.
     */
    @Unique
    private PlayerAnimationState determineAnimationState(PlayerEntityRenderState state, RenderStateExtension ext) {

        // --- EXCLUSION CHECK (A) --- (Permanent Vanilla Exclusions)
        // If any of these are true, the custom animation system is ignored completely.
        if (ext.wowmod$isFlying() || state.isGliding || ext.wowmod$isRiding() || ext.wowmod$isClimbing() || ext.wowmod$isInWater() || ext.wowmod$isSwimming()) {
            wowmod$isCustomJumpActive = false;
            return PlayerAnimationState.VANILLA_OVERRIDE;
        }

        // --- SNEAKING CHECK ---
        if (state.sneaking) {
            wowmod$isCustomJumpActive = false;
            return PlayerAnimationState.SNEAKING;
        }

        // --- JUMP STATE MACHINE ---
        // Note: Casting vertical velocity to float for math consistency in animation blending
        float vy = (float) ext.wowmod$getVerticalVelocity();
        boolean onGround = ext.wowmod$isOnGround();
        long timeSinceLand = ext.wowmod$getTimeSinceLanding();

        // Check 1: Initiation (Starts the custom jump flag)
        if (onGround && vy > 0.05f) {
            wowmod$isCustomJumpActive = true;
        }

        // Check 2: Landing sequence (Priority 1: Highest priority when touching ground)
        if (onGround && (wowmod$isCustomJumpActive || timeSinceLand < 10)) {
            // Stop the custom jump flag only when landing animation is complete (10 ticks = 0.5s)
            if (timeSinceLand >= 10) {
                wowmod$isCustomJumpActive = false;
            }
            return PlayerAnimationState.LANDING;
        }

        // Check 3: Airborne Sustain (Priority 2: If airborne)
        if (!onGround) {
            if (wowmod$isCustomJumpActive) {
                // Determine if ascending or descending/apex
                return (vy > 0.05f) ? PlayerAnimationState.JUMP_ASCEND : PlayerAnimationState.JUMP_DESCENT;
            } else if (vy != 0.0f) {
                // Generic Fall: use jump descent animation for walking off a cliff
                return PlayerAnimationState.FALLING;
            }
        }

        // Check 4: Movement/Idle (Fallback)
        if (state.limbSwingAmplitude > 0.1f) {
            return ext.wowmod$isSprinting() ? PlayerAnimationState.SPRINTING : PlayerAnimationState.WALKING;
        }

        return PlayerAnimationState.IDLE;
    }

    /**
     * Applies the keyframe data from the Animation object to the ModelParts, including
     * complex body pivot corrections.
     */
    @Unique
    private void applyCustomAnimation(Animation anim, PlayerEntityRenderState state, PlayerAnimationState animState, RenderStateExtension ext) {

        long timeSinceLand = ext.wowmod$getTimeSinceLanding();
        float timeSeconds;

        // Calculate the time within the animation's loop duration
        if (animState == PlayerAnimationState.LANDING) {
            // FIX 1: Use timeSinceLand directly, normalized against the 10-tick target duration.
            // Ensure the time doesn't exceed animation length.
            timeSeconds = Math.min(timeSinceLand / 10.0f * anim.animation_length, anim.animation_length);
        } else {
            // Looping animations (idle, walk, sprint, jump phases)
            timeSeconds = (state.age * 0.05f) % anim.animation_length;
        }

        // ModelPart default offsets (copied from original code)
        float headY = 0.0f;
        float bodyY = 0.0f;
        float legX = 1.9f;
        float legY = 12.0f;
        float rightArmX = -5.0f;
        float leftArmX = 5.0f;
        float armY = 2.0f;

        // Retrieve arm bone data for both custom and sneaking poses
        Animation.Bone rightArmBone = anim.bones.get("rightArm");
        Animation.Bone leftArmBone = anim.bones.get("leftArm");


        // Apply custom poses only if not sneaking (sneaking relies on vanilla body/leg pose)
        if (animState != PlayerAnimationState.SNEAKING) {

            // 1. Apply Body Animation
            applyBone(this.body, anim.bones.get("body"), timeSeconds, 0, bodyY, 0);

            // Calculate animated translation of the body
            float bodyAnimX = 0, bodyAnimY = 0, bodyAnimZ = 0;
            Animation.Bone bodyBone = anim.bones.get("body");
            if (bodyBone != null && bodyBone.position != null) {
                float[] pos = getInterpolatedValue(bodyBone.position, timeSeconds);
                bodyAnimX = pos[0];
                bodyAnimY = -pos[1]; // Y-axis inverted for position
                bodyAnimZ = pos[2];
            }

            // Apply Pivot Correction (Complex math to account for body rotation)
            float bodyPitch = this.body.pitch;
            float pivotY = 13.0f;
            float dY = (float) (pivotY - pivotY * Math.cos(bodyPitch));
            float dZ = (float) -(pivotY * Math.sin(bodyPitch));
            this.body.originY += dY;
            this.body.originZ += dZ;

            // 2. Apply Head Animation
            applyBone(this.head, anim.bones.get("head"), timeSeconds, 0, headY, 0);

            // Correct Head position relative to pivoted body
            float shoulderY = 2.0f;
            float sDY = (float) (shoulderY * Math.cos(bodyPitch) - shoulderY);
            float sDZ = (float) (shoulderY * Math.sin(bodyPitch));

            this.head.originX += bodyAnimX;
            this.head.originY += dY + bodyAnimY;
            this.head.originZ += dZ + bodyAnimZ;

            // 3. Apply Leg Animation
            applyBone(this.rightLeg, anim.bones.get("rightLeg"), timeSeconds, -legX, legY, 0);
            applyBone(this.leftLeg, anim.bones.get("leftLeg"), timeSeconds, legX, legY, 0);

            // 4. Apply Arm Animation (Only if not swinging/using item/sneaking, as per original logic)
            if (state.handSwingProgress == 0.0f && !state.isUsingItem) {
                float armBaseY = armY + dY + sDY + bodyAnimY;
                float armBaseZ = 0 + dZ + sDZ + bodyAnimZ;

                applyBone(this.rightArm, rightArmBone, timeSeconds, rightArmX + bodyAnimX, armBaseY, armBaseZ);
                applyBone(this.leftArm, leftArmBone, timeSeconds, leftArmX + bodyAnimX, armBaseY, armBaseZ);
            }
        } else if (animState == PlayerAnimationState.SNEAKING) {

            // FIX: Arms need to align with the body's vanilla sneak pose translation.
            // The vanilla setAngles method already moved the body (and thus the arm origins) down/forward.
            // We read the final origin positions from the body part.

            // Get the final calculated position of the body part after base class setup (sneak translation)
            float bodyOffsetX = this.body.originX;
            float bodyOffsetY = this.body.originY;
            float bodyOffsetZ = this.body.originZ;

            // Only apply arm rotations/position offsets from custom animation data if not swinging/using item.
            if (state.handSwingProgress == 0.0f && !state.isUsingItem) {

                // Base arm positions in the default Biped model are at (5, 2, 0) relative to body origin.
                // We apply the custom arm animation using the *current* body origin as the base.
                applyBone(this.rightArm, rightArmBone, timeSeconds, bodyOffsetX + rightArmX, bodyOffsetY + armY, bodyOffsetZ);
                applyBone(this.leftArm, leftArmBone, timeSeconds, bodyOffsetX + leftArmX, bodyOffsetY + armY, bodyOffsetZ);
            }
        }
    }


    // --- UTILITY METHODS (Unchanged but included for completeness) ---

    private void applyBone(ModelPart part, Animation.Bone boneData, float time, float defaultX, float defaultY, float defaultZ) {
        if (part == null || boneData == null) return;

        if (boneData.rotation != null) {
            float[] rot = getInterpolatedValue(boneData.rotation, time);
            part.pitch = (float) Math.toRadians(rot[0]);
            part.yaw = (float) Math.toRadians(rot[1]);
            part.roll = (float) Math.toRadians(rot[2]);
        }

        if (boneData.position != null) {
            float[] pos = getInterpolatedValue(boneData.position, time);
            part.originX = defaultX + pos[0];
            part.originY = defaultY - pos[1];
            part.originZ = defaultZ + pos[2];
        } else {
            part.originX = defaultX;
            part.originY = defaultY;
            part.originZ = defaultZ;
        }
    }

    private float[] getInterpolatedValue(Map<String, float[]> keyframes, float time) {
        float prevTime = 0;
        float nextTime = 0;
        float[] prevVal = null;
        float[] nextVal = null;

        // Find the previous and next keyframes
        for (String key : keyframes.keySet()) {
            float t = Float.parseFloat(key);
            float[] v = keyframes.get(key);

            if (t <= time) {
                if (prevVal == null || t > prevTime) {
                    prevTime = t;
                    prevVal = v;
                }
            }
            if (t >= time) {
                if (nextVal == null || t < nextTime || (nextVal != null && t == nextTime && t == 0)) {
                    nextTime = t;
                    nextVal = v;
                }
            }
        }

        if (prevVal == null) return nextVal != null ? nextVal : new float[]{0,0,0};
        if (nextVal == null) return prevVal;
        if (prevTime == nextTime) return prevVal;

        // Linear interpolation (LERP)
        float alpha = (time - prevTime) / (nextTime - prevTime);
        return new float[] {
                lerp(prevVal[0], nextVal[0], alpha),
                lerp(prevVal[1], nextVal[1], alpha),
                lerp(prevVal[2], nextVal[2], alpha)
        };
    }

    private float lerp(float start, float end, float alpha) {
        return start + alpha * (end - start);
    }
}