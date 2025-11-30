package net.wowmod.mixin;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.wowmod.util.RenderStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public class PlayerEntityRenderStateMixin implements RenderStateExtension {
    @Unique
    private boolean isSprinting;

    @Override
    public boolean wowmod$isSprinting() {
        return isSprinting;
    }

    @Override
    public void wowmod$setSprinting(boolean sprinting) {
        this.isSprinting = sprinting;
    }
}