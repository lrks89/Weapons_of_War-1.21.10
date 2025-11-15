package net.wowmod.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.wowmod.WeaponsOfWar; // <-- We keep this import for the Logger

public class ModItemGroups {

    // REFINEMENT: Added a local MOD_ID to prevent potential static loading issues
    // and for consistency with ModItems.java
    public static final String MOD_ID = "wowmod";

    public static final ItemGroup WEAPONS_OF_WAR = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.M1223A_LONGSWORD))
            // This key is perfect, no change needed.
            .displayName(Text.translatable("itemGroup.wowmod.weapons_of_war"))
            .entries((context, entries) -> {

                entries.add(ModItems.M1113A_DAGGER);
                entries.add(ModItems.M1213A_SHORTSWORD);
                entries.add(ModItems.M1223A_LONGSWORD);
                entries.add(ModItems.M1513A_GREATSWORD);
                entries.add(ModItems.M2613A_SPEAR);

                entries.add(ModItems.TEST_SHIELD);

            }).build();

    public static void initialize() {
        // This is fine, as initialize() is called *after* WeaponsOfWar is loaded
        WeaponsOfWar.LOGGER.debug("Registering Mod Item Groups for " + MOD_ID);

        // REFINEMENT: Use the local MOD_ID
        Registry.register(Registries.ITEM_GROUP,
                Identifier.of(MOD_ID, "weapons_of_war"),
                WEAPONS_OF_WAR);
    }
}