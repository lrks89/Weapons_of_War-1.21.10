package net.wowmod.attribute;

import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.wowmod.WeaponsOfWar;

public class ModAttributes {

    public static final RegistryEntry<EntityAttribute> ATTACK_RANGE = register(
            "attack_range",
            new ClampedEntityAttribute("attribute.name.wowmod.attack_range", 3.0, 0.0, 1024.0).setTracked(true)
    );

    private static RegistryEntry<EntityAttribute> register(String id, EntityAttribute attribute) {
        return Registry.registerReference(Registries.ATTRIBUTE, Identifier.of(WeaponsOfWar.MOD_ID, id), attribute);
    }

    public static void registerModAttributes() {
        WeaponsOfWar.LOGGER.info("Registering Mod Attributes for " + WeaponsOfWar.MOD_ID);
    }
}