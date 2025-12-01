package net.wowmod.util;

public interface IAnimatedPlayer {
    long wowmod$getLastLandTime();
    void wowmod$setLastLandTime(long time);
    boolean wowmod$wasOnGround();
    void wowmod$setWasOnGround(boolean onGround);
}