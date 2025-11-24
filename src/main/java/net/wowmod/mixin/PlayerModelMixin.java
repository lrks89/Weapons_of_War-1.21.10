package net.wowmod.mixin;

import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.wowmod.animation.*;
import net.wowmod.animation.PlayerAnimationManager.AnimationState;
import net.wowmod.item.custom.ParryShieldItem;
import net.wowmod.item.custom.WeaponItem;
import net.wowmod.util.IPlayerStateExtension;
import net.wowmod.util.ModelPartUtils;
import net.wowmod.WeaponsOfWar;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(PlayerEntityModel.class)
public abstract class PlayerModelMixin extends BipedEntityModel<PlayerEntityRenderState> {

    @Shadow public final ModelPart leftSleeve;
    @Shadow public final ModelPart rightSleeve;
    @Shadow public final ModelPart leftPants;
    @Shadow public final ModelPart rightPants;
    @Shadow public final ModelPart jacket;

    private static final Identifier DEFAULT_IDLE = Identifier.of(WeaponsOfWar.MOD_ID, "default_idle");
    private static final Identifier DEFAULT_WALK = Identifier.of(WeaponsOfWar.MOD_ID, "default_walking");
    private static final Identifier DEFAULT_SPRINT = Identifier.of(WeaponsOfWar.MOD_ID, "default_sprinting");

    public PlayerModelMixin(ModelPart root) {
        super(root);
        this.leftSleeve = root.getChild("left_sleeve");
        this.rightSleeve = root.getChild("right_sleeve");
        this.leftPants = root.getChild("left_pants");
        this.rightPants = root.getChild("right_pants");
        this.jacket = root.getChild("jacket");
    }

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    public void applyCustomAnimations(PlayerEntityRenderState state, CallbackInfo ci) {

        ModelPartUtils.initialize(this.rightArm);

        if (!(state instanceof IPlayerStateExtension extension)) return;
        PlayerEntity player = extension.wowmod_getPlayer();
        if (player == null) return;

        AnimationContext context = determineCurrentAnimation(player);

        // If context is null (e.g. blocking or sneaking), we return early.
        // This allows the vanilla model logic (which ran before this injection) to persist.
        if (context == null || context.id() == null) return;

        AnimationDefinition animDef = AnimationLoader.ANIMATIONS.get(context.id());
        if (animDef == null) return;

        float timePointer = context.elapsedTime();
        if (animDef.loop()) timePointer = timePointer % animDef.length();

        // 1. Capture Vanilla Bases
        float baseBodyY = ModelPartUtils.getY(this.body);
        float baseLegZ = ModelPartUtils.getZ(this.rightLeg);

        // 2. SNEAK ADJUSTMENTS
        boolean isSneaking = player.isInSneakingPose();

        float sneakYOffset = isSneaking ? 3.2F : 0.0F;
        float sneakZOffset = isSneaking ? 3.0F : 0.0F;
        float sneakHeightFix = isSneaking ? -2.0F : 0.0F;
        float sneakHeadY = isSneaking ? 1.0F : 0.0F;

        // 3. Apply Animation Rotations

        // Head & Headwear
        applyRotation(animDef, "head", this.head, timePointer, true);
        applyRotation(animDef, "headwear", this.hat, timePointer, true);

        // Body & Jacket
        applyRotation(animDef, "body", this.body, timePointer, true);
        applyRotation(animDef, "jacket", this.jacket, timePointer, true);

        // --- VANILLA ATTACK RESTORATION LOGIC ---

        // Get the requested action state (e.g., "Attack")
        AnimationState activeState = PlayerAnimationManager.getState(player);

        boolean isPlayingAction = activeState != null && context.id().equals(activeState.id);

        boolean skipRightArm = false;
        boolean skipLeftArm = false;

        // If we are NOT successfully playing a custom attack animation, AND the player is swinging...
        if (!isPlayingAction && player.handSwingProgress > 0) {
            if (player.preferredHand == Hand.MAIN_HAND) {
                if (player.getMainArm() == Arm.RIGHT) skipRightArm = true;
                else skipLeftArm = true;
            } else {
                if (player.getMainArm() == Arm.RIGHT) skipLeftArm = true;
                else skipRightArm = true;
            }
        }

        // Only apply custom arm animation if we aren't letting the vanilla swing happen
        if (!skipRightArm) {
            applyRotation(animDef, "right_arm", this.rightArm, timePointer, false);
            applyRotation(animDef, "right_sleeve", this.rightSleeve, timePointer, false);
        }

        if (!skipLeftArm) {
            applyRotation(animDef, "left_arm", this.leftArm, timePointer, false);
            applyRotation(animDef, "left_sleeve", this.leftSleeve, timePointer, false);
        }

        // Legs & Pants always animate
        applyRotation(animDef, "right_leg", this.rightLeg, timePointer, false);
        applyRotation(animDef, "right_pants", this.rightPants, timePointer, false);
        applyRotation(animDef, "left_leg", this.leftLeg, timePointer, false);
        applyRotation(animDef, "left_pants", this.leftPants, timePointer, false);


        // 4. HIP ANCHORING
        Vec3d anchorOffset = calculateHipAnchorOffset(this.body, sneakYOffset);
        Vec3d bodyAnimDelta = getPositionDelta(animDef, "body", timePointer);

        float bodyFinalX = 0.0F + (float) bodyAnimDelta.x + (float) anchorOffset.x;
        float bodyFinalY = baseBodyY + (float) -bodyAnimDelta.y + (float) anchorOffset.y + sneakHeightFix;
        float bodyFinalZ = 0.0F + (float) bodyAnimDelta.z + (float) anchorOffset.z + baseLegZ + sneakZOffset;

        ModelPartUtils.setPivot(this.body, bodyFinalX, bodyFinalY, bodyFinalZ);

        // 5. ATTACH LIMBS (Arms AND Legs)
        syncLimbsToBody(this.body, this.head, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg, sneakHeadY);

        // 6. Apply Leg Positions (After Syncing)
        applyPosition(animDef, "right_leg", this.rightLeg, timePointer);
        applyPosition(animDef, "left_leg", this.leftLeg, timePointer);
    }

    private void syncLimbsToBody(ModelPart body, ModelPart head, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg, float sneakHeadY) {
        float neckX = ModelPartUtils.getX(body);
        float neckY = ModelPartUtils.getY(body);
        float neckZ = ModelPartUtils.getZ(body);

        // --- Head ---
        ModelPartUtils.setPivot(head, neckX, neckY + sneakHeadY, neckZ);

        // --- Arms ---
        Vec3d rShoulder = rotateVector(new Vec3d(-5, 2, 0), body.pitch, body.yaw, body.roll);
        Vec3d lShoulder = rotateVector(new Vec3d(5, 2, 0), body.pitch, body.yaw, body.roll);

        ModelPartUtils.setPivot(rightArm, neckX + (float)rShoulder.x, neckY + (float)rShoulder.y, neckZ + (float)rShoulder.z);
        ModelPartUtils.setPivot(leftArm, neckX + (float)lShoulder.x, neckY + (float)lShoulder.y, neckZ + (float)lShoulder.z);

        // --- Legs ---
        Vec3d rLegPivot = rotateVector(new Vec3d(-1.9, 12, 0), body.pitch, body.yaw, body.roll);
        Vec3d lLegPivot = rotateVector(new Vec3d(1.9, 12, 0), body.pitch, body.yaw, body.roll);

        ModelPartUtils.setPivot(rightLeg, neckX + (float)rLegPivot.x, neckY + (float)rLegPivot.y, neckZ + (float)rLegPivot.z);
        ModelPartUtils.setPivot(leftLeg, neckX + (float)lLegPivot.x, neckY + (float)lLegPivot.y, neckZ + (float)lLegPivot.z);

        rightArm.yaw += body.yaw;
        rightArm.roll += body.roll;

        leftArm.yaw += body.yaw;
        leftArm.roll += body.roll;
    }

    private Vec3d calculateHipAnchorOffset(ModelPart body, float sneakYOffset) {
        double x = 0;
        double y = 12.0 + sneakYOffset;
        double z = 0;
        return calculateReverseOffset(x, y, z, body.pitch, body.yaw, body.roll);
    }

    private Vec3d calculateReverseOffset(double x, double y, double z, float pitch, float yaw, float roll) {
        double x1 = x;
        double y1 = y * Math.cos(pitch) - z * Math.sin(pitch);
        double z1 = y * Math.sin(pitch) + z * Math.cos(pitch);
        double x2 = x1 * Math.cos(yaw) + z1 * Math.sin(yaw);
        double y2 = y1;
        double z2 = -x1 * Math.sin(yaw) + z1 * Math.cos(yaw);
        double x3 = x2 * Math.cos(roll) - y2 * Math.sin(roll);
        double y3 = x2 * Math.sin(roll) + y2 * Math.cos(roll);
        double z3 = z2;
        return new Vec3d(x - x3, y - y3, z - z3);
    }

    private Vec3d rotateVector(Vec3d vec, float pitch, float yaw, float roll) {
        double x1 = vec.x;
        double y1 = vec.y * Math.cos(pitch) - vec.z * Math.sin(pitch);
        double z1 = vec.y * Math.sin(pitch) + vec.z * Math.cos(pitch);
        double x2 = x1 * Math.cos(yaw) + z1 * Math.sin(yaw);
        double y2 = y1;
        double z2 = -x1 * Math.sin(yaw) + z1 * Math.cos(yaw);
        double x3 = x2 * Math.cos(roll) - y2 * Math.sin(roll);
        double y3 = x2 * Math.sin(roll) + y2 * Math.cos(roll);
        double z3 = z2;
        return new Vec3d(x3, y3, z3);
    }

    private Vec3d getPositionDelta(AnimationDefinition anim, String boneName, float time) {
        if (!anim.bones().containsKey(boneName)) return Vec3d.ZERO;
        BoneAnimation bone = anim.bones().get(boneName);
        if (bone.positionKeyframes() == null || bone.positionKeyframes().isEmpty()) return Vec3d.ZERO;
        return interpolate(bone.positionKeyframes(), time);
    }

    private void applyRotation(AnimationDefinition anim, String boneName, ModelPart part, float time, boolean add) {
        if (!anim.bones().containsKey(boneName)) return;
        BoneAnimation bone = anim.bones().get(boneName);
        if (bone.rotationKeyframes() != null && !bone.rotationKeyframes().isEmpty()) {
            Vec3d rot = interpolate(bone.rotationKeyframes(), time);
            float rX = (float) Math.toRadians(rot.x);
            float rY = (float) Math.toRadians(rot.y);
            float rZ = (float) Math.toRadians(rot.z);
            if (add) { part.pitch += rX; part.yaw += rY; part.roll += rZ; }
            else { part.pitch = rX; part.yaw = rY; part.roll = rZ; }
        }
    }

    private void applyPosition(AnimationDefinition anim, String boneName, ModelPart part, float time) {
        if (!anim.bones().containsKey(boneName)) return;
        BoneAnimation bone = anim.bones().get(boneName);
        if (bone.positionKeyframes() != null && !bone.positionKeyframes().isEmpty()) {
            Vec3d pos = interpolate(bone.positionKeyframes(), time);

            float currentX = ModelPartUtils.getX(part);
            float currentY = ModelPartUtils.getY(part);
            float currentZ = ModelPartUtils.getZ(part);

            // Invert Y for Minecraft coordinate space
            ModelPartUtils.setPivot(part,
                    currentX + (float) pos.x,
                    currentY - (float) pos.y,
                    currentZ + (float) pos.z
            );
        }
    }

    private AnimationContext determineCurrentAnimation(PlayerEntity player) {
        long now = System.currentTimeMillis();
        AnimationState activeState = PlayerAnimationManager.getState(player);

        // 1. High Priority: Action Animations (Attack, etc.)
        if (activeState != null) {
            AnimationDefinition def = AnimationLoader.ANIMATIONS.get(activeState.id);
            if (def != null) {
                float elapsed = (now - activeState.startTime) / 1000f;
                if (elapsed <= def.length()) return new AnimationContext(activeState.id, elapsed);
            }
        }

        ItemStack stack = player.getMainHandStack();
        WeaponAnimationSet config = null;

        // Detect custom items
        if (stack.getItem() instanceof WeaponItem weapon) {
            config = weapon.getAnimations();
        } else if (stack.getItem() instanceof ParryShieldItem) {
            // Shield detected - config stays null for now
        } else {
            // If holding neither, do nothing (vanilla)
            return null;
        }

        float loopTime = (float) (now % 100000L) / 1000f;
        Identifier animId = null;

        // FIX: If blocking, return NULL to let vanilla logic handle the shield blocking pose
        if (player.isBlocking()) {
            return null;
        }
        else if (player.isInSneakingPose()) {
            return null;
        }
        else if (player.isSprinting()) {
            animId = (config != null && config.sprintAnim() != null) ? config.sprintAnim() : DEFAULT_SPRINT;
        }
        else {
            boolean isMoving = player.getVelocity().horizontalLengthSquared() > 0.0001;
            if (isMoving) {
                animId = (config != null && config.walkAnim() != null) ? config.walkAnim() : DEFAULT_WALK;
            } else {
                animId = (config != null && config.idleAnim() != null) ? config.idleAnim() : DEFAULT_IDLE;
            }
        }

        if (animId != null && AnimationLoader.ANIMATIONS.containsKey(animId)) {
            return new AnimationContext(animId, loopTime);
        }

        return null;
    }

    private Vec3d interpolate(Map<Float, Vec3d> keyframes, float time) {
        if (keyframes == null || keyframes.isEmpty()) return Vec3d.ZERO;
        float prevTime = -1.0f; float nextTime = -1.0f;
        for (float k : keyframes.keySet()) {
            if (k <= time) { if (prevTime == -1.0f || k > prevTime) prevTime = k; }
            if (k >= time) { if (nextTime == -1.0f || k < nextTime) nextTime = k; }
        }
        if (prevTime == -1.0f && nextTime != -1.0f) return keyframes.get(nextTime);
        if (nextTime == -1.0f && prevTime != -1.0f) return keyframes.get(prevTime);
        if (prevTime == -1.0f && nextTime == -1.0f) return Vec3d.ZERO;
        if (prevTime == nextTime) return keyframes.get(prevTime);
        float delta = (time - prevTime) / (nextTime - prevTime);
        return keyframes.get(prevTime).lerp(keyframes.get(nextTime), delta);
    }
}