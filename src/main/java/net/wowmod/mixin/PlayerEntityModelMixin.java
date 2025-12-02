package net.wowmod.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.wowmod.animation.player_animations.PlayerAnimationState;
import net.wowmod.animation.player_animations.Animation;
import net.wowmod.animation.player_animations.AnimationLoader;
import net.wowmod.animation.player_animations.player_weapons.WeaponAnimationConfig;
import net.wowmod.animation.player_animations.player_weapons.WeaponAnimationLoader;
import net.wowmod.util.RenderStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Map;
import org.joml.Quaternionf;

@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin extends BipedEntityModel<PlayerEntityRenderState> {

    @Unique private static boolean wowmod$isCustomJumpActive = false;

    // Virtual bones for items
    @Unique public ModelPart wowmod$rightItem;
    @Unique public ModelPart wowmod$leftItem;

    public PlayerEntityModelMixin(ModelPart root) {
        super(root);
    }

    // Initialize the virtual bones
    @Inject(method = "<init>", at = @At("TAIL"))
    public void initItemBones(ModelPart root, boolean thinArms, CallbackInfo ci) {
        // Create empty parts (no cubes) solely for transformation
        this.wowmod$rightItem = new ModelPart(Collections.emptyList(), Collections.emptyMap());
        this.wowmod$leftItem = new ModelPart(Collections.emptyList(), Collections.emptyMap());
    }

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    public void replaceAnimations(PlayerEntityRenderState state, CallbackInfo ci) {

        // 1. Safely extract extended state data
        RenderStateExtension ext = (state instanceof RenderStateExtension e) ? e : null;
        if (ext == null) return;

        // 2. Determine the desired animation state
        PlayerAnimationState animState = determineAnimationState(state, ext);

        // 3. Handle Vanilla Override
        if (animState == PlayerAnimationState.VANILLA_OVERRIDE) {
            return;
        }

        // --- CUSTOM ANIMATION LOOKUP LOGIC ---

        ItemStack mainHandStack = ext.wowmod$getMainHandStack();

        Identifier itemID = null;
        if (mainHandStack != null && !mainHandStack.isEmpty()) {
            itemID = mainHandStack.getItem().getRegistryEntry().getKey().map(key -> key.getValue()).orElse(null);
        }

        // 5. Look up custom animation config for the item
        WeaponAnimationConfig config = null;
        if (itemID != null) {
            config = WeaponAnimationLoader.WEAPON_ANIMATION_CONFIGS.get(itemID);
        }

        // 6. Determine final animation name
        String finalAnimName = animState.getDefaultAnimationName();

        if (config != null) {
            Identifier customAnimID = config.getAnimation(animState);
            if (customAnimID != null) {
                finalAnimName = customAnimID.getPath().replace("player_animations/", "");
            }
        }

        // 7. Load and apply the animation
        Animation anim = AnimationLoader.ANIMATIONS.get(finalAnimName);
        if (anim == null) return;

        applyCustomAnimation(anim, state, animState, ext);
    }

    // Override setArmAngle instead of translateToHand (which doesn't exist in 1.21 mappings)
    // This is called by ItemInHandRenderer to position the matrices before rendering the item.
    @Override
    public void setArmAngle(PlayerEntityRenderState state, Arm arm, MatrixStack matrices) {
        // 1. Apply the standard arm transformation (Move to shoulder, rotate arm)
        super.setArmAngle(state, arm, matrices);

        // 2. Apply our custom Item Bone transformation
        // This transformation happens "at the hand" because super.setArmAngle has already moved the matrix there.
        ModelPart itemBone = (arm == Arm.RIGHT) ? this.wowmod$rightItem : this.wowmod$leftItem;

        if (itemBone != null) {
            // Manual transformation application

            // Translate (offset from parent/hand)
            // ModelPart coordinates are usually in 16ths of a block (pixels)
            matrices.translate(itemBone.originX / 16.0F, itemBone.originY / 16.0F, itemBone.originZ / 16.0F);

            // Rotate
            if (itemBone.roll != 0.0F || itemBone.yaw != 0.0F || itemBone.pitch != 0.0F) {
                matrices.multiply(new Quaternionf().rotationZYX(itemBone.roll, itemBone.yaw, itemBone.pitch));
            }

            // Scale
            if (itemBone.xScale != 1.0F || itemBone.yScale != 1.0F || itemBone.zScale != 1.0F) {
                matrices.scale(itemBone.xScale, itemBone.yScale, itemBone.zScale);
            }
        }
    }

    @Unique
    private PlayerAnimationState determineAnimationState(PlayerEntityRenderState state, RenderStateExtension ext) {
        if (ext.wowmod$isFlying() || state.isGliding || ext.wowmod$isRiding() || ext.wowmod$isClimbing() || ext.wowmod$isInWater() || ext.wowmod$isSwimming()) {
            wowmod$isCustomJumpActive = false;
            return PlayerAnimationState.VANILLA_OVERRIDE;
        }

        if (state.sneaking) {
            wowmod$isCustomJumpActive = false;
            return PlayerAnimationState.SNEAKING;
        }

        float vy = (float) ext.wowmod$getVerticalVelocity();
        boolean onGround = ext.wowmod$isOnGround();
        long timeSinceLand = ext.wowmod$getTimeSinceLanding();

        if (onGround && vy > 0.05f) {
            wowmod$isCustomJumpActive = true;
        }

        // --- UPDATED LANDING LOGIC START ---
        if (onGround && (wowmod$isCustomJumpActive || timeSinceLand < 10)) {
            if (timeSinceLand >= 10) {
                wowmod$isCustomJumpActive = false;
            }

            // Check limbSwingAmplitude to determine if player is physically moving
            if (state.limbSwingAmplitude > 0.1f) {
                if (ext.wowmod$isSprinting()) {
                    return PlayerAnimationState.LANDING_SPRINTING;
                } else {
                    return PlayerAnimationState.LANDING_WALKING;
                }
            } else {
                return PlayerAnimationState.LANDING_IDLE;
            }
        }
        // --- UPDATED LANDING LOGIC END ---

        if (!onGround) {
            if (wowmod$isCustomJumpActive) {
                return (vy > 0.05f) ? PlayerAnimationState.JUMP_ASCEND : PlayerAnimationState.JUMP_DESCENT;
            } else if (vy != 0.0f) {
                return PlayerAnimationState.FALLING;
            }
        }

        if (state.limbSwingAmplitude > 0.1f) {
            return ext.wowmod$isSprinting() ? PlayerAnimationState.SPRINTING : PlayerAnimationState.WALKING;
        }

        return PlayerAnimationState.IDLE;
    }

    @Unique
    private void applyCustomAnimation(Animation anim, PlayerEntityRenderState state, PlayerAnimationState animState, RenderStateExtension ext) {
        long timeSinceLand = ext.wowmod$getTimeSinceLanding();
        float timeSeconds;

        // --- UPDATED TIMING LOGIC START ---
        // Treat all landing states as one-shot animations based on timeSinceLand
        if (animState == PlayerAnimationState.LANDING_IDLE ||
                animState == PlayerAnimationState.LANDING_WALKING ||
                animState == PlayerAnimationState.LANDING_SPRINTING) {

            // Map 10 ticks to the full length of the animation
            timeSeconds = Math.min(timeSinceLand / 10.0f * anim.animation_length, anim.animation_length);
        } else {
            // Standard looping logic for other states
            timeSeconds = (state.age * 0.05f) % anim.animation_length;
        }
        // --- UPDATED TIMING LOGIC END ---

        float headY = 0.0f;
        float bodyY = 0.0f;
        float legX = 1.9f;
        float legY = 12.0f;
        float rightArmX = -5.0f;
        float leftArmX = 5.0f;
        float armY = 2.0f;

        Animation.Bone rightArmBone = anim.bones.get("rightArm");
        Animation.Bone leftArmBone = anim.bones.get("leftArm");

        // Fetch item bones
        Animation.Bone rightItemBone = anim.bones.get("rightItem");
        Animation.Bone leftItemBone = anim.bones.get("leftItem");

        if (animState != PlayerAnimationState.SNEAKING) {
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

            applyBone(this.head, anim.bones.get("head"), timeSeconds, 0, headY, 0);

            float shoulderY = 2.0f;
            float sDY = (float) (shoulderY * Math.cos(bodyPitch) - shoulderY);
            float sDZ = (float) (shoulderY * Math.sin(bodyPitch));

            this.head.originX += bodyAnimX;
            this.head.originY += dY + bodyAnimY;
            this.head.originZ += dZ + bodyAnimZ;

            applyBone(this.rightLeg, anim.bones.get("rightLeg"), timeSeconds, -legX, legY, 0);
            applyBone(this.leftLeg, anim.bones.get("leftLeg"), timeSeconds, legX, legY, 0);

            if (state.handSwingProgress == 0.0f && !state.isUsingItem) {
                float armBaseY = armY + dY + sDY + bodyAnimY;
                float armBaseZ = 0 + dZ + sDZ + bodyAnimZ;

                applyBone(this.rightArm, rightArmBone, timeSeconds, rightArmX + bodyAnimX, armBaseY, armBaseZ);
                applyBone(this.leftArm, leftArmBone, timeSeconds, leftArmX + bodyAnimX, armBaseY, armBaseZ);
            }
        } else if (animState == PlayerAnimationState.SNEAKING) {
            float bodyOffsetX = this.body.originX;
            float bodyOffsetY = this.body.originY;
            float bodyOffsetZ = this.body.originZ;

            if (state.handSwingProgress == 0.0f && !state.isUsingItem) {
                applyBone(this.rightArm, rightArmBone, timeSeconds, bodyOffsetX + rightArmX, bodyOffsetY + armY, bodyOffsetZ);
                applyBone(this.leftArm, leftArmBone, timeSeconds, bodyOffsetX + leftArmX, bodyOffsetY + armY, bodyOffsetZ);
            }
        }

        // Apply animations to item bones.
        // We use 0,0,0 as default because the base offset (to the hand) is handled in setArmAngle via matrix translation.
        applyBone(this.wowmod$rightItem, rightItemBone, timeSeconds, 0, 0, 0);
        applyBone(this.wowmod$leftItem, leftItemBone, timeSeconds, 0, 0, 0);
    }

    private void applyBone(ModelPart part, Animation.Bone boneData, float time, float defaultX, float defaultY, float defaultZ) {
        if (part == null) return;

        // Even if boneData is null, we must reset the part to default to avoid "sticking" animations
        if (boneData == null) {
            part.originX = defaultX;
            part.originY = defaultY;
            part.originZ = defaultZ;
            part.pitch = 0;
            part.yaw = 0;
            part.roll = 0;
            return;
        }

        if (boneData.rotation != null) {
            float[] rot = getInterpolatedValue(boneData.rotation, time);
            part.pitch = (float) Math.toRadians(rot[0]);
            part.yaw = (float) Math.toRadians(rot[1]);
            part.roll = (float) Math.toRadians(rot[2]);
        } else {
            part.pitch = 0;
            part.yaw = 0;
            part.roll = 0;
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
                if (nextVal == null || t < nextTime || (nextVal != null && t == nextTime && t == 0)) {
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