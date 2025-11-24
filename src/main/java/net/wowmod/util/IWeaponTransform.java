package net.wowmod.util;

import net.minecraft.util.math.Vec3d;

public interface IWeaponTransform {
    void wowmod_setWeaponRotation(Vec3d rotation);
    void wowmod_setWeaponPosition(Vec3d position);

    Vec3d wowmod_getWeaponRotation();
    Vec3d wowmod_getWeaponPosition();
}