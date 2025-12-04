package net.wowmod.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    // This creates a public bridge to the protected 'getHandSwingDuration' method
    @Invoker("getHandSwingDuration")
    int wowmod$getHandSwingDuration();
}
