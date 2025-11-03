package net.wowmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.consume.UseAction;
import net.wowmod.item.custom.WeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//Injects Blocking Mechanic into game logic
@Mixin(LivingEntity.class)
public class LivingEntityIsBlockingMixin {

    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void wowmod_checkCustomBlock(CallbackInfoReturnable<Boolean> cir) {

        if (!((Object)this instanceof PlayerEntity player)) {
            return;
        }

        if (player.isUsingItem()) {
            if (player.getActiveItem().getItem() instanceof WeaponItem) {
                if (player.getActiveItem().getUseAction() == UseAction.BLOCK) {
                    cir.setReturnValue(true);
                    cir.cancel();
                }
            }
        }
    }
}
