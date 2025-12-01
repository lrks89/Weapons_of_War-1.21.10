package net.wowmod.util;

public interface IAnimatedPlayer {
    long wowmod$getLastLandTime();
    void wowmod$setLastLandTime(long time);
    boolean wowmod$wasOnGround();
    void wowmod$setWasOnGround(boolean onGround);

    // New: Track if the block immediately below the player was a fluid (used for jump suppression)
    boolean wowmod$wasFluidBelow();
    void wowmod$setWasFluidBelow(boolean fluidBelow);

    long wowmod$getLastFluidContactTime();
    void wowmod$setLastFluidContactTime(long time);
}