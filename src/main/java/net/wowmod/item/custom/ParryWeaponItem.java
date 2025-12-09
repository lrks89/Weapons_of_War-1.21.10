package net.wowmod.item.custom;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.TridentItem;
import net.minecraft.item.CrossbowItem;
// Restored your requested import.
// If this errors on standard 1.21.1, switch to: net.minecraft.util.UseAction
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.wowmod.effect.ModEffects;
import net.wowmod.util.IParryPlayer;

public class ParryWeaponItem extends Item implements IParryItem {
    private static final int PARRY_COOLDOWN_TICKS = 10;

    private final int parryWindow;
    private final float damageReduction;

    public ParryWeaponItem(Settings settings, int parryWindow, float damageReduction) {
        super(settings);
        this.parryWindow = parryWindow;
        this.damageReduction = damageReduction;
    }

    @Override
    public int getParryWindow() {
        return this.parryWindow;
    }

    @Override
    public float getDamageReduction() {
        return this.damageReduction;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    /**
     * Reverting to 'void' as your environment reports a clash with 'boolean'.
     * Ensure IParryItem interface also uses 'void postHit(...)'.
     */
    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.damage(1, attacker, EquipmentSlot.MAINHAND);
    }

    @Override
    public boolean postMine(ItemStack stack, World world, net.minecraft.block.BlockState state, net.minecraft.util.math.BlockPos pos, LivingEntity miner) {
        if (state.getHardness(world, pos) != 0.0F) {
            stack.damage(2, miner, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        // 1. Check for Slimed Effect
        if (player.hasStatusEffect(ModEffects.SLIMED)) {
            return ActionResult.FAIL;
        }

        if (hand == Hand.MAIN_HAND) {
            ItemStack offhandStack = player.getStackInHand(Hand.OFF_HAND);
            Item offhandItem = offhandStack.getItem();

            if (offhandItem instanceof ShieldItem ||
                    offhandItem instanceof TridentItem ||
                    offhandItem instanceof CrossbowItem ||
                    offhandItem instanceof ParryWeaponItem ||
                    offhandItem instanceof ParryShieldItem) {

                return ActionResult.PASS;
            }
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