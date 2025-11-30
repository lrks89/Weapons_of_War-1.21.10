package net.wowmod;

import net.fabricmc.api.ClientModInitializer;
// Added
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.wowmod.animation.AnimationLoader;

import java.util.ArrayList;
import java.util.List;

public class WeaponsOfWarClient implements ClientModInitializer {

    private static final Identifier RANGE_ID = Identifier.of("minecraft", "entity_interaction_range");

    @Override
    public void onInitializeClient() {

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new AnimationLoader());
    }
}