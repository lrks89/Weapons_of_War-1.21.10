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
import net.wowmod.Weapons_Of_War;

import java.util.function.Function;

public class ModItems {
    public static final Item M1113A_DAGGER = registerWeapon("m1113a_dagger",
            Item::new, new Item.Settings().sword (ModToolMaterials.IRON_WEAPONS, -1, -2.0f));
    public static final Item M1213A_SHORTSWORD = registerWeapon("m1213a_shortsword",
            Item::new, new Item.Settings().sword (ModToolMaterials.IRON_WEAPONS, 0, -2.4f));
    public static final Item M1223A_LONGSWORD = registerWeapon("m1223a_longsword",
            Item::new, new Item.Settings().sword (ModToolMaterials.IRON_WEAPONS, 1, -2.8f));
    public static final Item M1513A_GREATSWORD = registerWeapon("m1513a_greatsword",
            Item::new, new Item.Settings().sword (ModToolMaterials.IRON_WEAPONS, 2, -3.0f));
    public static final Item M2613A_SPEAR = registerWeapon("m2613a_spear",
            Item::new, new Item.Settings().sword (ModToolMaterials.IRON_WEAPONS, 0, -2.6f));

    private static void customModIngredients(FabricItemGroupEntries entries) {
    }
    private static void customModWeapons(FabricItemGroupEntries entries) {

        entries.add(M1113A_DAGGER);
        entries.add(M1213A_SHORTSWORD);
        entries.add(M1223A_LONGSWORD);
        entries.add(M1513A_GREATSWORD);
        entries.add(M2613A_SPEAR);

    }
    private static Item registerWeapon(String name, Function<Item.Settings, Item> itemFactory, Item.Settings settings) {
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Weapons_Of_War.MOD_ID, name));
        Item item = itemFactory.apply(settings.registryKey(itemKey));
        Registry.register(Registries.ITEM, itemKey, item);
        return item;
    }
    public static void  registerModItems(){
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS). register(ModItems::customModIngredients);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT). register(ModItems::customModWeapons);
    }
}
