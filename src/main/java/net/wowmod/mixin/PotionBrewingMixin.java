package net.wowmod.mixin;

import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.wowmod.potion.ModPotions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingRecipeRegistry.class)
public abstract class PotionBrewingMixin {

    @Inject(method = "registerDefaults", at = @At("TAIL"))
    private static void wowmod$registerBrewingRecipes(BrewingRecipeRegistry.Builder builder, CallbackInfo ci) {
        // Awkward Potion + Slime Ball -> Potion of Slimed
        builder.registerPotionRecipe(Potions.AWKWARD, Items.SLIME_BALL, ModPotions.SLIMED_POTION);
    }
}