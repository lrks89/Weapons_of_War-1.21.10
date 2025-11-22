package net.wowmod.animation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import net.wowmod.WeaponsOfWar;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class WeaponConfigLoader implements SynchronousResourceReloader, IdentifiableResourceReloadListener {
    public static final Map<Identifier, WeaponAnimationSet> WEAPON_CONFIGS = new HashMap<>();
    private static final Gson GSON = new Gson();

    // Folder in assets/wowmod/
    private static final String DATA_FOLDER = "weapon_data";

    @Override
    public Identifier getFabricId() {
        return Identifier.of(WeaponsOfWar.MOD_ID, "weapon_config_loader");
    }

    @Override
    public void reload(ResourceManager manager) {
        WEAPON_CONFIGS.clear();
        WeaponsOfWar.LOGGER.info("Loading Weapon Configurations from 'assets/wowmod/" + DATA_FOLDER + "'...");

        manager.findResources(DATA_FOLDER, id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (InputStreamReader reader = new InputStreamReader(resource.getInputStream())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);

                Identifier idle = json.has("idle") ? Identifier.of(json.get("idle").getAsString()) : null;
                Identifier walk = json.has("walk") ? Identifier.of(json.get("walk").getAsString()) : null;
                Identifier sprint = json.has("sprint") ? Identifier.of(json.get("sprint").getAsString()) : null;
                Identifier sneak = json.has("sneak") ? Identifier.of(json.get("sneak").getAsString()) : null;
                Identifier attack = json.has("attack") ? Identifier.of(json.get("attack").getAsString()) : null;

                // Calculate Item ID from filename
                String namespace = id.getNamespace();
                String path = id.getPath(); // e.g., "weapon_data/m2613a_spear.json"

                // Extract name: remove "weapon_data/" (12 chars) and ".json" (5 chars)
                String itemName = path.substring(DATA_FOLDER.length() + 1, path.length() - 5);
                Identifier itemId = Identifier.of(namespace, itemName);

                WEAPON_CONFIGS.put(itemId, new WeaponAnimationSet(idle, walk, sprint, sneak, attack));

                WeaponsOfWar.LOGGER.info("Loaded Weapon Config for Item: " + itemId);

            } catch (Exception e) {
                WeaponsOfWar.LOGGER.error("Failed to load weapon config for: " + id, e);
            }
        });

        if (WEAPON_CONFIGS.isEmpty()) {
            WeaponsOfWar.LOGGER.warn("!!! NO WEAPON CONFIGS LOADED. Check your folder structure !!!");
        }
    }

    public static WeaponAnimationSet get(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        WeaponAnimationSet set = WEAPON_CONFIGS.get(id);
        if (set == null) {
            // Rate-limited debug warning could go here, but keeping it clean for now
        }
        return set;
    }
}