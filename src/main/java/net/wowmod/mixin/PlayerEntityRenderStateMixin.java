package net.wowmod.mixin;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.wowmod.util.RenderStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public class PlayerEntityRenderStateMixin implements RenderStateExtension {

    // Fields tracking vanilla data injected by PlayerEntityRendererMixin:
    @Unique private boolean wowmod$sprinting;
    @Unique private double wowmod$verticalVelocity;
    @Unique private boolean wowmod$isFlying;
    @Unique private boolean wowmod$isSwimming;
    @Unique private boolean wowmod$isRiding;
    @Unique private boolean wowmod$isClimbing;
    @Unique private boolean wowmod$isInWater;
    // Removed: @Unique private boolean wowmod$wasFluidBelow; // New field for jump origin tracking

    // Fields tracking complex data:
    @Unique private long wowmod$timeSinceLanding;
    @Unique private boolean wowmod$onGround;

    // FIX ADDED: Field for storing the associated entity
    @Unique private Object wowmod$entity;


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

    // Removed: @Override public boolean wowmod$wasFluidBelow() { return this.wowmod$wasFluidBelow; }
    // Removed: @Override public void wowmod$setWasFluidBelow(boolean fluidBelow) { this.wowmod$wasFluidBelow = fluidBelow; }

    // FIX ADDED: Entity Getter and Setter Implementation
    @Override public Object wowmod$getEntity() { return this.wowmod$entity; }
    @Override public void wowmod$setEntity(Object entity) { this.wowmod$entity = entity; }
}