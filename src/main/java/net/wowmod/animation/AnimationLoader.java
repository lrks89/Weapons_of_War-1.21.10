package net.wowmod.animation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.wowmod.WeaponsOfWar;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class AnimationLoader implements SynchronousResourceReloader, IdentifiableResourceReloadListener {
    public static final Map<Identifier, AnimationDefinition> ANIMATIONS = new HashMap<>();
    private static final Gson GSON = new Gson();

    @Override
    public Identifier getFabricId() {
        return Identifier.of(WeaponsOfWar.MOD_ID, "animation_loader");
    }

    @Override
    public void reload(ResourceManager manager) {
        ANIMATIONS.clear();
        WeaponsOfWar.LOGGER.info("Loading Player Animations...");

        manager.findResources("player_animations", id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (InputStreamReader reader = new InputStreamReader(resource.getInputStream())) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);

                if (root.has("animations")) {
                    JsonObject animationsObj = root.getAsJsonObject("animations");

                    for (String animName : animationsObj.keySet()) {
                        JsonObject animData = animationsObj.getAsJsonObject(animName);

                        // 1. Generate Clean ID (e.g., wowmod:default_idle)
                        String path = id.getPath();
                        String filename = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
                        Identifier cleanId = Identifier.of(id.getNamespace(), filename);

                        // 2. Parse Animation
                        AnimationDefinition def = parseAnimation(animName, animData);

                        if (def != null) {
                            // 3. Register under BOTH IDs to ensure lookup works regardless of config style
                            ANIMATIONS.put(cleanId, def);
                            ANIMATIONS.put(id, def);

                            // Also try to register without extension if it's in a subfolder (common config pattern)
                            // e.g., wowmod:player_animations/default_idle
                            String pathNoExt = path.substring(0, path.lastIndexOf('.'));
                            Identifier noExtId = Identifier.of(id.getNamespace(), pathNoExt);
                            ANIMATIONS.put(noExtId, def);

                            WeaponsOfWar.LOGGER.info("Registered Animation: " + cleanId);
                        }
                    }
                }

            } catch (Exception e) {
                WeaponsOfWar.LOGGER.error("Failed to load animation: " + id, e);
            }
        });
    }

    private AnimationDefinition parseAnimation(String animName, JsonObject json) {
        float length = json.has("animation_length") ? json.get("animation_length").getAsFloat() : 0.0f;
        boolean loop = json.has("loop") && json.get("loop").getAsBoolean();

        // Auto-loop detection
        if (!loop) {
            String lowerName = animName.toLowerCase();
            if (lowerName.contains("idle") || lowerName.contains("walk") || lowerName.contains("sprint") || lowerName.contains("run")) {
                loop = true;
            }
        }

        Map<String, BoneAnimation> bones = new HashMap<>();

        if (json.has("bones")) {
            JsonObject bonesJson = json.getAsJsonObject("bones");
            bonesJson.keySet().forEach(boneName -> {
                JsonObject boneData = bonesJson.getAsJsonObject(boneName);
                Map<Float, Vec3d> rotations = parseKeyframes(boneData, "rotation");
                Map<Float, Vec3d> positions = parseKeyframes(boneData, "position");
                bones.put(boneName, new BoneAnimation(rotations, positions));
            });
        }

        return new AnimationDefinition(length, loop, bones);
    }

    private Map<Float, Vec3d> parseKeyframes(JsonObject json, String key) {
        Map<Float, Vec3d> keyframes = new HashMap<>();
        if (json.has(key)) {
            JsonElement element = json.get(key);

            // Case 1: Static Array [x, y, z]
            if (element.isJsonArray()) {
                var arr = element.getAsJsonArray();
                keyframes.put(0.0f, new Vec3d(arr.get(0).getAsDouble(), arr.get(1).getAsDouble(), arr.get(2).getAsDouble()));
            }
            // Case 2: Object with timestamps "0.0": ...
            else if (element.isJsonObject()) {
                JsonObject keyframeData = element.getAsJsonObject();
                keyframeData.keySet().forEach(timeStr -> {
                    try {
                        float time = Float.parseFloat(timeStr);
                        JsonElement kfElement = keyframeData.get(timeStr);

                        // Handle Bedrock "vector" object format
                        if (kfElement.isJsonObject()) {
                            JsonObject kfObj = kfElement.getAsJsonObject();
                            if (kfObj.has("vector")) {
                                var arr = kfObj.getAsJsonArray("vector");
                                keyframes.put(time, new Vec3d(arr.get(0).getAsDouble(), arr.get(1).getAsDouble(), arr.get(2).getAsDouble()));
                            } else if (kfObj.has("post")) { // Handle curves
                                var arr = kfObj.getAsJsonArray("post");
                                keyframes.put(time, new Vec3d(arr.get(0).getAsDouble(), arr.get(1).getAsDouble(), arr.get(2).getAsDouble()));
                            }
                        }
                        // Handle simple array format inside timestamp
                        else if (kfElement.isJsonArray()) {
                            var arr = kfElement.getAsJsonArray();
                            keyframes.put(time, new Vec3d(arr.get(0).getAsDouble(), arr.get(1).getAsDouble(), arr.get(2).getAsDouble()));
                        }
                    } catch (NumberFormatException ignored) {}
                });
            }
        }
        return keyframes;
    }
}