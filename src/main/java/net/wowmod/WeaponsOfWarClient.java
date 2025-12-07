package net.wowmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.wowmod.animation.player_animations.AnimationLoader;
import net.wowmod.animation.player_animations.player_weapons.WeaponAnimationLoader;
import net.wowmod.item.custom.IParryItem;

import java.util.ArrayList;
import java.util.List;

public class WeaponsOfWarClient implements ClientModInitializer {

    private static final Identifier RANGE_ID = Identifier.of("minecraft", "entity_interaction_range");

    @Override
    public void onInitializeClient() {
        // Register the generic animation loader
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new AnimationLoader());

        // Register the weapon-specific configuration loader
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new WeaponAnimationLoader());

        // --- ATTACK RANGE TOOLTIP LOGIC ---
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            try {
                AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
                if (modifiers != null) {
                    // 1. Calculate the modifier value
                    double rangeModifier = 0;
                    boolean hasRange = false;

                    for (var entry : modifiers.modifiers()) {
                        if (entry.attribute() != null) {
                            // Safe match logic
                            boolean isMatch = false;
                            try {
                                if (entry.attribute().matchesId(RANGE_ID)) isMatch = true;
                                else if (entry.attribute().value().getTranslationKey().contains("entity_interaction_range")) isMatch = true;
                            } catch(Exception e) {}

                            if (isMatch) {
                                rangeModifier += entry.modifier().value();
                                hasRange = true;
                            }
                        }
                    }

                    // 2. If we have a range modifier, we want to fix the tooltip
                    if (hasRange) {
                        // Calculate total (Assuming Base = 3.0 for Survival)
                        double displayValue = rangeModifier + 3.0;

                        // Create the custom line: " 5.0 Blocks Attack Range"
                        Text customLine = Text.literal(" ")
                                .append(Text.literal(String.format("%.1f", displayValue)).formatted(Formatting.DARK_GREEN))
                                .append(Text.literal(" ").formatted(Formatting.DARK_GREEN))
                                .append(Text.translatable("attack_range").formatted(Formatting.DARK_GREEN));

                        // 3. Find and Remove the vanilla line
                        String targetName = "Attack Range"; // This must match en_us.json value!
                        List<Text> toRemove = new ArrayList<>();

                        int insertIndex = -1;

                        for (int i = 0; i < lines.size(); i++) {
                            Text line = lines.get(i);
                            String string = line.getString();
                            if (string.contains(targetName) && (string.contains("+") || string.contains("-"))) {
                                toRemove.add(line);
                                if (insertIndex == -1) insertIndex = i; // Remember where it was
                            }
                        }

                        lines.removeAll(toRemove);

                        if (insertIndex != -1 && insertIndex < lines.size()) {
                            lines.add(insertIndex, customLine);
                        } else {
                            lines.add(customLine);
                        }
                    }
                }
            } catch (Exception e) {
                // Safety ignore
            }
        });

        // --- PARRY STATS TOOLTIP LOGIC ---
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.getItem() instanceof IParryItem parryItem) {
                // Add a blank line for spacing if needed
                // lines.add(Text.empty());

                int window = parryItem.getParryWindow();
                // Convert ticks to seconds (20 ticks = 1 second)
                double seconds = window / 20.0;
                String timeDisplay = String.format("%.2f", seconds);

                int blockPercent = (int)(parryItem.getDamageReduction() * 100);

                lines.add(Text.translatable("item.wowmod.tooltip.parry_window", timeDisplay).formatted(Formatting.BLUE));
                lines.add(Text.translatable("item.wowmod.tooltip.damage_reduction", blockPercent).formatted(Formatting.BLUE));
            }
        });
    }
}