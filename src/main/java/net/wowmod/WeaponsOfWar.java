package net.wowmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.wowmod.networking.LongJumpHandler;
import net.wowmod.item.ModItemGroups;
import net.wowmod.item.ModItems;
import net.wowmod.networking.ModPackets;
import net.wowmod.networking.LongJumpPayload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeaponsOfWar implements ModInitializer {
    public static final String MOD_ID = "wowmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Weapons of War!");

        ModItems.registerModItems();
        ModItemGroups.initialize();

        // 1. Register Packet Types
        ModPackets.registerPackets();

        // 2. Register Receiver using the new API
        ServerPlayNetworking.registerGlobalReceiver(LongJumpPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                // Logic moved to its own handler class
                LongJumpHandler.performLongJump(context.player());
            });
        });

        LOGGER.info("Weapons of War initialized successfully.");
    }
}