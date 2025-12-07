package net.wowmod.mixin;

import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Item.class)
public interface ItemAccessor {
    // In 1.21+, item properties are stored in this "components" map
    @Accessor("components")
    @Mutable
    void wowmod$setComponents(ComponentMap components);

    @Accessor("components")
    ComponentMap wowmod$getComponents();
}