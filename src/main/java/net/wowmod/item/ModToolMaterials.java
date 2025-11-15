package net.wowmod.item;

import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;

public class ModToolMaterials {

    public static final ToolMaterial IRON_WEAPONS = new ToolMaterial(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL,
            250,
            6.0F,
            5.0F,
            14,
            ItemTags.REPAIRS_IRON_ARMOR
    );
}