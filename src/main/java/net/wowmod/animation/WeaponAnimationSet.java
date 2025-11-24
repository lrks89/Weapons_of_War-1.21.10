package net.wowmod.animation;

import net.minecraft.util.Identifier;

public record WeaponAnimationSet(
        Identifier idleAnim,
        Identifier walkAnim,
        Identifier sprintAnim,
        Identifier attackAnim
) {}