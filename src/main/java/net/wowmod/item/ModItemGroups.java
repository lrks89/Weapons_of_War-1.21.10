package net.wowmod.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup WEAPONS_OF_WAR = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.M1223A_LONGSWORD))
            .displayName(Text.translatable("itemGroup.weapons_of_war.weapons_of_war"))
            .entries((context, entries) -> {

                entries.add(ModItems.M1113A_DAGGER);
                entries.add(ModItems.M1213A_SHORTSWORD);
                entries.add(ModItems.M1223A_LONGSWORD);
                entries.add(ModItems.M1513A_GREATSWORD);
                entries.add(ModItems.M2613A_SPEAR);

            }).build();

    public static void initialize() {
        // Since 1.21:
        Registry.register(Registries.ITEM_GROUP, Identifier.of("weapons_of_war", "weapons_of_war"), WEAPONS_OF_WAR);
    }
}
