package net.wowmod.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

        // --- SPLIT ATTACK LOGIC ---
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

                            if (returnAnim != null) {
                                customTime = returnProgress * returnAnim.animation_length;
                            }
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

        // HANDS: Offset from Shoulder to Wrist
        // Defaults to +/- 1.0f (6 - 5)
        this.wowmod$rightHand.pitch = 0; this.wowmod$rightHand.yaw = 0; this.wowmod$rightHand.roll = 0;
        this.wowmod$rightHand.originX = -1.0f; this.wowmod$rightHand.originY = 12.0f; this.wowmod$rightHand.originZ = 0.0f;
        this.wowmod$rightHand.xScale = 1.0f; this.wowmod$rightHand.yScale = 1.0f; this.wowmod$rightHand.zScale = 1.0f;

        this.wowmod$leftHand.pitch = 0; this.wowmod$leftHand.yaw = 0; this.wowmod$leftHand.roll = 0;
        this.wowmod$leftHand.originX = 1.0f; this.wowmod$leftHand.originY = 12.0f; this.wowmod$leftHand.originZ = 0.0f;
        this.wowmod$leftHand.xScale = 1.0f; this.wowmod$leftHand.yScale = 1.0f; this.wowmod$leftHand.zScale = 1.0f;

        // ITEMS: Defaults to Wrist Pivot (0,0,0) relative to Hand
        this.wowmod$rightItem.pitch = 0; this.wowmod$rightItem.yaw = 0; this.wowmod$rightItem.roll = 0;
        this.wowmod$rightItem.originX = 0; this.wowmod$rightItem.originY = 0; this.wowmod$rightItem.originZ = 0;
        this.wowmod$rightItem.xScale = 1.0f; this.wowmod$rightItem.yScale = 1.0f; this.wowmod$rightItem.zScale = 1.0f;

        this.wowmod$leftItem.pitch = 0; this.wowmod$leftItem.yaw = 0; this.wowmod$leftItem.roll = 0;
        this.wowmod$leftItem.originX = 0; this.wowmod$leftItem.originY = 0; this.wowmod$leftItem.originZ = 0;
        this.wowmod$leftItem.xScale = 1.0f; this.wowmod$leftItem.yScale = 1.0f; this.wowmod$leftItem.zScale = 1.0f;

        applyCustomAnimation(anim, state, animState, ext, customTime);
    }

    @Override
    public void setArmAngle(PlayerEntityRenderState state, Arm arm, MatrixStack matrices) {
        super.setArmAngle(state, arm, matrices);

        ModelPart handBone = (arm == Arm.RIGHT) ? this.wowmod$rightHand : this.wowmod$leftHand;
        ModelPart itemBone = (arm == Arm.RIGHT) ? this.wowmod$rightItem : this.wowmod$leftItem;

        // 1. Hand (Wrist Pivot)
        if (handBone != null) {
            matrices.translate(handBone.originX / 16.0F, handBone.originY / 16.0F, handBone.originZ / 16.0F);
            if (handBone.roll != 0.0F || handBone.yaw != 0.0F || handBone.pitch != 0.0F) {
                matrices.multiply(new Quaternionf().rotationZYX(handBone.roll, handBone.yaw, handBone.pitch));
            }
            if (handBone.xScale != 1.0F || handBone.yScale != 1.0F || handBone.zScale != 1.0F) {
                matrices.scale(handBone.xScale, handBone.yScale, handBone.zScale);
            }
        }

        // 2. Item (Stable "Cancel" Logic)
        if (itemBone != null) {

            // --- [ADJUST THESE VALUES TO FINE TUNE] ---

            // HEIGHT: Moves item UP (negative) or DOWN (positive).
            // Start small (-1.0f or -2.0f).
            float visualHeightOffset = -3.0f;

            // SIDEWAYS: Moves item LEFT/RIGHT relative to the body.
            // Negative moves OUT away from body. Positive moves IN.
            float visualBodyOffset = 1f;

            // ANTI-ORBIT: This cancels the Vanilla Item Renderer offset.
            // If the item "swings" around the handle when rotating, CHANGE THIS.
            // Try values between 10.0f and 12.0f until the spin looks centered.
            float vanillaCancelOffset = 9f;

            // ------------------------------------------

            float xOffset = (arm == Arm.RIGHT) ? visualBodyOffset : -visualBodyOffset;

            // STEP A: Translate to Pivot + Apply Visual Offsets
            // Note: Changing visual offsets here technically moves the pivot point slightly.
            matrices.translate((itemBone.originX + xOffset) / 16.0F, (itemBone.originY + visualHeightOffset) / 16.0F, itemBone.originZ / 16.0F);

            // STEP B: Apply Rotation
            if (itemBone.roll != 0.0F || itemBone.yaw != 0.0F || itemBone.pitch != 0.0F) {
                matrices.multiply(new Quaternionf().rotationZYX(itemBone.roll, itemBone.yaw, itemBone.pitch));
            }

            // STEP C: Return & Cancel
            // We return from the pivot (origin), but we add the 'vanillaCancelOffset' to neutralize Vanilla's translation.
            // Note: We do NOT subtract the visual offsets here. This leaves them applied.
            matrices.translate(-itemBone.originX / 16.0F, (-itemBone.originY - vanillaCancelOffset) / 16.0F, -itemBone.originZ / 16.0F);

            if (itemBone.xScale != 1.0F || itemBone.yScale != 1.0F || itemBone.zScale != 1.0F) {
                matrices.scale(itemBone.xScale, itemBone.yScale, itemBone.zScale);
            }
        }
    }

    @Unique
    private PlayerAnimationState determineAnimationState(PlayerEntityRenderState state, RenderStateExtension ext) {
        if (state.isGliding || ext.wowmod$isRiding() || ext.wowmod$isClimbing() || ext.wowmod$isInWater() || ext.wowmod$isSwimming()) {
            wowmod$isCustomJumpActive = false;
            return PlayerAnimationState.VANILLA_OVERRIDE;
        }

        if (state.handSwingProgress > 0.0f) {
            return PlayerAnimationState.STANDING_ATTACK;
        }

        if (state.sneaking) {
            wowmod$isCustomJumpActive = false;
            return PlayerAnimationState.SNEAKING;
        }

        float vy = (float) ext.wowmod$getVerticalVelocity();
        boolean onGround = ext.wowmod$isOnGround();
        long timeSinceLand = ext.wowmod$getTimeSinceLanding();

        if ((onGround && vy > 0.05f) || (!onGround && vy > 0.2f)) {
            wowmod$isCustomJumpActive = true;
        }

        if (onGround && (wowmod$isCustomJumpActive || timeSinceLand < 10)) {
            if (timeSinceLand >= 10) {
                wowmod$isCustomJumpActive = false;
            }
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

        if (!onGround) {
            if (wowmod$isCustomJumpActive) {
                return PlayerAnimationState.JUMPING;
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
    private void applyCustomAnimation(Animation anim, PlayerEntityRenderState state, PlayerAnimationState animState, RenderStateExtension ext, float customTime) {
        long timeSinceLand = ext.wowmod$getTimeSinceLanding();
        float timeSeconds;

        if (customTime >= 0) {
            timeSeconds = customTime;
        } else if (animState == PlayerAnimationState.STANDING_ATTACK) {
            timeSeconds = state.handSwingProgress * anim.animation_length;
        } else if (animState == PlayerAnimationState.LANDING_IDLE ||
                animState == PlayerAnimationState.LANDING_WALKING ||
                animState == PlayerAnimationState.LANDING_SPRINTING) {
            timeSeconds = Math.min(timeSinceLand / 10.0f * anim.animation_length, anim.animation_length);
        } else {
            timeSeconds = (state.age * 0.05f) % anim.animation_length;
        }

        boolean isSneaking = (animState == PlayerAnimationState.SNEAKING);

        BoneModifier.applyBone(this.wowmod$controller, anim.bones.get("controller"), timeSeconds, 0, 0, 0);

        Quaternionf controllerRot = new Quaternionf().rotationZYX(
                this.wowmod$controller.roll,
                this.wowmod$controller.yaw,
                this.wowmod$controller.pitch
        );

        if (!isSneaking) {
            BoneModifier.applyBone(this.body, anim.bones.get("body"), timeSeconds, 0, 0, 0);
            this.body.pitch += this.wowmod$controller.pitch;
            this.body.yaw += this.wowmod$controller.yaw;
            this.body.roll += this.wowmod$controller.roll;

            float bodyPitch = this.body.pitch;
            float pivotY = 12.0f;
            float dY = (float) (pivotY - pivotY * Math.cos(bodyPitch));
            float dZ = (float) -(pivotY * Math.sin(bodyPitch));

            Vector3f bodyPos = new Vector3f(this.body.originX, this.body.originY + dY, this.body.originZ + dZ);
            bodyPos.rotate(controllerRot);

            this.body.originX = this.wowmod$controller.originX + bodyPos.x;
            this.body.originY = this.wowmod$controller.originY + bodyPos.y;
            this.body.originZ = this.wowmod$controller.originZ + bodyPos.z;
        }

        Quaternionf bodyRot = new Quaternionf().rotationZYX(
                this.body.roll,
                this.body.yaw,
                this.body.pitch
        );

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

        if (!isSneaking) {
            BoneModifier.applyBone(this.rightLeg, anim.bones.get("rightLeg"), timeSeconds, -1.9f, 12.0f, 0);
            BoneModifier.applyBone(this.leftLeg, anim.bones.get("leftLeg"), timeSeconds, 1.9f, 12.0f, 0);

            this.rightLeg.pitch += this.wowmod$controller.pitch;
            this.rightLeg.yaw += this.wowmod$controller.yaw;
            this.rightLeg.roll += this.wowmod$controller.roll;

            this.leftLeg.pitch += this.wowmod$controller.pitch;
            this.leftLeg.yaw += this.wowmod$controller.yaw;
            this.leftLeg.roll += this.wowmod$controller.roll;

            Vector3f rightLegPos = new Vector3f(this.rightLeg.originX, this.rightLeg.originY, this.rightLeg.originZ);
            Vector3f leftLegPos = new Vector3f(this.leftLeg.originX, this.leftLeg.originY, this.leftLeg.originZ);

            rightLegPos.rotate(controllerRot);
            leftLegPos.rotate(controllerRot);

            this.rightLeg.originX = this.wowmod$controller.originX + rightLegPos.x;
            this.rightLeg.originY = this.wowmod$controller.originY + rightLegPos.y;
            this.rightLeg.originZ = this.wowmod$controller.originZ + rightLegPos.z;

            this.leftLeg.originX = this.wowmod$controller.originX + leftLegPos.x;
            this.leftLeg.originY = this.wowmod$controller.originY + leftLegPos.y;
            this.leftLeg.originZ = this.wowmod$controller.originZ + leftLegPos.z;
        }

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

        Animation.Bone rightArmBone = anim.bones.get("rightArm");
        if (shouldAnimateArms) {
            if (rightArmBone != null) {
                BoneModifier.applyBone(this.rightArm, rightArmBone, timeSeconds, -5.0f, 2.0f, 0.0f);
            } else {
                this.rightArm.pitch = 0; this.rightArm.yaw = 0; this.rightArm.roll = 0;
                this.rightArm.originX = -5.0f; this.rightArm.originY = 2.0f; this.rightArm.originZ = 0.0f;
            }
            this.rightArm.pitch += this.body.pitch;
            this.rightArm.yaw += this.body.yaw;
            this.rightArm.roll += this.body.roll;
        } else {
            this.rightArm.pitch = vanillaRightPitch + this.body.pitch;
            this.rightArm.yaw = vanillaRightYaw + this.body.yaw;
            this.rightArm.roll = vanillaRightRoll + this.body.roll;
            this.rightArm.originX = -5.0f; this.rightArm.originY = 2.0f; this.rightArm.originZ = 0.0f;
        }

        Vector3f rightArmPos = new Vector3f(this.rightArm.originX, this.rightArm.originY, this.rightArm.originZ);
        rightArmPos.rotate(bodyRot);
        this.rightArm.originX = this.body.originX + rightArmPos.x;
        this.rightArm.originY = this.body.originY + rightArmPos.y;
        this.rightArm.originZ = this.body.originZ + rightArmPos.z;

        Animation.Bone leftArmBone = anim.bones.get("leftArm");
        if (shouldAnimateArms) {
            if (leftArmBone != null) {
                BoneModifier.applyBone(this.leftArm, leftArmBone, timeSeconds, 5.0f, 2.0f, 0.0f);
            } else {
                this.leftArm.pitch = 0; this.leftArm.yaw = 0; this.leftArm.roll = 0;
                this.leftArm.originX = 5.0f; this.leftArm.originY = 2.0f; this.leftArm.originZ = 0.0f;
            }
            this.leftArm.pitch += this.body.pitch;
            this.leftArm.yaw += this.body.yaw;
            this.leftArm.roll += this.body.roll;
        } else {
            this.leftArm.pitch = vanillaLeftPitch + this.body.pitch;
            this.leftArm.yaw = vanillaLeftYaw + this.body.yaw;
            this.leftArm.roll = vanillaLeftRoll + this.body.roll;
            this.leftArm.originX = 5.0f; this.leftArm.originY = 2.0f; this.leftArm.originZ = 0.0f;
        }

        Vector3f leftArmPos = new Vector3f(this.leftArm.originX, this.leftArm.originY, this.leftArm.originZ);
        leftArmPos.rotate(bodyRot);
        this.leftArm.originX = this.body.originX + leftArmPos.x;
        this.leftArm.originY = this.body.originY + leftArmPos.y;
        this.leftArm.originZ = this.body.originZ + leftArmPos.z;

        // --- HANDS & ITEMS (Wrist Pivots) ---
        // 1. Move Hand to Wrist (Pivot Point)
        BoneModifier.applyBone(this.wowmod$rightHand, anim.bones.get("rightHand"), timeSeconds, -1f, 12.0f, 0);
        BoneModifier.applyBone(this.wowmod$leftHand, anim.bones.get("leftHand"), timeSeconds, 1f, 12.0f, 0);

        // 2. Move Item Bones
        BoneModifier.applyBone(this.wowmod$rightItem, anim.bones.get("rightItem"), timeSeconds, 0, 0.0f, 0);
        BoneModifier.applyBone(this.wowmod$leftItem, anim.bones.get("leftItem"), timeSeconds, 0, 0.0f, 0);
    }
}