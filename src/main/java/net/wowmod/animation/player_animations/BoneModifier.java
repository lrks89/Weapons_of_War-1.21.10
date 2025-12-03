package net.wowmod.animation.player_animations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.model.ModelPart;
import java.util.Map;

public class BoneModifier {

    public static void applyBone(ModelPart part, Animation.Bone boneData, float time, float defaultX, float defaultY, float defaultZ) {
        if (part == null) return;

        // If boneData is null, we do NOT reset the bone.
        // This allows Vanilla animations (set previously in setAngles) to persist for bones
        // that are not explicitly animated by the custom file.
        if (boneData == null) {
            return;
        }

        if (boneData.rotation != null) {
            float[] rot = getInterpolatedValue(boneData.rotation, time);
            part.pitch = (float) Math.toRadians(rot[0]);
            part.yaw = (float) Math.toRadians(rot[1]);
            part.roll = (float) Math.toRadians(rot[2]);
        } else {
            // If the bone is present but rotation is missing, we reset rotation to 0 (strict override).
            // This assumes if you list a bone, you want to control it fully.
            part.pitch = 0;
            part.yaw = 0;
            part.roll = 0;
        }

        if (boneData.position != null) {
            float[] pos = getInterpolatedValue(boneData.position, time);
            part.originX = defaultX + pos[0];
            part.originY = defaultY - pos[1]; // Note the minus sign, typical for Minecraft coordinate conversions
            part.originZ = defaultZ + pos[2];
        } else {
            part.originX = defaultX;
            part.originY = defaultY;
            part.originZ = defaultZ;
        }

        // Scale support can be added here similarly if needed, based on boneData.scale
    }

    private static float[] getInterpolatedValue(Map<String, JsonElement> keyframes, float time) {
        float prevTime = 0;
        float nextTime = 0;
        float[] prevVal = null;
        float[] nextVal = null;

        for (String key : keyframes.keySet()) {
            float t = Float.parseFloat(key);
            JsonElement vElement = keyframes.get(key);
            float[] v = parseVector(vElement);

            if (t <= time) {
                if (prevVal == null || t > prevTime) {
                    prevTime = t;
                    prevVal = v;
                }
            }
            if (t >= time) {
                if (nextVal == null || t < nextTime || (nextVal != null && t == nextTime && t == 0)) {
                    nextTime = t;
                    nextVal = v;
                }
            }
        }

        if (prevVal == null) return nextVal != null ? nextVal : new float[]{0, 0, 0};
        if (nextVal == null) return prevVal;
        if (prevTime == nextTime) return prevVal;

        float alpha = (time - prevTime) / (nextTime - prevTime);
        return new float[]{
                lerp(prevVal[0], nextVal[0], alpha),
                lerp(prevVal[1], nextVal[1], alpha),
                lerp(prevVal[2], nextVal[2], alpha)
        };
    }

    private static float[] parseVector(JsonElement element) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            return new float[]{
                    array.get(0).getAsFloat(),
                    array.get(1).getAsFloat(),
                    array.get(2).getAsFloat()
            };
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            // Bedrock animation "vector" handling
            if (obj.has("vector")) {
                return parseVector(obj.get("vector"));
            } else if (obj.has("post")) {
                return parseVector(obj.get("post"));
            } else if (obj.has("pre")) {
                return parseVector(obj.get("pre"));
            }
        }
        return new float[]{0, 0, 0};
    }

    private static float lerp(float start, float end, float alpha) {
        return start + alpha * (end - start);
    }
}