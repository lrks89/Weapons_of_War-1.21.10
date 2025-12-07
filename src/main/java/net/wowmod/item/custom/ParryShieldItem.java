package net.wowmod.item.custom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.wowmod.util.IParryPlayer;

public class ParryShieldItem extends ShieldItem implements IParryItem {

    private static final int PARRY_COOLDOWN_TICKS = 8;

    public ParryShieldItem(Settings settings) {
        super(settings);
    }

    @Override
    public int getParryWindow() {
        return 5; // Standard window for shields
    }

    @Override
    public float getDamageReduction() {
        return 1.0f; // 100% block
    }

    // Removed appendTooltip override. Using Client ItemTooltipCallback instead.

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

        // Check for parry spam (de-bounce)
        if (player.getItemCooldownManager().isCoolingDown(itemStack)) {
            return ActionResult.PASS;
        }

        // This is the default ShieldItem behavior, it starts the 'using' animation
        player.setCurrentHand(hand);

        if (!world.isClient()) {
            // Set the parry time on the player
            // This is the same logic WeaponItem uses
            ((IParryPlayer) player).wowmod_setLastParryTime(world.getTime());

            // Apply the cooldown
            player.getItemCooldownManager().set(itemStack, PARRY_COOLDOWN_TICKS);
        }

        return ActionResult.CONSUME;
    }
}