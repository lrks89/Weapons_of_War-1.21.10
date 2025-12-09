package net.wowmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.wowmod.item.custom.TwoHandedWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Changed Mixin target to ItemCooldownManager to avoid ItemRenderer signature issues
@Mixin(ItemCooldownManager.class)
public class ItemRendererCooldownMixin {

    /**
     * Intercepts the progress calculation directly.
     * This affects the visual "grey overlay" on the item icon.
     */
    @Inject(method = "getCooldownProgress", at = @At("HEAD"), cancellable = true)
    private void wowmod$hideCooldownOverlay(ItemStack stack, float partialTicks, CallbackInfoReturnable<Float> cir) {
        // Access the client player safely
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        // Ensure we are modifying the client player's manager
        if (player != null && player.getItemCooldownManager() == (Object) this) {

            ItemStack mainHand = player.getMainHandStack();
            ItemStack offHand = player.getOffHandStack();

            // 1. Check if holding a Two-Handed Weapon in Main Hand
            if (mainHand.getItem() instanceof TwoHandedWeaponItem) {

                // 2. Check if the item being queried is the Offhand Stack
                if (stack == offHand) {
                    // Return 0.0F (0% progress) so the renderer draws no overlay
                    cir.setReturnValue(0.0F);
                }
            }
        }
    }
}