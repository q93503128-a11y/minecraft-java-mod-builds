package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class BossPresentationAssetManifestCodecTest {
    @Test
    void decodesSelectedProductionAssetManifest() {
        String json = """
            {
              "kind":"boss_presentation_asset_manifest",
              "schema_version":1,
              "assets":[
                {
                  "logical_key":"riftfrontier:models/region_01_boss",
                  "asset_kind":"MODEL",
                  "resource_id":"riftfrontier:geckolib/models/entity/region_01_boss.geo",
                  "source":"Riftfrontier production asset",
                  "license_note":"project-owned"
                },
                {
                  "logical_key":"riftfrontier:animations/region_01_boss/charge",
                  "asset_kind":"ANIMATION",
                  "resource_id":"riftfrontier:geckolib/animations/entity/region_01_boss.animation",
                  "source":"Riftfrontier production asset",
                  "license_note":"project-owned"
                }
              ]
            }
            """;

        BossPresentationAssetManifest manifest = new BossPresentationAssetManifestCodec().decode(json);
        assertEquals(2, manifest.assets().size());
        var model = manifest.assets().get(ContentId.parse("riftfrontier:models/region_01_boss"));
        assertEquals(BossPresentationAssetManifest.Kind.MODEL, model.kind());
        assertEquals(ContentId.parse("riftfrontier:geckolib/models/entity/region_01_boss.geo"), model.resourceId());
    }

    @Test
    void rejectsUnsupportedSchemaAndUnknownFields() {
        String wrongSchema = """
            {"kind":"boss_presentation_asset_manifest","schema_version":2,"assets":[]}
            """;
        assertThrows(IllegalArgumentException.class, () -> new BossPresentationAssetManifestCodec().decode(wrongSchema));

        String unknownField = """
            {"kind":"boss_presentation_asset_manifest","schema_version":1,"assets":[],"fallback":"guess"}
            """;
        assertThrows(IllegalArgumentException.class, () -> new BossPresentationAssetManifestCodec().decode(unknownField));
    }

    @Test
    void rejectsUnknownKindsAndDuplicateLogicalKeys() {
        String unknownKind = """
            {"kind":"boss_presentation_asset_manifest","schema_version":1,"assets":[
              {"logical_key":"riftfrontier:vfx/x","asset_kind":"PARTICLE_MAGIC","resource_id":"riftfrontier:particles/x","source":"owned","license_note":"project-owned"}
            ]}
            """;
        assertThrows(IllegalArgumentException.class, () -> new BossPresentationAssetManifestCodec().decode(unknownKind));

        String duplicate = """
            {"kind":"boss_presentation_asset_manifest","schema_version":1,"assets":[
              {"logical_key":"riftfrontier:sounds/x","asset_kind":"SOUND","resource_id":"riftfrontier:sounds/x","source":"owned","license_note":"project-owned"},
              {"logical_key":"riftfrontier:sounds/x","asset_kind":"SOUND","resource_id":"riftfrontier:sounds/y","source":"owned","license_note":"project-owned"}
            ]}
            """;
        assertThrows(IllegalArgumentException.class, () -> new BossPresentationAssetManifestCodec().decode(duplicate));
    }

    @Test
    void rejectsBlankProvenanceInsteadOfPromotingAnUntraceableAsset() {
        String blankSource = """
            {"kind":"boss_presentation_asset_manifest","schema_version":1,"assets":[
              {"logical_key":"riftfrontier:models/x","asset_kind":"MODEL","resource_id":"riftfrontier:models/x","source":" ","license_note":"project-owned"}
            ]}
            """;
        assertThrows(IllegalArgumentException.class, () -> new BossPresentationAssetManifestCodec().decode(blankSource));
    }
}
