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

public class DualWieldedWeaponItem extends WeaponItem {

    private static final int PARRY_COOLDOWN_TICKS = 10;

    public DualWieldedWeaponItem(Settings settings, int parryWindow, float damageReduction) {
        super(settings, parryWindow, damageReduction);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        // Logic copied from TwoHandedWeaponItem to disable offhand usage
        if (slot == EquipmentSlot.MAINHAND && entity instanceof PlayerEntity player) {
            ItemStack offhandStack = player.getOffHandStack();
            if (!offhandStack.isEmpty()) {
                if (!player.getItemCooldownManager().isCoolingDown(offhandStack)) {
                    player.getItemCooldownManager().set(offhandStack, 10);
                }
            }
        }
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        // Logic copied from TwoHandedWeaponItem to ignore offhand items
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
            player.getItemCooldownManager().set(itemStack, PARRY_COOLDOWN_TICKS);
        }
        return ActionResult.CONSUME;
    }
}