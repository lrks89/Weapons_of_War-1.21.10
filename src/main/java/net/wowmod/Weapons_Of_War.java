package net.wowmod;

import net.fabricmc.api.ModInitializer;
import net.wowmod.item.ModItemGroups;
import net.wowmod.item.ModItems;

public class Weapons_Of_War implements ModInitializer {
	public static final String MOD_ID = "wowmod";

    @Override
    public void onInitialize() {
        ModItemGroups.initialize();
        ModItems.registerModItems();
    }
}