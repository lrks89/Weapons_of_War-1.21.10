package net.wowmod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.wowmod.util.IParryPlayer;
import net.wowmod.util.IAnimatedPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements IParryPlayer, IAnimatedPlayer {

    // --- IParryPlayer Fields ---
    @Unique
    private long wowmod$lastParryTime;

    // --- IAnimatedPlayer Fields ---
    @Unique private long wowmod$lastLandTime;
    @Unique private boolean wowmod$wasOnGround;
    // Removed: @Unique private boolean wowmod$wasFluidBelow;
    @Unique private long wowmod$lastFluidContactTime = -999;

    // IParryPlayer Implementation
    @Override public long wowmod_getLastParryTime() { return this.wowmod$lastParryTime; }
    @Override public void wowmod_setLastParryTime(long time) { this.wowmod$lastParryTime = time; }

    // IAnimatedPlayer Implementation
    @Override public long wowmod$getLastLandTime() { return this.wowmod$lastLandTime; }
    @Override public void wowmod$setLastLandTime(long time) { this.wowmod$lastLandTime = time; }
    @Override public boolean wowmod$wasOnGround() { return this.wowmod$wasOnGround; }
    @Override public void wowmod$setWasOnGround(boolean onGround) { this.wowmod$wasOnGround = onGround; }
    // Removed: @Override public boolean wowmod$wasFluidBelow() { return this.wowmod$wasFluidBelow; }
    // Removed: @Override public void wowmod$setWasFluidBelow(boolean fluidBelow) { this.wowmod$wasFluidBelow = fluidBelow; }

    // Fluid Contact Time Implementation
    @Override public long wowmod$getLastFluidContactTime() { return this.wowmod$lastFluidContactTime; }
    @Override public void wowmod$setLastFluidContactTime(long time) { this.wowmod$lastFluidContactTime = time; }


    @Inject(method = "tick", at = @At("HEAD"))
    private void trackLandingAndGroundState(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        boolean onGround = self.isOnGround();

        // 1. Landing Detection
        if (onGround && !this.wowmod$wasOnGround) {
            this.wowmod$lastLandTime = self.getEntityWorld().getTime();
        }

        // 2. Fluid Contact Cooldown (Primary fluid check)
        // Check if the player is currently touching, submerged, or splashing in any fluid
        if (self.isTouchingWater() || self.isInLava() || self.isSubmergedInWater() || self.isSprinting()) {
            this.wowmod$lastFluidContactTime = self.getEntityWorld().getTime();
        }
        // Note: Checking for fluid directly below (wowmod$wasFluidBelow) is no longer necessary
        // as the cooldown based on surface contact (isTouchingWater/isInLava) handles the jump suppression.

        // 3. Update previous state
        this.wowmod$wasOnGround = onGround;
    }
}