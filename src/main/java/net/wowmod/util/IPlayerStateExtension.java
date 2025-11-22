package net.wowmod.util;

import net.minecraft.entity.player.PlayerEntity;

public interface IPlayerStateExtension {
    void wowmod_setPlayer(PlayerEntity player);
    PlayerEntity wowmod_getPlayer();
}