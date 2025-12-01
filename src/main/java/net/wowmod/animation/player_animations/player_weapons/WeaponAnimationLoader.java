package net.wowmod.animation.player_animations.player_weapons;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import net.wowmod.WeaponsOfWar;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class WeaponAnimationLoader implements IdentifiableResourceReloadListener {
    public static final Identifier ID = Identifier.of(WeaponsOfWar.MOD_ID, "weapon_animation_loader");
    private static final Gson GSON = new Gson();

    // Stores the loaded configs: Key is the Item Identifier, Value is the config.
    public static final Map<Identifier, WeaponAnimationConfig> WEAPON_ANIMATION_CONFIGS = new HashMap<>();

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public CompletableFuture<Void> reload(ResourceReloader.Store store, Executor prepareExecutor, ResourceReloader.Synchronizer synchronizer, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> {
                    Map<Identifier, WeaponAnimationConfig> loaded = new HashMap<>();
                    ResourceManager manager = store.getResourceManager();

                    // Finds all .json files in assets/wowmod/weapon_data
                    Map<Identifier, Resource> resources = manager.findResources("weapon_data", id -> id.getPath().endsWith(".json"));

                    for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
                        try (InputStreamReader reader = new InputStreamReader(entry.getValue().getInputStream())) {
                            // Infers the item ID (e.g., wowmod:m2613a_spear) from the file name.
                            String filename = entry.getKey().getPath().replace("weapon_data/", "").replace(".json", "");
                            Identifier itemIdentifier = Identifier.of(WeaponsOfWar.MOD_ID, filename);

                            WeaponAnimationConfig config = GSON.fromJson(reader, WeaponAnimationConfig.class);
                            loaded.put(itemIdentifier, config);

                        } catch (Exception e) {
                            WeaponsOfWar.LOGGER.error("Failed to load weapon animation config: " + entry.getKey(), e);
                        }
                    }
                    return loaded;
                }, prepareExecutor)
                .thenCompose(loaded -> synchronizer.whenPrepared(null).thenApply(v -> loaded))
                .thenAcceptAsync(loaded -> {
                    WEAPON_ANIMATION_CONFIGS.clear();
                    WEAPON_ANIMATION_CONFIGS.putAll(loaded);
                    WeaponsOfWar.LOGGER.info("WeaponsOfWar: Loaded " + loaded.size() + " custom weapon animation configs.");
                }, applyExecutor);
    }
}