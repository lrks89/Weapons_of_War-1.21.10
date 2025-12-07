package net.wowmod.mixin;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.ItemStack;
import net.wowmod.util.RenderStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public class AnimationPlayerEntityRenderStateMixin implements RenderStateExtension {

    // Fields tracking vanilla data injected by PlayerEntityRendererMixin:
    @Unique private boolean wowmod$sprinting;
    @Unique private double wowmod$verticalVelocity;
    @Unique private boolean wowmod$isFlying;
    @Unique private boolean wowmod$isSwimming;
    @Unique private boolean wowmod$isRiding;
    @Unique private boolean wowmod$isClimbing;
    @Unique private boolean wowmod$isInWater;

    // NEW: Fluid Contact Time
    @Unique private long wowmod$lastFluidContactTime;

    // Fields tracking complex data:
    @Unique private long wowmod$timeSinceLanding;
    @Unique private boolean wowmod$onGround;

    // --- FIX: Item Stack Storage ---
    @Unique private ItemStack wowmod$mainHandStack = ItemStack.EMPTY;
    // --- NEW: Off Hand Storage ---
    @Unique private ItemStack wowmod$offHandStack = ItemStack.EMPTY;

    // --- NEW: Swing Duration ---
    @Unique private int wowmod$handSwingDuration;

    // --- NEW: Blocking State ---
    @Unique private boolean wowmod$isBlocking;

    // Implementation of RenderStateExtension interface

    @Override public boolean wowmod$isSprinting() { return this.wowmod$sprinting; }
    @Override public void wowmod$setSprinting(boolean sprinting) { this.wowmod$sprinting = sprinting; }

    @Override public double wowmod$getVerticalVelocity() { return this.wowmod$verticalVelocity; }
    @Override public void wowmod$setVerticalVelocity(double vy) { this.wowmod$verticalVelocity = vy; }

    @Override public long wowmod$getTimeSinceLanding() { return this.wowmod$timeSinceLanding; }
    @Override public void wowmod$setTimeSinceLanding(long ticks) { this.wowmod$timeSinceLanding = ticks; }

    @Override public boolean wowmod$isOnGround() { return this.wowmod$onGround; }
    @Override public void wowmod$setOnGround(boolean onGround) { this.wowmod$onGround = onGround; }

    @Override public boolean wowmod$isFlying() { return this.wowmod$isFlying; }
    @Override public void wowmod$setFlying(boolean flying) { this.wowmod$isFlying = flying; }

    @Override public boolean wowmod$isSwimming() { return this.wowmod$isSwimming; }
    @Override public void wowmod$setSwimming(boolean swimming) { this.wowmod$isSwimming = swimming; }

    @Override public boolean wowmod$isRiding() { return this.wowmod$isRiding; }
    @Override public void wowmod$setRiding(boolean riding) { this.wowmod$isRiding = riding; }

    @Override public boolean wowmod$isClimbing() { return this.wowmod$isClimbing; }
    @Override public void wowmod$setClimbing(boolean climbing) { this.wowmod$isClimbing = climbing; }

    @Override public boolean wowmod$isInWater() { return this.wowmod$isInWater; }
    @Override public void wowmod$setInWater(boolean inWater) { this.wowmod$isInWater = inWater; }

    // --- FIX: Item Stack Accessors ---
    @Override public ItemStack wowmod$getMainHandStack() { return this.wowmod$mainHandStack; }
    @Override public void wowmod$setMainHandStack(ItemStack stack) { this.wowmod$mainHandStack = stack; }

    // --- NEW: Off Hand Accessors ---
    @Override public ItemStack wowmod$getOffHandStack() { return this.wowmod$offHandStack; }
    @Override public void wowmod$setOffHandStack(ItemStack stack) { this.wowmod$offHandStack = stack; }

    // --- NEW: Swing Duration Accessors ---
    @Override public int wowmod$getHandSwingDuration() { return this.wowmod$handSwingDuration; }
    @Override public void wowmod$setHandSwingDuration(int ticks) { this.wowmod$handSwingDuration = ticks; }

    // --- NEW: Blocking State Accessors ---
    @Override public boolean wowmod$isBlocking() { return this.wowmod$isBlocking; }
    @Override public void wowmod$setBlocking(boolean blocking) { this.wowmod$isBlocking = blocking; }
}