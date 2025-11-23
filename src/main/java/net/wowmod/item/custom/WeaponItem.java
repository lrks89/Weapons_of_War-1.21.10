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
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player) {
            World world = player.getEntityWorld();
            if (world.isClient()) {
                playAttackAnimation(player);
            }
        }
        super.postHit(stack, target, attacker);
    }

    @Override
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