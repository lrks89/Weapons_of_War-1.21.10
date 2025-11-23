package net.wowmod.animation;

import net.minecraft.util.math.Vec3d;
import java.util.Map;

public record AnimationDefinition(float length, boolean loop, Map<String, BoneAnimation> bones) {}