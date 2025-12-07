package net.wowmod.effect;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.wowmod.WeaponsOfWar;

public class ModEffects {

    public static final RegistryEntry<StatusEffect> SLIMED = register("slimed", new SlimedEffect()
            .addAttributeModifier(
                    EntityAttributes.ATTACK_SPEED,
                    Identifier.of(WeaponsOfWar.MOD_ID, "effect.slimed.attack_speed"),
                    -0.8,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));

    private static RegistryEntry<StatusEffect> register(String id, StatusEffect effect) {
        return Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(WeaponsOfWar.MOD_ID, id), effect);
    }

    public static void registerEffects() {
        WeaponsOfWar.LOGGER.info("Registering Mod Effects for " + WeaponsOfWar.MOD_ID);
    }
}