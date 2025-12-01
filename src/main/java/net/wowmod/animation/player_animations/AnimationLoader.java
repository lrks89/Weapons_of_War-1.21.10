package net.wowmod.animation.player_animations;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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

public class AnimationLoader implements IdentifiableResourceReloadListener {
    public static final Identifier ID = Identifier.of(WeaponsOfWar.MOD_ID, "animation_loader");
    private static final Gson GSON = new Gson();

    // Global registry of loaded animations.
    // Key: Animation name (e.g., "default_idle"), Value: Animation data
    public static final Map<String, Animation> ANIMATIONS = new HashMap<>();

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public CompletableFuture<Void> reload(ResourceReloader.Store store, Executor prepareExecutor, ResourceReloader.Synchronizer synchronizer, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> {
                    // 1. Prepare: Load data on background thread
                    Map<String, Animation> loaded = new HashMap<>();
                    ResourceManager manager = store.getResourceManager();

                    // Finds all .json files in assets/wowmod/player_animations
                    Map<Identifier, Resource> resources = manager.findResources("player_animations", id -> id.getPath().endsWith(".json"));

                    for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
                        try (InputStreamReader reader = new InputStreamReader(entry.getValue().getInputStream())) {
                            // Parse the root JSON
                            JsonObject json = GSON.fromJson(reader, JsonObject.class);

                            // Bedrock files usually have a top-level "animations" object
                            if (json.has("animations")) {
                                JsonObject anims = json.getAsJsonObject("animations");
                                for (String key : anims.keySet()) {
                                    // key is likely "default_idle", "default_walking", etc.
                                    Animation anim = GSON.fromJson(anims.get(key), Animation.class);
                                    loaded.put(key, anim);
                                }
                            }
                        } catch (Exception e) {
                            WeaponsOfWar.LOGGER.error("Failed to load player animation: " + entry.getKey(), e);
                        }
                    }
                    return loaded;
                }, prepareExecutor)
                .thenCompose(loaded -> synchronizer.whenPrepared(null).thenApply(v -> loaded)) // Fixed synchronization logic
                .thenAcceptAsync(loaded -> {
                    // 3. Apply: Update game state on main thread
                    ANIMATIONS.clear();
                    ANIMATIONS.putAll(loaded);
                    WeaponsOfWar.LOGGER.info("WeaponsOfWar: Loaded " + loaded.size() + " custom player animations.");
                }, applyExecutor);
    }
}