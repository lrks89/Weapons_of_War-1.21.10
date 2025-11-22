package net.wowmod.item;

// REFINEMENT: Restored all the necessary imports that were deleted
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
// REFINEMENT: Added imports for the correct registration pattern
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.wowmod.WeaponsOfWar;
import net.wowmod.item.custom.ParryShieldItem;
import net.wowmod.item.custom.ParryWeaponItem;
import net.wowmod.item.custom.WeaponItem;
// REFINEMENT: Added import for the registration pattern
import java.util.function.Function;

public class ModItems {

    // REFINEMENT: Added MOD_ID here to prevent static loading order crashes.
    public static final String MOD_ID = "wowmod";

    // REFINEMENT: Changed to the crash-proof registration pattern
    public static final Item M1113A_DAGGER = registerItem("m1113a_dagger",
            ParryWeaponItem::new, new Item.Settings().sword(ModToolMaterials.IRON_WEAPONS, -1, -2.0f));

    public static final Item M1213A_SHORTSWORD = registerItem("m1213a_shortsword",
            ParryWeaponItem::new, new Item.Settings().sword(ModToolMaterials.IRON_WEAPONS, 0, -2.4f));

    public static final Item M1223A_LONGSWORD = registerItem("m1223a_longsword",
            ParryWeaponItem::new, new Item.Settings().sword(ModToolMaterials.IRON_WEAPONS, 1, -2.8f));

    public static final Item M1513A_GREATSWORD = registerItem("m1513a_greatsword",
            ParryWeaponItem::new, new Item.Settings().sword(ModToolMaterials.IRON_WEAPONS, 2, -3.0f));

    public static final Item M2613A_SPEAR = registerItem("m2613a_spear",
            WeaponItem::new, new Item.Settings().sword(ModToolMaterials.IRON_WEAPONS, 0, -2.6f));


    public static final Item TEST_SHIELD = registerItem("test_shield",
            ParryShieldItem::new, new Item.Settings().maxDamage(500));

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

    // REFINEMENT: Reverted to the correct, crash-proof registration method.
    // This creates the Item's ID *before* the item is created, fixing the crash.
    private static <T extends Item> T registerItem(String name, Function<Item.Settings, T> itemFactory, Item.Settings settings) {
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, name));
        T item = itemFactory.apply(settings.registryKey(itemKey));
        Registry.register(Registries.ITEM, itemKey, item);
        return item;
    }

    public static void registerModItems() {
        // This log is great for debugging to make sure this class is loaded.
        // This is safe to call, as this method is called *after* WeaponsOfWar is initialized.
        WeaponsOfWar.LOGGER.debug("Registering Mod Items for " + MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(ModItems::customModIngredients);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(ModItems::customModWeapons);
    }
}