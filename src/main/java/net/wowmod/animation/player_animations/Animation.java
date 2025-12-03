package net.wowmod.animation.player_animations;

import com.google.gson.JsonElement;
import java.util.Map;

public class Animation {

    public boolean loop;
    public float animation_length;
    public Map<String, Bone> bones;

    public static class Bone {
        // We changed this to JsonElement to support complex keyframes (arrays or objects)
        public Map<String, JsonElement> rotation;
        public Map<String, JsonElement> position;
        public Map<String, JsonElement> scale;
    }
}