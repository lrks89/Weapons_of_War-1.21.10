package net.wowmod.item.custom;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.wowmod.effect.ModEffects;
import net.wowmod.util.IParryPlayer;

public class DualWieldedWeaponItem extends WeaponItem {

    private static final int PARRY_COOLDOWN_TICKS = 10;

    public DualWieldedWeaponItem(Settings settings, int parryWindow, float damageReduction) {
        super(settings, parryWindow, damageReduction);
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        // Logic copied from TwoHandedWeaponItem to ignore offhand items
        if (player.hasStatusEffect(ModEffects.SLIMED)) {
            return ActionResult.FAIL;
        }

        ItemStack itemStack = player.getStackInHand(hand);

        // Return FAIL instead of PASS to stop the game from attempting to use the Offhand
        if (player.getItemCooldownManager().isCoolingDown(itemStack)) {
            return ActionResult.FAIL;
        }

        player.setCurrentHand(hand);

        if (!world.isClient()) {
            ((IParryPlayer) player).wowmod_setLastParryTime(world.getTime());
            player.getItemCooldownManager().set(itemStack, PARRY_COOLDOWN_TICKS);
        }
        return ActionResult.CONSUME;
    }
}