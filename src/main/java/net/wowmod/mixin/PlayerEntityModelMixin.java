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
    @Unique public ModelPart wowmod$controller; // The new Controller Bone
    @Unique public ModelPart wowmod$rightItem;
    @Unique public ModelPart wowmod$leftItem;

    public PlayerEntityModelMixin(ModelPart root) {
        super(root);
    }

    // Initialize the virtual bones
    @Inject(method = "<init>", at = @At("TAIL"))
    public void initItemBones(ModelPart root, boolean thinArms, CallbackInfo ci) {
        // Create empty parts (no cubes) solely for transformation
        this.wowmod$controller = new ModelPart(Collections.emptyList(), Collections.emptyMap());
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
        if (state.isGliding || ext.wowmod$isRiding() || ext.wowmod$isClimbing() || ext.wowmod$isInWater() || ext.wowmod$isSwimming()) {
            wowmod$isCustomJumpActive = false;
            return PlayerAnimationState.VANILLA_OVERRIDE;
        }

        // --- NEW: Attack Detection ---
        // If handSwingProgress > 0, we are in the middle of a swing
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

        // FIX: Allow activation if strictly on ground OR if clearly moving up while airborne
        // Adding (!onGround && vy > 0.2f) helps catch the frame immediately after leaving the ground
        if ((onGround && vy > 0.05f) || (!onGround && vy > 0.2f)) {
            wowmod$isCustomJumpActive = true;
        }

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
    private void applyCustomAnimation(Animation anim, PlayerEntityRenderState state, PlayerAnimationState animState, RenderStateExtension ext) {
        long timeSinceLand = ext.wowmod$getTimeSinceLanding();
        float timeSeconds;

        if (animState == PlayerAnimationState.STANDING_ATTACK) {
            timeSeconds = state.handSwingProgress * anim.animation_length;
        } else if (animState == PlayerAnimationState.LANDING_IDLE ||
                animState == PlayerAnimationState.LANDING_WALKING ||
                animState == PlayerAnimationState.LANDING_SPRINTING) {
            timeSeconds = Math.min(timeSinceLand / 10.0f * anim.animation_length, anim.animation_length);
        } else {
            timeSeconds = (state.age * 0.05f) % anim.animation_length;
        }

        boolean isSneaking = (animState == PlayerAnimationState.SNEAKING);

        // --- 0. Animate Controller Bone ---
        BoneModifier.applyBone(this.wowmod$controller, anim.bones.get("controller"), timeSeconds, 0, 0, 0);

        // Calculate Controller Rotation Matrix
        Quaternionf controllerRot = new Quaternionf().rotationZYX(
                this.wowmod$controller.roll,
                this.wowmod$controller.yaw,
                this.wowmod$controller.pitch
        );

        // --- 1. Apply Body Animation ---
        if (!isSneaking) {
            BoneModifier.applyBone(this.body, anim.bones.get("body"), timeSeconds, 0, 0, 0);

            // Add rotation (simple addition for Euler angles)
            this.body.pitch += this.wowmod$controller.pitch;
            this.body.yaw += this.wowmod$controller.yaw;
            this.body.roll += this.wowmod$controller.roll;

            // Apply Waist Pivot Logic (Leaning)
            float bodyPitch = this.body.pitch;
            float pivotY = 12.0f;
            float dY = (float) (pivotY - pivotY * Math.cos(bodyPitch));
            float dZ = (float) -(pivotY * Math.sin(bodyPitch));

            // Get local position and add pivot offset
            Vector3f bodyPos = new Vector3f(this.body.originX, this.body.originY + dY, this.body.originZ + dZ);

            // Orbit Body around Controller
            bodyPos.rotate(controllerRot);

            // Set Final Position
            this.body.originX = this.wowmod$controller.originX + bodyPos.x;
            this.body.originY = this.wowmod$controller.originY + bodyPos.y;
            this.body.originZ = this.wowmod$controller.originZ + bodyPos.z;
        }

        // Calculate Body Rotation Matrix (for children: Head/Arms)
        Quaternionf bodyRot = new Quaternionf().rotationZYX(
                this.body.roll,
                this.body.yaw,
                this.body.pitch
        );

        // --- 2. Apply Head Animation ---
        if (!isSneaking) {
            BoneModifier.applyBone(this.head, anim.bones.get("head"), timeSeconds, 0, 0, 0);

            // Add rotation
            this.head.pitch += this.body.pitch;
            this.head.yaw += this.body.yaw;
            this.head.roll += this.body.roll;

            // Orbit Head around Body
            Vector3f headPos = new Vector3f(this.head.originX, this.head.originY, this.head.originZ);
            headPos.rotate(bodyRot);

            this.head.originX = this.body.originX + headPos.x;
            this.head.originY = this.body.originY + headPos.y;
            this.head.originZ = this.body.originZ + headPos.z;
        }

        // --- 3. Apply Leg Animations (Independent of Body, Parented to Controller) ---
        if (!isSneaking) {
            BoneModifier.applyBone(this.rightLeg, anim.bones.get("rightLeg"), timeSeconds, -1.9f, 12.0f, 0);
            BoneModifier.applyBone(this.leftLeg, anim.bones.get("leftLeg"), timeSeconds, 1.9f, 12.0f, 0);

            this.rightLeg.pitch += this.wowmod$controller.pitch;
            this.rightLeg.yaw += this.wowmod$controller.yaw;
            this.rightLeg.roll += this.wowmod$controller.roll;

            this.leftLeg.pitch += this.wowmod$controller.pitch;
            this.leftLeg.yaw += this.wowmod$controller.yaw;
            this.leftLeg.roll += this.wowmod$controller.roll;

            // Orbit Legs around Controller
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

        // --- 4. Animate Arms (Parented to Body) ---
        boolean isAttacking = (animState == PlayerAnimationState.STANDING_ATTACK);
        boolean shouldAnimateArms = isAttacking || (state.handSwingProgress == 0.0f && !state.isUsingItem);

        // Save vanilla rotations
        float vanillaRightPitch = this.rightArm.pitch;
        float vanillaRightYaw = this.rightArm.yaw;
        float vanillaRightRoll = this.rightArm.roll;

        float vanillaLeftPitch = this.leftArm.pitch;
        float vanillaLeftYaw = this.leftArm.yaw;
        float vanillaLeftRoll = this.leftArm.roll;

        // RIGHT ARM
        Animation.Bone rightArmBone = anim.bones.get("rightArm");
        if (shouldAnimateArms) {
            // Apply Custom (Offset -5, 2, 0 passed as default)
            if (rightArmBone != null) {
                BoneModifier.applyBone(this.rightArm, rightArmBone, timeSeconds, -5.0f, 2.0f, 0.0f);
            } else {
                // Reset to default if bone missing but animating
                this.rightArm.pitch = 0; this.rightArm.yaw = 0; this.rightArm.roll = 0;
                this.rightArm.originX = -5.0f; this.rightArm.originY = 2.0f; this.rightArm.originZ = 0.0f;
            }
            this.rightArm.pitch += this.body.pitch;
            this.rightArm.yaw += this.body.yaw;
            this.rightArm.roll += this.body.roll;
        } else {
            // Restore Vanilla + Parent Rotation
            this.rightArm.pitch = vanillaRightPitch + this.body.pitch;
            this.rightArm.yaw = vanillaRightYaw + this.body.yaw;
            this.rightArm.roll = vanillaRightRoll + this.body.roll;
            // Reset position to default offset so it can be rotated correctly
            this.rightArm.originX = -5.0f; this.rightArm.originY = 2.0f; this.rightArm.originZ = 0.0f;
        }

        // Orbit Right Arm around Body
        Vector3f rightArmPos = new Vector3f(this.rightArm.originX, this.rightArm.originY, this.rightArm.originZ);
        rightArmPos.rotate(bodyRot);
        this.rightArm.originX = this.body.originX + rightArmPos.x;
        this.rightArm.originY = this.body.originY + rightArmPos.y;
        this.rightArm.originZ = this.body.originZ + rightArmPos.z;


        // LEFT ARM
        Animation.Bone leftArmBone = anim.bones.get("leftArm");
        if (shouldAnimateArms) {
            // Apply Custom (Offset 5, 2, 0 passed as default)
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

        // Orbit Left Arm around Body
        Vector3f leftArmPos = new Vector3f(this.leftArm.originX, this.leftArm.originY, this.leftArm.originZ);
        leftArmPos.rotate(bodyRot);
        this.leftArm.originX = this.body.originX + leftArmPos.x;
        this.leftArm.originY = this.body.originY + leftArmPos.y;
        this.leftArm.originZ = this.body.originZ + leftArmPos.z;


        // --- 5. Animate Items (Parented to Hand) ---
        BoneModifier.applyBone(this.wowmod$rightItem, anim.bones.get("rightItem"), timeSeconds, 0, 0, 0);
        BoneModifier.applyBone(this.wowmod$leftItem, anim.bones.get("leftItem"), timeSeconds, 0, 0, 0);
    }
}