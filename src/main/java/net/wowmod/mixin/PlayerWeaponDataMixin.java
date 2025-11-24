package net.wowmod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.wowmod.util.IWeaponTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntity.class)
public class PlayerWeaponDataMixin implements IWeaponTransform {

    @Unique
    private Vec3d wowmod_weaponRotation = Vec3d.ZERO;

    @Unique
    private Vec3d wowmod_weaponPosition = Vec3d.ZERO;

    @Override
    public void wowmod_setWeaponRotation(Vec3d rotation) {
        this.wowmod_weaponRotation = rotation;
    }

    @Override
    public void wowmod_setWeaponPosition(Vec3d position) {
        this.wowmod_weaponPosition = position;
    }

    @Override
    public Vec3d wowmod_getWeaponRotation() {
        return this.wowmod_weaponRotation;
    }

    @Override
    public Vec3d wowmod_getWeaponPosition() {
        return this.wowmod_weaponPosition;
    }
}