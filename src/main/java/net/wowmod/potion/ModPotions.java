package net.wowmod.potion;

import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.wowmod.WeaponsOfWar;
import net.wowmod.effect.ModEffects;
import net.wowmod.mixin.ItemAccessor;

public class ModPotions {
    public static RegistryEntry<Potion> SLIMED_POTION;

    public static void registerPotions() {
        // Changed duration to 600 ticks (30 seconds)
        SLIMED_POTION = register("slimed_potion",
                new Potion("slimed_potion", new StatusEffectInstance(ModEffects.SLIMED, 2400, 0)));

        // Increase stack size for all potions to 16 by updating their components
        modifyItemMaxCount(Items.POTION, 16);
        modifyItemMaxCount(Items.SPLASH_POTION, 16);
        modifyItemMaxCount(Items.LINGERING_POTION, 16);
    }

    private static void modifyItemMaxCount(Item item, int count) {
        // 1. Get existing components
        ComponentMap oldMap = ((ItemAccessor) item).wowmod$getComponents();

        // 2. Create a new map with the updated MAX_STACK_SIZE
        ComponentMap newMap = ComponentMap.builder()
                .addAll(oldMap)
                .add(DataComponentTypes.MAX_STACK_SIZE, count)
                .build();

        // 3. Set the new components back to the item
        ((ItemAccessor) item).wowmod$setComponents(newMap);
    }

    public static void registerPotionRecipes() {
        // Logic moved to PotionBrewingMixin.java to bypass missing Fabric API class
    }

    private static RegistryEntry<Potion> register(String name, Potion potion) {
        return Registry.registerReference(Registries.POTION, Identifier.of(WeaponsOfWar.MOD_ID, name), potion);
    }
}