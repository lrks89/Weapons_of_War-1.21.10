package net.wowmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.consume.UseAction;
import net.wowmod.item.custom.ParryShieldItem;
import net.wowmod.item.custom.ParryWeaponItem;
import net.wowmod.logic.ParryLogic;
import net.wowmod.util.IParryStunnedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 1001)
public abstract class BlockingLivingEntityMixin implements IParryStunnedEntity {

    @Unique private int wowmod_parriedStunTicks = 0;

    @Override public int wowmod_getStunTicks() { return this.wowmod_parriedStunTicks; }
    @Override public void wowmod_setStunTicks(int ticks) { this.wowmod_parriedStunTicks = ticks; }

    @Inject(method = "tick", at = @At("HEAD"))
    private void wowmod_applyStunAndDisableTick(CallbackInfo ci) {
        if (this.wowmod_parriedStunTicks > 0) {
            this.wowmod_parriedStunTicks--;
            // Delegate effect logic to ParryLogic
            ParryLogic.tickStunnedEntity((LivingEntity)(Object)this, this.wowmod_parriedStunTicks);
        }

        // Keep simple boolean check here or move if it gets complex
        if ((Object) this instanceof PlayerEntity player) {
            if (player.isBlocking() && player.isSprinting()) {
                player.setSprinting(false);
            }
        }
    }

    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void wowmod_checkCustomBlock(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object)this instanceof PlayerEntity player)) return;

        if (player.isUsingItem()) {
            if (player.getActiveItem().getItem() instanceof ParryWeaponItem
                    || player.getActiveItem().getItem() instanceof ParryShieldItem) {
                if (player.getActiveItem().getUseAction() == UseAction.BLOCK) {
                    cir.setReturnValue(true);
                    cir.cancel();
                }
            }
        }
    }

    @Inject(method = "takeKnockback(DDD)V", at = @At("HEAD"), cancellable = true)
    private void wowmod_cancelDefensiveKnockback(double strength, double x, double z, CallbackInfo ci) {
        if ((Object)this instanceof PlayerEntity player) {
            if (player.isBlocking()) {
                ci.cancel();
            }
        }
    }
}