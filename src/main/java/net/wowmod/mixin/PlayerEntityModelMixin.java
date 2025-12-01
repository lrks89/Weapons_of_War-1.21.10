package net.wowmod.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.wowmod.animation.Animation;
import net.wowmod.animation.AnimationLoader;
import net.wowmod.util.RenderStateExtension;
import net.wowmod.util.IAnimatedPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin extends BipedEntityModel<PlayerEntityRenderState> {

    // Store state of the custom jump in the Model instance itself (client-side only)
    @Unique private static boolean wowmod$isCustomJumpActive = false;

    public PlayerEntityModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    public void replaceAnimations(PlayerEntityRenderState state, CallbackInfo ci) {
        String animName = "default_idle";

        float limbDistance = state.limbSwingAmplitude;
        float animationProgress = state.age;

        // Defaults
        boolean isSprinting = false;
        double vy = 0;
        boolean onGround = true;
        long timeSinceLand = 100;
        boolean isFlying = false;
        boolean isSwimming = false;
        boolean isRiding = false;
        boolean isGliding = false;
        boolean isClimbing = false;
        boolean isInWater = false;
        // Removed: boolean wasFluidBelow = false;
        long lastFluidContactTime = -999; // Updated initial value to better reflect 'not in contact'

        try {
            isGliding = state.isGliding;
        } catch (NoSuchFieldError e) {
            // Field missing in specific version
        }

        if (state instanceof RenderStateExtension ext) {
            isSprinting = ext.wowmod$isSprinting();
            vy = ext.wowmod$getVerticalVelocity();
            onGround = ext.wowmod$isOnGround();
            timeSinceLand = ext.wowmod$getTimeSinceLanding();
            isFlying = ext.wowmod$isFlying();
            isSwimming = ext.wowmod$isSwimming();
            isRiding = ext.wowmod$isRiding();
            isClimbing = ext.wowmod$isClimbing();
            isInWater = ext.wowmod$isInWater();
            // wasFluidBelow = ext.wowmod$wasFluidBelow(); // No longer needed

            // Fetch the fluid contact time from the entity's mixin via the render state
            // The entity mixin (PlayerEntityMixin) is responsible for tracking this value persistently.
            Object entity = ext.wowmod$getEntity();
            if (entity instanceof IAnimatedPlayer animatedPlayer) {
                lastFluidContactTime = animatedPlayer.wowmod$getLastFluidContactTime();
            }
        }

        long currentTime = (long)animationProgress; // Approximation of world time / client age
        final long FLUID_COOLDOWN_TICKS = 20; // 1 second

        // --- EXCLUSION CHECK (A) ---
        // 1. Permanent Vanilla Exclusions (e.g., flight, riding, active swim)
        if (isFlying || isGliding || isRiding || isClimbing) {
            wowmod$isCustomJumpActive = false; // Reset jump state when overriding with vanilla
            return; // Use vanilla animations
        }

        // 2. Fluid Exclusion: If player is in contact OR recently contacted fluid
        boolean isFluidJumpOnCooldown = (currentTime - lastFluidContactTime) < FLUID_COOLDOWN_TICKS;
        boolean skipCustomJump = isInWater || isSwimming || isFluidJumpOnCooldown;

        // If currently submerged/swimming OR recently touched fluid, we revert to vanilla logic until cooldown expires.
        if (skipCustomJump) {
            wowmod$isCustomJumpActive = false; // Disable custom jumps during cooldown
            return; // Use vanilla bobbing/floating/jump logic
        }


        // --- SNEAKING CHECK ---
        if (state.sneaking) {
            animName = "default_idle"; // Will default to vanilla sneak pose
            wowmod$isCustomJumpActive = false; // Sneaking also disables custom jump
        }

        boolean isMoving = limbDistance > 0.1f;
        boolean isJumping = false;

        // --- JUMP STATE MACHINE ---

        // Check 1: Initiation (Only runs when touching ground and jumping up, AND NOT SNEAKING/FLUID)
        if (onGround && vy > 0.05 && !state.sneaking && !skipCustomJump) {
            // Jump is initiated if on solid ground and moving upwards
            wowmod$isCustomJumpActive = true;
        }

        // Check 2: Landing sequence (Priority 1: Highest priority when touching ground)
        // Note: The `!skipCustomJump` here is technically redundant if we `return` above, but keeps logic clean.
        if (onGround && (wowmod$isCustomJumpActive || timeSinceLand < 10) && !state.sneaking && !skipCustomJump) {

            // Stop the custom jump flag only when landing animation is complete
            if (timeSinceLand >= 10) {
                wowmod$isCustomJumpActive = false;
            }

            // Sustain landing animation for a short duration
            animName = "default_jumping_down";
            animationProgress = (float) timeSinceLand;
            isJumping = true;
        }

        // Check 3: Airborne Sustain (Priority 2: If airborne and jump was initiated, sustain animation)
        else if (!onGround && !state.sneaking && !skipCustomJump) {

            // Sustain custom jump logic for the entire airborne phase
            if (wowmod$isCustomJumpActive) {
                isJumping = true;
                if (vy > 0.05) {
                    animName = "default_jumping_up"; // Ascent
                } else {
                    animName = "default_jumping_pose"; // Apex / Descent
                }
            }
            // Else: If vy != 0 but custom jump wasn't initiated (e.g., walking off a cliff),
            // fallback to sustained pose for general fall look.
            else if (vy != 0) {
                animName = "default_jumping_pose";
            }
        }

        // Check 4: Movement/Idle (Fallback) - Only runs if not actively jumping or sneaking/fluid
        else if (isMoving && !state.sneaking) {
            animName = isSprinting ? "default_sprinting" : "default_walking";
        }
        // else animName remains "default_idle" (set at the top) or uses vanilla sneaking/idle pose.

        // --- Apply Poses ---

        Animation anim = AnimationLoader.ANIMATIONS.get(animName);
        if (anim == null) return;

        float timeSeconds;
        if (animName.equals("default_jumping_down")) {
            timeSeconds = animationProgress * 0.05f;
            if (timeSeconds > anim.animation_length) timeSeconds = anim.animation_length;
        } else {
            timeSeconds = (animationProgress * 0.05f) % anim.animation_length;
        }

        if (anim.bones != null) {
            float headY = 0.0f;
            float bodyY = 0.0f;
            float rightArmX = -5.0f; float rightArmY = 2.0f;
            float leftArmX = 5.0f; float leftArmY = 2.0f;
            float legX = 1.9f; float legY = 12.0f;

            // Only apply custom offsets if we are using custom animations for jump/idle/walk/sprint
            if (isJumping || !state.sneaking) {
                applyBone(this.head, anim.bones.get("head"), timeSeconds, 0, headY, 0);

                applyBone(this.body, anim.bones.get("body"), timeSeconds, 0, bodyY, 0);

                float bodyAnimX = 0, bodyAnimY = 0, bodyAnimZ = 0;
                Animation.Bone bodyBone = anim.bones.get("body");
                if (bodyBone != null && bodyBone.position != null) {
                    float[] pos = getInterpolatedValue(bodyBone.position, timeSeconds);
                    bodyAnimX = pos[0];
                    bodyAnimY = -pos[1];
                    bodyAnimZ = pos[2];
                }

                float bodyPitch = this.body.pitch;
                float pivotY = 13.0f;
                float dY = (float) (pivotY - pivotY * Math.cos(bodyPitch));
                float dZ = (float) -(pivotY * Math.sin(bodyPitch));

                this.body.originY += dY;
                this.body.originZ += dZ;

                float shoulderY = 2.0f;
                float sDY = (float) (shoulderY * Math.cos(bodyPitch) - shoulderY);
                float sDZ = (float) (shoulderY * Math.sin(bodyPitch));

                this.head.originX += bodyAnimX;
                this.head.originY += dY + bodyAnimY;
                this.head.originZ += dZ + bodyAnimZ;

                applyBone(this.rightLeg, anim.bones.get("rightLeg"), timeSeconds, -legX, legY, 0);
                applyBone(this.leftLeg, anim.bones.get("leftLeg"), timeSeconds, legX, legY, 0);

                float armBaseX_R = rightArmX + bodyAnimX;
                float armBaseX_L = leftArmX + bodyAnimX;
                float armBaseY = rightArmY + dY + sDY + bodyAnimY;
                float armBaseZ = 0 + dZ + sDZ + bodyAnimZ;

                if (state.handSwingProgress == 0.0f && !state.isUsingItem && !state.sneaking) {
                    applyBone(this.rightArm, anim.bones.get("rightArm"), timeSeconds, armBaseX_R, armBaseY, armBaseZ);
                    applyBone(this.leftArm, anim.bones.get("leftArm"), timeSeconds, armBaseX_L, armBaseY, armBaseZ);
                }
            } else {
                // When falling back to vanilla pose (e.g., vanilla idle/walk/sprint or vanilla sneaking)
                // We only apply arm/item poses if needed (swinging, item use, etc.)
                if (state.handSwingProgress == 0.0f && !state.isUsingItem && !state.sneaking) {
                    applyBone(this.rightArm, anim.bones.get("rightArm"), timeSeconds, rightArmX, rightArmY, 0);
                    applyBone(this.leftArm, anim.bones.get("leftArm"), timeSeconds, leftArmX, leftArmY, 0);
                }
            }
        }
    }

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
                if (nextVal == null || t < nextTime) {
                    nextTime = t;
                    nextVal = v;
                }
            }
        }

        if (prevVal == null) return nextVal != null ? nextVal : new float[]{0,0,0};
        if (nextVal == null) return prevVal;
        if (prevTime == nextTime) return prevVal;

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