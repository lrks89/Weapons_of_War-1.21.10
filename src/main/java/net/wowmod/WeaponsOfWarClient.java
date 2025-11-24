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
import net.wowmod.animation.AnimationLoader;
import net.wowmod.animation.WeaponConfigLoader;

import java.util.ArrayList;
import java.util.List;

public class WeaponsOfWarClient implements ClientModInitializer {

    private static final Identifier RANGE_ID = Identifier.of("minecraft", "entity_interaction_range");

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new AnimationLoader());
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new WeaponConfigLoader());

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
                        // If you want to show just the modifier ("+2.0"), use rangeModifier.
                        // If you want total ("5.0"), use rangeModifier + 3.0.
                        double displayValue = rangeModifier + 3.0;

                        // Create the custom line: " 5.0 Blocks Attack Range"
                        Text customLine = Text.literal(" ")
                                .append(Text.literal(String.format("%.1f", displayValue)).formatted(Formatting.DARK_GREEN))
                                .append(Text.literal(" ").formatted(Formatting.DARK_GREEN))
                                .append(Text.translatable("attack_range").formatted(Formatting.DARK_GREEN));

                        // 3. Find and Remove the vanilla line
                        // Vanilla line looks like: " +2 Attack Range" (blue)
                        // We search for lines containing our translated name "Attack Range"
                        String targetName = "Attack Range"; // This must match en_us.json value!
                        List<Text> toRemove = new ArrayList<>();

                        int insertIndex = -1;

                        for (int i = 0; i < lines.size(); i++) {
                            Text line = lines.get(i);
                            String string = line.getString();
                            // Check if line contains "Attack Range" and looks like an attribute modifier
                            if (string.contains(targetName) && (string.contains("+") || string.contains("-"))) {
                                toRemove.add(line);
                                if (insertIndex == -1) insertIndex = i; // Remember where it was
                            }
                        }

                        // Remove vanilla lines
                        lines.removeAll(toRemove);

                        // 4. Add our custom line
                        // If we found where the old one was, put ours there. Otherwise add to end.
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
    }
}