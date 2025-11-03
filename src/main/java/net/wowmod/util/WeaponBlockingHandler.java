package net.wowmod.util;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.wowmod.item.custom.WeaponItem;

public class WeaponBlockingHandler implements ServerLivingEntityEvents.AllowDamage {

    @Override
    public boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (entity instanceof PlayerEntity player) {
            if (player.isUsingItem()) {
                ItemStack blockingStack = player.getActiveItem();

                if (!blockingStack.isEmpty() && blockingStack.getItem() instanceof WeaponItem) {
                    int itemDamageToApply = 1;
                    blockingStack.damage(itemDamageToApply, player, EquipmentSlot.MAINHAND); // Use the correct slot
                    return false;
                }
            }
        }
        return true;
    }
}
