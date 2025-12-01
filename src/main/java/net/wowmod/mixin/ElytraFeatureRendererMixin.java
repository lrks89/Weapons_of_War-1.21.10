package net.wowmod.mixin;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.joml.Quaternionf;

@Mixin(ElytraFeatureRenderer.class)
public abstract class ElytraFeatureRendererMixin {

    // Helper to get the context model from the FeatureRenderer parent class.
    private EntityModel<?> getContextModelCasted() {
        return ((FeatureRenderer<?, ?>) (Object) this).getContextModel();
    }

    // This injection runs right before the vanilla Elytra rendering logic starts.
    @Inject(method = "render", at = @At("HEAD"))
    public void moveElytra(MatrixStack matrices,
                           OrderedRenderCommandQueue commandQueue,
                           int light,
                           BipedEntityRenderState state,
                           float f,
                           float g,
                           CallbackInfo ci) {

        if (this.getContextModelCasted() instanceof BipedEntityModel<?> biped) {
            // Read the modified position and rotation from the body part
            float tx = biped.body.originX;
            float ty = biped.body.originY;
            float tz = biped.body.originZ;

            float pitch = biped.body.pitch;
            float yaw = biped.body.yaw;
            float roll = biped.body.roll;

            // --- 1. Apply Translation (Position Fix) ---
            // Move the Elytra to the corrected body position (waist detachment fix)
            if (tx != 0 || ty != 0 || tz != 0) {
                // originX/Y/Z are in model pixels (1/16th blocks)
                matrices.translate(tx / 16.0f, ty / 16.0f, tz / 16.0f);
            }

            // --- 2. Apply Rotation ---
            // We apply the rotation from the body bone (pitch, yaw, roll)
            // to ensure the Elytra bends with the body when leaning forward (jumping) or twisting.
            if (pitch != 0.0F || yaw != 0.0F || roll != 0.0F) {
                // Apply rotation using the Quaternionf utility
                matrices.multiply((new Quaternionf()).rotationZYX(roll, yaw, pitch));
            }
        }
    }
}