package net.wowmod.item.custom;

public interface IParryItem {
    /**
     * @return The number of ticks after raising the item during which a "Perfect Parry" occurs.
     */
    int getParryWindow();

    /**
     * @return The percentage of damage blocked during a standard block (not a perfect parry).
     * 1.0f = 100% blocked, 0.5f = 50% blocked.
     */
    float getDamageReduction();
}