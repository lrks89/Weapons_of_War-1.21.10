// Make sure to rename the file to "WeaponsOfWar.java"
package net.wowmod;

import net.fabricmc.api.ModInitializer;
import net.wowmod.item.ModItemGroups;
import net.wowmod.item.ModItems;

// Import the Logger classes
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Refinement 1: Class name changed to UpperCamelCase
public class WeaponsOfWar implements ModInitializer {
    public static final String MOD_ID = "wowmod";

    // Refinement 2: Added a static logger
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Refinement 2: Added a log message for feedback
        LOGGER.info("Initializing Weapons of War!");

        // Your original methods are great. The order is good:
        // 1. Register items first.
        // 2. Register item groups (which use the items) second.
        // (I've swapped them based on typical dependency)
        ModItems.registerModItems();
        ModItemGroups.initialize();

        LOGGER.info("Weapons of War initialized successfully.");
    }
}