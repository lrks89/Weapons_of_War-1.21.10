package net.wowmod.mixin;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.PlayerEntity;
import net.wowmod.util.IPlayerStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin to add the 'wowmod_player' field to the vanilla PlayerEntityRenderState class.
 * This serves as the storage for the data passed in by PlayerEntityRendererMixin.
 */
@Mixin(PlayerEntityRenderState.class)
public class PlayerStateMixin implements IPlayerStateExtension {
    @Unique
    private PlayerEntity wowmod_player;

    @Override
    public void wowmod_setPlayer(PlayerEntity player) {
        this.wowmod_player = player;
    }

    @Override
    public PlayerEntity wowmod_getPlayer() {
        return this.wowmod_player;
    }
}