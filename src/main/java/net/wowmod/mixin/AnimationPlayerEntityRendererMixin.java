package net.wowmod.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.wowmod.item.custom.DualWieldedWeaponItem;
import net.wowmod.item.custom.TwoHandedWeaponItem;
import net.wowmod.util.RenderStateExtension;
import net.wowmod.util.IAnimatedPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * We target EntityRenderer because it orchestrates the state population.
 */
@Mixin(EntityRenderer.class)
public class AnimationPlayerEntityRendererMixin {

    @Unique private ItemStack wowmod$storedOffHandStack = ItemStack.EMPTY;
    @Unique private boolean wowmod$isSwapped = false;

    /**
     * INJECT AT HEAD of 'getAndUpdateRenderState':
     * This method is responsible for reading the Entity and populating the RenderState.
     * We swap the inventory here so that ALL downstream logic (vanilla or custom)
     * sees the fake Dual Wield item.
     */
    @Inject(
            method = "getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;",
            at = @At("HEAD")
    )
    private void wowmod$preGetState(Entity entity, float tickDelta, CallbackInfoReturnable<EntityRenderState> cir) {
        this.wowmod$isSwapped = false;

        if (entity instanceof AbstractClientPlayerEntity player) {
            ItemStack mainStack = player.getMainHandStack();

            // 1. Dual Wield Logic: Clone Main Hand to Offhand visually
            if (mainStack.getItem() instanceof DualWieldedWeaponItem) {
                this.wowmod$storedOffHandStack = player.getOffHandStack();
                player.equipStack(EquipmentSlot.OFFHAND, mainStack.copy());
                this.wowmod$isSwapped = true;
            }
            // 2. Two-Handed Logic: Hide Offhand
            else if (mainStack.getItem() instanceof TwoHandedWeaponItem) {
                this.wowmod$storedOffHandStack = player.getOffHandStack();
                player.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                this.wowmod$isSwapped = true;
            }
        }
    }

    /**
     * INJECT AT TAIL of 'getAndUpdateRenderState':
     * Immediately after state population is done, we restore the real inventory.
     */
    @Inject(
            method = "getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;",
            at = @At("TAIL")
    )
    private void wowmod$postGetState(Entity entity, float tickDelta, CallbackInfoReturnable<EntityRenderState> cir) {
        if (this.wowmod$isSwapped && entity instanceof AbstractClientPlayerEntity player) {
            player.equipStack(EquipmentSlot.OFFHAND, this.wowmod$storedOffHandStack);
            this.wowmod$isSwapped = false;
            this.wowmod$storedOffHandStack = ItemStack.EMPTY;
        }
    }

    /**
     * Custom Render State Population.
     * Since we swapped the inventory in 'preGetState', the 'player.getOffHandStack()'
     * call here returns the Visual item (Main copy or Empty), which is correct.
     */
    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void wowmod$updateRenderState(Entity entity, EntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayerEntity player && state instanceof RenderStateExtension extendedState) {

            extendedState.wowmod$setMainHandStack(player.getMainHandStack());
            extendedState.wowmod$setOffHandStack(player.getOffHandStack());

            extendedState.wowmod$setSprinting(player.isSprinting());
            extendedState.wowmod$setVerticalVelocity(player.getVelocity().y);
            extendedState.wowmod$setOnGround(player.isOnGround());

            boolean isElytra = player.isGliding();
            extendedState.wowmod$setFlying(player.getAbilities().flying || isElytra);

            extendedState.wowmod$setSwimming(player.isInSwimmingPose());
            extendedState.wowmod$setRiding(player.hasVehicle());
            extendedState.wowmod$setClimbing(player.isClimbing());

            boolean inCollisionFluid = player.isInLava() || player.isSubmergedInWater() || player.isTouchingWater();
            extendedState.wowmod$setInWater(inCollisionFluid);

            extendedState.wowmod$setBlocking(player.isBlocking());

            if (player instanceof IAnimatedPlayer animatedPlayer) {
                long diff = player.getEntityWorld().getTime() - animatedPlayer.wowmod$getLastLandTime();
                extendedState.wowmod$setTimeSinceLanding(diff);
            }
        }
    }
}