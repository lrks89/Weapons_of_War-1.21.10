package net.wowmod.util;

import net.minecraft.util.Hand;

public interface IParryPlayer {
    long wowmod_getLastParryTime();
    void wowmod_setLastParryTime(long time);
}
