package dev.moonseungjun.augmentationsurvival.augment.movement;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Extends vanilla movement-efficiency semantics to cobweb stuck multipliers, including vertical descent. */
public final class TerrainResistanceService {
    private TerrainResistanceService() {
    }

    public static Vec3 adjustCobwebMultiplier(Player player, Vec3 vanillaMultiplier) {
        double efficiency = Mth.clamp(player.getAttributeValue(Attributes.MOVEMENT_EFFICIENCY), 0.0D, 1.0D);
        if (efficiency <= 0.0D) {
            return vanillaMultiplier;
        }
        return new Vec3(
                towardNormal(vanillaMultiplier.x, efficiency),
                towardNormal(vanillaMultiplier.y, efficiency),
                towardNormal(vanillaMultiplier.z, efficiency)
        );
    }

    private static double towardNormal(double multiplier, double efficiency) {
        return multiplier + (1.0D - multiplier) * efficiency;
    }
}
