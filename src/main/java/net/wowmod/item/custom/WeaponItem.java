package net.wowmod.item.custom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.wowmod.animation.PlayerAnimationManager;
import net.wowmod.animation.WeaponAnimationSet;
import net.wowmod.animation.WeaponConfigLoader;

public class WeaponItem extends ParryWeaponItem {

    public WeaponItem(Settings settings) {
        super(settings);
    }

    public WeaponAnimationSet getAnimations() {
        return WeaponConfigLoader.get(this);
    }

    @Override
    // 1. FIX: Change return type from 'boolean' to 'void'
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 1. Check if it is a Player first (safer)
        if (attacker instanceof PlayerEntity player) {

            // 2. Use the working world accessor (getEntityWorld() confirmed by you)
            World world = player.getEntityWorld();

            if (world.isClient()) {
                playAttackAnimation(player);
            }
        }

        super.postHit(stack, target, attacker);
        // REMOVED: return true;
    }

    @Override
    // 2. FIX: Remove the <ItemStack> type parameter from ActionResult
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            playAttackAnimation(user);
        }
        return super.use(world, user, hand);
    }

    private void playAttackAnimation(PlayerEntity player) {
        WeaponAnimationSet anims = getAnimations();
        if (anims != null && anims.attackAnim() != null) {
            PlayerAnimationManager.playAnimation(player, anims.attackAnim());
        }
    }
}