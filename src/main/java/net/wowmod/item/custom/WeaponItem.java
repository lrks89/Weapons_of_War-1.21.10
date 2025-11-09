package net.wowmod.item.custom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem; // ADDED
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem; // ADDED
import net.minecraft.item.TridentItem; // ADDED
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.wowmod.util.IParryPlayer;

public class WeaponItem extends Item {
    // --- Blocking / Parrying Mechanics ---
    private static final int PARRY_COOLDOWN_TICKS = 10; //0.5 second cooldown

    public WeaponItem(Settings settings) {
        super(settings);
    }

    // Applies Blocking Mechanic to WeaponItems
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        // Keeping this high so the player can block indefinitely
        return 72000;
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {

        // --- UPDATED PRIORITY CHECK FOR OFFHAND ITEMS ---
        if (hand == Hand.MAIN_HAND) {
            ItemStack offhandStack = player.getStackInHand(Hand.OFF_HAND);
            Item offhandItem = offhandStack.getItem();

            // 1. Check if the offhand item is a high-priority vanilla item (Shield, Trident, Crossbow).
            // 2. OR, check if the offhand item is a custom WeaponItem.
            //    If either is true, return PASS to give the offhand slot priority.
            if (offhandItem instanceof ShieldItem ||
                    offhandItem instanceof TridentItem ||
                    offhandItem instanceof CrossbowItem ||
                    offhandItem instanceof WeaponItem ||
                    offhandItem instanceof ParryShieldItem) {

                return ActionResult.PASS;
            }
        }
        // --- END PRIORITY CHECK ---

        ItemStack itemStack = player.getStackInHand(hand);

        // Check weapon cooldown (parry debounce)
        if (player.getItemCooldownManager().isCoolingDown(itemStack)) {
            return ActionResult.PASS;
        }

        // Start the blocking/parry animation
        player.setCurrentHand(hand);

        if (!world.isClient()) {
            // Server-side logic for setting the parry window and applying cooldown
            ((IParryPlayer) player).wowmod_setLastParryTime(world.getTime());
            player.getItemCooldownManager().set(itemStack, PARRY_COOLDOWN_TICKS);
        }
        return ActionResult.CONSUME;
    }
}