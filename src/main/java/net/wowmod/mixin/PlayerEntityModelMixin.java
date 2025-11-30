package net.wowmod.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.wowmod.animation.Animation;
import net.wowmod.animation.AnimationLoader;
import net.wowmod.util.RenderStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Injects into the Player Model to override rotations with custom animation data.
 */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin extends BipedEntityModel<PlayerEntityRenderState> {

    public PlayerEntityModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    public void replaceAnimations(PlayerEntityRenderState state, CallbackInfo ci) {
        // 1. Determine which animation to play
        String animName;

        float limbDistance = state.limbSwingAmplitude;
        float animationProgress = state.age;

        boolean isMoving = limbDistance > 0.1f;

        // Retrieve our custom sprinting flag safely
        boolean isSprinting = false;
        if (state instanceof RenderStateExtension ext) {
            isSprinting = ext.wowmod$isSprinting();
        }

        if (isMoving) {
            animName = isSprinting ? "default_sprinting" : "default_walking";
        } else {
            animName = "default_idle";
        }

        // 2. Fetch Animation Data
        Animation anim = AnimationLoader.ANIMATIONS.get(animName);

        // If animation not found, let vanilla logic persist (do nothing)
        if (anim == null) return;

        // 3. Calculate time
        float timeSeconds = (animationProgress * 0.05f) % anim.animation_length;

        // 4. Apply transformations to specific bones
        if (anim.bones != null) {
            // FIX: Don't override body/legs if sneaking, to preserve the vanilla crouch pose
            // In 1.21+, 'isSneaking' is typically a field in BipedEntityRenderState (which PlayerEntityRenderState extends)
            if (!state.sneaking) {
                applyBone(this.head, anim.bones.get("head"), timeSeconds);
                applyBone(this.body, anim.bones.get("body"), timeSeconds);
                applyBone(this.rightLeg, anim.bones.get("rightLeg"), timeSeconds);
                applyBone(this.leftLeg, anim.bones.get("leftLeg"), timeSeconds);
            }

            // FIX: Only override arms if the player is NOT performing a vanilla action
            // Vanilla actions we want to preserve:
            // - Attacking (handSwingProgress > 0)
            // - Using an item/Blocking (isUsingItem is true)
            // - Sneaking (often changes arm position too)
            if (state.handSwingProgress == 0.0f && !state.isUsingItem && !state.sneaking) {
                applyBone(this.rightArm, anim.bones.get("rightArm"), timeSeconds);
                applyBone(this.leftArm, anim.bones.get("leftArm"), timeSeconds);
            }
        }
    }

    private void applyBone(ModelPart part, Animation.Bone boneData, float time) {
        if (part == null || boneData == null) return;

        if (boneData.rotation != null) {
            float[] rot = getInterpolatedValue(boneData.rotation, time);
            part.pitch = (float) Math.toRadians(rot[0]);
            part.yaw = (float) Math.toRadians(rot[1]);
            part.roll = (float) Math.toRadians(rot[2]);
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