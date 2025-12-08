package net.wowmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.LingeringPotionEntity;
import net.minecraft.entity.projectile.thrown.SplashPotionEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// --- MIXIN 1: SHARED LOGIC (Targets the base class for general hooks) ---
@Mixin(Item.class)
public abstract class ThrowablePotionBaseMixin {

    // --- Configuration Constants ---
    private static final int MAX_CHARGE_DURATION = 20; // 1 second max charge
    private static final int MIN_CHARGE_TICKS = 5; // 0.25 seconds minimum charge
    private static final float MIN_VELOCITY = 0.5F;
    private static final float MAX_VELOCITY = 2.0F;

    // Vanilla throw constants (used for quick tap)
    private static final float VANILLA_VELOCITY = 0.5F;
    private static final float VANILLA_PITCH_OFFSET = -20.0F; // Used to launch slightly downwards/forwards

    // Helper to check if the current Item instance is a throwable potion we care about
    protected boolean isTargetItem(Item instance) {
        return instance instanceof SplashPotionItem || instance instanceof LingeringPotionItem;
    }

    // 1. Force Max Use Time to enable charging mechanic
    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    protected void wowmod$setChargeTime(ItemStack stack, LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        if (!isTargetItem(stack.getItem())) return;
        cir.setReturnValue(72000);
    }

    // 2. Set the Use Action to SPEAR for visual feedback
    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
    protected void wowmod$setUseAction(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
        if (!isTargetItem(stack.getItem())) return;
        cir.setReturnValue(UseAction.SPEAR);
    }

    // 4. Handle the Throw when the player releases right-click (The logic for the charged throw)
    @Inject(method = "onStoppedUsing", at = @At("HEAD"), cancellable = true)
    protected void wowmod$throwChargedPotion(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (!isTargetItem(stack.getItem())) return;
        if (!(user instanceof PlayerEntity player)) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        int totalUseDuration = stack.getMaxUseTime(user);
        int timeCharged = totalUseDuration - remainingUseTicks;

        final boolean isQuickTap = timeCharged < MIN_CHARGE_TICKS;

        if (world.isClient()) {
            // Client side only needs to handle the packet send, so we just return false
            if (isQuickTap) {
                // If it's a quick tap, let the vanilla client logic proceed if applicable, or just exit.
                // We must still cancel the mixin to prevent it from failing later.
                cir.setReturnValue(false);
                cir.cancel();
                return;
            }
        }

        // --- Server-side Throw Logic ---

        float finalVelocity;
        float pitchOffset;
        float soundPitch;

        // LOGIC: If charge is too short, execute the standard vanilla throw
        if (isQuickTap) {
            finalVelocity = VANILLA_VELOCITY;
            pitchOffset = VANILLA_PITCH_OFFSET;
            soundPitch = 1.0F; // Standard pitch
        } else {
            // Charged throw
            float chargeProgress = Math.min((float)timeCharged / MAX_CHARGE_DURATION, 1.0f);
            finalVelocity = MIN_VELOCITY + (chargeProgress * (MAX_VELOCITY - MIN_VELOCITY));
            pitchOffset = 0.0F; // No pitch offset for charged throw, throws straight ahead
            soundPitch = 0.8F + (chargeProgress * 0.7F);
        }

        // Prepare projectile item (copying stack details)
        ItemStack projectileItem = stack.copyWithCount(1);

        // Determine which projectile entity to launch
        ThrownItemEntity projectileEntity = null;
        if (stack.getItem() instanceof SplashPotionItem) {
            projectileEntity = new SplashPotionEntity(world, user, projectileItem);
        } else if (stack.getItem() instanceof LingeringPotionItem) {
            projectileEntity = new LingeringPotionEntity(world, user, projectileItem);
        }

        if (projectileEntity != null) {
            final float DIVERGENCE = 1.0F;

            // FIX: Decrement the stack only after confirming the entity was successfully created.
            stack.decrement(1);

            // Set velocity and launch
            projectileEntity.setVelocity(player, player.getPitch(), player.getYaw(), pitchOffset, finalVelocity, DIVERGENCE);

            world.spawnEntity(projectileEntity);

            // Play sound (using vanilla trident throw sound for consistency)
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sound.SoundEvents.ITEM_TRIDENT_THROW, net.minecraft.sound.SoundCategory.PLAYERS, 1.0F, soundPitch);
        } else {
            // If projectile creation failed, ensure we return false (or handle re-equipping the item,
            // though in most Mixin scenarios, if creation fails, we just prevent consumption).
            // Since stack.decrement(1) is now guarded, we just allow the method to exit.
        }

        // Cancel the original method and return true (success/consumed)
        cir.setReturnValue(true);
        cir.cancel();
    }
}