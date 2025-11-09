package net.wowmod.item.custom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.wowmod.util.IParryPlayer;

public class ParryShieldItem extends ShieldItem {

    //Parry Cooldown
    private static final int PARRY_COOLDOWN_TICKS = 8;

    public ParryShieldItem(Settings settings) {
        super(settings);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {

        ItemStack itemStack = player.getStackInHand(hand);

        // Check weapon cooldown (parry debounce)
        if (player.getItemCooldownManager().isCoolingDown(itemStack)) {
            return ActionResult.PASS;
        }

        // Start the blocking/parry animation
        player.setCurrentHand(hand);

        if (!world.isClient() && player instanceof IParryPlayer parryPlayer) {
            parryPlayer.wowmod_setLastParryTime(world.getTime());
            player.getItemCooldownManager().set(itemStack, PARRY_COOLDOWN_TICKS);
        }
        return ActionResult.CONSUME;
    }
}