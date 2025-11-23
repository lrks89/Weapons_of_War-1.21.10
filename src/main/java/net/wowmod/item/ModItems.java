package net.wowmod.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.wowmod.WeaponsOfWar;
import net.wowmod.item.custom.ParryShieldItem;
import net.wowmod.item.custom.ParryWeaponItem;
import net.wowmod.item.custom.WeaponItem;
import java.util.function.Function;

public class ModItems {

    public static final String MOD_ID = "wowmod";

    // FIX: Added .maxDamage(250) to all weapons so they can take durability damage when blocking.
    // Without this, the items are considered "unbreakable" by the game logic.

    public static final Item M1113A_DAGGER = registerItem("m1113a_dagger",
            ParryWeaponItem::new, new Item.Settings()
                    .maxDamage(250)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "m1113a_dagger"))));

    public static final Item M1213A_SHORTSWORD = registerItem("m1213a_shortsword",
            ParryWeaponItem::new, new Item.Settings()
                    .maxDamage(250)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "m1213a_shortsword"))));

    public static final Item M1223A_LONGSWORD = registerItem("m1223a_longsword",
            ParryWeaponItem::new, new Item.Settings()
                    .maxDamage(250)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "m1223a_longsword"))));

    public static final Item M1513A_GREATSWORD = registerItem("m1513a_greatsword",
            ParryWeaponItem::new, new Item.Settings()
                    .maxDamage(250)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "m1513a_greatsword"))));

    public static final Item M2613A_SPEAR = registerItem("m2613a_spear",
            WeaponItem::new, new Item.Settings()
                    .maxDamage(250)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "m2613a_spear"))));


    public static final Item TEST_SHIELD = registerItem("test_shield",
            ParryShieldItem::new, new Item.Settings().maxDamage(500).registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "test_shield"))));

    private static void customModIngredients(FabricItemGroupEntries entries) {
    }

    private static void customModWeapons(FabricItemGroupEntries entries) {
        entries.add(M1113A_DAGGER);
        entries.add(M1213A_SHORTSWORD);
        entries.add(M1223A_LONGSWORD);
        entries.add(M1513A_GREATSWORD);
        entries.add(M2613A_SPEAR);

        entries.add(TEST_SHIELD);
    }

    private static <T extends Item> T registerItem(String name, Function<Item.Settings, T> itemFactory, Item.Settings settings) {
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, name));
        // Settings must have the key assigned
        T item = itemFactory.apply(settings);
        Registry.register(Registries.ITEM, itemKey, item);
        return item;
    }

    public static void registerModItems() {
        WeaponsOfWar.LOGGER.debug("Registering Mod Items for " + MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(ModItems::customModIngredients);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(ModItems::customModWeapons);
    }
}