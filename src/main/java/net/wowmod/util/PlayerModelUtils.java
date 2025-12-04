package net.wowmod.util;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.joml.Quaternionf;

public class PlayerModelUtils {

    // ==========================================
    //       FINE TUNING CONFIGURATION
    // ==========================================

    /**
     * Vertical adjustment.
     * Negative moves the item UP (closer to shoulder).
     * Positive moves the item DOWN.
     */
    public static final float VISUAL_HEIGHT_OFFSET = -3.5f;

    /**
     * Sideways adjustment relative to the body.
     * Negative moves the item OUT (away from body).
     * Positive moves the item IN (closer to body).
     */
    public static final float VISUAL_BODY_OFFSET = 1.5f;

    /**
     * Anti-Orbit Correction.
     * This neutralizes the Vanilla Item Renderer's default offset.
     * If the item "swings" or "orbits" around the handle when rotating, tweak this value.
     * Standard values are usually between 10.0f and 12.0f.
     */
    public static final float VANILLA_CANCEL_OFFSET = 8.5f;

    // ==========================================

    /**
     * Applies the custom item bone transformation to the MatrixStack.
     * This handles the pivot point, rotation, visual offsets, and vanilla cancellation.
     */
    public static void applyItemTransform(Arm arm, MatrixStack matrices, ModelPart itemBone) {
        if (itemBone == null) return;

        // Calculate X offset based on arm side
        // Right Hand: Negative visual offset moves outward (left in local space)
        // Left Hand: Positive visual offset moves outward (right in local space)
        float xOffset = (arm == Arm.RIGHT) ? VISUAL_BODY_OFFSET : -VISUAL_BODY_OFFSET;

        // STEP A: Translate to Pivot + Apply Visual Offsets
        matrices.translate(
                (itemBone.originX + xOffset) / 16.0F,
                (itemBone.originY + VISUAL_HEIGHT_OFFSET) / 16.0F,
                itemBone.originZ / 16.0F
        );

        // STEP B: Apply Rotation
        if (itemBone.roll != 0.0F || itemBone.yaw != 0.0F || itemBone.pitch != 0.0F) {
            matrices.multiply(new Quaternionf().rotationZYX(itemBone.roll, itemBone.yaw, itemBone.pitch));
        }

        // STEP C: Return & Cancel Vanilla Offsets
        // We return from the pivot (origin), but we add VANILLA_CANCEL_OFFSET to neutralize Vanilla's translation.
        matrices.translate(
                -itemBone.originX / 16.0F,
                (-itemBone.originY - VANILLA_CANCEL_OFFSET) / 16.0F,
                -itemBone.originZ / 16.0F
        );

        if (itemBone.xScale != 1.0F || itemBone.yScale != 1.0F || itemBone.zScale != 1.0F) {
            matrices.scale(itemBone.xScale, itemBone.yScale, itemBone.zScale);
        }
    }
}