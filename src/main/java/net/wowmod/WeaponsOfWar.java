package net.wowmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.wowmod.effect.ModEffects;
import net.wowmod.item.ModItemGroups;
import net.wowmod.item.ModItems;
import net.wowmod.networking.LongJumpHandler;
import net.wowmod.networking.LongJumpPayload;
import net.wowmod.networking.ModPackets;
import net.wowmod.potion.ModPotions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeaponsOfWar implements ModInitializer {
    public static final String MOD_ID = "wowmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Weapons of War!");

        // 0. Register Effects & Potions
        ModEffects.registerEffects();
        ModPotions.registerPotions();
        ModPotions.registerPotionRecipes(); // New method call

        // 1. Register Items & Groups
        ModItems.registerModItems();
        ModItemGroups.initialize();

        // 2. Register Packet Types
        ModPackets.registerPackets();

        // 3. Register Receiver
        ServerPlayNetworking.registerGlobalReceiver(LongJumpPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                LongJumpHandler.performLongJump(context.player());
            });
        });

        LOGGER.info("Weapons of War initialized successfully.");
    }
}