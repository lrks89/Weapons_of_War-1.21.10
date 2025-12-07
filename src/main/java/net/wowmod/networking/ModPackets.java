package net.wowmod.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ModPackets {
    public static void registerPackets() {
        // Register the payload for Client -> Server communication
        PayloadTypeRegistry.playC2S().register(LongJumpPayload.ID, LongJumpPayload.CODEC);
    }
}