package net.wowmod;

import net.fabricmc.api.ModInitializer;
import net.wowmod.item.ModItemGroups;
import net.wowmod.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Weapons_Of_War implements ModInitializer {
	public static final String MOD_ID = "wowmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItemGroups.initialize();
        ModItems.registerModItems();
    }
}