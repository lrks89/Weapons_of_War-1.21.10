package net.wowmod.animation;

import net.minecraft.util.math.Vec3d;
import java.util.Map;

// Holds the entire animation
public record AnimationDefinition(float length, boolean loop, Map<String, BoneAnimation> bones) {}

