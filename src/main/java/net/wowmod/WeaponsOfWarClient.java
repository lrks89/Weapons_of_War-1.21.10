package net.wowmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import net.wowmod.animation.AnimationLoader;
import net.wowmod.animation.WeaponConfigLoader;

public class WeaponsOfWarClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register the Animation Loader (Loads the .json files from assets)
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new AnimationLoader());

        // Register the Weapon Config Loader (Loads the .json files from data)
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new WeaponConfigLoader());
    }
}