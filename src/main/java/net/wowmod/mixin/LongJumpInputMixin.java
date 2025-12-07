package net.wowmod.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.wowmod.networking.LongJumpPayload;
import net.wowmod.util.IAnimatedPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class LongJumpInputMixin {

    @Unique private boolean wowmod$wasSneakPressed = false;
    @Unique private int wowmod$sneakHoldTicks = 0;
    @Unique private boolean wowmod$sprintingAtPress = false;

    // Defines a "Tap" vs "Hold" (7 ticks = 0.35 seconds)
    @Unique private static final int TAP_THRESHOLD = 7;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void checkLongJumpInput(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.options == null) return;

        boolean isSneakPressed = client.options.sneakKey.isPressed();

        if (isSneakPressed) {
            if (!wowmod$wasSneakPressed) {
                // --- KEY DOWN EVENT ---
                // Reset counter
                wowmod$sneakHoldTicks = 0;
                // Capture state: Were we sprinting when we started this tap?
                // This is crucial because vanilla will stop the sprint immediately after this tick.
                wowmod$sprintingAtPress = player.isSprinting();
            } else {
                // --- KEY HELD EVENT ---
                wowmod$sneakHoldTicks++;
            }
        } else {
            if (wowmod$wasSneakPressed) {
                // --- KEY UP EVENT (Release) ---

                // 1. Was it a short tap?
                // 2. Were we sprinting when we started pressing?
                // 3. Are we on the ground?
                if (wowmod$sneakHoldTicks <= TAP_THRESHOLD && wowmod$sprintingAtPress && player.isOnGround()) {

                    IAnimatedPlayer animatedPlayer = (IAnimatedPlayer) player;
                    // Check cooldown
                    if (animatedPlayer.wowmod$getLongJumpCooldown() <= 0) {
                        // Trigger Jump
                        ClientPlayNetworking.send(new LongJumpPayload());
                    }
                }
            }
        }

        wowmod$wasSneakPressed = isSneakPressed;
    }
}