package net.wowmod.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ShieldItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.wowmod.Weapons_Of_War;
import net.wowmod.item.custom.ParryShieldItem;
import net.wowmod.item.custom.WeaponItem;

import java.util.function.Function;

public class ModItems {
    public static final Item M1113A_DAGGER = registerItem("m1113a_dagger",
            WeaponItem::new, new Item.Settings().sword (ModToolMaterials.IRON_WEAPONS, -1, -2.0f));

    public static final Item M1213A_SHORTSWORD = registerItem("m1213a_shortsword",
            WeaponItem::new, new Item.Settings().sword (ModToolMaterials.IRON_WEAPONS, 0, -2.4f));

    public static final Item M1223A_LONGSWORD = registerItem("m1223a_longsword",
            WeaponItem::new, new Item.Settings().sword (ModToolMaterials.IRON_WEAPONS, 1, -2.8f));

    public static final Item M1513A_GREATSWORD = registerItem("m1513a_greatsword",
            WeaponItem::new, new Item.Settings().sword (ModToolMaterials.IRON_WEAPONS, 2, -3.0f));

    public static final Item M2613A_SPEAR = registerItem("m2613a_spear",
            WeaponItem::new, new Item.Settings().sword (ModToolMaterials.IRON_WEAPONS, 0, -2.6f));


    public static final Item TEST_SHIELD = registerShieldItem("test_shield",
            ParryShieldItem::new,
            new Item.Settings().maxDamage(500));

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
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Weapons_Of_War.MOD_ID, name));
        T item = itemFactory.apply(settings.registryKey(itemKey));
        Registry.register(Registries.ITEM, itemKey, item);
        return item;
    }

    private static <T extends Item> T registerShieldItem(String name, Function<Item.Settings, T> itemFactory, Item.Settings settings) {
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Weapons_Of_War.MOD_ID, name));
        T item = itemFactory.apply(settings.registryKey(itemKey));
        Registry.register(Registries.ITEM, itemKey, item);
        return item;
    }

    public static void  registerModItems(){
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS). register(ModItems::customModIngredients);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT). register(ModItems::customModWeapons);
    }
}
