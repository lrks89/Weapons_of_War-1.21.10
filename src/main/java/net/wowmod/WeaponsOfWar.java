package net.wowmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
// TypedActionResult import removed
import net.wowmod.effect.ModEffects;
import net.wowmod.item.ModItemGroups;
import net.wowmod.item.ModItems;
import net.wowmod.item.custom.DualWieldedWeaponItem;
import net.wowmod.item.custom.TwoHandedWeaponItem;
import net.wowmod.networking.LongJumpHandler;
import net.wowmod.networking.LongJumpPayload;
import net.wowmod.networking.ModPackets;
import net.wowmod.potion.ModPotions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeaponsOfWar implements ModInitializer {
    public static final String MOD_ID = "wowmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Weapons of War!");

        // 0. Register Effects & Potions
        ModEffects.registerEffects();
        ModPotions.registerPotions();
        ModPotions.registerPotionRecipes();

        // 1. Register Items & Groups
        ModItems.registerModItems();
        ModItemGroups.initialize();

        // 2. Register Packet Types
        ModPackets.registerPackets();

        // 3. Register Receiver
        ServerPlayNetworking.registerGlobalReceiver(LongJumpPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                LongJumpHandler.performLongJump(context.player());
            });
        });

        // 4. Robust Offhand Prevention
        registerOffhandBlocking();

        LOGGER.info("Weapons of War initialized successfully.");
    }

    private void registerOffhandBlocking() {
        // Prevent Item Usage (Right Click Air/Item) with Offhand
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand == Hand.OFF_HAND) {
                ItemStack mainStack = player.getMainHandStack();
                if (mainStack.getItem() instanceof TwoHandedWeaponItem ||
                        mainStack.getItem() instanceof DualWieldedWeaponItem) {
                    // Changed from TypedActionResult.fail(...) to ActionResult.FAIL
                    return ActionResult.FAIL;
                }
            }
            // Changed from TypedActionResult.pass(...) to ActionResult.PASS
            return ActionResult.PASS;
        });

        // Prevent Block Interaction (Right Click Block) with Offhand
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand == Hand.OFF_HAND) {
                ItemStack mainStack = player.getMainHandStack();
                if (mainStack.getItem() instanceof TwoHandedWeaponItem ||
                        mainStack.getItem() instanceof DualWieldedWeaponItem) {
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });
    }
}