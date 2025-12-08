package net.wowmod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// --- MIXIN: LINGERING POTION FIX (Targets the subclass to cancel instant throw) ---
@Mixin(LingeringPotionItem.class)
public abstract class LingeringPotionItemChargeMixin { // Removed 'extends Item'

    // Inject and cancel the subclass's 'use' method to initiate charging.
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void wowmod$startLingeringPotionCharge(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        // Start the item "use" (charging) animation and timer
        ItemStack stack = user.getStackInHand(hand);
        user.setCurrentHand(hand);

        // Set return value to CONSUME and cancel the original method
        cir.setReturnValue(ActionResult.CONSUME);
        cir.cancel();
    }
}