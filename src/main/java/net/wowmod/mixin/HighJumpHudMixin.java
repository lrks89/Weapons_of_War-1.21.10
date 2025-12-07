package net.wowmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.wowmod.util.IAnimatedPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class HighJumpHudMixin {

    @Shadow @Final private MinecraftClient client;

    private static final Identifier JUMP_BAR_BACKGROUND = Identifier.of("minecraft", "hud/jump_bar_background");
    private static final Identifier JUMP_BAR_PROGRESS = Identifier.of("minecraft", "hud/jump_bar_progress");

    // CHANGED: Target "render" instead of "renderMiscOverlays"
    // "render" is the master method that calls everything else.
    // Injecting at TAIL ensures we are the absolute last thing drawn.
    @Inject(method = "render", at = @At("TAIL"))
    private void renderHighJumpBar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ClientPlayerEntity player = this.client.player;

        if (player != null && !player.hasVehicle()) {
            // Check if game mode is Survival/Adventure (has XP bar) to ensure we overlap correctly
            // or just always render at height - 29 to cover that slot.

            float charge = ((IAnimatedPlayer) player).wowmod$getHighJumpCharge();

            if (charge > 0.0F) {
                int width = context.getScaledWindowWidth();
                int x = width / 2 - 91;
                int y = context.getScaledWindowHeight() - 29;

                // Draw Background
                context.drawGuiTexture(
                        RenderPipelines.GUI_TEXTURED,
                        JUMP_BAR_BACKGROUND,
                        x, y,
                        182, 5
                );

                // Draw Progress
                if (charge > 0.0F) {
                    int barWidth = (int)(charge * 182.0F);
                    if (barWidth > 0) {
                        context.drawGuiTexture(
                                RenderPipelines.GUI_TEXTURED,
                                JUMP_BAR_PROGRESS,
                                182, 5,
                                0, 0,
                                x, y,
                                barWidth, 5
                        );
                    }
                }
            }
        }
    }
}