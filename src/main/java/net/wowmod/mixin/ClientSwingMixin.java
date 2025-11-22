package net.wowmod.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.wowmod.animation.PlayerAnimationManager;
import net.wowmod.animation.WeaponAnimationSet;
import net.wowmod.item.custom.WeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientSwingMixin {

    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;)V", at = @At("HEAD"))
    private void onSwingHand(Hand hand, CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        // Check if holding our weapon
        if (player.getMainHandStack().getItem() instanceof WeaponItem weapon) {
            WeaponAnimationSet anims = weapon.getAnimations();
            if (anims != null && anims.attackAnim() != null) {
                // Trigger the animation!
                PlayerAnimationManager.playAnimation(player, anims.attackAnim());
            }
        }
    }
}