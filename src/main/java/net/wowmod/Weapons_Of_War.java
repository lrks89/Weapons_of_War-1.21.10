package net.wowmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.wowmod.item.ModItemGroups;
import net.wowmod.item.ModItems;
import net.wowmod.networking.WeaponBlockingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Weapons_Of_War implements ModInitializer {
	public static final String MOD_ID = "wowmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItemGroups.initialize();
        ModItems.registerModItems();

        // Blocking Mechanic
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(new WeaponBlockingHandler());

        System.out.println("Weapons of War initialized!");
    }
}