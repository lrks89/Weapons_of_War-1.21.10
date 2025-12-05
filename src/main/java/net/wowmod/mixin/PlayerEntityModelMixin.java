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
import net.wowmod.animation.player_animations.BoneModifier;
import net.wowmod.animation.player_animations.player_weapons.WeaponAnimationConfig;
import net.wowmod.animation.player_animations.player_weapons.WeaponAnimationLoader;
import net.wowmod.util.PlayerModelUtils;
import net.wowmod.util.RenderStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@SuppressWarnings("deprecation")
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin extends BipedEntityModel<PlayerEntityRenderState> {

    @Unique private static boolean wowmod$isCustomJumpActive = false;

    // Virtual bones
    @Unique public ModelPart wowmod$controller;
    @Unique public ModelPart wowmod$rightHand;
    @Unique public ModelPart wowmod$leftHand;
    @Unique public ModelPart wowmod$rightItem;
    @Unique public ModelPart wowmod$leftItem;

    // New Parent Bones
    @Unique public ModelPart wowmod$leftShoulder;
    @Unique public ModelPart wowmod$rightShoulder;
    @Unique public ModelPart wowmod$leftHip;
    @Unique public ModelPart wowmod$rightHip;

    // --- ANATOMICAL CONSTANTS ---
    @Unique private static final float WAIST_Y = 12.0f;

    @Unique private static final float RIGHT_ARM_DEFAULT_X = -5.0f;
    @Unique private static final float LEFT_ARM_DEFAULT_X = 5.0f;
    @Unique private static final float ARM_DEFAULT_Y = 2.0f;

    @Unique private static final float RIGHT_LEG_DEFAULT_X = -1.9f;
    @Unique private static final float LEFT_LEG_DEFAULT_X = 1.9f;
    @Unique private static final float LEG_DEFAULT_Y = 12.0f;

    public PlayerEntityModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void initItemBones(ModelPart root, boolean thinArms, CallbackInfo ci) {
        this.wowmod$controller = new ModelPart(Collections.emptyList(), Collections.emptyMap());
        this.wowmod$rightHand = new ModelPart(Collections.emptyList(), Collections.emptyMap());
        this.wowmod$leftHand = new ModelPart(Collections.emptyList(), Collections.emptyMap());
        this.wowmod$rightItem = new ModelPart(Collections.emptyList(), Collections.emptyMap());
        this.wowmod$leftItem = new ModelPart(Collections.emptyList(), Collections.emptyMap());

        this.wowmod$leftShoulder = new ModelPart(Collections.emptyList(), Collections.emptyMap());
        this.wowmod$rightShoulder = new ModelPart(Collections.emptyList(), Collections.emptyMap());
        this.wowmod$leftHip = new ModelPart(Collections.emptyList(), Collections.emptyMap());
        this.wowmod$rightHip = new ModelPart(Collections.emptyList(), Collections.emptyMap());
    }

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    public void replaceAnimations(PlayerEntityRenderState state, CallbackInfo ci) {

        RenderStateExtension ext = (state instanceof RenderStateExtension e) ? e : null;
        if (ext == null) return;

        PlayerAnimationState animState = determineAnimationState(state, ext);

        if (animState == PlayerAnimationState.VANILLA_OVERRIDE) {
            return;
        }

        ItemStack mainHandStack = ext.wowmod$getMainHandStack();
        Identifier itemID = null;
        if (mainHandStack != null && !mainHandStack.isEmpty()) {
            itemID = mainHandStack.getItem().getRegistryEntry().getKey().map(key -> key.getValue()).orElse(null);
        }

        WeaponAnimationConfig config = null;
        if (itemID != null) {
            config = WeaponAnimationLoader.WEAPON_ANIMATION_CONFIGS.get(itemID);
        }

        float customTime = -1.0f;
        if (animState == PlayerAnimationState.STANDING_ATTACK && config != null) {
            Identifier strikeID = config.getAnimation(PlayerAnimationState.ATTACK_STRIKE);
            if (strikeID != null) {
                String strikeName = strikeID.getPath().replace("player_animations/", "");
                Animation strikeAnim = AnimationLoader.ANIMATIONS.get(strikeName);
                if (strikeAnim != null) {
                    int totalDurationTicks = ext.wowmod$getHandSwingDuration();
                    if (totalDurationTicks <= 0) totalDurationTicks = 1;
                    float currentTick = state.handSwingProgress * totalDurationTicks;
                    float strikeLengthTicks = strikeAnim.animation_length * 20.0f;

                    if (currentTick <= strikeLengthTicks) {
                        animState = PlayerAnimationState.ATTACK_STRIKE;
                        customTime = currentTick / 20.0f;
                    } else {
                        Identifier returnID = config.getAnimation(PlayerAnimationState.ATTACK_RETURN);
                        if (returnID != null) {
                            animState = PlayerAnimationState.ATTACK_RETURN;
                            float returnProgress = (currentTick - strikeLengthTicks) / (totalDurationTicks - strikeLengthTicks);
                            returnProgress = Math.max(0.0f, Math.min(1.0f, returnProgress));
                            String returnName = returnID.getPath().replace("player_animations/", "");
                            Animation returnAnim = AnimationLoader.ANIMATIONS.get(returnName);
                            if (returnAnim != null) customTime = returnProgress * returnAnim.animation_length;
                        }
                    }
                }
            }
        }

        String finalAnimName = animState.getDefaultAnimationName();
        if (config != null) {
            Identifier customAnimID = config.getAnimation(animState);
            if (customAnimID != null) {
                finalAnimName = customAnimID.getPath().replace("player_animations/", "");
            }
        }

        Animation anim = AnimationLoader.ANIMATIONS.get(finalAnimName);
        if (anim == null) return;

        // --- RESET BONES ---
        resetBoneToDefault(this.wowmod$rightHand, -1.0f, 12.0f, 0.0f);
        resetBoneToDefault(this.wowmod$leftHand, 1.0f, 12.0f, 0.0f);
        resetBoneToDefault(this.wowmod$rightItem, 0, 0, 0);
        resetBoneToDefault(this.wowmod$leftItem, 0, 0, 0);

        resetBoneToDefault(this.wowmod$rightShoulder, RIGHT_ARM_DEFAULT_X, ARM_DEFAULT_Y, 0.0f);
        resetBoneToDefault(this.wowmod$leftShoulder, LEFT_ARM_DEFAULT_X, ARM_DEFAULT_Y, 0.0f);
        resetBoneToDefault(this.wowmod$rightHip, RIGHT_LEG_DEFAULT_X, LEG_DEFAULT_Y, 0.0f);
        resetBoneToDefault(this.wowmod$leftHip, LEFT_LEG_DEFAULT_X, LEG_DEFAULT_Y, 0.0f);

        applyCustomAnimation(anim, state, animState, ext, customTime);
    }

    @Unique
    private void resetBoneToDefault(ModelPart part, float x, float y, float z) {
        if (part == null) return;
        part.pitch = 0; part.yaw = 0; part.roll = 0;
        part.originX = x; part.originY = y; part.originZ = z;
        part.xScale = 1.0f; part.yScale = 1.0f; part.zScale = 1.0f;
    }

    @Override
    public void setArmAngle(PlayerEntityRenderState state, Arm arm, MatrixStack matrices) {
        super.setArmAngle(state, arm, matrices);
        ModelPart handBone = (arm == Arm.RIGHT) ? this.wowmod$rightHand : this.wowmod$leftHand;
        ModelPart itemBone = (arm == Arm.RIGHT) ? this.wowmod$rightItem : this.wowmod$leftItem;

        if (handBone != null) {
            matrices.translate(handBone.originX / 16.0F, handBone.originY / 16.0F, handBone.originZ / 16.0F);
            if (handBone.roll != 0.0F || handBone.yaw != 0.0F || handBone.pitch != 0.0F) {
                matrices.multiply(new Quaternionf().rotationZYX(handBone.roll, handBone.yaw, handBone.pitch));
            }
            if (handBone.xScale != 1.0F || handBone.yScale != 1.0F || handBone.zScale != 1.0F) {
                matrices.scale(handBone.xScale, handBone.yScale, handBone.zScale);
            }
        }
        if (itemBone != null) {
            PlayerModelUtils.applyItemTransform(arm, matrices, itemBone);
        }
    }

    @Unique
    private PlayerAnimationState determineAnimationState(PlayerEntityRenderState state, RenderStateExtension ext) {
        if (state.isGliding || ext.wowmod$isRiding() || ext.wowmod$isClimbing() || ext.wowmod$isInWater() || ext.wowmod$isSwimming()) {
            wowmod$isCustomJumpActive = false;
            return PlayerAnimationState.VANILLA_OVERRIDE;
        }
        if (state.handSwingProgress > 0.0f) return PlayerAnimationState.STANDING_ATTACK;
        if (state.sneaking) { wowmod$isCustomJumpActive = false; return PlayerAnimationState.SNEAKING; }

        float vy = (float) ext.wowmod$getVerticalVelocity();
        boolean onGround = ext.wowmod$isOnGround();
        long timeSinceLand = ext.wowmod$getTimeSinceLanding();

        if ((onGround && vy > 0.05f) || (!onGround && vy > 0.2f)) wowmod$isCustomJumpActive = true;

        if (onGround && (wowmod$isCustomJumpActive || timeSinceLand < 10)) {
            if (timeSinceLand >= 10) wowmod$isCustomJumpActive = false;
            if (state.limbSwingAmplitude > 0.1f) return ext.wowmod$isSprinting() ? PlayerAnimationState.LANDING_SPRINTING : PlayerAnimationState.LANDING_WALKING;
            else return PlayerAnimationState.LANDING_IDLE;
        }

        if (!onGround) {
            if (wowmod$isCustomJumpActive) return PlayerAnimationState.JUMPING;
            else if (vy != 0.0f) return PlayerAnimationState.FALLING;
        }

        if (state.limbSwingAmplitude > 0.1f) return ext.wowmod$isSprinting() ? PlayerAnimationState.SPRINTING : PlayerAnimationState.WALKING;
        return PlayerAnimationState.IDLE;
    }

    @Unique
    private void applyCustomAnimation(Animation anim, PlayerEntityRenderState state, PlayerAnimationState animState, RenderStateExtension ext, float customTime) {
        long timeSinceLand = ext.wowmod$getTimeSinceLanding();
        float timeSeconds;
        if (customTime >= 0) timeSeconds = customTime;
        else if (animState == PlayerAnimationState.STANDING_ATTACK) timeSeconds = state.handSwingProgress * anim.animation_length;
        else if (animState == PlayerAnimationState.LANDING_IDLE || animState == PlayerAnimationState.LANDING_WALKING || animState == PlayerAnimationState.LANDING_SPRINTING) timeSeconds = Math.min(timeSinceLand / 10.0f * anim.animation_length, anim.animation_length);
        else timeSeconds = (state.age * 0.05f) % anim.animation_length;

        boolean isSneaking = (animState == PlayerAnimationState.SNEAKING);

        // --- CONTROLLER ---
        BoneModifier.applyBone(this.wowmod$controller, anim.bones.get("controller"), timeSeconds, 0, 0, 0);
        Quaternionf controllerRot = new Quaternionf().rotationZYX(this.wowmod$controller.roll, this.wowmod$controller.yaw, this.wowmod$controller.pitch);

        // --- BODY ---
        if (!isSneaking) {
            BoneModifier.applyBone(this.body, anim.bones.get("body"), timeSeconds, 0, 0, 0);
            this.body.pitch += this.wowmod$controller.pitch;
            this.body.yaw += this.wowmod$controller.yaw;
            this.body.roll += this.wowmod$controller.roll;

            float bodyPitch = this.body.pitch;
            float pivotY = WAIST_Y; // 12.0
            float dY = (float) (pivotY - pivotY * Math.cos(bodyPitch));
            float dZ = (float) -(pivotY * Math.sin(bodyPitch));

            Vector3f bodyPos = new Vector3f(this.body.originX, this.body.originY + dY, this.body.originZ + dZ);
            bodyPos.rotate(controllerRot);

            this.body.originX = this.wowmod$controller.originX + bodyPos.x;
            this.body.originY = this.wowmod$controller.originY + bodyPos.y;
            this.body.originZ = this.wowmod$controller.originZ + bodyPos.z;
        }
        Quaternionf bodyRot = new Quaternionf().rotationZYX(this.body.roll, this.body.yaw, this.body.pitch);

        // --- HEAD ---
        if (!isSneaking) {
            BoneModifier.applyBone(this.head, anim.bones.get("head"), timeSeconds, 0, 0, 0);
            this.head.pitch += this.body.pitch;
            this.head.yaw += this.body.yaw;
            this.head.roll += this.body.roll;

            Vector3f headPos = new Vector3f(this.head.originX, this.head.originY, this.head.originZ);
            headPos.rotate(bodyRot);

            this.head.originX = this.body.originX + headPos.x;
            this.head.originY = this.body.originY + headPos.y;
            this.head.originZ = this.body.originZ + headPos.z;
        }

        // --- LEGS & HIPS ---
        if (!isSneaking) {
            BoneModifier.applyBone(this.wowmod$rightHip, anim.bones.get("rightHip"), timeSeconds, RIGHT_LEG_DEFAULT_X, LEG_DEFAULT_Y, 0);
            BoneModifier.applyBone(this.wowmod$leftHip, anim.bones.get("leftHip"), timeSeconds, LEFT_LEG_DEFAULT_X, LEG_DEFAULT_Y, 0);

            this.wowmod$rightHip.pitch += this.wowmod$controller.pitch;
            this.wowmod$rightHip.yaw += this.wowmod$controller.yaw;
            this.wowmod$rightHip.roll += this.wowmod$controller.roll;

            this.wowmod$leftHip.pitch += this.wowmod$controller.pitch;
            this.wowmod$leftHip.yaw += this.wowmod$controller.yaw;
            this.wowmod$leftHip.roll += this.wowmod$controller.roll;

            Vector3f rightHipPos = new Vector3f(this.wowmod$rightHip.originX, this.wowmod$rightHip.originY, this.wowmod$rightHip.originZ);
            Vector3f leftHipPos = new Vector3f(this.wowmod$leftHip.originX, this.wowmod$leftHip.originY, this.wowmod$leftHip.originZ);

            rightHipPos.rotate(controllerRot);
            leftHipPos.rotate(controllerRot);

            this.wowmod$rightHip.originX = this.wowmod$controller.originX + rightHipPos.x;
            this.wowmod$rightHip.originY = this.wowmod$controller.originY + rightHipPos.y;
            this.wowmod$rightHip.originZ = this.wowmod$controller.originZ + rightHipPos.z;

            this.wowmod$leftHip.originX = this.wowmod$controller.originX + leftHipPos.x;
            this.wowmod$leftHip.originY = this.wowmod$controller.originY + leftHipPos.y;
            this.wowmod$leftHip.originZ = this.wowmod$controller.originZ + leftHipPos.z;

            BoneModifier.applyBone(this.rightLeg, anim.bones.get("rightLeg"), timeSeconds, RIGHT_LEG_DEFAULT_X, LEG_DEFAULT_Y, 0);
            BoneModifier.applyBone(this.leftLeg, anim.bones.get("leftLeg"), timeSeconds, LEFT_LEG_DEFAULT_X, LEG_DEFAULT_Y, 0);

            this.rightLeg.pitch += this.wowmod$rightHip.pitch;
            this.rightLeg.yaw += this.wowmod$rightHip.yaw;
            this.rightLeg.roll += this.wowmod$rightHip.roll;

            this.leftLeg.pitch += this.wowmod$leftHip.pitch;
            this.leftLeg.yaw += this.wowmod$leftHip.yaw;
            this.leftLeg.roll += this.wowmod$leftHip.roll;

            Quaternionf rightHipRot = new Quaternionf().rotationZYX(this.wowmod$rightHip.roll, this.wowmod$rightHip.yaw, this.wowmod$rightHip.pitch);
            Quaternionf leftHipRot = new Quaternionf().rotationZYX(this.wowmod$leftHip.roll, this.wowmod$leftHip.yaw, this.wowmod$leftHip.pitch);

            Vector3f rightLegPos = new Vector3f(this.rightLeg.originX - RIGHT_LEG_DEFAULT_X, this.rightLeg.originY - LEG_DEFAULT_Y, this.rightLeg.originZ);
            Vector3f leftLegPos = new Vector3f(this.leftLeg.originX - LEFT_LEG_DEFAULT_X, this.leftLeg.originY - LEG_DEFAULT_Y, this.leftLeg.originZ);

            rightLegPos.rotate(rightHipRot);
            leftLegPos.rotate(leftHipRot);

            this.rightLeg.originX = this.wowmod$rightHip.originX + rightLegPos.x;
            this.rightLeg.originY = this.wowmod$rightHip.originY + rightLegPos.y;
            this.rightLeg.originZ = this.wowmod$rightHip.originZ + rightLegPos.z;

            this.leftLeg.originX = this.wowmod$leftHip.originX + leftLegPos.x;
            this.leftLeg.originY = this.wowmod$leftHip.originY + leftLegPos.y;
            this.leftLeg.originZ = this.wowmod$leftHip.originZ + leftLegPos.z;
        }

        // --- ARMS & SHOULDERS ---
        boolean isAttacking = (animState == PlayerAnimationState.STANDING_ATTACK ||
                animState == PlayerAnimationState.ATTACK_STRIKE ||
                animState == PlayerAnimationState.ATTACK_RETURN);
        boolean shouldAnimateArms = isAttacking || (state.handSwingProgress == 0.0f && !state.isUsingItem);

        float vanillaRightPitch = this.rightArm.pitch;
        float vanillaRightYaw = this.rightArm.yaw;
        float vanillaRightRoll = this.rightArm.roll;
        float vanillaLeftPitch = this.leftArm.pitch;
        float vanillaLeftYaw = this.leftArm.yaw;
        float vanillaLeftRoll = this.leftArm.roll;

        if (shouldAnimateArms) {
            // Apply Shoulders
            BoneModifier.applyBone(this.wowmod$rightShoulder, anim.bones.get("rightShoulder"), timeSeconds, RIGHT_ARM_DEFAULT_X, ARM_DEFAULT_Y, 0.0f);
            BoneModifier.applyBone(this.wowmod$leftShoulder, anim.bones.get("leftShoulder"), timeSeconds, LEFT_ARM_DEFAULT_X, ARM_DEFAULT_Y, 0.0f);

            this.wowmod$rightShoulder.pitch += this.body.pitch;
            this.wowmod$rightShoulder.yaw += this.body.yaw;
            this.wowmod$rightShoulder.roll += this.body.roll;

            this.wowmod$leftShoulder.pitch += this.body.pitch;
            this.wowmod$leftShoulder.yaw += this.body.yaw;
            this.wowmod$leftShoulder.roll += this.body.roll;

            // FIX: ATTACH TO BODY BONE (NECK) DIRECTLY to prevent detachment
            // Body bone has already been rotated by controller.
            // We just need to rotate the relative shoulder position by the body rotation.

            // Relative vector from Body(0,0,0) to Shoulder
            Vector3f rightRel = new Vector3f(this.wowmod$rightShoulder.originX, this.wowmod$rightShoulder.originY, this.wowmod$rightShoulder.originZ);
            rightRel.rotate(bodyRot);
            this.wowmod$rightShoulder.originX = this.body.originX + rightRel.x;
            this.wowmod$rightShoulder.originY = this.body.originY + rightRel.y;
            this.wowmod$rightShoulder.originZ = this.body.originZ + rightRel.z;

            Vector3f leftRel = new Vector3f(this.wowmod$leftShoulder.originX, this.wowmod$leftShoulder.originY, this.wowmod$leftShoulder.originZ);
            leftRel.rotate(bodyRot);
            this.wowmod$leftShoulder.originX = this.body.originX + leftRel.x;
            this.wowmod$leftShoulder.originY = this.body.originY + leftRel.y;
            this.wowmod$leftShoulder.originZ = this.body.originZ + leftRel.z;

            // Apply Arms (Child of Shoulder)
            // Arms default is 0 relative to shoulder in our system
            Animation.Bone rightArmBone = anim.bones.get("rightArm");
            if (rightArmBone != null) BoneModifier.applyBone(this.rightArm, rightArmBone, timeSeconds, 0.0f, 0.0f, 0.0f);
            else { this.rightArm.pitch = 0; this.rightArm.yaw = 0; this.rightArm.roll = 0; this.rightArm.originX = 0; this.rightArm.originY = 0; this.rightArm.originZ = 0; }

            Animation.Bone leftArmBone = anim.bones.get("leftArm");
            if (leftArmBone != null) BoneModifier.applyBone(this.leftArm, leftArmBone, timeSeconds, 0.0f, 0.0f, 0.0f);
            else { this.leftArm.pitch = 0; this.leftArm.yaw = 0; this.leftArm.roll = 0; this.leftArm.originX = 0; this.leftArm.originY = 0; this.leftArm.originZ = 0; }

            this.rightArm.pitch += this.wowmod$rightShoulder.pitch;
            this.rightArm.yaw += this.wowmod$rightShoulder.yaw;
            this.rightArm.roll += this.wowmod$rightShoulder.roll;

            this.leftArm.pitch += this.wowmod$leftShoulder.pitch;
            this.leftArm.yaw += this.wowmod$leftShoulder.yaw;
            this.leftArm.roll += this.wowmod$leftShoulder.roll;

            Quaternionf rightShoulderRot = new Quaternionf().rotationZYX(this.wowmod$rightShoulder.roll, this.wowmod$rightShoulder.yaw, this.wowmod$rightShoulder.pitch);
            Quaternionf leftShoulderRot = new Quaternionf().rotationZYX(this.wowmod$leftShoulder.roll, this.wowmod$leftShoulder.yaw, this.wowmod$leftShoulder.pitch);

            Vector3f rightArmPos = new Vector3f(this.rightArm.originX, this.rightArm.originY, this.rightArm.originZ);
            Vector3f leftArmPos = new Vector3f(this.leftArm.originX, this.leftArm.originY, this.leftArm.originZ);

            rightArmPos.rotate(rightShoulderRot);
            leftArmPos.rotate(leftShoulderRot);

            this.rightArm.originX = this.wowmod$rightShoulder.originX + rightArmPos.x;
            this.rightArm.originY = this.wowmod$rightShoulder.originY + rightArmPos.y;
            this.rightArm.originZ = this.wowmod$rightShoulder.originZ + rightArmPos.z;

            this.leftArm.originX = this.wowmod$leftShoulder.originX + leftArmPos.x;
            this.leftArm.originY = this.wowmod$leftShoulder.originY + leftArmPos.y;
            this.leftArm.originZ = this.wowmod$leftShoulder.originZ + leftArmPos.z;

        } else {
            // Vanilla Fallback
            this.rightArm.pitch = vanillaRightPitch + this.body.pitch;
            this.rightArm.yaw = vanillaRightYaw + this.body.yaw;
            this.rightArm.roll = vanillaRightRoll + this.body.roll;
            this.rightArm.originX = RIGHT_ARM_DEFAULT_X; this.rightArm.originY = ARM_DEFAULT_Y; this.rightArm.originZ = 0.0f;

            this.leftArm.pitch = vanillaLeftPitch + this.body.pitch;
            this.leftArm.yaw = vanillaLeftYaw + this.body.yaw;
            this.leftArm.roll = vanillaLeftRoll + this.body.roll;
            this.leftArm.originX = LEFT_ARM_DEFAULT_X; this.leftArm.originY = ARM_DEFAULT_Y; this.leftArm.originZ = 0.0f;

            // Simple body-relative attachment
            Vector3f rightArmPos = new Vector3f(this.rightArm.originX, this.rightArm.originY, this.rightArm.originZ);
            rightArmPos.rotate(bodyRot);
            this.rightArm.originX = this.body.originX + rightArmPos.x;
            this.rightArm.originY = this.body.originY + rightArmPos.y;
            this.rightArm.originZ = this.body.originZ + rightArmPos.z;

            Vector3f leftArmPos = new Vector3f(this.leftArm.originX, this.leftArm.originY, this.leftArm.originZ);
            leftArmPos.rotate(bodyRot);
            this.leftArm.originX = this.body.originX + leftArmPos.x;
            this.leftArm.originY = this.body.originY + leftArmPos.y;
            this.leftArm.originZ = this.body.originZ + leftArmPos.z;
        }

        // --- HANDS & ITEMS ---
        BoneModifier.applyBone(this.wowmod$rightHand, anim.bones.get("rightHand"), timeSeconds, -1f, 12.0f, 0);
        BoneModifier.applyBone(this.wowmod$leftHand, anim.bones.get("leftHand"), timeSeconds, 1f, 12.0f, 0);
        BoneModifier.applyBone(this.wowmod$rightItem, anim.bones.get("rightItem"), timeSeconds, 0, 0.0f, 0);
        BoneModifier.applyBone(this.wowmod$leftItem, anim.bones.get("leftItem"), timeSeconds, 0, 0.0f, 0);
    }
}