package net.wowmod.item.custom;

import com.mojang.serialization.DataResult;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.wowmod.WeaponsOfWar;
import org.jetbrains.annotations.Nullable;

public class TwoHandedWeaponItem extends WeaponItem {

    private static final String OFFHAND_STORE_KEY = "wowmod_stored_offhand";

    public TwoHandedWeaponItem(Settings settings, int parryWindow, float damageReduction) {
        super(settings, parryWindow, damageReduction);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        // Logic only runs on server (ServerWorld guarantees this) and for players
        if (entity instanceof PlayerEntity player) {

            // Determine if this specific item stack is currently held in the main hand
            boolean selected = (slot == EquipmentSlot.MAINHAND);

            // 1. Equip Logic: Player holds weapon AND has offhand item
            if (selected && !player.getOffHandStack().isEmpty()) {
                storeOffhandItem(stack, player, world.getRegistryManager());
            }
            // 2. Unequip Logic: Player is NOT holding weapon (or logic runs) AND has stored item
            else if (!selected && hasStoredItem(stack)) {
                restoreOffhandItem(stack, player, world.getRegistryManager());
            }
        }
    }

    private boolean hasStoredItem(ItemStack stack) {
        // Fast check to avoid expensive NBT operations every tick
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);

        // FIX: NbtComponent does not have .contains(). We must use .copyNbt() to check the underlying compound.
        // Added !component.isEmpty() check first to avoid copying empty tags.
        return component != null && !component.isEmpty() && component.copyNbt().contains(OFFHAND_STORE_KEY);
    }

    private void storeOffhandItem(ItemStack stack, PlayerEntity player, RegistryWrapper.WrapperLookup registries) {
        ItemStack offHandStack = player.getOffHandStack();

        // Get mutable NBT
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = component.copyNbt();

        if (nbt.contains(OFFHAND_STORE_KEY)) {
            // Safety: If slot is already full, drop the new item to prevent deletion
            player.dropItem(offHandStack, false);
        } else {
            // Encode using 1.21 Codec
            DataResult<NbtElement> result = ItemStack.CODEC.encodeStart(registries.getOps(NbtOps.INSTANCE), offHandStack);

            result.resultOrPartial(error -> WeaponsOfWar.LOGGER.error("Failed to store offhand: " + error))
                    .ifPresent(element -> {
                        nbt.put(OFFHAND_STORE_KEY, element);
                        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
                    });
        }

        // Clear offhand
        player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
    }

    private void restoreOffhandItem(ItemStack stack, PlayerEntity player, RegistryWrapper.WrapperLookup registries) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = component.copyNbt();

        if (nbt.contains(OFFHAND_STORE_KEY)) {
            NbtElement storedNbt = nbt.get(OFFHAND_STORE_KEY);

            // Decode using 1.21 Codec
            ItemStack stored = ItemStack.CODEC.parse(registries.getOps(NbtOps.INSTANCE), storedNbt)
                    .resultOrPartial(error -> WeaponsOfWar.LOGGER.error("Failed to restore offhand: " + error))
                    .orElse(ItemStack.EMPTY);

            if (!stored.isEmpty()) {
                if (player.getOffHandStack().isEmpty()) {
                    player.setStackInHand(Hand.OFF_HAND, stored);
                } else {
                    player.getInventory().offerOrDrop(stored);
                }
            }

            // Remove key and update stack
            nbt.remove(OFFHAND_STORE_KEY);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        }
    }
}