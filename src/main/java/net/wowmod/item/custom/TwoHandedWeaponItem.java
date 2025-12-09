package net.wowmod.item.custom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.wowmod.effect.ModEffects;
import net.wowmod.util.IParryPlayer;

public class TwoHandedWeaponItem extends WeaponItem {

    private static final int PARRY_COOLDOWN_TICKS = 10;

    public TwoHandedWeaponItem(Settings settings, int parryWindow, float damageReduction) {
        super(settings, parryWindow, damageReduction);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        // We no longer store/hide the item in NBT, allowing full inventory interaction.

        // "Disable" offhand usage by applying a constant short cooldown.
        // This prevents right-clicking (e.g., placing blocks, eating, using shields) while holding this weapon.
        if (slot == EquipmentSlot.MAINHAND && entity instanceof PlayerEntity player) {
            ItemStack offhandStack = player.getOffHandStack();
            if (!offhandStack.isEmpty()) {
                // Check if the item is already cooling down to prevent "fluttering" (UI refresh spam)
                if (!player.getItemCooldownManager().isCoolingDown(offhandStack)) {
                    // Apply a short cooldown (e.g., 10 ticks) to lock the item without spamming updates
                    player.getItemCooldownManager().set(offhandStack, 10);
                }
            }
        }
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        // 1. Check for Slimed Effect (inherited restriction)
        if (player.hasStatusEffect(ModEffects.SLIMED)) {
            return ActionResult.FAIL;
        }

        ItemStack itemStack = player.getStackInHand(hand);

        if (player.getItemCooldownManager().isCoolingDown(itemStack)) {
            return ActionResult.PASS;
        }

        player.setCurrentHand(hand);

        if (!world.isClient()) {
            ((IParryPlayer) player).wowmod_setLastParryTime(world.getTime());

            // Pass the itemStack, not the item, to the cooldown manager if that's what your version requires.
            player.getItemCooldownManager().set(itemStack, PARRY_COOLDOWN_TICKS);
        }
        return ActionResult.CONSUME;
    }
}