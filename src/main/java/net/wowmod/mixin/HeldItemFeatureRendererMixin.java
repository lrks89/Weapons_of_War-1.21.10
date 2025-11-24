package net.wowmod.mixin;

import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.wowmod.util.IWeaponTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Correct import for 1.21.10
import net.minecraft.client.render.command.OrderedRenderCommandQueue;

@Mixin(HeldItemFeatureRenderer.class)
public class HeldItemFeatureRendererMixin {

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void wowmod_applyWeaponBoneTransform(
            ArmedEntityRenderState entityState,
            ItemRenderState itemRenderState,
            Arm arm,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int light,
            CallbackInfo ci
    ) {
        if (entityState instanceof IWeaponTransform weaponData) {
            Vec3d rot = weaponData.wowmod_getWeaponRotation();
            Vec3d pos = weaponData.wowmod_getWeaponPosition();

            if (rot != Vec3d.ZERO || pos != Vec3d.ZERO) {
                // Apply Position (Scale down by 16 because blockbench uses pixels)
                // Note: If weapon detaches, ensure JSON values are relative to Hand, not Body.
                matrices.translate(pos.x / 16.0, -pos.y / 16.0, pos.z / 16.0);

                // Apply Rotation
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rot.x));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rot.y));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rot.z));
            }
        }
    }
}