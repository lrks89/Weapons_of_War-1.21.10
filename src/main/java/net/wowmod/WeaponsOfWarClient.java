package net.wowmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.wowmod.animation.player_animations.AnimationLoader;
import net.wowmod.animation.player_animations.player_weapons.WeaponAnimationLoader;

public class WeaponsOfWarClient implements ClientModInitializer {

    private static final Identifier RANGE_ID = Identifier.of("minecraft", "entity_interaction_range");

    @Override
    public void onInitializeClient() {
        // Register the generic animation loader
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new AnimationLoader());

        // Register the weapon-specific configuration loader
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new WeaponAnimationLoader());
    }
}