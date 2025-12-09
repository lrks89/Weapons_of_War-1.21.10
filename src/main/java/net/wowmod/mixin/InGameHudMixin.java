package net.wowmod.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.wowmod.item.custom.DualWieldedWeaponItem;
import net.wowmod.item.custom.TwoHandedWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    // Target the "renderHotbar" method where it checks "player.getOffHandStack()"
    // We redirect this call. If a Two-Handed weapon is held, we tell the HUD the offhand is empty.
    @Redirect(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getOffHandStack()Lnet/minecraft/item/ItemStack;"))
    private ItemStack wowmod$hideOffhandInHotbar(PlayerEntity player) {
        ItemStack realOffhand = player.getOffHandStack();
        ItemStack mainHand = player.getMainHandStack();

        // Check Main Hand for Two-Handed Weapon OR Dual-Wielded Weapon
        // Since DualWieldedWeaponItem DOES NOT extend TwoHandedWeaponItem (per your request), we check both.
        if (mainHand.getItem() instanceof TwoHandedWeaponItem || mainHand.getItem() instanceof DualWieldedWeaponItem) {
            // Return empty stack to the renderer so it skips drawing the offhand slot
            return ItemStack.EMPTY;
        }

        return realOffhand;
    }
}