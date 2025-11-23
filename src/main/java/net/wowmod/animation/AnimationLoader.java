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

        // Looks for files in: assets/wowmod/player_animations/
        manager.findResources("player_animations", id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (InputStreamReader reader = new InputStreamReader(resource.getInputStream())) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);

                if (root.has("animations")) {
                    JsonObject animationsObj = root.getAsJsonObject("animations");

                    for (String animName : animationsObj.keySet()) {
                        JsonObject animData = animationsObj.getAsJsonObject(animName);
                        parseAnimation(id, animName, animData);
                    }
                }

            } catch (Exception e) {
                WeaponsOfWar.LOGGER.error("Failed to load animation: " + id, e);
            }
        });
    }

    private void parseAnimation(Identifier fileId, String animName, JsonObject json) {
        float length = json.has("animation_length") ? json.get("animation_length").getAsFloat() : 0.0f;

        boolean loop = json.has("loop") && json.get("loop").getAsBoolean();
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

        ANIMATIONS.put(fileId, new AnimationDefinition(length, loop, bones));
        WeaponsOfWar.LOGGER.info("Registered Animation: " + fileId);
    }

    private Map<Float, Vec3d> parseKeyframes(JsonObject json, String key) {
        Map<Float, Vec3d> keyframes = new HashMap<>();
        if (json.has(key)) {
            JsonElement element = json.get(key);

            if (element.isJsonArray()) {
                var arr = element.getAsJsonArray();
                keyframes.put(0.0f, new Vec3d(arr.get(0).getAsDouble(), arr.get(1).getAsDouble(), arr.get(2).getAsDouble()));
            }
            else if (element.isJsonObject()) {
                JsonObject keyframeData = element.getAsJsonObject();
                keyframeData.keySet().forEach(timeStr -> {
                    try {
                        float time = Float.parseFloat(timeStr);
                        JsonElement kfElement = keyframeData.get(timeStr);

                        if (kfElement.isJsonArray()) {
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