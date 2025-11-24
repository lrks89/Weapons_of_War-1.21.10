package net.wowmod.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.wowmod.WeaponsOfWar;
import net.wowmod.item.custom.ParryShieldItem;
import net.wowmod.item.custom.ParryWeaponItem;
import net.wowmod.item.custom.WeaponItem;
import java.util.function.Function;

public class ModItems {

    public static final String MOD_ID = "wowmod";
    public static final Identifier WEAPON_RANGE_MODIFIER_ID = Identifier.of(MOD_ID, "weapon_range_modifier");

    // DYNAMIC LOOKUP: Finds the attribute by ID string ("minecraft:player.entity_interaction_range")
    private static final RegistryEntry<EntityAttribute> RANGE_ATTRIBUTE =
            Registries.ATTRIBUTE.getEntry(Registries.ATTRIBUTE.get(Identifier.of("minecraft", "entity_interaction_range")));

    // --- WEAPONS ---
    public static final Item M1113A_DAGGER = registerItem("m1113a_dagger",
            WeaponItem::new, createWeaponSettings(3.0, -1.2f, -0.5));

    public static final Item M1213A_SHORTSWORD = registerItem("m1213a_shortsword",
            WeaponItem::new, createWeaponSettings(4.0, -2.0f, 0.0));

    public static final Item M1223A_LONGSWORD = registerItem("m1223a_longsword",
            WeaponItem::new, createWeaponSettings(6.0, -2.6f, 0.5));

    public static final Item M1513A_GREATSWORD = registerItem("m1513a_greatsword",
            WeaponItem::new, createWeaponSettings(8.0, -3.0f, 1.0));

    public static final Item M2613A_SPEAR = registerItem("m2613a_spear",
            WeaponItem::new, createWeaponSettings(5.0, -2.8f, 2.0));

    public static final Item TEST_SHIELD = registerItem("test_shield",
            ParryShieldItem::new, new Item.Settings().maxDamage(500));

    // --- HELPER METHODS ---

    private static Item.Settings createWeaponSettings(double damage, float speed, double range) {
        return new Item.Settings()
                .maxDamage(250)
                .attributeModifiers(AttributeModifiersComponent.builder()
                                .add(
                                        EntityAttributes.ATTACK_DAMAGE,
                                        new EntityAttributeModifier(
                                                Item.BASE_ATTACK_DAMAGE_MODIFIER_ID,
                                                damage,
                                                EntityAttributeModifier.Operation.ADD_VALUE
                                        ),
                                        AttributeModifierSlot.MAINHAND
                                )
                                .add(
                                        EntityAttributes.ATTACK_SPEED,
                                        new EntityAttributeModifier(
                                                Item.BASE_ATTACK_SPEED_MODIFIER_ID,
                                                speed,
                                                EntityAttributeModifier.Operation.ADD_VALUE
                                        ),
                                        AttributeModifierSlot.MAINHAND
                                )
                                // Use our dynamic lookup variable
                                .add(
                                        RANGE_ATTRIBUTE,
                                        new EntityAttributeModifier(
                                                WEAPON_RANGE_MODIFIER_ID,
                                                range,
                                                EntityAttributeModifier.Operation.ADD_VALUE
                                        ),
                                        AttributeModifierSlot.MAINHAND
                                )
                                .build()
                        // REMOVED .withShowInTooltip(false) to fix error
                );
    }

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
        T item = itemFactory.apply(settings.registryKey(itemKey));
        Registry.register(Registries.ITEM, itemKey, item);
        return item;
    }

    public static void registerModItems() {
        WeaponsOfWar.LOGGER.debug("Registering Mod Items for " + MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(ModItems::customModIngredients);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(ModItems::customModWeapons);
    }
}