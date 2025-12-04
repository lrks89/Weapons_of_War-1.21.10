package net.wowmod.util;

import net.minecraft.item.ItemStack;

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

    // --- FIX: Add Main Hand Item Stack Storage ---
    ItemStack wowmod$getMainHandStack();
    void wowmod$setMainHandStack(ItemStack stack);

    // --- NEW: Attack Duration for Split Animation ---
    int wowmod$getHandSwingDuration();
    void wowmod$setHandSwingDuration(int ticks);
}