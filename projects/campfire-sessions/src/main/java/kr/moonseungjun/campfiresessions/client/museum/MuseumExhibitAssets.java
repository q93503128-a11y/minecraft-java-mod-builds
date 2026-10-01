package kr.moonseungjun.campfiresessions.client.museum;

import kr.moonseungjun.campfiresessions.CampfireSessions;
import net.minecraft.resources.Identifier;

/**
 * Curated production museum assets.
 *
 * <p>The identifiers below are contracts for verified Smithsonian-derived runtime
 * assets. They are intentionally not backed by fallback geometry. Until visual and
 * performance acceptance is complete, the files are supplied only by the asset-review
 * resource path and are not bundled into Campfire's normal runtime resources.</p>
 */
public final class MuseumExhibitAssets {
    public static final MuseumStaticMeshAsset TRICERATOPS_100K = new MuseumStaticMeshAsset(
            id("museum/triceratops_horridus/mesh_00_primitive_00.cfmesh"),
            "89ec914596997248b7e6c7f542f5bd63d22f48d4fdd3010f49a98681a34febcf",
            id("museum/triceratops_horridus/textures/image_00.jpg"),
            id("museum/triceratops_horridus/textures/image_02.jpg"),
            id("museum/triceratops_horridus/textures/image_01.jpg")
    );

    private MuseumExhibitAssets() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CampfireSessions.MOD_ID, path);
    }

    public record MuseumStaticMeshAsset(
            Identifier mesh,
            String meshSha256,
            Identifier baseColorTexture,
            Identifier normalTexture,
            Identifier occlusionTexture
    ) {
        public MuseumStaticMeshAsset {
            if (meshSha256.length() != 64 || !meshSha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("Museum mesh SHA-256 must be 64 lowercase hex characters");
            }
        }
    }
}
