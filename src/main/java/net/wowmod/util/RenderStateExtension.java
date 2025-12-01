package net.wowmod.util;

// NOTE: In a real Minecraft mod, 'Object' should be the common base entity class,
// e.g., 'net.minecraft.entity.Entity' or 'net.minecraft.entity.LivingEntity'.

public interface RenderStateExtension {
    boolean wowmod$isSprinting();
    void wowmod$setSprinting(boolean sprinting);

    // Jumping fields
    double wowmod$getVerticalVelocity();
    void wowmod$setVerticalVelocity(double vy);

    long wowmod$getTimeSinceLanding();
    void wowmod$setTimeSinceLanding(long ticks);

    boolean wowmod$isOnGround();
    void wowmod$setOnGround(boolean onGround);

    // Flight check
    boolean wowmod$isFlying();
    void wowmod$setFlying(boolean flying);

    // New State Checks
    boolean wowmod$isSwimming();
    void wowmod$setSwimming(boolean swimming);

    boolean wowmod$isRiding();
    void wowmod$setRiding(boolean riding);

    // Climbing check
    boolean wowmod$isClimbing();
    void wowmod$setClimbing(boolean climbing);

    // Water detection (New)
    boolean wowmod$isInWater();
    void wowmod$setInWater(boolean inWater);

    // Jump Origin Check (was fluid below at start of jump)
    boolean wowmod$wasFluidBelow();
    void wowmod$setWasFluidBelow(boolean fluidBelow);

    Object wowmod$getEntity();
    void wowmod$setEntity(Object entity);
}