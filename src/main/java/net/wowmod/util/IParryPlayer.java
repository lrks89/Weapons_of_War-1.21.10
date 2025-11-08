package net.wowmod.util;

public interface IParryPlayer {
    long wowmod_getLastParryTime();
    void wowmod_setLastParryTime(long time);
    int wowmod_getShieldDisableTicks();
    void wowmod_setShieldDisableTicks(int ticks);
}
