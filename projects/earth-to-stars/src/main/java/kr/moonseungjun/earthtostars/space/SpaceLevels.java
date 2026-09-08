package kr.moonseungjun.earthtostars.space;

import kr.moonseungjun.earthtostars.EarthToStars;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class SpaceLevels {
    public static final ResourceKey<Level> ORBITAL_SPACE = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(EarthToStars.MOD_ID, "orbital_space")
    );
    public static final ResourceKey<Level> SHIP_INTERIORS = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(EarthToStars.MOD_ID, "ship_interiors")
    );

    private SpaceLevels() {
    }
}
