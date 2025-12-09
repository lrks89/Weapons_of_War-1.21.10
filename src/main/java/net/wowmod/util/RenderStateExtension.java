package net.wowmod.util;

import net.minecraft.item.ItemStack;

/**
 * Duck Interface injected into LivingEntityRenderState via Mixin.
 * This holds the extra data needed for your animation system.
 */
public interface RenderStateExtension {
    void wowmod$setSprinting(boolean sprinting);
    boolean wowmod$isSprinting();

    void wowmod$setVerticalVelocity(double velocity);
    double wowmod$getVerticalVelocity();

    void wowmod$setOnGround(boolean onGround);
    boolean wowmod$isOnGround();

    void wowmod$setFlying(boolean flying);
    boolean wowmod$isFlying();

    void wowmod$setSwimming(boolean swimming);
    boolean wowmod$isSwimming();

    void wowmod$setRiding(boolean riding);
    boolean wowmod$isRiding();

    void wowmod$setClimbing(boolean climbing);
    boolean wowmod$isClimbing();

    void wowmod$setInWater(boolean inWater);
    boolean wowmod$isInWater();

    void wowmod$setMainHandStack(ItemStack stack);
    ItemStack wowmod$getMainHandStack();

    void wowmod$setOffHandStack(ItemStack stack);
    ItemStack wowmod$getOffHandStack();

    void wowmod$setHandSwingDuration(int duration);
    int wowmod$getHandSwingDuration();

    void wowmod$setBlocking(boolean blocking);
    boolean wowmod$isBlocking();

    void wowmod$setTimeSinceLanding(long time);
    long wowmod$getTimeSinceLanding();
}