package net.wowmod.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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

        // Check Main Hand for Two-Handed Weapon
        if (player.getMainHandStack().getItem() instanceof TwoHandedWeaponItem) {
            // Return empty stack to the renderer so it skips drawing the offhand slot
            return ItemStack.EMPTY;
        }

        return realOffhand;
    }
}