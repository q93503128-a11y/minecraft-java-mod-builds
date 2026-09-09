package kr.moonseungjun.riftfrontier.client.render;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import net.minecraft.resources.Identifier;

/** Canonical client resource identifiers for the accepted Region 01 boss presentation payload. */
public final class Region01BossRuntimeResources {
    public static final Identifier ACCEPTED_GEOMETRY = Identifier.fromNamespaceAndPath(
        Riftfrontier.MOD_ID,
        "boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf"
    );

    public static final String ACCEPTED_GEOMETRY_SHA256 =
        "ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac";

    private Region01BossRuntimeResources() {}
}
