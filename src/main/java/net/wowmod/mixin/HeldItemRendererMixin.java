package net.wowmod.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.wowmod.util.IWeaponTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// This import is crucial if you are using the new rendering system
// If it fails to resolve, remove it and change the argument type to Object
import net.minecraft.client.render.command.OrderedRenderCommandQueue; // Ensure this import exists

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void wowmod_applyWeaponTransform(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            // If OrderedRenderCommandQueue fails to import, change this type to Object
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int light,
            CallbackInfo ci
    ) {
        // Apply the transform to the MatrixStack for First Person
        if (player instanceof IWeaponTransform weaponData) {
            Vec3d rot = weaponData.wowmod_getWeaponRotation();
            Vec3d pos = weaponData.wowmod_getWeaponPosition();

            if (rot != Vec3d.ZERO || pos != Vec3d.ZERO) {
                // Apply Position (Scale down by 16 because blockbench uses pixels)
                matrices.translate(pos.x / 16.0, -pos.y / 16.0, pos.z / 16.0);

                // Apply Rotation
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rot.x));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rot.y));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rot.z));
            }
        }
    }
}