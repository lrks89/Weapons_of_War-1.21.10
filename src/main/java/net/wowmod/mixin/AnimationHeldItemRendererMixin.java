package net.wowmod.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import net.wowmod.item.custom.DualWieldedWeaponItem;
import net.wowmod.item.custom.TwoHandedWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HeldItemRenderer.class)
public class AnimationHeldItemRendererMixin {

    /**
     * Redirects the call to 'getOffHandStack' inside 'updateHeldItems'.
     *
     * Why this works:
     * 1. 'updateHeldItems' is called every tick to sync the visual item with the inventory.
     * 2. It updates the internal field 'this.offHand'.
     * 3. 'renderItem' reads 'this.offHand' to decide what to draw.
     *
     * By injecting here, we update the cached field with our fake item, so the
     * renderer sees it automatically without needing complex render loop injections.
     */
    @Redirect(
            method = "updateHeldItems",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getOffHandStack()Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack wowmod$redirectOffHandUpdate(ClientPlayerEntity player) {
        ItemStack mainStack = player.getMainHandStack();
        ItemStack offHandStack = player.getOffHandStack(); // The real (likely empty) stack

        // 1. Dual Wield Logic:
        // If holding a Dual Wield weapon, pretend the offhand holds a copy of the main hand.
        if (mainStack.getItem() instanceof DualWieldedWeaponItem) {
            return mainStack;
        }

        // 2. Two-Handed Logic:
        // If holding a 2H weapon, force the offhand to be Empty (hiding shields/torches).
        if (mainStack.getItem() instanceof TwoHandedWeaponItem) {
            return ItemStack.EMPTY;
        }

        // 3. Default Behavior:
        // Return the actual item.
        return offHandStack;
    }
}