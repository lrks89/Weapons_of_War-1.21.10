package net.wowmod.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.wowmod.util.IAnimatedPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class HighJumpClientPlayerMixin extends AbstractClientPlayerEntity {

    public HighJumpClientPlayerMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Unique private int wowmod$jumpChargeTicks;
    @Unique private boolean wowmod$isChargingJump;
    @Unique private boolean wowmod$isChargeIncreasing = true;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void manageHighJumpInput(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        boolean isJumpPressed = MinecraftClient.getInstance().options.jumpKey.isPressed();
        boolean isSneaking = player.isSneaking();

        // 1. START Charging Condition:
        // Must be on ground, sneaking, and holding jump. Not already charging.
        if (!wowmod$isChargingJump && player.isOnGround() && isSneaking && isJumpPressed) {
            wowmod$isChargingJump = true;
            wowmod$jumpChargeTicks = 0;
            wowmod$isChargeIncreasing = true;
        }

        // 2. CONTINUE Charging Condition:
        // If we are already charging, we stay charging as long as we hold sneak.
        // We do NOT check isOnGround here. This prevents the bar from vanishing if the player
        // slightly lifts off due to a bump or server correction.
        if (wowmod$isChargingJump) {

            // If player stops sneaking, cancel everything immediately.
            if (!isSneaking) {
                wowmod$isChargingJump = false;
                wowmod$jumpChargeTicks = 0;
                ((IAnimatedPlayer) player).wowmod$setHighJumpCharge(0.0f);
                return;
            }

            if (isJumpPressed) {
                // Still holding jump -> Continue Charging logic (Ping-Pong)
                if (wowmod$isChargeIncreasing) {
                    wowmod$jumpChargeTicks++;
                    if (wowmod$jumpChargeTicks >= 25) {
                        wowmod$isChargeIncreasing = false;
                    }
                } else {
                    wowmod$jumpChargeTicks--;
                    if (wowmod$jumpChargeTicks <= 0) {
                        wowmod$isChargeIncreasing = true;
                    }
                }

                // Sync charge to interface for HUD
                float charge = (float) wowmod$jumpChargeTicks / 25.0f;
                ((IAnimatedPlayer) player).wowmod$setHighJumpCharge(charge);

            } else {
                // Released Jump Key -> EXECUTE JUMP
                wowmod$isChargingJump = false;

                // Only trigger the jump if we have a meaningful charge (prevents accidental hops)
                if (wowmod$jumpChargeTicks > 1) {
                    player.jump();
                }

                // Reset ticks after jump
                wowmod$jumpChargeTicks = 0;
                // Charge float is reset in the jump() mixin or next tick
            }
        } else {
            // Not charging, ensure HUD is clear
            ((IAnimatedPlayer) player).wowmod$setHighJumpCharge(0.0f);
        }
    }
}