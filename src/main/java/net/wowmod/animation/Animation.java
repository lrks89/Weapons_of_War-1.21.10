package net.wowmod.animation;

import java.util.Map;

public class Animation {
    public boolean loop;
    public float animation_length;
    public Map<String, Bone> bones;

    public static class Bone {
        public Map<String, float[]> rotation;
        public Map<String, float[]> position;
        public Map<String, float[]> scale;
    }
}