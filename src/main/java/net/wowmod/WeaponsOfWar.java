package net.wowmod;

import net.fabricmc.api.ModInitializer;
import net.wowmod.item.ModItemGroups;
import net.wowmod.item.ModItems;

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

        LOGGER.info("Weapons of War initialized successfully.");
    }
}