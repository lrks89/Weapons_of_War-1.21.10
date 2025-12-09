package net.wowmod.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.wowmod.item.custom.TwoHandedWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class AnimationHeldItemRendererMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void wowmod$hideOffhandForTwoHanded(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            OrderedRenderCommandQueue renderBuffer, // Using the type from your snippet
            int light,
            CallbackInfo ci
    ) {
        // If we are trying to render the Offhand...
        if (hand == Hand.OFF_HAND) {
            // ...check if the Main Hand has a Two-Handed Weapon
            ItemStack mainHandStack = player.getMainHandStack();
            if (mainHandStack.getItem() instanceof TwoHandedWeaponItem) {
                // Cancel rendering completely
                ci.cancel();
            }
        }
    }
}