package net.wowmod.util;

public interface IAnimatedPlayer {
    long wowmod$getLastLandTime();
    void wowmod$setLastLandTime(long time);
    boolean wowmod$wasOnGround();
    void wowmod$setWasOnGround(boolean onGround);

    // High Jump Mechanics
    float wowmod$getHighJumpCharge();
    void wowmod$setHighJumpCharge(float charge);

    // Long Jump Mechanics
    int wowmod$getLongJumpCooldown();
    void wowmod$setLongJumpCooldown(int ticks);
}