package net.wowmod.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.wowmod.effect.ModEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlimeEntity.class)
public abstract class MobSlimeEntityMixin extends MobEntity {

    protected MobSlimeEntityMixin(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "damage", at = @At(value = "HEAD"))
    private void applySlimedEffect(LivingEntity target, CallbackInfo ci) {
        if (target instanceof PlayerEntity player) {
            // Apply for 5 seconds (100 ticks)
            player.addStatusEffect(new StatusEffectInstance(ModEffects.SLIMED, 160, 0));
        }
    }
}