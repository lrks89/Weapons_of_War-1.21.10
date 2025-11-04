package net.wowmod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.wowmod.util.IParryPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityBlockingMixin implements IParryPlayer {

    @Unique
    private long wowmod_lastParryTime = 0; // The custom field

    @Override
    public long wowmod_getLastParryTime() {
        return this.wowmod_lastParryTime;
    }

    @Override
    public void wowmod_setLastParryTime(long time) {
        this.wowmod_lastParryTime = time;
    }
}
