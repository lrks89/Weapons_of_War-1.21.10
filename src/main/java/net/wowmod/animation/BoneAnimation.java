package net.wowmod.animation;

import net.minecraft.util.math.Vec3d;

import java.util.Map;

// Holds data for a specific body part (e.g., right_arm)
public record BoneAnimation(Map<Float, Vec3d> rotationKeyframes, Map<Float, Vec3d> positionKeyframes) {}
