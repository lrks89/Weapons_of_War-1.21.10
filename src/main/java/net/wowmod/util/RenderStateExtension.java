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

    // Water detection
    boolean wowmod$isInWater();
    void wowmod$setInWater(boolean inWater);

    // Fluid Contact Cooldown (NEW)
    long wowmod$getLastFluidContactTime();
    void wowmod$setLastFluidContactTime(long time);

    Object wowmod$getEntity();
    void wowmod$setEntity(Object entity);
}