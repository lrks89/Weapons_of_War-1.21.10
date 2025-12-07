package net.wowmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.wowmod.item.custom.IParryItem;
import net.wowmod.logic.ParryLogic;
import net.wowmod.util.IParryPlayer;
import net.wowmod.util.IParryStunnedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class BlockingPlayerEntityMixin implements IParryPlayer {

    @Unique private long wowmod_lastParryTime = 0L;

    @Override public long wowmod_getLastParryTime() { return this.wowmod_lastParryTime; }
    @Override public void wowmod_setLastParryTime(long time) { this.wowmod_lastParryTime = time; }

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void wowmod_parryCancel(
            ServerWorld serverWorld,
            DamageSource source,
            float originalAmount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // Delegate logic to ParryLogic
        if (ParryLogic.attemptPerfectParry((PlayerEntity)(Object)this, serverWorld, source)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @ModifyVariable(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(value = "HEAD"),
            argsOnly = true
    )
    private float wowmod_blockAndCounterLogic(
            float originalAmount,
            ServerWorld world,
            DamageSource source
    ) {
        // Delegate logic to ParryLogic
        return ParryLogic.calculateModifiedDamage((PlayerEntity)(Object)this, world, source, originalAmount);
    }

    // REMOVED: wowmod_playerConditionallyBypassBlock
    // This redirect failed because PlayerEntity likely does not contain the "isBlocking()" check
    // in its damage() method; it inherits that logic from LivingEntity.
    // The redirect in BlockingLivingEntityMixin should handle this for players as well.

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(value = "RETURN", ordinal = 1)
    )
    private void wowmod_spawnCounterCritParticles(
            ServerWorld world,
            DamageSource source,
            float originalAmount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ()) return;

        LivingEntity target = (LivingEntity) (Object) this;
        Entity attacker = source.getAttacker();

        if (attacker instanceof PlayerEntity && target instanceof IParryStunnedEntity stunnedTarget) {
            if (stunnedTarget.wowmod_getStunTicks() > 0) {
                world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.9D), target.getZ(),
                        10, 0.5D, 0.5D, 0.5D, 0.1D
                );
            }
        }
    }
}